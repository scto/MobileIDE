/// Document symbols provider: produces a hierarchical outline of all keys
/// and values in a JSON document.
///
/// Uses direct recursive traversal matching the approach of
/// vscode-json-languageservice. All node dispatch uses pre-cached numeric
/// kind/field IDs and position conversion uses the O(1) ASCII fast-path.
use lsp_types::*;
use tree_sitter::Node;

use crate::document::Document;
use crate::tree::{self, FieldIds, KindIds, is_value_node_id};

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/// Produce hierarchical document symbols.
pub fn document_symbols(doc: &Document) -> Vec<DocumentSymbol> {
    let Some(root_value) = tree::root_value(&doc.tree) else {
        return Vec::new();
    };

    let kinds = doc.kind_ids();
    let fields = doc.field_ids();
    let source = doc.source();
    let root_kind = root_value.kind_id();

    if root_kind == kinds.object {
        collect_object(doc, source, root_value, kinds, fields)
    } else if root_kind == kinds.array {
        collect_array(doc, source, root_value, kinds, fields)
    } else {
        Vec::new()
    }
}

// ---------------------------------------------------------------------------
// Object children
// ---------------------------------------------------------------------------

fn collect_object(
    doc: &Document,
    source: &[u8],
    object: Node<'_>,
    kinds: &KindIds,
    fields: &FieldIds,
) -> Vec<DocumentSymbol> {
    let mut cursor = object.walk();
    if !cursor.goto_first_child() {
        return Vec::new();
    }

    let mut result = Vec::with_capacity(object.named_child_count());

    loop {
        let pair = cursor.node();
        if pair.kind_id() == kinds.pair {
            if let Some(sym) = pair_symbol(doc, source, pair, kinds, fields) {
                result.push(sym);
            }
        }
        if !cursor.goto_next_sibling() {
            break;
        }
    }

    result
}

#[inline]
fn pair_symbol(
    doc: &Document,
    source: &[u8],
    pair: Node<'_>,
    kinds: &KindIds,
    fields: &FieldIds,
) -> Option<DocumentSymbol> {
    let key_node = pair.child_by_field_id(fields.key)?;
    let name = string_content_fast(key_node, source)?.to_string();

    let value_node = pair.child_by_field_id(fields.value);
    let (detail, kind, children) = match value_node {
        Some(v) => {
            let vk = v.kind_id();
            let children = if vk == kinds.object {
                Some(collect_object(doc, source, v, kinds, fields))
            } else if vk == kinds.array {
                Some(collect_array(doc, source, v, kinds, fields))
            } else {
                None
            };
            (
                value_detail(source, v, kinds),
                symbol_kind(vk, kinds),
                children,
            )
        }
        None => (None, SymbolKind::NULL, None),
    };

    let range = doc.node_range(&pair);
    let selection_range = doc.node_range(&key_node);

    #[allow(deprecated)]
    Some(DocumentSymbol {
        name,
        detail,
        kind,
        tags: None,
        deprecated: None,
        range,
        selection_range,
        children,
    })
}

// ---------------------------------------------------------------------------
// Array children
// ---------------------------------------------------------------------------

fn collect_array(
    doc: &Document,
    source: &[u8],
    array: Node<'_>,
    kinds: &KindIds,
    fields: &FieldIds,
) -> Vec<DocumentSymbol> {
    let mut cursor = array.walk();
    if !cursor.goto_first_child() {
        return Vec::new();
    }

    let mut result = Vec::with_capacity(array.named_child_count());
    let mut index = 0usize;
    let mut itoa_buf = itoa::Buffer::new();

    loop {
        let item = cursor.node();
        let item_kind = item.kind_id();
        if is_value_node_id(item_kind, kinds) {
            let formatted = itoa_buf.format(index);
            let mut name = String::with_capacity(formatted.len() + 2);
            name.push('[');
            name.push_str(formatted);
            name.push(']');
            let detail = value_detail(source, item, kinds);
            let kind = symbol_kind(item_kind, kinds);
            let range = doc.node_range(&item);

            let children = if item_kind == kinds.object {
                Some(collect_object(doc, source, item, kinds, fields))
            } else if item_kind == kinds.array {
                Some(collect_array(doc, source, item, kinds, fields))
            } else {
                None
            };

            #[allow(deprecated)]
            result.push(DocumentSymbol {
                name,
                detail,
                kind,
                tags: None,
                deprecated: None,
                range,
                selection_range: range,
                children,
            });
            index += 1;
        }
        if !cursor.goto_next_sibling() {
            break;
        }
    }

    result
}

#[inline]
fn symbol_kind_num(kind_id: u16, kinds: &KindIds) -> u32 {
    if kind_id == kinds.object {
        19
    } else if kind_id == kinds.array {
        18
    } else if kind_id == kinds.string {
        15
    } else if kind_id == kinds.number {
        16
    } else if kind_id == kinds.r#true || kind_id == kinds.r#false {
        17
    } else if kind_id == kinds.null {
        21
    } else {
        20 // KEY
    }
}

// ---------------------------------------------------------------------------
// Direct-to-string serialization (zero intermediate allocations)
// ---------------------------------------------------------------------------

/// Context passed through the recursive write functions to avoid repeated
/// field lookups and to carry the pre-resolved `is_ascii` flag.
struct WriteCtx<'a> {
    doc: &'a Document,
    source: &'a [u8],
    kinds: &'a KindIds,
    fields: &'a FieldIds,
    is_ascii: bool,
}

/// Produce document symbols serialized directly to a JSON string.
/// Single-pass: walks the tree and appends JSON to a `String` buffer,
/// avoiding all intermediate `Value` / `Map` / `Vec<Value>` allocations.
pub fn document_symbols_string(doc: &Document) -> String {
    let mut buf = String::with_capacity(doc.source().len().max(128) * 2);
    write_document_symbols(doc, &mut buf);
    buf
}

/// Append document symbols JSON to an existing buffer. This allows callers
/// to pre-fill the buffer with a prefix (e.g. a JSON-RPC envelope) and
/// avoid a second allocation + copy.
pub fn write_document_symbols(doc: &Document, buf: &mut String) {
    let mut itoa_buf = itoa::Buffer::new();

    let Some(root_value) = tree::root_value(&doc.tree) else {
        buf.push_str("[]");
        return;
    };

    let ctx = WriteCtx {
        doc,
        source: doc.source(),
        kinds: doc.kind_ids(),
        fields: doc.field_ids(),
        is_ascii: doc.is_ascii(),
    };
    let root_kind = root_value.kind_id();

    // Single cursor reused across the entire traversal — avoids one
    // ts_tree_cursor_new / ts_tree_cursor_delete FFI round-trip per container.
    let mut cursor = root_value.walk();

    buf.push('[');
    if root_kind == ctx.kinds.object {
        write_object_children(buf, &mut itoa_buf, &ctx, &mut cursor, root_value);
    } else if root_kind == ctx.kinds.array {
        write_array_children(buf, &mut itoa_buf, &ctx, &mut cursor, root_value);
    }
    buf.push(']');
}

fn write_object_children<'a>(
    buf: &mut String,
    itoa_buf: &mut itoa::Buffer,
    ctx: &WriteCtx<'a>,
    cursor: &mut tree_sitter::TreeCursor<'a>,
    object: Node<'a>,
) {
    cursor.reset(object);
    if !cursor.goto_first_child() {
        return;
    }

    let mut first = true;
    loop {
        let pair = cursor.node();
        if pair.kind_id() == ctx.kinds.pair {
            if let Some(key_node) = pair.child_by_field_id(ctx.fields.key) {
                if let Some(name) = string_content_fast(key_node, ctx.source) {
                    if !first {
                        buf.push(',');
                    }
                    first = false;

                    let value_node = pair.child_by_field_id(ctx.fields.value);
                    // Compute kind_id once — reused for symbol_kind, detail, and children dispatch
                    let vk = value_node.map(|v| v.kind_id());
                    let kind = match vk {
                        Some(k) => symbol_kind_num(k, ctx.kinds),
                        None => 21,
                    };

                    buf.push('{');
                    buf.push_str("\"name\":\"");
                    write_json_string_content(buf, name);
                    buf.push('"');
                    if let Some(v) = value_node {
                        write_detail_to_buf(buf, ctx.source, v, vk.unwrap(), ctx.kinds);
                    }
                    buf.push_str(",\"kind\":");
                    buf.push_str(itoa_buf.format(kind));
                    buf.push_str(",\"range\":");
                    write_node_range(buf, itoa_buf, ctx, &pair);
                    buf.push_str(",\"selectionRange\":");
                    write_node_range(buf, itoa_buf, ctx, &key_node);
                    if let Some(v) = value_node {
                        write_children(
                            buf,
                            itoa_buf,
                            ctx,
                            cursor,
                            vk.unwrap(),
                            v,
                            object,
                            pair.start_byte(),
                        );
                    }
                    buf.push('}');
                }
            }
        }
        if !cursor.goto_next_sibling() {
            break;
        }
    }
}

fn write_array_children<'a>(
    buf: &mut String,
    itoa_buf: &mut itoa::Buffer,
    ctx: &WriteCtx<'a>,
    cursor: &mut tree_sitter::TreeCursor<'a>,
    array: Node<'a>,
) {
    cursor.reset(array);
    if !cursor.goto_first_child() {
        return;
    }

    let mut index = 0usize;
    let mut first = true;

    loop {
        let item = cursor.node();
        let item_kind = item.kind_id();
        if is_value_node_id(item_kind, ctx.kinds) {
            if !first {
                buf.push(',');
            }
            first = false;

            let kind = symbol_kind_num(item_kind, ctx.kinds);

            buf.push('{');
            // "name":"[N]"
            buf.push_str("\"name\":\"[");
            buf.push_str(itoa_buf.format(index));
            buf.push_str("]\"");
            // ,"detail":"..." (optional) — written directly, no String allocation
            write_detail_to_buf(buf, ctx.source, item, item_kind, ctx.kinds);
            // ,"kind":N
            buf.push_str(",\"kind\":");
            buf.push_str(itoa_buf.format(kind));
            // ,"range":{...}
            buf.push_str(",\"range\":");
            write_node_range(buf, itoa_buf, ctx, &item);
            // ,"selectionRange":{...} (same as range for array items)
            buf.push_str(",\"selectionRange\":");
            write_node_range(buf, itoa_buf, ctx, &item);
            write_children(
                buf,
                itoa_buf,
                ctx,
                cursor,
                item_kind,
                item,
                array,
                item.start_byte(),
            );
            buf.push('}');
            index += 1;
        }
        if !cursor.goto_next_sibling() {
            break;
        }
    }
}

/// Write `,"children":[...]` for object/array values, restoring the cursor
/// to `parent` at `restore_byte` afterwards. No-op for non-container values.
fn write_children<'a>(
    buf: &mut String,
    itoa_buf: &mut itoa::Buffer,
    ctx: &WriteCtx<'a>,
    cursor: &mut tree_sitter::TreeCursor<'a>,
    value_kind: u16,
    value_node: Node<'a>,
    parent: Node<'a>,
    restore_byte: usize,
) {
    if value_kind == ctx.kinds.object {
        buf.push_str(",\"children\":[");
        write_object_children(buf, itoa_buf, ctx, cursor, value_node);
        buf.push(']');
        cursor.reset(parent);
        cursor.goto_first_child_for_byte(restore_byte);
    } else if value_kind == ctx.kinds.array {
        buf.push_str(",\"children\":[");
        write_array_children(buf, itoa_buf, ctx, cursor, value_node);
        buf.push(']');
        cursor.reset(parent);
        cursor.goto_first_child_for_byte(restore_byte);
    }
}

/// Write an LSP range directly from a tree-sitter node. For ASCII documents
/// (the common case) uses the O(1) row/column path; otherwise converts via
/// `Document::node_range` for correct UTF-16 columns.
#[inline]
fn write_node_range(
    buf: &mut String,
    itoa_buf: &mut itoa::Buffer,
    ctx: &WriteCtx<'_>,
    node: &Node<'_>,
) {
    let (sl, sc, el, ec) = if ctx.is_ascii {
        let sp = node.start_position();
        let ep = node.end_position();
        (
            sp.row as u32,
            sp.column as u32,
            ep.row as u32,
            ep.column as u32,
        )
    } else {
        let r = ctx.doc.node_range(node);
        (r.start.line, r.start.character, r.end.line, r.end.character)
    };
    buf.push_str("{\"start\":{\"line\":");
    buf.push_str(itoa_buf.format(sl));
    buf.push_str(",\"character\":");
    buf.push_str(itoa_buf.format(sc));
    buf.push_str("},\"end\":{\"line\":");
    buf.push_str(itoa_buf.format(el));
    buf.push_str(",\"character\":");
    buf.push_str(itoa_buf.format(ec));
    buf.push_str("}}");
}

/// Write a `,"detail":"..."` field directly to the buffer if the node has a
/// displayable detail. Avoids the `String` allocation that `value_detail()` does.
/// Accepts a pre-computed `kind_id` to avoid a redundant FFI call.
#[inline]
fn write_detail_to_buf(buf: &mut String, source: &[u8], node: Node<'_>, kid: u16, kinds: &KindIds) {
    if kid == kinds.string {
        let Some(s) = string_content_fast(node, source) else {
            return;
        };
        buf.push_str(",\"detail\":\"\\\"");
        if s.len() > 60 {
            // Find a char boundary at or before byte 57 to avoid panicking
            // on multi-byte UTF-8 sequences.
            write_json_string_content(buf, &s[..s.floor_char_boundary(57)]);
            buf.push_str("...");
        } else {
            write_json_string_content(buf, s);
        }
        buf.push_str("\\\"\"");
    } else if kid == kinds.number
        || kid == kinds.r#true
        || kid == kinds.r#false
        || kid == kinds.null
    {
        let start = node.start_byte();
        let end = node.end_byte();
        // Safety: source is Document.text.as_bytes(), guaranteed valid UTF-8.
        // Number/bool/null tokens are pure ASCII.
        let text = unsafe { std::str::from_utf8_unchecked(&source[start..end]) };
        buf.push_str(",\"detail\":\"");
        buf.push_str(text);
        buf.push('"');
    }
}

/// Write JSON-escaped string content (without surrounding quotes) to the buffer.
#[inline]
fn write_json_string_content(buf: &mut String, s: &str) {
    // Fast path: scan for bytes that need escaping. Most JSON keys are simple
    // ASCII identifiers with no special characters.
    if !s.bytes().any(|b| b == b'"' || b == b'\\' || b < 0x20) {
        buf.push_str(s);
        return;
    }
    // Slow path: escape character by character.
    for ch in s.chars() {
        match ch {
            '"' => buf.push_str("\\\""),
            '\\' => buf.push_str("\\\\"),
            '\n' => buf.push_str("\\n"),
            '\r' => buf.push_str("\\r"),
            '\t' => buf.push_str("\\t"),
            c if (c as u32) < 0x20 => {
                buf.push_str("\\u");
                let n = c as u32;
                for &shift in &[12, 8, 4, 0] {
                    let nibble = (n >> shift) & 0xF;
                    buf.push(char::from_digit(nibble, 16).unwrap());
                }
            }
            c => buf.push(c),
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/// Extract the unquoted string content directly from source bytes.
/// Avoids the FFI round-trip of `node.utf8_text()` by slicing source directly.
///
/// # Safety justification for `from_utf8_unchecked`:
/// `source` is always `Document.text.as_bytes()` where `text` is a `String`,
/// so the bytes are valid UTF-8. Tree-sitter byte offsets always land on
/// character boundaries, and quote bytes (`"`) are single-byte ASCII, so
/// `start + 1` / `end - 1` also land on character boundaries.
#[inline]
fn string_content_fast<'a>(node: Node<'_>, source: &'a [u8]) -> Option<&'a str> {
    let start = node.start_byte();
    let end = node.end_byte();
    if end <= start {
        return None;
    }
    unsafe {
        if end - start >= 2 && source[start] == b'"' && source[end - 1] == b'"' {
            Some(std::str::from_utf8_unchecked(&source[start + 1..end - 1]))
        } else {
            Some(std::str::from_utf8_unchecked(&source[start..end]))
        }
    }
}

#[inline]
fn symbol_kind(kind_id: u16, kinds: &KindIds) -> SymbolKind {
    if kind_id == kinds.object {
        SymbolKind::OBJECT
    } else if kind_id == kinds.array {
        SymbolKind::ARRAY
    } else if kind_id == kinds.string {
        SymbolKind::STRING
    } else if kind_id == kinds.number {
        SymbolKind::NUMBER
    } else if kind_id == kinds.r#true || kind_id == kinds.r#false {
        SymbolKind::BOOLEAN
    } else if kind_id == kinds.null {
        SymbolKind::NULL
    } else {
        SymbolKind::KEY
    }
}

#[inline]
fn value_detail(source: &[u8], node: Node<'_>, kinds: &KindIds) -> Option<String> {
    let kid = node.kind_id();
    if kid == kinds.string {
        let s = string_content_fast(node, source)?;
        if s.len() > 60 {
            Some(format!("\"{}...\"", &s[..s.floor_char_boundary(57)]))
        } else {
            Some(format!("\"{s}\""))
        }
    } else if kid == kinds.number
        || kid == kinds.r#true
        || kid == kinds.r#false
        || kid == kinds.null
    {
        let start = node.start_byte();
        let end = node.end_byte();
        Some(std::str::from_utf8(&source[start..end]).ok()?.to_string())
    } else {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::document::Document;

    #[test]
    fn empty_object_no_symbols() {
        let doc = Document::new("{}".into(), 0);
        let syms = document_symbols(&doc);
        assert!(syms.is_empty());
    }

    #[test]
    fn flat_object_symbols() {
        let doc = Document::new(r#"{"name": "Alice", "age": 30}"#.into(), 0);
        let syms = document_symbols(&doc);
        assert_eq!(syms.len(), 2);
        assert_eq!(syms[0].name, "name");
        assert_eq!(syms[1].name, "age");
    }

    #[test]
    fn nested_object_symbols() {
        let doc = Document::new(r#"{"person": {"name": "Bob"}}"#.into(), 0);
        let syms = document_symbols(&doc);
        assert_eq!(syms.len(), 1);
        assert_eq!(syms[0].name, "person");
        assert!(syms[0].children.is_some());
        let children = syms[0].children.as_ref().unwrap();
        assert_eq!(children.len(), 1);
        assert_eq!(children[0].name, "name");
    }

    #[test]
    fn array_symbols() {
        let doc = Document::new(r#"{"items": [1, 2, 3]}"#.into(), 0);
        let syms = document_symbols(&doc);
        assert_eq!(syms.len(), 1);
        let children = syms[0].children.as_ref().unwrap();
        assert_eq!(children.len(), 3);
        assert_eq!(children[0].name, "[0]");
        assert_eq!(children[1].name, "[1]");
        assert_eq!(children[2].name, "[2]");
    }

    #[test]
    fn root_array_no_symbols() {
        let doc = Document::new("[1, 2, 3]".into(), 0);
        let syms = document_symbols(&doc);
        assert_eq!(syms.len(), 3);
    }

    #[test]
    fn symbol_kinds() {
        let doc = Document::new(
            r#"{"str": "hi", "num": 42, "bool": true, "nil": null, "obj": {}, "arr": []}"#.into(),
            0,
        );
        let syms = document_symbols(&doc);
        assert_eq!(syms.len(), 6);
        assert_eq!(syms[0].kind, SymbolKind::STRING);
        assert_eq!(syms[1].kind, SymbolKind::NUMBER);
        assert_eq!(syms[2].kind, SymbolKind::BOOLEAN);
        assert_eq!(syms[3].kind, SymbolKind::NULL);
        assert_eq!(syms[4].kind, SymbolKind::OBJECT);
        assert_eq!(syms[5].kind, SymbolKind::ARRAY);
    }

    #[test]
    fn empty_document_no_symbols() {
        let doc = Document::new("".into(), 0);
        let syms = document_symbols(&doc);
        assert!(syms.is_empty());
    }

    /// Helper: verify `document_symbols_string` produces semantically identical
    /// JSON to `serde_json::to_value(document_symbols(...))`.
    fn assert_string_parity(json: &str) {
        let doc = Document::new(json.into(), 0);
        let expected = serde_json::to_value(document_symbols(&doc)).unwrap();
        let direct_str = document_symbols_string(&doc);
        let actual: serde_json::Value = serde_json::from_str(&direct_str)
            .unwrap_or_else(|e| panic!("Invalid JSON for input {json}: {e}\nOutput: {direct_str}"));
        assert_eq!(expected, actual, "Mismatch for input: {json}");
    }

    /// Comprehensive parity test covering: flat/nested objects, root arrays,
    /// all value types, empty documents, escapes, control chars, unicode,
    /// deep nesting, large arrays, empty strings, and malformed JSON.
    #[test]
    fn string_parity() {
        let long_2byte = format!("{}émore_text_here_to_exceed_sixty", "a".repeat(56));
        let long_3byte = format!("{}中文text_to_pad_past_sixty_chars_limit!!", "a".repeat(55));
        let long_4byte = format!(
            "{}\u{1F600}more_text_to_exceed_sixty_bytes!!",
            "a".repeat(55)
        );
        let large_array = format!(
            "[{}]",
            (0..50).map(|i| i.to_string()).collect::<Vec<_>>().join(",")
        );

        let cases = vec![
            // basic
            r#"{"name": "Alice", "age": 30}"#.into(),
            r#"{"person": {"name": "Bob"}, "scores": [100, 200]}"#.into(),
            r#"[1, "hello", true, null, {}, []]"#.into(),
            "{}".into(),
            "".into(),
            // escapes & control chars
            r#"{"key\"with\\escapes": 1, "new\nline": 2}"#.into(),
            r#"{"back\\slash": "back\\slash"}"#.into(),
            r#"{"tab\there": "tab\there"}"#.into(),
            "{\"key\": \"line1\\tline2\\nline3\\rline4\"}".into(),
            // multi-byte truncation boundaries
            format!(r#"{{"key": "{}"}}"#, long_2byte),
            format!(r#"{{"key": "{}"}}"#, long_3byte),
            format!(r#"{{"key": "{}"}}"#, long_4byte),
            // unicode (non-ASCII position path)
            r#"{"名前": "太郎", "数値": 42}"#.into(),
            // empty strings, deep nesting, large array
            r#"{"": "", "a": ""}"#.into(),
            r#"{"a": {"b": {"c": {"d": [1, {"e": true}]}}}}"#.into(),
            large_array,
            // malformed JSON (error recovery)
            r#"{"a": 1, "b": }"#.into(),
            r#"{"a": 1 "b": 2}"#.into(),
            r#"{"a": 1, }"#.into(),
            r#"{,}"#.into(),
            r#"{"a": [1, 2,]}"#.into(),
            r#"{"key": }"#.into(),
        ];
        for json in &cases {
            assert_string_parity(json);
        }
    }
}

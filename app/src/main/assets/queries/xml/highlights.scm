; XML Tags
(element
  open_tag: (STag
    "<" @punctuation.bracket
    (Name) @tag
    ">" @punctuation.bracket))

(element
  close_tag: (ETag
    "</" @punctuation.bracket
    (Name) @tag
    ">" @punctuation.bracket))

(empty_element
  "<" @punctuation.bracket
  (Name) @tag
  "/>" @punctuation.bracket)

; Attributes
(Attribute
  (Name) @attribute)

; Attribute values
(AttValue) @string

; Comments
(Comment) @comment

; Processing instructions
(PI
  "<?" @punctuation.bracket
  (PITarget) @keyword
  "?>" @punctuation.bracket)

; CDATA
(CDSect
  "<![CDATA[" @punctuation.special
  "]]]>" @punctuation.special) @string.special

; DOCTYPE declaration
(doctypedecl
  "<!DOCTYPE" @keyword
  ">" @punctuation.bracket)

; Entity references
(EntityRef
  "&" @punctuation.special
  (Name) @string.special
  ";" @punctuation.special)

(CharRef) @number

; XML declaration
(XMLDecl
  "<?xml" @keyword
  "?>" @punctuation.bracket)

; Namespace prefixes
(NSAttName
  (PrefixedAttName
    (NCName) @attribute))

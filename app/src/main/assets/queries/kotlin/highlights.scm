; Keywords
[
  "as"
  "as?"
  "break"
  "class"
  "continue"
  "do"
  "else"
  "false"
  "for"
  "fun"
  "if"
  "in"
  "!in"
  "interface"
  "is"
  "!is"
  "null"
  "object"
  "package"
  "return"
  "super"
  "this"
  "throw"
  "true"
  "try"
  "typealias"
  "val"
  "var"
  "when"
  "while"
  "by"
  "catch"
  "constructor"
  "delegate"
  "dynamic"
  "field"
  "file"
  "finally"
  "get"
  "import"
  "init"
  "param"
  "property"
  "receiver"
  "set"
  "setparam"
  "where"
  "actual"
  "abstract"
  "annotation"
  "companion"
  "const"
  "crossinline"
  "data"
  "enum"
  "expect"
  "external"
  "final"
  "infix"
  "inline"
  "inner"
  "internal"
  "lateinit"
  "noinline"
  "open"
  "operator"
  "out"
  "override"
  "private"
  "protected"
  "public"
  "reified"
  "sealed"
  "suspend"
  "tailrec"
  "vararg"
] @keyword

; Comments
(multiline_comment) @comment
(line_comment) @comment

; Strings
(string_literal) @string
(character_literal) @string

; Numbers
[
  (integer_literal)
  (long_literal)
  (real_literal)
  (bin_literal)
  (hex_literal)
] @number

; Function definitions
(function_declaration
  (simple_identifier) @function)

(function_declaration
  receiver_type: (_)
  (simple_identifier) @function.method)

; Function calls
(call_expression
  (simple_identifier) @function)

(call_expression
  (navigation_expression
    (simple_identifier) @function.method))

; Class definitions
(class_declaration
  (type_identifier) @type)

(object_declaration
  (type_identifier) @type)

(interface_declaration
  (type_identifier) @type)

; Type references
(type_identifier) @type

; Variables
(variable_declaration
  (simple_identifier) @variable)

(multiline_lambda_parameter
  (simple_identifier) @variable)

; Parameters
(parameter
  (simple_identifier) @variable.builtin)

; Properties
(navigation_suffix
  (simple_identifier) @property)

; Operators
[
  "+"
  "-"
  "*"
  "/"
  "%"
  "="
  "+="
  "-="
  "*="
  "/="
  "%="
  "=="
  "!="
  "<"
  ">"
  "<="
  ">="
  "&&"
  "||"
  "!"
  "!!"
  "++"
  "--"
  ".."
  "?:"
  "->"
  "=>"
  "::"
] @operator

; Brackets / Punctuation
[
  "("
  ")"
  "["
  "]"
  "{"
  "}"
] @punctuation.bracket

[
  "."
  ","
  ";"
  ":"
] @punctuation.delimiter

; Annotations
(annotation
  (use_site_target) @attribute
  "@" @attribute)

(annotation
  "@" @attribute
  (simple_identifier) @attribute)

; Labels
(label) @attribute

; String interpolation
(string_template_expression
  "$" @punctuation.special)

; Escape sequences
(character_escape_seq) @string.special

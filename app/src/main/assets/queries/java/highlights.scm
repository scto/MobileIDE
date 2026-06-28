; Keywords
[
  "abstract"
  "assert"
  "break"
  "case"
  "catch"
  "class"
  "continue"
  "default"
  "do"
  "else"
  "enum"
  "extends"
  "final"
  "finally"
  "for"
  "if"
  "implements"
  "import"
  "instanceof"
  "interface"
  "new"
  "package"
  "private"
  "protected"
  "public"
  "return"
  "static"
  "super"
  "switch"
  "synchronized"
  "this"
  "throw"
  "throws"
  "transient"
  "try"
  "volatile"
  "while"
  "record"
  "sealed"
  "permits"
  "yield"
] @keyword

; Primitive types
[
  "void"
  "byte"
  "short"
  "char"
  "int"
  "long"
  "float"
  "double"
  "boolean"
] @keyword

; Boolean literals and null
[
  (true)
  (false)
  (null_literal)
] @constant.builtin

; Comments
(line_comment) @comment
(block_comment) @comment

; Strings
(string_literal) @string
(character_literal) @string
(text_block) @string

; Numbers
[
  (decimal_integer_literal)
  (hex_integer_literal)
  (octal_integer_literal)
  (binary_integer_literal)
  (decimal_floating_point_literal)
  (hex_floating_point_literal)
] @number

; Method/Function declarations
(method_declaration
  name: (identifier) @function)

(constructor_declaration
  name: (identifier) @constructor)

; Method calls
(method_invocation
  name: (identifier) @function.method)

(method_invocation
  object: (_)
  name: (identifier) @function.method)

; Class/Interface/Enum declarations
(class_declaration
  name: (identifier) @type)

(interface_declaration
  name: (identifier) @type)

(enum_declaration
  name: (identifier) @type)

(record_declaration
  name: (identifier) @type)

; Annotations
(annotation
  name: (identifier) @attribute)

(marker_annotation
  name: (identifier) @attribute)

"@" @attribute

; Type identifiers
(type_identifier) @type

; Variables
(variable_declarator
  name: (identifier) @variable)

(field_declaration
  declarator: (variable_declarator
    name: (identifier) @property))

(formal_parameter
  name: (identifier) @variable.builtin)

; Field access
(field_access
  field: (identifier) @property)

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
  "++"
  "--"
  "&"
  "|"
  "^"
  "~"
  "<<"
  ">>"
  ">>>"
  "&="
  "|="
  "^="
  "<<="
  ">>="
  ">>>="
  "->"
  "::"
  "?"
  ":"
] @operator

; Brackets
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
] @punctuation.delimiter

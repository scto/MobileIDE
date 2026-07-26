; Keywords
[
  "False"
  "None"
  "True"
  "and"
  "as"
  "assert"
  "async"
  "await"
  "break"
  "class"
  "continue"
  "def"
  "del"
  "elif"
  "else"
  "except"
  "exec"
  "finally"
  "for"
  "from"
  "global"
  "if"
  "import"
  "in"
  "is"
  "lambda"
  "nonlocal"
  "not"
  "or"
  "pass"
  "print"
  "raise"
  "return"
  "try"
  "while"
  "with"
  "yield"
  "match"
  "case"
  "type"
] @keyword

; Comments
(comment) @comment

; Strings
[
  (string)
  (concatenated_string)
  (interpolated_string_expression)
] @string

; Numbers
[
  (integer)
  (float)
] @number

; Function definitions
(function_definition
  name: (identifier) @function)

; Method definitions
(class_definition
  body: (block
    (function_definition
      name: (identifier) @function.method)))

; Function calls
(call
  function: (identifier) @function)

(call
  function: (attribute
    attribute: (identifier) @function.method))

; Class definitions
(class_definition
  name: (identifier) @type)

; Decorators
(decorator
  "@" @attribute
  (identifier) @attribute)

(decorator
  "@" @attribute
  (attribute
    attribute: (identifier) @attribute))

; Types in annotations
(type
  (identifier) @type)

; Parameters
(parameters
  (identifier) @variable.builtin)

(default_parameter
  name: (identifier) @variable.builtin)

; Variables
(assignment
  left: (identifier) @variable)

; Attributes / properties
(attribute
  attribute: (identifier) @property)

; Operators
[
  "+"
  "-"
  "*"
  "/"
  "//"
  "%"
  "**"
  "="
  "+="
  "-="
  "*="
  "/="
  "//="
  "%="
  "**="
  "=="
  "!="
  "<"
  ">"
  "<="
  ">="
  "&"
  "|"
  "^"
  "~"
  "<<"
  ">>"
  "&="
  "|="
  "^="
  "<<="
  ">>="
  "->"
  "@"
  "@="
  ":="
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
  ":"
  ";"
] @punctuation.delimiter

; Special built-ins
((identifier) @variable.builtin
 (#match? @variable.builtin "^(self|cls|super)$"))

((identifier) @function.builtin
 (#match? @function.builtin "^(print|len|range|type|isinstance|issubclass|hasattr|getattr|setattr|delattr|callable|iter|next|map|filter|zip|enumerate|sorted|reversed|sum|min|max|abs|round|int|float|str|bool|list|tuple|set|dict|frozenset|bytes|bytearray|repr|hash|id|input|open|format|vars|dir|globals|locals|eval|exec|compile|__import__)$"))

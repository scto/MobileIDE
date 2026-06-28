; Logcat log levels

; Verbose - least important
((log_level) @comment
 (#match? @log_level "^V$"))

; Debug
((log_level) @function
 (#match? @log_level "^D$"))

; Info
((log_level) @string
 (#match? @log_level "^I$"))

; Warning
((log_level) @number
 (#match? @log_level "^W$"))

; Error - most important
((log_level) @keyword
 (#match? @log_level "^E$"))

; Fatal / Assert
((log_level) @operator
 (#match? @log_level "^[FA]$"))

; Timestamps
(timestamp) @constant.builtin

; Process ID, Thread ID
(pid) @number
(tid) @number

; Tag (component name)
(tag) @attribute

; Log message body
(message) @string

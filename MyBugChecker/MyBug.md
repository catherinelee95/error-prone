Useless increment in return statement

Developers may sometimes perform mathematical operations in a return statement. However, if the post-increment (e.g. x++) or post-decrement (e.g. x--) operator is used during a return statement, the variable would remain unchanged. The post operator would not be executed. That is, the statement [return x++] would be equivalent to [return x]. This bug may not be apparent to developers initially, but can cause unwanted behaviour.

This bug is also referenced by the Spotbugs [https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html] as “DLS: Useless increment in return statement”.

We cover all valid cases of a post-increment and post-decrement being used in a return statement. As post-increment/decrements can only be applied to integers stored in variables, we only cover such cases. 

Valid instances of a ‘useless increment in return statement’ bug:
(1) A single variable being post incremented/decremented (e.g. x++ or x--)
(2) A variable being post incremented/decremented within a more complex expression(e.g. (x++ - y) or (y + x++))
(3) A non-integer numeric datatype being type casted into an integer, and then is involved in either case (1) or (2)
(4) Any of the above cases involving parentheses. 

Invalid instances of the return statement bug:
(1) A return statement returning a string that contains  a post-increment/decrement (e.g. return “x++”)
(2) Any return statement that does not contain any post-increment/decrement.

Uncompilable instances (that are also invalid instances of our bug):
(1) A variable storing a non-integer datatype being post-incremented/decremented (e.g. (float) x++)
(2) The result of a method being post-incremented/decremented
(3) An integer not stored in a variable being post-incremented/decremented

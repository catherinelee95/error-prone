Our Choice:

We chose to implement our bug pattern on Error Prone. Between the three open source projects, we considered firstly the popularity of the projects and how friendly the project was for the developers on our team. Our development was done on MacOS and Windows 10. 

To determine the relative popularity of the projects, we checked the number of stars that each of the repos had. Soot had at about 1000 stars, making it the least popular repo compared to Error Prone with around 4000 stars and Infer with around 9000. 

Infer was clearly the most popular choice among developers. However, it was not compatible with a Windows environment, so we selected the next most popular option, Error Prone. This ensured that our tool was easy for both team members to contribute to and is relevant to developers.

How we Implemented:

Conceptually, we use the framework to create an AST of the given code, and then identify the return statements of all methods that returned an integer. The AST would also allow us to extract the contents of the return statement, so we can use this to determine if a postfix increment/decrement was present.

To implement our pattern on Error Prone, we followed existing conventions and extended the BugChecker class.Then, we used the matcher provided to filter out any methods that did not return an integer. Using the abstract syntax tree that Error Prone produced, we extracted the expression from the return statement of the method. If the expression was a unary expression, we would check if it is of type “POSTFIX_INCREMENT” or “POSTFIX_DECREMENT”, as this would indicate that a variable is being post-incremented/decremented at a return statement. If the statement was more complex, we would then match the statement against a regex, to identify if a valid variable name contained a postfix increment or decrement.


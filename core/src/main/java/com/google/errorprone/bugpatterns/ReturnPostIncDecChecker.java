package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.contains;

import java.util.List;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;


/**
 * Bug checker to detect usage of {@code return x++;}.
 */
@BugPattern(
  name = "DoNotReturnIncrement",
  summary = "Do not return increment.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE)
public class ReturnPostIncDecChecker extends BugChecker implements MethodTreeMatcher {
	
	
  private static final Matcher<Tree> RETURN_POSTINC = new PostFixIncrementMatcher();
  private static final Matcher<Tree> CONTAINS_RETURN_POSTINC = contains(RETURN_POSTINC);
  
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
     if (!CONTAINS_RETURN_POSTINC.matches(tree.getBody(), state)) {
       return Description.NO_MATCH;
     }
     return describeMatch(tree);
  }
  
  
  /**
   * Matches any Tree that represents return of a postfix increment.
   */
  private static class PostFixIncrementMatcher implements Matcher<Tree> {

    @Override
    public boolean matches(Tree tree, VisitorState state) {
      if (tree instanceof ReturnTree) {
    	System.out.println("This is the OUTPUT: " + ((ReturnTree) tree).getExpression().getKind());
  
        return ((ReturnTree) tree).getExpression().getKind() == Kind.POSTFIX_INCREMENT;
      }
      return false;
    }
  }
}

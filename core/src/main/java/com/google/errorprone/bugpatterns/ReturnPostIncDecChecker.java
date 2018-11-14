package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.contains;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.lang.model.type.TypeKind;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;


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
	  if(!(((PrimitiveTypeTree)tree.getReturnType()).getPrimitiveTypeKind() == TypeKind.INT)) {
		  return Description.NO_MATCH;
	  }
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
			  ExpressionTree expression = ((ReturnTree) tree).getExpression();
			  
			  if(expression instanceof UnaryTree) {
				  return expression.getKind() == Kind.POSTFIX_INCREMENT || expression.getKind() == Kind.POSTFIX_DECREMENT;
				  }
			  
			  return matchPrePostFix(tree);
      }
		  
      return false;
    }
	  
	private ExpressionTree removeTypeCast(ExpressionTree expression) {
		System.out.println(expression.getKind());
		if(expression.getKind() != Kind.TYPE_CAST && expression.getKind() != Kind.PARENTHESIZED) {
			return expression;
		}
	
		if(expression.getKind() == Kind.TYPE_CAST ) {
			return removeTypeCast(((TypeCastTree) expression).getExpression());
		}

		return removeTypeCast(((ParenthesizedTree) expression).getExpression());
		
	}
    
    private boolean matchPrePostFix(Tree tree) {
	    	String returnExpression = ((ReturnTree) tree).getExpression().toString();
		  	Pattern postIncPattern = Pattern.compile("[a-zA-Z0-9$_]+(\\+\\+|\\-\\-)");
		java.util.regex.Matcher matcher = postIncPattern.matcher(returnExpression);
		
		if(matcher.find()) {
	  		return true;
	  	}
		
		return false;
    }
  }
  
 
}

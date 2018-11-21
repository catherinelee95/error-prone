package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.contains;

import java.util.regex.Pattern;

import javax.lang.model.type.TypeKind;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;


/**
 * Bug checker to detect usage of {@code return x++; or @code return x--;}.
 */
@BugPattern(
  name = "DoNotReturnIntIncrementDecrement",
  summary = "Do not return post increment or decrement.",
  category = JDK,
  severity = ERROR,
  tags = StandardTags.LIKELY_ERROR)
public class ReturnPostIncDecChecker extends BugChecker implements MethodTreeMatcher {
	
	private static final Matcher<Tree> RETURN_POST_INC_DEC = new PostIncDecMatcher();
	private static final Matcher<Tree> CONTAINS_RETURN_POST_INC_DEC = contains(RETURN_POST_INC_DEC);

	@Override
	public Description matchMethod(MethodTree tree, VisitorState state) {
		if (tree.getReturnType() instanceof PrimitiveTypeTree
				&& (((PrimitiveTypeTree) tree.getReturnType()).getPrimitiveTypeKind() == TypeKind.INT
						|| ((PrimitiveTypeTree) tree.getReturnType()).getPrimitiveTypeKind() == TypeKind.DOUBLE 
						|| ((PrimitiveTypeTree) tree.getReturnType()).getPrimitiveTypeKind() == TypeKind.FLOAT
						|| ((PrimitiveTypeTree) tree.getReturnType()).getPrimitiveTypeKind() == TypeKind.LONG
						|| ((PrimitiveTypeTree) tree.getReturnType()).getPrimitiveTypeKind() == TypeKind.BYTE
						|| ((PrimitiveTypeTree) tree.getReturnType()).getPrimitiveTypeKind() == TypeKind.SHORT)
				&& CONTAINS_RETURN_POST_INC_DEC.matches(tree.getBody(), state)) {
			return describeMatch(tree);
		}
		
		return Description.NO_MATCH;
	}

	/**
	 * Matches any Tree that represents return of an integer post increment and decrement.
	 */
	private static class PostIncDecMatcher implements Matcher<Tree> {

		@Override
		public boolean matches(Tree tree, VisitorState state) {
			if (tree instanceof ReturnTree) {
				ExpressionTree expression = ((ReturnTree) tree).getExpression();

				if (expression instanceof UnaryTree) {
					return expression.getKind() == Kind.POSTFIX_INCREMENT
							|| expression.getKind() == Kind.POSTFIX_DECREMENT;
				}

				return matchPrePostFix(tree);
			}

			return false;
		}

		private boolean matchPrePostFix(Tree tree) {
			String returnExpression = ((ReturnTree) tree).getExpression().toString();
			Pattern postIncPattern = Pattern.compile("[a-zA-Z0-9$_]+(\\+\\+|\\-\\-)");
			java.util.regex.Matcher matcher = postIncPattern.matcher(returnExpression);

			if (matcher.find()) {
				return true;
			}

			return false;
		}
	}
  
}

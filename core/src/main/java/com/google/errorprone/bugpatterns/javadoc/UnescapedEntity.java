/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getEndPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getStartPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Finds unescaped entities in Javadocs.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "UnescapedEntity",
    summary = "Javadoc is interpreted as HTML, so HTML entities such as &, <, > must be escaped.",
    severity = WARNING,
    tags = STYLE,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class UnescapedEntity extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final ImmutableSet<String> PRE_TAGS = ImmutableSet.of("pre", "code");

  private static final Pattern GENERIC = Pattern.compile("([A-Z][a-zA-Z0-9_]*<[A-Z][A-Za-z, ]+>)");

  private static final Pattern CONTAINS_HTML = Pattern.compile("&(lt|gt|amp);|&#|\\*\\s*@");

  private final boolean reportAll;

  public UnescapedEntity(ErrorProneFlags flags) {
    reportAll = flags.getBoolean("UnescapedEntity:ReportAllUnescapedEntities").orElse(false);
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return handle(Utils.getDocTreePath(state), state);
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    return handle(Utils.getDocTreePath(state), state);
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    return handle(Utils.getDocTreePath(state), state);
  }

  private Description handle(@Nullable DocTreePath path, VisitorState state) {
    if (path == null) {
      return NO_MATCH;
    }
    RangesFinder rangesFinder = new RangesFinder(state);
    rangesFinder.scan(path, null);
    RangeSet<Integer> emittedFixes =
        fixGenerics(path, rangesFinder.preTags, rangesFinder.dontEmitCodeFix, state);
    new EntityChecker(state, rangesFinder.preTags, emittedFixes).scan(path, null);
    return NO_MATCH;
  }

  private RangeSet<Integer> fixGenerics(
      DocTreePath path,
      RangeSet<Integer> preTags,
      RangeSet<Integer> dontEmitCodeFix,
      VisitorState state) {
    RangeSet<Integer> emittedFixes = TreeRangeSet.create();
    Comment comment = ((DCDocComment) path.getDocComment()).comment;
    Matcher matcher = GENERIC.matcher(comment.getText());
    while (matcher.find()) {
      Range<Integer> range =
          Range.closed(comment.getSourcePos(matcher.start()), comment.getSourcePos(matcher.end()));
      if (emittedFixes.intersects(range) || dontEmitCodeFix.intersects(range)) {
        continue;
      }
      Range<Integer> containingPre = preTags.rangeContaining(range.lowerEndpoint());
      if (containingPre == null) {
        containingPre = range;
      }
      emittedFixes.add(containingPre);
      state.reportMatch(
          buildDescription(
                  getDiagnosticPosition(containingPre.lowerEndpoint(), state.getPath().getLeaf()))
              .setMessage(
                  "This looks like a type with type parameters. The < and > characters here will "
                      + "be interpreted as HTML, which can be avoided by wrapping it in a "
                      + "{@code } tag.")
              .addFix(wrapInCodeTag(containingPre))
              .build());
    }
    return emittedFixes;
  }

  private static final class RangesFinder extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    /**
     * Ranges of {@code <pre>} and {@code <code>} tags that could reasonably have an inner
     * {@literal @}code tag put inside them to escape HTML.
     */
    private final RangeSet<Integer> preTags = TreeRangeSet.create();

    /**
     * Ranges of source code which should not have an inner {@literal @}code tag. This could be
     * because they're already escaping literals, or because it's a {@code <pre>} block with
     * already-escaped HTML.
     */
    private final RangeSet<Integer> dontEmitCodeFix = TreeRangeSet.create();

    private final Deque<Integer> startPosStack = new ArrayDeque<>();
    private boolean containsAnotherTag = false;

    private RangesFinder(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitStartElement(StartElementTree startTree, Void unused) {
      if (PRE_TAGS.contains(startTree.getName().toString())) {
        startPosStack.offerLast(getEndPosition(startTree, state));
        containsAnotherTag = false;
      }
      return super.visitStartElement(startTree, null);
    }

    @Override
    public Void visitEndElement(EndElementTree endTree, Void unused) {
      if (PRE_TAGS.contains(endTree.getName().toString())) {
        if (!containsAnotherTag) {
          Integer startPos = startPosStack.pollLast();
          if (startPos != null) {
            int endPos = getStartPosition(endTree, state);
            String source = state.getSourceCode().subSequence(startPos, endPos).toString();
            if (CONTAINS_HTML.matcher(source).find()) {
              dontEmitCodeFix.add(Range.closed(startPos, endPos));
            } else {
              preTags.add(Range.closed(startPos, endPos));
            }
          }
        }
        containsAnotherTag = true;
        return super.visitEndElement(endTree, null);
      }
      return super.visitEndElement(endTree, null);
    }

    @Override
    public Void visitLink(LinkTree linkTree, Void unused) {
      excludeFromCodeFixes(linkTree);
      return super.visitLink(linkTree, null);
    }

    @Override
    public Void visitLiteral(LiteralTree literalTree, Void unused) {
      excludeFromCodeFixes(literalTree);
      return super.visitLiteral(literalTree, null);
    }

    @Override
    public Void visitSee(SeeTree seeTree, Void unused) {
      excludeFromCodeFixes(seeTree);
      return super.visitSee(seeTree, null);
    }

    private void excludeFromCodeFixes(DocTree tree) {
      dontEmitCodeFix.add(Range.closed(getStartPosition(tree, state), getEndPosition(tree, state)));
    }
  }

  private final class EntityChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;
    private final RangeSet<Integer> preTags;
    private final RangeSet<Integer> emittedFixes;

    private EntityChecker(
        VisitorState state, RangeSet<Integer> preTags, RangeSet<Integer> emittedFixes) {
      this.state = state;
      this.preTags = preTags;
      this.emittedFixes = emittedFixes;
    }

    @Override
    public Void visitErroneous(ErroneousTree erroneousTree, Void unused) {
      if (erroneousTree.getBody().equals("&")) {
        generateFix("&amp;").ifPresent(state::reportMatch);
        return super.visitErroneous(erroneousTree, null);
      }
      if (erroneousTree.getBody().equals("<")) {
        generateFix("&lt;").ifPresent(state::reportMatch);
        return super.visitErroneous(erroneousTree, null);
      }
      if (erroneousTree.getBody().equals(">")) {
        generateFix("&gt;").ifPresent(state::reportMatch);
        return super.visitErroneous(erroneousTree, null);
      }
      return super.visitErroneous(erroneousTree, null);
    }

    private Optional<Description> generateFix(String replacement) {
      int startPosition = getStartPosition(getCurrentPath().getLeaf(), state);
      if (emittedFixes.contains(startPosition)) {
        // We already emitted a fix surrounding this location.
        return Optional.empty();
      }
      Range<Integer> containingPre = preTags.rangeContaining(startPosition);
      if (containingPre == null) {
        return reportAll ? Optional.of(replacementFix(replacement)) : Optional.empty();
      }
      if (emittedFixes.intersects(containingPre)) {
        return Optional.empty();
      }

      emittedFixes.add(containingPre);

      SuggestedFix fix = wrapInCodeTag(containingPre);
      return Optional.of(
          buildDescription(diagnosticPosition(getCurrentPath(), state))
              .setMessage(
                  "This HTML entity is invalid. Enclosing the code in this <pre>/<code> tag with"
                      + " a {@code } block will force Javadoc to interpret HTML literally.")
              .addFix(fix)
              .build());
    }

    private Description replacementFix(String replacement) {
      return buildDescription(diagnosticPosition(getCurrentPath(), state))
          .addFix(replace(getCurrentPath().getLeaf(), replacement, state))
          .build();
    }

    @Override
    public Void scan(DocTree docTree, Void aVoid) {
      return super.scan(docTree, aVoid);
    }
  }

  private static SuggestedFix wrapInCodeTag(Range<Integer> containingPre) {
    return SuggestedFix.builder()
        .replace(containingPre.lowerEndpoint(), containingPre.lowerEndpoint(), "{@code ")
        .replace(containingPre.upperEndpoint(), containingPre.upperEndpoint(), "}")
        .build();
  }
}

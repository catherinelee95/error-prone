package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


/** Unit tests for {@link ReturnPostIncDecChecker}. */
@RunWith(JUnit4.class)
public class ReturnPostIncDecCheckerTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setup() {
    compilationHelper = CompilationTestHelper.newInstance(ReturnPostIncDecChecker.class, getClass());
  }

  @Test
  public void returnIncrementCheckerPositiveCases() {
    compilationHelper.addSourceFile("ReturnPostIncDecCheckerPositiveCases.java").doTest();
  }

  @Test
  public void returnIncrementCheckerNegativeCases() {
    compilationHelper.addSourceFile("ReturnPostIncDecCheckerNegativeCases.java").doTest();
  }
}

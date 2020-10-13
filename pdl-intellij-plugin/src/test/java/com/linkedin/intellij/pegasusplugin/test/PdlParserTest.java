package com.linkedin.intellij.pegasusplugin.test;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ParsingTestCase;
import com.linkedin.intellij.pegasusplugin.PdlParserDefinition;
import java.io.IOException;


/**
 * Tests the parser against valid .pdl files.
 */
public class PdlParserTest extends ParsingTestCase {
  public PdlParserTest() {
    super("parser/fixtures/", "pdl", false /*lowerCaseFirstLetter*/, new PdlParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/resources";
  }

  public void testMessage() throws Throwable {
    doTest(true);
  }

  public void testFruits() throws Throwable {
    doTest(true);
  }

  public void testWithComplexTypesUnion() throws Throwable {
    doTest(true);
  }

  public void testWithPrimitivesArray() throws Throwable {
    doTest(true);
  }

  public void testWithPackage() throws Throwable {
    doTest(true);
  }

  public void testAnonMap() throws Throwable {
    doTest(true);
  }

  public void testGrammar() throws Throwable {
    doTest(true);
  }

  public void testWithNullMember() throws Throwable {
    doTest(true);
  }

  public void testIncludesBefore() throws Throwable {
    doTest(true);
  }

  public void testIncludesAfter() throws Throwable {
    doTest(true);
  }

  @Override
  protected void checkResult(String targetDataName, PsiFile file) throws IOException {
    // Fail on parse errors
    file.accept(new PsiElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof PsiErrorElement) {
          fail("Error found in file:" + file.getName());
          return;
        }
        element.acceptChildren(this);
      }
    });
    super.checkResult(targetDataName, file);
  }
}

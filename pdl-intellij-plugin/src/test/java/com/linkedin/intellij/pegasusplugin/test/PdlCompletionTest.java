package com.linkedin.intellij.pegasusplugin.test;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.Lookup;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.Assert;


/**
 * Tests the functionality provided by {@link com.linkedin.intellij.pegasusplugin.completion.PdlCompletionContributor}.
 */
public class PdlCompletionTest extends PdlLightTestBase {
  @Override
  protected String getTestDataPath() {
    return "src/test/resources/completion/fixtures/";
  }

  public void testTypeCompletion() {
    myFixture.configureByFiles("Completion.pdl", "DateTime.pdl");
    myFixture.complete(CompletionType.BASIC);
    assertLookupElementStrings("Data", "Date", "DateTime");
  }

  public void testAddImport() {
    myFixture.configureByFiles("AddImport.pdl", "DateTime.pdl");
    myFixture.complete(CompletionType.BASIC);
    myFixture.checkResultByFile("AddImport.pdl.after");
  }

  public void testAddExternalImport() {
    myFixture.configureByFiles("AddExternalImport.pdl");
    myFixture.complete(CompletionType.BASIC);
    myFixture.checkResultByFile("AddExternalImport.pdl.after");
  }

  public void testAddPdscImport() {
    myFixture.configureByFiles("AddPdscImport.pdl");
    myFixture.copyFileToProject("ToImport.pdsc", "org/example/ToImport.pdsc");
    myFixture.complete(CompletionType.BASIC);
    myFixture.checkResultByFile("AddPdscImport.pdl.after");
  }

  public void testAddExternalPdscImport() {
    myFixture.configureByFiles("AddPdscExternalImport.pdl");
    myFixture.complete(CompletionType.BASIC);
    myFixture.checkResultByFile("AddPdscExternalImport.pdl.after");
  }

  public void testFQNCompletion() {
    myFixture.configureByFiles("FQNCompletion.pdl", "DateTime.pdl", "Dummy.pdl");
    myFixture.copyFileToProject("ToImport.pdsc", "org/example/ToImport.pdsc");
    myFixture.completeBasic();
    assertLookupElementStrings("org.", "FQNCompletion", "DateTime", "Dummy", "ExternalPdl",
        "ExternalPdsc", "ToImport", "boolean", "bytes", "double", "float", "int", "long", "null", "string");
    myFixture.type('o');
    assertLookupElementStrings("org.", "boolean", "double", "float", "long");
    myFixture.type('r');
    assertLookupElementStrings("org.");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.completeBasic();
    assertLookupElementStrings("joda.", "example.");
    myFixture.type('e');
    assertLookupElementStrings("example.");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.completeBasic();
    assertLookupElementStrings("extras.", "FQNCompletion", "ExternalPdl", "ExternalPdsc", "ToImport");
    myFixture.type("extras");
    assertLookupElementStrings("extras.");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.completeBasic();
    assertLookupElementStrings("Dummy");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.checkResultByFile("FQNCompletion.pdl.after");
  }

  public void testImportDeclarationCompletionFQN() {
    myFixture.configureByFiles("ImportDeclarationCompletion.pdl", "DateTime.pdl", "Dummy.pdl");
    myFixture.copyFileToProject("ToImport.pdsc", "org/example/ToImport.pdsc");
    myFixture.completeBasic();
    assertLookupElementStrings("org.", "Dummy", "DateTime");
    myFixture.type('o');
    assertLookupElementStrings("org.");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.completeBasic();
    assertLookupElementStrings("joda.", "example.");
    myFixture.type('e');
    assertLookupElementStrings("example.");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.completeBasic();
    assertLookupElementStrings("extras.");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.completeBasic();
    assertLookupElementStrings("Dummy");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.checkResultByFile("ImportDeclarationCompletion.pdl.after");
  }

  public void testImportDeclarationCompletionSimple() {
    myFixture.configureByFiles("ImportDeclarationCompletion.pdl", "DateTime.pdl", "Dummy.pdl");
    myFixture.copyFileToProject("ToImport.pdsc", "org/example/ToImport.pdsc");
    myFixture.completeBasic();
    assertLookupElementStrings("org.", "Dummy", "DateTime");
    myFixture.type("Dummy");
    assertLookupElementStrings("Dummy");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.checkResultByFile("ImportDeclarationCompletion.pdl.after");
  }

  public void testImportTypeConflict() {
    myFixture.configureByFiles("ImportTypeConflict.pdl", "Dummy.pdl");
    myFixture.copyFileToProject("../../common/Dummy.pdl", "org/example/common/Dummy.pdl");
    myFixture.completeBasic();
    assertLookupElementStrings("Dummy", "Dummy");
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.checkResultByFile("ImportTypeConflict.after.pdl");
  }

  public void testExistingImportConflict() {
    myFixture.configureByFiles("ExistingImportConflict.pdl", "Dummy.pdl");
    myFixture.copyFileToProject("../../common/Dummy.pdl", "org/example/common/Dummy.pdl");
    myFixture.completeBasic();
    assertLookupElementStrings("Dummy", "Dummy");
    myFixture.getLookup().setCurrentItem(Objects.requireNonNull(myFixture.getLookupElements())[1]);
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
    myFixture.checkResultByFile("ExistingImportConflict.after.pdl");
  }

  /**
   * Assert that the completion lookup elements are equivalent to the given strings, in no particular order.
   * @param expectedLookupStrings expected lookup element strings
   */
  private void assertLookupElementStrings(String ... expectedLookupStrings) {
    List<String> actualLookupStrings = myFixture.getLookupElementStrings();
    if (actualLookupStrings == null) {
      Assert.fail("Actual set of lookup strings is null, expected: " + Arrays.asList(expectedLookupStrings));
    }
    assertSameElements(actualLookupStrings, expectedLookupStrings);
  }
}

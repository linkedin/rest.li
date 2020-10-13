package com.linkedin.intellij.pegasusplugin.test;

import com.linkedin.intellij.pegasusplugin.inspections.FilenameInspection;
import com.linkedin.intellij.pegasusplugin.inspections.imports.ProhibitedImportInspection;
import com.linkedin.intellij.pegasusplugin.inspections.imports.UnusedImportInspection;
import com.linkedin.intellij.pegasusplugin.inspections.union.HeterogeneouslyAliasedUnionInspection;
import org.junit.Assert;


/**
 * TODO(evwillia): Refactor inspection tests to automatically find before/after files based on inspection & fix names.
 */
public class PdlIntentionsTest extends PdlLightTestBase {
  @Override
  protected String getTestDataPath() {
    return "src/test/resources/intentions/fixtures/";
  }

  public void testUnusedImportQuickFix() {
    myFixture.configureByFiles("OptimizeImports.pdl", "DateTime.pdl");
    myFixture.enableInspections(UnusedImportInspection.class);
    myFixture.doHighlighting();
    myFixture.launchAction(myFixture.findSingleIntention("Optimize Imports"));
    myFixture.checkResultByFile("OptimizeImports.pdl.after");
  }

  public void testHeterogeneouslyAliasedUnionAddQuickFix() {
    myFixture.configureByFiles("HeterogeneouslyAliasedUnion.pdl", "DateTime.pdl");
    myFixture.enableInspections(HeterogeneouslyAliasedUnionInspection.class);
    myFixture.doHighlighting();
    myFixture.launchAction(myFixture.findSingleIntention("Add Missing Aliases"));
    myFixture.checkResultByFile("HeterogeneouslyAliasedUnion.pdl.add.after");
  }

  public void testHeterogeneouslyAliasedUnionRemoveQuickFix() {
    myFixture.configureByFiles("HeterogeneouslyAliasedUnion.pdl", "DateTime.pdl");
    myFixture.enableInspections(HeterogeneouslyAliasedUnionInspection.class);
    myFixture.doHighlighting();
    myFixture.launchAction(myFixture.findSingleIntention("Remove Aliases"));
    myFixture.checkResultByFile("HeterogeneouslyAliasedUnion.pdl.remove.after");
  }

  public void testMismatchedFilenameRenameTypeQuickFix() {
    myFixture.configureByFiles("MismatchedFilename.pdl");
    myFixture.enableInspections(FilenameInspection.class);
    myFixture.doHighlighting();
    myFixture.launchAction(myFixture.findSingleIntention("Rename 'MismatchedFoilgnome' to 'MismatchedFilename'"));
    myFixture.checkResultByFile("MismatchedFilename.pdl.renametype.after");
  }

  public void testMismatchedFilenameRenameFileQuickFix() {
    myFixture.configureByFiles("MismatchedFilename.pdl");
    myFixture.enableInspections(FilenameInspection.class);
    myFixture.doHighlighting();
    myFixture.launchAction(myFixture.findSingleIntention("Rename File"));
    String newName = myFixture.getFile().getVirtualFile().getNameWithoutExtension();
    Assert.assertEquals("MismatchedFoilgnome", newName);
  }

  public void testProhibitedImportRootNamespaceQuickFix() {
    myFixture.configureByFiles("ProhibitedImportRootNamespace.pdl", "Dummy.pdl");
    myFixture.enableInspections(ProhibitedImportInspection.class);
    myFixture.doHighlighting();
    myFixture.launchAction(myFixture.findSingleIntention("Remove import and use fully qualified name"));
    myFixture.checkResultByFile("ProhibitedImportRootNamespace.pdl.after");
  }

  public void testProhibitedImportReferencesLocalQuickFix() {
    myFixture.configureByFiles("ProhibitedImportReferencesLocal.pdl");
    myFixture.enableInspections(ProhibitedImportInspection.class);
    myFixture.doHighlighting();
    myFixture.launchAction(myFixture.findSingleIntention("Remove import and use fully qualified name"));
    myFixture.checkResultByFile("ProhibitedImportReferencesLocal.pdl.after");
  }

  public void testProhibitedImportConflictsLocalQuickFix() {
    myFixture.configureByFiles("ProhibitedImportConflictsLocal.pdl", "Dummy.pdl");
    myFixture.enableInspections(ProhibitedImportInspection.class);
    myFixture.doHighlighting();
    myFixture.launchAction(myFixture.findSingleIntention("Remove import and use fully qualified name"));
    myFixture.checkResultByFile("ProhibitedImportConflictsLocal.pdl.after");
  }
}

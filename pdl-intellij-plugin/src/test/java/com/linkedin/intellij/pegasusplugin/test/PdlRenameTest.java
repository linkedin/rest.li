package com.linkedin.intellij.pegasusplugin.test;

public class PdlRenameTest extends PdlLightTestBase {
  @Override
  protected String getTestDataPath() {
    return "src/test/resources/rename/fixtures/";
  }

  public void testUnresolvedType() {
    myFixture.configureByFile("Rename.pdl");
    myFixture.renameElementAtCaret("NewName");
    myFixture.checkResultByFile("NewName.pdl", "Rename.pdl.after", false);
  }
}

package com.linkedin.intellij.pegasusplugin.test;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;


public class PdlFormatterTest extends PdlLightTestBase {
  @Override
  protected String getTestDataPath() {
    return "src/test/resources/formatter/fixtures/";
  }

  public void testFormat() {
    myFixture.configureByFile("Formatter.pdl");

    new WriteCommandAction.Simple(getProject()) {
      @Override
      protected void run() throws Throwable {
        CodeStyleManager.getInstance(getProject()).reformatText(myFixture.getFile(),
            ContainerUtil.newArrayList(myFixture.getFile().getTextRange()));
      }
    }.execute();

    myFixture.checkResultByFile("Formatter.pdl.after");
  }
}

package com.linkedin.intellij.pegasusplugin.test;

import com.intellij.usageView.UsageInfo;
import java.util.Collection;


public class PdlFindUsagesTest extends PdlLightTestBase {
  @Override
  protected String getTestDataPath() {
    return "src/test/resources/findusages/fixtures/";
  }

  public void testFindUsages() {
    Collection<UsageInfo> usageInfos = myFixture.testFindUsages("Example.pdl", "WithUsages1.pdl");
    assertEquals(2, usageInfos.size());
  }
}

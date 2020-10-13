package com.linkedin.intellij.pegasusplugin.test;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.linkedin.intellij.pegasusplugin.inspections.imports.ProhibitedImportInspection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;


/**
 * Tests the functionality provided by {@link com.linkedin.intellij.pegasusplugin.PdlAnnotator}.
 */
public class PdlAnnotatorTest extends PdlLightTestBase {

  @Override
  protected String getTestDataPath() {
    return "src/test/resources/annotator/fixtures/";
  }

  public void testUnresolvedType() {
    myFixture.configureByFile("UnresolvedType.pdl");
    // checkHighlighting(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings, boolean ignoreExtraHighlighting)
    myFixture.checkHighlighting(false, false, true, true);
    assertNumErrors(1);
  }

  public void testUnresolvedImport() {
    myFixture.configureByFile("UnresolvedImport.pdl");
    myFixture.checkHighlighting(false, false, true, true);
    assertNumErrors(1);
  }

  public void testDeprecated() {
    myFixture.configureByFile("Deprecated.pdl");
    myFixture.checkHighlighting(true, false, true, true);
    assertNumErrors(0);
  }

  public void testDeprecatedReference() {
    myFixture.configureByFiles("DeprecatedReference.pdl", "Deprecated.pdl");
    myFixture.checkHighlighting(true, false, true, true);
    assertNumWarnings(1);
  }

  public void testDeprecatedEnumSymbol() {
    myFixture.configureByFiles("DeprecatedEnumSymbols.pdl");
    myFixture.checkHighlighting(true, false, true, true);
    assertNumErrors(0);
  }

  public void testPdscReference() {
    myFixture.configureByFiles("WithPdscReference.pdl", "PegasusExample.pdsc");
    assertNumErrors(0);
  }

  public void testUnresolvedScopedType() {
    myFixture.configureByFiles("UnresolvedScopedType.pdl");
    myFixture.checkHighlighting(false, false, true, true);
    assertNumErrors(7);
  }

  public void testProhibitedImports() {
    myFixture.configureByFiles("ProhibitedImports.pdl", "Dummy.pdl", "OtherDummy.pdl");
    myFixture.enableInspections(ProhibitedImportInspection.class);
    myFixture.checkHighlighting(false, false, true, true);
    assertNumErrors(6);
  }

  private void assertNumWarnings(int expected) {
    List<String> warningDescriptions = myFixture.doHighlighting()
        .stream()
        .filter(highlightInfo -> highlightInfo.getSeverity().equals(HighlightSeverity.WARNING))
        .map(HighlightInfo::getDescription)
        .collect(Collectors.toList());
    long numWarnings = warningDescriptions.size();
    String message = String.format("Expected %d warning(s) but found %d: \"%s\"",
        expected,
        numWarnings,
        String.join("\", \"", warningDescriptions));
    Assert.assertEquals(message, expected, numWarnings);
  }

  private void assertNumErrors(int expected) {
    List<String> errorDescriptions = myFixture.doHighlighting()
        .stream()
        .filter(highlightInfo -> highlightInfo.getSeverity().equals(HighlightSeverity.ERROR))
        .map(HighlightInfo::getDescription)
        .collect(Collectors.toList());
    long numErrors = errorDescriptions.size();
    String message = String.format("Expected %d error(s) but found %d: \"%s\"",
        expected,
        numErrors,
        String.join("\", \"", errorDescriptions));
    Assert.assertEquals(message, expected, numErrors);
  }
}

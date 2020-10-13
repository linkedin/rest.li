package com.linkedin.intellij.pegasusplugin.test;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.fileTemplates.impl.CustomFileTemplate;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;


/**
 * Test {@link com.linkedin.intellij.pegasusplugin.NewPegasusTypeAction} functionality by validating the individual
 * file templates used.
 */
public class PdlFileTemplateTest extends PdlLightTestBase {
  private static final String TEMPLATE_CATEGORY_NAME = "PDL";
  private static final String TEMPLATE_EXTENSION = "ft";

  private static final File TEMPLATES_DIR = new File("src/main/resources/fileTemplates/internal");
  private static final File FIXTURES_DIR = new File("src/test/resources/filetemplates/fixtures");

  @Override
  protected String getTestDataPath() {
    return "src";
  }

  public void testNewRecord() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("PACKAGE_NAME", "org.example");
    properties.setProperty("NAME", "ExampleRecord");
    doTestTemplate("Record.pdl", properties, "ExampleRecord.pdl.expected");
  }

  public void testNewRecordNoNamespace() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("PACKAGE_NAME", "");
    properties.setProperty("NAME", "ExampleRecordNoNamespace");
    doTestTemplate("Record.pdl", properties, "ExampleRecordNoNamespace.pdl.expected");
  }

  public void testNewEnum() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("PACKAGE_NAME", "org.example");
    properties.setProperty("NAME", "ExampleEnum");
    doTestTemplate("Enum.pdl", properties, "ExampleEnum.pdl.expected");
  }

  public void testNewEnumNoNamespace() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("PACKAGE_NAME", "");
    properties.setProperty("NAME", "ExampleEnumNoNamespace");
    doTestTemplate("Enum.pdl", properties, "ExampleEnumNoNamespace.pdl.expected");
  }

  public void testNewTyperef() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("PACKAGE_NAME", "org.example");
    properties.setProperty("NAME", "ExampleTyperef");
    doTestTemplate("Typeref.pdl", properties, "ExampleTyperef.pdl.expected");
  }

  public void testNewTyperefNoNamespace() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("PACKAGE_NAME", "");
    properties.setProperty("NAME", "ExampleTyperefNoNamespace");
    doTestTemplate("Typeref.pdl", properties, "ExampleTyperefNoNamespace.pdl.expected");
  }

  public void testNewUnion() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("PACKAGE_NAME", "org.example");
    properties.setProperty("NAME", "ExampleUnion");
    doTestTemplate("Union.pdl", properties, "ExampleUnion.pdl.expected");
  }

  public void testNewUnionNoNamespace() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("PACKAGE_NAME", "");
    properties.setProperty("NAME", "ExampleUnionNoNamespace");
    doTestTemplate("Union.pdl", properties, "ExampleUnionNoNamespace.pdl.expected");
  }

  private void doTestTemplate(String recordTemplateFilename, Properties properties, String expectedFilename) throws Exception {
    final File recordTemplateFile = new File(TEMPLATES_DIR, recordTemplateFilename + "." + TEMPLATE_EXTENSION);
    final File expectedFile = new File(FIXTURES_DIR, expectedFilename);

    final FileTemplateManager templateManager = FileTemplateManager.getInstance(getProject());
    ArrayList<FileTemplate> templates = new ArrayList<>(Arrays.asList(templateManager.getAllPatterns()));

    final CustomFileTemplate custom = new CustomFileTemplate(recordTemplateFilename, TEMPLATE_EXTENSION);
    final String includeText = FileUtil.loadFile(recordTemplateFile, FileTemplate.ourEncoding);
    custom.setText(includeText);
    templateManager.setTemplates(TEMPLATE_CATEGORY_NAME, templates);

    String inputString = FileUtil.loadFile(recordTemplateFile, FileTemplate.ourEncoding);
    String expectedString = FileUtil.loadFile(expectedFile, FileTemplate.ourEncoding);
    inputString = StringUtil.convertLineSeparators(inputString);
    expectedString = StringUtil.convertLineSeparators(expectedString);

    final String result = FileTemplateUtil.mergeTemplate(properties, inputString, false);
    assertEquals(expectedString, result);

    List<String>
        attrs = Arrays.asList(FileTemplateUtil.calculateAttributes(inputString, new Properties(), false, getProject()));
    assertTrue(properties.size() - 1 <= attrs.size());
    Enumeration e = properties.propertyNames();
    while (e.hasMoreElements()) {
      String s = (String)e.nextElement();
      assertTrue("Attribute '" + s + "' not found in properties", attrs.contains(s) || FileTemplateManager.PROJECT_NAME_VARIABLE.equals(s));
    }
  }
}

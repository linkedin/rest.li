package com.linkedin.restli.tools.data;

import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.util.FileUtil;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestSchemaFormatTranslator
{
  private static final String FS = File.separator;
  private static final String SOURCE_ROOT = String.join(FS, "src", "test", "resources", "pegasus");
  private static final String EXTERNAL_RESOURCES = String.join(FS, "src", "test", "resources", "external");
  private static final String RESOLVER_DIR = SOURCE_ROOT + File.pathSeparator + EXTERNAL_RESOURCES;

  @DataProvider
  public Object[][] fullClassName()
  {
    final String greetingsAPI = "com.linkedin.greetings.api";
    final String property = "com.linkedin.property";
    final String demo = "com.linkedin.demo";
    return new Object[][]
        {
            { greetingsAPI, "Greeting" },
            { greetingsAPI, "Tone" },
            { greetingsAPI, "ArrayTestRecord" },
            { greetingsAPI, "InlineSchemaTyperef" },
            { greetingsAPI, "IncludeSchema" },
            { property, "FieldValidate" },
            { property, "NestedValidate" },
            { property, "IncludeValidate" },
            { demo, "Request" },
            { demo, "RequestCommon" },
            { demo, "Response" },
            { demo, "ResponseCommon" }
        };
  }

  @Test(dataProvider = "fullClassName")
  public void testTranslatePdscToPdl(String packageName, String className) throws Exception
  {
    String temp = Files.createTempDirectory("restli").toFile().getAbsolutePath();
    SchemaFormatTranslator.main(new String[]{"-o", RESOLVER_DIR, SOURCE_ROOT, temp});
    MultiFormatDataSchemaResolver sourceResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(RESOLVER_DIR);
    MultiFormatDataSchemaResolver translatedResolver =
        MultiFormatDataSchemaResolver.withBuiltinFormats(temp + File.pathSeparator + EXTERNAL_RESOURCES);
    assertSameSchemas(packageName + "." + className, sourceResolver, translatedResolver);
  }

  @Test(dataProvider = "fullClassName")
  public void testTranslatorWorksWithArgFile(String packageName, String className) throws Exception
  {
    File tempDir = Files.createTempDirectory("restli").toFile();
    File argFile = new File(tempDir, "resolverPath");
    Files.write(argFile.toPath(), Collections.singletonList(RESOLVER_DIR));
    SchemaFormatTranslator.main(
        new String[]{"-o", String.format("@%s", argFile.toPath()), SOURCE_ROOT, tempDir.getAbsolutePath()});
    MultiFormatDataSchemaResolver sourceResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(RESOLVER_DIR);
    MultiFormatDataSchemaResolver translatedResolver =
        MultiFormatDataSchemaResolver.withBuiltinFormats(tempDir.getAbsolutePath() + File.pathSeparator + EXTERNAL_RESOURCES);
    assertSameSchemas(packageName + "." + className, sourceResolver, translatedResolver);
  }

  @Test(dataProvider = "fullClassName")
  public void testTranslatePdscFromConvertedPdlInSchema(String packageName, String className) throws Exception
  {
    FileUtil.FileExtensionFilter pdscFilter = new FileUtil.FileExtensionFilter(SchemaParser.FILE_EXTENSION);
    FileUtil.FileExtensionFilter pdlFilter = new FileUtil.FileExtensionFilter(PdlSchemaParser.FILE_EXTENSION);
    // pdsc to pdl, keep source fields ('-o' flag)
    String pdlTemp = Files.createTempDirectory("restli").toFile().getAbsolutePath();
    // Keep original in source root.
    SchemaFormatTranslator.main(new String[]{"-o", RESOLVER_DIR, SOURCE_ROOT, pdlTemp});
    // Source files are not deleted
    List<File> sourceFiles = FileUtil.listFiles(new File(SOURCE_ROOT), pdscFilter);
    Assert.assertTrue(sourceFiles.size() > 0);
    List<File> destFiles = FileUtil.listFiles(new File(pdlTemp), pdlFilter);
    Assert.assertTrue(destFiles.size() > 0);
    // All source files are translated.
    Assert.assertEquals(destFiles.size(), sourceFiles.size());

    // pdl to pdsc, delete source files (no '-o' flag)
    int inputPdlFileCount = destFiles.size();
    String pdscTemp = Files.createTempDirectory("restli").toFile().getAbsolutePath();
    String pdlResolverPath = EXTERNAL_RESOURCES + File.pathSeparator + pdlTemp;
    SchemaFormatTranslator.main(new String[]{"-spdl", "-dpdsc", pdlResolverPath, pdlTemp, pdscTemp});
    destFiles = FileUtil.listFiles(new File(pdscTemp), pdscFilter);
    Assert.assertTrue(destFiles.size() > 0);
    Assert.assertEquals(destFiles.size(), inputPdlFileCount);
    // Source files are deleted.
    Assert.assertTrue(FileUtil.listFiles(new File(pdlTemp), pdlFilter).isEmpty());

    MultiFormatDataSchemaResolver sourceResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(RESOLVER_DIR);
    MultiFormatDataSchemaResolver translatedResolver =
        MultiFormatDataSchemaResolver.withBuiltinFormats(pdscTemp + File.pathSeparator + EXTERNAL_RESOURCES);
    assertSameSchemas(packageName + "." + className, sourceResolver, translatedResolver);
  }

  private void assertSameSchemas(String fullname, MultiFormatDataSchemaResolver sourceResolver,
      MultiFormatDataSchemaResolver translatedResolver)
  {
    StringBuilder translatedErrors = new StringBuilder();
    NamedDataSchema translated = translatedResolver.findDataSchema(fullname, translatedErrors);
    if (translatedErrors.toString().length() > 0)
    {
      Assert.fail("Errors resolving translated schemas: " + translatedErrors.toString());
    }

    StringBuilder sourceErrors = new StringBuilder();
    NamedDataSchema source = sourceResolver.findDataSchema(fullname, sourceErrors);
    if (sourceErrors.toString().length() > 0)
    {
      Assert.fail("Errors resolving source schemas: " + sourceErrors.toString());
    }
    Assert.assertEquals(translated, source,
        "Schemas translation failed. fullname: " + fullname + " source: " + source + " translated: " + translated);
  }
}

package com.linkedin.restli.tools.data;

import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import java.io.File;
import java.nio.file.Files;
import java.util.Objects;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestSchemaFormatTranslator {
  private static final String FS = File.separator;
  private static final String SOURCE_ROOT = String.join(FS, "src", "test", "resources", "pegasus");
  private static final String EXTERNAL_RESOURCES = String.join(FS, "src", "test", "resources", "external");
  private static final String RESOLVER_DIR = SOURCE_ROOT + File.pathSeparator + EXTERNAL_RESOURCES;

  @Test
  public void testTranslatePdscToPdl() throws Exception {
    String temp = Files.createTempDirectory("restli").toFile().getAbsolutePath();
    SchemaFormatTranslator.main(new String[]{RESOLVER_DIR, SOURCE_ROOT, temp});
    MultiFormatDataSchemaResolver sourceResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(RESOLVER_DIR);
    MultiFormatDataSchemaResolver translatedResolver =
        MultiFormatDataSchemaResolver.withBuiltinFormats(temp + File.pathSeparator + EXTERNAL_RESOURCES);
    assertSameSchemas("com.linkedin.greetings.api.Greeting", sourceResolver, translatedResolver);
    assertSameSchemas("com.linkedin.greetings.api.Tone", sourceResolver, translatedResolver);
    assertSameSchemas("com.linkedin.greetings.api.ArrayTestRecord", sourceResolver, translatedResolver);
    assertSameSchemas("com.linkedin.greetings.api.InlineSchemaTyperef", sourceResolver, translatedResolver);
    assertSameSchemas("com.linkedin.greetings.api.IncludeSchema", sourceResolver, translatedResolver);
  }

  @Test
  public void testTranslatePdscFromTranslatedPdl() throws Exception {
    // pdsc to pdl
    String pdlTemp = Files.createTempDirectory("restli").toFile().getAbsolutePath();
    SchemaFormatTranslator.main(new String[]{RESOLVER_DIR, SOURCE_ROOT, pdlTemp});
    assertTrue(Objects.requireNonNull(new File(pdlTemp).listFiles()).length > 0);

    // pdl to pdsc
    String pdscTemp = Files.createTempDirectory("restli").toFile().getAbsolutePath();
    SchemaFormatTranslator.main(new String[]{"-spdl", "-dpdsc", RESOLVER_DIR, pdlTemp, pdscTemp});
    assertTrue(Objects.requireNonNull(new File(pdscTemp).listFiles()).length > 0);

    MultiFormatDataSchemaResolver sourceResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(RESOLVER_DIR);
    MultiFormatDataSchemaResolver translatedResolver =
        MultiFormatDataSchemaResolver.withBuiltinFormats(pdscTemp + File.pathSeparator + EXTERNAL_RESOURCES);
    assertSameSchemas("com.linkedin.greetings.api.InlineSchemaTyperef", sourceResolver, translatedResolver);
    assertSameSchemas("com.linkedin.greetings.api.IncludeSchema", sourceResolver, translatedResolver);
  }

  private void assertSameSchemas(String fullname, MultiFormatDataSchemaResolver sourceResolver,
      MultiFormatDataSchemaResolver translatedResolver) {
    StringBuilder translatedErrors = new StringBuilder();
    NamedDataSchema translated = translatedResolver.findDataSchema(fullname, translatedErrors);
    if (translatedErrors.toString().length() > 0) {
      fail("Errors resolving translated schemas: " + translatedErrors.toString());
    }

    StringBuilder sourceErrors = new StringBuilder();
    NamedDataSchema source = sourceResolver.findDataSchema(fullname, sourceErrors);
    if (sourceErrors.toString().length() > 0) {
      fail("Errors resolving source schemas: " + sourceErrors.toString());
    }
    assertEquals(translated, source,
        "Schemas translation failed. fullname: " + fullname + " source: " + source + " translated: " + translated);
  }
}

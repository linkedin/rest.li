package com.linkedin.restli.tools.data;

import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import java.io.File;
import java.nio.file.Files;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestSchemaFormatTranslator {
  private static final String FS = File.separator;
  private static final String RESOLVER_DIR = "src" + FS + "test" + FS + "resources" + FS + "pegasus";

  @Test
  public void testTranslatePdscToPdl() throws Exception {
    String temp = Files.createTempDirectory("restli").toFile().getAbsolutePath();
    SchemaFormatTranslator.main(new String[] {RESOLVER_DIR, RESOLVER_DIR, temp});
    MultiFormatDataSchemaResolver sourceResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(RESOLVER_DIR);
    MultiFormatDataSchemaResolver translatedResolver = MultiFormatDataSchemaResolver.withBuiltinFormats(temp);
    assertSameSchemas("com.linkedin.greetings.api.Greeting", sourceResolver, translatedResolver);
    assertSameSchemas("com.linkedin.greetings.api.Tone", sourceResolver, translatedResolver);
    assertSameSchemas("com.linkedin.greetings.api.ArrayTestRecord", sourceResolver, translatedResolver);
    assertSameSchemas("com.linkedin.greetings.api.InlineSchemaTyperef", sourceResolver, translatedResolver);
  }

  private void assertSameSchemas(String fullname, MultiFormatDataSchemaResolver sourceResolver, MultiFormatDataSchemaResolver translatedResolver) {
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
    assertEquals(source, translated, "Schemas translation failed. fullname: " + fullname + " source: " + source + " translated: " + translated);
  }
}

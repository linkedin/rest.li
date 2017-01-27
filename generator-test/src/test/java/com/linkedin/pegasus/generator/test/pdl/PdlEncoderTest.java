package com.linkedin.pegasus.generator.test.pdl;

import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaToPdlEncoder;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PdlEncoderTest extends GeneratorTest
{
  private final File pegasusSrcDir = new File(System.getProperty("testDir") + "/pegasus");
  private final DataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(pegasusSrcDir.getAbsolutePath());

  /**
   * Validate {@link SchemaToPdlEncoder} by parsing a variety of .pdl files, encoding them back to source, and
   * verifying that the re-encoded source matches the original file.
   */
  @Test
  public void testEncode() throws IOException
  {
    assertRoundTrip("arrays.WithPrimitivesArray");
    assertRoundTrip("denormalized.WithNamespacedDeclarations");
    assertRoundTrip("denormalized.WithIncludeDeclaration");
    assertRoundTrip("deprecated.DeprecatedRecord");
    assertRoundTrip("enums.Fruits");
    assertRoundTrip("enums.EnumProperties");
    assertRoundTrip("enums.DeprecatedSymbols");
    assertRoundTrip("escaping.PdlKeywordEscaping");
    assertRoundTrip("fixed.Fixed8");
    assertRoundTrip("maps.WithPrimitivesMap");
    assertRoundTrip("records.Note");
    assertRoundTrip("records.WithInclude");
    assertRoundTrip("records.WithInlineRecord");
    assertRoundTrip("records.WithPrimitives");
    assertRoundTrip("records.WithOptionalPrimitives");
    assertRoundTrip("records.NumericDefaults");
    assertRoundTrip("records.WithComplexTypeDefaults");
    assertRoundTrip("typerefs.UnionWithInlineRecord");
    assertRoundTrip("typerefs.MapTyperef");
    assertRoundTrip("typerefs.IntTyperef");
    assertRoundTrip("unions.WithPrimitivesUnion");
  }

  private NamedDataSchema parseSchema(String name) throws IOException
  {
    StringBuilder errors = new StringBuilder();
    NamedDataSchema dataSchema = resolver.findDataSchema(name, errors);
    if (errors.length() > 0)
    {
      fail("Parse error: " + errors.toString());
    }
    return dataSchema;
  }

  private void assertRoundTrip(String relativeName) throws IOException
  {
    String fullName = "com.linkedin.pegasus.generator.test.idl." + relativeName;
    String path = "/" + fullName.replace('.', '/') + ".pdl";

    NamedDataSchema parsed = parseSchema(fullName);
    String original = loadSchema(path);
    assertNotNull(parsed, "Failed to resolve: " + fullName + "resolver path: " + pegasusSrcDir.getAbsolutePath());

    StringWriter writer = new StringWriter();
    SchemaToPdlEncoder encoder = new SchemaToPdlEncoder(writer);
    encoder.setTypeReferenceFormat(SchemaToPdlEncoder.TypeReferenceFormat.PRESERVE);
    encoder.encode(parsed);
    String encoded = writer.toString();
    assertEqualsIgnoringSpacing(original, encoded);
  }

  private void assertEqualsIgnoringSpacing(String lhs, String rhs)
  {
    assertEquals(canonicalize(lhs), canonicalize(rhs));
  }

  private String loadSchema(String filename)
  {
    try
    {
      return FileUtils.readFileToString(new File(pegasusSrcDir, filename));
    }
    catch (IOException e)
    {
      fail("Failed to load file: " + filename + ": " + e.getMessage());
      return null;
    }
  }

  private String canonicalize(String pdlSource)
  {
    return pdlSource
        .replaceAll("([{}\\[\\]\\?=:])", " $1 ") // force spacing around grammatical symbols
        .replaceAll(",", " ") // commas are insignificant in pdl, strip them out
        .replaceAll("\\s+", " ").trim(); // canonicalize spacing
  }
}

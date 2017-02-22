package com.linkedin.pegasus.generator.test.pdl;

import com.linkedin.data.schema.AbstractSchemaParser;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaToPdlEncoder;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import java.io.File;
import java.io.FileInputStream;
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
    assertRoundTrip("arrays.AnonArray");
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

  private DataSchema parseSchema(File file) throws IOException
  {
    AbstractSchemaParser parser = new PdlSchemaParser(resolver);
    parser.parse(new FileInputStream(file));
    StringBuilder errorMessageBuilder = parser.errorMessageBuilder();
    if (errorMessageBuilder.length() > 0)
    {
      fail(
          "Failed to parse schema: " + file.getAbsolutePath() + "\nerrors: " + errorMessageBuilder.toString());
      System.exit(1);
    }
    if (parser.topLevelDataSchemas().size() != 1)
    {
      fail(
          "Failed to parse any schemas from: " + file.getAbsolutePath() + "\nerrors: " + errorMessageBuilder.toString());
      System.exit(1);
    }
    return parser.topLevelDataSchemas().get(0);
  }

  private void assertRoundTrip(String relativeName) throws IOException
  {
    String fullName = "com.linkedin.pegasus.generator.test.idl." + relativeName;
    File file = new File(pegasusSrcDir, "/" + fullName.replace('.', '/') + ".pdl");

    DataSchema parsed = parseSchema(file);
    String original = loadSchema(file);
    assertNotNull(parsed, "Failed to resolve: " + fullName + " resolver path: " + pegasusSrcDir.getAbsolutePath());

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

  private String loadSchema(File file)
  {
    try
    {
      return FileUtils.readFileToString(file);
    }
    catch (IOException e)
    {
      fail("Failed to load file: " + file + ": " + e.getMessage());
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

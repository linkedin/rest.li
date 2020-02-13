/*
   Copyright (c) 2017 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.pegasus.generator.test.pdl;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.AbstractSchemaEncoder.TypeReferenceFormat;
import com.linkedin.data.schema.AbstractSchemaParser;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.SchemaToPdlEncoder;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.pegasus.generator.test.idl.EncodingStyle;
import com.linkedin.pegasus.generator.test.idl.PdlEncoderTestConfig;
import com.linkedin.pegasus.generator.test.idl.ReferenceFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests {@link SchemaToPdlEncoder} by parsing a .pdl file then encoding it back to .pdl and comparing the two.
 * Furthermore, it parses the resulting encoded .pdl text to test that different encoding styles
 * ({@link com.linkedin.data.schema.SchemaToPdlEncoder.EncodingStyle}) produce correct schemas.
 */
public class PdlEncoderTest extends GeneratorTest
{
  private final File pegasusSrcDir = new File(System.getProperty("testDir", "src/test") + "/pegasus");

  @DataProvider(name = "pdlFilePaths")
  private Object[][] providePdlFilePaths()
  {
    return new Object[][]
        {
            { "arrays.AnonArray" },
            { "arrays.WithPrimitivesArray" },
            { "denormalized.WithNamespacedDeclarations" },
            { "denormalized.WithIncludeDeclaration" },
            { "deprecated.DeprecatedRecord" },
            { "enums.Fruits" },
            { "enums.EnumProperties" },
            { "enums.EscapedSymbols" },
            { "enums.DeprecatedSymbols" },
            { "enums.WithAliases" },
            { "escaping.record.NamespacePackageEscaping" },
            { "escaping.PdlKeywordEscaping" },
            { "escaping.PropertyKeyEscaping" },
            { "fixed.Fixed8" },
            { "fixed.WithAliases" },
            { "imports.ConflictResolution" },
            { "imports.NamespaceOverrides" },
            { "imports.Includes" },
            { "imports.InlineTypeConflict" },
            { "maps.WithOrders" },
            { "maps.WithPrimitivesMap" },
            { "records.EmptyNamespace" },
            { "records.Note" },
            { "records.RecursivelyDefinedRecord" },
            { "records.WithAliases" },
            { "records.WithInclude" },
            { "records.WithIncludeAfter" },
            { "records.WithPrimitiveDefaults" },
            { "records.WithInlineRecord" },
            { "records.WithPrimitives" },
            { "records.WithOptionalPrimitiveDefaults" },
            { "records.WithOptionalPrimitives" },
            { "records.NumericDefaults" },
            { "records.WithComplexTypeDefaults" },
            { "typerefs.UnionWithInlineRecord" },
            { "typerefs.MapTyperef" },
            { "typerefs.IntTyperef" },
            { "typerefs.WithAliases" },
            { "unions.WithPrimitivesUnion" },
            { "unions.WithAliases" }
        };
  }

  /**
   * Validate {@link SchemaToPdlEncoder} by parsing a variety of .pdl files, encoding them back to source, and
   * verifying that the re-encoded source matches the original file.
   */
  @Test(dataProvider = "pdlFilePaths")
  public void testEncode(String pdlFilePath) throws IOException
  {
    assertRoundTrip(pdlFilePath);
  }

  private void assertRoundTrip(String relativeName) throws IOException
  {
    String fullName = "com.linkedin.pegasus.generator.test.idl." + relativeName;
    File file = new File(pegasusSrcDir, "/" + fullName.replace('.', '/') + ".pdl");

    DataSchema parsed = parseSchema(file);
    String original = loadSchema(file);
    Assert.assertNotNull(parsed, "Failed to resolve: " + fullName + " resolver path: " + pegasusSrcDir.getAbsolutePath());

    // Read the test config (if any) from the schema
    DataMap testConfigDataMap = (DataMap) parsed.getProperties().getOrDefault("testConfig", new DataMap());
    PdlEncoderTestConfig testConfig = new PdlEncoderTestConfig(testConfigDataMap);

    // Encode and compare for each type reference format (just PRESERVE by default)
    for (ReferenceFormat referenceFormat : testConfig.getReferenceFormats())
    {
      // Do this as well for each encoding style (all by default)
      for (EncodingStyle encodingStyle : testConfig.getEncodingStyles())
      {
        StringWriter writer = new StringWriter();
        SchemaToPdlEncoder encoder = new SchemaToPdlEncoder(writer);
        encoder.setTypeReferenceFormat(TypeReferenceFormat.valueOf(referenceFormat.name()));
        encoder.setEncodingStyle(SchemaToPdlEncoder.EncodingStyle.valueOf(encodingStyle.name()));
        encoder.encode(parsed);
        String encoded = writer.toString();

        // Only test against original text for indented encoding since original text uses this style
        if (encodingStyle == EncodingStyle.INDENTED)
        {
          assertEqualsIgnoringSpacing(encoded, original,
              "Encoded schema doesn't match original for type reference format: \"" + referenceFormat + "\".");
        }

        // Parse the newly encoded PDL text to ensure the encoding style didn't produce an invalid schema
        DataSchema parsedAgain = parseSchema(encoded, relativeName);
        Assert.assertEquals(parsedAgain, parsed,
            "Encoding using the \"" + encodingStyle + "\" style resulted in a different/invalid schema.");
      }
    }
  }

  private DataSchema parseSchema(File file) throws IOException
  {
    DataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(pegasusSrcDir.getAbsolutePath());
    AbstractSchemaParser parser = new PdlSchemaParser(resolver);
    parser.parse(new FileInputStream(file));
    return extractSchema(parser, file.getAbsolutePath());
  }

  private DataSchema parseSchema(String text, String name) throws IOException
  {
    DataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(pegasusSrcDir.getAbsolutePath());
    AbstractSchemaParser parser = new PdlSchemaParser(resolver);
    parser.parse(text);
    return extractSchema(parser, name);
  }

  private DataSchema extractSchema(AbstractSchemaParser parser, String name)
  {
    StringBuilder errorMessageBuilder = parser.errorMessageBuilder();
    if (errorMessageBuilder.length() > 0)
    {
      Assert.fail("Failed to parse schema: " + name + "\nerrors: " + errorMessageBuilder.toString());
    }
    if (parser.topLevelDataSchemas().size() != 1)
    {
      Assert.fail("Failed to parse any schemas from: " + name + "\nerrors: " + errorMessageBuilder.toString());
    }
    return parser.topLevelDataSchemas().get(0);
  }

  private void assertEqualsIgnoringSpacing(String lhs, String rhs, String message)
  {
    Assert.assertEquals(canonicalize(lhs), canonicalize(rhs), message);
  }

  private String loadSchema(File file)
  {
    try
    {
      return FileUtils.readFileToString(file);
    }
    catch (IOException e)
    {
      Assert.fail("Failed to load file: " + file + ": " + e.getMessage());
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

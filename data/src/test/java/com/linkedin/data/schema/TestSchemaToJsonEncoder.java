package com.linkedin.data.schema;

import java.io.IOException;
import org.testng.annotations.Test;

import static org.junit.Assert.*;


public class TestSchemaToJsonEncoder {

  @Test
  public void testEncodeWithPreserve() throws IOException {
    SchemaParser parser = new SchemaParser();
    String commonSchemaJson =
        "{ \"type\": \"record\", \"name\": \"ReferencedFieldType\", \"namespace\": \"com.linkedin.common\", \"fields\" : []}";
    parser.parse(commonSchemaJson);

    String originalSchemaJsonOne = "{ " + "  \"type\": \"record\"," + "  \"name\": \"OriginalOne\","
        + "  \"namespace\": \"com.linkedin.test.data\","
        + "  \"include\": [ \"com.linkedin.common.ReferencedFieldType\" ]," + "  \"fields\" : ["
        + "    {\"name\": \"inlineFieldType\", \"type\": { \"type\": \"record\", \"name\": \"InlineOne\", \"fields\": [] }},"
        + "    {\"name\": \"referencedFieldType\", \"type\": \"com.linkedin.common.ReferencedFieldType\" },"
        + "    {\"name\": \"referencedTyperefType\", \"type\": { \"type\": \"typeref\", \"name\": \"ReferencedTyperef\", \"ref\": \"com.linkedin.common.ReferencedFieldType\" }}"
        + "  ]" + "}";
    parser.parse(originalSchemaJsonOne);

    String originalSchemaJsonTwo = "{ " + "  \"type\": \"record\"," + "  \"name\": \"OriginalTwo\","
        + "  \"namespace\": \"com.linkedin.test.data\","
        + "  \"include\": [ \"com.linkedin.common.ReferencedFieldType\" ]," + "  \"fields\" : ["
        + "    {\"name\": \"inlineFieldType\", \"type\": { \"type\": \"record\", \"name\": \"InlineTwo\", \"fields\": [] }},"
        + "    {\"name\": \"referencedFieldType\", \"type\": \"com.linkedin.common.ReferencedFieldType\" },"
        + "    {\"name\": \"referencedTyperefType\", \"type\": { \"type\": \"typeref\", \"name\": \"ReferencedTyperef\", \"ref\": \"com.linkedin.common.ReferencedFieldType\" }}"
        + "  ]" + "}";
    parser.parse(originalSchemaJsonTwo);

    JsonBuilder originalBuilder = new JsonBuilder(JsonBuilder.Pretty.INDENTED);
    SchemaToJsonEncoder originalEncoder = new SchemaToJsonEncoder(originalBuilder);
    originalEncoder.setTypeReferenceFormat(SchemaToJsonEncoder.TypeReferenceFormat.PRESERVE);
    for (DataSchema schema : parser.topLevelDataSchemas()) {
      originalEncoder.encode(schema);
    }

    String expected = String.join("\n", commonSchemaJson, originalSchemaJsonOne, originalSchemaJsonTwo);
    assertEqualsIgnoringSpacing(originalBuilder.result(), expected);
  }

  private void assertEqualsIgnoringSpacing(String actual, String expected) {
    assertEquals(canonicalize(actual), canonicalize(expected));
  }

  private String canonicalize(String source) {
    return source.replaceAll("([{}\\[\\]?=:])", " $1 ") // force spacing around grammatical symbols
        .replaceAll(",", " ") // commas are insignificant in pdl, strip them out
        .replaceAll("\\s+", " ").trim(); // canonicalize spacing
  }
}

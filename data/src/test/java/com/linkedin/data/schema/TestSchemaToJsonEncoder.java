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

  @Test
  public void testForceFullyQualifiedNames() throws IOException {
    SchemaParser parser = new SchemaParser();
    String commonSchemaJson =
        "{ \"type\": \"record\", \"name\": \"ReferencedFieldType\", \"namespace\": \"com.linkedin.common\", \"fields\" : []}";
    parser.parse(commonSchemaJson);

    String originalSchemaJson = "{ " + "  \"type\": \"record\"," + "  \"name\": \"Original\","
        + "  \"namespace\": \"com.linkedin.common\","
        + "  \"include\": [ \"com.linkedin.common.ReferencedFieldType\" ]," + "  \"fields\" : ["
        + "    {\"name\": \"inlineFieldType\", \"type\": { \"type\": \"record\", \"name\": \"InlineOne\", \"fields\": [] }},"
        + "    {\"name\": \"referencedFieldType\", \"type\": \"com.linkedin.common.ReferencedFieldType\" },"
        + "    {\"name\": \"referencedTyperefType\", \"type\": { \"type\": \"typeref\", \"name\": \"ReferencedTyperef\", \"ref\": \"ReferencedFieldType\" }}"
        + "  ]" + "}";
    parser.parse(originalSchemaJson);
    DataSchema originalSchema = parser.parseObject("com.linkedin.common.Original");

    String generatedSchemaUsingNamespace = "{ " + "  \"type\": \"record\"," + "  \"name\": \"Original\","
        + "  \"namespace\": \"com.linkedin.common\","
        + "  \"include\": [ \"ReferencedFieldType\" ]," + "  \"fields\" : ["
        + "    {\"name\": \"inlineFieldType\", \"type\": { \"type\": \"record\", \"name\": \"InlineOne\", \"fields\": [] }},"
        + "    {\"name\": \"referencedFieldType\", \"type\": \"ReferencedFieldType\" },"
        + "    {\"name\": \"referencedTyperefType\", \"type\": { \"type\": \"typeref\", \"name\": \"ReferencedTyperef\", \"ref\": \"ReferencedFieldType\" }}"
        + "  ]" + "}";

    String generatedSchemaUsingFullyQualifiedNames = "{ " + "  \"type\": \"record\"," + "  \"name\": \"Original\","
        + "  \"namespace\": \"com.linkedin.common\","
        + "  \"include\": [ \"com.linkedin.common.ReferencedFieldType\" ]," + "  \"fields\" : ["
        + "    {\"name\": \"inlineFieldType\", \"type\": { \"type\": \"record\", \"name\": \"InlineOne\", \"fields\": [] }},"
        + "    {\"name\": \"referencedFieldType\", \"type\": \"com.linkedin.common.ReferencedFieldType\" },"
        + "    {\"name\": \"referencedTyperefType\", \"type\": { \"type\": \"typeref\", \"name\": \"ReferencedTyperef\", \"ref\": \"com.linkedin.common.ReferencedFieldType\" }}"
        + "  ]" + "}";

    JsonBuilder jsonBuilder = new JsonBuilder(JsonBuilder.Pretty.INDENTED);
    SchemaToJsonEncoder encoder = new SchemaToJsonEncoder(jsonBuilder);
    encoder.setTypeReferenceFormat(SchemaToJsonEncoder.TypeReferenceFormat.PRESERVE);
    encoder.encode(originalSchema);
    // First encode without forcing fully qualified names, all references matching namespace will use simple names.
    assertEqualsIgnoringSpacing(jsonBuilder.result(), generatedSchemaUsingNamespace);

    jsonBuilder = new JsonBuilder(JsonBuilder.Pretty.INDENTED);
    encoder = new SchemaToJsonEncoder(jsonBuilder);
    encoder.setTypeReferenceFormat(SchemaToJsonEncoder.TypeReferenceFormat.PRESERVE);
    encoder.setAlwaysUseFullyQualifiedName(true);
    encoder.encode(originalSchema);
    // Encode using fully qualified names for all references.
    assertEqualsIgnoringSpacing(jsonBuilder.result(), generatedSchemaUsingFullyQualifiedNames);
  }

  private void assertEqualsIgnoringSpacing(String actual, String expected) {
    assertEquals(canonicalize(actual), canonicalize(expected));
  }

  private String canonicalize(String source) {
    return source.replaceAll("([{}\\[\\]?=:])", " $1 ") // force spacing around grammatical symbols
        .replaceAll("\\s+", " ").trim(); // canonicalize spacing
  }
}

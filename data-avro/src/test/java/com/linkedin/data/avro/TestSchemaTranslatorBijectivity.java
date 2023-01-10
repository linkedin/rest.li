/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.data.avro;

import com.linkedin.data.schema.AbstractSchemaParser;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.avro.Schema;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests whether avroSchema == SchemaTranslator.dataSchemaToAvro(SchemaTranslator.avroToDataSchema(avroSchema))
 * and where it fails.
 */
public class TestSchemaTranslatorBijectivity {
  @Test(dataProvider = "avroPassingSchemas")
  public void testAvroConversion_correctlyConverted(String filePath, String avroRootDir) throws IOException {
    String avroJsonSchema = readFile(new File(filePath));
    Schema initialAvroSchema = new Schema.Parser().parse(avroJsonSchema);
    AvroToDataSchemaTranslationOptions avroToDataSchemaTranslationOptions =
        new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.TRANSLATE).setFileResolutionPaths(
            avroRootDir);
    DataToAvroSchemaTranslationOptions dataToAvroSchemaTranslationOptions =
        new DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty.INDENTED);

    DataSchema pdscSchema = SchemaTranslator.avroToDataSchema(initialAvroSchema, avroToDataSchemaTranslationOptions);
    Schema resultingAvroSchema = SchemaTranslator.dataToAvroSchema(pdscSchema, dataToAvroSchemaTranslationOptions);

    Assert.assertTrue(AvroSchemaEquals.equals(resultingAvroSchema, initialAvroSchema, true, true, true),
        initialAvroSchema + " ---------- " + resultingAvroSchema.toString());
  }

  @Test(dataProvider = "avroFailingSchemas")
  public void testAvroConversion_incorrectlyConverted(String filePath, String avroRootDir) throws IOException {
    String avroJsonSchema = readFile(new File(filePath));
    Schema initialAvroSchema = new Schema.Parser().parse(avroJsonSchema);
    AvroToDataSchemaTranslationOptions avroToDataSchemaTranslationOptions =
        new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.TRANSLATE).setFileResolutionPaths(
            avroRootDir);
    DataToAvroSchemaTranslationOptions dataToAvroSchemaTranslationOptions =
        new DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty.INDENTED);

    Schema resultingAvroSchema = null;
    try {
      DataSchema pdscSchema = SchemaTranslator.avroToDataSchema(initialAvroSchema, avroToDataSchemaTranslationOptions);
      resultingAvroSchema = SchemaTranslator.dataToAvroSchema(pdscSchema, dataToAvroSchemaTranslationOptions);
    } catch (Exception ignored) {
    }
    if (resultingAvroSchema == null || AvroSchemaEquals.equals(resultingAvroSchema, initialAvroSchema, true,
        true, true)) {
      if (!filePath.contains("does" + File.separator + "not" + File.separator + "fail")) {
        Assert.fail("Schema with path " + filePath + " should fail");
      }
    }
  }

  @Test(dataProvider = "pegasusPassingSchemas")
  public void testPegasusConversion_correctlyConverted(String filePath, String pegasusRootDir) throws IOException {
    DataSchema initialPegasusSchema = parseSchema(new File(filePath), new File(pegasusRootDir));
    AvroToDataSchemaTranslationOptions avroToDataSchemaTranslationOptions =
        new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.TRANSLATE).setFileResolutionPaths(
            pegasusRootDir);
    DataToAvroSchemaTranslationOptions dataToAvroSchemaTranslationOptions =
        new DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty.INDENTED);

    Schema avroSchema = SchemaTranslator.dataToAvroSchema(initialPegasusSchema, dataToAvroSchemaTranslationOptions);
    DataSchema resultingPdscSchema = SchemaTranslator.avroToDataSchema(avroSchema, avroToDataSchemaTranslationOptions);

    Assert.assertEquals(resultingPdscSchema.toString(), initialPegasusSchema.toString());
  }

  @Test(dataProvider = "pegasusFailingSchemas")
  public void testPegasusConversion_incorrectlyConverted(String filePath, String pegasusRootDir) throws IOException {
    DataSchema initialPegasusSchema = parseSchema(new File(filePath), new File(pegasusRootDir));
    AvroToDataSchemaTranslationOptions avroToDataSchemaTranslationOptions =
        new AvroToDataSchemaTranslationOptions(AvroToDataSchemaTranslationMode.TRANSLATE).setFileResolutionPaths(
            pegasusRootDir);
    DataToAvroSchemaTranslationOptions dataToAvroSchemaTranslationOptions =
        new DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty.INDENTED);

    DataSchema resultingPdscSchema = null;
    try {
      Schema avroSchema = SchemaTranslator.dataToAvroSchema(initialPegasusSchema, dataToAvroSchemaTranslationOptions);
      resultingPdscSchema = SchemaTranslator.avroToDataSchema(avroSchema, avroToDataSchemaTranslationOptions);
    } catch (Exception ignored) {
    }

    if (resultingPdscSchema == null || resultingPdscSchema.toString().equals(initialPegasusSchema.toString())) {
      if (!filePath.contains("does" + File.separator + "not" + File.separator + "fail")) {
        Assert.fail("Schema with path " + filePath + " should fail");
      }
    }
  }

  @DataProvider
  public Object[][] avroPassingSchemas() {
    return getDataProviderResponse("avro-passing");
  }

  @DataProvider
  public Object[][] avroFailingSchemas() {
    return getDataProviderResponse("avro-failing");
  }

  @DataProvider
  public Object[][] pegasusPassingSchemas() {
    return getDataProviderResponse("pegasus-passing");
  }

  @DataProvider
  public Object[][] pegasusFailingSchemas() {
    return getDataProviderResponse("pegasus-failing");
  }

  /**
   * Gets all schemas for a directory, located in /resources.
   */
  private Object[][] getDataProviderResponse(String childDir) {
    String parentDir = getClass().getClassLoader().getResource("bijectivity-schemas").getFile();
    File rootDir = new File(new File(parentDir).getAbsolutePath(), childDir);
    File[] listOfFiles = rootDir.listFiles();

    ArrayList<Object[]> returnObjectsAsArraylist = new ArrayList<>();
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        returnObjectsAsArraylist.add(new Object[]{listOfFiles[i].getAbsolutePath(), rootDir.getAbsolutePath()});
      }
    }

    Object[][] objectsToReturn = new Object[returnObjectsAsArraylist.size()][2];
    for (int i = 0; i < returnObjectsAsArraylist.size(); i++) {
      objectsToReturn[i] = returnObjectsAsArraylist.get(i);
    }

    return objectsToReturn;
  }

  private DataSchema parseSchema(File file, File srcDir) throws IOException {
    DataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(srcDir.getAbsolutePath());
    AbstractSchemaParser parser = new PdlSchemaParser(resolver);
    parser.parse(new FileInputStream(file));
    return extractSchema(parser, file.getAbsolutePath());
  }

  private DataSchema extractSchema(AbstractSchemaParser parser, String name) {
    StringBuilder errorMessageBuilder = parser.errorMessageBuilder();
    if (errorMessageBuilder.length() > 0) {
      Assert.fail("Failed to parse schema: " + name + "\nerrors: " + errorMessageBuilder);
    }
    if (parser.topLevelDataSchemas().size() != 1) {
      Assert.fail("Failed to parse any schemas from: " + name + "\nerrors: " + errorMessageBuilder);
    }
    return parser.topLevelDataSchemas().get(0);
  }

  private static String readFile(File file) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(file));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line).append("\n");
    }
    return sb.toString();
  }
}

/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.common;

import com.linkedin.data.schema.AbstractSchemaEncoder;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.pegasus.generator.DataSchemaParser;
import com.linkedin.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.reporters.Files;


/**
 * TODO: Remove this and PDSC field in resources permanently once translated PDSCs are no longer needed.
 */
public class TestLegacyPdscEquivalence
{
  private final String pdlSrcDir = System.getProperty("srcDir", "src/main") + "/pegasus";
  private final File legacyPdscDir =
      new File(System.getProperty("resourcesDir", "src/main/resources") + "/legacyPegasusSchemas");

  @Test
  public void testPdscEquivalence() throws IOException
  {
    final Map<String, File> pdscFiles = FileUtil.listFiles(legacyPdscDir,
        (file) -> FilenameUtils.getExtension(file.getName()).equals(SchemaParser.FILETYPE))
        .stream()
        .collect(Collectors.toMap((file) -> FilenameUtils.removeExtension(file.getName()), file -> file));

    DataSchemaParser dataSchemaParser = new DataSchemaParser.Builder(pdlSrcDir).build();
    Map<DataSchema, DataSchemaLocation> schemaLocationMap =
        dataSchemaParser.parseSources(new String[]{pdlSrcDir + "/com/linkedin/restli/common"}).getSchemaAndLocations();
    for (Map.Entry<DataSchema, DataSchemaLocation> schemaEntry : schemaLocationMap.entrySet())
    {
      String pdlFileName = FilenameUtils.removeExtension(schemaEntry.getValue().getSourceFile().getName());
      if (schemaEntry.getKey() instanceof NamedDataSchema && pdlFileName.equals(
          ((NamedDataSchema) schemaEntry.getKey()).getName()))
      {
        JsonBuilder builder = new JsonBuilder(JsonBuilder.Pretty.INDENTED);
        SchemaToJsonEncoder pdscEncoder =
            new SchemaToJsonEncoder(builder, AbstractSchemaEncoder.TypeReferenceFormat.PRESERVE);
        pdscEncoder.encode(schemaEntry.getKey());
        String translatedPdsc = builder.result();
        Assert.assertEquals(translatedPdsc, Files.readFile(
            pdscFiles.get(FilenameUtils.removeExtension(schemaEntry.getValue().getSourceFile().getName()))));
      }
    }
  }
}

/*
   Copyright (c) 2020 LinkedIn Corp.

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
package com.linkedin.restli.tools.snapshot.gen;

import com.linkedin.data.schema.AbstractSchemaEncoder;
import com.linkedin.data.schema.AbstractSchemaParser;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.schema.SchemaToPdlEncoder;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * PegasusSchemaSnapshotExporter, generating pegasus schema snapshot(.pdl) files
 *
 * @author Yingjie Bi
 */
public class PegasusSchemaSnapshotExporter
{
  private static final String PDL = ".pdl";

  private static final String PDSC = ".pdsc";

  /**
   * Generate pegasus schema snapshot(pegasusSchemaSnapshot.pdl) files to the provided output directory
   * based on the given input pegasus schemas.
   *
   * @param resolverPath schema resolver path
   * @param inputPath input files directory
   * @param outputDir output files directory
   * @throws IOException
   */
  public void export(String resolverPath, String inputPath, File outputDir) throws IOException
  {
    List<DataSchema> dataSchemas = parseDataSchema(resolverPath, inputPath);
    for (DataSchema dataSchema : dataSchemas)
    {
      writeSnapshotFile(outputDir, ((NamedDataSchema) dataSchema).getFullName(), dataSchema);
    }
  }

  private static List<DataSchema> parseDataSchema(String resolverPath, String inputPath)
      throws RuntimeException, IOException
  {
    try (Stream<Path> paths = Files.walk(Paths.get(inputPath)))
    {
      return paths
          .filter(path -> path.toString().endsWith(PDL) || path.toString().endsWith(PDSC))
          .map(path ->
              {
                File inputFile = path.toFile();
                DataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(resolverPath);
                String fileExtension = getFileExtension(inputFile.getName());
                PegasusSchemaParser parser = AbstractSchemaParser.parserForFileExtension(fileExtension, resolver);
                try
                {
                  parser.parse(new FileInputStream(inputFile));
                }
                catch (FileNotFoundException e)
                {
                  throw new RuntimeException(e);
                }
                if (parser.hasError())
                {
                  throw new RuntimeException("Error: " + parser.errorMessage() + ", while parsing schema: " + inputFile.getAbsolutePath());
                }

                List<DataSchema> topLevelDataSchemas = parser.topLevelDataSchemas();
                if (topLevelDataSchemas.size() != 1)
                {
                  throw new RuntimeException("The number of top level schemas is not 1, while parsing schema: " + inputFile.getAbsolutePath());
                }
                DataSchema topLevelDataSchema = topLevelDataSchemas.get(0);
                if (!(topLevelDataSchema instanceof NamedDataSchema))
                {
                  throw new RuntimeException("Invalid schema : " + inputFile.getAbsolutePath() + ", the schema is not a named schema.");
                }
                return topLevelDataSchema;
              })
          .collect(Collectors.toList());
    }
  }

  private static void writeSnapshotFile(File outputDir, String fileName, DataSchema dataSchema) throws IOException
  {
    StringWriter stringWriter = new StringWriter();
    SchemaToPdlEncoder schemaToPdlEncoder = new SchemaToPdlEncoder(stringWriter);
    schemaToPdlEncoder.setTypeReferenceFormat(AbstractSchemaEncoder.TypeReferenceFormat.DENORMALIZE);
    schemaToPdlEncoder.encode(dataSchema);

    File generatedSnapshotFile = new File(outputDir, fileName + PDL);

    Files.write(generatedSnapshotFile.toPath(), stringWriter.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String getFileExtension(String fileName)
  {
    return fileName.substring(fileName.lastIndexOf('.') + 1);
  }
}
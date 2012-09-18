/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.data.avro.generator;


import com.linkedin.data.avro.DataToAvroSchemaTranslationOptions;
import com.linkedin.data.avro.OptionalDefaultMode;
import com.linkedin.data.avro.SchemaTranslator;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.generator.AbstractGenerator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generate Avro avsc files from {@link RecordDataSchema}s.
 * <p>
 * Invoke {@link #main(String[])} with 1st argument being the output directory for
 * generated avsc files. The remaining arguments may be a file name of a Pegasus
 * data schema file, a directory containing Pegasus data schema files,
 * or a fully qualified schema name.
 * <p>
 * The schema resolver use the path specified by the "generator.resolver.path"
 * system property. The path is a colon separated list of jar files and/or directories.
 * <p>
 * Avro avsc files are emitted only for record types.
 */
public class AvroSchemaGenerator extends AbstractGenerator
{
  public static final String GENERATOR_AVRO_TRANSLATE_OPTIONAL_DEFAULT = "generator.avro.optional.default";

  private static final Logger _log = LoggerFactory.getLogger(AvroSchemaGenerator.class);

  /**
   * Generate Avro avsc files from {@link RecordDataSchema}s.
   * <p>
   * Invoke {@link #main(String[])} with 1st argument being the output directory for
   * generated avsc files. The remaining arguments may be a file name of a Pegasus
   * data schema file or a fully qualified schema name.
   * <p>
   * The schema resolver use the path specified by the "generator.resolver.path"
   * system property. The path is a colon separated list of jar files and/or directories.
   * <p>
   * Avro avsc files are emitted only for record types.
   *
   * @param args arguments, as explained above
   * @throws IOException if there are problems opening or deleting files
   */
  public static void main(String[] args) throws IOException
  {
    if (args.length < 2)
    {
      _log.error("Usage: AvroSchemaGenerator targetDirectoryPath [sourceFile or sourceDirectory or schemaName]+");
      System.exit(1);
    }

    AvroSchemaGenerator generator = new AvroSchemaGenerator();

    String translateOptionalDefaultProperty = System.getProperty(GENERATOR_AVRO_TRANSLATE_OPTIONAL_DEFAULT);
    if (translateOptionalDefaultProperty != null)
    {
      translateOptionalDefaultProperty = translateOptionalDefaultProperty.toUpperCase();
      OptionalDefaultMode optionalDefaultMode = OptionalDefaultMode.valueOf(translateOptionalDefaultProperty);
      generator.getDataToAvroSchemaTranslationOptions().setOptionalDefaultMode(optionalDefaultMode);
    }

    generator.run(args[0], Arrays.copyOfRange(args, 1, args.length));
  }

  /**
   * @return all options for schema translations
   */
  public DataToAvroSchemaTranslationOptions getDataToAvroSchemaTranslationOptions()
  {
    return _options;
  }

  /**
   * Parses provided sources and generates Avro avsc files.
   *
   * @param sources provides the paths to schema files and/or fully qualified schema names.
   * @param targetDirectoryPath path to target root java source directory
   * @throws IOException if there are problems opening or deleting files.
   */
  public void run(String targetDirectoryPath, String sources[]) throws IOException
  {
    initSchemaResolver();

    List<File> sourceFiles = parseSources(sources);

    if (getMessage().length() > 0)
    {
      throw new IOException(getMessage().toString());
    }

    File targetDirectory = new File(targetDirectoryPath);
    List<File> targetFiles = targetFiles(targetDirectory);

    if (upToDate(sourceFiles, targetFiles))
    {
      _log.info("Target files are up-to-date: " + targetFiles);
      return;
    }

    _log.info("Generating " + targetFiles.size() + " files: " + targetFiles);
    outputAvroSchemas(targetDirectory);
  }

  private Map<String, String> _nameToAvroSchema = new HashMap<String, String>();
  private final DataToAvroSchemaTranslationOptions _options = new DataToAvroSchemaTranslationOptions();

  @Override
  protected void handleSchema(DataSchema schema)
  {
    if (schema instanceof RecordDataSchema == false)
    {
      // avro only allows records to be top level schemas
      return;
    }
    String preTranslateSchemaText = schema.toString();
    RecordDataSchema recordDataSchema = (RecordDataSchema) schema;
    String fullName = recordDataSchema.getFullName();
    Schema avroSchema = SchemaTranslator.dataToAvroSchema(recordDataSchema);
    String avroSchemaText = avroSchema.toString();
    _nameToAvroSchema.put(fullName, avroSchemaText);
    String postTranslateSchemaText = schema.toString();
    assert(preTranslateSchemaText.equals(postTranslateSchemaText));
  }

  protected void outputAvroSchemas(File targetDirectory) throws IOException
  {
    for (Map.Entry<String, String> entry : _nameToAvroSchema.entrySet())
    {
      File generatedFile = fileForAvroSchema(entry.getKey(), targetDirectory);
      File parentDir = generatedFile.getParentFile();
      if (parentDir.isDirectory() == false)
        parentDir.mkdirs();
      FileOutputStream os = new FileOutputStream(generatedFile, false);
      os.write(entry.getValue().getBytes());
      os.close();
    }
  }

  protected List<File> targetFiles(File targetDirectory)
  {
    ArrayList<File> generatedFiles = new ArrayList<File>(_nameToAvroSchema.size());
    for (String fullName : _nameToAvroSchema.keySet())
    {
      File generatedFile = fileForAvroSchema(fullName, targetDirectory);
      generatedFiles.add(generatedFile);
    }
    return generatedFiles;
  }

  protected File fileForAvroSchema(String fullName, File targetDirectory)
  {
    return new File(targetDirectory, fullName.replace('.', File.separatorChar) + ".avsc");
  }
}

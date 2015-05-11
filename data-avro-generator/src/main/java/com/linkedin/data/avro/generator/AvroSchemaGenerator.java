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


import com.linkedin.data.Data;
import com.linkedin.data.avro.DataToAvroSchemaTranslationOptions;
import com.linkedin.data.avro.OptionalDefaultMode;
import com.linkedin.data.avro.SchemaTranslator;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.generator.AbstractGenerator;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  private final Set<DataSchemaLocation> _sourceLocations = new HashSet<DataSchemaLocation>();

  /**
   * Sources as set.
   */
  private final Set<String> _sources = new HashSet<String>();

  /**
   * Map of output file and the schema that should be written in the output file.
   */
  private final Map<File, String> _fileToAvroSchemaMap = new HashMap<File, String>();

  /**
   * Options that specify how Avro schema should be generated.
   */
  private final DataToAvroSchemaTranslationOptions _options = new DataToAvroSchemaTranslationOptions(JsonBuilder.Pretty.INDENTED);

  private final Config _config;

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

    run(System.getProperty(GENERATOR_RESOLVER_PATH),
        System.getProperty(GENERATOR_AVRO_TRANSLATE_OPTIONAL_DEFAULT),
        args[0],
        Arrays.copyOfRange(args, 1, args.length));
  }

  public AvroSchemaGenerator(Config config)
  {
    super();
    _config = config;
  }

  public static void run(String resolverPath, String optionalDefault, String targetDirectoryPath, String[] sources) throws IOException
  {
    final AvroSchemaGenerator generator = new AvroSchemaGenerator(new Config(resolverPath));

    if (optionalDefault != null)
    {
      final OptionalDefaultMode optionalDefaultMode = OptionalDefaultMode.valueOf(optionalDefault.toUpperCase());
      generator.getDataToAvroSchemaTranslationOptions().setOptionalDefaultMode(optionalDefaultMode);
    }

    generator.generate(targetDirectoryPath, sources);
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
  private void generate(String targetDirectoryPath, String[] sources) throws IOException
  {
    initSchemaResolver();

    _fileToAvroSchemaMap.clear();
    _sourceLocations.clear();
    _sources.addAll(Arrays.asList(sources));

    List<File> sourceFiles = parseSources(sources);

    if (getMessage().length() > 0)
    {
      throw new IOException(getMessage().toString());
    }

    File targetDirectory = new File(targetDirectoryPath);
    List<File> targetFiles = targetFiles(targetDirectory);

    if (FileUtil.upToDate(sourceFiles, targetFiles))
    {
      _log.info("Target files are up-to-date: " + targetFiles);
      return;
    }

    _log.info("Generating " + targetFiles.size() + " files: " + targetFiles);
    outputAvroSchemas(targetDirectory);
  }

  @Override
  protected Config getConfig()
  {
    return _config;
  }

  @Override
  protected void parseFile(File schemaSourceFile) throws IOException
  {
    super.parseFile(schemaSourceFile);
    _sourceLocations.add(new FileDataSchemaLocation(schemaSourceFile));
  }

  @Override
  protected void handleSchema(DataSchema schema)
  {
    // no-op
  }

  protected void outputAvroSchemas(File targetDirectory) throws IOException
  {
    for (Map.Entry<File, String> entry : _fileToAvroSchemaMap.entrySet())
    {
      File generatedFile = entry.getKey();
      File parentDir = generatedFile.getParentFile();
      if (parentDir.isDirectory() == false)
        parentDir.mkdirs();
      FileOutputStream os = new FileOutputStream(generatedFile, false);
      os.write(entry.getValue().getBytes(Data.UTF_8_CHARSET));
      os.close();
    }
  }

  protected List<File> targetFiles(File targetDirectory)
  {
    ArrayList<File> generatedFiles = new ArrayList<File>();

    DataSchemaResolver resolver = getSchemaResolver();
    Map<String, DataSchemaLocation> nameToLocations = resolver.nameToDataSchemaLocations();
    Map<String, NamedDataSchema> nameToSchema = resolver.bindings();

    for (Map.Entry<String, DataSchemaLocation> entry : nameToLocations.entrySet())
    {
      String fullName = entry.getKey();
      DataSchemaLocation location = entry.getValue();
      if (_sourceLocations.contains(location) || _sources.contains(fullName))
      {
        NamedDataSchema schema = nameToSchema.get(fullName);
        if (schema instanceof RecordDataSchema)
        {
          RecordDataSchema recordDataSchema = (RecordDataSchema) schema;
          File generatedFile = fileForAvroSchema(fullName, targetDirectory);
          generatedFiles.add(generatedFile);

          String preTranslateSchemaText = recordDataSchema.toString();
          String avroSchemaText = SchemaTranslator.dataToAvroSchemaJson(recordDataSchema, _options);
          _fileToAvroSchemaMap.put(generatedFile, avroSchemaText);
          String postTranslateSchemaText = recordDataSchema.toString();
          assert(preTranslateSchemaText.equals(postTranslateSchemaText));
        }
      }
    }

    return generatedFiles;
  }

  protected File fileForAvroSchema(String fullName, File targetDirectory)
  {
    return new File(targetDirectory, fullName.replace('.', File.separatorChar) + ".avsc");
  }
}

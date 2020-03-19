/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.restli.tools.data;

import com.linkedin.data.schema.AbstractSchemaEncoder;
import com.linkedin.data.schema.AbstractSchemaParser;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PegasusSchemaParser;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.SchemaToPdlEncoder;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.restli.tools.data.ScmUtil.DESTINATION;
import static com.linkedin.restli.tools.data.ScmUtil.SOURCE;

/**
 * Command line tool to translate files between .pdl and .pdsc schema formats. By default, the tool will abort if the
 * translated schemas are not semantically equivalent to the original schemas.
 */
public class SchemaFormatTranslator
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaFormatTranslator.class);

  private static final Pattern LINE_END_SPACES = Pattern.compile("\\s+$", Pattern.MULTILINE);

  private static final Options OPTIONS = new Options();
  static
  {
    OPTIONS.addOption(OptionBuilder.withLongOpt("help")
        .withDescription("Print help")
        .create('h'));

    OPTIONS.addOption(OptionBuilder.withLongOpt("source-format").withArgName("(pdl|pdsc)").hasArg()
        .withDescription("Source file format ('pdsc' by default)")
        .create('s'));

    OPTIONS.addOption(OptionBuilder.withLongOpt("destination-format").withArgName("(pdl|pdsc)").hasArg()
        .withDescription("Destination file format ('pdl' by default)")
        .create('d'));

    OPTIONS.addOption(OptionBuilder.withLongOpt("keep-original")
        .withDescription("Keep the original files after translation (deleted by default)")
        .create('o'));

    OPTIONS.addOption(OptionBuilder.withLongOpt("preserve-source").hasArg()
        .withDescription("Preserve source history command, use '" + SOURCE + "' as the source filename and use '" + DESTINATION + "' as the destination filename.")
        .create('p'));

    OPTIONS.addOption(OptionBuilder.withLongOpt("skip-verification")
        .withDescription("Skip verification of the translated schemas. Be cautious, as incorrect translations will not be caught.")
        .create('k'));
  }

  public static void main(String[] args) throws Exception
  {
    try
    {
      final CommandLineParser parser = new GnuParser();
      CommandLine cl = parser.parse(OPTIONS, args);

      if (cl.hasOption('h'))
      {
        help();
        System.exit(0);
      }

      String sourceFormat = cl.getOptionValue('s', SchemaParser.FILETYPE).trim();
      String destFormat = cl.getOptionValue('d', PdlSchemaParser.FILETYPE).trim();
      boolean keepOriginal = cl.hasOption('o');
      String preserveSourceCmd = cl.getOptionValue('p');
      boolean skipVerification = cl.hasOption('k');

      String[] cliArgs = cl.getArgs();
      if (cliArgs.length != 3)
      {
        LOGGER.error("Missing arguments, expected 3 ([resolverPath] [sourceRoot] [destinationPath]), got "
            + cliArgs.length);
        help();
        System.exit(1);
      }
      int i = 0;
      String resolverPaths = cliArgs[i++];
      String sourcePath = cliArgs[i++];
      String destPath = cliArgs[i++];

      File sourceDir = new File(sourcePath);
      File destDir = new File(destPath);
      if (!sourceDir.exists() || !sourceDir.canRead())
      {
        LOGGER.error("Source directory does not exist or cannot be read: " + sourceDir.getAbsolutePath());
        System.exit(1);
      }
      destDir.mkdirs();
      if (!destDir.exists() || !destDir.canWrite())
      {
        LOGGER.error("Destination directory does not exist or cannot be written to: " + destDir.getAbsolutePath());
        System.exit(1);
      }

      SchemaFormatTranslator translator = new SchemaFormatTranslator(
          resolverPaths,
          sourceDir,
          destDir,
          sourceFormat,
          destFormat,
          keepOriginal,
          preserveSourceCmd,
          skipVerification);

      translator.translateFiles();
    }
    catch (ParseException e)
    {
      LOGGER.error("Invalid arguments: " + e.getMessage());
      help();
      System.exit(1);
    }
  }

  private String _resolverPath;
  private File _sourceDir;
  private File _destDir;
  private String _sourceFormat;
  private String _destFormat;
  private boolean _keepOriginal;
  private String _preserveSourceCmd;
  private boolean _skipVerification;

  SchemaFormatTranslator(String resolverPath, File sourceDir, File destDir, String sourceFormat, String destFormat,
      boolean keepOriginal, String preserveSourceCmd, boolean skipVerification)
  {
    _resolverPath = resolverPath;
    _sourceDir = sourceDir;
    _destDir = destDir;
    _sourceFormat = sourceFormat;
    _destFormat = destFormat;
    _keepOriginal = keepOriginal;
    _preserveSourceCmd = preserveSourceCmd;
    _skipVerification = skipVerification;
  }

  private void translateFiles() throws IOException, InterruptedException
  {
    LOGGER.info("Translating files. Source dir: {}, sourceFormat: {}, destDir: {}, destFormat: {}, keepOriginal: {}, skipVerification: {}",
        _sourceDir, _sourceFormat, _destDir, _destFormat, _keepOriginal, _skipVerification);
    Map<String, SchemaInfo> topLevelTranslatedSchemas = getTopLevelSchemaToTranslatedSchemaMap();
    if (!_skipVerification)
    {
      verifyTranslatedSchemas(topLevelTranslatedSchemas);
    }
    // Write the destination files. Source files are deleted for this step unless keepOriginal flag is set.
    writeTranslatedSchemasToDirectory(topLevelTranslatedSchemas, _destDir, !_keepOriginal, _preserveSourceCmd, true);
  }

  /**
   * Parses all the top-level schemas in the source directory, encodes into the destination format and returns
   * a map from the top-level schema name to the parsed schema and translated schema string.
   */
  private Map<String, SchemaInfo> getTopLevelSchemaToTranslatedSchemaMap() throws IOException
  {
    Map<String, SchemaInfo> topLevelTranslatedSchemas = new HashMap<>();

    Iterator<File> iter = FileUtils.iterateFiles(_sourceDir, new String[]{_sourceFormat}, true);
    StringBuilder errorMessages = new StringBuilder();
    while(iter.hasNext())
    {
      File sourceFile = iter.next();
      String relativePath = _sourceDir.toURI().relativize(sourceFile.toURI()).getPath();
      String relativeMinusExt = trimFileExtension(relativePath);
      String schemaFullname = relativeMinusExt.replace(File.separatorChar, '.');

      // When translating files 1:1, a new resolver and parser are required for each file translated
      // so that a single top level output schema is matched to each input file.
      MultiFormatDataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(_resolverPath);
      PegasusSchemaParser parser = AbstractSchemaParser.parserForFileExtension(_sourceFormat, resolver);
      parser.parse(new FileInputStream(sourceFile));
      LOGGER.debug("Loaded source schema: {}, from location: {}", schemaFullname, sourceFile.getAbsolutePath());
      NamedDataSchema schema = checkForErrorsAndGetTopLevelSchema(sourceFile, schemaFullname, parser, errorMessages);
      if (schema != null)
      {
        topLevelTranslatedSchemas.put(schemaFullname, new SchemaInfo(schema, sourceFile, encode(schema, _destFormat)));
      }
    }
    if (errorMessages.length() > 0)
    {
      LOGGER.error(errorMessages.toString());
      System.exit(1);
    }
    return topLevelTranslatedSchemas;
  }

  private void verifyTranslatedSchemas(Map<String, SchemaInfo> topLevelTranslatedSchemas) throws IOException, InterruptedException
  {
    File tempDir = new File(FileUtils.getTempDirectory(), "tmpPegasus" + _sourceDir.hashCode());
    File errorSchemasDir = new File(FileUtils.getTempDirectory(), "tmpPegasusErrors" + _sourceDir.hashCode());
    FileUtils.deleteDirectory(tempDir);
    FileUtils.deleteDirectory(errorSchemasDir);
    assert tempDir.mkdirs();
    // Write the schemas to temp directory for validation. Source files are not deleted/moved for this.
    writeTranslatedSchemasToDirectory(
        topLevelTranslatedSchemas, tempDir, false, null, false);

    // Exclude the source models directory from the resolver path
    StringTokenizer paths = new StringTokenizer(_resolverPath, File.pathSeparator);
    StringBuilder pathBuilder = new StringBuilder();
    while (paths.hasMoreTokens())
    {
      String path = paths.nextToken();
      if (path.equals(_sourceDir.getPath()) || path.equals(_sourceDir.getAbsolutePath()))
      {
        // Skip the source models directory
        continue;
      }
      pathBuilder.append(path);
      pathBuilder.append(File.pathSeparatorChar);
    }
    // Include the directory with the generated models in the resolver path
    pathBuilder.append(tempDir.getPath());

    // Now try loading the schemas from the temp directory and compare with source schema.
    String path = pathBuilder.toString();
    LOGGER.debug("Creating resolver with path :{}", path);
    MultiFormatDataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(path);
    boolean hasError = false;
    List<SchemaInfo> failedSchemas = new ArrayList<>();
    for (SchemaInfo schemaInfo : topLevelTranslatedSchemas.values())
    {
      NamedDataSchema sourceSchema = schemaInfo.getSourceSchema();
      String schemaName = sourceSchema.getFullName();
      DataSchema destSchema = resolver.findDataSchema(schemaName, new StringBuilder());

      if (destSchema == null)
      {
        LOGGER.error("Unable to load translated schema: {}", schemaName);
        failedSchemas.add(schemaInfo);
        hasError = true;
      }
      else
      {
        LOGGER.debug("Loaded translated schema: {}, from location: {}", schemaName,
            resolver.nameToDataSchemaLocations().get(schemaName).getSourceFile().getAbsolutePath());

        // Verify that the source schema and the translated schema are semantically equivalent
        if (!sourceSchema.equals(destSchema))
        {
          LOGGER.error("Translation failed for schema: {}", schemaName);
          // Write the translated schema to temp dir.
          File sourceFile = new File(errorSchemasDir, sourceSchema.getName() + "_" + _sourceFormat);
          FileUtils.writeStringToFile(sourceFile, SchemaToJsonEncoder.schemaToJson(sourceSchema, JsonBuilder.Pretty.INDENTED));
          File destFile = new File(errorSchemasDir, sourceSchema.getName() + "_" + _destFormat);
          FileUtils.writeStringToFile(destFile, SchemaToJsonEncoder.schemaToJson(destSchema, JsonBuilder.Pretty.INDENTED));
          LOGGER.error("To see the difference between source and tanslated schemas, run: \ndiff {} {}",
              sourceFile.getAbsolutePath(), destFile.getAbsolutePath());
          failedSchemas.add(schemaInfo);
          hasError = true;
        }
      }
    }
    FileUtils.deleteDirectory(tempDir);
    if (hasError)
    {
      LOGGER.error("Found translation errors, aborting translation. Failed schemas:");
      for (SchemaInfo schemaInfo : failedSchemas)
      {
        LOGGER.error(schemaInfo.getSourceFile().getAbsolutePath());
      }
      System.exit(1);
    }
  }

  private void writeTranslatedSchemasToDirectory(
      Map<String, SchemaInfo> topLevelTranslatedSchemas, File outputDir, boolean moveSource, String preserveSourceCmd,
      boolean trimFile) throws IOException, InterruptedException
  {
    for (SchemaInfo schemaInfo : topLevelTranslatedSchemas.values())
    {
      NamedDataSchema sourceSchema = schemaInfo.getSourceSchema();
      File destinationFile = new File(outputDir,
          sourceSchema.getNamespace().replace('.', File.separatorChar)
              + File.separatorChar + sourceSchema.getName() + "." + _destFormat);
      File path = destinationFile.getParentFile();
      path.mkdirs();
      if (!path.exists() || !path.canWrite())
      {
        LOGGER.error("Unable to create or cannot write to directory: " + path.getAbsolutePath());
        System.exit(1);
      }
      LOGGER.debug("Writing " + destinationFile.getAbsolutePath());
      if (moveSource)
      {
        ScmUtil.tryUpdateSourceHistory(preserveSourceCmd, schemaInfo.getSourceFile(), destinationFile);
      }
      String fileContent = (trimFile && _destFormat.equals(PdlSchemaParser.FILETYPE)) ?
          schemaInfo.getTrimmedDestEncodedSchemaString() :
          schemaInfo.getDestEncodedSchemaString();
      FileUtils.writeStringToFile(destinationFile, fileContent);
    }
  }

  private static NamedDataSchema checkForErrorsAndGetTopLevelSchema(
      File file, String schemaFullname, PegasusSchemaParser parser, StringBuilder errorMessages)
  {
    StringBuilder errorMessageBuilder = parser.errorMessageBuilder();
    if (errorMessageBuilder.length() > 0)
    {
      errorMessages.append("Failed to parse schema: ")
          .append(file.getAbsolutePath())
          .append("\nfullname: ")
          .append(schemaFullname)
          .append("\nerrors: ")
          .append(errorMessageBuilder.toString())
          .append("\n");
      return null;
    }
    List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();
    if (topLevelSchemas.size() != 1)
    {
      errorMessages.append("Expected one top level schema for: ")
          .append(file.getAbsolutePath())
          .append(" but got ")
          .append(topLevelSchemas.size())
          .append(" schemas.")
          .append("\n");
      return  null;
    }
    DataSchema sourceSchema = topLevelSchemas.get(0);
    if (!(sourceSchema instanceof NamedDataSchema) ||
        !((NamedDataSchema) sourceSchema).getFullName().equals(schemaFullname))
    {
      errorMessages.append("Parsed top-level schema does not match the schema file name. File: ")
          .append(file.getAbsolutePath())
          .append("\n parsedSchemaName: ")
          .append(sourceSchema.getUnionMemberKey())
          .append("\n");
      return null;
    }
    return (NamedDataSchema) sourceSchema;
  }

  private static String encode(DataSchema schema, String format) throws IOException
  {
    if (format.equals(PdlSchemaParser.FILETYPE))
    {
      StringWriter writer = new StringWriter();
      SchemaToPdlEncoder encoder = new SchemaToPdlEncoder(writer);
      encoder.setTypeReferenceFormat(AbstractSchemaEncoder.TypeReferenceFormat.PRESERVE);
      encoder.encode(schema);
      return writer.toString();
    }
    else if (format.equals(SchemaParser.FILETYPE))
    {
      JsonBuilder.Pretty pretty = JsonBuilder.Pretty.INDENTED;
      JsonBuilder builder = new JsonBuilder(pretty);
      try
      {
        SchemaToJsonEncoder encoder = new SchemaToJsonEncoder(builder, AbstractSchemaEncoder.TypeReferenceFormat.PRESERVE);
        encoder.encode(schema);
        return builder.result();
      }
      finally
      {
        builder.closeQuietly();
      }
    }
    else
    {
      throw new IllegalArgumentException("Unsupported format: " + format);
    }
  }

  public static String stripLineEndSpaces(String str)
  {
    return LINE_END_SPACES.matcher(str).replaceAll("");
  }

  private static String trimFileExtension(String path)
  {
    return path.substring(0, path.lastIndexOf('.'));
  }

  private static void help()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(120,
        SchemaFormatTranslator.class.getSimpleName(),
        "[resolverPath] [sourceRoot] [destinationPath]", OPTIONS,
        "",
        true);
  }

  private static class SchemaInfo
  {
    private final NamedDataSchema _sourceSchema;
    private final File _sourceFile;
    private final String _destEncodedSchemaString;

    private SchemaInfo(NamedDataSchema sourceSchema, File sourceFile, String destEncodedSchemaString)
    {
      _sourceSchema = sourceSchema;
      _sourceFile = sourceFile;
      _destEncodedSchemaString = destEncodedSchemaString;
    }

    NamedDataSchema getSourceSchema()
    {
      return _sourceSchema;
    }

    File getSourceFile()
    {
      return _sourceFile;
    }

    String getDestEncodedSchemaString()
    {
      return _destEncodedSchemaString;
    }

    String getTrimmedDestEncodedSchemaString()
    {
      return stripLineEndSpaces(_destEncodedSchemaString);
    }
  }
}

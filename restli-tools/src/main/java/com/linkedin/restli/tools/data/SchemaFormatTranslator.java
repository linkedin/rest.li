package com.linkedin.restli.tools.data;

import com.linkedin.data.schema.AbstractSchemaEncoder;
import com.linkedin.data.schema.AbstractSchemaParser;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.JsonBuilder;
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
import java.util.Iterator;
import java.util.List;
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


/**
 * Command line tool to translate files between .pdl and .pdsc schema formats.
 */
public class SchemaFormatTranslator {
  private static final Logger _log = LoggerFactory.getLogger(SchemaFormatTranslator.class);

  private static final Options _options = new Options();
  static
  {
    _options.addOption(OptionBuilder.withLongOpt("help")
        .withDescription("Print help")
        .create('h'));

    _options.addOption(OptionBuilder.withLongOpt("source-format").withArgName("(pdl|pdsc)").hasArg()
        .withDescription("Source file format ('pdsc' by default)")
        .create('s'));

    _options.addOption(OptionBuilder.withLongOpt("destination-format").withArgName("(pdl|pdsc)").hasArg()
        .withDescription("Destination file format ('pdl' by default)")
        .create('d'));
  }

  public static void main(String[] args) throws Exception
  {
    try
    {
      final CommandLineParser parser = new GnuParser();
      CommandLine cl = parser.parse(_options, args);

      if (cl.hasOption('h'))
      {
        help();
        System.exit(0);
      }

      String sourceFormat = cl.getOptionValue('s', SchemaParser.FILETYPE);
      String destFormat = cl.getOptionValue('d', PdlSchemaParser.FILETYPE);

      String[] cliArgs = cl.getArgs();
      if (cliArgs.length != 3) {
        _log.error("Missing arguments, expected 3 ([resourcePath] [sourceRoot] [destinationPath]), got " + cliArgs.length);
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
        _log.error("Source directory does not exist or cannot be read: " + sourceDir.getAbsolutePath());
        System.exit(1);
      }
      destDir.mkdirs();
      if (!destDir.exists() || !destDir.canWrite())
      {
        _log.error("Destination directory does not exist or cannot be written to: " + destDir.getAbsolutePath());
        System.exit(1);
      }

      SchemaFormatTranslator translator =
          new SchemaFormatTranslator(resolverPaths, sourceDir, destDir, sourceFormat, destFormat);
      translator.translateFiles();
    }
    catch (ParseException e)
    {
      _log.error("Invalid arguments: " + e.getMessage());
      help();
      System.exit(1);
    }
  }

  private String _resolverPath;
  private File _sourceDir;
  private File _destDir;
  private String _sourceFormat;
  private String _destFormat;

  public SchemaFormatTranslator(String resolverPath, File sourceDir, File destDir, String sourceFormat, String destFormat) {
    _resolverPath = resolverPath;
    _sourceDir = sourceDir;
    _destDir = destDir;
    _sourceFormat = sourceFormat;
    _destFormat = destFormat;
  }

  private void translateFiles() throws IOException
  {
    Iterator<File> iter = FileUtils.iterateFiles(_sourceDir, new String[]{_sourceFormat}, true);
    while(iter.hasNext())
    {
      File sourceFile = iter.next();
      String relativePath = _sourceDir.toURI().relativize(sourceFile.toURI()).getPath();
      String relativeMinusExt = trimFileExtension(relativePath);
      String schemaFullname = relativeMinusExt.replace(File.separatorChar, '.');
      File destinationFile = new File(_destDir, relativeMinusExt + "." + _destFormat);
      File path = destinationFile.getParentFile();
      path.mkdirs();
      if (!path.exists() || !path.canWrite())
      {
        _log.error("Unable to create or cannot write to destination directory: " + path.getAbsolutePath());
        System.exit(1);
      }

      translateFile(sourceFile, destinationFile, schemaFullname);
    }
  }

  private void translateFile(File sourceFile, File destinationFile, String schemaFullname) throws IOException {
    // When translating files 1:1, a new resolver and parser are required for each file translated
    // so that a single top level output schema is matched to each input file.
    MultiFormatDataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(_resolverPath);
    PegasusSchemaParser parser = AbstractSchemaParser.parserForFileExtension(_sourceFormat, resolver);
    parser.parse(new FileInputStream(sourceFile));
    checkForErrors(_resolverPath, sourceFile, schemaFullname, parser);
    List<DataSchema> topLevelSchemas = parser.topLevelDataSchemas();
    if (topLevelSchemas.size() == 1)
    {
      DataSchema schema = topLevelSchemas.get(0);
      String encoded = encode(schema, _destFormat);
      _log.debug("Writing " + destinationFile.getAbsolutePath());
      FileUtils.writeStringToFile(destinationFile, encoded);
    }
    else
    {
      _log.error("Expected one top level schema for " + destinationFile.getAbsolutePath() + " but got " + topLevelSchemas.size());
    }
  }

  private static void checkForErrors(String resolverPath, File file, String schemaFullname, PegasusSchemaParser parser) {
    StringBuilder errorMessageBuilder = parser.errorMessageBuilder();
    if (errorMessageBuilder.length() > 0)
    {
      _log.error(
          "Failed to parse schema: " + file.getAbsolutePath() + "\nfullname: " + schemaFullname + "\nerrors: " + errorMessageBuilder.toString() + "\nresolverPath: " + resolverPath);
      System.exit(1);
    }
    if (parser.topLevelDataSchemas().size() != 1)
    {
      _log.error(
          "Failed to parse any schemas from: " + file.getAbsolutePath() + "\nfullname: " + schemaFullname + "\nerrors: " + errorMessageBuilder.toString() + "\nresolverPath: " + resolverPath);
      System.exit(1);
    }
  }

  private static String encode(DataSchema schema, String format) throws IOException {
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

  private static String trimFileExtension(String path)
  {
    return path.substring(0, path.lastIndexOf('.'));
  }

  private static void help()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(120,
        SchemaFormatTranslator.class.getSimpleName(),
        "[resolverPath] [sourceRoot] [destinationPath]",
        _options,
        "",
        true);
  }
}

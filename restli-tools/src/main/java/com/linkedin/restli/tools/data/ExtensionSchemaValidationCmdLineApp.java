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
package com.linkedin.restli.tools.data;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.internal.tools.ArgumentFileProcessor;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
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


public class ExtensionSchemaValidationCmdLineApp
{
  private static final Logger _logger = LoggerFactory.getLogger(ExtensionSchemaValidationCmdLineApp.class);
  private static final Options _options = new Options();
  private static final String _pdl = "pdl";

  static
  {
    _options.addOption(OptionBuilder.withLongOpt("help")
        .withDescription("Print help")
        .create('h'));
  }

  public static void main(String[] args) throws Exception
  {
    try {
      final CommandLineParser parser = new GnuParser();
      CommandLine cl = parser.parse(_options, args);

      if (cl.hasOption('h'))
      {
        help();
        System.exit(0);
      }

      String[] cliArgs = cl.getArgs();
      if (cliArgs.length != 2)
      {
        _logger.error("Invalid arguments");
        help();
        System.exit(1);
      }
      int i = 0;
      String resolverPath = RestLiToolsUtils.readArgFromFileIfNeeded(cliArgs[i++]);
      String inputPath = cliArgs[i];

      File inputDir = new File(inputPath);

      if (!inputDir.exists() || !inputDir.canRead()) {
        _logger.error("Input directory does not exist or cannot be read: " + inputDir.getAbsolutePath());
        System.exit(1);
      }

      parseAndValidateExtensionSchemas(resolverPath, inputDir);
    }
    catch (ParseException e)
    {
      _logger.error("Invalid arguments: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void parseAndValidateExtensionSchemas(String resolverPath, File inputDir) throws IOException
  {
    // Parse each extension schema and validate it
    Iterator<File> iterator = FileUtils.iterateFiles(inputDir, new String[]{_pdl}, true);
    while(iterator.hasNext())
    {
      File inputFile = iterator.next();
      DataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(resolverPath);
      PdlSchemaParser parser = new PdlSchemaParser(resolver);
      parser.parse(new FileInputStream(inputFile));
      if (parser.hasError())
      {
        _logger.error(parser.errorMessage());
        System.exit(1);
      }

      List<DataSchema> topLevelDataSchemas = parser.topLevelDataSchemas();
      if (topLevelDataSchemas == null || topLevelDataSchemas.isEmpty() || topLevelDataSchemas.size() > 1)
      {
        _logger.error("Could not parse extension schema : " + inputFile.getAbsolutePath());
        System.exit(1);
      }
      DataSchema topLevelDataSchema = topLevelDataSchemas.get(0);
      if (!(topLevelDataSchema instanceof NamedDataSchema))
      {
        _logger.error("Invalid extension schema : [{}], the schema is not a named schema.", inputFile.getAbsolutePath());
        System.exit(1);
      }
      if (!((NamedDataSchema) topLevelDataSchema).getFullName().endsWith("Extensions"))
      {
        _logger.error("Invalid extension schema name : [{}]. The name of the extension schema must be <baseSchemaName> + 'Extensions'", ((NamedDataSchema) topLevelDataSchema).getFullName());
        System.exit(1);
      }

      List<NamedDataSchema> includes = ((RecordDataSchema) topLevelDataSchema).getInclude();
      // TODO: Check includes schemas can only be the resource schemas

      List<RecordDataSchema.Field> extensionSchemaFields = ((RecordDataSchema) topLevelDataSchema).getFields()
          .stream()
          .filter(f -> !((RecordDataSchema) topLevelDataSchema).isFieldFromIncludes(f))
          .collect(Collectors.toList());
      // TODO : Call annotation process to validate each field's annotation, need to create schemaVisitor and handler for extension annotation.
    }
  }

  private static void help()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(120,
        ExtensionSchemaValidationCmdLineApp.class.getSimpleName(),
        "[resolverPath], [inputPath]",
         _options,
        "",
        true);
  }
}

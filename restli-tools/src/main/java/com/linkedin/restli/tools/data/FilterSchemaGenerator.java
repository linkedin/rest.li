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

package com.linkedin.restli.tools.data;


import com.linkedin.data.it.Predicate;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.util.Filters;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.util.FileUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;


/**
 * Provides main function for filtering {@link com.linkedin.data.schema.DataSchema}s in a directory
 * by removing unwanted fields or custom properties of the schema according to given {@link com.linkedin.data.it.Predicate}.
 *
 * @author Keren Jin
 */
public class FilterSchemaGenerator
{
  public static void main(String[] args)
  {
    final CommandLineParser parser = new GnuParser();
    CommandLine cl = null;
    try
    {
      cl = parser.parse(_options, args);
    }
    catch (ParseException e)
    {
      _log.error("Invalid arguments: " + e.getMessage());
      reportInvalidArguments();
    }

    final String[] directoryArgs = cl.getArgs();
    if (directoryArgs.length != 2)
    {
      reportInvalidArguments();
    }

    final File sourceDirectory = new File(directoryArgs[0]);
    if (!sourceDirectory.exists())
    {
      _log.error(sourceDirectory.getPath() + " does not exist");
      System.exit(1);
    }
    if (!sourceDirectory.isDirectory())
    {
      _log.error(sourceDirectory.getPath() + " is not a directory");
      System.exit(1);
    }
    final URI sourceDirectoryURI = sourceDirectory.toURI();

    final File outputDirectory = new File(directoryArgs[1]);
    if (outputDirectory.exists() && !sourceDirectory.isDirectory())
    {
      _log.error(outputDirectory.getPath() + " is not a directory");
      System.exit(1);
    }

    final boolean isAvroMode = cl.hasOption('a');
    final String predicateExpression = cl.getOptionValue('e');
    final Predicate predicate = PredicateExpressionParser.parse(predicateExpression);

    final Collection<File> sourceFiles = FileUtil.listFiles(sourceDirectory, null);
    int exitCode = 0;
    for (File sourceFile : sourceFiles)
    {
      try
      {
        final ValidationOptions val = new ValidationOptions();
        val.setAvroUnionMode(isAvroMode);

        final SchemaParser schemaParser = new SchemaParser();
        schemaParser.setValidationOptions(val);

        schemaParser.parse(new FileInputStream(sourceFile));
        if (schemaParser.hasError())
        {
          _log.error("Error parsing " + sourceFile.getPath() + ": " + schemaParser.errorMessageBuilder().toString());
          exitCode = 1;
          continue;
        }

        final DataSchema originalSchema = schemaParser.topLevelDataSchemas().get(0);
        if (!(originalSchema instanceof NamedDataSchema))
        {
          _log.error(sourceFile.getPath() + " does not contain valid NamedDataSchema");
          exitCode = 1;
          continue;
        }

        final SchemaParser filterParser = new SchemaParser();
        filterParser.setValidationOptions(val);

        final NamedDataSchema filteredSchema = Filters.removeByPredicate((NamedDataSchema) originalSchema,
                                                                         predicate, filterParser);
        if (filterParser.hasError())
        {
          _log.error("Error applying predicate: " + filterParser.errorMessageBuilder().toString());
          exitCode = 1;
          continue;
        }

        final String relativePath = sourceDirectoryURI.relativize(sourceFile.toURI()).getPath();
        final String outputFilePath = outputDirectory.getPath() + File.separator + relativePath;
        final File outputFile = new File(outputFilePath);
        final File outputFileParent = outputFile.getParentFile();
        outputFileParent.mkdirs();
        if (!outputFileParent.exists())
        {
          _log.error("Unable to write filtered schema to " + outputFileParent.getPath());
          exitCode = 1;
          continue;
        }

        FileOutputStream fout = new FileOutputStream(outputFile);
        fout.write(filteredSchema.toString().getBytes(RestConstants.DEFAULT_CHARSET));
        fout.close();
      }
      catch (IOException e)
      {
        _log.error(e.getMessage());
        exitCode = 1;
      }
    }

    System.exit(exitCode);
  }

  private static void reportInvalidArguments()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(FilterSchemaGenerator.class.getName() + " sourceDirectoryPath outputDirectoryPath", _options, true);
    System.exit(1);
  }

  private static final Logger _log = LoggerFactory.getLogger(FilterSchemaGenerator.class);
  private static final Options _options = new Options();
  static
  {
    _options.addOption(OptionBuilder.withLongOpt("avro")
                          .withDescription("Specify this option if processing Avro schemas")
                          .create("a"));
    _options.addOption(OptionBuilder.withLongOpt("expr").withArgName("expression").hasArg().isRequired().withDescription(
        "Expression of filter predicate combinations, which are expressed in fully qualified class names").create("e"));
  }
}

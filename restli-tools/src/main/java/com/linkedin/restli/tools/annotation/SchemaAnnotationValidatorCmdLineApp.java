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

package com.linkedin.restli.tools.annotation;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.annotation.SchemaAnnotationHandler;
import com.linkedin.data.schema.annotation.SchemaAnnotationProcessor;
import com.linkedin.pegasus.generator.DataSchemaParser;
import com.linkedin.restli.internal.tools.ClassJarPathUtil;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Validate Schema Annotation using {@link com.linkedin.data.schema.annotation.SchemaAnnotationProcessor}
 */
public class SchemaAnnotationValidatorCmdLineApp
{
  private static final Logger _log = LoggerFactory.getLogger(SchemaAnnotationValidatorCmdLineApp.class);

  private static final Options _options = new Options();
  static
  {
    _options.addOption(OptionBuilder.withLongOpt("help")
                                    .withDescription("Print help")
                                    .create('h'));

    _options.addOption(OptionBuilder.withLongOpt("handler-jarpath").hasArgs()
                                    .withDescription("path of the jars which contains the handlers")
                                    .withArgName("jar path of the handlers")
                                    .isRequired()
                                    .create('j'));

    _options.addOption(OptionBuilder.withLongOpt("handler-classnames").hasArgs()
                                    .withDescription("class names of the handlers")
                                    .withArgName("class name of the handlers")
                                    .isRequired()
                                    .create('c'));

    _options.addOption(OptionBuilder.withLongOpt("resolverPath").hasArgs()
                                    .withDescription("resolver path for schema parsers")
                                    .withArgName("resolver path")
                                    .isRequired()
                                    .create('r'));
  }

  public static void main(String[] args) throws IOException
  {
    String resolverPath = null;
    String handlerJarPaths = null;
    String handlerClassNames = null;
    String inputDir = null;
    try
    {
      final CommandLineParser parser = new GnuParser();
      CommandLine cl = parser.parse(_options, args);
      if (cl.hasOption('h'))
      {
        help();
        System.exit(0);
      }


      String[] cliArgs = cl.getArgs();
      if (cliArgs.length != 1)
      {
        _log.error("Wrong argument given");
        help();
        System.exit(1);
      }
      resolverPath =  RestLiToolsUtils.readArgFromFileIfNeeded(cl.getOptionValue('r'));;
      handlerJarPaths = cl.getOptionValue('j');
      handlerClassNames = cl.getOptionValue('c');
      inputDir = cliArgs[0];

    }
    catch (ParseException e)
    {
      _log.error("Invalid arguments: " + e.getMessage());
      help();
      System.exit(1);
    }

    List<SchemaAnnotationHandler> handlers = null;
    try
    {
      handlers = ClassJarPathUtil.getAnnotationHandlers(handlerJarPaths, handlerClassNames);
    }
    catch (IllegalStateException e)
    {
      _log.error(e.getMessage());
      throw new IllegalStateException("ValidateSchemaAnnotation task failed");
    }

    boolean hasError = false;
    List<String> schemaWithFailures = new ArrayList<>();
    List<DataSchema> namedDataSchema = parseSchemas(resolverPath, inputDir);
    for (DataSchema dataSchema: namedDataSchema)
    {
      SchemaAnnotationProcessor.SchemaAnnotationProcessResult result =
          SchemaAnnotationProcessor.process(handlers, dataSchema, new SchemaAnnotationProcessor.AnnotationProcessOption());
      // If any of the nameDataSchema failed to be processed, log error and throw exception
      if (result.hasError())
      {
        String schemaName = ((NamedDataSchema) dataSchema).getFullName();
        _log.error("Annotation processing for data schema [{}] failed, detailed error: \n",
                   schemaName);
        _log.error(result.getErrorMsgs());
        schemaWithFailures.add(schemaName);
        hasError = true;
      }
      else {
        _log.info("Successfully resolved and validated data schema [{}]", ((NamedDataSchema) dataSchema).getFullName());
      }
    }

    if(hasError)
    {
      _log.error("ValidateSchemaAnnotation task failed due to failure in following schemas [{}]", schemaWithFailures);
      // Throw exception at the end if any of
      throw new IllegalStateException("ValidateSchemaAnnotation task failed");
    }
  }

  private static List<DataSchema> parseSchemas(String resolverPath, String modelsLocation) throws IOException
  {
    DataSchemaParser dataSchemaParser = new DataSchemaParser.Builder(resolverPath).build();
    DataSchemaParser.ParseResult parsedSources = dataSchemaParser.parseSources(new String[]{modelsLocation});

    Map<DataSchema, DataSchemaLocation> schemaLocations = parsedSources.getSchemaAndLocations();

    return schemaLocations.entrySet()
                          .stream()
                          .filter(
                              entry -> entry.getKey() instanceof NamedDataSchema)// only the named schemas will be checked
                          .filter(entry -> !entry.getValue()
                                                 .getSourceFile()
                                                 .getAbsolutePath()
                                                 .contains(".jar"))// schemas defined only in the current module
                          .map(Map.Entry::getKey)
                          .collect(Collectors.toList());
  }

  private static void help()
  {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(120,
                        SchemaAnnotationValidatorCmdLineApp.class.getSimpleName(),
                        "[input file path]",
                        _options,
                        "",
                        true);
  }
}
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

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.grammar.PdlSchemaParser;
import com.linkedin.data.schema.resolver.MultiFormatDataSchemaResolver;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.UnrecognizedFieldMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.restli.common.ExtensionSchemaAnnotation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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


/**
 * This class is used to validate extension schema, the validation covers following parts:
 * 1. The extension schema is a validate schema.
 * 2. The extension schema's fields should only be annotated by "extension" namespace.
 * 3. The extension schema's fields annotations should be {@link ExtensionSchemaAnnotation}.
 * 4. The extension schema's field's schema only be annotated by "resourceKey" namespace.
 *
 *
 * @author Yingjie Bi
 */
public class ExtensionSchemaValidationCmdLineApp
{
  private static final Logger _logger = LoggerFactory.getLogger(ExtensionSchemaValidationCmdLineApp.class);
  private static final Options _options = new Options();
  private static final String PDL = "pdl";
  private static final String EXTENSION_ANNOTATION_NAMESPACE= "extension";
  private static final String RESOURCE_KEY_ANNOTATION_NAMESPACE = "resourceKey";

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
    Iterator<File> iterator = FileUtils.iterateFiles(inputDir, new String[]{PDL}, true);
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

      checkExtensionSchemaFields(extensionSchemaFields);
    }
  }

  private static void checkExtensionSchemaFields(List<RecordDataSchema.Field> extensionSchemaFields)
  {
    for (RecordDataSchema.Field field : extensionSchemaFields)
    {
      // check extension schema field annotation
      Map<String, Object> properties = field.getProperties();
      if (properties.isEmpty() || properties.keySet().size() != 1 || !properties.containsKey(EXTENSION_ANNOTATION_NAMESPACE))
      {
        _logger.error("The field [{}] of extension schema must and only be annotated with 'extension'", field.getName());
        System.exit(1);
      }
      Object dataElement = properties.get(EXTENSION_ANNOTATION_NAMESPACE);

      ValidationOptions validationOptions =
          new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.STRING_TO_PRIMITIVE, UnrecognizedFieldMode.DISALLOW);
      try
      {
        if (!(dataElement instanceof DataMap))
        {
          _logger.error("extension schema annotation is not a datamap!");
          System.exit(1);
        }
        DataSchema extensionSchemaAnnotationSchema = new ExtensionSchemaAnnotation().schema();
        ValidationResult result = ValidateDataAgainstSchema.validate(dataElement, extensionSchemaAnnotationSchema, validationOptions);
        if (!result.isValid())
        {
          _logger.error("extension schema annotation is not valid: " + result.getMessages());
          System.exit(1);
        }
      }
      catch (Exception e)
      {
        _logger.error("Error while checking extension schema field annotation: " + e.getMessage());
        System.exit(1);
      }
      checkExtensionSchemaFieldSchema(field.getType());
    }
  }

  private static void checkExtensionSchemaFieldSchema(DataSchema fieldSchema)
  {
    if (!isAnnotatedWithResourceKey(fieldSchema) && !isArrayAndItemIsAnnotatedWithResourceKey(fieldSchema))
    {
        _logger.error("Field schema: [{}] is not annotated with 'resourceKey'", fieldSchema.toString());
        System.exit(1);
    }
  }

  private static boolean isArrayAndItemIsAnnotatedWithResourceKey(DataSchema schema)
  {
    if (schema.getType() == DataSchema.Type.ARRAY)
    {
      DataSchema itemSchema = ((ArrayDataSchema) schema).getItems();
      return isAnnotatedWithResourceKey(itemSchema);
    }
    return false;
  }

  private static boolean isAnnotatedWithResourceKey(DataSchema fieldSchema)
  {
    Map<String, Object> fieldAnnotation = fieldSchema.getProperties();
    return !fieldAnnotation.isEmpty() && fieldAnnotation.containsKey(RESOURCE_KEY_ANNOTATION_NAMESPACE);
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

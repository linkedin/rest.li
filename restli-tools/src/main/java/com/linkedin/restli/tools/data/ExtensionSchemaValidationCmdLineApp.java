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

import com.linkedin.data.DataList;
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

import static com.linkedin.data.schema.annotation.ExtensionSchemaAnnotationHandler.EXTENSION_ANNOTATION_NAMESPACE;


/**
 * This class is used to validate extension schemas, the validation covers following parts:
 * 1. The extension schema is a valid schema.
 * 2. The extension schema name has to follow the naming convention: <baseSchemaName> + "Extensions"
 * 3. The extension schema can only include the base schema.
 * 4. The extension schema's field annotation keys must be in the "extension" namespace
 * 5. The extension schema's field annotations must conform to {@link ExtensionSchemaAnnotation}.
 * 6. The extension schema's fields can only be Typeref or array of Typeref
 * 7. The extension schema's field schema's annotation keys must be in the "resourceKey" namespace.
 * 8. The extension schema's field annotation versionSuffix value has to match the versionSuffix value in "resourceKey" annotation on the field schema.
 *
 *
 * @author Yingjie Bi
 */
public class ExtensionSchemaValidationCmdLineApp
{
  private static final Logger _logger = LoggerFactory.getLogger(ExtensionSchemaValidationCmdLineApp.class);
  private static final Options _options = new Options();
  private static final String PDL = "pdl";
  private static final String RESOURCE_KEY_ANNOTATION_NAMESPACE = "resourceKey";
  private static final String EXTENSIONS_SUFFIX = "Extensions";
  private static final String VERSION_SUFFIX = "versionSuffix";

  static
  {
    _options.addOption(OptionBuilder.withLongOpt("help")
        .withDescription("Print help")
        .create('h'));
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
    catch (InvalidExtensionSchemaException e)
    {
      _logger.error("Invalid extension schema: " + e.getMessage());
      System.exit(1);
    }
  }

 static void parseAndValidateExtensionSchemas(String resolverPath, File inputDir)
     throws IOException, InvalidExtensionSchemaException
 {
   // Parse each extension schema and validate it
   Iterator<File> iterator = FileUtils.iterateFiles(inputDir, new String[]{PDL}, true);
   DataSchemaResolver resolver = MultiFormatDataSchemaResolver.withBuiltinFormats(resolverPath);
   while (iterator.hasNext())
   {
     File inputFile = iterator.next();
     PdlSchemaParser parser = new PdlSchemaParser(resolver);
     parser.parse(new FileInputStream(inputFile));
     if (parser.hasError())
     {
       throw new InvalidExtensionSchemaException(parser.errorMessage());
     }

     List<DataSchema> topLevelDataSchemas = parser.topLevelDataSchemas();
     if (topLevelDataSchemas == null || topLevelDataSchemas.isEmpty() || topLevelDataSchemas.size() > 1)
     {
       throw new InvalidExtensionSchemaException("Could not parse extension schema : " + inputFile.getAbsolutePath());
     }
     DataSchema topLevelDataSchema = topLevelDataSchemas.get(0);
     if (!(topLevelDataSchema instanceof NamedDataSchema))
     {
       throw new InvalidExtensionSchemaException("Invalid extension schema : " + inputFile.getAbsolutePath() + ", the schema is not a named schema.");
     }
     if (!((NamedDataSchema) topLevelDataSchema).getName().endsWith(EXTENSIONS_SUFFIX))
     {
       throw new InvalidExtensionSchemaException(
           "Invalid extension schema name: '" + ((NamedDataSchema) topLevelDataSchema).getName() + "'. The name of the extension schema must be <baseSchemaName> + 'Extensions'");
     }

     List<NamedDataSchema> includes = ((RecordDataSchema) topLevelDataSchema).getInclude();
     if (includes.size() != 1)
     {
       throw new InvalidExtensionSchemaException("The extension schema: '" + ((NamedDataSchema) topLevelDataSchema).getName() + "' should include and only include the base schema");
     }

     NamedDataSchema includeSchema = includes.get(0);
     if (!((NamedDataSchema) topLevelDataSchema).getName().startsWith(includeSchema.getName()))
     {
       throw new InvalidExtensionSchemaException(
           "Invalid extension schema name: '" + ((NamedDataSchema) topLevelDataSchema).getName() + "'. The name of the extension schema must be baseSchemaName: '"
               + includeSchema.getName() + "' + 'Extensions");
     }

      List<RecordDataSchema.Field> extensionSchemaFields = ((RecordDataSchema) topLevelDataSchema).getFields()
          .stream()
          .filter(f -> !((RecordDataSchema) topLevelDataSchema).isFieldFromIncludes(f))
          .collect(Collectors.toList());

      checkExtensionSchemaFields(extensionSchemaFields);
    }
  }

  private static void checkExtensionSchemaFields(List<RecordDataSchema.Field> extensionSchemaFields)
      throws InvalidExtensionSchemaException
  {
    for (RecordDataSchema.Field field : extensionSchemaFields)
    {
      // check extension schema field annotation
      Map<String, Object> properties = field.getProperties();
      if (properties.isEmpty() || properties.keySet().size() != 1 || !properties.containsKey(
          EXTENSION_ANNOTATION_NAMESPACE))
      {
        throw new InvalidExtensionSchemaException("The field : " + field.getName() + " of extension schema must and only be annotated with 'extension'");
      }
      Object dataElement = properties.get(EXTENSION_ANNOTATION_NAMESPACE);

      ValidationOptions validationOptions =
          new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.STRING_TO_PRIMITIVE, UnrecognizedFieldMode.DISALLOW);
      try
      {
        if (!(dataElement instanceof DataMap))
        {
          throw new InvalidExtensionSchemaException("Extension schema annotation is not a datamap!");
        }
        DataSchema extensionSchemaAnnotationSchema = new ExtensionSchemaAnnotation().schema();
        ValidationResult result = ValidateDataAgainstSchema.validate(dataElement, extensionSchemaAnnotationSchema, validationOptions);
        if (!result.isValid())
        {
          throw new InvalidExtensionSchemaException("Extension schema annotation is not valid: " + result.getMessages());
        }
      }
      catch (InvalidExtensionSchemaException e)
      {
        throw e;
      }
      catch (Exception e)
      {
        _logger.error("Error while checking extension schema field annotation: " + e.getMessage());
        System.exit(1);
      }
      checkExtensionSchemaFieldSchema(field.getType(), properties);
    }
  }

  private static void checkExtensionSchemaFieldSchema(DataSchema fieldSchema, Map<String, Object> extensionAnnotations)
      throws InvalidExtensionSchemaException
  {
      if (fieldSchema.getType() == DataSchema.Type.ARRAY)
      {
        fieldSchema = ((ArrayDataSchema) fieldSchema).getItems();
      }
      if (fieldSchema.getType() != DataSchema.Type.TYPEREF)
      {
        throw new InvalidExtensionSchemaException("Field schema: '" + fieldSchema.toString() + "' is not a TypeRef type.");
      }
      isAnnotatedWithResourceKey(fieldSchema, extensionAnnotations);
  }

  private static void isAnnotatedWithResourceKey(DataSchema fieldSchema, Map<String, Object> extensionAnnotations)
      throws InvalidExtensionSchemaException
  {
    Map<String, Object> fieldAnnotation = fieldSchema.getProperties();
    if (!fieldAnnotation.isEmpty() && fieldAnnotation.containsKey(RESOURCE_KEY_ANNOTATION_NAMESPACE))
    {
     checkExtensionVersionSuffixValue(fieldAnnotation, extensionAnnotations);
    }
    else
    {
      throw new InvalidExtensionSchemaException("Field schema: " + fieldSchema.toString() + " is not annotated with 'resourceKey'");
    }
  }

  private static void checkExtensionVersionSuffixValue(Map<String, Object> resourceKeyAnnotations, Map<String, Object> extensionAnnotations)
      throws InvalidExtensionSchemaException
  {
    DataMap extensionAnnotationMap = (DataMap) extensionAnnotations.getOrDefault(EXTENSION_ANNOTATION_NAMESPACE, new DataMap());
    if (extensionAnnotationMap.containsKey(VERSION_SUFFIX))
    {
      boolean versionSuffixValueIsValid = false;
      DataList resourceKeyAnnotationList = (DataList) resourceKeyAnnotations.getOrDefault(RESOURCE_KEY_ANNOTATION_NAMESPACE, new DataList());
      if (resourceKeyAnnotationList.size() < 2)
      {
        throw new InvalidExtensionSchemaException("resourceKey annotation: "+ resourceKeyAnnotations.toString()  + " is not defined as multiple versions");
      }
      for (int i = 1; i < resourceKeyAnnotationList.size(); i++)
      {
        DataMap resourceKeyAnnotation = (DataMap) resourceKeyAnnotationList.get(i);
        String versionSuffixValueInResourceKey = (String) resourceKeyAnnotation.get(VERSION_SUFFIX);
        if (((String)extensionAnnotationMap.get(VERSION_SUFFIX)).equals(versionSuffixValueInResourceKey))
        {
          versionSuffixValueIsValid = true;
          break;
        }
      }
      if (!versionSuffixValueIsValid)
      {
        throw new InvalidExtensionSchemaException("versionSuffix value: '" + (String)extensionAnnotationMap.get(VERSION_SUFFIX) +
            "' does not match the versionSuffix value which was defined in resourceKey annotation");
      }
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

  private static class InvalidExtensionSchemaException extends Exception
  {
    private static final long serialVersionUID = 1;
    public InvalidExtensionSchemaException(String message) {
      super(message);
    }
  }
}

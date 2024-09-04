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
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.UnrecognizedFieldMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.restli.common.ExtensionSchemaAnnotation;
import com.linkedin.restli.common.GrpcExtensionAnnotation;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import static com.linkedin.data.schema.annotation.ExtensionSchemaAnnotationHandler.*;
import static com.linkedin.data.schema.annotation.GrpcExtensionAnnotationHandler.*;


/**
 * This class is used to validate extension schemas, the validation covers following parts:
 * 1. The extension schema is a valid schema.
 * 2. The extension schema name has to follow the naming convention: <baseSchemaName> + "Extensions"
 * 3. The extension schema can only include the base schema.
 * 4. The extension schema's field annotation keys must be in the "extension" and/or "grpcExtension" namespaces
 * 5. The extension schema's field annotations must conform to {@link ExtensionSchemaAnnotation} and/or {@link GrpcExtensionAnnotation}.
 * 6. The extension schema's fields can only be Typeref or array of Typeref.
 * 7. The extension schema's field schema's annotation keys must be in the "resourceKey" and/or "grpcService" namespaces.
 * 8. The extension schema's field annotation versionSuffix value has to match the versionSuffix value in "resourceKey"/"grpcService" annotation on the field schema.
 *
 * @author Yingjie Bi
 */
public class ExtensionSchemaValidationCmdLineApp
{
  private static final Logger _logger = LoggerFactory.getLogger(ExtensionSchemaValidationCmdLineApp.class);
  private static final Options _options = new Options();
  private static final String PDL = "pdl";
  private static final String RESOURCE_KEY_ANNOTATION_NAMESPACE = "resourceKey";
  private static final String GRPC_SERVICE_ANNOTATION_NAMESPACE = "grpcService";
  private static final String EXTENSIONS_SUFFIX = "Extensions";
  private static final String VERSION_SUFFIX = "versionSuffix";
  private static final Set<String> ALLOWED_EXTENSION_FIELD_ANNOTATIONS = new HashSet<>(Arrays.asList(
      // The "extension" and "grpcExtension" annotations are always allowed of course...
      EXTENSION_ANNOTATION_NAMESPACE,
      GRPC_EXTENSION_ANNOTATION_NAMESPACE,
      // The following are special-cased annotations, this list should be minimized
      // TODO: This is only present as a workaround, remove this once the feature gap is filled
      "ExcludedInGraphQL"
  ));

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

     // Validate that the schema is a named schema
     DataSchema topLevelDataSchema = topLevelDataSchemas.get(0);
     if (!(topLevelDataSchema instanceof NamedDataSchema))
     {
       throw new InvalidExtensionSchemaException("Invalid extension schema : " + inputFile.getAbsolutePath() + ", the schema is not a named schema.");
     }

     // Validate that the schema has the proper suffix in its name
     if (!((NamedDataSchema) topLevelDataSchema).getName().endsWith(EXTENSIONS_SUFFIX))
     {
       throw new InvalidExtensionSchemaException(
           "Invalid extension schema name: '" + ((NamedDataSchema) topLevelDataSchema).getName() + "'. The name of the extension schema must be <baseSchemaName> + 'Extensions'");
     }

     // Validate that the schema includes exactly one base schema
     List<NamedDataSchema> includes = ((RecordDataSchema) topLevelDataSchema).getInclude();
     if (includes.size() != 1)
     {
       throw new InvalidExtensionSchemaException("The extension schema: '" + ((NamedDataSchema) topLevelDataSchema).getName() + "' should include and only include the base schema");
     }

     // Validate that the schema's name is suffixed with the name of the base schema
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

      // Validate all the extension fields
      checkExtensionSchemaFields(extensionSchemaFields);
    }
  }

  private static void checkExtensionSchemaFields(List<RecordDataSchema.Field> extensionSchemaFields)
      throws InvalidExtensionSchemaException
  {
    for (RecordDataSchema.Field field : extensionSchemaFields)
    {
      // Check extension schema field annotations
      Map<String, Object> properties = field.getProperties();
      // First, assert that the extension field is annotated with anything
      if (properties.isEmpty())
      {
        throw new InvalidExtensionSchemaException("The extension schema field '"
            + field.getName() + "' must be annotated with 'extension' or 'grpcExtension'");
      }

      // Assert that there are no unexpected annotations on this field
      for (String annotationKey : properties.keySet())
      {
        if (!ALLOWED_EXTENSION_FIELD_ANNOTATIONS.contains(annotationKey))
        {
          throw new InvalidExtensionSchemaException("The extension schema field '"
              + field.getName() + "' is annotated with unexpected annotation '" + annotationKey + "'");
        }
      }

      // Validate the actual content/structure of the annotation value
      if (properties.containsKey(EXTENSION_ANNOTATION_NAMESPACE))
      {
        validateRestLiExtensionField(field);
      }
      if (properties.containsKey(GRPC_EXTENSION_ANNOTATION_NAMESPACE))
      {
        validateGrpcExtensionField(field);
      }
    }
  }

  private static void validateRestLiExtensionField(RecordDataSchema.Field field)
      throws InvalidExtensionSchemaException {
    Map<String, Object> properties = field.getProperties();

    // Validate the actual content/structure of the annotation value
    validateFieldAnnotation(properties.get(EXTENSION_ANNOTATION_NAMESPACE), new ExtensionSchemaAnnotation().schema());

    // Validate that the field has the appropriate type
    DataSchema injectedUrnType = getExtensionSchemaFieldSchema(field.getType());

    // Validate that the URN type has a resourceKey annotation with the corresponding suffix (if present)
    isAnnotatedWithResourceKey(injectedUrnType, properties);
  }

  private static void validateGrpcExtensionField(RecordDataSchema.Field field)
      throws InvalidExtensionSchemaException {
    Map<String, Object> properties = field.getProperties();

    // Validate the actual content/structure of the annotation value
    validateFieldAnnotation(properties.get(GRPC_EXTENSION_ANNOTATION_NAMESPACE), new GrpcExtensionAnnotation().schema());

    // Validate that the field has the appropriate type
    DataSchema injectedUrnType = getExtensionSchemaFieldSchema(field.getType());

    // Validate that the URN type has a grpcService annotation with the corresponding suffix (if present)
    isAnnotatedWithGrpcService(injectedUrnType, properties);
  }

  private static void validateFieldAnnotation(Object dataElement, DataSchema annotationSchema)
      throws InvalidExtensionSchemaException
  {
    ValidationOptions validationOptions =
        new ValidationOptions(RequiredMode.MUST_BE_PRESENT, CoercionMode.STRING_TO_PRIMITIVE, UnrecognizedFieldMode.DISALLOW);
    try
    {
      if (!(dataElement instanceof DataMap))
      {
        throw new InvalidExtensionSchemaException("Extension schema annotation is not a datamap!");
      }
      ValidationResult result = ValidateDataAgainstSchema.validate(dataElement, annotationSchema, validationOptions);
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
  }

  private static DataSchema getExtensionSchemaFieldSchema(DataSchema fieldSchema)
      throws InvalidExtensionSchemaException
  {
      DataSchema resolvedSchema = fieldSchema;
      if (resolvedSchema.getType() == DataSchema.Type.ARRAY)
      {
        resolvedSchema = ((ArrayDataSchema) resolvedSchema).getItems();
      }
      if (resolvedSchema.getType() != DataSchema.Type.TYPEREF)
      {
        throw new InvalidExtensionSchemaException("Field schema: '" + resolvedSchema.toString() + "' is not a TypeRef type.");
      }
      return resolvedSchema;
  }

  private static void isAnnotatedWithResourceKey(DataSchema fieldSchema, Map<String, Object> extensionAnnotations)
      throws InvalidExtensionSchemaException
  {
    Map<String, Object> fieldAnnotation = fieldSchema.getProperties();
    if (!fieldAnnotation.isEmpty() && fieldAnnotation.containsKey(RESOURCE_KEY_ANNOTATION_NAMESPACE))
    {
      // If the extension field explicitly references a version, validate that version exists on the resource key
      final DataMap extensionAnnotationMap = (DataMap) extensionAnnotations.getOrDefault(EXTENSION_ANNOTATION_NAMESPACE, new DataMap());
      if (extensionAnnotationMap.containsKey(VERSION_SUFFIX))
      {
        final DataList restLiResolvers = (DataList) fieldAnnotation.getOrDefault(RESOURCE_KEY_ANNOTATION_NAMESPACE, new DataList());
        checkExtensionVersionSuffixValue(restLiResolvers, (String) extensionAnnotationMap.get(VERSION_SUFFIX));
      }
    }
    else
    {
      throw new InvalidExtensionSchemaException("Field schema: " + fieldSchema.toString() + " is not annotated with 'resourceKey'");
    }
  }

  private static void isAnnotatedWithGrpcService(DataSchema fieldSchema, Map<String, Object> extensionAnnotations)
      throws InvalidExtensionSchemaException
  {
    Map<String, Object> fieldAnnotation = fieldSchema.getProperties();
    if (!fieldAnnotation.isEmpty() && fieldAnnotation.containsKey(GRPC_SERVICE_ANNOTATION_NAMESPACE))
    {
      // If the extension field explicitly references a version, validate that version exists on the gRPC service
      final DataMap extensionAnnotationMap = (DataMap) extensionAnnotations.getOrDefault(GRPC_EXTENSION_ANNOTATION_NAMESPACE, new DataMap());
      if (extensionAnnotationMap.containsKey(VERSION_SUFFIX))
      {
        final DataList grpcResolvers = (DataList) fieldAnnotation.getOrDefault(GRPC_SERVICE_ANNOTATION_NAMESPACE, new DataList());
        checkExtensionVersionSuffixValue(grpcResolvers, (String) extensionAnnotationMap.get(VERSION_SUFFIX));
      }
    }
    else
    {
      throw new InvalidExtensionSchemaException("Field schema: " + fieldSchema.toString() + " is not annotated with 'grpcService'");
    }
  }

  /**
   * Validates that a particular version suffix is defined in the provided resolver list.
   * @param resolvers List of Rest.li resolvers (resourceKey) or gRPC resolvers (grpcService)
   * @param extensionVersionSuffix The version suffix referenced by the extension schema field
   */
  private static void checkExtensionVersionSuffixValue(DataList resolvers, String extensionVersionSuffix)
      throws InvalidExtensionSchemaException
  {
    boolean versionSuffixValueIsValid = false;
    if (resolvers.size() < 2)
    {
      throw new InvalidExtensionSchemaException("resourceKey/grpcService annotation: "+ resolvers  + " does not have multiple versions");
    }
    for (int i = 1; i < resolvers.size(); i++)
    {
      DataMap resolverAnnotation = (DataMap) resolvers.get(i);
      String versionSuffixValueInResolver = (String) resolverAnnotation.get(VERSION_SUFFIX);
      if (extensionVersionSuffix.equals(versionSuffixValueInResolver))
      {
        versionSuffixValueIsValid = true;
        break;
      }
    }
    if (!versionSuffixValueIsValid)
    {
      throw new InvalidExtensionSchemaException("versionSuffix value: '" + extensionVersionSuffix +
          "' does not match the versionSuffix value which was defined in resourceKey/grpcService annotation");
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

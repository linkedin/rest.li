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

package com.linkedin.restli.tools.clientgen;


import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.StringArray;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.pegasus.generator.DataTemplateGenerator;
import com.linkedin.pegasus.generator.GeneratorResult;
import com.linkedin.restli.client.base.ActionRequestBuilderBase;
import com.linkedin.restli.client.base.BatchCreateRequestBuilderBase;
import com.linkedin.restli.client.base.BatchDeleteRequestBuilderBase;
import com.linkedin.restli.client.base.BatchGetRequestBuilderBase;
import com.linkedin.restli.client.base.BatchPartialUpdateRequestBuilderBase;
import com.linkedin.restli.client.base.BatchUpdateRequestBuilderBase;
import com.linkedin.restli.client.base.CreateRequestBuilderBase;
import com.linkedin.restli.client.base.DeleteRequestBuilderBase;
import com.linkedin.restli.client.base.FindRequestBuilderBase;
import com.linkedin.restli.client.base.GetAllRequestBuilderBase;
import com.linkedin.restli.client.base.GetRequestBuilderBase;
import com.linkedin.restli.client.base.PartialUpdateRequestBuilderBase;
import com.linkedin.restli.client.base.UpdateRequestBuilderBase;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CompoundKey.TypeInfo;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.TyperefUtils;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.FinderSchemaArray;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.ResourceSchemaArray;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.util.FileUtil;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;
import com.sun.codemodel.writer.FileCodeWriter;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates Java request builders from Rest.li idl.
 *
 * @author Eran Leshem
 */
public class RestRequestBuilderGenerator extends DataTemplateGenerator
{
  private static class JavaBinding
  {
    /**
     * schemaClass provides the class from which the schema can be obtained
     * schemaClass may be different from valueClass if the actual schema is a typeref
     * in the case of a typeref, the TyperefInfo class cannot be a valueClass but is a schemaClass
     */
    private JClass schemaClass;
    /**
     * value class is used for method params and return types
     */
    private JClass valueClass;
  }

  private static final String GENERATOR_REST_GENERATE_DATATEMPLATES = "generator.rest.generate.datatemplates";
  private static final Logger log = LoggerFactory.getLogger(RestRequestBuilderGenerator.class);
  private static final String NAME = "name";
  private static final String NAMESPACE = "namespace";

  private final JClass _voidClass = getCodeModel().ref(Void.class);
  private final JClass _fieldDefClass = getCodeModel().ref(FieldDef.class);
  private final JClass _resourceSpecClass = getCodeModel().ref(ResourceSpec.class);
  private final JClass _resourceSpecImplClass = getCodeModel().ref(ResourceSpecImpl.class);
  private final JClass _enumSetClass = getCodeModel().ref(EnumSet.class);
  private final JClass _resourceMethodClass = getCodeModel().ref(ResourceMethod.class);
  private final JClass _classClass = getCodeModel().ref(Class.class);
  private final JClass _objectClass = getCodeModel().ref(Object.class);
  private final HashSet<JClass> _generatedArrayClasses = new HashSet<JClass>();
  private final ClassLoader _classLoader;
  private final Config _config;

  private static final RestSpecCodec _codec = new RestSpecCodec();

  protected static class Config extends DataTemplateGenerator.Config
  {
    public Config(String resolverPath, String defaultPackage, Boolean generateImported, Boolean generateDataTemplates)
    {
      super(resolverPath, defaultPackage, generateImported);
      _generateDataTemplates = generateDataTemplates;
    }

    /**
     * @return true if the related data template source files will be generated as well, false otherwise.
     *         if null is assigned to this value, by default it returns true.
     */
    public boolean getGenerateDataTemplates()
    {
      return _generateDataTemplates == null || _generateDataTemplates;
    }

    private final Boolean _generateDataTemplates;
  }

  /**
   * @param args Usage: RestRequestBuilderGenerator targetDirectoryPath sourceFilePaths
   * @throws IOException if there are problems opening or deleting files
   */
  public static void main(String[] args) throws IOException
  {
    if (args.length < 2)
    {
      log.error("Usage: RestRequestBuilderGenerator targetDirectoryPath [sourceFile or sourceDirectory]+");
      System.exit(1);
    }

    final String generateImported = System.getProperty(GENERATOR_GENERATE_IMPORTED);
    final String generateDataTemplates = System.getProperty(GENERATOR_REST_GENERATE_DATATEMPLATES);
    run(System.getProperty(GENERATOR_RESOLVER_PATH),
        System.getProperty(GENERATOR_DEFAULT_PACKAGE),
        generateImported == null ? null : Boolean.parseBoolean(generateImported),
        generateDataTemplates == null ? null : Boolean.parseBoolean(generateDataTemplates),
        args[0],
        Arrays.copyOfRange(args, 1, args.length));
  }

  public static GeneratorResult run(String resolverPath,
                                    String defaultPackage,
                                    Boolean generateImported,
                                    Boolean generateDataTemplates,
                                    String targetDirectoryPath,
                                    String[] sources) throws IOException
  {
    final Config config = new Config(resolverPath, defaultPackage, generateImported, generateDataTemplates);
    final RestRequestBuilderGenerator generator = new RestRequestBuilderGenerator(config);

    return generator.generate(targetDirectoryPath, sources);
  }

  public RestRequestBuilderGenerator(Config config)
  {
    super();
    _config = config;
    _classLoader = getClassLoader();
  }

  @Override
  protected Config getConfig()
  {
    return _config;
  }

  /**
   * Parses REST IDL files and generates Request builders from them.
   * @param sources path to IDL files or directories
   * @param targetDirectoryPath path to target root java source directory
   * @return a result that includes collection of files accessed, would have generated and actually modified.
   * @throws IOException if there are problems opening or deleting files.
   */
  private GeneratorResult generate(String targetDirectoryPath, String[] sources) throws IOException
  {
    List<File> sourceFiles = parseSources(sources);

    if(getMessage().length() > 0)
    {
      throw new IOException(getMessage().toString());
    }

    File targetDirectory = new File(targetDirectoryPath);
    List<File> targetFiles = targetFiles(targetDirectory);

    List<File> modifiedFiles;
    if (upToDate(sourceFiles, targetFiles))
    {
      modifiedFiles = Collections.emptyList();
      log.info("Target files are up-to-date: " + targetFiles);
    }
    else
    {
      modifiedFiles = targetFiles;
      log.info("Generating " + targetFiles.size() + " files: " + targetFiles);
      getCodeModel().build(new FileCodeWriter(targetDirectory, true));
    }
    return new Result(sourceFiles, targetFiles, modifiedFiles);
  }

  @Override
  protected boolean hideClass(JDefinedClass clazz)
  {
    if (getConfig().getGenerateDataTemplates() || _generatedArrayClasses.contains(clazz))
    {
      try
      {
        Class.forName(clazz.fullName(), false, _classLoader);
      }
      catch (ClassNotFoundException e)
      {
        return false;
      }
    }
    return super.hideClass(clazz);
  }

  @Override
  protected List<File> parseSources(String[] sourcePaths)
  {
    initializeDefaultPackage();
    initSchemaResolver();

    JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    jsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    List<File> sourceFiles = new ArrayList<File>();

    for(String sourcePath : sourcePaths)
    {
      File source = new File(sourcePath);
      if (! source.exists())
      {
        getMessage().append("IDL file or directory doesn't exist: ").append(source.getAbsolutePath());
      }
      else
      {
        File[] sources;

        if (source.isDirectory())
        {
          FileUtil.FileExtensionFilter filter = new FileUtil.FileExtensionFilter(RestConstants.RESOURCE_MODEL_FILENAME_EXTENSION);
          List<File> sourceFilesInDirectory = FileUtil.listFiles(source, filter);
          sources = sourceFilesInDirectory.toArray(new File[0]);
        }
        else
        {
          sources = new File[] { source };
        }

        for (File sourceFile : sources)
        {
          sourceFiles.add(sourceFile);
          try
          {
            ResourceSchema resource = _codec.readResourceSchema(new FileInputStream(sourceFile));
            pushCurrentLocation(new FileDataSchemaLocation(sourceFile));
            generateResourceFacade(resource, sourceFile.getAbsolutePath(), new HashMap<String, JClass>());
            popCurrentLocation();
          }
          catch (JClassAlreadyExistsException e)
          {
            // should never occur
            throw new IllegalStateException("Unexpected exception parsing " + sourceFile + ", " + e.getExistingClass().fullName() + " exists", e);
          }
          catch(JsonParseException e)
          {
            getMessage().append("Error parsing json file [").append(sourceFile.getAbsolutePath()).append("] [").
                            append(e.getMessage()).append(']');
          }
          catch(IOException e)
          {
            getMessage().append("Error processing file [").append(sourceFile.getAbsolutePath()).append(']').append(e.getMessage());
          }
        }
      }
    }

    appendSourceFilesFromSchemaResolver(sourceFiles);
    return sourceFiles;
  }

  private void annotate(JDefinedClass requestBuilderClass, String sourceFile)
  {
    annotate(requestBuilderClass, "Request Builder", sourceFile);
  }

  private JDefinedClass generateResourceFacade(ResourceSchema resource,
                                               String sourceFile,
                                               Map<String, JClass> pathKeyTypes)
          throws JClassAlreadyExistsException, IOException
  {
    ValidationResult validationResult = ValidateDataAgainstSchema.validate(resource.data(), resource.schema(),
                                                                           new ValidationOptions(
                                                                                           RequiredMode.MUST_BE_PRESENT));
    if (!validationResult.isValid())
    {
        throw new IllegalArgumentException(String.format(
                "Resource validation error.  Resource File '%s', Error Details '%s'",
                sourceFile,
                validationResult.toString()));
    }

    String resourceName = capitalize(resource.getName());

    String packageName = resource.getNamespace();
    JPackage clientPackage = (packageName == null || packageName.isEmpty()) ? getPackage() : getPackage(packageName);

    JDefinedClass facadeClass = clientPackage._class(resourceName + "Builders");
    annotate(facadeClass, sourceFile);

    JFieldVar baseUriField = facadeClass.field(JMod.PRIVATE | JMod.FINAL, String.class,
                                               "_baseUriTemplate");

    String resourcePath = getResourcePath(resource.getPath());

    // make the original resource name available via a private final static variable.
    JFieldVar originalResourceField = facadeClass.field(JMod.PRIVATE | JMod.STATIC |JMod.FINAL, String.class,
                                                        "ORIGINAL_RESOURCE_NAME");

    originalResourceField.init(JExpr.lit(resourcePath));

    JMethod constructor = facadeClass.constructor(JMod.PUBLIC);
    constructor.body().assign(baseUriField, JExpr.lit(resourcePath));

    JMethod overrideConstructor = facadeClass.constructor(JMod.PUBLIC);
    JVar resourceNameParam = overrideConstructor.param(_stringClass, "primaryResourceName");
    if (resourcePath.contains("/"))
    {
      overrideConstructor.body().assign(baseUriField, JExpr.lit(resourcePath)
              .invoke("replaceFirst").arg(JExpr.lit("[^/]*/")).arg(resourceNameParam.plus(JExpr.lit("/"))));
    }
    else
    {
      overrideConstructor.body().assign(baseUriField, resourceNameParam);
    }

    JMethod primaryResourceGetter = facadeClass.method(JMod.PUBLIC | JMod.STATIC, String.class, "getPrimaryResource");
    primaryResourceGetter.body()._return(originalResourceField);

    List<String> pathKeys = getPathKeys(resourcePath);

    JClass keyTyperefClass = null;
    JClass keyClass;
    JClass keyKeyClass = null;
    JClass keyParamsClass = null;
    Class<?> resourceSchemaClass;
    Map<String, AssocKeyTypeInfo> assocKeyTypeInfos = Collections.emptyMap();
    StringArray supportsList=null;
    RestMethodSchemaArray restMethods = null;
    FinderSchemaArray finders = null;
    ResourceSchemaArray subresources = null;
    ActionSchemaArray resourceActions = null;
    ActionSchemaArray entityActions = null;

    if (resource.getCollection() != null)
    {
      resourceSchemaClass = CollectionSchema.class;
      CollectionSchema collection = resource.getCollection();
      String keyName = collection.getIdentifier().getName();
      // In case of collection with a simple key, return the one specified by "type" in
      // the "identifier". Otherwise, get both "type" and "params", and return
      // ComplexKeyResource parameterized by those two.
      if (collection.getIdentifier().getParams() == null)
      {
        keyClass = getJavaBindingType(collection.getIdentifier().getType(), facadeClass).valueClass;
        JClass declaredClass = getClassRefForSchema(RestSpecCodec.textToSchema(collection.getIdentifier().getType(), getSchemaResolver()), facadeClass);
        if(!declaredClass.equals(keyClass))
        {
          keyTyperefClass = declaredClass;
        }
      }
      else
      {
        keyKeyClass = getJavaBindingType(collection.getIdentifier().getType(), facadeClass).valueClass;
        keyParamsClass = getJavaBindingType(collection.getIdentifier().getParams(), facadeClass).valueClass;
        keyClass = getCodeModel().ref(ComplexResourceKey.class).narrow(keyKeyClass, keyParamsClass);
      }
      pathKeyTypes.put(keyName, keyClass);
      supportsList = collection.getSupports();
      restMethods = collection.getMethods();
      finders = collection.getFinders();
      subresources = collection.getEntity().getSubresources();
      resourceActions = collection.getActions();
      entityActions = collection.getEntity().getActions();
    }
    else if (resource.getAssociation() != null)
    {
      resourceSchemaClass = AssociationSchema.class;
      AssociationSchema association = resource.getAssociation();
      keyClass = getCodeModel().ref(CompoundKey.class);
      supportsList = association.getSupports();
      restMethods = association.getMethods();
      finders = association.getFinders();
      subresources = association.getEntity().getSubresources();
      resourceActions = association.getActions();
      entityActions = association.getEntity().getActions();

      assocKeyTypeInfos = generateAssociationKey(facadeClass, association);

      for(Map.Entry<String, AssocKeyTypeInfo> entry: assocKeyTypeInfos.entrySet())
      {
        pathKeyTypes.put(entry.getKey(), entry.getValue().getBindingType());
      }
    }
    else if (resource.getSimple() != null)
    {
      resourceSchemaClass = SimpleSchema.class;
      SimpleSchema simpleSchema = resource.getSimple();
      keyClass = _voidClass;
      supportsList = simpleSchema.getSupports();
      restMethods = simpleSchema.getMethods();
      subresources = simpleSchema.getEntity().getSubresources();
      resourceActions = simpleSchema.getActions();
    }
    else if (resource.getActionsSet() != null)
    {
      resourceSchemaClass = ActionsSetSchema.class;
      ActionsSetSchema actionsSet = resource.getActionsSet();
      resourceActions = actionsSet.getActions();

      keyClass = _voidClass;
    }
    else
    {
      throw new IllegalArgumentException("unsupported resource type for resource: '" + resourceName + '\'');
    }

    JFieldVar resourceSpecField = facadeClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, _resourceSpecClass, "_resourceSpec");
    if (resourceSchemaClass == CollectionSchema.class ||
        resourceSchemaClass == AssociationSchema.class ||
        resourceSchemaClass == SimpleSchema.class)
    {
      JClass schemaClass = getJavaBindingType(resource.getSchema(), null).schemaClass;

      Set<ResourceMethod> supportedMethods = getSupportedMethods(supportsList);
      JInvocation supportedMethodsExpr;
      if (supportedMethods.isEmpty())
      {
        supportedMethodsExpr = _enumSetClass.staticInvoke("noneOf").arg(_resourceMethodClass.dotclass());
      }
      else
      {
        supportedMethodsExpr = _enumSetClass.staticInvoke("of");
        for (ResourceMethod resourceMethod: supportedMethods)
        {
          validateResourceMethod(resourceSchemaClass, resourceName, resourceMethod);
          supportedMethodsExpr.arg(_resourceMethodClass.staticRef(resourceMethod.name()));
        }
      }

      JBlock staticInit = facadeClass.init();

      JVar methodSchemaMap = methodMetadataMapInit(facadeClass, resourceActions, entityActions,
                                                   staticInit);
      JVar responseSchemaMap = responseMetadataMapInit(facadeClass, resourceActions, entityActions,
                                                       staticInit);

      if (resourceSchemaClass == CollectionSchema.class ||
          resourceSchemaClass == AssociationSchema.class)
      {
        JClass assocKeyClass = getCodeModel().ref(TypeInfo.class);
        JClass hashMapClass = getCodeModel().ref(HashMap.class).narrow(_stringClass, assocKeyClass);
        JVar keyPartsVar = staticInit.decl(hashMapClass, "keyParts").init(JExpr._new(hashMapClass));
        for (Map.Entry<String, AssocKeyTypeInfo> typeInfoEntry : assocKeyTypeInfos.entrySet())
        {
          AssocKeyTypeInfo typeInfo = typeInfoEntry.getValue();
          JInvocation typeArg = JExpr._new(assocKeyClass)
            .arg(typeInfo.getBindingType().dotclass())
            .arg(typeInfo.getDeclaredType().dotclass());
          staticInit.add(keyPartsVar.invoke("put").arg(typeInfoEntry.getKey()).arg(typeArg));
        }

        staticInit.assign(resourceSpecField,
                          JExpr._new(_resourceSpecImplClass)
                                  .arg(supportedMethodsExpr)
                                  .arg(methodSchemaMap)
                                  .arg(responseSchemaMap)
                                  .arg(keyTyperefClass == null ? keyClass.dotclass() : keyTyperefClass.dotclass())
                                  .arg(keyKeyClass == null ? JExpr._null() : keyKeyClass.dotclass())
                                  .arg(keyKeyClass == null ? JExpr._null() : keyParamsClass.dotclass())
                                  .arg(schemaClass.dotclass())
                                  .arg(keyPartsVar));
      }
      else //simple schema
      {
        staticInit.assign(resourceSpecField,
                          JExpr._new(_resourceSpecImplClass)
                              .arg(supportedMethodsExpr)
                              .arg(methodSchemaMap)
                              .arg(responseSchemaMap)
                              .arg(schemaClass.dotclass()));
      }

      generateBasicMethods(facadeClass,
                          baseUriField,
                          keyClass,
                          schemaClass,
                          supportedMethods,
                          restMethods,
                          resourceSpecField,
                          resourceName,
                          pathKeys,
                          pathKeyTypes);

      if (resourceSchemaClass == CollectionSchema.class ||
          resourceSchemaClass == AssociationSchema.class)
      {
        generateFinders(facadeClass,
                        baseUriField,
                        finders,
                        keyClass,
                        schemaClass,
                        assocKeyTypeInfos,
                        resourceSpecField,
                        resourceName,
                        pathKeys,
                        pathKeyTypes);
      }

      generateSubResources(sourceFile, subresources, pathKeyTypes);
    }
    else //action set
    {
      JBlock staticInit = facadeClass.init();
      JInvocation supportedMethodsExpr = _enumSetClass.staticInvoke("noneOf").arg(_resourceMethodClass.dotclass());
      JVar methodSchemaMap = methodMetadataMapInit(facadeClass, resourceActions, entityActions,
                                                   staticInit);
      JVar responseSchemaMap = responseMetadataMapInit(facadeClass, resourceActions, entityActions,
                                                       staticInit);
      staticInit.assign(resourceSpecField,
                        JExpr._new(_resourceSpecImplClass)
                                .arg(supportedMethodsExpr)
                                .arg(methodSchemaMap)
                                .arg(responseSchemaMap)
                                .arg(keyClass.dotclass())
                                .arg(JExpr._null())
                                .arg(JExpr._null())
                                .arg(JExpr._null())
                                .arg(getCodeModel().ref(Collections.class).staticInvoke(
                                        "<String, Class<?>>emptyMap")));
    }

    generateActions(facadeClass,
                    baseUriField,
                    resourceActions,
                    entityActions,
                    keyClass,
                    resourceSpecField,
                    resourceName,
                    pathKeys,
                    pathKeyTypes);


    generateClassJavadoc(facadeClass, resource);

    return facadeClass;
  }

  private void validateResourceMethod(Class<?> resourceSchemaClass, String resourceName, ResourceMethod resourceMethod)
  {
    if (resourceSchemaClass == SimpleSchema.class && !RestConstants.SIMPLE_RESOURCE_METHODS.contains(resourceMethod))
    {
      throw new IllegalArgumentException(
          String.format(
              "'%s' is not a supported method on resource: '%s'", resourceMethod.toString(), resourceName));
    }
  }

  private JVar responseMetadataMapInit(JDefinedClass facadeClass,
                                       ActionSchemaArray resourceActions,
                                       ActionSchemaArray entityActions,
                                       JBlock staticInit) // lets work on this first...
  {
    JClass MetadataMapClass = getCodeModel().ref(HashMap.class).narrow(_stringClass, getCodeModel().ref(DynamicRecordMetadata.class));
    JVar responseMetadataMap = staticInit.decl(MetadataMapClass, "responseMetadataMap").init(JExpr._new(MetadataMapClass));

    // get all actions into a single ActionSchemaArray
    int resourceActionsSize = resourceActions == null ? 0 : resourceActions.size();
    int entityActionsSize = entityActions == null ? 0 : entityActions.size();
    ActionSchemaArray allActionSchema = new ActionSchemaArray(resourceActionsSize + entityActionsSize);
    allActionSchema.addAll(resourceActions == null ? new ActionSchemaArray() : resourceActions);
    allActionSchema.addAll(entityActions == null ? new ActionSchemaArray() : entityActions);

    String returnName = "value";

    for(ActionSchema actionSchema : allActionSchema)
    {
      String methodName = actionSchema.getName();
      JInvocation returnFieldDefs;
      if (actionSchema.hasReturns())
      {
        JInvocation returnFieldDef = createFieldDef(returnName, actionSchema.getReturns(), facadeClass);
        returnFieldDefs = getCodeModel().ref(Collections.class).staticInvoke("singletonList").arg(returnFieldDef);
      }
      else
      {
        returnFieldDefs = getCodeModel().ref(Collections.class).staticInvoke("<FieldDef<?>>emptyList");
      }
      JInvocation returnMetadata = createMetadata(methodName, returnFieldDefs);
      staticInit.add(responseMetadataMap.invoke("put").arg(methodName).arg(returnMetadata));
    }

    return responseMetadataMap;
  }

  private JVar methodMetadataMapInit(JDefinedClass facadeClass,
                                     ActionSchemaArray resourceActions,
                                     ActionSchemaArray entityActions,
                                     JBlock staticInit)
  {
    // CreateMetadata (only for actions right now)
    JClass MetadataMapClass = getCodeModel().ref(HashMap.class).narrow(_stringClass, getCodeModel().ref(DynamicRecordMetadata.class));
    JVar requestMetadataMap = staticInit.decl(MetadataMapClass, "requestMetadataMap").init(JExpr._new(MetadataMapClass));
    JClass fieldDefListClass = getCodeModel().ref(ArrayList.class).narrow(getCodeModel().ref(FieldDef.class).narrow(getCodeModel().ref(Object.class).wildcard()));

    // get all actions into a single ActionSchemaArray
    int resourceActionsSize = resourceActions == null ? 0 : resourceActions.size();
    int entityActionsSize = entityActions == null ? 0 : entityActions.size();
    ActionSchemaArray allActionSchema = new ActionSchemaArray(resourceActionsSize + entityActionsSize);
    allActionSchema.addAll(resourceActions == null ? new ActionSchemaArray() : resourceActions);
    allActionSchema.addAll(entityActions == null ? new ActionSchemaArray() : entityActions);

    for(ActionSchema actionSchema : allActionSchema)
    {
      String varName = actionSchema.getName() + "Params";
      JVar currMethodParams = staticInit.decl(fieldDefListClass, varName).init(JExpr._new(fieldDefListClass));

      ParameterSchemaArray parameters = actionSchema.getParameters();
      for (ParameterSchema parameterSchema : parameters == null? new ParameterSchemaArray() : parameters)
      {
        JInvocation fieldDefParam = createFieldDef(parameterSchema.getName(), parameterSchema.getType(), facadeClass);
        staticInit.add(currMethodParams.invoke("add").arg(fieldDefParam));
      }
      String methodName = actionSchema.getName();
      JInvocation newSchema = createMetadata(methodName, currMethodParams);
      staticInit.add(requestMetadataMap.invoke("put").arg(methodName).arg(newSchema));
    }
    return requestMetadataMap;
  }

  /**
   * Helper class to create FieldDefs
   *
   * @param name the fieldDef name
   * @param type the fieldDef type
   * @param parent the class the FieldDef is being created in
   * @return JInvocation of the creation of the FieldDef
   */
  private JInvocation createFieldDef(String name, String type, JDefinedClass parent)
  {
    JavaBinding binding = getJavaBindingType(type, parent);
    JExpression schema = getCodeModel().ref(DataTemplateUtil.class).staticInvoke("getSchema").arg(binding.schemaClass.dotclass());
    JInvocation fieldDefInvocation = JExpr._new(getCodeModel().ref(FieldDef.class).narrow(binding.valueClass))
        .arg(name).arg(binding.valueClass.dotclass())
        .arg(schema);
    return fieldDefInvocation;
  }

  private JInvocation createMetadata(String name, JExpression fieldDefs)
  {
    return JExpr._new(getCodeModel().ref(DynamicRecordMetadata.class)).arg(name).arg(fieldDefs);
  }

  /**
   * Get the key class. In case of collection with a simple key, return the one specified by "type" in the
   * "identifier". Otherwise, get both "type" and "params", and return ComplexKeyResource parameterized by
   * those two.
   *
   * @param collection
   * @param facadeClass
   * @return JClass as described above
   */
  private JClass getKeyClass(CollectionSchema collection, JDefinedClass facadeClass) {
    JClass keyClass = getJavaBindingType(collection.getIdentifier().getType(), facadeClass).valueClass;
    if (collection.getIdentifier().getParams() == null)
    {
      return keyClass;
    }

    JClass paramsClass = getJavaBindingType(collection.getIdentifier().getParams(), facadeClass).valueClass;

    return getCodeModel().ref(ComplexResourceKey.class).narrow(keyClass, paramsClass);
  }

  private static List<String> getPathKeys(String basePath)
  {
    UriTemplate template = new UriTemplate(basePath);
    return template.getTemplateVariables();
  }

  private void generateSubResources(String sourceFile,
                                    ResourceSchemaArray subresources,
                                    Map<String, JClass> pathKeyTypes)
          throws JClassAlreadyExistsException, IOException
  {
    if (subresources == null)
    {
      return;
    }

    for (ResourceSchema resource : subresources)
    {
      generateResourceFacade(resource, sourceFile, pathKeyTypes);
    }
  }

  private void generateFinders(JDefinedClass facadeClass,
                               JFieldVar baseUriField,
                               FinderSchemaArray finderSchemas,
                               JClass keyClass,
                               JClass valueClass,
                               Map<String, AssocKeyTypeInfo> assocKeys,
                               JVar resourceSpecField,
                               String resourceName,
                               List<String> pathKeys,
                               Map<String, JClass> pathKeyTypes)
          throws JClassAlreadyExistsException
  {
    if (finderSchemas != null)
    {
      JClass baseBuilderClass = getCodeModel().ref(FindRequestBuilderBase.class).narrow(keyClass, valueClass);

      for (FinderSchema finder : finderSchemas)
      {
        String finderName = finder.getName();
        String builderName = capitalize(resourceName) + "FindBy" + capitalize(finderName) + "Builder";
        JDefinedClass finderBuilderClass = generateDerivedBuilder(baseBuilderClass,
                                                                  valueClass,
                                                                  finderName,
                                                                  builderName,
                                                                  facadeClass.getPackage());

        JMethod finderMethod = facadeClass.method(JMod.PUBLIC, finderBuilderClass,
                                                "findBy" + capitalize(finderName));

        finderMethod.body()._return(JExpr._new(finderBuilderClass).arg(baseUriField).arg(resourceSpecField));

        Set<String> finderKeys = new HashSet<String>();
        if (finder.getAssocKey() != null)
        {
          finderKeys.add(finder.getAssocKey());
        }
        if (finder.getAssocKeys() != null)
        {
          for (String assocKey : finder.getAssocKeys())
          {
            finderKeys.add(assocKey);
          }
        }

        generatePathKeyBindingMethods(pathKeys, finderBuilderClass, pathKeyTypes);

        generateAssocKeyBindingMethods(assocKeys, finderBuilderClass, finderKeys);

        if (finder.getParameters() != null)
        {
          generateQueryParamBindingMethods(facadeClass, finder.getParameters(), finderBuilderClass);
        }

        //process the metadata schema file
        if (finder.getMetadata() != null)
        {
          String metadataClass = finder.getMetadata().getType();
          getJavaBindingType(metadataClass, facadeClass);
        }

        generateClassJavadoc(finderBuilderClass, finder);
        generateFactoryMethodJavadoc(finderMethod, finder);
      }
    }
  }

  private void generateQueryParamBindingMethods(JDefinedClass facadeClass,
                                                ParameterSchemaArray parameters,
                                                JDefinedClass derivedBuilderClass)
  {
    for (ParameterSchema param : parameters)
    {
      if ("array".equals(param.getType()))
      {
        final JClass paramItemsClass = getJavaBindingType(param.getItems(), facadeClass).valueClass;
        final JClass paramClass = getCodeModel().ref(Iterable.class).narrow(paramItemsClass);
        generateQueryParamSetMethod(derivedBuilderClass, param, paramClass);
        generateQueryParamAddMethod(derivedBuilderClass, param, paramItemsClass);
      }
      else
      {
        final DataSchema typeSchema = RestSpecCodec.textToSchema(param.getType(), getSchemaResolver());
        final JClass paramClass = getJavaBindingType(typeSchema, facadeClass).valueClass;
        generateQueryParamSetMethod(derivedBuilderClass, param, paramClass);

        // we deprecate the "items" field from ParameterSchema, which generates Iterable<Foo> in the builder
        // instead, we use the standard way to represent arrays, which generates FooArray
        // for backwards compatibility, add the method with Iterable<Foo> parameter
        if (typeSchema instanceof ArrayDataSchema)
        {
          final DataSchema itemsSchema = ((ArrayDataSchema) typeSchema).getItems();
          final JClass paramItemsClass = getJavaBindingType(itemsSchema, facadeClass).valueClass;
          final JClass iterableItemsClass = getCodeModel().ref(Iterable.class).narrow(paramItemsClass);
          generateQueryParamSetMethod(derivedBuilderClass, param, iterableItemsClass);
          generateQueryParamAddMethod(derivedBuilderClass, param, paramItemsClass);
        }
      }
    }
  }

  private JDefinedClass generateDerivedBuilder(JClass baseBuilderClass,
                                               JClass valueClass,
                                               String finderName,
                                               String derivedBuilderName,
                                               JPackage clientPackage)
          throws JClassAlreadyExistsException
  {
    JDefinedClass derivedBuilderClass = clientPackage._class(JMod.PUBLIC, derivedBuilderName);
    annotate(derivedBuilderClass, null);
    derivedBuilderClass._extends(baseBuilderClass.narrow(derivedBuilderClass));
    JMethod derivedBuilderConstructor = derivedBuilderClass.constructor(JMod.PUBLIC);
    JVar uriParam = derivedBuilderConstructor.param(_stringClass, "baseUriTemplate");
    JVar resourceSpecParam = derivedBuilderConstructor.param(_resourceSpecClass, "resourceSpec");
    derivedBuilderConstructor.body().invoke(SUPER).arg(uriParam).arg(valueClass.dotclass()).arg(resourceSpecParam);
    if (finderName != null)
    {
      derivedBuilderConstructor.body().add(JExpr._super().invoke("name").arg(finderName));
    }

    return derivedBuilderClass;
  }

  private void generateAssocKeyBindingMethods(Map<String, AssocKeyTypeInfo> assocKeys,
                                              JDefinedClass finderBuilderClass,
                                              Set<String> finderKeys)
  {
    StringBuilder errorBuilder = new StringBuilder();

    for (String assocKey : finderKeys)
    {
      JClass assocKeyClass = assocKeys.get(assocKey).getBindingType();
      if (assocKeyClass == null)
      {
        errorBuilder.append(String.format("assocKey %s in finder is not found\n", assocKey));
        continue;
      }

      JMethod keyMethod = finderBuilderClass.method(JMod.PUBLIC, finderBuilderClass, nameCamelCase(assocKey + "Key"));
      JVar keyMethodParam = keyMethod.param(assocKeyClass, "key");
      keyMethod.body().add(JExpr._super().invoke("assocKey").arg(assocKey).arg(keyMethodParam));
      keyMethod.body()._return(JExpr._this());
    }

    if (errorBuilder.length() > 0)
    {
      throw new IllegalArgumentException(errorBuilder.toString());
    }
  }

  private void generatePathKeyBindingMethods(List<String> pathKeys,
                                             JDefinedClass builderClass,
                                             Map<String, JClass> pathKeyTypes)
  {
    for (String pathKey : pathKeys)
    {
      JMethod keyMethod = builderClass.method(JMod.PUBLIC, builderClass, nameCamelCase(pathKey + "Key"));
      JVar keyMethodParam = keyMethod.param(pathKeyTypes.get(pathKey), "key");
      keyMethod.body().add(JExpr._super().invoke("pathKey").arg(pathKey).arg(keyMethodParam));
      keyMethod.body()._return(JExpr._this());
    }
  }

  private void generateActions(JDefinedClass facadeClass,
                               JFieldVar baseUriField,
                               ActionSchemaArray resourceActions,
                               ActionSchemaArray entityActions,
                               JClass keyClass,
                               JVar resourceSpecField,
                               String resourceName,
                               List<String> pathKeys,
                               Map<String, JClass> pathKeyTypes)
          throws JClassAlreadyExistsException
  {
    if (resourceActions != null)
    {
      for (ActionSchema action : resourceActions)
      {
        generateActionMethod(facadeClass, baseUriField, _voidClass, action, resourceSpecField, resourceName, pathKeys,
                             pathKeyTypes);
      }
    }

    if (entityActions != null)
    {
      for (ActionSchema action : entityActions)
      {
        generateActionMethod(facadeClass, baseUriField, keyClass, action, resourceSpecField, resourceName, pathKeys,
                             pathKeyTypes);
      }
    }
  }

  private void generateActionMethod(JDefinedClass facadeClass,
                                    JFieldVar baseUriField,
                                    JClass keyClass,
                                    ActionSchema action,
                                    JVar resourceSpecField,
                                    String resourceName, List<String> pathKeys,
                                    Map<String, JClass> pathKeyTypes)
          throws JClassAlreadyExistsException
  {
    JClass returnType = getActionReturnType(facadeClass, action.getReturns());
    JClass vanillaActionBuilderClass = getCodeModel().ref(ActionRequestBuilderBase.class).narrow(keyClass,
                                                                                returnType);
    String actionName = action.getName();

    String actionBuilderClassName = capitalize(resourceName) + "Do" + capitalize(actionName) + "Builder";
    JDefinedClass actionBuilderClass = facadeClass.getPackage()._class(JMod.PUBLIC, actionBuilderClassName);
    annotate(actionBuilderClass, null);
    actionBuilderClass._extends(vanillaActionBuilderClass.narrow(actionBuilderClass));
    JMethod actionBuilderConstructor = actionBuilderClass.constructor(JMod.PUBLIC);
    JVar uriParam = actionBuilderConstructor.param(_stringClass, "baseUriTemplate");
    JVar classParam = actionBuilderConstructor.param(getCodeModel().ref(Class.class).narrow(returnType), "returnClass");
    JVar resourceSpecParam = actionBuilderConstructor.param(_resourceSpecClass, "resourceSpec");
    actionBuilderConstructor.body().invoke(SUPER).arg(uriParam).arg(classParam).arg(
            resourceSpecParam);
    actionBuilderConstructor.body().add(JExpr._super().invoke("name").arg(actionName));

    JMethod actionMethod = facadeClass.method(JMod.PUBLIC, actionBuilderClass, "action" + capitalize(actionName));
    actionMethod.body()._return(JExpr._new(actionBuilderClass).arg(baseUriField).arg(returnType.dotclass()).arg(resourceSpecField));

    if (action.getParameters() != null)
    {
      for (ParameterSchema param : action.getParameters())
      {
        String paramName = param.getName();
        boolean isOptional = param.isOptional() == null ? false : param.isOptional();
        JavaBinding binding = getJavaBindingType(param.getType(), facadeClass);

        JMethod typesafeMethod = actionBuilderClass.method(JMod.PUBLIC, actionBuilderClass,
                                                           "param" + capitalize(paramName));
        JVar typesafeMethodParam = typesafeMethod.param(binding.valueClass, "value");

        JClass dataTemplateUtil = getCodeModel().ref(DataTemplateUtil.class);
        JExpression dataSchema = dataTemplateUtil.staticInvoke("getSchema").arg(binding.schemaClass.dotclass());

        typesafeMethod.body().add(JExpr._super().invoke(isOptional ? "setParam" : "setReqParam")
                                          .arg(resourceSpecField
                                                       .invoke("getRequestMetadata").arg(actionName)
                                                       .invoke("getFieldDef").arg(paramName))
                                          .arg(typesafeMethodParam));
        typesafeMethod.body()._return(JExpr._this());
      }
    }

    generatePathKeyBindingMethods(pathKeys, actionBuilderClass, pathKeyTypes);

    generateClassJavadoc(actionBuilderClass, action);
    generateFactoryMethodJavadoc(actionMethod, action);
  }

  private void generateBasicMethods(JDefinedClass facadeClass,
                                    JFieldVar baseUriField,
                                    JClass keyClass,
                                    JClass valueClass,
                                    Set<ResourceMethod> supportedMethods,
                                    RestMethodSchemaArray restMethods,
                                    JVar resourceSpecField,
                                    String resourceName,
                                    List<String> pathKeys,
                                    Map<String, JClass> pathKeyTypes)
          throws JClassAlreadyExistsException
  {
    final Map<ResourceMethod, RestMethodSchema> schemaMap = new HashMap<ResourceMethod, RestMethodSchema>();
    if (restMethods != null)
    {
      for (RestMethodSchema restMethod : restMethods)
      {
        schemaMap.put(ResourceMethod.fromString(restMethod.getMethod()), restMethod);
      }
    }

    Map<ResourceMethod, Class<?>> crudBuilderClasses = new HashMap<ResourceMethod, Class<?>>();
    crudBuilderClasses.put(ResourceMethod.CREATE, CreateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.GET, GetRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.UPDATE, UpdateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.PARTIAL_UPDATE, PartialUpdateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.DELETE, DeleteRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.BATCH_CREATE, BatchCreateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.BATCH_GET, BatchGetRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.BATCH_UPDATE, BatchUpdateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.BATCH_PARTIAL_UPDATE, BatchPartialUpdateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.BATCH_DELETE, BatchDeleteRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.GET_ALL, GetAllRequestBuilderBase.class);

    for (Map.Entry<ResourceMethod, Class<?>> entry : crudBuilderClasses.entrySet())
    {
      ResourceMethod method = entry.getKey();
      if (supportedMethods.contains(method))
      {
        String methodName = normalizeUnderscores(method.toString());

        JClass builderClass = getCodeModel().ref(entry.getValue()).narrow(keyClass, valueClass);
        JDefinedClass derivedBuilder = generateDerivedBuilder(builderClass, valueClass, null, resourceName + nameCapsCase(methodName) + "Builder",
                                                              facadeClass.getPackage());
        generatePathKeyBindingMethods(pathKeys, derivedBuilder, pathKeyTypes);

        JMethod factoryMethod = facadeClass.method(JMod.PUBLIC, derivedBuilder, nameCamelCase(methodName));
        factoryMethod.body()._return(JExpr._new(derivedBuilder).arg(baseUriField).arg(resourceSpecField));

        final RestMethodSchema schema = schemaMap.get(method);
        if (schema != null)
        {
          if (schema.hasParameters())
          {
            generateQueryParamBindingMethods(facadeClass, schema.getParameters(), derivedBuilder);
          }
          generateClassJavadoc(derivedBuilder, schema);
          generateFactoryMethodJavadoc(factoryMethod, schema);
        }
      }
    }
  }

  private static class AssocKeyTypeInfo
  {
    private final JClass _bindingType;
    private final JClass _declaredType;

    public AssocKeyTypeInfo(JClass bindingType, JClass declaredType)
    {
      _bindingType = bindingType;
      _declaredType = declaredType;
    }

    public JClass getBindingType()
    {
      return _bindingType;
    }

    public JClass getDeclaredType()
    {
      return _declaredType;
    }
  }

  private Map<String, AssocKeyTypeInfo> generateAssociationKey(JDefinedClass facadeClass,
                                      AssociationSchema associationSchema)
          throws JClassAlreadyExistsException
  {
    JDefinedClass typesafeKeyClass = facadeClass._class(JMod.PUBLIC | JMod.STATIC, "Key");
    typesafeKeyClass._extends(CompoundKey.class);
    Map<String, AssocKeyTypeInfo> assocKeyTypeInfos = new HashMap<String, AssocKeyTypeInfo>();
    for (AssocKeySchema assocKey : associationSchema.getAssocKeys())
    {
      String name = assocKey.getName();
      JClass clazz = getJavaBindingType(assocKey.getType(), facadeClass).valueClass;
      JClass declaredClass = getClassRefForSchema(RestSpecCodec.textToSchema(assocKey.getType(), getSchemaResolver()), facadeClass);

      JMethod typesafeSetter = typesafeKeyClass.method(JMod.PUBLIC, typesafeKeyClass, "set" + nameCapsCase(name));
      JVar setterParam = typesafeSetter.param(clazz, name);
      typesafeSetter.body().add(JExpr.invoke("append").arg(JExpr.lit(name)).arg(setterParam));
      typesafeSetter.body()._return(JExpr._this());

      JMethod typesafeGetter = typesafeKeyClass.method(JMod.PUBLIC, clazz, "get" + nameCapsCase(name));
      typesafeGetter.body()._return(JExpr.cast(clazz,JExpr.invoke("getPart").arg(name)));

      assocKeyTypeInfos.put(name, new AssocKeyTypeInfo(clazz, declaredClass));
    }

    //default constructor
    typesafeKeyClass.constructor(JMod.PUBLIC);
    return assocKeyTypeInfos;
  }

  private static void generateQueryParamSetMethod(JDefinedClass derivedBuilderClass, ParameterSchema param, JClass paramClass)
  {
    final String paramName = param.getName();
    final boolean isOptional = param.isOptional() == null ? false : param.isOptional();

    final String methodName = nameCamelCase(paramName + "Param");
    final JMethod setMethod = derivedBuilderClass.method(JMod.PUBLIC, derivedBuilderClass, methodName);
    final JVar setMethodParam = setMethod.param(paramClass, "value");
    setMethod.body().add(JExpr._super().invoke(isOptional ? "setParam" : "setReqParam")
                             .arg(paramName)
                             .arg(setMethodParam));
    setMethod.body()._return(JExpr._this());

    generateParamJavadoc(setMethod, setMethodParam, param);
  }

  private static void generateQueryParamAddMethod(JDefinedClass derivedBuilderClass, ParameterSchema param, JClass paramClass)
  {
    final String paramName = param.getName();
    final boolean isOptional = param.isOptional() == null ? false : param.isOptional();

    final String methodName = nameCamelCase("add" + normalizeCaps(paramName) + "Param");
    final JMethod addMethod = derivedBuilderClass.method(JMod.PUBLIC, derivedBuilderClass, methodName);
    final JVar addMethodParam = addMethod.param(paramClass, "value");
    addMethod.body().add(JExpr._super().invoke(isOptional ? "addParam" : "addReqParam")
                             .arg(paramName)
                             .arg(addMethodParam));
    addMethod.body()._return(JExpr._this());

    generateParamJavadoc(addMethod, addMethodParam, param);
  }

  private static void generateClassJavadoc(JDefinedClass clazz, RecordTemplate schema)
  {
    final String doc = schema.data().getString("doc");
    if (doc != null)
    {
      clazz.javadoc().append(doc);
    }
  }

  private static void generateFactoryMethodJavadoc(JMethod method, RecordTemplate schema)
  {
    final String doc = schema.data().getString("doc");
    if (doc != null)
    {
      method.javadoc().append(doc);
      method.javadoc().addReturn().add("builder for the resource method");
    }
  }

  private static void generateParamJavadoc(JMethod method, JVar param, ParameterSchema schema)
  {
    if (schema.hasDoc())
    {
      method.javadoc().addParam(param).append(schema.getDoc());
      method.javadoc().addReturn().append("this builder");
    }
  }

  private static String getResourcePath(String rawPath)
  {
    if (rawPath.charAt(0) == '/')
    {
      return rawPath.substring(1);
    }
    return rawPath;
  }

  private static String capsUnderscore(String name)
  {
    StringBuilder s = new StringBuilder(name.length());
    boolean appendedUnderscore = false;
    for (int i =0; i <name.length(); ++i)
    {
      char c = name.charAt(i);
      if (Character.isUpperCase(c))
      {
        if (!appendedUnderscore)
        {
          s.append('_');
          appendedUnderscore = true;
        }
        else
        {
          appendedUnderscore = false;
        }
        s.append(c);
      }
      else
      {
        s.append(Character.toUpperCase(c));
        appendedUnderscore = false;
      }
    }
    return s.toString();
  }


  private static Set<ResourceMethod> getSupportedMethods(StringArray supportsList)
  {
    Set<ResourceMethod> supportedMethods = EnumSet.noneOf(ResourceMethod.class);
    for (String methodEntry: supportsList)
    {
      supportedMethods.add(ResourceMethod.fromString(methodEntry));
    }
    return supportedMethods;
  }

  private JClass getActionReturnType(JDefinedClass facadeClass, String returns)
  {
    JClass returnType;
    if (returns != null)
    {
      returnType = getJavaBindingType(returns, facadeClass).valueClass;
    }
    else
    {
      returnType = _voidClass;
    }
    return returnType;
  }

  private static String getName(JsonNode namedEntry)
  {
    return namedEntry.get(NAME).getTextValue();
  }

  private static String getNamespace(JsonNode entry)
  {
    if (entry.path(NAMESPACE).isMissingNode())
    {
      return "";
    }
    else
    {
      return entry.get(NAMESPACE).getTextValue();
    }
  }

  private JavaBinding getJavaBindingType(String typeSchema, JDefinedClass parentClass)
  {
    return getJavaBindingType(RestSpecCodec.textToSchema(typeSchema, getSchemaResolver()), parentClass);
  }

  private JavaBinding getJavaBindingType(DataSchema schema, JDefinedClass parentClass)
  {
    final JavaBinding binding = new JavaBinding();

    if (getConfig().getGenerateDataTemplates() || schema instanceof ArrayDataSchema)
    {
      binding.schemaClass = processSchema(schema, parentClass, null);
      if (schema instanceof ArrayDataSchema)
      {
        _generatedArrayClasses.add(binding.schemaClass);
      }
    }

    binding.schemaClass = getClassRefForSchema(schema, parentClass);
    binding.valueClass = getClassRefForSchema(schema, parentClass);

    if (schema instanceof TyperefDataSchema)
    {
      TyperefDataSchema typerefDataSchema = (TyperefDataSchema) schema;
      if (typerefDataSchema.getDereferencedDataSchema().getType() != DataSchema.Type.UNION)
      {
        String javaClassNameFromSchema = TyperefUtils.getJavaClassNameFromSchema(typerefDataSchema);
        if (javaClassNameFromSchema != null)
        {
          binding.valueClass = getCodeModel().directClass(javaClassNameFromSchema);
        }
        else
        {
          binding.valueClass = getJavaBindingType(typerefDataSchema.getRef(), parentClass).valueClass;
        }
      }
    }

    return binding;
  }

  private static String normalizeUnderscores(String name)
  {
    StringBuilder output = new StringBuilder();
    boolean capitalize = false;
    for (int i = 0; i < name.length(); ++i)
    {
      if (name.charAt(i) == '_')
      {
        capitalize = true;
        continue;
      }
      else if (capitalize)
      {
        output.append(Character.toUpperCase(name.charAt(i)));
        capitalize = false;
      }
      else
      {
        output.append(name.charAt(i));
      }
    }
    return output.toString();
  }

  private static StringBuilder normalizeCaps(String name)
  {
    StringBuilder output = new StringBuilder();
    boolean boundary = true;
    for (int i =0; i <name.length(); ++i)
    {
      boolean currentUpper = Character.isUpperCase(name.charAt(i));
      if (currentUpper)
      {
        if (i ==0)
        {
          boundary = true;
        }
        else if (i ==name.length()-1)
        {
          boundary = false;
        }
        else if (Character.isLowerCase(name.charAt(i -1)) || Character.isLowerCase(name.charAt(i +1)))
        {
          boundary = true;
        }
      }

      if (boundary)
      {
        output.append(Character.toUpperCase(name.charAt(i)));
      }
      else
      {
        output.append(Character.toLowerCase(name.charAt(i)));
      }

      boundary = false;
    }
    return output;
  }

  private static StringBuilder normalizeName(String name)
  {
    return normalizeCaps(name);
  }

  private static String nameCamelCase(String name)
  {
    StringBuilder builder = normalizeName(name);
    char firstLower = Character.toLowerCase(builder.charAt(0));
    builder.setCharAt(0, firstLower);
    return builder.toString();
  }

  private static String nameCapsCase(String name)
  {
    return normalizeName(name).toString();
  }
}


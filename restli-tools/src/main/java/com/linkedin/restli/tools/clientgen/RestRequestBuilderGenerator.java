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


import com.linkedin.common.Version;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
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
import com.linkedin.restli.client.OptionsRequestBuilder;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.base.ActionRequestBuilderBase;
import com.linkedin.restli.client.base.BatchCreateIdRequestBuilderBase;
import com.linkedin.restli.client.base.BatchDeleteRequestBuilderBase;
import com.linkedin.restli.client.base.BatchGetEntityRequestBuilderBase;
import com.linkedin.restli.client.base.BatchPartialUpdateRequestBuilderBase;
import com.linkedin.restli.client.base.BatchUpdateRequestBuilderBase;
import com.linkedin.restli.client.base.BuilderBase;
import com.linkedin.restli.client.base.CreateIdRequestBuilderBase;
import com.linkedin.restli.client.base.DeleteRequestBuilderBase;
import com.linkedin.restli.client.base.FindRequestBuilderBase;
import com.linkedin.restli.client.base.GetAllRequestBuilderBase;
import com.linkedin.restli.client.base.GetRequestBuilderBase;
import com.linkedin.restli.client.base.PartialUpdateRequestBuilderBase;
import com.linkedin.restli.client.base.UpdateRequestBuilderBase;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CompoundKey.TypeInfo;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.internal.common.TyperefUtils;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.model.ResourceModelEncoder;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;
import com.sun.codemodel.writer.FileCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final String GENERATOR_REST_GENERATE_VERSION = "generator.rest.generate.version";
  private static final Logger log = LoggerFactory.getLogger(RestRequestBuilderGenerator.class);
  private static final String NAME = "name";
  private static final String NAMESPACE = "namespace";
  private static final RestSpecCodec _codec = new RestSpecCodec();
  // Generate the validateInput() method for these resource methods
  private static final List<ResourceMethod> _validateEntityMethods = Arrays.asList(
      ResourceMethod.CREATE, ResourceMethod.UPDATE, ResourceMethod.BATCH_CREATE, ResourceMethod.BATCH_UPDATE);
  private static final List<ResourceMethod> _validatePatchMethods = Arrays.asList(
      ResourceMethod.PARTIAL_UPDATE, ResourceMethod.BATCH_PARTIAL_UPDATE);

  private static final Map<RestliVersion, String> ROOT_BUILDERS_SUFFIX;
  private static final Map<RestliVersion, String> METHOD_BUILDER_SUFFIX;

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

  static
  {
    ROOT_BUILDERS_SUFFIX = new HashMap<RestliVersion, String>();
    ROOT_BUILDERS_SUFFIX.put(RestliVersion.RESTLI_1_0_0, "Builders");
    ROOT_BUILDERS_SUFFIX.put(RestliVersion.RESTLI_2_0_0, "RequestBuilders");

    METHOD_BUILDER_SUFFIX = new HashMap<RestliVersion, String>();
    METHOD_BUILDER_SUFFIX.put(RestliVersion.RESTLI_1_0_0, "Builder");
    METHOD_BUILDER_SUFFIX.put(RestliVersion.RESTLI_2_0_0, "RequestBuilder");
  }

  protected static class Config extends DataTemplateGenerator.Config
  {
    public Config(String resolverPath,
                  String defaultPackage,
                  Boolean generateImported,
                  Boolean generateDataTemplates,
                  RestliVersion version,
                  RestliVersion deprecatedByVersion)
    {
      super(resolverPath, defaultPackage, generateImported);
      _generateDataTemplates = generateDataTemplates;
      _version = version;
      _deprecatedByVersion = deprecatedByVersion;
    }

    /**
     * @return true if the related data template source files will be generated as well, false otherwise.
     *         if null is assigned to this value, by default it returns true.
     */
    public boolean getGenerateDataTemplates()
    {
      return _generateDataTemplates == null || _generateDataTemplates;
    }

    public RestliVersion getVersion()
    {
      return _version;
    }

    public RestliVersion getDeprecatedByVersion()
    {
      return _deprecatedByVersion;
    }

    private final Boolean _generateDataTemplates;
    private final RestliVersion _version;
    private final RestliVersion _deprecatedByVersion;
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
    final String versionString = System.getProperty(GENERATOR_GENERATE_IMPORTED);
    final RestliVersion version = RestliVersion.lookUpRestliVersion(new Version(versionString));
    if (version == null)
    {
      throw new IllegalArgumentException("Unrecognized version: " + versionString);
    }

    run(System.getProperty(GENERATOR_RESOLVER_PATH),
        System.getProperty(GENERATOR_DEFAULT_PACKAGE),
        generateImported == null ? null : Boolean.parseBoolean(generateImported),
        generateDataTemplates == null ? null : Boolean.parseBoolean(generateDataTemplates),
        version,
        null,
        args[0],
        Arrays.copyOfRange(args, 1, args.length));
  }

  public static GeneratorResult run(String resolverPath,
                                    String defaultPackage,
                                    Boolean generateImported,
                                    Boolean generateDataTemplates,
                                    RestliVersion version,
                                    RestliVersion deprecatedByVersion,
                                    String targetDirectoryPath,
                                    String[] sources) throws IOException
  {
    final Config config = new Config(resolverPath, defaultPackage, generateImported, generateDataTemplates, version, deprecatedByVersion);
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

  private static String getBuilderClassNameByVersion(RestliVersion version, String namespace, String builderName, boolean isRootBuilders)
  {
    final String className = (namespace == null || namespace.trim().isEmpty() ? "" : namespace + ".") + capitalize(builderName);
    final Map<RestliVersion, String> suffixMap = (isRootBuilders ? ROOT_BUILDERS_SUFFIX : METHOD_BUILDER_SUFFIX);
    return className + suffixMap.get(version);
  }

  private static boolean checkVersionAndDeprecateBuilderClass(Config config, JDefinedClass clazz, boolean isRootBuilders)
  {
    if (config.getDeprecatedByVersion() == null)
    {
      return false;
    }
    else
    {
      clazz.annotate(Deprecated.class);

      final Map<RestliVersion, String> suffixMap = (isRootBuilders ? ROOT_BUILDERS_SUFFIX : METHOD_BUILDER_SUFFIX);
      final String deprecatedBuilderName = clazz.name();
      final String replacementBuilderName = deprecatedBuilderName.substring(0, deprecatedBuilderName.length() - suffixMap.get(config.getVersion()).length());
      clazz.javadoc().addDeprecated().append("This format of request builder is obsolete. Please use {@link " +
                                                 getBuilderClassNameByVersion(config.getDeprecatedByVersion(), clazz.getPackage().name(), replacementBuilderName, isRootBuilders) +
                                                 "} instead.");
      return true;
    }
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
            generateResourceFacade(resource,
                                   sourceFile.getAbsolutePath(),
                                   new HashMap<String, JClass>(),
                                   new HashMap<String, JClass>(),
                                   new HashMap<String, List<String>>());
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
                                               Map<String, JClass> pathKeyTypes,
                                               Map<String, JClass> assocKeyTypes,
                                               Map<String, List<String>> pathToAssocKeys)
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

    String packageName = resource.getNamespace();
    JPackage clientPackage = (packageName == null || packageName.isEmpty()) ? getPackage() : getPackage(packageName);

    String className;
    if (_config.getVersion() == RestliVersion.RESTLI_2_0_0)
    {
      className = getBuilderClassNameByVersion(RestliVersion.RESTLI_2_0_0, null, resource.getName(), true);
    }
    else
    {
      className = getBuilderClassNameByVersion(RestliVersion.RESTLI_1_0_0, null, resource.getName(), true);
    }
    JDefinedClass facadeClass = clientPackage._class(className);
    annotate(facadeClass, sourceFile);

    final JFieldVar baseUriField;
    final JFieldVar requestOptionsField;
    final JExpression baseUriGetter = JExpr.invoke("getBaseUriTemplate");
    final JExpression requestOptionsGetter = JExpr.invoke("getRequestOptions");

    if (_config.getVersion() == RestliVersion.RESTLI_2_0_0)
    {
      baseUriField = null;
      requestOptionsField = null;
      facadeClass._extends(BuilderBase.class);
    }
    else
    {
      // for old builder, instead of extending from RequestBuilderBase, add fields and getters in the class
      baseUriField = facadeClass.field(JMod.PRIVATE | JMod.FINAL, String.class, "_baseUriTemplate");
      requestOptionsField = facadeClass.field(JMod.PRIVATE, RestliRequestOptions.class, "_requestOptions");
      facadeClass.method(JMod.PRIVATE, String.class, "getBaseUriTemplate").body()._return(baseUriField);
      facadeClass.method(JMod.PUBLIC, RestliRequestOptions.class, "getRequestOptions").body()._return(requestOptionsField);
    }

    // make the original resource path available via a private final static variable.
    JFieldVar originalResourceField = facadeClass.field(JMod.PRIVATE | JMod.STATIC |JMod.FINAL, String.class,
                                                        "ORIGINAL_RESOURCE_PATH");
    String resourcePath = getResourcePath(resource.getPath());
    originalResourceField.init(JExpr.lit(resourcePath));

    // create reference to RestliRequestOptions.DEFAULT_OPTIONS
    final JClass restliRequestOptionsClass = getCodeModel().ref(RestliRequestOptions.class);
    JFieldRef defaultOptionsField = restliRequestOptionsClass.staticRef("DEFAULT_OPTIONS");

    if (_config.getVersion() == RestliVersion.RESTLI_1_0_0)
    {
      // same getPathComponents() logic as in RequestBuilderBase
      JMethod pathComponentsGetter = facadeClass.method(JMod.PUBLIC, String[].class, "getPathComponents");
      pathComponentsGetter.body()._return(getCodeModel().ref(URIParamUtils.class).staticInvoke("extractPathComponentsFromUriTemplate").arg(baseUriField));

      // method that expresses the following logic
      //   (requestOptions == null) ? return RestliRequestOptions.DEFAULT_OPTIONS : requestOptions;
      JMethod requestOptionsAssigner = facadeClass.method(JMod.PRIVATE | JMod.STATIC, RestliRequestOptions.class, "assignRequestOptions");
      JVar requestOptionsAssignerParam = requestOptionsAssigner.param(RestliRequestOptions.class, "requestOptions");
      JConditional requestNullCheck = requestOptionsAssigner.body()._if(requestOptionsAssignerParam.eq(JExpr._null()));
      requestNullCheck._then().block()._return(defaultOptionsField);
      requestNullCheck._else().block()._return(requestOptionsAssignerParam);
    }

    /*
    There will be 4 constructors:
      ()
      (RestliRequestOptions)
      (String)
      (String, RestliRequestOptions)
    */
    JMethod noArgConstructor = facadeClass.constructor(JMod.PUBLIC);
    JMethod requestOptionsOverrideConstructor = facadeClass.constructor(JMod.PUBLIC);
    JMethod resourceNameOverrideConstructor = facadeClass.constructor(JMod.PUBLIC);
    JMethod mainConstructor = facadeClass.constructor(JMod.PUBLIC);

    // no-argument constructor, delegates to the request options override constructor
    noArgConstructor.body().invoke(THIS).arg(defaultOptionsField);

    // request options override constructor
    JVar requestOptionsOverrideOptionsParam = requestOptionsOverrideConstructor.param(RestliRequestOptions.class, "requestOptions");

    if (_config.getVersion() == RestliVersion.RESTLI_2_0_0)
    {
      requestOptionsOverrideConstructor.body().invoke(SUPER).arg(originalResourceField).arg(requestOptionsOverrideOptionsParam);
    }
    else
    {
      requestOptionsOverrideConstructor.body().assign(baseUriField, originalResourceField);
      final JInvocation requestOptionsOverrideAssignRequestOptions = new JBlock().invoke("assignRequestOptions").arg(requestOptionsOverrideOptionsParam);
      requestOptionsOverrideConstructor.body().assign(requestOptionsField, requestOptionsOverrideAssignRequestOptions);
    }

    // primary resource name override constructor, delegates to the main constructor
    JVar resourceNameOverrideResourceNameParam = resourceNameOverrideConstructor.param(_stringClass, "primaryResourceName");
    resourceNameOverrideConstructor.body().invoke(THIS).arg(resourceNameOverrideResourceNameParam).arg(defaultOptionsField);

    // main constructor
    JVar mainConsResourceNameParam = mainConstructor.param(_stringClass, "primaryResourceName");
    JVar mainConsOptionsParam = mainConstructor.param(RestliRequestOptions.class, "requestOptions");

    JExpression baseUriExpr;
    if (resourcePath.contains("/"))
    {
      baseUriExpr = originalResourceField.invoke("replaceFirst").arg(JExpr.lit("[^/]*/")).arg(mainConsResourceNameParam.plus(JExpr.lit("/")));
    }
    else
    {
      baseUriExpr = mainConsResourceNameParam;
    }

    if (_config.getVersion() == RestliVersion.RESTLI_2_0_0)
    {
      mainConstructor.body().invoke(SUPER).arg(baseUriExpr).arg(mainConsOptionsParam);
    }
    else
    {
      final JInvocation mainAssignRequestOptions = new JBlock().invoke("assignRequestOptions").arg(mainConsOptionsParam);
      mainConstructor.body().assign(baseUriField, baseUriExpr);
      mainConstructor.body().assign(requestOptionsField, mainAssignRequestOptions);
    }

    String resourceName = capitalize(resource.getName());
    JMethod primaryResourceGetter = facadeClass.method(JMod.PUBLIC | JMod.STATIC, String.class, "getPrimaryResource");
    primaryResourceGetter.body()._return(originalResourceField);

    List<String> pathKeys = getPathKeys(resourcePath, pathToAssocKeys);

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

      String keyName = getAssociationKey(resource, association);
      pathKeyTypes.put(keyName, keyClass);

      List<String> assocKeys = new ArrayList<String>(4);
      for(Map.Entry<String, AssocKeyTypeInfo> entry: assocKeyTypeInfos.entrySet())
      {
        assocKeys.add(entry.getKey());
        assocKeyTypes.put(entry.getKey(), entry.getValue().getBindingType());
      }
      pathToAssocKeys.put(keyName, assocKeys);
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

    generateOptions(facadeClass, baseUriGetter, requestOptionsGetter);

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
                           baseUriGetter,
                           keyClass,
                           schemaClass,
                           supportedMethods,
                           restMethods,
                           resourceSpecField,
                           resourceName,
                           pathKeys,
                           pathKeyTypes,
                           assocKeyTypes,
                           pathToAssocKeys,
                           requestOptionsGetter,
                           resource.data().getDataMap("annotations"));

      if (resourceSchemaClass == CollectionSchema.class ||
          resourceSchemaClass == AssociationSchema.class)
      {
        generateFinders(facadeClass,
                        baseUriGetter,
                        finders,
                        keyClass,
                        schemaClass,
                        assocKeyTypeInfos,
                        resourceSpecField,
                        resourceName,
                        pathKeys,
                        pathKeyTypes,
                        assocKeyTypes,
                        pathToAssocKeys,
                        requestOptionsGetter);
      }

      generateSubResources(sourceFile, subresources, pathKeyTypes, assocKeyTypes, pathToAssocKeys);
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
                    baseUriGetter,
                    resourceActions,
                    entityActions,
                    keyClass,
                    resourceSpecField,
                    resourceName,
                    pathKeys,
                    pathKeyTypes,
                    assocKeyTypes,
                    pathToAssocKeys,
                    requestOptionsGetter);


    generateClassJavadoc(facadeClass, resource);

    if (!checkVersionAndDeprecateBuilderClass(_config, facadeClass, true))
    {
      checkRestSpecAndDeprecateRootBuildersClass(facadeClass, resource);
    }

    return facadeClass;
  }

  private String getAssociationKey(ResourceSchema resource, AssociationSchema association)
  {
    if (association.getIdentifier() == null)
    {
      return resource.getName() + "Id";
    }
    else
    {
      return association.getIdentifier();
    }
  }

  private static List<String> fixOldStylePathKeys(List<String> pathKeys, String resourcePath, Map<String, List<String>> pathToAssocKeys)
  {
    if (resourcePath.contains("="))
    {
      // this is an old-style IDL.
      List<String> newPathKeys = new ArrayList<String>(pathKeys.size());
      Map<String, String> assocToPathKeys = reverseMap(pathToAssocKeys);
      Set<String> prevRealPathKeys = new HashSet<String>();
      for (String currKey : pathKeys)
      {
        if (assocToPathKeys.containsKey(currKey))
        {
          // currKey actually an assocKey
          if (!prevRealPathKeys.contains(assocToPathKeys.get(currKey)))
          {
            // if we already added the real path key, don't add it again.
            prevRealPathKeys.add(assocToPathKeys.get(currKey));
            newPathKeys.add(assocToPathKeys.get(currKey));
          }
        }
        else
        {
          newPathKeys.add(currKey);
        }
      }
      return newPathKeys;
    }
    else
    {
      return pathKeys;
    }
  }

  private static Map<String, String> reverseMap(Map<String, List<String>> toReverse)
  {
    Map<String, String> reversed = new HashMap<String, String>();
    for (Map.Entry<String, List<String>> entry: toReverse.entrySet())
    {
      for (String element : entry.getValue())
      {
        reversed.put(element, entry.getKey());
      }
    }
    return reversed;
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

  private static List<String> getPathKeys(String basePath, Map<String, List<String>> pathToAssocKeys)
  {
    UriTemplate template = new UriTemplate(basePath);
    return fixOldStylePathKeys(template.getTemplateVariables(), basePath, pathToAssocKeys);
  }


  private void generateOptions(JDefinedClass facadeClass, JExpression baseUriExpr, JExpression requestOptionsExpr)
  {
    JClass builderClass = getCodeModel().ref(OptionsRequestBuilder.class);
    JMethod finderMethod = facadeClass.method(JMod.PUBLIC, OptionsRequestBuilder.class, "options");
    finderMethod.body()._return(JExpr._new(builderClass).arg(baseUriExpr).arg(requestOptionsExpr));
  }

  private void generateSubResources(String sourceFile,
                                    ResourceSchemaArray subresources,
                                    Map<String, JClass> pathKeyTypes,
                                    Map<String, JClass> assocKeyTypes,
                                    Map<String, List<String>> pathToAssocKeys)
          throws JClassAlreadyExistsException, IOException
  {
    if (subresources == null)
    {
      return;
    }

    for (ResourceSchema resource : subresources)
    {
      generateResourceFacade(resource, sourceFile, pathKeyTypes, assocKeyTypes, pathToAssocKeys);
    }
  }

  private void generateFinders(JDefinedClass facadeClass,
                               JExpression baseUriExpr,
                               FinderSchemaArray finderSchemas,
                               JClass keyClass,
                               JClass valueClass,
                               Map<String, AssocKeyTypeInfo> assocKeys,
                               JVar resourceSpecField,
                               String resourceName,
                               List<String> pathKeys,
                               Map<String, JClass> pathKeyTypes,
                               Map<String, JClass> assocKeyTypes,
                               Map<String, List<String>> pathToAssocKeys,
                               JExpression requestOptionsExpr)
          throws JClassAlreadyExistsException
  {
    if (finderSchemas != null)
    {
      JClass baseBuilderClass = getCodeModel().ref(FindRequestBuilderBase.class).narrow(keyClass, valueClass);

      for (FinderSchema finder : finderSchemas)
      {
        String finderName = finder.getName();

        String builderName = capitalize(resourceName) + "FindBy" + capitalize(finderName) + METHOD_BUILDER_SUFFIX.get(_config.getVersion());
        JDefinedClass finderBuilderClass = generateDerivedBuilder(baseBuilderClass,
                                                                  valueClass,
                                                                  finderName,
                                                                  builderName,
                                                                  facadeClass.getPackage(),
                                                                  ResourceMethod.FINDER,
                                                                  null);

        JMethod finderMethod = facadeClass.method(JMod.PUBLIC, finderBuilderClass,
                                                "findBy" + capitalize(finderName));

        finderMethod.body()._return(JExpr._new(finderBuilderClass).arg(baseUriExpr).arg(resourceSpecField).arg(requestOptionsExpr));

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

        generatePathKeyBindingMethods(pathKeys, finderBuilderClass, pathKeyTypes, assocKeyTypes, pathToAssocKeys);

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
                                               JPackage clientPackage,
                                               ResourceMethod resourceMethod,
                                               DataMap annotations)
          throws JClassAlreadyExistsException
  {
    // this method applies to REST methods and finder

    JDefinedClass derivedBuilderClass = clientPackage._class(JMod.PUBLIC, derivedBuilderName);
    annotate(derivedBuilderClass, null);
    checkVersionAndDeprecateBuilderClass(_config, derivedBuilderClass, false);
    derivedBuilderClass._extends(baseBuilderClass.narrow(derivedBuilderClass));
    JMethod derivedBuilderConstructor = derivedBuilderClass.constructor(JMod.PUBLIC);
    JVar uriParam = derivedBuilderConstructor.param(_stringClass, "baseUriTemplate");
    JVar resourceSpecParam = derivedBuilderConstructor.param(_resourceSpecClass, "resourceSpec");
    JVar requestOptionsParam = derivedBuilderConstructor.param(RestliRequestOptions.class, "requestOptions");
    JInvocation invocation = derivedBuilderConstructor.body().invoke(SUPER).arg(uriParam);

    // the new BatchGetEntityRequestBuilderBase does not need the valueClass parameter in constructor
    if (!baseBuilderClass.fullName().startsWith(BatchGetEntityRequestBuilderBase.class.getName() + "<"))
    {
      invocation.arg(valueClass.dotclass());
    }
    invocation.arg(resourceSpecParam).arg(requestOptionsParam);
    if (finderName != null)
    {
      derivedBuilderConstructor.body().add(JExpr._super().invoke("name").arg(finderName));
    }

    if (_validateEntityMethods.contains(resourceMethod) || _validatePatchMethods.contains(resourceMethod))
    {
      JMethod validateMethod = derivedBuilderClass.method(JMod.PUBLIC | JMod.STATIC, ValidationResult.class, "validateInput");
      JVar inputParam;
      if (_validateEntityMethods.contains(resourceMethod))
      {
        inputParam = validateMethod.param(valueClass, "input");
      }
      else
      {
        inputParam = validateMethod.param(getCodeModel().ref(PatchRequest.class).narrow(valueClass), "patch");
      }
      JBlock block = validateMethod.body();
      JVar annotationMap = block.decl(getCodeModel().ref(Map.class).narrow(String.class).narrow(getCodeModel().ref(List.class).narrow(String.class)), "annotations")
          .init(JExpr._new(getCodeModel().ref(HashMap.class).narrow(String.class).narrow(getCodeModel().ref(List.class).narrow(String.class))));
      if (annotations != null)
      {
        for (Map.Entry<String, Object> entry : annotations.entrySet())
        {
          DataList values = ((DataMap) entry.getValue()).getDataList("value");
          if (values != null)
          {
            JInvocation list = getCodeModel().ref(Arrays.class).staticInvoke("asList");
            for (Object value : values)
            {
              list.arg(value.toString());
            }
            block.add(annotationMap.invoke("put").arg(entry.getKey()).arg(list));
          }
        }
      }
      JClass validatorClass = getCodeModel().ref(RestLiDataValidator.class);
      JVar validator = block.decl(validatorClass, "validator").init(JExpr._new(validatorClass)
                                                                        .arg(annotationMap)
                                                                        .arg(valueClass.dotclass())
                                                                        .arg(getCodeModel().ref(ResourceMethod.class).staticRef(resourceMethod.name())));
      block._return(validator.invoke("validate").arg(inputParam));
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

      JMethod keyMethod = finderBuilderClass.method(JMod.PUBLIC, finderBuilderClass, RestLiToolsUtils.nameCamelCase(
          assocKey + "Key"));
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
                                             Map<String, JClass> pathKeyTypes,
                                             Map<String, JClass> assocKeyTypes,
                                             Map<String, List<String>> pathToAssocKeys)
  {
    for (String pathKey : pathKeys)
    {
      if (pathToAssocKeys.get(pathKey) != null)
      {
        JFieldVar compoundKey = builderClass.field(JMod.PRIVATE,
                                                   CompoundKey.class,
                                                   RestLiToolsUtils.nameCamelCase(pathKey),
                                                   JExpr._new(pathKeyTypes.get(pathKey)));

        for (String assocKeyName : pathToAssocKeys.get(pathKey))
        {
          JMethod assocKeyMethod = builderClass.method(JMod.PUBLIC, builderClass, RestLiToolsUtils.nameCamelCase(
              assocKeyName + "Key"));
          JVar assocKeyMethodParam = assocKeyMethod.param(assocKeyTypes.get(assocKeyName), "key");

          // put stuff into CompoundKey
          assocKeyMethod.body().add(compoundKey.invoke("append").arg(assocKeyName).arg(assocKeyMethodParam));
          // old and new super.pathKey()s
          assocKeyMethod.body().add(JExpr._super().invoke("pathKey").arg(assocKeyName).arg(assocKeyMethodParam));
          assocKeyMethod.body().add(JExpr._super().invoke("pathKey").arg(pathKey).arg(compoundKey)); // new...
          assocKeyMethod.body()._return(JExpr._this());
        }
      }
      else
      {
        JMethod keyMethod = builderClass.method(JMod.PUBLIC, builderClass, RestLiToolsUtils.nameCamelCase(pathKey + "Key"));
        JVar keyMethodParam = keyMethod.param(pathKeyTypes.get(pathKey), "key");
        keyMethod.body().add(JExpr._super().invoke("pathKey").arg(pathKey).arg(keyMethodParam));
        keyMethod.body()._return(JExpr._this());
      }
    }
  }

  private void generateActions(JDefinedClass facadeClass,
                               JExpression baseUriExpr,
                               ActionSchemaArray resourceActions,
                               ActionSchemaArray entityActions,
                               JClass keyClass,
                               JVar resourceSpecField,
                               String resourceName,
                               List<String> pathKeys,
                               Map<String, JClass> pathKeyTypes,
                               Map<String, JClass> assocKeyTypes,
                               Map<String, List<String>> pathToAssocKeys,
                               JExpression requestOptionsExpr)
          throws JClassAlreadyExistsException
  {
    if (resourceActions != null)
    {
      for (ActionSchema action : resourceActions)
      {
        generateActionMethod(facadeClass,
                             baseUriExpr,
                             _voidClass,
                             action,
                             resourceSpecField,
                             resourceName,
                             pathKeys,
                             pathKeyTypes,
                             assocKeyTypes,
                             pathToAssocKeys,
                             requestOptionsExpr);
      }
    }

    if (entityActions != null)
    {
      for (ActionSchema action : entityActions)
      {
        generateActionMethod(facadeClass,
                             baseUriExpr,
                             keyClass,
                             action,
                             resourceSpecField,
                             resourceName,
                             pathKeys,
                             pathKeyTypes,
                             assocKeyTypes,
                             pathToAssocKeys,
                             requestOptionsExpr);
      }
    }
  }

  private void generateActionMethod(JDefinedClass facadeClass,
                                    JExpression baseUriExpr,
                                    JClass keyClass,
                                    ActionSchema action,
                                    JVar resourceSpecField,
                                    String resourceName,
                                    List<String> pathKeys,
                                    Map<String, JClass> pathKeyTypes,
                                    Map<String, JClass> assocKeysTypes,
                                    Map<String, List<String>> pathToAssocKeys,
                                    JExpression requestOptionsExpr)
          throws JClassAlreadyExistsException
  {
    JClass returnType = getActionReturnType(facadeClass, action.getReturns());
    JClass vanillaActionBuilderClass = getCodeModel().ref(ActionRequestBuilderBase.class).narrow(keyClass,
                                                                                returnType);
    String actionName = action.getName();

    String actionBuilderClassName = capitalize(resourceName) + "Do" + capitalize(actionName) + METHOD_BUILDER_SUFFIX.get(_config.getVersion());
    JDefinedClass actionBuilderClass = facadeClass.getPackage()._class(JMod.PUBLIC, actionBuilderClassName);
    annotate(actionBuilderClass, null);
    checkVersionAndDeprecateBuilderClass(_config, actionBuilderClass, false);
    actionBuilderClass._extends(vanillaActionBuilderClass.narrow(actionBuilderClass));
    JMethod actionBuilderConstructor = actionBuilderClass.constructor(JMod.PUBLIC);
    JVar uriParam = actionBuilderConstructor.param(_stringClass, "baseUriTemplate");
    JVar classParam = actionBuilderConstructor.param(getCodeModel().ref(Class.class).narrow(returnType), "returnClass");
    JVar resourceSpecParam = actionBuilderConstructor.param(_resourceSpecClass, "resourceSpec");
    JVar actionsRequestOptionsParam = actionBuilderConstructor.param(RestliRequestOptions.class, "requestOptions");
    actionBuilderConstructor.body().invoke(SUPER).arg(uriParam).arg(classParam).arg(
            resourceSpecParam).arg(actionsRequestOptionsParam);
    actionBuilderConstructor.body().add(JExpr._super().invoke("name").arg(actionName));

    JMethod actionMethod = facadeClass.method(JMod.PUBLIC, actionBuilderClass, "action" + capitalize(actionName));
    actionMethod.body()._return(JExpr._new(actionBuilderClass).arg(baseUriExpr).arg(returnType.dotclass()).arg(resourceSpecField).arg(requestOptionsExpr));

    if (action.getParameters() != null)
    {
      for (ParameterSchema param : action.getParameters())
      {
        String paramName = param.getName();
        boolean isOptional = param.isOptional() == null ? false : param.isOptional();
        JavaBinding binding = getJavaBindingType(param.getType(), facadeClass);

        JMethod typesafeMethod = _config.getVersion() == RestliVersion.RESTLI_2_0_0 ?
            actionBuilderClass.method(JMod.PUBLIC, actionBuilderClass, RestLiToolsUtils.nameCamelCase(paramName + "Param")) :
            actionBuilderClass.method(JMod.PUBLIC, actionBuilderClass, "param" + capitalize(paramName));
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

    generatePathKeyBindingMethods(pathKeys, actionBuilderClass, pathKeyTypes, assocKeysTypes, pathToAssocKeys);

    generateClassJavadoc(actionBuilderClass, action);
    generateFactoryMethodJavadoc(actionMethod, action);
  }

  @SuppressWarnings("deprecation")
  private void generateBasicMethods(JDefinedClass facadeClass,
                                    JExpression baseUriExpr,
                                    JClass keyClass,
                                    JClass valueClass,
                                    Set<ResourceMethod> supportedMethods,
                                    RestMethodSchemaArray restMethods,
                                    JVar resourceSpecField,
                                    String resourceName,
                                    List<String> pathKeys,
                                    Map<String, JClass> pathKeyTypes,
                                    Map<String, JClass> assocKeyTypes,
                                    Map<String, List<String>> pathToAssocKeys,
                                    JExpression requestOptionsExpr,
                                    DataMap annotations)
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
    if (_config.getVersion() == RestliVersion.RESTLI_2_0_0)
    {
      crudBuilderClasses.put(ResourceMethod.CREATE, CreateIdRequestBuilderBase.class);
    }
    else
    {
      // we use fully qualified class name to avoid importing the deprecated class
      // so far in Java there is no way to suppress deprecation warning for import
      crudBuilderClasses.put(ResourceMethod.CREATE, com.linkedin.restli.client.base.CreateRequestBuilderBase.class);
    }
    crudBuilderClasses.put(ResourceMethod.GET, GetRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.UPDATE, UpdateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.PARTIAL_UPDATE, PartialUpdateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.DELETE, DeleteRequestBuilderBase.class);
    if (_config.getVersion() == RestliVersion.RESTLI_2_0_0)
    {
      crudBuilderClasses.put(ResourceMethod.BATCH_CREATE, BatchCreateIdRequestBuilderBase.class);
    }
    else
    {
      // we use fully qualified class name to avoid importing the deprecated class
      // so far in Java there is no way to suppress deprecation warning for import
      crudBuilderClasses.put(ResourceMethod.BATCH_CREATE, com.linkedin.restli.client.base.BatchCreateRequestBuilderBase.class);
    }
    crudBuilderClasses.put(ResourceMethod.BATCH_UPDATE, BatchUpdateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.BATCH_PARTIAL_UPDATE, BatchPartialUpdateRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.BATCH_DELETE, BatchDeleteRequestBuilderBase.class);
    crudBuilderClasses.put(ResourceMethod.GET_ALL, GetAllRequestBuilderBase.class);
    if (_config.getVersion() == RestliVersion.RESTLI_2_0_0)
    {
      crudBuilderClasses.put(ResourceMethod.BATCH_GET, BatchGetEntityRequestBuilderBase.class);
    }
    else
    {
      // we use fully qualified class name to avoid importing the deprecated class
      // so far in Java there is no way to suppress deprecation warning for import
      crudBuilderClasses.put(ResourceMethod.BATCH_GET, com.linkedin.restli.client.base.BatchGetRequestBuilderBase.class);
    }

    for (Map.Entry<ResourceMethod, Class<?>> entry : crudBuilderClasses.entrySet())
    {
      ResourceMethod method = entry.getKey();
      if (supportedMethods.contains(method))
      {
        String methodName = RestLiToolsUtils.normalizeUnderscores(method.toString());

        JClass builderClass = getCodeModel().ref(entry.getValue()).narrow(keyClass, valueClass);
        JDefinedClass derivedBuilder = generateDerivedBuilder(builderClass,
                                                              valueClass,
                                                              null,
                                                              resourceName + RestLiToolsUtils.nameCapsCase(methodName) +
                                                                METHOD_BUILDER_SUFFIX.get(_config.getVersion()),
                                                              facadeClass.getPackage(),
                                                              method,
                                                              annotations);
        generatePathKeyBindingMethods(pathKeys, derivedBuilder, pathKeyTypes, assocKeyTypes, pathToAssocKeys);

        JMethod factoryMethod = facadeClass.method(JMod.PUBLIC, derivedBuilder, RestLiToolsUtils.nameCamelCase(
            methodName));
        factoryMethod.body()._return(JExpr._new(derivedBuilder).arg(baseUriExpr).arg(resourceSpecField).arg(requestOptionsExpr));

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

      JMethod typesafeSetter = typesafeKeyClass.method(JMod.PUBLIC, typesafeKeyClass, "set" + RestLiToolsUtils.nameCapsCase(
          name));
      JVar setterParam = typesafeSetter.param(clazz, name);
      typesafeSetter.body().add(JExpr.invoke("append").arg(JExpr.lit(name)).arg(setterParam));
      typesafeSetter.body()._return(JExpr._this());

      JMethod typesafeGetter = typesafeKeyClass.method(JMod.PUBLIC, clazz, "get" + RestLiToolsUtils.nameCapsCase(name));
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

    final String methodName = RestLiToolsUtils.nameCamelCase(paramName + "Param");
    final JMethod setMethod = derivedBuilderClass.method(JMod.PUBLIC, derivedBuilderClass, methodName);
    final JVar setMethodParam = setMethod.param(paramClass, "value");
    setMethod.body().add(JExpr._super().invoke(isOptional ? "setParam" : "setReqParam").arg(paramName).arg(setMethodParam));
    setMethod.body()._return(JExpr._this());

    generateParamJavadoc(setMethod, setMethodParam, param);
  }

  private static void generateQueryParamAddMethod(JDefinedClass derivedBuilderClass, ParameterSchema param, JClass paramClass)
  {
    final String paramName = param.getName();
    final boolean isOptional = param.isOptional() == null ? false : param.isOptional();

    final String methodName = RestLiToolsUtils.nameCamelCase("add" + RestLiToolsUtils.normalizeCaps(paramName) + "Param");
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

  private static void checkRestSpecAndDeprecateRootBuildersClass(JDefinedClass clazz, ResourceSchema schema)
  {
    // this method only applies to the root builders class

    if(schema.data().containsKey("annotations"))
    {
      DataMap annotations = schema.data().getDataMap("annotations");
      DataMap deprecated = annotations.getDataMap(ResourceModelEncoder.DEPRECATED_ANNOTATION_NAME);
      if(deprecated != null)
      {
        clazz.annotate(Deprecated.class);

        if(deprecated.containsKey(ResourceModelEncoder.DEPRECATED_ANNOTATION_DOC_FIELD))
        {
          clazz.javadoc().addDeprecated().append(deprecated.getString(ResourceModelEncoder.DEPRECATED_ANNOTATION_DOC_FIELD));
        }
      }
    }
  }

  private static void generateFactoryMethodJavadoc(JMethod method, RecordTemplate schema)
  {
    StringBuilder docString = new StringBuilder();
    if(schema.data().containsKey("annotations"))
    {
      DataMap annotations = schema.data().getDataMap("annotations");
      if(annotations.containsKey("testMethod"))
      {
        DataMap testMethod = annotations.getDataMap("testMethod");
        docString.append("<b>Test Method");

        String testMethodDoc = testMethod.getString("doc");
        if(testMethodDoc !=  null)
        {
          docString.append(": ");
          docString.append(testMethodDoc);
        }

        docString.append("</b>\n");
      }
    }
    final String doc = schema.data().getString("doc");
    if (doc != null)
    {
      docString.append(doc);
    }

    if(docString.length() > 0)
    {
      method.javadoc().append(docString.toString());
      method.javadoc().addReturn().add("builder for the resource method");
    }

    if(schema.data().containsKey("annotations"))
    {
      DataMap annotations = schema.data().getDataMap("annotations");
      if(annotations.containsKey(ResourceModelEncoder.DEPRECATED_ANNOTATION_NAME))
      {
        method.annotate(Deprecated.class);

        DataMap deprecated = annotations.getDataMap(ResourceModelEncoder.DEPRECATED_ANNOTATION_NAME);
        if(deprecated.containsKey(ResourceModelEncoder.DEPRECATED_ANNOTATION_DOC_FIELD))
        {
          method.javadoc().addDeprecated().append(deprecated.getString(ResourceModelEncoder.DEPRECATED_ANNOTATION_DOC_FIELD));
        }
      }
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
    return namedEntry.get(NAME).textValue();
  }

  private static String getNamespace(JsonNode entry)
  {
    if (entry.path(NAMESPACE).isMissingNode())
    {
      return "";
    }
    else
    {
      return entry.get(NAMESPACE).textValue();
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
}

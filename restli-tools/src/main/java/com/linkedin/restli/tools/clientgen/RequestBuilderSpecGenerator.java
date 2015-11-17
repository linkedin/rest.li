/*
   Copyright (c) 2015 LinkedIn Corp.

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
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.StringArray;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.pegasus.generator.CodeUtil;
import com.linkedin.pegasus.generator.TemplateSpecGenerator;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ActionsSetSchema;
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
import com.linkedin.restli.tools.clientgen.builderspec.ActionParamBindingMethodSpec;
import com.linkedin.restli.tools.clientgen.builderspec.BuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.RequestBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.FinderBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.ActionBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.ActionSetRootBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.CollectionRootBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.PathKeyBindingMethodSpec;
import com.linkedin.restli.tools.clientgen.builderspec.QueryParamBindingMethodSpec;
import com.linkedin.restli.tools.clientgen.builderspec.RestMethodBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.RootBuilderMethodSpec;
import com.linkedin.restli.tools.clientgen.builderspec.RootBuilderSpec;
import com.linkedin.restli.tools.clientgen.builderspec.SimpleRootBuilderSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class will parse the resource spec IDL to generate request builder specifications containing all the meta
 * information defined in the restspec.json IDL file.
 *
 * @author Min Chen
 */
public class RequestBuilderSpecGenerator
{
  private static final Logger log = LoggerFactory.getLogger(RequestBuilderSpecGenerator.class);

  private static final Map<RestliVersion, String> ROOT_BUILDERS_SUFFIX;
  private static final Map<RestliVersion, String> METHOD_BUILDER_SUFFIX;
  private final String _customMethodBuilderSuffix;

  // use LinkedHashSet to keep insertion order to avoid randomness in generated code in giant root builder case.
  protected final Set<BuilderSpec> _builderSpecs = new LinkedHashSet<BuilderSpec>();

  private final DataSchemaResolver _schemaResolver;
  private final TemplateSpecGenerator _templateSpecGenerator;
  private final RestliVersion _version;
  private final Map<ResourceMethod, String> _builderBaseMap;

  // idl schema location under process
  private DataSchemaLocation _currentSchemaLocation;

  static
  {
    ROOT_BUILDERS_SUFFIX = new HashMap<RestliVersion, String>();
    ROOT_BUILDERS_SUFFIX.put(RestliVersion.RESTLI_1_0_0, "Builders");
    ROOT_BUILDERS_SUFFIX.put(RestliVersion.RESTLI_2_0_0, "RequestBuilders");

    METHOD_BUILDER_SUFFIX = new HashMap<RestliVersion, String>();
    METHOD_BUILDER_SUFFIX.put(RestliVersion.RESTLI_1_0_0, "Builder");
    METHOD_BUILDER_SUFFIX.put(RestliVersion.RESTLI_2_0_0, "RequestBuilder");
  }

  public RequestBuilderSpecGenerator(DataSchemaResolver schemaResolver,
                                     TemplateSpecGenerator templateSpecGenerator,
                                     RestliVersion version,
                                     Map<ResourceMethod, String> builderBaseMap)
  {
    this(schemaResolver, templateSpecGenerator, version, builderBaseMap, null);
  }

  public RequestBuilderSpecGenerator(DataSchemaResolver schemaResolver,
                                     TemplateSpecGenerator templateSpecGenerator,
                                     RestliVersion version,
                                     Map<ResourceMethod, String> builderBaseMap,
                                     String customMethodBuilderSuffix)
  {
    _schemaResolver = schemaResolver;
    _templateSpecGenerator = templateSpecGenerator;
    _version = version;
    _builderBaseMap = builderBaseMap;
    _customMethodBuilderSuffix = customMethodBuilderSuffix;
  }

  public String getBuilderBase(ResourceMethod method)
  {
    if (_builderBaseMap != null)
    {
      return _builderBaseMap.get(method);
    }
    return null;
  }

  private void registerBuilderSpec(BuilderSpec builder)
  {
    _builderSpecs.add(builder);
  }

  public Set<BuilderSpec> getBuilderSpec()
  {
    return _builderSpecs;
  }

  private static String getBuilderClassNameByVersion(RestliVersion version,
                                                     String namespace,
                                                     String builderName,
                                                     boolean isRootBuilders)
  {
    final String className =
        (namespace == null || namespace.trim().isEmpty() ? "" : namespace + ".") + CodeUtil.capitalize(builderName);
    final Map<RestliVersion, String> suffixMap = (isRootBuilders ? ROOT_BUILDERS_SUFFIX : METHOD_BUILDER_SUFFIX);
    return className + suffixMap.get(version);
  }

  public void generate(ResourceSchema resource, File sourceFile)
  {
    try
    {
      _currentSchemaLocation = new FileDataSchemaLocation(sourceFile);
      generateRootRequestBuilder(resource, sourceFile.getAbsolutePath(), new HashMap<String, String>());
    }
    catch (IOException e)
    {
      throw new RuntimeException("Error processing file [" + sourceFile.getAbsolutePath() + "] " + e.getMessage(), e);
    }
  }

  private RootBuilderSpec generateRootRequestBuilder(ResourceSchema resource,
                                                     String sourceFile,
                                                     Map<String, String> pathKeyTypes)
      throws IOException
  {
    ValidationResult validationResult = ValidateDataAgainstSchema.validate(resource.data(), resource.schema(), new ValidationOptions(RequiredMode.MUST_BE_PRESENT));
    if (!validationResult.isValid())
    {
      throw new IllegalArgumentException(String.format(
          "Resource validation error.  Resource File '%s', Error Details '%s'", sourceFile,
          validationResult.toString()));
    }

    String packageName = resource.getNamespace();
    String resourceName = CodeUtil.capitalize(resource.getName());
    String className;
    if (_version == RestliVersion.RESTLI_2_0_0)
    {
      className = getBuilderClassNameByVersion(RestliVersion.RESTLI_2_0_0, null, resource.getName(), true);
    }
    else
    {
      className = getBuilderClassNameByVersion(RestliVersion.RESTLI_1_0_0, null, resource.getName(), true);
    }

    RootBuilderSpec rootBuilderSpec = null;
    if (resource.hasCollection())
    {
      rootBuilderSpec = new CollectionRootBuilderSpec(resource);
    }
    else if (resource.hasSimple())
    {
      rootBuilderSpec = new SimpleRootBuilderSpec(resource);
    }
    else if (resource.hasActionsSet())
    {
      rootBuilderSpec = new ActionSetRootBuilderSpec(resource);
    }
    else
    {
      throw new IllegalArgumentException("unsupported resource type for resource: '" + resourceName + '\'');
    }
    rootBuilderSpec.setNamespace(packageName);
    rootBuilderSpec.setClassName(className);
    if (_version == RestliVersion.RESTLI_2_0_0)
    {
      rootBuilderSpec.setBaseClassName("BuilderBase");
    }
    rootBuilderSpec.setSourceIdlName(sourceFile);
    String resourcePath = getResourcePath(resource.getPath());
    rootBuilderSpec.setResourcePath(resourcePath);
    List<String> pathKeys = getPathKeys(resourcePath);
    rootBuilderSpec.setPathKeys(pathKeys);

    StringArray supportsList = null;
    RestMethodSchemaArray restMethods = null;
    FinderSchemaArray finders = null;
    ResourceSchemaArray subresources = null;
    ActionSchemaArray resourceActions = null;
    ActionSchemaArray entityActions = null;
    String keyClass = null;

    if (resource.getCollection() != null)
    {
      CollectionSchema collection = resource.getCollection();
      String keyName = collection.getIdentifier().getName();
      // Complex key is not supported
      keyClass = collection.getIdentifier().getType();
      pathKeyTypes.put(keyName, collection.getIdentifier().getType());
      supportsList = collection.getSupports();
      restMethods = collection.getMethods();
      finders = collection.getFinders();
      subresources = collection.getEntity().getSubresources();
      resourceActions = collection.getActions();
      entityActions = collection.getEntity().getActions();
    }
    else if (resource.getSimple() != null)
    {
      SimpleSchema simpleSchema = resource.getSimple();
      keyClass = "Void";
      supportsList = simpleSchema.getSupports();
      restMethods = simpleSchema.getMethods();
      subresources = simpleSchema.getEntity().getSubresources();
      resourceActions = simpleSchema.getActions();
    }
    else if (resource.getActionsSet() != null)
    {
      ActionsSetSchema actionsSet = resource.getActionsSet();
      resourceActions = actionsSet.getActions();
    }

    Set<ResourceMethod> supportedMethods = getSupportedMethods(supportsList);
    if (!supportedMethods.isEmpty())
    {
      for (ResourceMethod resourceMethod : supportedMethods)
      {
        validateResourceMethod(resource, resourceName, resourceMethod);
      }
    }

    List<RootBuilderMethodSpec> restMethodSpecs = new ArrayList<RootBuilderMethodSpec>();
    List<RootBuilderMethodSpec> finderSpecs = new ArrayList<RootBuilderMethodSpec>();
    List<RootBuilderMethodSpec> resourceActionSpecs = new ArrayList<RootBuilderMethodSpec>();
    List<RootBuilderMethodSpec> entityActionSpecs = new ArrayList<RootBuilderMethodSpec>();
    List<RootBuilderSpec> subresourceSpecs = new ArrayList<RootBuilderSpec>();
    String schemaClass = resource.getSchema();

    if (restMethods != null)
    {
      restMethodSpecs =
          generateBasicMethods(rootBuilderSpec, keyClass, schemaClass, supportedMethods, restMethods, resourceName,
                               pathKeys, pathKeyTypes);
    }

    if (finders != null)
    {
      finderSpecs =
          generateFinders(rootBuilderSpec, finders, keyClass, schemaClass, resourceName, pathKeys, pathKeyTypes);
    }

    if (resourceActions != null)
    {
      resourceActionSpecs =
          generateActions(rootBuilderSpec, resourceActions, keyClass, resourceName, pathKeys, pathKeyTypes);
    }

    if (entityActions != null)
    {
      entityActionSpecs =
          generateActions(rootBuilderSpec, entityActions, keyClass, resourceName, pathKeys, pathKeyTypes);
    }

    if (subresources != null)
    {
      subresourceSpecs = generateSubResources(sourceFile, subresources, pathKeyTypes);
    }

    // assign to rootBuilderClass
    if (rootBuilderSpec instanceof CollectionRootBuilderSpec)
    {
      CollectionRootBuilderSpec rootBuilder = (CollectionRootBuilderSpec) rootBuilderSpec;
      rootBuilder.setRestMethods(restMethodSpecs);
      rootBuilder.setFinders(finderSpecs);
      rootBuilder.setResourceActions(resourceActionSpecs);
      rootBuilder.setEntityActions(entityActionSpecs);
      rootBuilder.setSubresources(subresourceSpecs);
    }
    else if (rootBuilderSpec instanceof SimpleRootBuilderSpec)
    {
      SimpleRootBuilderSpec rootBuilder = (SimpleRootBuilderSpec) rootBuilderSpec;
      rootBuilder.setRestMethods(restMethodSpecs);
      rootBuilder.setResourceActions(resourceActionSpecs);
      rootBuilder.setSubresources(subresourceSpecs);
    }
    else if (rootBuilderSpec instanceof ActionSetRootBuilderSpec)
    {
      ActionSetRootBuilderSpec rootBuilder = (ActionSetRootBuilderSpec) rootBuilderSpec;
      rootBuilder.setResourceActions(resourceActionSpecs);
    }
    registerBuilderSpec(rootBuilderSpec);
    return rootBuilderSpec;
  }

  private static List<String> fixOldStylePathKeys(List<String> pathKeys,
                                                  String resourcePath,
                                                  Map<String, List<String>> pathToAssocKeys)
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
    for (Map.Entry<String, List<String>> entry : toReverse.entrySet())
    {
      for (String element : entry.getValue())
      {
        reversed.put(element, entry.getKey());
      }
    }
    return reversed;
  }

  private static void validateResourceMethod(ResourceSchema resourceSchema,
                                             String resourceName,
                                             ResourceMethod resourceMethod)
  {
    if (resourceSchema.getSimple() != null && !RestConstants.SIMPLE_RESOURCE_METHODS.contains(resourceMethod))
    {
      throw new IllegalArgumentException(
          String.format("'%s' is not a supported method on resource: '%s'", resourceMethod.toString(), resourceName));
    }
  }

  private static List<String> getPathKeys(String basePath)
  {
    UriTemplate template = new UriTemplate(basePath);
    return fixOldStylePathKeys(template.getTemplateVariables(), basePath, new HashMap<String, List<String>>());
  }

  private List<RootBuilderSpec> generateSubResources(String sourceFile,
                                                     ResourceSchemaArray subresources,
                                                     Map<String, String> pathKeyTypes)
      throws IOException
  {
    List<RootBuilderSpec> subSpecList = new ArrayList<RootBuilderSpec>();
    if (subresources != null)
    {
      for (ResourceSchema resource : subresources)
      {
        RootBuilderSpec resourceSpec = generateRootRequestBuilder(resource, sourceFile, pathKeyTypes);
        subSpecList.add(resourceSpec);
      }
    }
    return subSpecList;
  }

  private List<RootBuilderMethodSpec> generateFinders(RootBuilderSpec rootBuilderSpec,
                                                      FinderSchemaArray finderSchemas,
                                                      String keyClass,
                                                      String valueClass,
                                                      String resourceName,
                                                      List<String> pathKeys,
                                                      Map<String, String> pathKeyTypes)
  {
    List<RootBuilderMethodSpec> finderSpecList = new ArrayList<RootBuilderMethodSpec>();
    if (finderSchemas != null)
    {
      String baseBuilderClass = getBuilderBase(ResourceMethod.FINDER);

      for (FinderSchema finder : finderSchemas)
      {
        String finderName = finder.getName();

        String builderName =
            CodeUtil.capitalize(resourceName) + "FindBy" + CodeUtil.capitalize(finderName) + getMethodBuilderSuffix();
        FinderBuilderSpec finderBuilderClass =
            generateFinderRequestBuilder(rootBuilderSpec.getResource(), baseBuilderClass, keyClass, valueClass, builderName,
                                         rootBuilderSpec.getNamespace(), finderName);

        generatePathKeyBindingMethods(pathKeys, finderBuilderClass, pathKeyTypes);

        if (finder.getParameters() != null)
        {
          generateQueryParamBindingMethods(finder.getParameters(), finderBuilderClass);
        }

        // process custom metadata
        if (finder.getMetadata() != null)
        {
          String metadataClass = finder.getMetadata().getType();
          ClassTemplateSpec metadataClassSpec = classToTemplateSpec(metadataClass);
          finderBuilderClass.setMetadataType(metadataClassSpec);
        }
        String finderMethod = "findBy" + CodeUtil.capitalize(finderName);
        RootBuilderMethodSpec methodSpec =
            new RootBuilderMethodSpec(finderMethod, finder.getDoc(), finderBuilderClass, rootBuilderSpec);
        finderBuilderClass.setRootBuilderMethod(methodSpec);
        finderSpecList.add(methodSpec);
      }
    }
    return finderSpecList;
  }

  private RestMethodBuilderSpec generateRestMethodRequestBuilder(ResourceSchema resource,
                                                                 String baseBuilderClass,
                                                                 String keyClass,
                                                                 String valueClass,
                                                                 String requestBuilderName,
                                                                 String clientPackage,
                                                                 RestMethodSchema schema)
  {
    // this method applies to REST methods
    RestMethodBuilderSpec restMethodBuilderClass =
        new RestMethodBuilderSpec(clientPackage, requestBuilderName, baseBuilderClass, resource, schema.getMethod());
    final ClassTemplateSpec keyClassSpec = classToTemplateSpec(keyClass);
    restMethodBuilderClass.setKeyClass(keyClassSpec);
    final ClassTemplateSpec valueClassSpec = classToTemplateSpec(valueClass);
    restMethodBuilderClass.setValueClass(valueClassSpec);
    registerBuilderSpec(restMethodBuilderClass);

    return restMethodBuilderClass;
  }

  private FinderBuilderSpec generateFinderRequestBuilder(ResourceSchema resource,
                                                         String baseBuilderClass,
                                                         String keyClass,
                                                         String valueClass,
                                                         String requestBuilderName,
                                                         String clientPackage,
                                                         String finderName)
  {
    // this method applies to finder
    FinderBuilderSpec finderBuilderClass = new FinderBuilderSpec(clientPackage, requestBuilderName, baseBuilderClass, resource);
    final ClassTemplateSpec keyClassSpec = classToTemplateSpec(keyClass);
    finderBuilderClass.setKeyClass(keyClassSpec);
    final ClassTemplateSpec valueClassSpec = classToTemplateSpec(valueClass);
    finderBuilderClass.setValueClass(valueClassSpec);
    finderBuilderClass.setFinderName(finderName);
    registerBuilderSpec(finderBuilderClass);

    return finderBuilderClass;
  }

  private ActionBuilderSpec generateActionRequestBuilder(ResourceSchema resource,
                                                         String baseBuilderClass,
                                                         String keyClass,
                                                         String returnType,
                                                         String requestBuilderName,
                                                         String clientPackage,
                                                         ActionSchema schema)
  {
    ActionBuilderSpec actionBuilderClass =
        new ActionBuilderSpec(clientPackage, requestBuilderName, baseBuilderClass, resource, schema.getName());
    final ClassTemplateSpec keyClassSpec = classToTemplateSpec(keyClass);
    actionBuilderClass.setKeyClass(keyClassSpec);
    final ClassTemplateSpec returnClassSpec = classToTemplateSpec(returnType);
    actionBuilderClass.setValueClass(returnClassSpec);
    registerBuilderSpec(actionBuilderClass);

    return actionBuilderClass;
  }

  private List<RootBuilderMethodSpec> generateActions(RootBuilderSpec rootBuilderSpec,
                                                      ActionSchemaArray actions,
                                                      String keyClass,
                                                      String resourceName,
                                                      List<String> pathKeys,
                                                      Map<String, String> pathKeyTypes)
  {
    List<RootBuilderMethodSpec> actionSpecList = new ArrayList<RootBuilderMethodSpec>();
    if (actions != null)
    {
      for (ActionSchema action : actions)
      {
        RootBuilderMethodSpec actionSpec =
            generateActionMethod(rootBuilderSpec, keyClass, action, resourceName, pathKeys, pathKeyTypes);
        actionSpecList.add(actionSpec);
      }
    }
    return actionSpecList;
  }

  private RootBuilderMethodSpec generateActionMethod(RootBuilderSpec rootBuilderSpec,
                                                     String keyClass,
                                                     ActionSchema action,
                                                     String resourceName,
                                                     List<String> pathKeys,
                                                     Map<String, String> pathKeyTypes)
  {
    String actionName = action.getName();
    String returnType = action.getReturns();
    String actionBuilderClassName =
        CodeUtil.capitalize(resourceName) + "Do" + CodeUtil.capitalize(actionName) + getMethodBuilderSuffix();
    ActionBuilderSpec actionBuilderClass =
        generateActionRequestBuilder(rootBuilderSpec.getResource(), getBuilderBase(ResourceMethod.ACTION), keyClass, returnType,
                                     actionBuilderClassName, rootBuilderSpec.getNamespace(), action);

    if (action.hasParameters())
    {
      generateActionParamBindingMethods(action.getParameters(), actionBuilderClass);
    }
    generatePathKeyBindingMethods(pathKeys, actionBuilderClass, pathKeyTypes);

    String actionMethodName = "action" + CodeUtil.capitalize(actionName);
    RootBuilderMethodSpec actionMethod =
        new RootBuilderMethodSpec(actionMethodName, action.getDoc(), actionBuilderClass, rootBuilderSpec);
    actionBuilderClass.setRootBuilderMethod(actionMethod);
    return actionMethod;
  }

  @SuppressWarnings("deprecation")
  private List<RootBuilderMethodSpec> generateBasicMethods(RootBuilderSpec rootBuilderSpec,
                                                           String keyClass,
                                                           String valueClass,
                                                           Set<ResourceMethod> supportedMethods,
                                                           RestMethodSchemaArray restMethods,
                                                           String resourceName,
                                                           List<String> pathKeys,
                                                           Map<String, String> pathKeyTypes)
  {
    final Map<ResourceMethod, RestMethodSchema> schemaMap = new HashMap<ResourceMethod, RestMethodSchema>();
    if (restMethods != null)
    {
      for (RestMethodSchema restMethod : restMethods)
      {
        schemaMap.put(ResourceMethod.fromString(restMethod.getMethod()), restMethod);
      }
    }

    List<RootBuilderMethodSpec> methodSpecList = new ArrayList<RootBuilderMethodSpec>();
    for (Map.Entry<ResourceMethod, String> entry : _builderBaseMap.entrySet())
    {
      ResourceMethod method = entry.getKey();
      if (supportedMethods.contains(method))
      {
        String methodName = RestLiToolsUtils.normalizeUnderscores(method.toString());

        final RestMethodSchema schema = schemaMap.get(method);
        RestMethodBuilderSpec requestBuilder = generateRestMethodRequestBuilder(rootBuilderSpec.getResource(), entry.getValue(), keyClass, valueClass,
                                                                                resourceName + RestLiToolsUtils.nameCapsCase(methodName) + getMethodBuilderSuffix(),
                                                                                rootBuilderSpec.getNamespace(), schema);
        generatePathKeyBindingMethods(pathKeys, requestBuilder, pathKeyTypes);

        if (schema != null && schema.hasParameters())
        {
          this.generateQueryParamBindingMethods(schema.getParameters(), requestBuilder);
        }

        RootBuilderMethodSpec methodSpec =
            new RootBuilderMethodSpec(RestLiToolsUtils.nameCamelCase(methodName), schema.getDoc(), requestBuilder, rootBuilderSpec);
        requestBuilder.setRootBuilderMethod(methodSpec);
        methodSpecList.add(methodSpec);
      }
    }
    return methodSpecList;
  }

  private void generatePathKeyBindingMethods(List<String> pathKeys,
                                             RequestBuilderSpec builderClass,
                                             Map<String, String> pathKeyTypes)
  {
    for (String pathKey : pathKeys)
    {
      PathKeyBindingMethodSpec spec = new PathKeyBindingMethodSpec();
      spec.setPathKey(pathKey);
      spec.setMethodName(RestLiToolsUtils.nameCamelCase(pathKey + "Key"));
      final ClassTemplateSpec argTypeClassSpec = classToTemplateSpec(pathKeyTypes.get(pathKey));
      spec.setArgType(argTypeClassSpec);
      builderClass.addPathKeyMethod(spec);
    }
  }

  private void generateQueryParamBindingMethods(ParameterSchemaArray parameters, RequestBuilderSpec requestBuilderClass)
  {
    for (ParameterSchema param : parameters)
    {
      final String paramName = param.getName();
      final DataSchema typeSchema = RestSpecCodec.textToSchema(param.getType(), _schemaResolver);
      final ClassTemplateSpec typeClassSpec = schemaToTemplateSpec(typeSchema);
      final boolean isOptional = RestLiToolsUtils.isParameterOptional(param);
      final String setMethodName = RestLiToolsUtils.nameCamelCase(paramName + "Param");
      final QueryParamBindingMethodSpec spec = new QueryParamBindingMethodSpec(setMethodName);
      spec.setParamName(paramName);
      spec.setNeedAddParamMethod(false);
      spec.setOptional(isOptional);
      spec.setArgType(typeClassSpec);
      spec.setDoc(param.getDoc());
      requestBuilderClass.addQueryParamMethod(spec);

      if (typeSchema instanceof ArrayDataSchema)
      {
        final DataSchema itemsSchema = ((ArrayDataSchema) typeSchema).getItems();
        final ClassTemplateSpec itemSpecClass = schemaToTemplateSpec(itemsSchema);
        final String addMethodName =
            RestLiToolsUtils.nameCamelCase("add" + RestLiToolsUtils.normalizeCaps(paramName) + "Param");
        final QueryParamBindingMethodSpec addSpec = new QueryParamBindingMethodSpec(addMethodName);
        addSpec.setParamName(paramName);
        addSpec.setNeedAddParamMethod(true);
        addSpec.setOptional(isOptional);
        addSpec.setArgType(itemSpecClass);
        addSpec.setDoc(param.getDoc());
        requestBuilderClass.addQueryParamMethod(addSpec);
      }
    }
  }

  private void generateActionParamBindingMethods(ParameterSchemaArray parameters, ActionBuilderSpec requestBuilderClass)
  {
    for (ParameterSchema param : parameters)
    {
      final String paramName = param.getName();
      final ClassTemplateSpec typeClassSpec = classToTemplateSpec(param.getType());
      final boolean isOptional = RestLiToolsUtils.isParameterOptional(param);
      final String setMethodName = RestLiToolsUtils.nameCamelCase(paramName + "Param");
      ActionParamBindingMethodSpec spec = new ActionParamBindingMethodSpec(setMethodName);
      spec.setParamName(paramName);
      spec.setOptional(isOptional);
      spec.setArgType(typeClassSpec);
      spec.setDoc(param.getDoc());
      requestBuilderClass.addActionParamMethod(spec);
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
    if (supportsList == null)
    {
      return Collections.emptySet();
    }
    Set<ResourceMethod> supportedMethods = EnumSet.noneOf(ResourceMethod.class);
    for (String methodEntry : supportsList)
    {
      supportedMethods.add(ResourceMethod.fromString(methodEntry));
    }
    return supportedMethods;
  }

  private ClassTemplateSpec classToTemplateSpec(String classname)
  {
    if (classname == null || "Void".equals(classname))
    {
      return null;
    }
    else
    {
      final DataSchema typeSchema = RestSpecCodec.textToSchema(classname, _schemaResolver);
      return schemaToTemplateSpec(typeSchema);
    }
  }

  private ClassTemplateSpec schemaToTemplateSpec(DataSchema dataSchema)
  {
    // convert from DataSchema to ClassTemplateSpec
    return _templateSpecGenerator.generate(dataSchema, _currentSchemaLocation);
  }

  private String getMethodBuilderSuffix()
  {
    return _customMethodBuilderSuffix == null ? METHOD_BUILDER_SUFFIX.get(_version) : _customMethodBuilderSuffix;
  }
}

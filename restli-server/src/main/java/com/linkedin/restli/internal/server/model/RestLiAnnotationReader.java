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

package com.linkedin.restli.internal.server.model;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.data.element.DataElement;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.HasTyperefInfo;
import com.linkedin.data.template.KeyCoercer;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.data.template.TyperefInfo;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.common.validation.CreateOnly;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.internal.common.ReflectionUtils;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.NoMetadata;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.UnstructuredDataReactiveReader;
import com.linkedin.restli.server.UnstructuredDataWriter;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.AlternativeKey;
import com.linkedin.restli.server.annotations.AlternativeKeys;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.HeaderParam;
import com.linkedin.restli.server.annotations.MetadataProjectionParam;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.PagingProjectionParam;
import com.linkedin.restli.server.annotations.ParSeqContextParam;
import com.linkedin.restli.server.annotations.ParamError;
import com.linkedin.restli.server.annotations.PathKeyParam;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.ProjectionParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.ResourceContextParam;
import com.linkedin.restli.server.annotations.RestAnnotations;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiAttachmentsParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestLiTemplate;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ServiceErrorDef;
import com.linkedin.restli.server.annotations.ServiceErrors;
import com.linkedin.restli.server.annotations.SuccessResponse;
import com.linkedin.restli.server.annotations.UnstructuredDataReactiveReaderParam;
import com.linkedin.restli.server.annotations.UnstructuredDataWriterParam;
import com.linkedin.restli.server.annotations.ValidatorParam;
import com.linkedin.restli.server.errors.ServiceError;
import com.linkedin.restli.server.errors.ParametersServiceError;
import com.linkedin.restli.server.resources.ComplexKeyResource;
import com.linkedin.restli.server.resources.ComplexKeyResourceAsync;
import com.linkedin.restli.server.resources.ComplexKeyResourceTask;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.SingleObjectResource;
import com.linkedin.restli.server.resources.unstructuredData.KeyUnstructuredDataResource;
import com.linkedin.restli.server.resources.unstructuredData.SingleUnstructuredDataResource;
import com.linkedin.util.CustomTypeUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.restli.internal.server.model.ResourceModelEncoder.*;


/**
 * Collection of static helper methods used to read a Rest.li resource class and produce a {@link ResourceModel}.
 *
 * @author dellamag
 */
public final class RestLiAnnotationReader
{
  private static final int DEFAULT_METADATA_PARAMETER_INDEX = 1;
  private static final int BATCH_FINDER_METADATA_PARAMETER_INDEX = 2;
  private static final int BATCH_FINDER_MISSING_PARAMETER_INDEX = -1;
  private static final Logger log = LoggerFactory.getLogger(RestLiAnnotationReader.class);
  private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("\\W");
  private static final Set<ResourceMethod> POST_OR_PUT_RESOURCE_METHODS = new HashSet<>(Arrays.asList(ResourceMethod.ACTION,
                                                                                                      ResourceMethod.BATCH_CREATE,
                                                                                                      ResourceMethod.BATCH_PARTIAL_UPDATE,
                                                                                                      ResourceMethod.BATCH_UPDATE,
                                                                                                      ResourceMethod.CREATE,
                                                                                                      ResourceMethod.PARTIAL_UPDATE,
                                                                                                      ResourceMethod.UPDATE));

  /**
   * This is a utility class.
   */
  private RestLiAnnotationReader()
  {
  }

  /**
   * Processes an annotated resource class, producing a {@link ResourceModel}.
   *
   * @param resourceClass annotated resource class
   * @return {@link ResourceModel} for the provided resource class
   */
  public static ResourceModel processResource(final Class<?> resourceClass)
  {
    return processResource(resourceClass, null);
  }

  /**
   * Processes an annotated resource class, producing a {@link ResourceModel}.
   *
   * @param resourceClass annotated resource class
   * @return {@link ResourceModel} for the provided resource class
   */
  public static ResourceModel processResource(final Class<?> resourceClass, ResourceModel parentResourceModel)
  {
    checkAnnotation(resourceClass);
    ResourceModel model = buildBaseResourceModel(resourceClass, parentResourceModel);

    if (parentResourceModel != null)
    {
      parentResourceModel.addSubResource(model.getName(), model);
    }

    if (model.getValueClass() != null)
    {
      checkRestLiDataAnnotations(resourceClass, (RecordDataSchema) getDataSchema(model.getValueClass(), null));
    }

    addAlternativeKeys(model, resourceClass);
    addServiceErrors(model, resourceClass);
    validateServiceErrors(model, resourceClass);

    DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(resourceClass.getAnnotations());
    addDeprecatedAnnotation(annotationsMap, resourceClass);
    model.setCustomAnnotation(annotationsMap);

    return model;
  }

  private static ResourceModel buildBaseResourceModel(Class<?> resourceClass, ResourceModel parentResourceModel)
  {
    if (resourceClass.isAnnotationPresent(RestLiCollection.class) ||
        resourceClass.isAnnotationPresent(RestLiAssociation.class))
    {
      if (KeyValueResource.class.isAssignableFrom(resourceClass) ||
          KeyUnstructuredDataResource.class.isAssignableFrom(resourceClass))
      {
        return processCollection(resourceClass, parentResourceModel);
      }
      else
      {
        throw new RestLiInternalException(
            "Resource class '" + resourceClass.getName() +
            "' declares RestLiCollection/RestLiAssociation annotation but does not implement " +
            KeyValueResource.class.getName() + " or " + KeyUnstructuredDataResource.class.getName() + " interface.");
      }
    }
    else if (resourceClass.isAnnotationPresent(RestLiActions.class))
    {
      return processActions(resourceClass, parentResourceModel);
    }
    else if (resourceClass.isAnnotationPresent(RestLiSimpleResource.class))
    {
      if (SingleObjectResource.class.isAssignableFrom(resourceClass) ||
          SingleUnstructuredDataResource.class.isAssignableFrom(resourceClass))
      {
        return processSimpleResource(resourceClass, parentResourceModel);
      }
      else
      {
        throw new RestLiInternalException(
          "Resource class '" + resourceClass.getName() +
            "' declares RestLiSimpleResource annotation but does not implement " +
            SingleObjectResource.class.getName() + " or " + SingleUnstructuredDataResource.class.getName() + " interface.");
      }
    }
    else
    {
      throw new ResourceConfigException("Class '" + resourceClass.getName()
          + "' must be annotated with a valid @RestLi... annotation");
    }
  }

  /**
   * Add alternative keys, if there are any, to the given model.
   *
   * @param model The {@link com.linkedin.restli.internal.server.model.ResourceModel} that we are building.
   * @param resourceClass The resource {@link java.lang.Class}.
   */
  private static void addAlternativeKeys(ResourceModel model, Class<?> resourceClass)
  {
    if (resourceClass.isAnnotationPresent(AlternativeKey.class) || resourceClass.isAnnotationPresent(AlternativeKeys.class))
    {
      AlternativeKey[] alternativeKeyAnnotations;
      if (resourceClass.isAnnotationPresent(AlternativeKeys.class))
      {
        alternativeKeyAnnotations = resourceClass.getAnnotation(AlternativeKeys.class).alternativeKeys();
      }
      else //(resourceClass.isAnnotationPresent(AlternativeKey.class) == true)
      {
        alternativeKeyAnnotations = new AlternativeKey[]{resourceClass.getAnnotation(AlternativeKey.class)};
      }

      Map<String, com.linkedin.restli.server.AlternativeKey<?, ?>> alternativeKeyMap = new HashMap<String, com.linkedin.restli.server.AlternativeKey<?, ?>>(alternativeKeyAnnotations.length);
      for (AlternativeKey altKeyAnnotation : alternativeKeyAnnotations)
      {
        @SuppressWarnings("unchecked")
        com.linkedin.restli.server.AlternativeKey<?, ?> altKey = buildAlternativeKey(model.getName(), altKeyAnnotation);
        alternativeKeyMap.put(altKeyAnnotation.name(), altKey);
      }
      model.putAlternativeKeys(alternativeKeyMap);
    }
    else
    {
      model.putAlternativeKeys(new HashMap<String, com.linkedin.restli.server.AlternativeKey<?, ?>>());
    }
  }

  /**
   * Create an {@link com.linkedin.restli.server.AlternativeKey} object from an {@link com.linkedin.restli.server.annotations.AlternativeKey} annotation.
   *
   * @param resourceName Name of the resource.
   * @param altKeyAnnotation The {@link com.linkedin.restli.server.annotations.AlternativeKey} annotation.
   * @return {@link com.linkedin.restli.server.AlternativeKey} object.
   */
  private static com.linkedin.restli.server.AlternativeKey<?, ?> buildAlternativeKey(String resourceName,
                                                                                     AlternativeKey altKeyAnnotation)
  {
    String keyName = altKeyAnnotation.name();
    Class<?> keyType = altKeyAnnotation.keyType();
    Class<? extends TyperefInfo> altKeyTyperef = altKeyAnnotation.keyTyperefClass();

    KeyCoercer<?, ?> keyCoercer;
    try
    {
      keyCoercer = altKeyAnnotation.keyCoercer().newInstance();
    }
    catch (InstantiationException e)
    {
      throw new ResourceConfigException(String.format("KeyCoercer for alternative key '%s' on resource %s cannot be instantiated, %s",
                                                      keyName, resourceName, e.getMessage()),
                                        e);
    }
    catch (IllegalAccessException e)
    {
      throw new ResourceConfigException(String.format("KeyCoercer for alternative key '%s' on resource %s cannot be instantiated, %s",
                                                      keyName, resourceName, e.getMessage()),
                                        e);
    }

    try
    {
      @SuppressWarnings({"unchecked", "rawtypes"})
      com.linkedin.restli.server.AlternativeKey<?, ?> altKey =
        new com.linkedin.restli.server.AlternativeKey(keyCoercer,
                                                      keyType,
                                                      getDataSchema(keyType, getSchemaFromTyperefInfo(altKeyTyperef)));
      return altKey;
    }
    catch (TemplateRuntimeException e)
    {
      throw new ResourceConfigException(String.format("DataSchema for alternative key '%s' of type %s on resource %s cannot be found; type is invalid or requires typeref.",
                                                      keyName, keyType, resourceName),
                                        e);
    }
    catch (Exception e)
    {
      throw new ResourceConfigException(String.format("Typeref for alternative key '%s' on resource %s cannot be instantiated, %s", keyName, resourceName, e.getMessage()), e);
    }
  }

  /**
   * Check if resourceClass is annotated with the expected annotation of its template class,
   * expressed in the {@link RestLiTemplate} annotation.
   */
  private static void checkAnnotation(final Class<?> resourceClass)
  {
    Class<?> templateClass = resourceClass;
    while (templateClass != Object.class)
    {
      templateClass = templateClass.getSuperclass();

      final RestLiTemplate templateAnnotation = templateClass.getAnnotation(RestLiTemplate.class);
      if (templateAnnotation != null)
      {
        final Class<? extends Annotation> currentExpect = templateAnnotation.expectedAnnotation();
        if (currentExpect == RestLiCollection.class ||
            currentExpect == RestLiAssociation.class ||
            currentExpect == RestLiSimpleResource.class)
        {
          if (resourceClass.getAnnotation(currentExpect) == null)
          {
            throw new ResourceConfigException(resourceClass.getName() + " is not annotated with "
              + currentExpect.getName() + ", expected by " + templateClass.getName());
          }
          else
          {
            return;
          }
        }
        else
        {
          throw new ResourceConfigException("Unknown expected annotation " + currentExpect.getName()
            + " from " + templateClass.getName());
        }
      }
    }
  }

  private static DataMap addDeprecatedAnnotation(DataMap annotationsMap, Class<?> clazz)
  {
    if(clazz.isAnnotationPresent(Deprecated.class))
    {
      annotationsMap.put(DEPRECATED_ANNOTATION_NAME, new DataMap());
    }
    return annotationsMap;
  }

  private static DataMap addDeprecatedAnnotation(DataMap annotationsMap, AnnotatedElement annotatedElement)
  {
    if(annotatedElement.isAnnotationPresent(Deprecated.class))
    {
      annotationsMap.put(DEPRECATED_ANNOTATION_NAME, new DataMap());
    }
    return annotationsMap;
  }

  private static void checkPathsAgainstSchema(RecordDataSchema dataSchema, String resourceClassName, String annotationName, String[] paths)
  {
    for (String path : paths)
    {
      if (!DataSchemaUtil.containsPath(dataSchema, path))
      {
        throw new ResourceConfigException("In resource class '" + resourceClassName + "', "
                                              + annotationName + " annotation " + path + " is not a valid path for "
                                              + dataSchema.getName() + ".");
      }
    }
  }

  private static void checkRestLiDataAnnotations(final Class<?> resourceClass, RecordDataSchema dataSchema)
  {
    Map<String, String[]> annotations = new HashMap<String, String[]>();
    if (resourceClass.isAnnotationPresent(ReadOnly.class))
    {
      annotations.put(ReadOnly.class.getSimpleName(), resourceClass.getAnnotation(ReadOnly.class).value());
    }
    if (resourceClass.isAnnotationPresent(CreateOnly.class))
    {
      annotations.put(CreateOnly.class.getSimpleName(), resourceClass.getAnnotation(CreateOnly.class).value());
    }
    String resourceClassName = resourceClass.getName();
    // Check paths are valid.
    for (Map.Entry<String, String[]> annotationEntry : annotations.entrySet())
    {
      checkPathsAgainstSchema(dataSchema, resourceClassName, annotationEntry.getKey(), annotationEntry.getValue());
    }
    // Check for redundant or conflicting information.
    Map<String, String> pathToAnnotation = new HashMap<String, String>();
    for (Map.Entry<String, String[]> annotationEntry : annotations.entrySet())
    {
      String annotationName = annotationEntry.getKey();
      String[] paths = annotationEntry.getValue();
      for (String path : paths)
      {
        String existingAnnotationName = pathToAnnotation.get(path);
        if (existingAnnotationName != null)
        {
          if (existingAnnotationName.equals(annotationName))
          {
            throw new ResourceConfigException("In resource class '" + resourceClassName + "', "
                                                  + path + " is marked as " + annotationName + " multiple times.");
          }
          else
          {
            throw new ResourceConfigException("In resource class '" + resourceClassName + "', " + path
                                                  + " is marked as both " + existingAnnotationName + " and "
                                                  + annotationName + ".");
          }
        }
        for (Map.Entry<String, String> existingEntry : pathToAnnotation.entrySet())
        {
          String existingPath = existingEntry.getKey();
          existingAnnotationName = existingEntry.getValue();
          // Avoid marking 'field' and 'field1' as overlapping paths
          String existingPathWithSeparator = existingPath + DataElement.SEPARATOR;
          String pathWithSeparator = path + DataElement.SEPARATOR;
          if (existingPathWithSeparator.startsWith(pathWithSeparator))
          {
            throw new ResourceConfigException("In resource class '" + resourceClassName + "', " + existingPath
                                                  + " is marked as " + existingAnnotationName + ", but is contained in a "
                                                  + annotationName + " field " + path + ".");
          }
          else if (pathWithSeparator.startsWith(existingPathWithSeparator))
          {
            throw new ResourceConfigException("In resource class '" + resourceClassName + "', " + path
                                                  + " is marked as " + annotationName + ", but is contained in a "
                                                  + existingAnnotationName + " field " + existingPath + ".");
          }
        }
        pathToAnnotation.put(path, annotationName);
      }
    }
  }


  @SuppressWarnings({"unchecked","deprecation"})
  private static ResourceModel processCollection(final Class<?> collectionResourceClass,
                                                 ResourceModel parentResourceModel)
  {
    Class<?> keyClass;
    Class<? extends RecordTemplate> keyKeyClass = null;
    Class<? extends RecordTemplate> keyParamsClass = null;
    Class<? extends RecordTemplate> valueClass = null;
    Class<?> complexKeyResourceBase = null;
    // If ComplexKeyResource or ComplexKeyResourceAsync, the parameters are Key type K, Params type P and Resource
    // type V and the resource key type is ComplexResourceKey<K,P>
    if (ComplexKeyResource.class.isAssignableFrom(collectionResourceClass))
    {
      complexKeyResourceBase = ComplexKeyResource.class;
    }
    else if (ComplexKeyResourceAsync.class.isAssignableFrom(collectionResourceClass))
    {
      complexKeyResourceBase = ComplexKeyResourceAsync.class;
    }
    else if (ComplexKeyResourceTask.class.isAssignableFrom(collectionResourceClass))
    {
      complexKeyResourceBase = ComplexKeyResourceTask.class;
    }
    else if (com.linkedin.restli.server.resources.ComplexKeyResourcePromise.class.isAssignableFrom(collectionResourceClass))
    {
      complexKeyResourceBase = com.linkedin.restli.server.resources.ComplexKeyResourcePromise.class;
    }

    if (complexKeyResourceBase != null)
    {
      List<Class<?>> kvParams;
      if (complexKeyResourceBase.equals(ComplexKeyResource.class))
      {
        kvParams = ReflectionUtils.getTypeArguments(ComplexKeyResource.class,
                                                    (Class<? extends ComplexKeyResource<?, ?, ?>>) collectionResourceClass);
      }
      else if (complexKeyResourceBase.equals(ComplexKeyResourceAsync.class))
      {
        kvParams = ReflectionUtils.getTypeArguments(ComplexKeyResourceAsync.class,
                                                    (Class<? extends ComplexKeyResourceAsync<?, ?, ?>>) collectionResourceClass);
      }
      else if (complexKeyResourceBase.equals(ComplexKeyResourceTask.class))
      {
        kvParams = ReflectionUtils.getTypeArguments(ComplexKeyResourceTask.class,
                                                    (Class<? extends ComplexKeyResourceTask<?, ?, ?>>) collectionResourceClass);
      }
      else
      {
        kvParams = ReflectionUtils.getTypeArguments(com.linkedin.restli.server.resources.ComplexKeyResourcePromise.class,
                                                    (Class<? extends com.linkedin.restli.server.resources.ComplexKeyResourcePromise<?, ?, ?>>) collectionResourceClass);
      }

      keyClass = ComplexResourceKey.class;
      keyKeyClass = kvParams.get(0).asSubclass(RecordTemplate.class);
      keyParamsClass = kvParams.get(1).asSubclass(RecordTemplate.class);
      valueClass = kvParams.get(2).asSubclass(RecordTemplate.class);
    }

    // Otherwise, it is either:
    // - A KeyValueResource, whose parameters are resource key and resource value
    // - A KeyUnstructuredDataResource, whose parameter is resource key
    else
    {
      List<Type> actualTypeArguments = null;

      if (KeyValueResource.class.isAssignableFrom(collectionResourceClass))
      {
        @SuppressWarnings("unchecked")
        Class<? extends KeyValueResource<?, ?>> clazz = (Class<? extends KeyValueResource<?, ?>>) collectionResourceClass;
        actualTypeArguments = ReflectionUtils.getTypeArgumentsParametrized(KeyValueResource.class, clazz);

        // 2nd type parameter must be the resource value class
        valueClass = ReflectionUtils.getClass(actualTypeArguments.get(1)).asSubclass(RecordTemplate.class);
      }
      else if (KeyUnstructuredDataResource.class.isAssignableFrom(collectionResourceClass))
      {
        @SuppressWarnings("unchecked")
        Class<? extends KeyUnstructuredDataResource<?>> clazz = (Class<? extends KeyUnstructuredDataResource<?>>) collectionResourceClass;
        actualTypeArguments = ReflectionUtils.getTypeArgumentsParametrized(KeyUnstructuredDataResource.class, clazz);
      }

      keyClass = ReflectionUtils.getClass(actualTypeArguments.get(0));
      if (RecordTemplate.class.isAssignableFrom(keyClass))
      {
        // a complex key is being used and thus ComplexKeyResource should be implemented so that we can wrap it in a
        // ComplexResourceKey
        throw new ResourceConfigException("Class '" + collectionResourceClass.getName() +
                                              "' should implement 'ComplexKeyResource' as a complex key '" +
                                              keyClass.getName() + "' is being used.");
      }
      else if (TyperefInfo.class.isAssignableFrom(keyClass))
      {
        throw new ResourceConfigException("Typeref '" + keyClass.getName() + "' cannot be key type for class '" +
                                              collectionResourceClass.getName() + "'.");
      }

      if (keyClass.equals(ComplexResourceKey.class))
      {
        @SuppressWarnings("unchecked")
        Type[] typeArguments = ((ParameterizedType)actualTypeArguments.get(0)).getActualTypeArguments();
        keyKeyClass = ReflectionUtils.getClass(typeArguments[0]).asSubclass(RecordTemplate.class);
        keyParamsClass = ReflectionUtils.getClass(typeArguments[1]).asSubclass(RecordTemplate.class);
      }
    }

    ResourceType resourceType = getResourceType(collectionResourceClass);

    RestLiAnnotationData annotationData = getRestLiAnnotationData(collectionResourceClass);

    String name = annotationData.name();
    String namespace = annotationData.namespace();

    String keyName;
    if (annotationData.keyName() == null)
    {
      keyName = name + "Id";
    }
    else
    {
      keyName = annotationData.keyName();
    }

    Key primaryKey = buildKey(name, keyName, keyClass, annotationData.typerefInfoClass());
    Set<Key> keys = new HashSet<Key>();
    if (annotationData.keys() == null)
    {
      keys.add(primaryKey);
    }
    else
    {
      keys.addAll(buildKeys(name, annotationData.keys()));
    }

    Class<?> parentResourceClass =
        annotationData.parent().equals(RestAnnotations.ROOT.class) ? null
            : annotationData.parent();

    ResourceModel collectionModel =
        new ResourceModel(primaryKey,
                          keyKeyClass,
                          keyParamsClass,
                          keys,
                          valueClass,
                          collectionResourceClass,
                          parentResourceClass,
                          name,
                          resourceType,
                          namespace);

    collectionModel.setParentResourceModel(parentResourceModel);

    addResourceMethods(collectionResourceClass, collectionModel);

    log.debug("Processed collection resource '" + collectionResourceClass.getName() + "'");

    return collectionModel;
  }

  private static RestLiAnnotationData getRestLiAnnotationData(Class<?> resourceClass)
  {
    RestLiAnnotationData annotationData;
    if (resourceClass.isAnnotationPresent(RestLiCollection.class))
    {
      return new RestLiAnnotationData(resourceClass.getAnnotation(RestLiCollection.class));
    }
    else if (resourceClass.isAnnotationPresent(RestLiAssociation.class))
    {
      return new RestLiAnnotationData(resourceClass.getAnnotation(RestLiAssociation.class));
    }
    else if (resourceClass.isAnnotationPresent(RestLiSimpleResource.class))
    {
      return new RestLiAnnotationData(resourceClass.getAnnotation(RestLiSimpleResource.class));
    }
    else
    {
      throw new ResourceConfigException("No valid annotation on resource class '"
          + resourceClass.getName() + "'");
    }
  }

  private static ResourceModel processSimpleResource(final Class<?> resourceClass, ResourceModel parentResource)
  {
    Class<? extends RecordTemplate> valueClass = null;
    if (SingleObjectResource.class.isAssignableFrom(resourceClass))
    {
      @SuppressWarnings("unchecked")
      Class<? extends SingleObjectResource<?>> clazz = (Class<? extends SingleObjectResource<?>>) resourceClass;
      List<Class<?>> kvParams = ReflectionUtils.getTypeArguments(SingleObjectResource.class, clazz);
      valueClass = kvParams.get(0).asSubclass(RecordTemplate.class);
    }

    ResourceType resourceType = getResourceType(resourceClass);

    RestLiAnnotationData annotationData = getRestLiAnnotationData(resourceClass);

    String name = annotationData.name();
    String namespace = annotationData.namespace();

    Class<?> parentResourceClass =
        annotationData.parent().equals(RestAnnotations.ROOT.class) ? null
            : annotationData.parent();

    ResourceModel resourceModel =
        new ResourceModel(valueClass,
                          resourceClass,
                          parentResourceClass,
                          name,
                          resourceType,
                          namespace);

    resourceModel.setParentResourceModel(parentResource);

    addResourceMethods(resourceClass, resourceModel);

    log.debug("Processed simple resource '" + resourceClass.getName() + "'");

    return resourceModel;
  }

  private static ResourceType getResourceType(final Class<?> resourceClass)
  {
    RestLiCollection collAnno = resourceClass.getAnnotation(RestLiCollection.class);
    RestLiAssociation assocAnno = resourceClass.getAnnotation(RestLiAssociation.class);
    RestLiSimpleResource simpleResourceAnno = resourceClass.getAnnotation(RestLiSimpleResource.class);

    if (resourceClass.isAnnotationPresent(RestLiActions.class))
    {
      throw new ResourceConfigException("Resource class '" + resourceClass.getName()
                                            + "' cannot have both @RestLiCollection and @RestLiActions annotations.");
    }

    int annoCount = 0;
    annoCount += collAnno != null ? 1 : 0;
    annoCount += assocAnno != null ? 1 : 0;
    annoCount += simpleResourceAnno != null ? 1 : 0;

    if (annoCount > 1)
    {
      throw new ResourceConfigException("Class '" + resourceClass.getName()
          + "' is annotated " + "with too many RestLi annotations");
    }
    else if (collAnno != null)
    {
      return ResourceType.COLLECTION;
    }
    else if (assocAnno != null)
    {
      return ResourceType.ASSOCIATION;
    }
    else if (simpleResourceAnno != null)
    {
      return ResourceType.SIMPLE;
    }
    else
    {
      throw new ResourceConfigException("Class '" + resourceClass.getName()
          + "' should be annotated " + "with '" + RestLiAssociation.class.getName() + "'"
          + " or '" + RestLiCollection.class.getName() + "'" + " or '"
          + RestLiSimpleResource.class.getName() + "'");
    }
  }

  /**
   * Keys, values, patch requests, and batch requests have fixed positions.
   */
  private static Parameter<?> getPositionalParameter(final ResourceModel model,
                                                  final ResourceMethod methodType,
                                                  final int idx,
                                                  final AnnotationSet annotations)
  {
    boolean isSingleObjectResource = model.getResourceType() == ResourceType.SIMPLE;

    Parameter<?> parameter = null;

    if (isSingleObjectResource)
    {
      parameter = getPositionalParameterForSingleObject(model, methodType, idx, annotations);
    }
    else
    {
      parameter = getPositionalParameterForCollection(model, methodType, idx, annotations);
    }

    return parameter;
  }

  private static Parameter<?> getPositionalParameterForCollection(final ResourceModel model,
                                                               final ResourceMethod methodType,
                                                               final int idx,
                                                               final AnnotationSet annotations)
  {
    switch (methodType)
    {
      case GET:
        if (idx == 0)
        {
          return makeKeyParam(model);
        }
        break;
      case CREATE:
        if (idx == 0)
        {
          if (model.getResourceEntityType() == ResourceEntityType.UNSTRUCTURED_DATA)
          {
            return null;
          }
          return makeValueParam(model);
        }
        break;
      case UPDATE:
        if (idx == 0)
        {
          return makeKeyParam(model);
        }
        else if (idx == 1)
        {
          if (model.getResourceEntityType() == ResourceEntityType.UNSTRUCTURED_DATA)
          {
            return null;
          }
          return makeValueParam(model);
        }
        break;
      case DELETE:
        if (idx == 0)
        {
          return makeKeyParam(model);
        }
        break;
      case PARTIAL_UPDATE:
        if (idx == 0)
        {
          return makeKeyParam(model);
        }
        else if (idx == 1)
        {
          return makePatchParam(annotations);
        }
        break;
      case BATCH_GET:
        if (idx == 0)
        {
          @SuppressWarnings({"unchecked", "rawtypes"})
          Parameter p =
              new Parameter("",
                            Set.class,
                            null,
                            false,
                            null,
                            Parameter.ParamType.BATCH,
                            false,
                            annotations);
          return p;
        }
        break;
      case BATCH_CREATE:
        if (idx == 0)
        {
          @SuppressWarnings({"unchecked", "rawtypes"})
          Parameter p =
              new Parameter("",
                            BatchCreateRequest.class,
                            null,
                            false,
                            null,
                            Parameter.ParamType.BATCH,
                            false,
                            annotations);
          return p;
        }
        break;
      case BATCH_UPDATE:
        if (idx == 0)
        {
          @SuppressWarnings({"unchecked", "rawtypes"})
          Parameter p =
              new Parameter("",
                            BatchUpdateRequest.class,
                            null,
                            false,
                            null,
                            Parameter.ParamType.BATCH,
                            false,
                            annotations);
          return p;
        }
        break;
      case BATCH_DELETE:
        if (idx == 0)
        {
          @SuppressWarnings({"unchecked", "rawtypes"})
          Parameter p =
              new Parameter("",
                            BatchDeleteRequest.class,
                            null,
                            false,
                            null,
                            Parameter.ParamType.BATCH,
                            false,
                            annotations);
          return p;
        }
        break;
      case BATCH_PARTIAL_UPDATE:
        if (idx == 0)
        {
          @SuppressWarnings({"unchecked", "rawtypes"})
          Parameter p =
              new Parameter("",
                            BatchPatchRequest.class,
                            null,
                            false,
                            null,
                            Parameter.ParamType.BATCH,
                            false,
                            annotations);
          return p;
        }
        break;
      default:
        break;
    }

    return null;
  }

  private static Parameter<?> getPositionalParameterForSingleObject(final ResourceModel model,
                                                               final ResourceMethod methodType,
                                                               final int idx,
                                                               final AnnotationSet annotations)
  {
    Parameter<?> parameter = null;

    switch(methodType)
    {
      case UPDATE:
        if (idx == 0)
        {
          if (model.getResourceEntityType() == ResourceEntityType.UNSTRUCTURED_DATA)
          {
            return null;
          }
          return makeValueParam(model);
        }

        break;
      case PARTIAL_UPDATE:
        if (idx == 0)
        {
          return makePatchParam(annotations);
        }

        break;
    }

    return parameter;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Parameter<?> makeValueParam(final ResourceModel model)
  {
    return new Parameter("",
                         model.getValueClass(),
                         getDataSchema(model.getValueClass(), null),
                         false,
                         null,
                         Parameter.ParamType.POST,
                         false,
                         AnnotationSet.EMPTY);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Parameter makeKeyParam(final ResourceModel model)
  {
    return new Parameter(model.getKeyName(),
                         model.getKeyClass(),
                         model.getPrimaryKey().getDataSchema(),
                         false,
                         null,
                         Parameter.ParamType.RESOURCE_KEY,
                         false,
                         AnnotationSet.EMPTY);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static Parameter makePatchParam(AnnotationSet annotations)
  {
    return new Parameter("",
                         PatchRequest.class,
                         null,
                         false,
                         null,
                         Parameter.ParamType.POST,
                         false,
                         annotations);
  }

  private static String getDefaultValueData(final Optional optional)
  {
    if (optional == null || optional.value() == null || optional.value().equals(RestAnnotations.DEFAULT))
    {
      return null;
    }
    return optional.value();
  }

  @SuppressWarnings("deprecation")
  private static List<Parameter<?>> getParameters(final ResourceModel model,
                                                  final Method method,
                                                  final ResourceMethod methodType)
  {
    Set<String> paramNames = new HashSet<String>();

    List<Parameter<?>> queryParameters = new ArrayList<Parameter<?>>();
    Annotation[][] paramsAnnos = method.getParameterAnnotations();

    // Iterate over the method parameters.
    for (int idx = 0; idx < paramsAnnos.length; idx++)
    {
      AnnotationSet paramAnnotations = new AnnotationSet(paramsAnnos[idx]);
      Class<?> paramType = method.getParameterTypes()[idx];

      Parameter<?> param = getPositionalParameter(model, methodType, idx, paramAnnotations);

      // if no positional definition, look for custom annotated parameters
      if (param == null)
      {
        if (paramAnnotations.contains(QueryParam.class))
        {
          param = buildQueryParam(method, paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains(ActionParam.class))
        {
          param = buildActionParam(method, paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains( com.linkedin.restli.server.annotations.AssocKey.class))
        {
          param = buildAssocKeyParam(model, method, paramAnnotations, paramType,  com.linkedin.restli.server.annotations.AssocKey.class);
        }
        else if (paramAnnotations.contains(AssocKeyParam.class))
        {
          param = buildAssocKeyParam(model, method, paramAnnotations, paramType, AssocKeyParam.class);
        }
        else if (paramAnnotations.contains( com.linkedin.restli.server.annotations.Context.class))
        {
          param = buildPagingContextParam(paramAnnotations, paramType,  com.linkedin.restli.server.annotations.Context.class);
        }
        else if (paramAnnotations.contains(PagingContextParam.class))
        {
          param = buildPagingContextParam(paramAnnotations, paramType, PagingContextParam.class);
        }
        else if (paramAnnotations.contains(CallbackParam.class))
        {
          param = buildCallbackParam(method, methodType, idx, paramType, paramAnnotations);
        }
        else if (paramAnnotations.contains(ParSeqContextParam.class))
        {
          param = buildParSeqContextParam(method, methodType, idx, paramType, paramAnnotations, ParSeqContextParam.class);
        }
        else if (paramAnnotations.contains(com.linkedin.restli.server.annotations.ParSeqContext.class))
        {
          param = buildParSeqContextParam(method, methodType, idx, paramType, paramAnnotations, com.linkedin.restli.server.annotations.ParSeqContext.class);
        }
        else if (paramAnnotations.contains( com.linkedin.restli.server.annotations.Projection.class))
        {
          param = buildProjectionParam(paramAnnotations, paramType, Parameter.ParamType.PROJECTION);
        }
        else if (paramAnnotations.contains(ProjectionParam.class))
        {
          param = buildProjectionParam(paramAnnotations, paramType, Parameter.ParamType.PROJECTION_PARAM);
        }
        else if (paramAnnotations.contains(MetadataProjectionParam.class))
        {
          param = buildProjectionParam(paramAnnotations, paramType, Parameter.ParamType.METADATA_PROJECTION_PARAM);
        }
        else if (paramAnnotations.contains(PagingProjectionParam.class))
        {
          param = buildProjectionParam(paramAnnotations, paramType, Parameter.ParamType.PAGING_PROJECTION_PARAM);
        }
        else if (paramAnnotations.contains( com.linkedin.restli.server.annotations.Keys.class))
        {
          param = buildPathKeysParam(paramAnnotations, paramType,  com.linkedin.restli.server.annotations.Keys.class);
        }
        else if (paramAnnotations.contains(PathKeysParam.class))
        {
          param = buildPathKeysParam(paramAnnotations, paramType, PathKeysParam.class);
        }
        else if (paramAnnotations.contains(PathKeyParam.class))
        {
          param = buildPathKeyParam(model, paramAnnotations, paramType, PathKeyParam.class);
        }
        else if (paramAnnotations.contains(HeaderParam.class))
        {
          param = buildHeaderParam(paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains(ResourceContextParam.class))
        {
          param = buildResourceContextParam(paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains(ValidatorParam.class))
        {
          param = buildValidatorParam(paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains(RestLiAttachmentsParam.class))
        {
          param = buildRestLiAttachmentsParam(paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains(UnstructuredDataWriterParam.class))
        {
          param = buildUnstructuredDataWriterParam(paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains(UnstructuredDataReactiveReaderParam.class))
        {
          param = buildUnstructuredDataReactiveReader(paramAnnotations, paramType);
        }
        else
        {
          throw new ResourceConfigException(buildMethodMessage(method)
              + " must annotate each parameter with @QueryParam, @ActionParam, @AssocKeyParam, @PagingContextParam, " +
              "@ProjectionParam, @MetadataProjectionParam, @PagingProjectionParam, @PathKeysParam, @PathKeyParam, " +
              "@HeaderParam, @CallbackParam, @ResourceContext, @ParSeqContextParam, @ValidatorParam, " +
              "@RestLiAttachmentsParam, @UnstructuredDataWriterParam, @UnstructuredDataReactiveReaderParam, " +
              "or @ValidateParam");
        }
      }

      if (param != null)
      {
        validateParameter(method,
                          methodType,
                          paramNames,
                          paramAnnotations,
                          param,
                          paramType);

        queryParameters.add(param);
      }
    }

    return queryParameters;
  }

  private static Parameter<ResourceContext> buildResourceContextParam(AnnotationSet annotations, final Class<?> paramType)
  {
      if (!paramType.equals(ResourceContext.class))
      {
        throw new ResourceConfigException("Incorrect data type for param: @" + ResourceContextParam.class.getSimpleName() + " parameter annotation must be of type " +  ResourceContext.class.getName());
      }
      Optional optional = annotations.get(Optional.class);
      return new Parameter<>("",
                            ResourceContext.class,
                           null,
                           optional != null,
                           null,
                           Parameter.ParamType.RESOURCE_CONTEXT_PARAM,
                           false,
                           annotations);
    }

  private static Parameter<RestLiDataValidator> buildValidatorParam(AnnotationSet annotations, final Class<?> paramType)
  {
    if (!paramType.equals(RestLiDataValidator.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + ValidatorParam.class.getSimpleName() + " parameter annotation must be of type " +  RestLiDataValidator.class.getName());
    }
    return new Parameter<>("validator",
                          RestLiDataValidator.class,
                         null,
                         false,
                         null,
                         Parameter.ParamType.VALIDATOR_PARAM,
                         false,
                         annotations);
  }

  private static Parameter<RestLiAttachmentReader> buildRestLiAttachmentsParam(AnnotationSet annotations, final Class<?> paramType)
  {
    if (!paramType.equals(RestLiAttachmentReader.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + RestLiAttachmentsParam.class.getSimpleName() + " parameter annotation must be of type " +  RestLiAttachmentReader.class.getName());
    }

    return new Parameter<>("RestLi Attachment Reader",
                          RestLiAttachmentReader.class,
                         null,
                         false, //RestLiAttachments cannot be optional. If its in the request we provide it, otherwise it's null.
                         null,
                         Parameter.ParamType.RESTLI_ATTACHMENTS_PARAM,
                         false, //Not going to be persisted into the IDL at this time.
                         annotations);
  }

  private static Parameter<UnstructuredDataWriter> buildUnstructuredDataWriterParam(AnnotationSet annotations, final Class<?> paramType)
  {
    if (!paramType.equals(UnstructuredDataWriter.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + UnstructuredDataWriterParam.class.getSimpleName() + " parameter annotation must be of type " +  UnstructuredDataWriter.class.getName());
    }

    return new Parameter<>("RestLi Unstructured Data Writer",
                          UnstructuredDataWriter.class,
                         null,
                         false,
                         null,
                         Parameter.ParamType.UNSTRUCTURED_DATA_WRITER_PARAM,
                         false, //Not going to be persisted into the IDL at this time.
                         annotations);
  }

  private static Parameter<UnstructuredDataReactiveReader> buildUnstructuredDataReactiveReader(AnnotationSet annotations, final Class<?> paramType)
  {
    if (!paramType.equals(UnstructuredDataReactiveReader.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + UnstructuredDataReactiveReaderParam.class.getSimpleName() + " parameter annotation must be of type " +  UnstructuredDataReactiveReader.class.getName());
    }

    return new Parameter<>("RestLi Unstructured Data Reactive Reader",
                          UnstructuredDataReactiveReader.class,
                          null,
                          false,
                          null,
                          Parameter.ParamType.UNSTRUCTURED_DATA_REACTIVE_READER_PARAM,
                          false, //Not going to be persisted into the IDL at this time.
                          annotations);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Parameter buildCallbackParam(final Method method,
                                              final ResourceMethod methodType,
                                              final int idx,
                                              final Class<?> paramType,
                                              final AnnotationSet annotations)
  {
    if (!Callback.class.equals(paramType))
    {
      throw new ResourceConfigException(String.format("%s '%s' of class '%s' does not have a proper callback",
                                                      methodType,
                                                      method.getName(),
                                                      method.getDeclaringClass()
                                                            .getName()));
    }
    Parameter<?> param =
        new Parameter<>("",
                      paramType,
                      null,
                      false,
                      null,
                      Parameter.ParamType.CALLBACK,
                      false,
                      annotations);
    return param;
  }

  @SuppressWarnings("deprecation")
  private static Parameter<Context> buildParSeqContextParam(final Method method,
                                                                                final ResourceMethod methodType,
                                                                                final int idx,
                                                                                final Class<?> paramType,
                                                                                final AnnotationSet annotations,
                                                                                final Class<?> paramAnnotationType)
  {
    if (!Context.class.equals(paramType))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + ParSeqContextParam.class.getSimpleName() + " or @" + com.linkedin.restli.server.annotations.ParSeqContext.class.getSimpleName() +
              " parameter annotation must be of type " +  Context.class.getName());
    }
    if (getInterfaceType(method) != InterfaceType.PROMISE)
    {
      throw new ResourceConfigException("Cannot have ParSeq context on non-promise method");
    }
    Parameter.ParamType parameter = null;
    if(paramAnnotationType.equals(com.linkedin.restli.server.annotations.ParSeqContext.class))
    {
      parameter = Parameter.ParamType.PARSEQ_CONTEXT;
    }
    else if (paramAnnotationType.equals(ParSeqContextParam.class))
    {
      parameter = Parameter.ParamType.PARSEQ_CONTEXT_PARAM;
    }
    else
    {
      throw new ResourceConfigException("Param Annotation type must be 'ParseqContextParam' or the deprecated 'ParseqContext' for ParseqContext");
    }
    return new Parameter<Context>("",
                                                      Context.class,
                                                      null,
                                                      false,
                                                      null,
                                                      parameter,
                                                      false,
                                                      annotations);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void validateParameter(final Method method,
                                        final ResourceMethod methodType,
                                        final Set<String> paramNames,
                                        final AnnotationSet annotations,
                                        final Parameter<?> param,
                                        final Class<?> actualParamType)
  {
    String paramName = param.getName();
    if (!paramName.isEmpty() && paramNames.contains(paramName))
    {
      throw new ResourceConfigException("Parameter '" + paramName + "' on "
          + buildMethodMessage(method) + " is specified more than once");
    }
    paramNames.add(paramName);

    if (!actualParamType.isAssignableFrom(param.getType()))
    {
      throw new ResourceConfigException("Parameter '" + paramName + "' on "
          + buildMethodMessage(method) + " is not a valid type '" + actualParamType
          + "'.  Must be assignable from '" + param.getType() + "'.");
    }

    if (!POST_OR_PUT_RESOURCE_METHODS.contains(methodType))
    {
      //If this is not a post or put resource method, i.e a FINDER, then we can't have @RestLiAttachmentParams
      if (annotations.contains(RestLiAttachmentsParam.class))
      {
        throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                              + buildMethodMessage(method) + " is only allowed within the following "
                                              + "resource methods: " + POST_OR_PUT_RESOURCE_METHODS.toString());
      }
    }

    //Only GET can have @UnstructuredDataWriterParam
    if (methodType != ResourceMethod.GET && annotations.contains(UnstructuredDataWriterParam.class))
    {
      throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                            + buildMethodMessage(method) + " is only allowed within the following "
                                            + "resource methods: " + ResourceMethod.GET);
    }

    //Only CREATE can have @UnstructuredDataReactiveReaderParam
    if (methodType != ResourceMethod.CREATE && annotations.contains(UnstructuredDataReactiveReaderParam.class)
        && annotations.contains(CallbackParam.class))
    {
      throw new ResourceConfigException("Parameter '" + paramName + "' on "
          + buildMethodMessage(method) + " is only allowed within the following "
          + "resource methods: " + ResourceMethod.CREATE);
    }

    if (methodType == ResourceMethod.ACTION)
    {
      if (annotations.contains(QueryParam.class))
      {
        throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                              + buildMethodMessage(method) + " is a @QueryParam but action method cannot have @QueryParam");
      }

      if ((param.getParamType() == Parameter.ParamType.POST)
          && !(checkParameterType(param.getType(), RestModelConstants.VALID_ACTION_PARAMETER_TYPES) ||
          checkParameterHasTyperefSchema(param)))
      {
        throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                              + buildMethodMessage(method) + " is not a valid type (" + param.getType() + ')');
      }
    }
    else
    {
      if (annotations.contains(ActionParam.class))
      {
        throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                              + buildMethodMessage(method) + " is a @ActionParam but non-action method cannot have @ActionParam");
      }

      if ((param.getParamType() == Parameter.ParamType.QUERY)
          && !(checkParameterType(param.getType(), RestModelConstants.VALID_QUERY_PARAMETER_TYPES) ||
          checkParameterHasTyperefSchema(param)))
      {
        throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                              + buildMethodMessage(method) + " is not a valid type (" + param.getType() + ')');
      }
    }

    if (param.getType().isPrimitive() && param.isOptional() && !param.hasDefaultValue())
    {
      throw new ResourceConfigException("Parameter '"
          + paramName
          + "' on "
          + buildMethodMessage(method)
          + " is a primitive type, but does not specify a default value in the @Optional annotation");
    }

    final String checkTyperefMessage = checkTyperefSchema(param.getType(), param.getDataSchema());
    if (checkTyperefMessage != null)
    {
      throw new ResourceConfigException("Parameter '" + paramName + "' on "
          + buildMethodMessage(method) + ", " + checkTyperefMessage);
    }

    if (annotations.count(QueryParam.class,
                          ActionParam.class,
                          AssocKeyParam.class,
                          PagingContextParam.class,
                          CallbackParam.class,
                          ParSeqContextParam.class,
                          UnstructuredDataWriterParam.class,
                          UnstructuredDataReactiveReaderParam.class,
                          RestLiAttachmentsParam.class) > 1)
    {
      throw new ResourceConfigException(buildMethodMessage(method)
          + "' must declare only one of @QueryParam, "
          + "@ActionParam, "
          + "@AssocKeyParam, "
          + "@PagingContextParam, "
          + "@CallbackParam, "
          + "@ParSeqContextParam, "
          + "@RestLiAttachmentsParam, "
          + "@UnstructuredDataWriterParam"
          + "@UnstructuredDataReactiveReaderParam");
    }
  }

  private static boolean checkParameterHasTyperefSchema(Parameter<?> parameter)
  {
    boolean result = false;
    DataSchema dataSchema = parameter.getDataSchema();
    Class<?> dataType = parameter.getType();

    if (dataType.isArray())
    {
      if (dataSchema instanceof ArrayDataSchema)
      {
        dataSchema = ((ArrayDataSchema) dataSchema).getItems();
      }
      else
      {
        throw new ResourceConfigException(
            "Array typed parameter " + parameter.getName() + " must have an array schema.");
      }
    }

    if (dataSchema instanceof TyperefDataSchema)
    {
      result = true;
    }

    return result;
  }

  private static Set<Key> buildKeys(String resourceName,
                                    com.linkedin.restli.server.annotations.Key[] annoKeys)
  {
    Set<Key> keys = new HashSet<Key>();
    for(com.linkedin.restli.server.annotations.Key key : annoKeys)
    {
      keys.add(buildKey(resourceName, key.name(), key.type(), key.typeref()));
    }
    return keys;
  }

  private static Key buildKey(String resourceName,
                              String keyName, Class<?> keyType, Class<? extends TyperefInfo> typerefInfoClass)
  {
    try
    {
      // If the key is a non-primitive custom type, initialize the keyType class so that the corresponding coercer is
      // loaded into memory such that the coercer would have been registered when we process an incoming request for
      // this resource.
      if (typerefInfoClass != RestAnnotations.NULL_TYPEREF_INFO.class && DataSchemaUtil.classToPrimitiveDataSchema(
          keyType) == null)
      {
        Custom.initializeCustomClass(keyType);
      }
      return new Key(keyName, keyType, getDataSchema(keyType, getSchemaFromTyperefInfo(typerefInfoClass)));
    }

    catch (TemplateRuntimeException e)
    {
      throw new ResourceConfigException("DataSchema for key '" + keyName + "' of type " + keyType + " on resource "
                                                + resourceName + "cannot be found; type is invalid or requires typeref", e);
    }
    catch (Exception e)
    {
      throw new ResourceConfigException("Typeref for parameter '" + keyName + "' on resource "
                                                + resourceName + " cannot be instantiated, " + e.getMessage(), e);
    }

  }

  @SuppressWarnings("deprecation")
  private static Parameter<?> buildProjectionParam(final AnnotationSet annotations, final Class<?> paramType,
      final Parameter.ParamType projectionType)
  {
    if (!paramType.equals(MaskTree.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + ProjectionParam.class.getSimpleName() +
          ", @" +  com.linkedin.restli.server.annotations.Projection.class.getSimpleName() +
          ", @" + MetadataProjectionParam.class.getSimpleName() +
          " or @" + PagingProjectionParam.class.getSimpleName() +
          " parameter annotation must be of type " + MaskTree.class.getName());
    }
    Optional optional = annotations.get(Optional.class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    Parameter<?> param = new Parameter("",
                                       paramType,
                                       null,
                                       optional != null,
                                       null, // default mask is null.
                                       projectionType,
                                       false,
                                       annotations);
    return param;
  }

  private static void checkIfKeyIsValid(String paramName, final Class<?> paramType, ResourceModel model)
  {
    ResourceModel nextModel = model.getParentResourceModel();

    while (nextModel != null)
    {
      Set<Key> keys = nextModel.getKeys();

      for (Key key : keys)
      {
        if (key.getName().equals(paramName))
        {
          return;
        }
      }

      nextModel = nextModel.getParentResourceModel();
    }

    throw new ResourceConfigException("Parameter " + paramName + " not found in path keys of class " + model.getResourceClass());
  }

  private static Parameter<?> buildPathKeyParam(final ResourceModel model,
                                                AnnotationSet annotations,
                                                final Class<?> paramType,
                                                final Class<?> paramAnnotationType)
  {
    String paramName = annotations.get(PathKeyParam.class).value();

    checkIfKeyIsValid(paramName, paramType, model);

    Parameter<?> param = new Parameter<>(paramName,
                                        paramType,
                                        null,
                                        annotations.get(Optional.class) != null,
                                        null, // default mask is null.
                                        Parameter.ParamType.PATH_KEY_PARAM,
                                        false,
                                        annotations);

    return param;
  }

  @SuppressWarnings("deprecation")
  private static Parameter<?> buildPathKeysParam(final AnnotationSet annotations,
                                                 final Class<?> paramType,
                                                 final Class<?> paramAnnotationType)
  {
    if (!paramType.equals(PathKeys.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + PathKeysParam.class.getSimpleName() + " or @" +  com.linkedin.restli.server.annotations.Keys.class.getSimpleName() +
              " parameter annotation must be of type " +  PathKeys.class.getName());
    }

    Optional optional = annotations.get(Optional.class);
    Parameter.ParamType parameter = null;
    if(paramAnnotationType.equals( com.linkedin.restli.server.annotations.Keys.class))
    {
      parameter = Parameter.ParamType.PATH_KEYS;
    }
    else if (paramAnnotationType.equals(PathKeysParam.class))
    {
      parameter = Parameter.ParamType.PATH_KEYS_PARAM;
    }
    else
    {
      throw new ResourceConfigException("Param Annotation type must be 'PathKeysParam' or the deprecated 'Keys' for PathKeys");
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    Parameter<?> param = new Parameter("",
                                       paramType,
                                       null,
                                       optional != null,
                                       new PathKeysImpl(),
                                       parameter,
                                       false,
                                       annotations);
    return param;
  }

  private static Parameter<?> buildHeaderParam(final AnnotationSet annotations,
                                               final Class<?> paramType)
  {
    if (!paramType.equals(String.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + HeaderParam.class.getSimpleName() + " parameter annotation must be of type String");
    }
    Optional optional = annotations.get(Optional.class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    Parameter<?> param = new Parameter("",
                                       paramType,
                                       null,
                                       optional != null,
                                       "",
                                       Parameter.ParamType.HEADER,
                                       false,
                                       annotations);
    return param;
  }

  @SuppressWarnings("deprecation")
  private static Parameter<?> buildPagingContextParam(final AnnotationSet annotations,
                                                      final Class<?> paramType,
                                                      final Class<?> paramAnnotationType)
  {
    if (!paramType.equals(PagingContext.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + PagingContextParam.class.getSimpleName() + " or @" +  com.linkedin.restli.server.annotations.Context.class.getSimpleName() +
              " parameter annotation must be of type " +  PagingContext.class.getName());
    }

    PagingContext defaultContext = null;
    Parameter.ParamType parameter = null;
    if(paramAnnotationType.equals(PagingContextParam.class))
    {
      PagingContextParam pagingContextParam = annotations.get(PagingContextParam.class);
      defaultContext = new PagingContext(pagingContextParam.defaultStart(), pagingContextParam.defaultCount(), false, false);
      parameter = Parameter.ParamType.PAGING_CONTEXT_PARAM;
    }
    else if (paramAnnotationType.equals( com.linkedin.restli.server.annotations.Context.class))
    {
       com.linkedin.restli.server.annotations.Context contextParam = annotations.get( com.linkedin.restli.server.annotations.Context.class);
      defaultContext = new PagingContext(contextParam.defaultStart(), contextParam.defaultCount(), false, false);
      parameter = Parameter.ParamType.CONTEXT;
    }
    else
    {
      throw new ResourceConfigException("Param Annotation type must be 'PagingContextParam' or the deprecated 'Context' for PagingContext");
    }
    Optional optional = annotations.get(Optional.class);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Parameter<?> param =
        new Parameter("",
                      paramType,
                      null,
                      optional != null,
                      defaultContext,
                      parameter,
                      false,
                      annotations);
    return param;
  }

  private static boolean checkAssocKey(final Set<Key> keys,
                                       final String assocKeyValue)
  {
    for (Key k : keys)
    {
      if (k.getName().equals(assocKeyValue))
      {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("deprecation")
  private static Parameter<?> buildAssocKeyParam(final ResourceModel model,
                                                 final Method method,
                                                 final AnnotationSet annotations,
                                                 final Class<?> paramType,
                                                 final Class<?> paramAnnotationType)
  {
    Parameter.ParamType parameter = null;
    String assocKeyParamValue = null;
    Class<? extends TyperefInfo> typerefInfoClass = null;
    if (paramAnnotationType.equals( com.linkedin.restli.server.annotations.AssocKey.class))
    {
      parameter = Parameter.ParamType.KEY;
      assocKeyParamValue = annotations.get( com.linkedin.restli.server.annotations.AssocKey.class).value();
      typerefInfoClass = annotations.get( com.linkedin.restli.server.annotations.AssocKey.class).typeref();
    }
    else if (paramAnnotationType.equals(AssocKeyParam.class))
    {
      parameter = Parameter.ParamType.ASSOC_KEY_PARAM;
      assocKeyParamValue = annotations.get(AssocKeyParam.class).value();
      typerefInfoClass = annotations.get(AssocKeyParam.class).typeref();
    }
    else
    {
      throw new ResourceConfigException("Param Annotation type must be 'AssocKeysParam' or the deprecated 'AssocKey' for AssocKey");
    }
    Optional optional = annotations.get(Optional.class);

    if (!checkAssocKey(model.getKeys(), assocKeyParamValue))
    {
      throw new ResourceConfigException("Non-existing assocKey '" + assocKeyParamValue + "' on " + buildMethodMessage(method));
    }

    try
    {
      @SuppressWarnings({"unchecked", "rawtypes"})
      Parameter<?> param = new Parameter(assocKeyParamValue,
                                        paramType,
                                        getDataSchema(paramType, getSchemaFromTyperefInfo(typerefInfoClass)),
                                        optional != null,
                                        getDefaultValueData(optional),
                                        parameter,
                                        true,
                                        annotations);
      return param;
    }
    catch (TemplateRuntimeException e)
    {
      throw new ResourceConfigException("DataSchema for assocKey '" + assocKeyParamValue + "' of type " + paramType.getSimpleName() + " on "
                                                + buildMethodMessage(method) + "cannot be found; type is invalid or requires typeref", e);
    }
    catch (Exception e)
    {
      throw new ResourceConfigException("Typeref for assocKey '" + assocKeyParamValue + "' on "
                                                + buildMethodMessage(method) + " cannot be instantiated, " + e.getMessage(), e);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Parameter buildActionParam(final Method method,
                                            final AnnotationSet annotations,
                                            final Class<?> paramType)
  {
    ActionParam actionParam = annotations.get(ActionParam.class);
    Optional optional = annotations.get(Optional.class);
    String paramName = actionParam.value();
    Class<? extends TyperefInfo> typerefInfoClass = actionParam.typeref();
    try
    {
      Parameter param = new Parameter(paramName,
                                      paramType,
                                      getDataSchema(paramType, getSchemaFromTyperefInfo(typerefInfoClass)),
                                      optional != null,
                                      getDefaultValueData(optional),
                                      Parameter.ParamType.POST,
                                      true,
                                      annotations);
      return param;
    }
    catch (TemplateRuntimeException e)
    {
      throw new ResourceConfigException("DataSchema for parameter '" + paramName + "' of type " + paramType.getSimpleName() + " on "
          + buildMethodMessage(method) + "cannot be found; type is invalid or requires typeref", e);
    }
    catch (Exception e)
    {
      throw new ResourceConfigException("Typeref for parameter '" + paramName + "' on "
          + buildMethodMessage(method) + " cannot be instantiated, " + e.getMessage(), e);
    }
  }

  private static TyperefDataSchema getSchemaFromTyperefInfo(Class<? extends TyperefInfo> typerefInfoClass)
          throws IllegalAccessException, InstantiationException
  {
    if (typerefInfoClass == null)
    {
      return null;
    }

    TyperefInfo typerefInfo = typerefInfoClass.newInstance();
    return typerefInfo.getSchema();

  }

  private static DataSchema getDataSchema(Class<?> type, TyperefDataSchema typerefDataSchema)
  {
    // Unstructured data does not have data schema and corresponding class type
    if (type == null)
    {
      return null;
    }
    if (type == Void.TYPE)
    {
      return null;
    }
    if (typerefDataSchema != null)
    {
      if (type.isArray())
      {
        return new ArrayDataSchema(typerefDataSchema);
      }
      else
      {
        return typerefDataSchema;
      }
    }
    else if (RestModelConstants.CLASSES_WITHOUT_SCHEMAS.contains(type))
    {
      return null;
    }
    else if (type.isArray())
    {
      DataSchema itemSchema;
      if (HasTyperefInfo.class.isAssignableFrom(type.getComponentType()))
      {
        itemSchema = DataTemplateUtil.getTyperefInfo(type.getComponentType().asSubclass(DataTemplate.class)).getSchema();
      }
      else
      {
        itemSchema = DataTemplateUtil.getSchema(type.getComponentType());
      }
      return new ArrayDataSchema(itemSchema);
    }
    return DataTemplateUtil.getSchema(type);
  }


  private static Parameter<?> buildQueryParam(final Method method,
                                              final AnnotationSet annotations,
                                              final Class<?> paramType)
  {
    QueryParam queryParam = annotations.get(QueryParam.class);
    Optional optional = annotations.get(Optional.class);
    String paramName = queryParam.value();
    if (INVALID_CHAR_PATTERN.matcher(paramName).find())
    {
      throw new ResourceConfigException("Unsupported character in the parameter name :"
          + paramName);
    }
    Class<? extends TyperefInfo> typerefInfoClass = queryParam.typeref();
    try
    {
      @SuppressWarnings({"unchecked", "rawtypes"})
      Parameter<?> param = new Parameter(paramName,
                                        paramType,
                                        getDataSchema(paramType, getSchemaFromTyperefInfo(typerefInfoClass)),
                                        optional != null,
                                        getDefaultValueData(optional),
                                        Parameter.ParamType.QUERY,
                                        true,
                                        annotations);
      return param;
    }
    catch (TemplateRuntimeException e)
    {
      throw new ResourceConfigException("DataSchema for parameter '" + paramName + "' of type " + paramType.getSimpleName() + " on "
            + buildMethodMessage(method) + "cannot be found; type is invalid or requires typeref", e);
    }
    catch (Exception e)
    {
      throw new ResourceConfigException("Typeref for parameter '" + paramName + "' on "
          + buildMethodMessage(method) + " cannot be instantiated, " + e.getMessage(), e);
    }
  }

  private static String buildMethodMessage(final Method method)
  {
    return "Method '" + method.getName() + "' method on class '"
        + method.getDeclaringClass().getName() + '\'';
  }

  private static void registerCoercerForPrimitiveTypeRefArray(ArrayDataSchema schema)
  {
    DataSchema elementSchema = schema.getItems();
    if (elementSchema instanceof TyperefDataSchema)
    {
      TyperefDataSchema typerefSchema = (TyperefDataSchema) elementSchema;
      if (RestModelConstants.PRIMITIVE_DATA_SCHEMA_TYPE_ALLOWED_TYPES.containsKey(typerefSchema.getDereferencedType()))
      {
        if (CustomTypeUtil.getJavaCustomTypeClassNameFromSchema(typerefSchema) != null)
        {
          registerCoercer(typerefSchema);
        }
      }
    }
  }

  private static String checkTyperefSchema(final Class<?> type, final DataSchema dataSchema)
  {
    if (type.isArray() && dataSchema instanceof ArrayDataSchema)
    {
      registerCoercerForPrimitiveTypeRefArray((ArrayDataSchema) dataSchema);
    }

    if (!(dataSchema instanceof TyperefDataSchema))
    {
      return null;
    }

    TyperefDataSchema typerefSchema = (TyperefDataSchema)dataSchema;
    boolean ok;
    DataSchema.Type schemaType = typerefSchema.getDereferencedType();
    Class<?>[] validTypes =
        RestModelConstants.PRIMITIVE_DATA_SCHEMA_TYPE_ALLOWED_TYPES.get(schemaType);
    if (validTypes != null)
    {
      String javaClassNameFromSchema =
          CustomTypeUtil.getJavaCustomTypeClassNameFromSchema(typerefSchema);

      if (javaClassNameFromSchema != null)
      {
        registerCoercer(typerefSchema);
        ok =
            type.getName().equals(javaClassNameFromSchema)
                || (type.isArray() && (type.getComponentType().getName()).equals(javaClassNameFromSchema));
      }
      else
      {
        ok = checkParameterType(type, validTypes);
      }
    }
    else
    {
      try
      {
        DataSchema inferredSchema = DataTemplateUtil.getSchema(type);
        DataSchema derefSchema = typerefSchema.getDereferencedDataSchema();
        if (inferredSchema.equals(derefSchema))
        {
          return null;
        }
        return "typeref " + typerefSchema + " is not compatible with (" + type
            + ") with schema " + derefSchema;
      }
      catch (TemplateRuntimeException e)
      {
      }
      ok = false;
    }
    if (!ok)
    {
      return "typeref " + typerefSchema + " is not compatible with (" + type + ")";
    }
    return null;
  }

  private static void registerCoercer(final TyperefDataSchema schema)
  {
    String coercerClassName = CustomTypeUtil.getJavaCoercerClassFromSchema(schema);
    String javaClassNameFromSchema = CustomTypeUtil.getJavaCustomTypeClassNameFromSchema(schema);

    // initialize the custom class
    try
    {
      Custom.initializeCustomClass(Class.forName(javaClassNameFromSchema, true, Thread.currentThread().getContextClassLoader()));
    }
    catch (ClassNotFoundException e)
    {
      throw new ResourceConfigException("Could not find class for type "
          + javaClassNameFromSchema, e);
    }
    if (coercerClassName != null)
    {
      try
      {
        Custom.initializeCoercerClass(Class.forName(coercerClassName, true, Thread.currentThread().getContextClassLoader()));
      }
      catch (ClassNotFoundException e)
      {
        throw new ResourceConfigException("Could not find coercer " + coercerClassName
            + " for type " + javaClassNameFromSchema, e);
      }
    }
  }

  private static boolean checkParameterType(final Class<?> type,
                                            final Class<?>[] validTypes)
  {
    for (Class<?> validType : validTypes)
    {
      if (validType.isAssignableFrom(type))
      {
        return true;
      }
    }

    return false;
  }

  private static void addResourceMethods(final Class<?> resourceClass,
                                                   final ResourceModel model)
  {
    // this ignores methods declared in superclasses (e.g. template methods)
    for (Method method : resourceClass.getDeclaredMethods())
    {
      // ignore synthetic, type-erased versions of methods
      if (method.isSynthetic())
      {
        continue;
      }

      addActionResourceMethod(model, method);
      addFinderResourceMethod(model, method);
      addBatchFinderResourceMethod(model, method);
      addTemplateResourceMethod(resourceClass, model, method);
      addCrudResourceMethod(resourceClass, model, method);
    }

    validateResourceModel(model);
  }

  private static void validateResourceModel(final ResourceModel model)
  {
    validateAssociation(model);
    validateCrudMethods(model);
    validateSimpleResource(model);
  }

  private static void validateSimpleResource(ResourceModel model)
  {
    if (model.getResourceType() != ResourceType.SIMPLE)
    {
      return;
    }

    for (ResourceMethodDescriptor descriptor : model.getResourceMethodDescriptors())
    {
      ResourceMethod type = descriptor.getType();

      if (!RestConstants.SIMPLE_RESOURCE_METHODS.contains(type))
      {
        throw new ResourceConfigException(
            String.format(
                "Resource '%s' is a simple resource but it contains a method of type %s" +
                " which is not supported on simple resources.",
                                                        model.getName(),
                                                        type.toString()));
      }
    }
  }

  private static void validateAssociation(final ResourceModel model)
  {
    if (model.getResourceType() != ResourceType.ASSOCIATION)
    {
      return;
    }

    if (model.getKeys().size() <= 1)
    {
      throw new ResourceConfigException(String.format("Association '%s' requires more than 1 key.",
                                                      model.getName()));
    }
  }

  private static void validateCrudMethods(final ResourceModel model)
  {
    Map<ResourceMethod, ResourceMethodDescriptor> crudMethods =
        new HashMap<ResourceMethod, ResourceMethodDescriptor>();
    for (ResourceMethodDescriptor descriptor : model.getResourceMethodDescriptors())
    {
      ResourceMethod type = descriptor.getType();
      switch (type)
      {
        case ACTION:
          continue;
        case FINDER:
          continue;
        case BATCH_FINDER:
          continue;
        default:
          if (crudMethods.containsKey(type))
          {
            ResourceMethodDescriptor oldDescriptor = crudMethods.get(type);
            throw new ResourceConfigException(String.format("Resource '%s' contains duplicate methods of type '%s'.  Methods are '%s' and '%s'.",
                                                            model.getName(),
                                                            type.toString(),
                                                            oldDescriptor.getMethod()
                                                                         .getName(),
                                                            descriptor.getMethod()
                                                                      .getName()));
          }
          crudMethods.put(type, descriptor);
      }
    }
  }

  private static Class<? extends RecordTemplate> getCustomCollectionMetadata(final Method method, int metadataIndex)
  {
    Class<? extends RecordTemplate> metadataType = null;
    final Class<?> returnClass = getLogicalReturnClass(method);
    if (CollectionResult.class.isAssignableFrom(returnClass))
    {
      final List<Class<?>> typeArguments = ReflectionUtils.getTypeArguments(CollectionResult.class, returnClass.asSubclass(CollectionResult.class));
      final Class<?> metadataClass;
      if (typeArguments == null || typeArguments.get(metadataIndex) == null)
      {
        // the return type may leave metadata type as parameterized and specify in runtime
        metadataClass = ((Class<?>) ((ParameterizedType) getLogicalReturnType(method)).getActualTypeArguments()[metadataIndex]);
      }
      else
      {
        metadataClass = typeArguments.get(metadataIndex);
      }

      if (!metadataClass.equals(NoMetadata.class))
      {
        metadataType = metadataClass.asSubclass(RecordTemplate.class);
      }
    }
    return metadataType;
  }

  private static void addFinderResourceMethod(final ResourceModel model, final Method method)
  {
    Finder finderAnno = method.getAnnotation(Finder.class);
    if (finderAnno == null)
    {
      return;
    }

    String queryType = finderAnno.value();

    if (queryType != null)
    {
      if (!Modifier.isPublic(method.getModifiers()))
      {
        throw new ResourceConfigException(String.format("Resource '%s' contains non-public finder method '%s'.",
            model.getName(),
            method.getName()));
      }

      List<Parameter<?>> queryParameters = getParameters(model, method, ResourceMethod.FINDER);

      Class<? extends RecordTemplate> metadataType = getCustomCollectionMetadata(method, DEFAULT_METADATA_PARAMETER_INDEX);

      DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations());
      addDeprecatedAnnotation(annotationsMap, method);

      ResourceMethodDescriptor finderMethodDescriptor = ResourceMethodDescriptor.createForFinder(method,
                                                                                                 queryParameters,
                                                                                                 queryType,
                                                                                                 metadataType,
                                                                                                 getInterfaceType(method),
                                                                                                 annotationsMap);

      validateFinderMethod(finderMethodDescriptor, model);
      addServiceErrors(finderMethodDescriptor, method);
      addSuccessStatuses(finderMethodDescriptor, method);

      model.addResourceMethodDescriptor(finderMethodDescriptor);
    }
  }

  private static void addBatchFinderResourceMethod(final ResourceModel model, final Method method)
  {
    BatchFinder finderAnno = method.getAnnotation(BatchFinder.class);
    if (finderAnno == null)
    {
      return;
    }

    String queryType = finderAnno.value();
    if (queryType != null)
    {
      if (!Modifier.isPublic(method.getModifiers()))
      {
        throw new ResourceConfigException(String.format("Resource '%s' contains non-public batch finder method '%s'.",
                                                        model.getName(),
                                                        method.getName()));
      }

      List<Parameter<?>> queryParameters = getParameters(model, method, ResourceMethod.BATCH_FINDER);

      Class<? extends RecordTemplate> metadataType = getCustomCollectionMetadata(method,
                                                                                 BATCH_FINDER_METADATA_PARAMETER_INDEX);
      DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations());
      addDeprecatedAnnotation(annotationsMap, method);

      Integer criteriaIndex = getCriteriaParametersIndex(finderAnno,queryParameters);
      ResourceMethodDescriptor batchFinderMethodDescriptor = ResourceMethodDescriptor.createForBatchFinder(method,
                                                                                                          queryParameters,
                                                                                                          queryType,
                                                                                                          criteriaIndex,
                                                                                                          metadataType,
                                                                                                          getInterfaceType(method),
                                                                                                          annotationsMap);

      validateBatchFinderMethod(batchFinderMethodDescriptor, model);
      addServiceErrors(batchFinderMethodDescriptor, method);
      addSuccessStatuses(batchFinderMethodDescriptor, method);

      model.addResourceMethodDescriptor(batchFinderMethodDescriptor);
    }
  }

  private static Integer getCriteriaParametersIndex(BatchFinder annotation, List<Parameter<?>> parameters)
  {
    for (int i=0; i < parameters.size(); i++)
    {
      if (parameters.get(i).getName().equals(annotation.batchParam()))
      {
        return i;
      }
    }

    return ResourceMethodDescriptor.BATCH_FINDER_NULL_CRITERIA_INDEX;
  }

  /**
   * Handle method that overrides resource template method. Only meaningful for classes
   * that extend a resource template class and only for methods that are NOT annotated
   * with RestMethod. Those are handled in addCrudResourceMethod()
   */
  private static void addTemplateResourceMethod(final Class<?> resourceClass,
                                                final ResourceModel model,
                                                final Method method)
  {
    // Check if the resource class is derived from one of the resource template classes.
    if (!isResourceTemplateClass(resourceClass))
    {
      return;
    }

    // If the method is annotated with RestMethod - ignore - will be handled in
    // addCrudResourceMethod
    if (isRestMethodAnnotated(method))
    {
      return;
    }

    List<Class<?>> parameterTypes = Arrays.asList(method.getParameterTypes());
    boolean partial =
        parameterTypes.contains(PatchRequest.class)
            || parameterTypes.contains(BatchPatchRequest.class);
    ResourceMethod resourceMethod =
        ResourceMethodLookup.fromResourceMethodName(method.getName(), partial);

    if (resourceMethod != null)
    {
      if (!Modifier.isPublic(method.getModifiers()))
      {
        throw new ResourceConfigException(String.format("Resource '%s' contains non-public CRUD method '%s'.",
                                                        model.getName(),
                                                        method.getName()));
      }

      DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations());
      addDeprecatedAnnotation(annotationsMap, method);

      List<Parameter<?>> parameters = getParameters(model, method, resourceMethod);

      ResourceMethodDescriptor resourceMethodDescriptor = ResourceMethodDescriptor.createForRestful(resourceMethod,
                                                                                                    method,
                                                                                                    parameters,
                                                                                                    null,
                                                                                                    getInterfaceType(method),
                                                                                                    annotationsMap);

      addServiceErrors(resourceMethodDescriptor, method);
      addSuccessStatuses(resourceMethodDescriptor, method);

      model.addResourceMethodDescriptor(resourceMethodDescriptor);
    }
  }

  /**
   * Check whether the method is annotated with one of {@link RestMethod} annotations.
   */
  private static boolean isRestMethodAnnotated(final Method method)
  {
    Annotation[] methodAnnotations = method.getAnnotations();
    for (Annotation annotation : methodAnnotations)
    {
      if (RestMethod.getResourceMethod(annotation.annotationType()) != null)
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Check whether the class is one of the resource templated-derived classes.
   */
  private static boolean isResourceTemplateClass(final Class<?> resourceClass)
  {
    for (Class<?> c : RestModelConstants.FIXED_RESOURCE_CLASSES)
    {
      if (c.isAssignableFrom(resourceClass))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Find which rest method annotation is present in the method of the resource, if any,
   * and add MethodDescriptor to ResourceModel.
   *
   * @param resourceClass
   * @param model
   * @param method
   */
  private static void addCrudResourceMethod(final Class<?> resourceClass,
                                            final ResourceModel model,
                                            final Method method)
  {
    boolean restMethodAnnotationFound = false;
    for (Annotation methodAnnotation : method.getAnnotations())
    {
      ResourceMethod resourceMethod =
          RestMethod.getResourceMethod(methodAnnotation.annotationType());
      if (resourceMethod != null)
      {
        if (restMethodAnnotationFound)
        {
          throw new ResourceConfigException("Multiple rest method annotations in method "
              + method.getName());
        }
        restMethodAnnotationFound = true;

        if (!Modifier.isPublic(method.getModifiers()))
        {
          throw new ResourceConfigException(String.format("Resource '%s' contains non-public CRUD method '%s'.",
                                                          model.getName(),
                                                          method.getName()));
        }

        Class<? extends RecordTemplate> metadataType = null;
        if (ResourceMethod.GET_ALL.equals(resourceMethod))
        {
          metadataType = getCustomCollectionMetadata(method, DEFAULT_METADATA_PARAMETER_INDEX);
        }

        DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations());
        addDeprecatedAnnotation(annotationsMap, method);

        List<Parameter<?>> parameters = getParameters(model, method, resourceMethod);
        ResourceMethodDescriptor resourceMethodDescriptor = ResourceMethodDescriptor.createForRestful(resourceMethod,
                                                                                                      method,
                                                                                                      parameters,
                                                                                                      metadataType,
                                                                                                      getInterfaceType(method),
                                                                                                      annotationsMap);

        addServiceErrors(resourceMethodDescriptor, method);
        addSuccessStatuses(resourceMethodDescriptor, method);

        model.addResourceMethodDescriptor(resourceMethodDescriptor);
      }
    }
  }

  /**
   * Add the given action method to the given resource model,  validating the method is a action before adding.
   * @param model provides the model to add the method to.
   * @param method provides the method to add to the model.
   * @throws ResourceConfigException on validation errors.
   */
  private static void addActionResourceMethod(final ResourceModel model, final Method method)
  {
    Action actionAnno = method.getAnnotation(Action.class);
    if (actionAnno == null)
    {
      return;
    }

    String actionName = actionAnno.name();
    List<Parameter<?>> parameters = getParameters(model, method, ResourceMethod.ACTION);

    Class<?> returnClass = getActionReturnClass(model, method, actionAnno, actionName);
    TyperefDataSchema returnTyperefSchema = getActionTyperefDataSchema(model, actionAnno, actionName);
    validateActionReturnType(model, method, returnClass, returnTyperefSchema);

    if (!Modifier.isPublic(method.getModifiers()))
    {
      throw new ResourceConfigException(String.format("Resource '%s' contains non-public action method '%s'.",
                                                      model.getName(),
                                                      method.getName()));
    }

    RecordDataSchema recordDataSchema = DynamicRecordMetadata.buildSchema(method.getName(),
                                                                          parameters);

    RecordDataSchema actionReturnRecordDataSchema;
    FieldDef<?> returnFieldDef;
    if(returnClass != Void.TYPE)
    {
      @SuppressWarnings({"unchecked", "rawtypes"})
      FieldDef<?> nonVoidFieldDef = new FieldDef(ActionResponse.VALUE_NAME, returnClass, getDataSchema(returnClass, returnTyperefSchema));
      returnFieldDef = nonVoidFieldDef;
      actionReturnRecordDataSchema = DynamicRecordMetadata.buildSchema(ActionResponse.class.getName(), Collections.singleton((returnFieldDef)));
    }
    else
    {
      returnFieldDef = null;
      actionReturnRecordDataSchema = DynamicRecordMetadata.buildSchema(ActionResponse.class.getName(), Collections.<FieldDef<?>>emptyList());
    }

    if (model.getResourceLevel() == ResourceLevel.ENTITY && actionAnno.resourceLevel() == ResourceLevel.COLLECTION)
    {
      throw new ResourceConfigException(
          String.format("Resource '%s' is a simple resource, it cannot contain actions at resource level \"COLLECTION\".",
            model.getName()));
    }

    DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations());
    addDeprecatedAnnotation(annotationsMap, method);

    ResourceMethodDescriptor resourceMethodDescriptor = ResourceMethodDescriptor.createForAction(method,
                                                                                                parameters,
                                                                                                actionName,
                                                                                                getActionResourceLevel(actionAnno, model),
                                                                                                returnFieldDef,
                                                                                                actionReturnRecordDataSchema,
                                                                                                recordDataSchema,
                                                                                                getInterfaceType(method),
                                                                                                annotationsMap);

    addServiceErrors(resourceMethodDescriptor, method);
    addSuccessStatuses(resourceMethodDescriptor, method);

    model.addResourceMethodDescriptor(resourceMethodDescriptor);
  }

  private static TyperefDataSchema getActionTyperefDataSchema(ResourceModel model, Action actionAnno, String actionName)
  {
    TyperefDataSchema returnTyperefSchema = null;
    Class<? extends TyperefInfo> typerefInfoClass = actionAnno.returnTyperef();
    try
    {
      returnTyperefSchema = getSchemaFromTyperefInfo(typerefInfoClass);
    }
    catch (Exception e)
    {
      throw new ResourceConfigException("Typeref @Action method named '" + actionName
          + "' on class '" + model.getResourceClass().getName()
          + "' cannot be instantiated, " + e.getMessage());
    }
    return returnTyperefSchema;
  }

  private static Class<?> getActionReturnClass(ResourceModel model, Method method, Action actionAnno, String actionName)
  {
    final Type returnType = getLogicalReturnType(method);
    ResourceMethodDescriptor existingMethodDescriptor =
        model.findActionMethod(actionName, getActionResourceLevel(actionAnno, model));
    if (existingMethodDescriptor != null)
    {
      throw new ResourceConfigException("Found duplicate @Action method named '"
          + actionName + "' on class '" + model.getResourceClass().getName() + '\'');

    }

    Class<?> returnClass = getBoxedTypeFromPrimitive(getLogicalReturnClass(method));
    if (ActionResult.class.isAssignableFrom(returnClass))
    {
      assert(returnType instanceof ParameterizedType);
      final ParameterizedType paramReturnType = (ParameterizedType) returnType;
      final Type[] actualReturnTypes = paramReturnType.getActualTypeArguments();
      assert(actualReturnTypes.length == 1);
      if (!(actualReturnTypes[0] instanceof Class<?>))
      {
        throw new ResourceConfigException("Unsupported type parameter for ActionResult<?>.");
      }
      returnClass = (Class<?>) actualReturnTypes[0];

      if (returnClass == Void.class)
      {
        returnClass = Void.TYPE;
      }
    }
    return returnClass;
  }

  private static ResourceLevel getActionResourceLevel(Action annotation, ResourceModel definingModel)
  {
    return annotation.resourceLevel() == ResourceLevel.ANY ?
        definingModel.getResourceLevel() : annotation.resourceLevel();
  }

  private static Class<?> getBoxedTypeFromPrimitive(Class<?> type)
  {
    if (type.isPrimitive())
    {
      if(type == boolean.class)
        return Boolean.class;
      if(type == byte.class)
        return Byte.class;
      if(type == char.class)
        return Character.class;
      if(type == double.class)
        return Double.class;
      if(type == float.class)
        return Float.class;
      if(type == int.class)
        return Integer.class;
      if(type == long.class)
        return Long.class;
      if(type == short.class)
        return Short.class;
    }
    return type;
  }

  /**
   * Checks that a action method's return type is allowed.
   * @param model provides the resource model the method is being validated for, used for context.
   * @param method provides the action method to validate.
   * @param returnClass provides the declared return type of the method.
   * @param returnTyperefSchema provides the schema of the returnTyperef declared on the method's @Action annotation, or null if returnTyperef is absent.
   * @throws ResourceConfigException on validation errors.
   */
  private static void validateActionReturnType(ResourceModel model,
                                               Method method,
                                               Class<?> returnClass,
                                               TyperefDataSchema returnTyperefSchema)
  {
    if (returnTyperefSchema == null)
    {
      Class<?> returnType = getLogicalReturnClass(method);

      if (!checkParameterType(returnType, RestModelConstants.VALID_ACTION_RETURN_TYPES))
      {
        throw new ResourceConfigException("@Action method '" + method.getName()
            + "' on class '" + method.getDeclaringClass().getName()
            + "' has an invalid return type '" + returnType.getName()
            + "'. Expected a DataTemplate or a primitive");
      }
    }

    final String checkTyperefMessage = checkTyperefSchema(returnClass, returnTyperefSchema);
    if (checkTyperefMessage != null)
    {
      throw new ResourceConfigException("Typeref @Action method named '" + method.getAnnotation(Action.class).name()
                                            + "' on class '" + model.getResourceClass().getName() + "', "
                                            + checkTyperefMessage);
    }
  }

  private static void validateBatchFinderMethod(final ResourceMethodDescriptor batchFinderMethodDescriptor,
      final ResourceModel resourceModel)
  {
    Method method = batchFinderMethodDescriptor.getMethod();

    BatchFinder finderAnno = batchFinderMethodDescriptor.getMethod().getAnnotation(BatchFinder.class);
    if(finderAnno.batchParam().length() == 0){
      throw new ResourceConfigException("The batchParam annotation is required and can't be empty"
          + " on the @BatchFinder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName());
    }

    if(batchFinderMethodDescriptor.getBatchFinderCriteriaParamIndex() == BATCH_FINDER_MISSING_PARAMETER_INDEX) {
      throw new ResourceConfigException("The batchParam annotation doesn't match any parameter name"
          + " on the @BatchFinder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName());
    }

    Parameter<?> batchParam = batchFinderMethodDescriptor.getParameter(finderAnno.batchParam());

    if (!batchParam.isArray() || !DataTemplate.class.isAssignableFrom(batchParam.getItemType()))
    {
      throw new ResourceConfigException("The batchParam '" + finderAnno.batchParam()
          + "' on the @BatchFinder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName() + "' must be a array of RecordTemplate");
    }

    Class<?> valueClass = resourceModel.getValueClass();

    Class<?> returnType, elementType, criteriaType, metadataType;
    try
    {
      returnType = getLogicalReturnClass(method);
      final List<Class<?>> typeArguments;
      if (!BatchFinderResult.class.isAssignableFrom(returnType))
      {
        throw new ResourceConfigException("@BatchFinder method '" + method.getName()
            + "' on class '" + resourceModel.getResourceClass().getName()
            + "' has an unsupported return type");
      }

      final ParameterizedType collectionType = (ParameterizedType) getLogicalReturnType(method);
      criteriaType = (Class<?>) collectionType.getActualTypeArguments()[0];
      elementType = (Class<?>) collectionType.getActualTypeArguments()[1];
      metadataType = (Class<?>) collectionType.getActualTypeArguments()[2];
    }
    catch (ClassCastException e)
    {
      throw new ResourceConfigException("@BatchFinder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName()
          + "' has an invalid return or a data template type", e);
    }

    if (!RecordTemplate.class.isAssignableFrom(elementType)
        || !valueClass.equals(elementType))
    {
      String collectionClassName = returnType.getSimpleName();
      throw new ResourceConfigException("@BatchFinder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName()
          + "' has an invalid return type. Expected " + collectionClassName + "<"
          + valueClass.getName() + ">, but found " + collectionClassName + "<"
          + elementType + '>');
    }

    if (!RecordTemplate.class.isAssignableFrom(metadataType) ||
        !RecordTemplate.class.isAssignableFrom(criteriaType))
    {
      String collectionClassName = returnType.getSimpleName();
      throw new ResourceConfigException("@BatchFinder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName()
          + "' has an invalid return type. The criteria and the metadata parameterized types "
          + "must be a RecordTemplate");
    }


    ResourceMethodDescriptor existingBatchFinder =
        resourceModel.findBatchFinderMethod(batchFinderMethodDescriptor.getBatchFinderName());
    if (existingBatchFinder != null)
    {
      throw new ResourceConfigException("Found duplicate @BatchFinder method named '"
          + batchFinderMethodDescriptor.getFinderName() + "' on class '"
          + resourceModel.getResourceClass().getName() + '\'');
    }

  }

  private static void validateFinderMethod(final ResourceMethodDescriptor finderMethodDescriptor,
                                           final ResourceModel resourceModel)
  {
    Method method = finderMethodDescriptor.getMethod();
    Class<?> valueClass = resourceModel.getValueClass();

    if (ResourceEntityType.UNSTRUCTURED_DATA == resourceModel.getResourceEntityType())
    {
      throw new ResourceConfigException("Class '" + resourceModel.getResourceClass().getSimpleName()
          + "' does not support @Finder methods, because it's an unstructured data resource");
    }

    Class<?> returnType, elementType;
    try
    {
      returnType = getLogicalReturnClass(method);
      final List<Class<?>> typeArguments;
      if (List.class.isAssignableFrom(returnType))
      {
        typeArguments = ReflectionUtils.getTypeArguments(List.class, returnType.asSubclass(List.class));
      }
      else if (CollectionResult.class.isAssignableFrom(returnType))
      {
        typeArguments = ReflectionUtils.getTypeArguments(CollectionResult.class, returnType.asSubclass(CollectionResult.class));
      }
      else
      {
        throw new ResourceConfigException("@Finder method '" + method.getName()
            + "' on class '" + resourceModel.getResourceClass().getName()
            + "' has an unsupported return type");
      }

      if (typeArguments == null || typeArguments.get(0) == null)
      {
        // the return type may leave value type as parameterized and specify in runtime
        final ParameterizedType collectionType = (ParameterizedType) getLogicalReturnType(method);
        elementType = (Class<?>) collectionType.getActualTypeArguments()[0];
      }
      else
      {
        elementType = typeArguments.get(0);
      }
    }
    catch (ClassCastException e)
    {
      throw new ResourceConfigException("@Finder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName()
          + "' has an invalid return or a data template type", e);
    }

    if (!List.class.isAssignableFrom(returnType)
        && !CollectionResult.class.isAssignableFrom(returnType))
    {
      throw new ResourceConfigException("@Finder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName()
          + "' has an invalid return type '" + returnType.getName() + "'. Expected "
          + "List<" + valueClass.getName() + "> or CollectionResult<"
          + valueClass.getName() + ">");
    }

    String collectionClassName = returnType.getSimpleName();
    if (!RecordTemplate.class.isAssignableFrom(elementType)
        || !valueClass.equals(elementType))
    {
      throw new ResourceConfigException("@Finder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName()
          + "' has an invalid return type. Expected " + collectionClassName + "<"
          + valueClass.getName() + ">, but found " + collectionClassName + "<"
          + elementType + '>');
    }

    ResourceMethodDescriptor existingFinder =
        resourceModel.findFinderMethod(finderMethodDescriptor.getFinderName());
    if (existingFinder != null)
    {
      throw new ResourceConfigException("Found duplicate @Finder method named '"
          + finderMethodDescriptor.getFinderName() + "' on class '"
          + resourceModel.getResourceClass().getName() + '\'');
    }

    // query parameters are checked in getQueryParameters method
  }

  private static ResourceModel processActions(final Class<?> actionResourceClass, ResourceModel parentResourceModel)
  {
    RestLiActions actionsAnno = actionResourceClass.getAnnotation(RestLiActions.class);

    String name = actionsAnno.name();

    String namespace = actionsAnno.namespace();

    ResourceModel actionResourceModel = new ResourceModel(null, // primary key
                                                          null, // key key class
                                                          null, // key params class
                                                          Collections.<Key> emptySet(), // keys
                                                          null, // value class
                                                          actionResourceClass, // resource class
                                                          null, // parent resource class
                                                          name, // name
                                                          ResourceType.ACTIONS, // resource type
                                                          namespace); // namespace

    actionResourceModel.setParentResourceModel(parentResourceModel);

    for (Method method : actionResourceClass.getDeclaredMethods())
    {
      // ignore synthetic, type-erased versions of methods
      if (method.isSynthetic())
      {
        continue;
      }

      addActionResourceMethod(actionResourceModel, method);
    }

    log.info("Processed actions resource '" + actionResourceClass.getName() + '\'');

    return actionResourceModel;
  }

  /**
   * @return the type of interface that was implemented: synchronous, callback-based, or
   *         promise-based
   */
  private static InterfaceType getInterfaceType(final Method method)
  {
    boolean promise = Promise.class.isAssignableFrom(method.getReturnType());
    boolean task = Task.class.isAssignableFrom(method.getReturnType());
    boolean callback = getParamIndex(method, Callback.class) != -1;
    boolean isVoid = method.getReturnType().equals(Void.TYPE);

    if (callback && !isVoid)
    {
      throw new ResourceConfigException(String.format("%s has both callback and return value", method));
      // note that !callback && !isVoid is a legal synchronous action method
    }

    if (callback)
    {
      return InterfaceType.CALLBACK;
    }
    else if (task)
    {
      return InterfaceType.TASK;
    }
    else if (promise)
    {
      return InterfaceType.PROMISE;
    }
    else
    {
      return InterfaceType.SYNC;
    }
  }

  /**
   * @return the type of the callback parameter on the given method
   */
  private static Type getCallbackParamType(final Method method)
  {
    int i = getParamIndex(method, Callback.class);
    if (i == -1)
    {
      return null;
    }
    else
    {
      return method.getGenericParameterTypes()[i];
    }
  }

  /**
   * @return the index of the parameter of the given type, or -1 if there is no such
   *         parameter
   * @throws ResourceConfigException if the method has multiple parameters of the given
   *           type
   */
  private static int getParamIndex(final Method method, final Class<?> type)
  {
    Type[] types = method.getGenericParameterTypes();
    int where = -1;
    for (int i = 0; i < types.length; i++)
    {
      Class<?> c = ReflectionUtils.getClass(types[i]);
      if (c != null && type.equals(c))
      {
        if (where == -1)
        {
          where = i;
        }
        else
        {
          throw new ResourceConfigException(String.format("method '%s' has too many '%s' parameters",
                                                          method,
                                                          type));
        }
      }
    }
    return where;
  }

  /**
   * Analogous to {@link #getLogicalReturnType}. For callback and promise,
   * <code>java.lang.Void.class</code> is converted to <code>Void.TYPE</code>.
   */
  private static Class<?> getLogicalReturnClass(final Method method)
  {
    Class<?> c = ReflectionUtils.getClass(getLogicalReturnType(method));
    return Void.class.equals(c) ? Void.TYPE : c;
  }

  /**
   * Get the logical return type of a RestLi method. For callback-based resources, this is
   * the type argument of the callback parameter. For promise-based resources, this is the
   * type argument of the returned promise.
   */
  private static Type getLogicalReturnType(final Method method)
  {
    switch (getInterfaceType(method))
    {
      case CALLBACK:
        // Callback<T>
        ParameterizedType callbackType = (ParameterizedType) getCallbackParamType(method);
        return callbackType.getActualTypeArguments()[0];
      case PROMISE:
      case TASK:
        // Promise<T> or Task<T>
        ParameterizedType promiseType = (ParameterizedType) method.getGenericReturnType();
        return promiseType.getActualTypeArguments()[0];
      case SYNC:
        return method.getGenericReturnType();
      default:
        throw new AssertionError();
    }
  }

  /**
   * Reads annotations on a given resource class in order to build service errors, which are then added to
   * a given resource model.
   *
   * @param resourceModel resource model to add service errors to
   * @param resourceClass class annotated with service errors
   */
  private static void addServiceErrors(final ResourceModel resourceModel, final Class<?> resourceClass)
  {
    final ServiceErrorDef serviceErrorDefAnnotation = resourceClass.getAnnotation(ServiceErrorDef.class);
    final ServiceErrors serviceErrorsAnnotation = resourceClass.getAnnotation(ServiceErrors.class);

    final List<ServiceError> serviceErrors = buildServiceErrors(serviceErrorDefAnnotation,
                                                                    serviceErrorsAnnotation,
                                                                    null,
                                                                    resourceClass,
                                                                    null);

    if (serviceErrors == null)
    {
      return;
    }

    resourceModel.setServiceErrors(serviceErrors);
  }

  /**
   * Reads annotations on a given method in order to build service errors, which are then added to
   * a given resource method descriptor.
   *
   * @param resourceMethodDescriptor resource method descriptor to add service errors to
   * @param method method annotated with service errors
   */
  private static void addServiceErrors(final ResourceMethodDescriptor resourceMethodDescriptor, final Method method)
  {
    final Class<?> resourceClass = method.getDeclaringClass();
    final ServiceErrorDef serviceErrorDefAnnotation = resourceClass.getAnnotation(ServiceErrorDef.class);
    final ServiceErrors serviceErrorsAnnotation = method.getAnnotation(ServiceErrors.class);
    final ParamError[] paramErrorAnnotations = method.getAnnotationsByType(ParamError.class);

    final List<ServiceError> serviceErrors = buildServiceErrors(serviceErrorDefAnnotation,
                                                                    serviceErrorsAnnotation,
                                                                    paramErrorAnnotations,
                                                                    resourceClass,
                                                                    method);
    if (serviceErrors == null)
    {
      return;
    }

    // Form a set of parameter names which exist on this method
    final Set<String> acceptableParameterNames = resourceMethodDescriptor.getParameters()
        .stream()
        .map(Parameter::getName)
        .collect(Collectors.toSet());

    // Validate that all parameter names are valid
    for (ServiceError serviceError : serviceErrors)
    {
      if (serviceError instanceof ParametersServiceError)
      {
        final String[] parameterNames = ((ParametersServiceError) serviceError).parameterNames();
        if (parameterNames != null)
        {
          for (String parameterName : parameterNames)
          {
            if (!acceptableParameterNames.contains(parameterName))
            {
              throw new ResourceConfigException(
                  String.format("Nonexistent parameter '%s' specified for method-level service error '%s' in %s (valid parameters: %s)",
                      parameterName,
                      serviceError.code(),
                      buildExceptionLocationString(resourceClass, method),
                      acceptableParameterNames.toString()));
            }
          }
        }
      }
    }

    resourceMethodDescriptor.setServiceErrors(serviceErrors);
  }

  /**
   * Given a {@link ServiceErrorDef} annotation, a {@link ServiceErrors} annotation, and an array of {@link ParamError}
   * annotations, builds a list of service errors by mapping the service error codes in {@link ServiceErrors} and
   * {@link ParamError} against the service errors defined in {@link ServiceErrorDef}. Also, the {@link ParamError}
   * annotations are used to add parameter names. Uses the resource class and method purely for constructing
   * exception messages.
   *
   * @param serviceErrorDefAnnotation service error definition annotation
   * @param serviceErrorsAnnotation service error codes annotation, may be null
   * @param paramErrorAnnotations parameter error annotations, may be null
   * @param resourceClass resource class
   * @param method method, should be null if building resource-level service errors
   * @return list of service errors
   */
  private static List<ServiceError> buildServiceErrors(final ServiceErrorDef serviceErrorDefAnnotation,
      final ServiceErrors serviceErrorsAnnotation, final ParamError[] paramErrorAnnotations, final Class<?> resourceClass,
      final Method method)
  {
    if (serviceErrorsAnnotation == null && (paramErrorAnnotations == null || paramErrorAnnotations.length == 0))
    {
      return null;
    }

    if (serviceErrorDefAnnotation == null)
    {
      throw new ResourceConfigException(
          String.format("Resource '%s' is missing a @%s annotation",
              resourceClass.getName(),
              ServiceErrorDef.class.getSimpleName()));
    }

    // Create a mapping of all valid codes to their respective service errors
    // TODO: If this class is ever refactored into a better OO solution, only build this once per resource
    final Map<String, ServiceError> serviceErrorCodeMapping = Arrays.stream(serviceErrorDefAnnotation.value().getEnumConstants())
        .map((Enum<? extends ServiceError> e) -> (ServiceError) e )
        .collect(Collectors.toMap(
            ServiceError::code,
            Function.identity()
        ));

    // Build a list to collect all service error codes specified for this resource/method
    final List<String> serviceErrorCodes = new ArrayList<>();
    if (serviceErrorsAnnotation != null)
    {
      serviceErrorCodes.addAll(Arrays.asList(serviceErrorsAnnotation.value()));
    }

    // Create a mapping of service error codes to their parameters (order must be maintained for consistent IDLs)
    final LinkedHashMap<String, String[]> paramsMapping = buildServiceErrorParameters(paramErrorAnnotations,
                                                                                      resourceClass,
                                                                                      method);

    // Validate the codes and add any new codes to the master code list
    for (String serviceErrorCode : paramsMapping.keySet())
    {
      // Check for codes redundantly specified in the service errors annotation
      if (serviceErrorCodes.contains(serviceErrorCode))
      {
        throw new ResourceConfigException(
            String.format("Service error code '%s' redundantly specified in both @%s and @%s annotations on %s",
                serviceErrorCode,
                ServiceErrors.class.getSimpleName(),
                ParamError.class.getSimpleName(),
                buildExceptionLocationString(resourceClass, method)));
      }
      // Add new service error code
      serviceErrorCodes.add(serviceErrorCode);
    }

    // Build service errors from specified codes using this mapping
    return buildServiceErrors(serviceErrorCodes, serviceErrorCodeMapping, paramsMapping, resourceClass, method);
  }

  /**
   * Builds a list of {@link ServiceError} objects given a list of codes, a mapping from code to service error, and a
   * mapping from code to parameter names. Also uses the resource class and method to construct an exception message.
   *
   * @param serviceErrorCodes list of service error codes indicating which service errors to build
   * @param serviceErrorCodeMapping mapping from service error code to service error
   * @param paramsMapping mapping from service error codes to array of parameter names
   * @param resourceClass resource class
   * @param method resource method
   * @return list of service errors
   */
  private static List<ServiceError> buildServiceErrors(final List<String> serviceErrorCodes,
      final Map<String, ServiceError> serviceErrorCodeMapping, final Map<String, String[]> paramsMapping,
      final Class<?> resourceClass, final Method method)
  {
    final Set<String> existingServiceErrorCodes = new HashSet<>();
    final List<ServiceError> serviceErrors = new ArrayList<>(serviceErrorCodes.size());
    for (String serviceErrorCode : serviceErrorCodes)
    {
      // Check for duplicate service error codes
      if (existingServiceErrorCodes.contains(serviceErrorCode))
      {
        throw new ResourceConfigException(
            String.format("Duplicate service error code '%s' used in %s",
                serviceErrorCode,
                buildExceptionLocationString(resourceClass, method)));
      }

      // Attempt to map this code to its corresponding service error
      if (serviceErrorCodeMapping.containsKey(serviceErrorCode))
      {
        final ServiceError serviceError = serviceErrorCodeMapping.get(serviceErrorCode);

        // Validate that this service error doesn't use the ErrorDetails type
        final Class<? extends RecordTemplate> errorDetailType = serviceError.errorDetailType();
        if (errorDetailType != null && errorDetailType.equals(ErrorDetails.class))
        {
          throw new ResourceConfigException(
              String.format("Class '%s' is not meant to be used as an error detail type, please use a more specific "
                  + "model or remove from service error '%s' in %s",
                  errorDetailType.getCanonicalName(),
                  serviceErrorCode,
                  buildExceptionLocationString(resourceClass, method)));
        }

        // Determine if this is a method-level service error with parameters associated with it
        final String[] parameterNames = paramsMapping.get(serviceErrorCode);

        // Depending on if there are service errors, either add it directly or wrap it with the parameter names
        serviceErrors.add(parameterNames == null ? serviceError : new ParametersServiceError(serviceError, parameterNames));
      }
      else
      {
        throw new ResourceConfigException(
            String.format("Unknown service error code '%s' used in %s",
                serviceErrorCode,
                buildExceptionLocationString(resourceClass, method)));
      }

      // Mark this code as seen to prevent duplicates
      existingServiceErrorCodes.add(serviceErrorCode);
    }

    return serviceErrors;
  }

  /**
   * Given an array of {@link ParamError} annotations, build a mapping from service error code to parameter names.
   * Uses the resource class and method to construct exception messages.
   *
   * @param paramErrorAnnotations parameter error annotations
   * @param resourceClass resource class
   * @param method resource method
   * @return mapping from service error code to parameter names
   */
  private static LinkedHashMap<String, String[]> buildServiceErrorParameters(final ParamError[] paramErrorAnnotations,
      final Class<?> resourceClass, final Method method)
  {
    // Create a mapping of service error codes to their parameters (if any)
    final LinkedHashMap<String, String[]> paramsMapping = new LinkedHashMap<>();
    if (paramErrorAnnotations != null)
    {
      for (ParamError paramErrorAnnotation : paramErrorAnnotations)
      {
        final String serviceErrorCode = paramErrorAnnotation.code();

        // Check for redundant parameter error annotations
        if (paramsMapping.containsKey(serviceErrorCode))
        {
          throw new ResourceConfigException(
              String.format("Redundant @%s annotations for service error code '%s' used in %s",
                  ParamError.class.getSimpleName(),
                  serviceErrorCode,
                  buildExceptionLocationString(resourceClass, method)));
        }

        final String[] parameterNames = paramErrorAnnotation.parameterNames();

        // Ensure the parameter names array is non-empty
        if (parameterNames.length == 0)
        {
          throw new ResourceConfigException(
              String.format("@%s annotation on %s specifies no parameter names for service error code '%s'",
                  ParamError.class.getSimpleName(),
                  buildExceptionLocationString(resourceClass, method),
                  serviceErrorCode));
        }

        // Ensure that there are no duplicate parameter names
        if (parameterNames.length != new HashSet<>(Arrays.asList(parameterNames)).size())
        {
          throw new ResourceConfigException(
              String.format("Duplicate parameter specified for service error code '%s' in %s",
                  serviceErrorCode,
                  buildExceptionLocationString(resourceClass, method)));
        }

        paramsMapping.put(serviceErrorCode, paramErrorAnnotation.parameterNames());
      }
    }

    return paramsMapping;
  }

  /**
   * Does extra validation of the generated service errors for a resource. This method should only be called
   * after all the resource-level and method-level service errors have been added to the resource model.
   *
   * @param resourceModel resource model to validate
   * @param resourceClass class represented by the resource model
   */
  private static void validateServiceErrors(final ResourceModel resourceModel, final Class<?> resourceClass)
  {
    final ServiceErrorDef serviceErrorDefAnnotation = resourceClass.getAnnotation(ServiceErrorDef.class);

    // Log a warning if the resource uses an unnecessary service error definition annotation
    if (serviceErrorDefAnnotation != null && !resourceModel.isAnyServiceErrorListDefined()) {
      log.warn(String.format("Resource '%1$s' uses an unnecessary @%2$s annotation, as no corresponding @%3$s "
            + "or @%4$s annotations were found on the class or any of its methods. Either the @%2$s annotation should be "
            + "removed or a @%3$s or @%4$s annotation should be added.",
          resourceClass.getName(),
          ServiceErrorDef.class.getSimpleName(),
          ServiceErrors.class.getSimpleName(),
          ParamError.class.getSimpleName()));
    }
  }

  /**
   * Reads annotations on a given resource method in order to build success statuses, which are then added to
   * a given resource method descriptor.
   *
   * @param resourceMethodDescriptor resource method descriptor to add success statuses to
   * @param method method possibly annotated with a success annotation
   */
  private static void addSuccessStatuses(final ResourceMethodDescriptor resourceMethodDescriptor, final Method method)
  {
    final Class<?> resourceClass = method.getDeclaringClass();
    final SuccessResponse successResponseAnnotation = method.getAnnotation(SuccessResponse.class);

    if (successResponseAnnotation == null)
    {
      return;
    }

    // Build success status list from the annotation
    final List<HttpStatus> successStatuses = Arrays.stream(successResponseAnnotation.statuses())
        .collect(Collectors.toList());

    if (successStatuses.isEmpty())
    {
      throw new ResourceConfigException(
          String.format("@%s annotation on %s specifies no success statuses",
              SuccessResponse.class.getSimpleName(),
              buildExceptionLocationString(resourceClass, method)));
    }

    // Validate the success statuses
    for (HttpStatus successStatus : successStatuses)
    {
      if (successStatus.getCode() < 200 || successStatus.getCode() >= 400)
      {
        throw new ResourceConfigException(
            String.format("Invalid success status '%s' specified in %s",
                successStatus,
                buildExceptionLocationString(resourceClass, method)));
      }
    }

    resourceMethodDescriptor.setSuccessStatuses(successStatuses);
  }

  /**
   * Generates a human-readable phrase describing an exception's origin in a resource class,
   * whether it be at the resource level or at the level of a particular method.
   *
   * @param resourceClass resource class
   * @param method method (may be null)
   * @return human-readable string
   */
  private static String buildExceptionLocationString(Class<?> resourceClass, Method method)
  {
    if (method == null)
    {
      return String.format("resource '%s'", resourceClass.getName());
    }

    return String.format("method '%s' of resource '%s'", method.getName(), resourceClass.getName());
  }
}

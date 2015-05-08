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
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.data.template.TyperefInfo;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.validation.CreateOnly;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.common.validation.ValidationUtil;
import com.linkedin.restli.internal.common.ReflectionUtils;
import com.linkedin.restli.internal.common.TyperefUtils;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchDeleteRequest;
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
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.AssocKey;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.HeaderParam;
import com.linkedin.restli.server.annotations.Keys;
import com.linkedin.restli.server.annotations.MetadataProjectionParam;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.PagingProjectionParam;
import com.linkedin.restli.server.annotations.ParSeqContext;
import com.linkedin.restli.server.annotations.ParSeqContextParam;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.Projection;
import com.linkedin.restli.server.annotations.ProjectionParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.ResourceContextParam;
import com.linkedin.restli.server.annotations.RestAnnotations;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestLiTemplate;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ValidatorParam;
import com.linkedin.restli.server.resources.ComplexKeyResource;
import com.linkedin.restli.server.resources.ComplexKeyResourceAsync;
import com.linkedin.restli.server.resources.ComplexKeyResourcePromise;
import com.linkedin.restli.server.resources.ComplexKeyResourceTask;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.SingleObjectResource;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author dellamag
 */
public final class RestLiAnnotationReader
{
  private static final Logger log = LoggerFactory.getLogger(RestLiAnnotationReader.class);
  private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("\\W");
  /**
   * This is a utility class.
   */
  private RestLiAnnotationReader()
  {
  }

  /**
   * Processes an annotated resource class, producing a ResourceModel.
   *
   * @param resourceClass annotated resource class
   * @return {@link ResourceModel} for the provided resource class
   */
  public static ResourceModel processResource(final Class<?> resourceClass)
  {
    final ResourceModel model;

    checkAnnotation(resourceClass);

    if ((resourceClass.isAnnotationPresent(RestLiCollection.class) ||
         resourceClass.isAnnotationPresent(RestLiAssociation.class)))
    {
      // If any of these annotations, a subclass of KeyValueResource is expected
      if (!KeyValueResource.class.isAssignableFrom(resourceClass))
      {
        throw new RestLiInternalException("Resource class '" + resourceClass.getName()
            + "' declares RestLi annotation but does not implement "
            + KeyValueResource.class.getName() + " interface.");
      }

      @SuppressWarnings("unchecked")
      Class<? extends KeyValueResource<?, ?>> clazz =
          (Class<? extends KeyValueResource<?, ?>>) resourceClass;
      model = processCollection(clazz);
    }
    else if (resourceClass.isAnnotationPresent(RestLiActions.class))
    {
      model = processActions(resourceClass);
    }
    else if (resourceClass.isAnnotationPresent(RestLiSimpleResource.class))
    {
      @SuppressWarnings("unchecked")
      Class<? extends SingleObjectResource<?>> clazz =
          (Class<? extends SingleObjectResource<?>>) resourceClass;
      model = processSingleObjectResource(clazz);
    }
    else
    {
      throw new ResourceConfigException("Class '" + resourceClass.getName()
          + "' must be annotated with a valid @RestLi... annotation");
    }

    if (!model.isActions())
    {
      checkRestLiDataAnnotations(resourceClass, (RecordDataSchema) getDataSchema(model.getValueClass(), null));
    }
    DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(resourceClass.getAnnotations());
    addDeprecatedAnnotation(annotationsMap, resourceClass);

    model.setCustomAnnotation(annotationsMap);
    return model;
  }

  /**
   * Check if resourceClass is annotated with the expected annotation of its template class,
   * expressed in the {@link RestLiTemplate} annotation.
   */
  private static void checkAnnotation(final Class<?> resourceClass)
  {
    Class templateClass = resourceClass;
    while (templateClass != Object.class)
    {
      templateClass = templateClass.getSuperclass();

      final RestLiTemplate templateAnnotation = (RestLiTemplate) templateClass.getAnnotation(RestLiTemplate.class);
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
      annotationsMap.put("deprecated", new DataMap());
    }
    return annotationsMap;
  }

  private static DataMap addDeprecatedAnnotation(DataMap annotationsMap, AnnotatedElement annotatedElement)
  {
    if(annotatedElement.isAnnotationPresent(Deprecated.class))
    {
      annotationsMap.put("deprecated", new DataMap());
    }
    return annotationsMap;
  }

  private static void checkPathsAgainstSchema(RecordDataSchema dataSchema, String resourceClassName, String annotationName, String[] paths)
  {
    for (String path : paths)
    {
      if (!ValidationUtil.containsPath(dataSchema, path))
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
          if (existingPath.startsWith(path))
          {
            throw new ResourceConfigException("In resource class '" + resourceClassName + "', " + existingPath
                                                  + " is marked as " + existingAnnotationName + ", but is contained in a "
                                                  + annotationName + " field " + path + ".");
          }
          else if (path.startsWith(existingPath))
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

  @SuppressWarnings("unchecked")
  private static ResourceModel processCollection(final Class<? extends KeyValueResource<?, ?>> collectionResourceClass)
  {
    Class<?> keyClass;
    Class<? extends RecordTemplate> keyKeyClass = null;
    Class<? extends RecordTemplate> keyParamsClass = null;
    Class<? extends RecordTemplate> valueClass;
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
    else if (ComplexKeyResourcePromise.class.isAssignableFrom(collectionResourceClass))
    {
      complexKeyResourceBase = ComplexKeyResourcePromise.class;
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
        kvParams = ReflectionUtils.getTypeArguments(ComplexKeyResourcePromise.class,
                                                    (Class<? extends ComplexKeyResourcePromise<?, ?, ?>>) collectionResourceClass);
      }

      keyClass = ComplexResourceKey.class;
      keyKeyClass = kvParams.get(0).asSubclass(RecordTemplate.class);
      keyParamsClass = kvParams.get(1).asSubclass(RecordTemplate.class);
      valueClass = kvParams.get(2).asSubclass(RecordTemplate.class);
    }

    // Otherwise, it's a KeyValueResource, whose parameters are resource key and resource
    // value
    else
    {
      List<Type> actualTypeArguments =
          ReflectionUtils.getTypeArgumentsParametrized(KeyValueResource.class,
                                                       collectionResourceClass);
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

      valueClass = ReflectionUtils.getClass(actualTypeArguments.get(1)).asSubclass(RecordTemplate.class);
    }

    ResourceType resourceType = getResourceType(collectionResourceClass);

    RestLiAnnotationData annotationData;
    if (collectionResourceClass.isAnnotationPresent(RestLiCollection.class))
    {
      annotationData =
          new RestLiAnnotationData(collectionResourceClass.getAnnotation(RestLiCollection.class));
    }
    else if (collectionResourceClass.isAnnotationPresent(RestLiAssociation.class))
    {
      annotationData =
          new RestLiAnnotationData(collectionResourceClass.getAnnotation(RestLiAssociation.class));
    }
    else
    {
      throw new ResourceConfigException("No valid annotation on resource class '"
          + collectionResourceClass.getName() + "'");
    }

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
    addResourceMethods(collectionResourceClass, collectionModel);

    log.info("Processed collection resource '" + collectionResourceClass.getName() + "'");

    return collectionModel;
  }

  private static ResourceModel processSingleObjectResource(
      final Class<? extends SingleObjectResource<?>> singleObjectResourceClass)
  {
    Class<? extends RecordTemplate> valueClass;

    List<Class<?>> kvParams =
        ReflectionUtils.getTypeArguments(SingleObjectResource.class,
                singleObjectResourceClass);

    valueClass = kvParams.get(0).asSubclass(RecordTemplate.class);

    ResourceType resourceType = getResourceType(singleObjectResourceClass);

    RestLiAnnotationData annotationData;
    if (singleObjectResourceClass.isAnnotationPresent(RestLiSimpleResource.class))
    {
      annotationData =
          new RestLiAnnotationData(singleObjectResourceClass.getAnnotation(RestLiSimpleResource.class));
    }
    else
    {
      throw new ResourceConfigException("No valid annotation on resource class '"
                                            + singleObjectResourceClass.getName() + "'");
    }

    String name = annotationData.name();
    String namespace = annotationData.namespace();

    Class<?> parentResourceClass =
        annotationData.parent().equals(RestAnnotations.ROOT.class) ? null
            : annotationData.parent();

    ResourceModel singleObjectResourceModel =
        new ResourceModel(valueClass,
                          singleObjectResourceClass,
                          parentResourceClass,
                          name,
                          resourceType,
                          namespace);

    addResourceMethods(singleObjectResourceClass, singleObjectResourceModel);

    log.info("Processed single object resource '" + singleObjectResourceClass.getName() + "'");

    return singleObjectResourceModel;
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
        else if (paramAnnotations.contains(AssocKey.class))
        {
          param = buildAssocKeyParam(model, method, paramAnnotations, paramType, AssocKey.class);
        }
        else if (paramAnnotations.contains(AssocKeyParam.class))
        {
          param = buildAssocKeyParam(model, method, paramAnnotations, paramType, AssocKeyParam.class);
        }
        else if (paramAnnotations.contains(Context.class))
        {
          param = buildPagingContextParam(paramAnnotations, paramType, Context.class);
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
        else if (paramAnnotations.contains(ParSeqContext.class))
        {
          param = buildParSeqContextParam(method, methodType, idx, paramType, paramAnnotations, ParSeqContext.class);
        }
        else if (paramAnnotations.contains(Projection.class))
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
        else if (paramAnnotations.contains(Keys.class))
        {
          param = buildPathKeysParam(paramAnnotations, paramType, Keys.class);
        }
        else if (paramAnnotations.contains(PathKeysParam.class))
        {
          param = buildPathKeysParam(paramAnnotations, paramType, PathKeysParam.class);
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
        else
        {
          throw new ResourceConfigException(buildMethodMessage(method)
              + " must annotate each parameter with @QueryParam, @ActionParam, @AssocKeyParam, @PagingContextParam, " +
              "@ProjectionParam, @MetadataProjectionParam, @PagingProjectionParam, @PathKeysParam, @HeaderParam, " +
              "@CallbackParam, @ResourceContext or @ParSeqContextParam");
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

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static Parameter<ResourceContext> buildResourceContextParam(AnnotationSet annotations, final Class<?> paramType)
  {
      if (!paramType.equals(ResourceContext.class))
      {
        throw new ResourceConfigException("Incorrect data type for param: @" + ResourceContextParam.class.getSimpleName() + " parameter annotation must be of type " +  ResourceContext.class.getName());
      }
      Optional optional = annotations.get(Optional.class);
      return new Parameter("",
                           paramType,
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
    return new Parameter("validator",
        paramType,
        null,
        false,
        null,
        Parameter.ParamType.VALIDATOR_PARAM,
        false,
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
        new Parameter("",
                      paramType,
                      null,
                      false,
                      null,
                      Parameter.ParamType.CALLBACK,
                      false,
                      annotations);
    return param;
  }

  private static Parameter<com.linkedin.parseq.Context> buildParSeqContextParam(final Method method,
                                                                                final ResourceMethod methodType,
                                                                                final int idx,
                                                                                final Class<?> paramType,
                                                                                final AnnotationSet annotations,
                                                                                final Class<?> paramAnnotationType)
  {
    if (!com.linkedin.parseq.Context.class.equals(paramType))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + ParSeqContextParam.class.getSimpleName() + " or @" + ParSeqContext.class.getSimpleName() +
              " parameter annotation must be of type " +  com.linkedin.parseq.Context.class.getName());
    }
    if (getInterfaceType(method) != InterfaceType.PROMISE)
    {
      throw new ResourceConfigException("Cannot have ParSeq context on non-promise method");
    }
    Parameter.ParamType parameter = null;
    if(paramAnnotationType.equals(ParSeqContext.class))
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
    return new Parameter<com.linkedin.parseq.Context>("",
                                                      com.linkedin.parseq.Context.class,
                                                      null,
                                                      false,
                                                      null,
                                                      parameter,
                                                      false,
                                                      annotations);
  }

  // bug in javac 7 that doesn't obey the unchecked suppression, had to abstract to method to workaround.
  @SuppressWarnings({"unchecked"})
  private static Integer annotationCount(final AnnotationSet annotations)
  {
    return annotations.count(QueryParam.class,
                             ActionParam.class,
                             AssocKeyParam.class,
                             PagingContextParam.class,
                             CallbackParam.class,
                             ParSeqContextParam.class);
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

    if (methodType == ResourceMethod.ACTION)
    {
      if (annotations.contains(QueryParam.class))
      {
        throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                              + buildMethodMessage(method) + " is a @QueryParam but action method cannot have @QueryParam");
      }

      if (param.getParamType() == Parameter.ParamType.POST
          && !(checkParameterType(param.getType(), RestModelConstants.VALID_ACTION_PARAMETER_TYPES) ||
               checkParameterHasTyperefSchema(param)))
      {
        throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                              + buildMethodMessage(method) + " is not a valid type (" + param.getType() + ')');
      }
    }
    else if (param.getParamType() == Parameter.ParamType.QUERY
          && !(checkParameterType(param.getType(), RestModelConstants.VALID_QUERY_PARAMETER_TYPES) ||
               checkParameterHasTyperefSchema(param)))
    {
        throw new ResourceConfigException("Parameter '" + paramName + "' on "
                                              + buildMethodMessage(method) + " is not a valid type (" + param.getType() + ')');
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

    if (annotationCount(annotations) > 1)
    {
      throw new ResourceConfigException(buildMethodMessage(method)
          + "' must declare only one of @QueryParam, @ActionParam, @AssocKeyParam, @PagingContextParam, or @CallbackParam");
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

  private static Parameter<?> buildProjectionParam(final AnnotationSet annotations, final Class<?> paramType,
      final Parameter.ParamType projectionType)
  {
    if (!paramType.equals(MaskTree.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + ProjectionParam.class.getSimpleName() +
          ", @" + Projection.class.getSimpleName() +
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

  private static Parameter<?> buildPathKeysParam(final AnnotationSet annotations,
                                                 final Class<?> paramType,
                                                 final Class<?> paramAnnotationType)
  {
    if (!paramType.equals(PathKeys.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + PathKeysParam.class.getSimpleName() + " or @" + Keys.class.getSimpleName() +
              " parameter annotation must be of type " +  PathKeys.class.getName());
    }

    Optional optional = annotations.get(Optional.class);
    Parameter.ParamType parameter = null;
    if(paramAnnotationType.equals(Keys.class))
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

  private static Parameter<?> buildPagingContextParam(final AnnotationSet annotations,
                                                      final Class<?> paramType,
                                                      final Class<?> paramAnnotationType)
  {
    if (!paramType.equals(PagingContext.class))
    {
      throw new ResourceConfigException("Incorrect data type for param: @" + PagingContextParam.class.getSimpleName() + " or @" + Context.class.getSimpleName() +
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
    else if (paramAnnotationType.equals(Context.class))
    {
      Context contextParam = annotations.get(Context.class);
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

  private static Parameter<?> buildAssocKeyParam(final ResourceModel model,
                                                 final Method method,
                                                 final AnnotationSet annotations,
                                                 final Class<?> paramType,
                                                 final Class<?> paramAnnotationType)
  {
    Parameter.ParamType parameter = null;
    String assocKeyParamValue = null;
    Class<? extends TyperefInfo> typerefInfoClass = null;
    if(paramAnnotationType.equals(AssocKey.class))
    {
      parameter = Parameter.ParamType.KEY;
      assocKeyParamValue = annotations.get(AssocKey.class).value();
      typerefInfoClass = annotations.get(AssocKey.class).typeref();
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
      Parameter<?> param =
          new Parameter(assocKeyParamValue,
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
      Parameter param =
          new Parameter(paramName,
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
      DataSchema itemSchema = DataTemplateUtil.getSchema(type.getComponentType());
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
      Parameter<?> param =
          new Parameter(queryParam.value(),
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

  private static String checkTyperefSchema(final Class<?> type, final DataSchema dataSchema)
  {
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
          TyperefUtils.getJavaClassNameFromSchema(typerefSchema);

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
    String coercerClassName = TyperefUtils.getCoercerClassFromSchema(schema);
    String javaClassNameFromSchema = TyperefUtils.getJavaClassNameFromSchema(schema);

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

  private static void addFinderResourceMethod(final ResourceModel model, final Method method)
  {
    Finder finderAnno = method.getAnnotation(Finder.class);
    if (finderAnno == null)
    {
      return;
    }

    String queryType = finderAnno.value();

    List<Parameter<?>> queryParameters =
        getParameters(model, method, ResourceMethod.FINDER);

    if (queryType != null)
    {
      Class<? extends RecordTemplate> metadataType = null;
      final Class<?> returnClass = getLogicalReturnClass(method);
      if (CollectionResult.class.isAssignableFrom(returnClass))
      {
        final List<Class<?>> typeArguments = ReflectionUtils.getTypeArguments(CollectionResult.class, returnClass.asSubclass(CollectionResult.class));
        final Class<?> metadataClass;
        if (typeArguments == null || typeArguments.get(1) == null)
        {
          // the return type may leave metadata type as parameterized and specify in runtime
          metadataClass = ((Class<?>) ((ParameterizedType) getLogicalReturnType(method)).getActualTypeArguments()[1]);
        }
        else
        {
          metadataClass = typeArguments.get(1);
        }

        if (!metadataClass.equals(NoMetadata.class))
        {
          metadataType = metadataClass.asSubclass(RecordTemplate.class);
        }
      }

      DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations());
      addDeprecatedAnnotation(annotationsMap, method);

      ResourceMethodDescriptor finderMethodDescriptor =
          ResourceMethodDescriptor.createForFinder(method,
                                                   queryParameters,
                                                   queryType,
                                                   metadataType,
                                                   getInterfaceType(method),
                                                   annotationsMap);
      validateFinderMethod(finderMethodDescriptor, model);

      if (!Modifier.isPublic(method.getModifiers()))
      {
        throw new ResourceConfigException(String.format("Resource '%s' contains non-public finder method '%s'.",
                                                        model.getName(),
                                                        method.getName()));
      }

      model.addResourceMethodDescriptor(finderMethodDescriptor);
    }
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
      model.addResourceMethodDescriptor(ResourceMethodDescriptor.createForRestful(resourceMethod,
                                                                                  method,
                                                                                  parameters,
                                                                                  getInterfaceType(method),
                                                                                  annotationsMap));
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

        DataMap annotationsMap = ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations());
        addDeprecatedAnnotation(annotationsMap, method);

        List<Parameter<?>> parameters = getParameters(model, method, resourceMethod);
        model.addResourceMethodDescriptor(ResourceMethodDescriptor.createForRestful(resourceMethod,
                                                                                    method,
                                                                                    parameters,
                                                                                    getInterfaceType(method),
                                                                                    annotationsMap));
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

    model.addResourceMethodDescriptor(ResourceMethodDescriptor.createForAction(method,
                                                                               parameters,
                                                                               actionName,
                                                                               getActionResourceLevel(actionAnno, model),
                                                                               returnFieldDef,
                                                                               actionReturnRecordDataSchema,
                                                                               recordDataSchema,
                                                                               getInterfaceType(method),
                                                                               annotationsMap));

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

  private static void validateFinderMethod(final ResourceMethodDescriptor finderMethodDescriptor,
                                           final ResourceModel resourceModel)
  {
    Method method = finderMethodDescriptor.getMethod();
    Class<?> valueClass = resourceModel.getValueClass();

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
        || !resourceModel.getValueClass().equals(elementType))
    {
      throw new ResourceConfigException("@Finder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName()
          + "' has an invalid return type. Expected " + collectionClassName + "<"
          + valueClass.getName() + ">, but found " + collectionClassName + "<"
          + elementType + '>');
    }

    ResourceMethodDescriptor existingFinder =
        resourceModel.findNamedMethod(finderMethodDescriptor.getFinderName());
    if (existingFinder != null)
    {
      throw new ResourceConfigException("Found duplicate @Finder method named '"
          + finderMethodDescriptor.getFinderName() + "' on class '"
          + resourceModel.getResourceClass().getName() + '\'');
    }

    // query parameters are checked in getQueryParameters method
  }

  private static ResourceModel processActions(final Class<?> actionResourceClass)
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
      throw new ResourceConfigException(String.format("%s has both callback and return value",
                                                      method));
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
}

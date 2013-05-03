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


import java.lang.annotation.Annotation;
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

import com.linkedin.common.callback.Callback;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateRuntimeException;
import com.linkedin.data.template.TyperefInfo;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.ReflectionUtils;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.AssocKey;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.ParSeqContext;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestAnnotations;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiCollectionCompoundKey;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.ComplexKeyResource;
import com.linkedin.restli.server.resources.KeyValueResource;

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
    Set<ResourceModel> existingResourceModels = new HashSet<ResourceModel>();

    if ((resourceClass.isAnnotationPresent(RestLiCollection.class) ||
         resourceClass.isAnnotationPresent(RestLiAssociation.class) ||
         resourceClass.isAnnotationPresent(RestLiCollectionCompoundKey.class)))
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
      model = processCollection(clazz, existingResourceModels);
    }
    else if (resourceClass.isAnnotationPresent(RestLiActions.class))
    {
      model = processActions(resourceClass);
    }
    else
    {
      throw new ResourceConfigException("Class '" + resourceClass.getName()
          + "' must be annotated with a valid @RestLi... annotation");
    }

    model.setCustomAnnotation(ResourceModelAnnotation.getAnnotationsMap(resourceClass.getAnnotations()));
    return model;
  }

  private static ResourceModel processCollection(final Class<? extends KeyValueResource<?, ?>> collectionResourceClass,
                                                 final Set<ResourceModel> knownResourceModels)
  {
    Class<?> keyClass;
    Class<? extends RecordTemplate> keyKeyClass = null;
    Class<? extends RecordTemplate> keyParamsClass = null;
    Class<? extends RecordTemplate> valueClass;
    // If ComplexKeyResource, the parameters are Key type K, Params type P and Resource
    // type V and the resource key type is ComplexResourceKey<K,P>
    if (ComplexKeyResource.class.isAssignableFrom(collectionResourceClass))
    {
      @SuppressWarnings("unchecked")
      List<Class<?>> kvParams =
          ReflectionUtils.getTypeArguments(ComplexKeyResource.class,
                                           (Class<? extends ComplexKeyResource<?, ?, ?>>) collectionResourceClass);
      keyClass = ComplexResourceKey.class;
      keyKeyClass = kvParams.get(0).asSubclass(RecordTemplate.class);
      keyParamsClass = kvParams.get(1).asSubclass(RecordTemplate.class);
      valueClass = kvParams.get(2).asSubclass(RecordTemplate.class);
    }
    // Otherwise, it's a KeyValueResource, whose parameters are resource key and resource
    // value
    else
    {
      List<Class<?>> kvParams =
          ReflectionUtils.getTypeArguments(KeyValueResource.class,
                                           collectionResourceClass);
      keyClass = kvParams.get(0);
      valueClass = kvParams.get(1).asSubclass(RecordTemplate.class);
    }

    ResourceType resourceType = getResourceType(collectionResourceClass);

    RestLiAnnotationData annotationData;
    if (collectionResourceClass.isAnnotationPresent(RestLiCollection.class))
    {
      annotationData =
          new RestLiAnnotationData(collectionResourceClass.getAnnotation(RestLiCollection.class));
    }
    else if (collectionResourceClass.isAnnotationPresent(RestLiCollectionCompoundKey.class))
    {
      annotationData =
          new RestLiAnnotationData(collectionResourceClass.getAnnotation(RestLiCollectionCompoundKey.class));
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
    addCollectionResourceMethods(collectionResourceClass, collectionModel);
    knownResourceModels.add(collectionModel);

    log.info("Processed collection resource '" + collectionResourceClass.getName() + "'");

    return collectionModel;
  }

  private static ResourceType getResourceType(final Class<?> resourceClass)
  {
    RestLiCollection collAnno = resourceClass.getAnnotation(RestLiCollection.class);
    RestLiCollectionCompoundKey collCKAnno =
        resourceClass.getAnnotation(RestLiCollectionCompoundKey.class);
    RestLiAssociation assocAnno = resourceClass.getAnnotation(RestLiAssociation.class);

    if (resourceClass.isAnnotationPresent(RestLiActions.class))
    {
      throw new ResourceConfigException("Resource class '" + resourceClass.getName()
                                            + "' cannot have both @RestLiCollection and @RestLiActions annotations.");
    }

    int annoCount = 0;
    annoCount += collAnno != null ? 1 : 0;
    annoCount += collCKAnno != null ? 1 : 0;
    annoCount += assocAnno != null ? 1 : 0;

    if (annoCount > 1)
    {
      throw new ResourceConfigException("Class '" + resourceClass.getName()
          + "' is annotated " + "with too many RestLi annotations");
    }
    else if (collAnno != null || collCKAnno != null)
    {
      return ResourceType.COLLECTION;
    }
    else if (assocAnno != null)
    {
      return ResourceType.ASSOCIATION;
    }
    else
    {
      throw new ResourceConfigException("Class '" + resourceClass.getName()
          + "' should be annotated " + "with '" + RestLiAssociation.class.getName() + "'"
          + " or '" + RestLiCollection.class.getName() + "'" + " or '"
          + RestLiCollectionCompoundKey.class.getName() + "'");

    }
  }

  /**
   * Keys, values, patch requests, and batch requests have fixed positions.
   */
  private static Parameter getPositionalParameter(final ResourceModel model,
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
          @SuppressWarnings("unchecked")
          Parameter p =
              new Parameter("",
                            PatchRequest.class,
                            null,
                            false,
                            null,
                            Parameter.ParamType.POST,
                            false,
                            annotations);
          return p;
        }
        break;
      case BATCH_GET:
        if (idx == 0)
        {
          @SuppressWarnings("unchecked")
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
          @SuppressWarnings("unchecked")
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
          @SuppressWarnings("unchecked")
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
          @SuppressWarnings("unchecked")
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
          @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
  private static Parameter makeValueParam(final ResourceModel model)
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

  @SuppressWarnings("unchecked")
  private static Parameter makeKeyParam(final ResourceModel model)
  {
    return new Parameter(model.getKeyName(),
                         model.getKeyClass(),
                         model.getPrimaryKey().getDataSchema(),
                         false,
                         null,
                         Parameter.ParamType.KEY,
                         false,
                         AnnotationSet.EMPTY);
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
          param = buildAssocKeyParam(method, paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains(Context.class))
        {
          param = buildContextParam(paramAnnotations, paramType);
        }
        else if (paramAnnotations.contains(CallbackParam.class))
        {
          param = buildCallbackParam(method, methodType, idx, paramType, paramAnnotations);
        }
        else if (paramAnnotations.contains(ParSeqContext.class))
        {
          param = buildParSeqContextParam(method, methodType, idx, paramType, paramAnnotations);
        }
        else
        {
          throw new ResourceConfigException(buildMethodMessage(method)
              + "' must annotate each parameter with @QueryParam, @ActionParam, @AssocKey, @Context, @CallbackParam or @ParSeqContext");
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

  @SuppressWarnings("unchecked")
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
                                                                                final AnnotationSet annotations)
  {
    if (!com.linkedin.parseq.Context.class.equals(paramType))
    {
      throw new ResourceConfigException("@ParSeqContext must be com.linkedin.parseq.Context");
    }
    if (getInterfaceType(method) != InterfaceType.PROMISE)
    {
      throw new ResourceConfigException("Cannot have ParSeq context on non-promise method");
    }
    return new Parameter<com.linkedin.parseq.Context>("",
                                                      com.linkedin.parseq.Context.class,
                                                      null,
                                                      false,
                                                      null,
                                                      Parameter.ParamType.PARSEQ_CONTEXT,
                                                      false,
                                                      annotations);
  }

  @SuppressWarnings("unchecked")
  private static void validateParameter(final Method method,
                                        final ResourceMethod methodType,
                                        final Set<String> paramNames,
                                        final AnnotationSet annotations,
                                        final Parameter param,
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

    if (methodType == ResourceMethod.ACTION
        && param.getParamType() == Parameter.ParamType.POST
            && !(checkParameterType(param.getType(),
                                    RestModelConstants.VALID_ACTION_PARAMETER_TYPES) || param.getDataSchema() instanceof TyperefDataSchema))
    {
      throw new ResourceConfigException("Parameter '" + paramName + "' on "
          + buildMethodMessage(method) + " is not a valid type (" + param.getType() + ')');
    }

    if (methodType != ResourceMethod.ACTION
        && param.getParamType() == Parameter.ParamType.QUERY
        && !(checkParameterType(param.getType(),
                                RestModelConstants.VALID_QUERY_PARAMETER_TYPES) || param.getDataSchema() instanceof TyperefDataSchema))
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

    if (annotations.count(QueryParam.class,
                          ActionParam.class,
                          AssocKey.class,
                          Context.class,
                          CallbackParam.class,
                          ParSeqContext.class) > 1)
    {
      throw new ResourceConfigException(buildMethodMessage(method)
          + "' must declare only one of @QueryParam, @ActionParam, @AssocKey, @Context, or @CallbackParam");
    }
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

  @SuppressWarnings("unchecked")
  private static Parameter buildContextParam(final AnnotationSet annotations,
                                             final Class<?> paramType)
  {
    if (!paramType.equals(PagingContext.class))
    {
      throw new ResourceConfigException("Context must be PagingContext");
    }
    Parameter param;
    Context context = annotations.get(Context.class);
    Optional optional = annotations.get(Optional.class);
    PagingContext defaultContext =
        new PagingContext(context.defaultStart(), context.defaultCount(), false, false);
    param =
        new Parameter("",
                      paramType,
                      null,
                      optional != null,
                      defaultContext,
                      Parameter.ParamType.CONTEXT,
                      false,
                      annotations);
    return param;
  }

  @SuppressWarnings("unchecked")
  private static Parameter buildAssocKeyParam(final Method method,
                                              final AnnotationSet annotations,
                                              final Class<?> paramType)
  {
    Parameter param;
    AssocKey assocKey = annotations.get(AssocKey.class);
    Optional optional = annotations.get(Optional.class);
    Class<? extends TyperefInfo> typerefInfoClass = assocKey.typeref();
    try
    {
      param =
          new Parameter(assocKey.value(),
                        paramType,
                        getDataSchema(paramType, getSchemaFromTyperefInfo(typerefInfoClass)),
                        optional != null,
                        getDefaultValueData(optional),
                        Parameter.ParamType.KEY,
                        true,
                        annotations);
    }
    catch (TemplateRuntimeException e)
    {
      throw new ResourceConfigException("DataSchema for assocKey '" + assocKey.value() + "' of type " + paramType.getSimpleName() + " on "
                                                + buildMethodMessage(method) + "cannot be found; type is invalid or requires typeref", e);
    }
    catch (Exception e)
    {
      throw new ResourceConfigException("Typeref for assocKey '" + assocKey.value() + "' on "
                                                + buildMethodMessage(method) + " cannot be instantiated, " + e.getMessage(), e);
    }

    return param;
  }

  @SuppressWarnings("unchecked")
  private static Parameter buildActionParam(final Method method,
                                            final AnnotationSet annotations,
                                            final Class<?> paramType)
  {
    Parameter param;
    ActionParam actionParam = annotations.get(ActionParam.class);
    Optional optional = annotations.get(Optional.class);
    String paramName = actionParam.value();
    Class<? extends TyperefInfo> typerefInfoClass = actionParam.typeref();
    try
    {
      param =
          new Parameter(paramName,
                        paramType,
                        getDataSchema(paramType, getSchemaFromTyperefInfo(typerefInfoClass)),
                        optional != null,
                        getDefaultValueData(optional),
                        Parameter.ParamType.POST,
                        true,
                        annotations);
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
    return param;
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
      return typerefDataSchema;
    }
    else if (RestModelConstants.CLASSES_WITHOUT_SCHEMAS.contains(type))
    {
      return null;
    }
    else if (type.isArray())
    {
      return DataTemplateUtil.getSchema(type.getComponentType());
    }
    return DataTemplateUtil.getSchema(type);
  }

  @SuppressWarnings("unchecked")
  private static Parameter<?> buildQueryParam(final Method method,
                                              final AnnotationSet annotations,
                                              final Class<?> paramType)
  {
    Parameter<?> param;
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
      param =
          new Parameter(queryParam.value(),
                        paramType,
                        getDataSchema(paramType, getSchemaFromTyperefInfo(typerefInfoClass)),
                        optional != null,
                        getDefaultValueData(optional),
                        Parameter.ParamType.QUERY,
                        true,
                        annotations);
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
    return param;
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
          ArgumentUtils.getJavaClassNameFromSchema(typerefSchema);

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
    String coercerClassName = ArgumentUtils.getCoercerClassFromSchema(schema);
    String javaClassNameFromSchema = ArgumentUtils.getJavaClassNameFromSchema(schema);

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

  private static void addCollectionResourceMethods(final Class<?> resourceClass,
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

      checkActionResourceMethod(model, method);
      checkFinderResourceMethod(model, method);
      checkTemplateResourceMethods(resourceClass, model, method);
      checkCrudResourceMethods(resourceClass, model, method);
    }

    validateResourceModel(model);
  }

  private static void validateResourceModel(final ResourceModel model)
  {
    validateAssociation(model);
    validateCrudMethods(model);
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

  private static void checkFinderResourceMethod(final ResourceModel model,
                                                final Method method)
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
      if (getLogicalReturnClass(method).equals(CollectionResult.class))
      {
        metadataType =
            ((Class<?>) ((ParameterizedType) getLogicalReturnType(method)).getActualTypeArguments()[1]).asSubclass(RecordTemplate.class);
      }

      ResourceMethodDescriptor finderMethodDescriptor =
          ResourceMethodDescriptor.createForFinder(method,
                                                   queryParameters,
                                                   queryType,
                                                   metadataType,
                                                   getInterfaceType(method),
                                                   ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations()));
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
   * with RestMethod. Those are handled in checkCrudResourceMethods()
   */
  private static void checkTemplateResourceMethods(final Class<?> resourceClass,
                                                   final ResourceModel model,
                                                   final Method method)
  {
    // Check if the resource class is derived from one of the resource template classes.
    if (!isResourceTemplateClass(resourceClass))
    {
      return;
    }

    // If the method is annotated with RestMethod - ignore - will be handled in
    // checkCrudResourceMethods
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

      List<Parameter<?>> parameters = getParameters(model, method, resourceMethod);
      model.addResourceMethodDescriptor(ResourceMethodDescriptor.createForRestful(resourceMethod,
                                                                                  method,
                                                                                  parameters,
                                                                                  getInterfaceType(method),
                                                                                  ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations())));
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
  private static void checkCrudResourceMethods(final Class<?> resourceClass,
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

        List<Parameter<?>> parameters = getParameters(model, method, resourceMethod);
        model.addResourceMethodDescriptor(ResourceMethodDescriptor.createForRestful(resourceMethod,
                                                                                    method,
                                                                                    parameters,
                                                                                    getInterfaceType(method),
                                                                                    ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations())));
      }
    }
  }

  private static void checkActionResourceMethod(final ResourceModel model,
                                                final Method method)
  {
    Action actionAnno = method.getAnnotation(Action.class);
    if (actionAnno == null)
    {
      return;
    }

    String actionName = actionAnno.name();
    List<Parameter<?>> parameters = getParameters(model, method, ResourceMethod.ACTION);
    validateActionReturnType(method);
    final Type returnType = getLogicalReturnType(method);
    ResourceMethodDescriptor existingMethodDescriptor =
        model.findActionMethod(actionName, actionAnno.resourceLevel());
    if (existingMethodDescriptor != null)
    {
      throw new ResourceConfigException("Found duplicate @Action method named '"
          + actionName + "' on class '" + model.getResourceClass().getName() + '\'');

    }

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

    final String checkTyperefMessage = checkTyperefSchema(returnClass, returnTyperefSchema);
    if (checkTyperefMessage != null)
    {
      throw new ResourceConfigException("Typeref @Action method named '" + actionName
          + "' on class '" + model.getResourceClass().getName() + "', "
          + checkTyperefMessage);
    }

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
      @SuppressWarnings("unchecked")
      FieldDef<?> nonVoidFieldDef = new FieldDef(ActionResponse.VALUE_NAME, returnClass, getDataSchema(returnClass, returnTyperefSchema));
      returnFieldDef = nonVoidFieldDef;
      actionReturnRecordDataSchema = DynamicRecordMetadata.buildSchema(ActionResponse.class.getName(), Collections.singleton((returnFieldDef)));
    }
    else
    {
      returnFieldDef = null;
      actionReturnRecordDataSchema = DynamicRecordMetadata.buildSchema(ActionResponse.class.getName(), Collections.<FieldDef<?>>emptyList());
    }


    model.addResourceMethodDescriptor(ResourceMethodDescriptor.createForAction(method,
                                                                               parameters,
                                                                               actionName,
                                                                               actionAnno.resourceLevel(),
                                                                               returnFieldDef,
                                                                               actionReturnRecordDataSchema,
                                                                               recordDataSchema,
                                                                               getInterfaceType(method),
                                                                               ResourceModelAnnotation.getAnnotationsMap(method.getAnnotations())));

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

  private static void validateActionReturnType(final Method method)
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

  private static void validateFinderMethod(final ResourceMethodDescriptor finderMethodDescriptor,
                                           final ResourceModel resourceModel)
  {
    Method method = finderMethodDescriptor.getMethod();
    Class<?> valueClass = resourceModel.getValueClass();

    Class<?> returnType, elementType;
    try
    {
      ParameterizedType collectionType = (ParameterizedType) getLogicalReturnType(method);
      returnType = (Class<?>) collectionType.getRawType();
      elementType = (Class<?>) collectionType.getActualTypeArguments()[0];
    }
    catch (ClassCastException e)
    {
      throw new ResourceConfigException("@Finder method '" + method.getName()
          + "' on class '" + resourceModel.getResourceClass().getName()
          + "' has an invalid return or a data template type");
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

      checkActionResourceMethod(actionResourceModel, method);
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
      Class<?> c = typeToClass(types[i]);
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
    Class<?> c = typeToClass(getLogicalReturnType(method));
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
   * Convert the given Type object to a Class object if possible and return null
   * otherwise.
   */
  private static Class<?> typeToClass(final Type t)
  {
    // TODO is this dependent on implementation detail?
    // Type can be Class, GenericArrayType, ParameterizedType, TypeVariable, or
    // WildcardType
    if (t instanceof Class)
    {
      return (Class<?>) t;
    }
    else if (t instanceof ParameterizedType)
    {
      ParameterizedType param = (ParameterizedType) t;
      return (Class<?>) param.getRawType();
    }
    return null;
  }
}

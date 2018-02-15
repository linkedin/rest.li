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


import com.linkedin.data.DataMap;
import com.linkedin.data.codec.DataCodec;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.AbstractSchemaEncoder;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.HasTyperefInfo;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.TyperefInfo;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AlternativeKeySchema;
import com.linkedin.restli.restspec.AlternativeKeySchemaArray;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.AssocKeySchemaArray;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.CustomAnnotationContentSchemaMap;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.FinderSchemaArray;
import com.linkedin.restli.restspec.IdentifierSchema;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.ResourceSchemaArray;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import com.linkedin.restli.restspec.SimpleSchema;
import com.linkedin.restli.server.AlternativeKey;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.ResourceLevel;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;


/**
 * Encodes a ResourceModel (runtime-reflection oriented class) into the JSON-serializable
 * {@link ResourceSchema}. Accepts a {@link DocsProvider} plugin to incorporate documentation
 * from a JVM language such as JavaDoc and Scaladoc.
 *
 * @author jwalker, dellamag
 */
public class ResourceModelEncoder
{
  public static final String DEPRECATED_ANNOTATION_NAME = "deprecated";
  public static final String DEPRECATED_ANNOTATION_DOC_FIELD = "doc";
  public static final String COMPOUND_KEY_TYPE_NAME = "CompoundKey";

  private final DataCodec codec = new JacksonDataCodec();

  /**
   * Provides documentation strings from a JVM language to be incorporated into ResourceModels.
   */
  public interface DocsProvider
  {
    /**
     * Gets the file extensions this doc provider supports.  It must ignore any source files registered with it that
     * are not in this set.
     *
     * @return the supported files extensions
     */
    Set<String> supportedFileExtensions();

    /**
     * Registers the source files with the doc provider.  The doc provider should perform any initialization required to
     * handle subsequent requests for class, method and param docs from these source files.
     *
     * @param filenames provides the source file names to register.
     */
    void registerSourceFiles(Collection<String> filenames);

    /**
     * @param resourceClass resource class
     * @return class documentation, or null if no documentation is available
     */
    String getClassDoc(Class<?> resourceClass);

    String getClassDeprecatedTag(Class<?> resourceClass);

    /**
     * @param method resource {@link Method}
     * @return method documentation, or null if no documentation is available
     */
    String getMethodDoc(Method method);

    String getMethodDeprecatedTag(Method method);

    /**
     * @param method resource {@link Method}
     * @param name method param name
     * @return method param documentation, or null if no documentation is available
     */
    String getParamDoc(Method method, String name);

    /**
     * Returns the documentation for the @return tag
     *
     * @param method resource {@link Method}
     * @return Description of the @return tag
     */
    String getReturnDoc(Method method);
  }

  public static class NullDocsProvider implements DocsProvider
  {
    @Override
    public void registerSourceFiles(Collection<String> filenames)
    {

    }

    @Override
    public Set<String> supportedFileExtensions()
    {
      return Collections.emptySet();
    }

    @Override
    public String getClassDoc(final Class<?> resourceClass)
    {
      return null;
    }

    @Override
    public String getClassDeprecatedTag(Class<?> resourceClass)
    {
      return null;
    }

    @Override
    public String getMethodDoc(final Method method)
    {
      return null;
    }

    @Override
    public String getMethodDeprecatedTag(Method method)
    {
      return null;
    }

    @Override
    public String getParamDoc(final Method method, final String name)
    {
      return null;
    }

    @Override
    public String getReturnDoc(final Method method)
    {
      return null;
    }
  }

  private final DocsProvider _docsProvider;

  /**
   * @param docsProvider {@link DocsProvider} to pull in javadoc comments.
   */
  public ResourceModelEncoder(final DocsProvider docsProvider)
  {
    _docsProvider = docsProvider;
  }

  /**
   * @param resourceModel {@link ResourceModel} to build the schema for
   * @return {@link ResourceSchema} for the provided resource model
   */
  public ResourceSchema buildResourceSchema(final ResourceModel resourceModel)
  {
    ResourceSchema rootNode = new ResourceSchema();

    // Set the entityType only when it is a UNSTRUCTURED_DATA base resource to avoid
    // modifying all existing resources, which by default are STRUCTURED_DATA base.
    if (ResourceEntityType.UNSTRUCTURED_DATA == resourceModel.getResourceEntityType())
    {
      rootNode.setEntityType(ResourceEntityType.UNSTRUCTURED_DATA);
    }

    switch (resourceModel.getResourceType())
    {
      case ACTIONS:
        appendActionsModel(rootNode, resourceModel);
        break;
      case SIMPLE:
        appendSimple(rootNode, resourceModel);
        break;
      default:
        appendCollection(rootNode, resourceModel);
        break;
    }

    final DataMap customAnnotation = resourceModel.getCustomAnnotationData();
    if (!customAnnotation.isEmpty())
    {
      rootNode.setAnnotations(new CustomAnnotationContentSchemaMap(customAnnotation));
    }

    return rootNode;
  }

  public void appendAlternativeKeys(CollectionSchema rootNode, ResourceModel resourceModel)
  {
    Map<String,AlternativeKey<?, ?>> alternativeKeys = resourceModel.getAlternativeKeys();
    if (!alternativeKeys.isEmpty())
    {
      AlternativeKeySchemaArray altKeyArray = new AlternativeKeySchemaArray();
      for (Map.Entry<String, AlternativeKey<?, ?>> entry : alternativeKeys.entrySet())
      {
        AlternativeKeySchema altKeySchema = new AlternativeKeySchema();
        altKeySchema.setName(entry.getKey());
        altKeySchema.setType(buildDataSchemaType(entry.getValue().getType()));
        altKeySchema.setKeyCoercer(entry.getValue().getKeyCoercer().getClass().getCanonicalName());
        altKeyArray.add(altKeySchema);
      }
      rootNode.setAlternativeKeys(altKeyArray);
    }
  }

  /**
   * Checks if a matching .restspec.json file exists in the classpath for the given {@link ResourceModel}.
   * If one is found it is loaded.  If not, one is built from the {@link ResourceModel}.
   *
   * The .restspec.json is preferred because it contains the exact idl that was generated for the resource
   * and also includees includes javadoc from the server class in the restspec.json.
   *
   * @param resourceModel provides the name and namespace of the schema to load or build
   * @return the {@link ResourceSchema} for the given {@link ResourceModel}
   */
  public ResourceSchema loadOrBuildResourceSchema(final ResourceModel resourceModel)
  {
    StringBuilder resourceFilePath = new StringBuilder();
    if (resourceModel.getNamespace() != null)
    {
      resourceFilePath.append(resourceModel.getNamespace());
      resourceFilePath.append(".");
    }
    resourceFilePath.append(resourceModel.getName());
    resourceFilePath.append(RestConstants.RESOURCE_MODEL_FILENAME_EXTENSION);

    try
    {
      InputStream stream = getResourceStream(resourceFilePath.toString(),
        this.getClass().getClassLoader(),
        Thread.currentThread().getContextClassLoader());
      if(stream == null)
      {
        // restspec.json file not found, building one instead
        return buildResourceSchema(resourceModel);
      }
      else
      {
        DataMap resourceSchemaDataMap = codec.bytesToMap(IOUtils.toByteArray(stream));
        return new ResourceSchema(resourceSchemaDataMap);
      }
    }
    catch (IOException e)
    {
      throw new RuntimeException("Failed to read " + resourceFilePath.toString() + " from classpath.", e);
    }
  }

  // Get resource stream via a list of classloader.  This method will traverse via each class loader and
  // return as soon as a stream is found
  private static InputStream getResourceStream(String resourceName, ClassLoader... classLoaders)
  {
    for(ClassLoader classLoader: classLoaders)
    {
      InputStream res = classLoader.getResourceAsStream(resourceName);
      if (res != null)
      {
        return res;
      }
    }
    return null;
  }

  /*package*/ static String buildDataSchemaType(final Class<?> type)
  {
    final DataSchema schema = DataTemplateUtil.getSchema(type);
    return buildDataSchemaType(schema);
  }

  /*package*/ static String buildDataSchemaType(DataSchema schema)
  {
    if (schema instanceof PrimitiveDataSchema || schema instanceof NamedDataSchema)
    {
      return schema.getUnionMemberKey();
    }

    JsonBuilder builder = null;
    try
    {
      builder = new JsonBuilder(JsonBuilder.Pretty.SPACES);
      final SchemaToJsonEncoder encoder = new SchemaToJsonEncoder(builder, AbstractSchemaEncoder.TypeReferenceFormat.MINIMIZE);
      encoder.encode(schema);
      return builder.result();
    }
    catch (IOException e)
    {
      throw new RestLiInternalException("could not encode schema for '" + schema.toString() +  "'", e);
    }
    finally
    {
      if (builder != null)
      {
        builder.closeQuietly();
      }
    }
  }

  private static String buildDataSchemaType(final Class<?> type, final DataSchema dataSchema)
  {
    final DataSchema schemaToEncode;
    if (dataSchema instanceof TyperefDataSchema)
    {
      return ((TyperefDataSchema)dataSchema).getFullName();
    }
    else if (dataSchema instanceof PrimitiveDataSchema || dataSchema instanceof NamedDataSchema)
    {
      return dataSchema.getUnionMemberKey();
    }
    else if (dataSchema instanceof UnionDataSchema && HasTyperefInfo.class.isAssignableFrom(type))
    {
      final TyperefInfo unionRef = DataTemplateUtil.getTyperefInfo(type.asSubclass(DataTemplate.class));
      schemaToEncode = unionRef.getSchema();
    }
    else
    {
      schemaToEncode = dataSchema;
    }

    JsonBuilder builder = null;
    try
    {
      builder = new JsonBuilder(JsonBuilder.Pretty.SPACES);
      final SchemaToJsonEncoder encoder = new SchemaToJsonEncoder(builder, AbstractSchemaEncoder.TypeReferenceFormat.MINIMIZE);
      encoder.encode(schemaToEncode);
      return builder.result();
    }
    catch (IOException e)
    {
      throw new RestLiInternalException("could not encode schema for '" + type.getName() + "'", e);
    }
    finally
    {
      if (builder != null)
      {
        builder.closeQuietly();
      }
    }
  }

  public static String buildPath(final ResourceModel resourceModel)
  {
    StringBuilder sb = new StringBuilder();
    buildPathInternal(resourceModel, sb, false);
    return sb.toString();
  }

  private static String buildPathForEntity(final ResourceModel resourceModel)
  {
    StringBuilder sb = new StringBuilder();
    buildPathInternal(resourceModel, sb, true);
    return sb.toString();
  }

  private static void buildPathInternal(ResourceModel resourceModel,
                                        final StringBuilder sb,
                                        boolean addEntityElement)
  {
    do
    {
      if (addEntityElement)
      {
        if (resourceModel.getKeys().size() >= 1)
        {
          sb.insert(0, "/{" + resourceModel.getKeyName() + "}");
        }
      }
      sb.insert(0, "/" + resourceModel.getName());
      addEntityElement = true;
    }
    while ((resourceModel = resourceModel.getParentResourceModel()) != null);
  }

  private void appendCommon(final ResourceModel resourceModel,
                            final ResourceSchema resourceSchema)
  {
    resourceSchema.setName(resourceModel.getName());
    if (!resourceModel.getNamespace().isEmpty())
    {
      resourceSchema.setNamespace(resourceModel.getNamespace());
    }
    resourceSchema.setPath(buildPath(resourceModel));

    final Class<?> valueClass = resourceModel.getValueClass();
    if (valueClass != null)
    {
      resourceSchema.setSchema(DataTemplateUtil.getSchema(valueClass).getUnionMemberKey());
    }

    final Class<?> resourceClass = resourceModel.getResourceClass();
    final String doc = _docsProvider.getClassDoc(resourceClass);
    final StringBuilder docBuilder = new StringBuilder();
    if (doc != null)
    {
      docBuilder.append(doc).append("\n\n");
    }
    docBuilder.append("generated from: ").append(resourceClass.getCanonicalName());


    final String deprecatedDoc = _docsProvider.getClassDeprecatedTag(resourceClass);
    if(deprecatedDoc != null)
    {
      DataMap customAnnotationData = resourceModel.getCustomAnnotationData();
      if(customAnnotationData == null)
      {
        customAnnotationData = new DataMap();
        resourceModel.setCustomAnnotation(customAnnotationData);
      }
      customAnnotationData.put(DEPRECATED_ANNOTATION_NAME, deprecateDocToAnnotationMap(deprecatedDoc));
    }

    resourceSchema.setDoc(docBuilder.toString());
  }

  private void appendCollection(final ResourceSchema resourceSchema,
                                final ResourceModel collectionModel)
  {
    appendCommon(collectionModel, resourceSchema);

    CollectionSchema collectionSchema = new CollectionSchema();
    //HACK: AssociationSchema and CollectionSchema share many common elements, but have no inheritance
    //relationship.  Here, we construct them both as facades on the same DataMap, which allows
    //us to pass strongly typed CollectionSchema objects around, even when we're dealing with
    //an association.
    AssociationSchema associationSchema = new AssociationSchema(collectionSchema.data());

    if (collectionModel.getKeys().size() == 1)
    {
      appendIdentifierNode(collectionSchema, collectionModel);
    }
    else
    {
      appendKeys(associationSchema, collectionModel);
    }

    appendAlternativeKeys(collectionSchema, collectionModel);

    appendSupportsNodeToCollectionSchema(collectionSchema, collectionModel);
    appendMethodsToCollectionSchema(collectionSchema, collectionModel);
    FinderSchemaArray finders = createFinders(collectionModel);
    if (finders.size() > 0)
    {
      collectionSchema.setFinders(finders);
    }
    ActionSchemaArray actions = createActions(collectionModel, ResourceLevel.COLLECTION);
    if (actions.size() > 0)
    {
      collectionSchema.setActions(actions);
    }
    appendEntityToCollectionSchema(collectionSchema, collectionModel);

    switch(collectionModel.getResourceType())
    {
      case COLLECTION:
        resourceSchema.setCollection(collectionSchema);
        break;
      case ASSOCIATION:
        resourceSchema.setAssociation(associationSchema);
        break;
      default:
        throw new IllegalArgumentException("unsupported resource type");
    }
  }

  private void appendActionsModel(final ResourceSchema resourceSchema,
                                  final ResourceModel resourceModel)
  {
    appendCommon(resourceModel, resourceSchema);
    ActionsSetSchema actionsNode = new ActionsSetSchema();
    ActionSchemaArray actions = createActions(resourceModel, ResourceLevel.COLLECTION);
    if (actions.size() > 0)
    {
      actionsNode.setActions(actions);
    }
    resourceSchema.setActionsSet(actionsNode);
  }

  private void appendSimple(ResourceSchema resourceSchema, ResourceModel resourceModel)
  {
    appendCommon(resourceModel, resourceSchema);

    SimpleSchema simpleSchema = new SimpleSchema();

    appendSupportsNodeToSimpleSchema(simpleSchema, resourceModel);
    appendMethodsToSimpleSchema(simpleSchema, resourceModel);

    ActionSchemaArray actions = createActions(resourceModel, ResourceLevel.ENTITY);
    if (actions.size() > 0)
    {
      simpleSchema.setActions(actions);
    }

    appendEntityToSimpleSchema(simpleSchema, resourceModel);

    resourceSchema.setSimple(simpleSchema);
  }

  private void appendEntityToCollectionSchema(final CollectionSchema collectionSchema,
                                              final ResourceModel resourceModel)
  {
    EntitySchema entityNode = buildEntitySchema(resourceModel);

    collectionSchema.setEntity(entityNode);
  }

  private void appendEntityToSimpleSchema(final SimpleSchema simpleSchema,
                                          final ResourceModel resourceModel)
  {
    EntitySchema entityNode = buildEntitySchema(resourceModel);

    simpleSchema.setEntity(entityNode);
  }

  private EntitySchema buildEntitySchema(ResourceModel resourceModel)
  {
    EntitySchema entityNode = new EntitySchema();
    entityNode.setPath(buildPathForEntity(resourceModel));

    if (resourceModel.getResourceLevel() == ResourceLevel.COLLECTION)
    {
      ActionSchemaArray actions = createActions(resourceModel, ResourceLevel.ENTITY);
      if (actions.size() > 0)
      {
        entityNode.setActions(actions);
      }
    }

    // subresources
    ResourceSchemaArray subresources = new ResourceSchemaArray();
    for (ResourceModel subResourceModel : resourceModel.getSubResources())
    {
      ResourceSchema subresource = new ResourceSchema();

      switch (subResourceModel.getResourceType())
      {
        case COLLECTION:
        case ASSOCIATION:
          appendCollection(subresource, subResourceModel);
          break;
        case SIMPLE:
          appendSimple(subresource, subResourceModel);
          break;
        default:
          break;
      }

      final DataMap customAnnotation = subResourceModel.getCustomAnnotationData();
      if (!customAnnotation.isEmpty())
      {
        subresource.setAnnotations(new CustomAnnotationContentSchemaMap(customAnnotation));
      }

      subresources.add(subresource);
    }

    if (subresources.size() > 0)
    {
      Collections.sort(subresources, new Comparator<ResourceSchema>()
      {
        @Override
        public int compare(ResourceSchema resourceSchema, ResourceSchema resourceSchema2)
        {
          return resourceSchema.getName().compareTo(resourceSchema2.getName());
        }
      });
      entityNode.setSubresources(subresources);
    }
    return entityNode;
  }

  private void appendKeys(final AssociationSchema associationSchema,
                          final ResourceModel collectionModel)
  {
    AssocKeySchemaArray assocKeySchemaArray = new AssocKeySchemaArray();
    List<Key> sortedKeys = new ArrayList<Key>(collectionModel.getKeys());
    Collections.sort(sortedKeys, new Comparator<Key>()
    {
      @Override
      public int compare(final Key o1, final Key o2)
      {
        return o1.getName().compareTo(o2.getName());
      }
    });

    for (Key key : sortedKeys)
    {
      AssocKeySchema assocKeySchema = new AssocKeySchema();
      assocKeySchema.setName(key.getName());
      assocKeySchema.setType(buildDataSchemaType(key.getType(), key.getDataSchema()));
      assocKeySchemaArray.add(assocKeySchema);
    }

    associationSchema.setAssocKeys(assocKeySchemaArray);

    associationSchema.setIdentifier(collectionModel.getKeyName());

  }


  private ActionSchemaArray createActions(final ResourceModel resourceModel,
                                          final ResourceLevel resourceLevel)
  {
    ActionSchemaArray actionsArray = new ActionSchemaArray();

    List<ResourceMethodDescriptor> resourceMethodDescriptors =
        resourceModel.getResourceMethodDescriptors();
    Collections.sort(resourceMethodDescriptors, new Comparator<ResourceMethodDescriptor>()
    {
      @Override
      public int compare(final ResourceMethodDescriptor o1, final ResourceMethodDescriptor o2)
      {
        if (o1.getType().equals(ResourceMethod.ACTION))
        {
          if (o2.getType().equals(ResourceMethod.ACTION))
          {
            return o1.getActionName().compareTo(o2.getActionName());
          }
          else
          {
            return 1;
          }
        }
        else if (o2.getType().equals(ResourceMethod.ACTION))
        {
          return -1;
        }
        else
        {
          return 0;
        }
      }
    });

    for (ResourceMethodDescriptor resourceMethodDescriptor : resourceMethodDescriptors)
    {
      if (ResourceMethod.ACTION.equals(resourceMethodDescriptor.getType()))
      {
        //do not apply entity-level actions at collection level or vice-versa
        if (resourceMethodDescriptor.getActionResourceLevel() != resourceLevel)
        {
          continue;
        }

        ActionSchema action = new ActionSchema();

        action.setName(resourceMethodDescriptor.getActionName());

        //We have to construct the method doc for the action which includes the action return type
        final String methodDoc = _docsProvider.getMethodDoc(resourceMethodDescriptor.getMethod());
        if (methodDoc != null)
        {
          final StringBuilder methodDocBuilder = new StringBuilder(methodDoc.trim());
          if (methodDocBuilder.length() > 0)
          {
            final String returnDoc = sanitizeDoc(_docsProvider.getReturnDoc(resourceMethodDescriptor.getMethod()));
            if (returnDoc != null && !returnDoc.isEmpty())
            {
              methodDocBuilder.append("\n");
              methodDocBuilder.append("Service Returns: ");
              //Capitalize the first character
              methodDocBuilder.append(returnDoc.substring(0, 1).toUpperCase());
              methodDocBuilder.append(returnDoc.substring(1));
            }
          }
          action.setDoc(methodDocBuilder.toString());
        }

        ParameterSchemaArray parameters = createParameters(resourceMethodDescriptor);
        if (parameters.size() > 0)
        {
          action.setParameters(parameters);
        }

        Class<?> returnType = resourceMethodDescriptor.getActionReturnType();
        if (returnType != Void.TYPE)
        {
          String returnTypeString =
              buildDataSchemaType(returnType,
                                  resourceMethodDescriptor.getActionReturnRecordDataSchema().getField(ActionResponse.VALUE_NAME).getType());
          action.setReturns(returnTypeString);
        }

        final DataMap customAnnotation = resourceMethodDescriptor.getCustomAnnotationData();
        String deprecatedDoc = _docsProvider.getMethodDeprecatedTag(resourceMethodDescriptor.getMethod());
        if(deprecatedDoc != null)
        {
          customAnnotation.put(DEPRECATED_ANNOTATION_NAME, deprecateDocToAnnotationMap(deprecatedDoc));
        }

        if (!customAnnotation.isEmpty())
        {
          action.setAnnotations(new CustomAnnotationContentSchemaMap(customAnnotation));
        }

        actionsArray.add(action);
      }
    }
    return actionsArray;
  }

  /**
   * This method takes a Javadoc tag description and sanitizes it to do the following:
   * 1. Remove all leading and trailing white space including \n and \t by calling trim().
   * 2. Remove all superfluous whitespace between the words. This includes tabs and white spaces.
   * 3. Preserves \n characters within the description itself.
   *
   * @param doc The text to sanitize
   * @return The sanitized text
   */
  static String sanitizeDoc(final String doc)
  {
    if (doc != null)
    {
      //Remove all unnecessary whitespace including tabs. Note we can't use \s because it will chew
      //up \n's which we need to preserve
      String returnComment = doc.trim().replaceAll("[ \\t]+", " ");
      //We should not allow a space to the right or left of a new line character
      returnComment = returnComment.replace("\n ", "\n");
      returnComment = returnComment.replace(" \n", "\n");
      return returnComment;
    }
    return null;
  }

  private DataMap deprecateDocToAnnotationMap(String deprecatedDoc)
  {
    deprecatedDoc = deprecatedDoc.trim();
    DataMap deprecatedAnnotation = new DataMap();
    if(!deprecatedDoc.isEmpty())
    {
      deprecatedAnnotation.put(DEPRECATED_ANNOTATION_DOC_FIELD, deprecatedDoc);
    }
    return deprecatedAnnotation;
  }

  static Comparator<ResourceMethodDescriptor> RESOURCE_METHOD_COMPARATOR = (ResourceMethodDescriptor o1, ResourceMethodDescriptor o2) ->
  {
    if (o1.getFinderName() == o2.getFinderName())
    {
      return 0;
    }

    if (o1.getFinderName() == null)
    {
      return -1;
    }
    else if (o2.getFinderName() == null)
    {
      return 1;
    }

    return o1.getFinderName().compareTo(o2.getFinderName());
  };

  private FinderSchemaArray createFinders(final ResourceModel resourceModel)
  {
    FinderSchemaArray findersArray = new FinderSchemaArray();

    List<ResourceMethodDescriptor> resourceMethodDescriptors =
        resourceModel.getResourceMethodDescriptors();
    Collections.sort(resourceMethodDescriptors, RESOURCE_METHOD_COMPARATOR);

    for (ResourceMethodDescriptor resourceMethodDescriptor : resourceMethodDescriptors)
    {
      if (ResourceMethod.FINDER.equals(resourceMethodDescriptor.getType()))
      {
        FinderSchema finder = new FinderSchema();

        finder.setName(resourceMethodDescriptor.getFinderName());

        String doc = _docsProvider.getMethodDoc(resourceMethodDescriptor.getMethod());
        if (doc != null)
        {
          finder.setDoc(doc);
        }

        ParameterSchemaArray parameters = createParameters(resourceMethodDescriptor);
        if (parameters.size() > 0)
        {
          finder.setParameters(parameters);
        }
        StringArray assocKeys = createAssocKeyParameters(resourceMethodDescriptor);
        if (assocKeys.size() > 0)
        {
          finder.setAssocKeys(assocKeys);
        }
        if (resourceMethodDescriptor.getCollectionCustomMetadataType() != null)
        {
          finder.setMetadata(createMetadataSchema(resourceMethodDescriptor));
        }

        final DataMap customAnnotation = resourceMethodDescriptor.getCustomAnnotationData();

        String deprecatedDoc = _docsProvider.getMethodDeprecatedTag(resourceMethodDescriptor.getMethod());
        if(deprecatedDoc != null)
        {
          customAnnotation.put(DEPRECATED_ANNOTATION_NAME, deprecateDocToAnnotationMap(deprecatedDoc));
        }

        if (!customAnnotation.isEmpty())
        {
          finder.setAnnotations(new CustomAnnotationContentSchemaMap(customAnnotation));
        }

        if (resourceMethodDescriptor.isPagingSupported()) {
          finder.setPagingSupported(true);
        }

        findersArray.add(finder);
      }
    }
    return findersArray;
  }

  @SuppressWarnings("deprecation")
  private StringArray createAssocKeyParameters(final ResourceMethodDescriptor resourceMethodDescriptor)
  {
    StringArray assocKeys = new StringArray();
    for (Parameter<?> param : resourceMethodDescriptor.getParameters())
    {
      // assocKeys are listed outside the parameters list
      if (param.getParamType() == Parameter.ParamType.KEY || param.getParamType() == Parameter.ParamType.ASSOC_KEY_PARAM)
      {
        assocKeys.add(param.getName());
        continue;
      }
    }
    return assocKeys;
  }

  private MetadataSchema createMetadataSchema(final ResourceMethodDescriptor resourceMethodDescriptor)
  {
    Class<?> metadataType = resourceMethodDescriptor.getCollectionCustomMetadataType();
    MetadataSchema metadataSchema = new MetadataSchema();
    metadataSchema.setType(buildDataSchemaType(metadataType));
    return metadataSchema;
  }

  @SuppressWarnings("deprecation")
  private ParameterSchemaArray createParameters(final ResourceMethodDescriptor resourceMethodDescriptor)
  {
    ParameterSchemaArray parameterSchemaArray = new ParameterSchemaArray();
    for (Parameter<?> param : resourceMethodDescriptor.getParameters())
    {
      //only custom parameters need to be specified in the IDL
      if (!param.isCustom())
      {
        continue;
      }

      // assocKeys are listed outside the parameters list
      if (param.getParamType() == Parameter.ParamType.KEY  || param.getParamType() == Parameter.ParamType.ASSOC_KEY_PARAM)
      {
        continue;
      }

      ParameterSchema paramSchema = new ParameterSchema();
      paramSchema.setName(param.getName());
      paramSchema.setType(buildDataSchemaType(param.getType(), param.getDataSchema()));

      final Object defaultValueData = param.getDefaultValueData();
      if (defaultValueData == null && param.isOptional())
      {
        paramSchema.setOptional(true);
      }
      else if (defaultValueData != null)
      {
        paramSchema.setDefault(defaultValueData.toString());
      }

      String paramDoc = _docsProvider.getParamDoc(resourceMethodDescriptor.getMethod(), param.getName());
      if (paramDoc != null)
      {
        paramSchema.setDoc(paramDoc);
      }

      final DataMap customAnnotation = param.getCustomAnnotationData();
      if (param.getAnnotations().contains(Deprecated.class))
      {
        customAnnotation.put(DEPRECATED_ANNOTATION_NAME, new DataMap());
      }

      if (!customAnnotation.isEmpty())
      {
        paramSchema.setAnnotations(new CustomAnnotationContentSchemaMap(customAnnotation));
      }

      parameterSchemaArray.add(paramSchema);
    }

    return parameterSchemaArray;
  }

  private RestMethodSchemaArray createRestMethods(final ResourceModel resourceModel)
  {
    RestMethodSchemaArray restMethods = new RestMethodSchemaArray();

    ResourceMethod[] crudMethods =
            {
                    ResourceMethod.CREATE,
                    ResourceMethod.GET,
                    ResourceMethod.UPDATE,
                    ResourceMethod.PARTIAL_UPDATE,
                    ResourceMethod.DELETE,
                    ResourceMethod.BATCH_CREATE,
                    ResourceMethod.BATCH_GET,
                    ResourceMethod.BATCH_UPDATE,
                    ResourceMethod.BATCH_PARTIAL_UPDATE,
                    ResourceMethod.BATCH_DELETE,
                    ResourceMethod.GET_ALL
            };

    for (ResourceMethod method : crudMethods)
    {
      ResourceMethodDescriptor descriptor = resourceModel.findMethod(method);
      if (descriptor == null)
      {
        continue;
      }

      RestMethodSchema restMethod = new RestMethodSchema();

      restMethod.setMethod(method.toString());

      String doc = _docsProvider.getMethodDoc(descriptor.getMethod());
      if (doc != null)
      {
        restMethod.setDoc(doc);
      }

      ParameterSchemaArray parameters = createParameters(descriptor);
      if (parameters.size() > 0)
      {
        restMethod.setParameters(parameters);
      }

      final DataMap customAnnotation = descriptor.getCustomAnnotationData();


      String deprecatedDoc = _docsProvider.getMethodDeprecatedTag(descriptor.getMethod());
      if(deprecatedDoc != null)
      {
        customAnnotation.put(DEPRECATED_ANNOTATION_NAME, deprecateDocToAnnotationMap(deprecatedDoc));
      }

      if (!customAnnotation.isEmpty())
      {
        restMethod.setAnnotations(new CustomAnnotationContentSchemaMap(customAnnotation));
      }

      if (method == ResourceMethod.GET_ALL)
      {
        if (descriptor.getCollectionCustomMetadataType() != null)
        {
          restMethod.setMetadata(createMetadataSchema(descriptor));
        }

        if (descriptor.isPagingSupported())
        {
          restMethod.setPagingSupported(true);
        }
      }

      restMethods.add(restMethod);
    }

    return restMethods;
  }

  private void appendSupportsNodeToCollectionSchema(final CollectionSchema collectionSchema,
                                                    final ResourceModel resourceModel)
  {
    StringArray supportsArray = buildSupportsNode(resourceModel);
    collectionSchema.setSupports(supportsArray);
  }

  private void appendMethodsToCollectionSchema(CollectionSchema collectionSchema, ResourceModel resourceModel)
  {
    RestMethodSchemaArray restMethods = createRestMethods(resourceModel);
    if (restMethods.size() > 0)
    {
      collectionSchema.setMethods(restMethods);
    }
  }

  private void appendSupportsNodeToSimpleSchema(final SimpleSchema simpleSchema,
                                                      final ResourceModel resourceModel)
  {
    StringArray supportsArray = buildSupportsNode(resourceModel);
    simpleSchema.setSupports(supportsArray);
  }

  private void appendMethodsToSimpleSchema(SimpleSchema simpleSchema, ResourceModel resourceModel)
  {
    RestMethodSchemaArray restMethods = createRestMethods(resourceModel);
    if (restMethods.size() > 0)
    {
      simpleSchema.setMethods(restMethods);
    }
  }

  private StringArray buildSupportsNode(ResourceModel resourceModel)
  {
    StringArray supportsArray = new StringArray();

    buildSupportsArray(resourceModel, supportsArray);
    return supportsArray;
  }

  private void buildSupportsArray(final ResourceModel resourceModel, final StringArray supportsArray)
  {
    List<String> supportsStrings = new ArrayList<String>();
    for (ResourceMethodDescriptor resourceMethodDescriptor : resourceModel.getResourceMethodDescriptors())
    {
      ResourceMethod type = resourceMethodDescriptor.getType();
      if (! type.equals(ResourceMethod.FINDER) &&
          ! type.equals(ResourceMethod.ACTION))
      {
        supportsStrings.add(type.toString());
      }
    }

    Collections.sort(supportsStrings);

    for (String s : supportsStrings)
    {
      supportsArray.add(s);
    }
  }

  private void appendIdentifierNode(final CollectionSchema collectionNode,
                                    final ResourceModel collectionResource)
  {
    IdentifierSchema identifierSchema = new IdentifierSchema();
    identifierSchema.setName(collectionResource.getKeyName());
    // If the key is a complex key, set type to the schema type of the key part of the
    // complex key and params to that of the params part of the complex key.
    // Otherwise, just set the type to the key class schema type
    if (collectionResource.getKeyClass().equals(ComplexResourceKey.class))
    {
      identifierSchema.setType(buildDataSchemaType(collectionResource.getKeyKeyClass()));
      identifierSchema.setParams(buildDataSchemaType(collectionResource.getKeyParamsClass()));
    }
    else
    {
      Key key = collectionResource.getPrimaryKey();
      identifierSchema.setType(buildDataSchemaType(key.getType(), key.getDataSchema()));
    }

    collectionNode.setIdentifier(identifierSchema);
  }
}

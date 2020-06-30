/*
 Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.docgen.examplegen;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchema.Type;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.schema.generator.DataGenerator;
import com.linkedin.data.schema.generator.SchemaSampleDataGenerator;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DirectArrayTemplate;
import com.linkedin.data.template.DirectMapTemplate;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.FixedTemplate;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.data.template.UnionTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.AbstractRequestBuilder;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.ActionRequestBuilder;
import com.linkedin.restli.client.BatchCreateRequest;
import com.linkedin.restli.client.BatchCreateRequestBuilder;
import com.linkedin.restli.client.BatchDeleteRequest;
import com.linkedin.restli.client.BatchDeleteRequestBuilder;
import com.linkedin.restli.client.BatchFindRequest;
import com.linkedin.restli.client.BatchFindRequestBuilder;
import com.linkedin.restli.client.BatchGetKVRequest;
import com.linkedin.restli.client.BatchGetRequestBuilder;
import com.linkedin.restli.client.BatchPartialUpdateRequest;
import com.linkedin.restli.client.BatchPartialUpdateRequestBuilder;
import com.linkedin.restli.client.BatchUpdateRequest;
import com.linkedin.restli.client.BatchUpdateRequestBuilder;
import com.linkedin.restli.client.CreateRequest;
import com.linkedin.restli.client.CreateRequestBuilder;
import com.linkedin.restli.client.DeleteRequest;
import com.linkedin.restli.client.DeleteRequestBuilder;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.FindRequestBuilder;
import com.linkedin.restli.client.GetAllRequest;
import com.linkedin.restli.client.GetAllRequestBuilder;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.GetRequestBuilder;
import com.linkedin.restli.client.OptionsRequest;
import com.linkedin.restli.client.OptionsRequestBuilder;
import com.linkedin.restli.client.PartialUpdateRequest;
import com.linkedin.restli.client.PartialUpdateRequestBuilder;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RestfulRequestBuilder;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.UpdateRequest;
import com.linkedin.restli.client.UpdateRequestBuilder;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpMethod;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.util.ResourceSchemaToResourceSpecTranslator;
import com.linkedin.restli.common.util.ResourceSchemaToResourceSpecTranslator.ClassBindingResolver;
import com.linkedin.restli.common.util.RichResourceSchema;
import com.linkedin.restli.internal.client.RequestBodyTransformer;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.internal.server.response.ResponseUtils;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.BatchFinderSchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;


import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Generates examples requests and responses for a given interface definition (.restspec.json) and it's corresponding
 * data schemas (.pdscs).
 *
 * @author jbetz@linkedin.com
 */
public class ExampleRequestResponseGenerator
{
  private static final ClassBindingResolver BINDING_RESOLVER = new ExampleGeneratorClassBindingResolver();
  private static final JacksonDataTemplateCodec CODEC = new JacksonDataTemplateCodec();

  // We plan to switch to using AllProtocolVersions here, the way client builders currently work make this impossible
  private final RestliRequestOptions _requestOptions;

  private final RichResourceSchema _resourceSchema; // Source of truth resource representation.
  private final ResourceSpec _resourceSpec;         // A client side representation of the resource, for building example requests.
  private final ResourceModel _resourceModel;       // A server side representation of the resource, for building example responses.

  private final Map<ResourceSchema, ResourceSpec> _parentResources;

  private final DataSchemaResolver _schemaResolver;
  private final DataGenerator _dataGenerator;

  private final RestLiResponseHandler _responseHandler = new RestLiResponseHandler();
  private final String _uriTemplate;


  public ExampleRequestResponseGenerator(ResourceSchema resourceSchema,
                                         DataSchemaResolver schemaResolver)
  {
    this(Collections.<ResourceSchema>emptyList(),
        resourceSchema,
        schemaResolver);
  }

  public ExampleRequestResponseGenerator(ResourceSchema resourceSchema,
                                         DataSchemaResolver schemaResolver,
                                         DataGenerator dataGenerator,
                                         RestliRequestOptions requestOptions)
  {
    this(Collections.<ResourceSchema>emptyList(),
        resourceSchema,
        schemaResolver,
        dataGenerator,
        requestOptions);
  }

  public ExampleRequestResponseGenerator(List<ResourceSchema> parentResourceSchemas,
                                         ResourceSchema resourceSchema,
                                         DataSchemaResolver schemaResolver)
  {
    this(parentResourceSchemas,
        resourceSchema,
        schemaResolver,
        new SchemaSampleDataGenerator(schemaResolver, new SchemaSampleDataGenerator.DataGenerationOptions()),
        RestliRequestOptions.DEFAULT_OPTIONS);
  }

  public ExampleRequestResponseGenerator(List<ResourceSchema> parentResourceSpecs,
                                         ResourceSchema resourceSchema,
                                         DataSchemaResolver schemaResolver,
                                         DataGenerator dataGenerator,
                                         RestliRequestOptions requestOptions)
  {
    _requestOptions = requestOptions;
    _parentResources = translate(parentResourceSpecs, schemaResolver);

    _resourceSchema = new RichResourceSchema(resourceSchema);
    _resourceSpec = translate(resourceSchema, schemaResolver);
    _resourceModel = buildPlaceholderResourceModel(resourceSchema);
    _uriTemplate = _resourceSchema.getResourceSchema().getPath();
    _schemaResolver = schemaResolver;
    _dataGenerator = dataGenerator;
  }

  public ExampleRequestResponse method(ResourceMethod method)
  {
    switch(method)
    {
      case OPTIONS: return options();
      case GET_ALL: return getAll();
      case GET: return get();
      case CREATE: return create();
      case UPDATE: return update();
      case PARTIAL_UPDATE: return partialUpdate();
      case DELETE: return delete();

      case BATCH_GET: return batchGet();
      case BATCH_CREATE: return batchCreate();
      case BATCH_UPDATE: return batchUpdate();
      case BATCH_PARTIAL_UPDATE: return batchPartialUpdate();
      case BATCH_DELETE: return batchDelete();
      default: throw new IllegalArgumentException("Unrecognized ResourceMethod value requested, this method only supports core restful " +
                                                      "methods, finder and action not supported (for those, use finder() or action()).  for method:" + method);
    }
  }

  public ExampleRequestResponse finder(String name)
  {
    FinderSchema finderSchema = _resourceSchema.getFinder(name);
    if (finderSchema == null)
    {
      throw new IllegalArgumentException("No such finder for resource: " + name);
    }
    RecordDataSchema metadataSchema = null;
    if (finderSchema.hasMetadata())
    {
      metadataSchema = (RecordDataSchema) RestSpecCodec.textToSchema(finderSchema.getMetadata().getType(),
                                                                     _schemaResolver);
    }

    return buildRequestResponse(buildFinderRequest(finderSchema),
        buildFinderResult(metadataSchema),
        buildResourceMethodDescriptorForFinder(name));
  }

  public ExampleRequestResponse batchFinder(String name) {
    BatchFinderSchema batchFinderSchema = _resourceSchema.getBatchFinder(name);
    if (batchFinderSchema == null)
    {
      throw new IllegalArgumentException("No such batch finder for resource: " + name);
    }
    RecordDataSchema metadataSchema = null;
    if (batchFinderSchema.hasMetadata())
    {
      metadataSchema = (RecordDataSchema) RestSpecCodec.textToSchema(batchFinderSchema.getMetadata().getType(),
          _schemaResolver);
    }

    Request<?> request = buildBatchFinderRequest(batchFinderSchema);
    RestRequest restRequest = buildRequest(request);
    try
    {
      ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), restRequest, new RequestContext());
      DataList criteriaParams = (DataList)context.getStructuredParameter(batchFinderSchema.getBatchParam());
      // Since batchFinder has 2 kinds of responses. One is successful CollectionResponse. The other one is ErrorResponse.
      // When BatchFinderResponseBuilder cannot find a search criteria, it will return an ErrorResponse.
      // To include only one criteria in BatchFinderResult will make the response example diverse.
      AnyRecord batchFinderCriteria = new AnyRecord((DataMap) criteriaParams.get(0));// guarantee batchFinder request and response has a same criteria

      return buildRequestResponse(request, buildBatchFinderResult(metadataSchema, batchFinderCriteria), buildResourceMethodDescriptorForBatchFinder(name, batchFinderSchema.getBatchParam()));
    }
    catch (RestLiSyntaxException e)
    {
      throw new ExampleGenerationException("Internal error during example generation", e);
    }
  }

  public ExampleRequestResponse action(String name, ResourceLevel resourceLevel)
  {
    ActionSchema actionSchema;
    switch(resourceLevel)
    {
      case COLLECTION:
        actionSchema = _resourceSchema.getAction(name);
        break;
      case ENTITY:
        actionSchema = _resourceSchema.getEntityAction(name);
        break;
      default:
        throw new IllegalArgumentException("Unsupported resourceLevel: " + resourceLevel);
    }

    if (actionSchema == null)
    {
      throw new IllegalArgumentException("No such action for resource: " + name);
    }
    DynamicRecordMetadata returnsMetadata = _resourceSpec.getActionResponseMetadata(name);

    return buildRequestResponse(buildActionRequest(actionSchema, resourceLevel),
        buildActionResult(actionSchema),
        buildResourceMethodDescriptorForAction(name, returnsMetadata, resourceLevel));
  }

  public ExampleRequestResponse options()
  {
    OptionsRequest request = new OptionsRequestBuilder(_uriTemplate, _requestOptions).build();
    return buildRequestResponse(request, _resourceSchema, buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse getAll()
  {
    checkSupports(ResourceMethod.GET_ALL);
    GetAllRequestBuilder<Object, RecordTemplatePlaceholder> getAll =
      new GetAllRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);

    addParams(getAll, ResourceMethod.GET_ALL);
    addPathKeys(getAll);
    GetAllRequest<RecordTemplatePlaceholder> request = getAll.build();

    return buildRequestResponse(request, buildFinderResult(null), buildResourceMethodDescriptorForFinder("getAll"));
  }

  public ExampleRequestResponse get()
  {
    checkSupports(ResourceMethod.GET);
    GetRequestBuilder<Object, RecordTemplatePlaceholder> get =
      new GetRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);

    if (_resourceSpec.getKeyType() != null)
    {
      get.id(generateKey());
    }
    addParams(get, ResourceMethod.GET);
    addPathKeys(get);
    GetRequest<RecordTemplatePlaceholder> request = get.build();
    return buildRequestResponse(request, generateEntity(), buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse create()
  {
    checkSupports(ResourceMethod.CREATE);
    CreateRequestBuilder<Object, RecordTemplatePlaceholder> create =
      new CreateRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    create.input(generateEntity());
    addParams(create, ResourceMethod.CREATE);
    addPathKeys(create);
    CreateRequest<RecordTemplatePlaceholder> request = create.build();
    return buildRequestResponse(request, new CreateResponse(generateKey(), HttpStatus.S_201_CREATED), buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse update()
  {
    checkSupports(ResourceMethod.UPDATE);
    UpdateRequestBuilder<Object, RecordTemplatePlaceholder> update =
      new UpdateRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    if (_resourceSpec.getKeyType() != null)
    {
      update.id(generateKey());
    }
    update.input(generateEntity());
    addParams(update, ResourceMethod.UPDATE);
    addPathKeys(update);
    UpdateRequest<RecordTemplatePlaceholder> request = update.build();
    return buildRequestResponse(request, new UpdateResponse(HttpStatus.S_200_OK), buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse partialUpdate()
  {
    checkSupports(ResourceMethod.PARTIAL_UPDATE);
    PartialUpdateRequestBuilder<Object, RecordTemplatePlaceholder> update =
      new PartialUpdateRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    if (_resourceSpec.getKeyType() != null)
    {
      update.id(generateKey());
    }
    update.input(PatchGenerator.<RecordTemplatePlaceholder>diffEmpty(generateEntity()));
    addParams(update, ResourceMethod.PARTIAL_UPDATE);
    addPathKeys(update);
    PartialUpdateRequest<RecordTemplatePlaceholder> request = update.build();
    return buildRequestResponse(request, new UpdateResponse(HttpStatus.S_200_OK), buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse delete()
  {
    checkSupports(ResourceMethod.DELETE);
    DeleteRequestBuilder<Object, RecordTemplatePlaceholder> delete =
      new DeleteRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    if (_resourceSpec.getKeyType() != null)
    {
      delete.id(generateKey());
    }
    addParams(delete, ResourceMethod.DELETE);
    addPathKeys(delete);
    DeleteRequest<RecordTemplatePlaceholder> request = delete.build();
    return buildRequestResponse(request, new UpdateResponse(HttpStatus.S_200_OK), buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse batchGet()
  {
    checkSupports(ResourceMethod.BATCH_GET);
    BatchGetRequestBuilder<Object, RecordTemplatePlaceholder> batchGet =
      new BatchGetRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    Object id1 = generateKey(0);
    Object id2 = generateKey(1);
    batchGet.ids(id1, id2);
    addParams(batchGet, ResourceMethod.BATCH_GET);
    addPathKeys(batchGet);
    BatchGetKVRequest<Object, RecordTemplatePlaceholder> request = batchGet.buildKV();

    final Map<Object, RecordTemplatePlaceholder> bgResponseData = new HashMap<Object, RecordTemplatePlaceholder>();
    bgResponseData.put(id1, generateEntity());
    bgResponseData.put(id2, generateEntity());
    BatchResult<Object, RecordTemplatePlaceholder> result = new BatchResult<Object, RecordTemplatePlaceholder>(bgResponseData, new HashMap<Object, RestLiServiceException>());
    return buildRequestResponse(request, result, buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse batchCreate()
  {
    checkSupports(ResourceMethod.BATCH_CREATE);
    BatchCreateRequestBuilder<Object, RecordTemplatePlaceholder> create =
      new BatchCreateRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    create.input(generateEntity());
    create.input(generateEntity());
    addParams(create, ResourceMethod.BATCH_CREATE);
    addPathKeys(create);
    BatchCreateRequest<RecordTemplatePlaceholder> request = create.build();
    BatchCreateResult<Object, RecordTemplatePlaceholder> result = new BatchCreateResult<Object, RecordTemplatePlaceholder>(Arrays.asList(
        new CreateResponse(generateKey(), HttpStatus.S_201_CREATED),
        new CreateResponse(generateKey(), HttpStatus.S_201_CREATED)));
    return buildRequestResponse(request, result, buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse batchUpdate()
  {
    checkSupports(ResourceMethod.BATCH_UPDATE);
    BatchUpdateRequestBuilder<Object, RecordTemplatePlaceholder> update =
      new BatchUpdateRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    Object id1 = generateKey(0);
    Object id2 = generateKey(1);

    update.input(id1, generateEntity());
    update.input(id2, generateEntity());
    addParams(update, ResourceMethod.BATCH_UPDATE);
    addPathKeys(update);
    BatchUpdateRequest<Object, RecordTemplatePlaceholder> request = update.build();
    return buildRequestResponse(request, createBatchUpdateResult(id1, id2), buildResourceMethodDescriptorForRestMethod(request));
  }

  private BatchUpdateResult<Object, RecordTemplatePlaceholder> createBatchUpdateResult(Object id1, Object id2)
  {
    Map<Object, UpdateResponse> buResponseData = new HashMap<Object, UpdateResponse>();
    buResponseData.put(id1, new UpdateResponse(HttpStatus.S_200_OK));
    buResponseData.put(id2, new UpdateResponse(HttpStatus.S_200_OK));
    return new BatchUpdateResult<Object, RecordTemplatePlaceholder>(buResponseData);
  }

  public ExampleRequestResponse batchPartialUpdate()
  {
    checkSupports(ResourceMethod.BATCH_PARTIAL_UPDATE);
    BatchPartialUpdateRequestBuilder<Object, RecordTemplatePlaceholder> update =
      new BatchPartialUpdateRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    Object id1 = generateKey(0);
    Object id2 = generateKey(1);
    update.input(id1, PatchGenerator.<RecordTemplatePlaceholder>diffEmpty(generateEntity()));
    update.input(id2, PatchGenerator.<RecordTemplatePlaceholder>diffEmpty(generateEntity()));
    addParams(update, ResourceMethod.BATCH_PARTIAL_UPDATE);
    addPathKeys(update);
    BatchPartialUpdateRequest<Object, RecordTemplatePlaceholder> request = update.build();
    return buildRequestResponse(request, createBatchUpdateResult(id1, id2), buildResourceMethodDescriptorForRestMethod(request));
  }

  public ExampleRequestResponse batchDelete()
  {
    checkSupports(ResourceMethod.BATCH_DELETE);
    BatchDeleteRequestBuilder<Object, RecordTemplatePlaceholder> delete =
      new BatchDeleteRequestBuilder<Object, RecordTemplatePlaceholder>(
        _uriTemplate,
        RecordTemplatePlaceholder.class, _resourceSpec, _requestOptions);
    Object id1 = generateKey(0);
    Object id2 = generateKey(1);
    delete.ids(id1, id2);
    addParams(delete, ResourceMethod.BATCH_DELETE);
    addPathKeys(delete);
    BatchDeleteRequest<Object, RecordTemplatePlaceholder> request = delete.build();

    final Map<Object, UpdateResponse> bdResponseData = new HashMap<Object, UpdateResponse>();
    bdResponseData.put(id1, new UpdateResponse(HttpStatus.S_200_OK));
    bdResponseData.put(id2, new UpdateResponse(HttpStatus.S_200_OK));
    BatchUpdateResult<Object, RecordTemplatePlaceholder> result = new BatchUpdateResult<Object, RecordTemplatePlaceholder>(bdResponseData);
    return buildRequestResponse(request, result, buildResourceMethodDescriptorForRestMethod(request));
  }

  private ExampleRequestResponse buildRequestResponse(Request<?> request, Object responseEntity, ResourceMethodDescriptor method)
  {
    RestRequest restRequest = buildRequest(request);
    RestResponse restResponse = buildResponse(responseEntity, method, restRequest);
    return new ExampleRequestResponse(restRequest, restResponse);
  }

  private RestRequest buildRequest(Request<?> request)
  {
    ProtocolVersion protocolVersion;
    switch(_requestOptions.getProtocolVersionOption())
    {
      case FORCE_USE_LATEST:
        protocolVersion = AllProtocolVersions.LATEST_PROTOCOL_VERSION;
        break;
      case USE_LATEST_IF_AVAILABLE:
        protocolVersion = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;
        break;
      case FORCE_USE_NEXT:
        protocolVersion = AllProtocolVersions.NEXT_PROTOCOL_VERSION;
        break;
      case FORCE_USE_PREVIOUS:
        protocolVersion = AllProtocolVersions.PREVIOUS_PROTOCOL_VERSION;
        break;
      default:
        throw new IllegalArgumentException("Unsupported enum value: " + _requestOptions.getProtocolVersionOption());
    }

    URI uri = RestliUriBuilderUtil.createUriBuilder(request, "", protocolVersion).build();
    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri);
    requestBuilder.setMethod(request.getMethod().getHttpMethod().name());

    // unfortunately some headers get set in RestClient, and since we're not using rest client, we
    // replicate that behavior here
    requestBuilder.setHeader(RestConstants.HEADER_ACCEPT, RestConstants.HEADER_VALUE_APPLICATION_JSON);

    requestBuilder.setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());

    if (request.getMethod().getHttpMethod() == HttpMethod.POST)
    {
      requestBuilder.setHeader(RestConstants.HEADER_RESTLI_REQUEST_METHOD, request.getMethod().toString());
    }

    if (request.getInputRecord() != null)
    {
      requestBuilder.setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON);
      writeEntity(request, protocolVersion, requestBuilder);
    }

    return requestBuilder.build();
  }

  private static void writeEntity(Request<?> request, ProtocolVersion protocolVersion, RestRequestBuilder messageBuilder)
  {
    try
    {
      DataMap dataMap = RequestBodyTransformer.transform(request, protocolVersion);
      messageBuilder.setEntity(CODEC.mapToBytes(dataMap));
    }
    catch (IOException e)
    {
      throw new ExampleGenerationException("Unable to serializing data", e);
    }
  }

  private RestResponse buildResponse(Object responseEntity, ResourceMethodDescriptor method, RestRequest restRequest)
  {
    try
    {
      ServerResourceContext context = new ResourceContextImpl(new PathKeysImpl(), restRequest, new RequestContext());
      RestUtils.validateRequestHeadersAndUpdateResourceContext(
          restRequest.getHeaders(), Collections.emptySet(), context);
      method.setResourceModel(_resourceModel);
      final RoutingResult routingResult = new RoutingResult(context, method);

      RestLiResponseData<?> responseData = _responseHandler.buildRestLiResponseData(restRequest, routingResult, responseEntity);
      RestLiResponse restLiResponse = _responseHandler.buildPartialResponse(routingResult, responseData);
      return ResponseUtils.buildResponse(routingResult, restLiResponse);
    }
    catch (RestLiSyntaxException e)
    {
      throw new ExampleGenerationException("Internal error during example generation", e);
    }
    catch (IOException e)
    {
      throw new ExampleGenerationException("Unable to build example response", e);
    }
  }

  private void checkSupports(ResourceMethod method) throws IllegalArgumentException
  {
    if (!_resourceSpec.getSupportedMethods().contains(method))
    {
      throw new IllegalArgumentException(method + " not supported for resource.");
    }
  }

  private FindRequest<RecordTemplatePlaceholder> buildFinderRequest(FinderSchema finderSchema)
  {

    FindRequestBuilder<Object, RecordTemplatePlaceholder> finder =
        new FindRequestBuilder<Object, RecordTemplatePlaceholder>(
            _uriTemplate,
            RecordTemplatePlaceholder.class,
            _resourceSpec,
            _requestOptions);

    finder.name(finderSchema.getName());

    if (finderSchema.hasAssocKeys())
    {
      CompoundKey key = (CompoundKey)generateKey();
      for (String partKey : finderSchema.getAssocKeys())
      {
        finder.assocKey(partKey, key.getPart(partKey));
      }
    }
    else if (finderSchema.hasAssocKey()) // why do we have a separate field for the singular form?  assocKeys and assocKey.
    {
      String partKey = finderSchema.getAssocKey();
      CompoundKey key = (CompoundKey)generateKey();
      finder.assocKey(partKey, key.getPart(partKey));
    }

    if (finderSchema.hasParameters() && !finderSchema.getParameters().isEmpty())
    {
      addParams(finder, finderSchema.getParameters());
    }
    addPathKeys(finder);
    return finder.build();
  }

  private CollectionResult<RecordTemplatePlaceholder, RecordTemplatePlaceholder> buildFinderResult(RecordDataSchema finderMetadataSchema)
  {
    final List<RecordTemplatePlaceholder> results = new ArrayList<RecordTemplatePlaceholder>();
    results.add(generateEntity());
    results.add(generateEntity());

    if (finderMetadataSchema != null)
    {
      DataMap metadataDataMap = (DataMap)_dataGenerator.buildData("metadata", finderMetadataSchema);
      RecordTemplatePlaceholder metadata = new RecordTemplatePlaceholder(metadataDataMap, finderMetadataSchema);
      return new CollectionResult<RecordTemplatePlaceholder, RecordTemplatePlaceholder>(results, results.size(), metadata);
    }
    else
    {
      return new CollectionResult<RecordTemplatePlaceholder, RecordTemplatePlaceholder>(results);
    }
  }

  private BatchFindRequest<RecordTemplatePlaceholder> buildBatchFinderRequest(BatchFinderSchema batchFinderSchema)
  {

    BatchFindRequestBuilder<Object, RecordTemplatePlaceholder> batchFinder =
        new BatchFindRequestBuilder<Object, RecordTemplatePlaceholder>(
            _uriTemplate,
            RecordTemplatePlaceholder.class,
            _resourceSpec,
            _requestOptions);

    batchFinder.name(batchFinderSchema.getName());

    if (batchFinderSchema.hasAssocKeys())
    {
      CompoundKey key = (CompoundKey)generateKey();
      for (String partKey : batchFinderSchema.getAssocKeys())
      {
        batchFinder.assocKey(partKey, key.getPart(partKey));
      }
    }
    else if (batchFinderSchema.hasAssocKey())
    {
      String partKey = batchFinderSchema.getAssocKey();
      CompoundKey key = (CompoundKey)generateKey();
      batchFinder.assocKey(partKey, key.getPart(partKey));
    }

    if (batchFinderSchema.hasParameters() && !batchFinderSchema.getParameters().isEmpty())
    {
      addParams(batchFinder, batchFinderSchema.getParameters());
    }
    // Add specific batch parameter
    if (batchFinderSchema.hasBatchParam()) {
      addBatchParams(batchFinder, batchFinderSchema.getParameters(), batchFinderSchema.getBatchParam());
    }
    addPathKeys(batchFinder);
    return batchFinder.build();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private BatchFinderResult<RecordTemplatePlaceholder,RecordTemplatePlaceholder, RecordTemplatePlaceholder> buildBatchFinderResult(RecordDataSchema batchFinderMetadataSchema, RecordTemplate batchFinderCriteria)
  {
    final List<RecordTemplatePlaceholder> results = new ArrayList<RecordTemplatePlaceholder>();
    results.add(generateEntity());
    results.add(generateEntity());

    BatchFinderResult batchFinderResult = new BatchFinderResult();

    if (batchFinderMetadataSchema != null)
    {
      DataMap metadataDataMap = (DataMap)_dataGenerator.buildData("metadata", batchFinderMetadataSchema);
      RecordTemplatePlaceholder metadata = new RecordTemplatePlaceholder(metadataDataMap, batchFinderMetadataSchema);
      CollectionResult cr = new CollectionResult<RecordTemplatePlaceholder, RecordTemplatePlaceholder>(results, results.size(), metadata);
      batchFinderResult.putResult(batchFinderCriteria, cr);
    }
    else
    {
      CollectionResult cr = new CollectionResult<RecordTemplatePlaceholder, RecordTemplatePlaceholder>(results, results.size());
      batchFinderResult.putResult(batchFinderCriteria, cr);
    }

    return batchFinderResult;
  }

  @SuppressWarnings("unchecked")
  private ActionRequest<?> buildActionRequest(ActionSchema action, ResourceLevel resourceLevel)
  {
    DynamicRecordMetadata requestParamsMetadata = _resourceSpec.getRequestMetadata(action.getName());
    DynamicRecordMetadata responseMetadata = _resourceSpec.getActionResponseMetadata(action.getName());

    TypeSpec<RecordTemplatePlaceholder> responseType = null;

    if (responseMetadata != null)
    {
      FieldDef<?> fieldDef = responseMetadata.getFieldDef("value");
      if (fieldDef != null && fieldDef.getDataClass() != null)
      {
        responseType = new TypeSpec<RecordTemplatePlaceholder>(
          (Class<RecordTemplatePlaceholder>)fieldDef.getDataClass(),
          responseMetadata.getRecordDataSchema());
      }
    }
    ActionRequestBuilder<Object, RecordTemplatePlaceholder> request =
        new ActionRequestBuilder<Object, RecordTemplatePlaceholder>(
          _uriTemplate,
          responseType,
          _resourceSpec,
            _requestOptions);

    request.name(action.getName());
    if (resourceLevel == ResourceLevel.ENTITY && !_resourceSpec.isKeylessResource())
    {
      request.id(generateKey());
    }

    if (requestParamsMetadata != null)
    {
      addParams(request, requestParamsMetadata, action.getParameters());
    }
    addPathKeys(request);
    return request.build();
  }

  private ActionResult<?> buildActionResult(ActionSchema actionSchema)
  {
    DynamicRecordMetadata returnsMetadata = _resourceSpec.getActionResponseMetadata(actionSchema.getName());
    if (actionSchema.hasReturns())
    {
      FieldDef<?> fieldDef = returnsMetadata.getFieldDef("value");
      Object returnValue = generateFieldDefValue(fieldDef);
      return new ActionResult<Object>(returnValue);
    }
    else
    {
      return null;
    }
  }

  private static ResourceMethodDescriptor buildResourceMethodDescriptorForAction(String name, DynamicRecordMetadata returnsMetadata, ResourceLevel resourceLevel)
  {
    return ResourceMethodDescriptor.createForAction(null,
                                                    null,
                                                    name,
                                                    resourceLevel,
                                                    returnsMetadata == null ? null : returnsMetadata.getFieldDef("value"),
                                                    returnsMetadata.getRecordDataSchema(),
                                                    null,
                                                    null,
                                                    null);
  }

  private static ResourceMethodDescriptor buildResourceMethodDescriptorForRestMethod(Request<?> request)
  {
    return ResourceMethodDescriptor.createForRestful(request.getMethod(), null, null);
  }

  private static ResourceMethodDescriptor buildResourceMethodDescriptorForFinder(String name)
  {
    return ResourceMethodDescriptor.createForFinder(null,
                                                    Collections.<Parameter<?>>emptyList(),
                                                    name,
                                                    RecordTemplatePlaceholder.class,
                                                    null,
                                                    null);
  }

  private ResourceMethodDescriptor buildResourceMethodDescriptorForBatchFinder(String name, String batchParamName)
  {
    List<Parameter<?>> parameters = new ArrayList<>();
    parameters.add(new Parameter<>(batchParamName,
                                  String.class,
                                  null,
                                  true,
                                  null,
                                  Parameter.ParamType.QUERY,
                                  true,
                                  AnnotationSet.EMPTY));
    return ResourceMethodDescriptor.createForBatchFinder(null,
                                                          parameters,
                                                          name,
                                                          0,
                                                          RecordTemplatePlaceholder.class,
                                                          null,
                                                          null);
  }

  private void addParams(RestfulRequestBuilder<?, ?, ?> builder, ResourceMethod method)
  {
    RestMethodSchema methodSchema = _resourceSchema.getMethod(method.toString().toLowerCase());
    ParameterSchemaArray parameters = methodSchema.getParameters();
    addParams(builder, parameters);
  }

  private void addPathKeys(AbstractRequestBuilder<?, ?, ?> builder)
  {
    for (Map.Entry<ResourceSchema, ResourceSpec> entry : _parentResources.entrySet())
    {
      ResourceSchema resourceSchema = entry.getKey();
      ResourceSpec resourceSpec = entry.getValue();
      if (resourceSpec.getKeyType() != null)
      {
        switch(toResourceKeyType(resourceSpec.getKeyType().getType()))
        {
          case PRIMITIVE:
          case COMPLEX:
            String keyName = resourceSchema.getCollection().getIdentifier().getName();
            builder.pathKey(keyName, generateKey(resourceSpec, resourceSchema, null));
            break;
          case COMPOUND:
            // old assocKey version
            Map<String, CompoundKey.TypeInfo> keyParts = resourceSpec.getKeyParts();
            for (Map.Entry<String, CompoundKey.TypeInfo> infoEntry : keyParts.entrySet())
            {
              String key = infoEntry.getKey();
              CompoundKey.TypeInfo typeInfo = infoEntry.getValue();
              builder.pathKey(key, _dataGenerator.buildData(key, typeInfo.getBinding().getSchema()));
            }
            // new key version
            String assocKeyName = resourceSchema.getAssociation().getIdentifier();
            builder.pathKey(assocKeyName, generateKey(resourceSpec, resourceSchema, null));
            break;
          case NONE:
            break;
          default:
            throw new IllegalStateException("Unrecognized key type: " + resourceSpec.getKeyType().getType());
        }
      }
    }
  }

  private void addParams(RestfulRequestBuilder<?, ?, ?> builder, ParameterSchemaArray parameters)
  {
    if (parameters != null)
    {
      for (ParameterSchema parameter : parameters)
      {
        if (!parameter.hasItems()) // ignoring legacy case where items was used for arrays
        {
          DataSchema dataSchema = RestSpecCodec.textToSchema(parameter.getType(), _schemaResolver);
          Object value = _dataGenerator.buildData(parameter.getName(), dataSchema);
          builder.setParam(parameter.getName(), value);
        }
      }
    }
  }

  private void addBatchParams(RestfulRequestBuilder<?, ?, ?> builder, ParameterSchemaArray parameters, String batchParamName) {
    if (parameters != null)
    {
      for (ParameterSchema parameter : parameters)
      {
        if (parameter.getName().equals(batchParamName))
        {
          DataSchema dataSchema = RestSpecCodec.textToSchema(parameter.getType(), _schemaResolver);
          Object value = _dataGenerator.buildData(parameter.getName(), dataSchema);
          builder.setParam(parameter.getName(), value);
        }
      }
    }
  }

  private void addParams(ActionRequestBuilder<?, ?> request, DynamicRecordMetadata requestMetadata, ParameterSchemaArray parameters)
  {
    if (parameters != null)
    {
      for (ParameterSchema parameter : parameters)
      {
        FieldDef<?> fieldDef = requestMetadata.getFieldDef(parameter.getName());
        Object value = generateFieldDefValue(fieldDef);
        // For custom types(TypeRefs) we generate the example values using the dereferenced type. Changing the field-def
        // to the dereferenced type so the example values can be set on the request without coercing.
        if (fieldDef.getDataSchema().getType() == Type.TYPEREF)
        {
          FieldDef<?> deRefFieldDef = new FieldDef<>(fieldDef.getName(), fieldDef.getDataClass(), fieldDef.getDataSchema().getDereferencedDataSchema());
          deRefFieldDef.getField().setRecord(fieldDef.getField().getRecord());
          fieldDef = deRefFieldDef;
        }
        request.setParam(fieldDef, value);
      }
    }
  }

  private Object generateFieldDefValue(FieldDef<?> fieldDef)
  {
    Object value = _dataGenerator.buildData(fieldDef.getName(), fieldDef.getDataSchema());
    DataSchema dereferencedDataSchema = fieldDef.getDataSchema().getDereferencedDataSchema();
    if (!dereferencedDataSchema.isPrimitive())
    {
      switch(dereferencedDataSchema.getType())
      {
        case FIXED:
          value = new FixedTemplatePlaceholder(value, (FixedDataSchema)dereferencedDataSchema);
          break;
        case ENUM:
          // just use the string value already generated.  Will be coerced by DataTemplateUtil.DynamicEnumCoercer.
          break;
        case ARRAY:
          value = new ArrayTemplatePlaceholder<Object>((DataList)value, (ArrayDataSchema)dereferencedDataSchema, Object.class);
          break;
        case RECORD:
          value = new RecordTemplatePlaceholder((DataMap)value, (RecordDataSchema)dereferencedDataSchema);
          break;
        case MAP:
          value = new MapTemplatePlaceholder<Object>((DataMap)value, (MapDataSchema)dereferencedDataSchema, Object.class);
          break;
        case UNION:
          value = new UnionTemplatePlaceholder(value, (UnionDataSchema)dereferencedDataSchema);
          break;
        case TYPEREF:
          throw new IllegalStateException("TYPEREF should not be returned for a dereferenced byte. schema: " + fieldDef.getDataSchema());
        default:
          throw new IllegalStateException("Unrecognized enum value: " + dereferencedDataSchema.getType());
      }
    }
    return value;
  }

  private Object generateKey()
  {
    return generateKey(null);
  }

  private Object generateKey(Integer batchIdx)
  {
    return generateKey(_resourceSpec, _resourceSchema.getResourceSchema(), batchIdx);
  }

  private Object generateKey(ResourceSpec resourceSpec, ResourceSchema resourceSchema, Integer batchIdx)
  {
    if (resourceSpec.getKeyType() == null)
    {
      throw new IllegalArgumentException("Cannot generate key for keyless resource.");
    }

    switch(toResourceKeyType(resourceSpec.getKeyType().getType()))
    {
      case COMPLEX:
        RecordDataSchema keySchema = (RecordDataSchema)resourceSpec.getComplexKeyType().getKeyType().getSchema();
        DataMap keyData = (DataMap)_dataGenerator.buildData(postfixBatchIdx(keySchema.getName() + "Key", batchIdx),
                                                           keySchema);
        RecordDataSchema paramsSchema = (RecordDataSchema)resourceSpec.getComplexKeyType().getParamsType().getSchema();
        DataMap paramsData = (DataMap)_dataGenerator.buildData(postfixBatchIdx(keySchema.getName() + "Params", batchIdx),
                                                              paramsSchema);
        return new ComplexResourceKey<RecordTemplatePlaceholder, RecordTemplatePlaceholder>(
            new RecordTemplatePlaceholder(keyData, keySchema),
            new RecordTemplatePlaceholder(paramsData, paramsSchema)
        );
      case COMPOUND:
        CompoundKey compoundKey = new CompoundKey();
        for (Map.Entry<String, CompoundKey.TypeInfo> keyPart : resourceSpec.getKeyParts().entrySet())
        {
          String key = keyPart.getKey();
          CompoundKey.TypeInfo typeInfo = keyPart.getValue();
          compoundKey.append(key, _dataGenerator.buildData(postfixBatchIdx(key, batchIdx),
                                                          typeInfo.getBinding().getSchema()), typeInfo);
        }
        return compoundKey;
      case PRIMITIVE:
        String keyName = resourceSchema.getCollection().getIdentifier().getName();
        return _dataGenerator.buildData(postfixBatchIdx(keyName, batchIdx), resourceSpec.getKeyType().getSchema());
      case NONE:
        return null;
      default:
        throw new IllegalStateException("Unknown enum value: " + resourceSpec.getKeyType().getType());
    }
  }

  private static String postfixBatchIdx(String input, Integer batchIdx)
  {
    if (batchIdx == null)
    {
      return input;
    }
    else
    {
      return input + batchIdx;
    }
  }

  private RecordTemplatePlaceholder generateEntity()
  {
    RecordDataSchema inputSchema = (RecordDataSchema)_resourceSpec.getValueType().getSchema();
    DataMap inputData = (DataMap)_dataGenerator.buildData("entity", inputSchema);
    return new RecordTemplatePlaceholder(inputData, inputSchema);
  }

  private enum ResourceKeyType
  {
    COMPLEX,
    COMPOUND,
    PRIMITIVE,
    NONE
  }

  private static ResourceKeyType toResourceKeyType(Class<?> type)
  {
    if (ComplexResourceKey.class == type)
    {
      return ResourceKeyType.COMPLEX;
    }
    else if (CompoundKey.class.isAssignableFrom(type))
    {
      return ResourceKeyType.COMPOUND;
    }
    else if (Void.class == type)
    {
      return ResourceKeyType.NONE;
    }
    return ResourceKeyType.PRIMITIVE;
  }

  private static class ExampleGeneratorClassBindingResolver implements ClassBindingResolver
  {
    @SuppressWarnings("rawtypes")
    public Class<? extends DataTemplate> resolveTemplateClass(DataSchema schema)
    {
      switch(schema.getDereferencedType())
      {
        case FIXED:
          return FixedTemplatePlaceholder.class;
        case ARRAY:
          return ArrayTemplatePlaceholder.class;
        case RECORD:
          return RecordTemplatePlaceholder.class;
        case MAP:
          return MapTemplatePlaceholder.class;
        case UNION:
          return UnionTemplatePlaceholder.class;
        case TYPEREF:
          throw new IllegalStateException("TYPEREF should not be returned for a dereferenced byte. schema: " + schema);
        default:
          throw new IllegalStateException("Unrecognized enum value: " + schema.getDereferencedType());
      }
    }

    @SuppressWarnings("rawtypes")
    public Class<? extends Enum> resolveEnumClass(EnumDataSchema enumDataSchema)
    {
      // Using Enum.class here triggers dynamic coercion of strings to enums.
      // See DataTemplateUtil.DynamicEnumCoercer for details.
      return Enum.class;
    }
  }

  private static ResourceSpec translate(ResourceSchema resourceSchema, DataSchemaResolver schemaResolver)
  {
    return new ResourceSchemaToResourceSpecTranslator(schemaResolver, BINDING_RESOLVER).translate(resourceSchema);
  }

  private static Map<ResourceSchema, ResourceSpec> translate(List<ResourceSchema> resourceSchemas,
                                                             DataSchemaResolver schemaResolver)
  {
    Map<ResourceSchema, ResourceSpec> result = new HashMap<ResourceSchema, ResourceSpec>();
    for (ResourceSchema resourceSchema : resourceSchemas)
    {
      result.put(resourceSchema, translate(resourceSchema, schemaResolver));
    }
    return result;
  }

  /* ---------------------------------------------------------------------------------------------------------------
   * The following classes are used by the example generator to dynamically construct rest.li client builder classes.
   * They are needed as substitutes for concrete DataTemplate classes (e.g. Greetings.class) to satisfy all the
   * 'instanceof' and 'isAssignableFrom' checks within the builder implementations.  They are also used in the
   * above code as generic type placeholders. They should never be used outside this example generator. They are
   * declared as "public" as we need to access their constructors from RecordTemplate#obtainWrapped while generating
   * example requests and responses. These classes can be made private once RequestBodyTransformer is removed.
   * ---------------------------------------------------------------------------------------------------------------
   */

  /**
   * SHOULD NEVER BE INSTANTIATED DIRECTLY!
   */
  public static class RecordTemplatePlaceholder extends RecordTemplate
  {
    public RecordTemplatePlaceholder(DataMap object)
        throws TemplateOutputCastException
    {
      super(object, null);
    }

    public RecordTemplatePlaceholder(DataMap object, RecordDataSchema schema)
        throws TemplateOutputCastException
    {
      super(object, schema);
    }
  }

  /**
   * SHOULD NEVER BE INSTANTIATED DIRECTLY!
   */
  public static class UnionTemplatePlaceholder extends UnionTemplate
  {
    public UnionTemplatePlaceholder(Object object)
        throws TemplateOutputCastException
    {
      super(object, null);
    }

    public UnionTemplatePlaceholder(Object object, UnionDataSchema schema)
        throws TemplateOutputCastException
    {
      super(object, schema);
    }
  }

  /**
   * SHOULD NEVER BE INSTANTIATED DIRECTLY!
   */
  public static class ArrayTemplatePlaceholder<E> extends DirectArrayTemplate<E>
  {
    public ArrayTemplatePlaceholder(DataList list)
    {
      super(list, null, null);
    }

    public ArrayTemplatePlaceholder(DataList list, ArrayDataSchema schema, Class<E> elementClass)
    {
      super(list, schema, elementClass);
    }
  }

  /**
   * SHOULD NEVER BE INSTANTIATED DIRECTLY!
   */
  public static class MapTemplatePlaceholder<V> extends DirectMapTemplate<V>
  {
    public MapTemplatePlaceholder(DataMap map)
    {
      super(map, null, null);
    }

    public MapTemplatePlaceholder(DataMap map, MapDataSchema schema, Class<V> valueClass)
    {
      super(map, schema, valueClass);
    }
  }

  /**
   * SHOULD NEVER BE INSTANTIATED DIRECTLY!
   */
  public static class FixedTemplatePlaceholder extends FixedTemplate
  {
    public FixedTemplatePlaceholder(Object object)
        throws TemplateOutputCastException
    {
      super(object, null);
    }

    public FixedTemplatePlaceholder(Object object, FixedDataSchema schema)
        throws TemplateOutputCastException
    {
      super(object, schema);
    }
  }

  private static ResourceModel buildPlaceholderResourceModel(ResourceSchema resourceSchema)
  {
    return new ResourceModel(RecordTemplatePlaceholder.class,
        Object.class,
        null,
        resourceSchema.getName(),
        null,
        resourceSchema.getNamespace());
  }
}

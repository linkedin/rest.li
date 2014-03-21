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

package com.linkedin.restli.client;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.common.test.RecordTemplateWithPrimitiveKey;
import com.linkedin.restli.internal.client.ActionResponseDecoder;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;
import com.linkedin.restli.internal.client.EmptyResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestRequestObjectOverrides
{
  private static final String BASE_URI_TEMPLATE = "foo";

  @SuppressWarnings("deprecation")
  private <T> Request<T> buildRequest(URI uri,
                                      ResourceMethod method,
                                      RecordTemplate inputRecord,
                                      RestResponseDecoder<T> decoder,
                                      ResourceSpec resourceSpec,
                                      DataMap queryParams)
  {
    return new Request<T>(uri,
                          method,
                          inputRecord,
                          Collections.<String, String>emptyMap(),
                          decoder,
                          resourceSpec,
                          queryParams);
  }

  private ResourceSpec buildResourceSpec()
  {
    return new ResourceSpecImpl(EnumSet.of(ResourceMethod.GET, 
                                           ResourceMethod.ACTION, 
                                           ResourceMethod.DELETE, 
                                           ResourceMethod.FINDER, 
                                           ResourceMethod.PARTIAL_UPDATE, 
                                           ResourceMethod.UPDATE),
                                null,
                                null,
                                Long.class,
                                RecordTemplateWithPrimitiveKey.class,
                                Collections.<String, Object>emptyMap());
  }
  
  @DataProvider(name = "data")
  public Object[][] dataProvider()
      throws URISyntaxException
  {
    List<List<Object>> data = new ArrayList<List<Object>>();

    addGetRequestData(data);
    addActionRequestData(data);
    addDeleteRequestData(data);
    addFindRequestData(data);
    addPartialUpdateRequestData(data);
    addUpdateRequestData(data);
    addRequestData(data);

    Object[][] returnData = new Object[data.size()][3];

    for (int i = 0; i < data.size(); i++)
    {
      List<Object> params = data.get(i);
      returnData[i] = new Object[]{params.get(0), params.get(1), params.get(2)};
    }

    return returnData;
  }
  
  @Test(dataProvider = "data")
  @SuppressWarnings({"rawtypes"})
  public void testEqualsAndHashCode(Request request1, Request request2, boolean areEqual)
  {
    if (areEqual)
    {
      Assert.assertTrue(request1.equals(request2), "Request 1: " + request1.toString() + "\n" +
          "Request 2: " + request2.toString());
      Assert.assertTrue(request1.hashCode() == request2.hashCode(), "Hash codes are not the same!");
    }
    else
    {
      Assert.assertFalse(request1.equals(request2), "Request 1: " + request1.toString() + "\n" +
          "Request 2: " + request2.toString());
      Assert.assertFalse(request2.equals(request1), "Request 1: " + request1.toString() + "\n" +
          "Request 2: " + request2.toString());
    }
  }

  private void addGetRequestData(List<List<Object>> data)
      throws URISyntaxException
  {
    ResourceMethod getMethod = ResourceMethod.GET;

    // new constructor

    GetRequest<RecordTemplateWithPrimitiveKey> getRequest1 =
        new GetRequest<RecordTemplateWithPrimitiveKey>(Collections.<String, String>emptyMap(),
                                                       RecordTemplateWithPrimitiveKey.class,
                                                       1L,
                                                       Collections.<String, Object>emptyMap(),
                                                       buildResourceSpec(),
                                                       BASE_URI_TEMPLATE,
                                                       Collections.<String, Object>singletonMap("id", 1L),
                                                       RestliRequestOptions.DEFAULT_OPTIONS);

    GetRequest<RecordTemplateWithPrimitiveKey> getRequest2 =
        new GetRequest<RecordTemplateWithPrimitiveKey>(Collections.<String, String>emptyMap(),
                                                       RecordTemplateWithPrimitiveKey.class,
                                                       1L,
                                                       Collections.<String, Object>emptyMap(),
                                                       buildResourceSpec(),
                                                       BASE_URI_TEMPLATE,
                                                       Collections.<String, Object>singletonMap("id", 1L),
                                                       RestliRequestOptions.DEFAULT_OPTIONS);

    data.add(Arrays.asList(getRequest1, getRequest2, true));

    GetRequest<RecordTemplateWithPrimitiveKey> getRequest3 =
        new GetRequest<RecordTemplateWithPrimitiveKey>(Collections.<String, String>emptyMap(),
                                                       RecordTemplateWithPrimitiveKey.class,
                                                       2L,
                                                       Collections.<String, Object>emptyMap(),
                                                       buildResourceSpec(),
                                                       BASE_URI_TEMPLATE,
                                                       Collections.<String, Object>singletonMap("id", 2L),
                                                       RestliRequestOptions.DEFAULT_OPTIONS);

    data.add(Arrays.asList(getRequest1, getRequest3, false));

    // old constructor

    EntityResponseDecoder<RecordTemplateWithPrimitiveKey> getResponseDecoder = new EntityResponseDecoder<RecordTemplateWithPrimitiveKey>(
        RecordTemplateWithPrimitiveKey.class);

    Request<RecordTemplateWithPrimitiveKey> request1 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"), getMethod,
                                                                          null,
                                                                          getResponseDecoder,
                                                                          buildResourceSpec(),
                                                                          new DataMap());
    Request<RecordTemplateWithPrimitiveKey> request2 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"), getMethod,
                                                                          null,
                                                                          getResponseDecoder,
                                                                          buildResourceSpec(),
                                                                          new DataMap());

    data.add(Arrays.asList(request1, request2, true));

    Request<RecordTemplateWithPrimitiveKey> request3 = buildRequest(new URI(BASE_URI_TEMPLATE + "/2"), getMethod,
                                                                          null,
                                                                          getResponseDecoder,
                                                                          buildResourceSpec(),
                                                                          new DataMap());

    data.add(Arrays.asList(request1, request3, false));

    // old-new combinations

    Request<RecordTemplateWithPrimitiveKey> newConsRequest1 =
        new Request<RecordTemplateWithPrimitiveKey>(getMethod,
                                                    null,
                                                    Collections.<String, String>emptyMap(),
                                                    getResponseDecoder,
                                                    buildResourceSpec(),
                                                    Collections.<String, Object>emptyMap(),
                                                    null,
                                                    BASE_URI_TEMPLATE,
                                                    Collections.<String, Object>singletonMap("id", 1L),
                                                    RestliRequestOptions.DEFAULT_OPTIONS);

    data.add(Arrays.asList(request1, newConsRequest1, false));
    data.add(Arrays.asList(newConsRequest1, request1, false));
  }

  private void addActionRequestData(List<List<Object>> data)
      throws URISyntaxException
  {
    /*
    Action name: bar
    Action params: "param", a String
    Action response: "response", a String
     */
    ResourceMethod actionMethod = ResourceMethod.ACTION;
    
    FieldDef<String> actionParam = new FieldDef<String>("param", String.class, DataTemplateUtil.getSchema(String.class));
    DynamicRecordMetadata requestMetadata = new DynamicRecordMetadata("bar", Collections.singleton(actionParam));
    DataMap dataMap = new DataMap();
    dataMap.put("param", "paramValue");
    DynamicRecordTemplate input = new DynamicRecordTemplate(dataMap, requestMetadata.getRecordDataSchema());

    FieldDef<String> actionResponse =
        new FieldDef<String>("response", String.class, DataTemplateUtil.getSchema(String.class));
    DynamicRecordMetadata responseMetaData = new DynamicRecordMetadata("bar", Collections.singleton(actionResponse));
    ActionResponseDecoder<String> decoder =
        new ActionResponseDecoder<String>(actionResponse, responseMetaData.getRecordDataSchema());

    // new constructor

    ActionRequest<String> actionRequest1 =
        new ActionRequest<String>(input,
                                  Collections.<String, String>emptyMap(),
                                  decoder,
                                  buildResourceSpec(),
                                  Collections.<String, Object>emptyMap(),
                                  "bar",
                                  BASE_URI_TEMPLATE,
                                  Collections.<String, Object>singletonMap("id", 1L),
                                  RestliRequestOptions.DEFAULT_OPTIONS,
                                  1L);
    ActionRequest<String> actionRequest2 =
        new ActionRequest<String>(input,
                                  Collections.<String, String>emptyMap(),
                                  decoder,
                                  buildResourceSpec(),
                                  Collections.<String, Object>emptyMap(),
                                  "bar",
                                  BASE_URI_TEMPLATE,
                                  Collections.<String, Object>singletonMap("id", 1L),
                                  RestliRequestOptions.DEFAULT_OPTIONS,
                                  1L);

    data.add(Arrays.asList(actionRequest1, actionRequest2, true));

    ActionRequest<String> actionRequest3 =
        new ActionRequest<String>(input,
                                  Collections.<String, String>emptyMap(),
                                  decoder,
                                  buildResourceSpec(),
                                  Collections.<String, Object>emptyMap(),
                                  "bar",
                                  BASE_URI_TEMPLATE,
                                  Collections.<String, Object>singletonMap("id", 2L),
                                  RestliRequestOptions.DEFAULT_OPTIONS,
                                  2L);

    data.add(Arrays.asList(actionRequest1, actionRequest3, false));

    // old constructor

    Request<String> request1 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1?action=bar"),
                                            actionMethod,
                                            input,
                                            decoder,
                                            buildResourceSpec(),
                                            null);

    Request<String> request2 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1?action=bar"),
                                            actionMethod,
                                            input,
                                            decoder,
                                            buildResourceSpec(),
                                            null);

    data.add(Arrays.asList(request1, request2, true));

    Request<String> request3 = buildRequest(new URI(BASE_URI_TEMPLATE + "/2?action=bar"),
                                            actionMethod,
                                            input,
                                            decoder,
                                            buildResourceSpec(),
                                            null);

    data.add(Arrays.asList(request1, request3, false));

    // old-new combinations

    Request<String> newConsRequest1 = new Request<String>(actionMethod,
                                                          input,
                                                          Collections.<String, String>emptyMap(),
                                                          decoder,
                                                          buildResourceSpec(),
                                                          Collections.<String, Object>emptyMap(),
                                                          "bar",
                                                          BASE_URI_TEMPLATE,
                                                          Collections.<String, Object>singletonMap("id", 1L),
                                                          RestliRequestOptions.DEFAULT_OPTIONS);

    data.add(Arrays.asList(newConsRequest1, request1, false));
    data.add(Arrays.asList(request1, newConsRequest1, false));
  }

  private void addDeleteRequestData(List<List<Object>> data)
      throws URISyntaxException
  {
    ResourceMethod method = ResourceMethod.DELETE;

    // new constructor

    DeleteRequest<EmptyRecord> deleteRequest1 =
        new DeleteRequest<EmptyRecord>(Collections.<String, String>emptyMap(),
                                       buildResourceSpec(),
                                       Collections.<String, Object>emptyMap(),
                                       BASE_URI_TEMPLATE,
                                       Collections.<String, Object>singletonMap("id", 1L),
                                       RestliRequestOptions.DEFAULT_OPTIONS,
                                       1L);

    DeleteRequest<EmptyRecord> deleteRequest2 =
        new DeleteRequest<EmptyRecord>(Collections.<String, String>emptyMap(),
                                       buildResourceSpec(),
                                       Collections.<String, Object>emptyMap(),
                                       BASE_URI_TEMPLATE,
                                       Collections.<String, Object>singletonMap("id", 1L),
                                       RestliRequestOptions.DEFAULT_OPTIONS,
                                       1L);

    data.add(Arrays.asList(deleteRequest1, deleteRequest2, true));

    DeleteRequest<EmptyRecord> deleteRequest3 =
        new DeleteRequest<EmptyRecord>(Collections.<String, String>emptyMap(),
                                       buildResourceSpec(),
                                       Collections.<String, Object>emptyMap(),
                                       BASE_URI_TEMPLATE,
                                       Collections.<String, Object>singletonMap("id", 2L),
                                       RestliRequestOptions.DEFAULT_OPTIONS,
                                       2L);

    data.add(Arrays.asList(deleteRequest1, deleteRequest3, false));

    // old constructor

    EmptyResponseDecoder decoder = new EmptyResponseDecoder();

    Request<EmptyRecord> request1 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"), method,
                                                 null,
                                                 decoder,
                                                 buildResourceSpec(),
                                                 new DataMap());
    Request<EmptyRecord> request2 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"), method,
                                                 null,
                                                 decoder,
                                                 buildResourceSpec(),
                                                 new DataMap());

    data.add(Arrays.asList(request1, request2, true));

    Request<EmptyRecord> request3 = buildRequest(new URI(BASE_URI_TEMPLATE + "/2"), method,
                                                 null,
                                                 decoder,
                                                 buildResourceSpec(),
                                                 new DataMap());

    data.add(Arrays.asList(request1, request3, false));

    // old-new combinations

    Request<EmptyRecord> newConsRequest1 =
        new Request<EmptyRecord>(method,
                                 null,
                                 Collections.<String, String>emptyMap(),
                                 decoder,
                                 buildResourceSpec(),
                                 Collections.<String, Object>emptyMap(),
                                 null,
                                 BASE_URI_TEMPLATE,
                                 Collections.<String, Object>singletonMap("id", 1L),
                                 RestliRequestOptions.DEFAULT_OPTIONS);

    data.add(Arrays.asList(request1, newConsRequest1, false));
    data.add(Arrays.asList(newConsRequest1, request1, false));
  }

  private void addFindRequestData(List<List<Object>> data)
      throws URISyntaxException
  {
    /*
    Finder name: bar
    Query params (for the finder): bar -> bar
    Association key: (baz -> baz)
     */

    ResourceMethod method = ResourceMethod.FINDER;

    // new constructor

    // we have to create these two objects because the Request constructor modifies the passed in map
    Map<String, Object> queryParams1 = new HashMap<String, Object>();
    queryParams1.put("bar", "bar");
    Map<String, Object> queryParams2 = new HashMap<String, Object>(queryParams1);

    FindRequest<RecordTemplateWithPrimitiveKey> findRequest1 =
        new FindRequest<RecordTemplateWithPrimitiveKey>(Collections.<String, String>emptyMap(),
                                                        RecordTemplateWithPrimitiveKey.class,
                                                        buildResourceSpec(),
                                                        queryParams1,
                                                        "bar",
                                                        BASE_URI_TEMPLATE,
                                                        Collections.<String, Object>emptyMap(),
                                                        RestliRequestOptions.DEFAULT_OPTIONS,
                                                        new CompoundKey().append("baz", "baz"));

    FindRequest<RecordTemplateWithPrimitiveKey> findRequest2 =
        new FindRequest<RecordTemplateWithPrimitiveKey>(Collections.<String, String>emptyMap(),
                                                        RecordTemplateWithPrimitiveKey.class,
                                                        buildResourceSpec(),
                                                        queryParams2,
                                                        "bar",
                                                        BASE_URI_TEMPLATE,
                                                        Collections.<String, Object>emptyMap(),
                                                        RestliRequestOptions.DEFAULT_OPTIONS,
                                                        new CompoundKey().append("baz", "baz"));

    data.add(Arrays.asList(findRequest1, findRequest2, true));

    Map<String, Object> queryParams3 = new HashMap<String, Object>(queryParams1);

    FindRequest<RecordTemplateWithPrimitiveKey> findRequest3 =
        new FindRequest<RecordTemplateWithPrimitiveKey>(Collections.<String, String>emptyMap(),
                                                        RecordTemplateWithPrimitiveKey.class,
                                                        buildResourceSpec(),
                                                        queryParams3,
                                                        "bar",
                                                        BASE_URI_TEMPLATE,
                                                        Collections.<String, Object>emptyMap(),
                                                        RestliRequestOptions.DEFAULT_OPTIONS,
                                                        new CompoundKey().append("baz", "BAZ"));

    data.add(Arrays.asList(findRequest1, findRequest3, false));

    // old constructor

    CollectionResponseDecoder<RecordTemplateWithPrimitiveKey> decoder =
        new CollectionResponseDecoder<RecordTemplateWithPrimitiveKey>(RecordTemplateWithPrimitiveKey.class);

    DataMap dataMapQueryParams1 = new DataMap();
    dataMapQueryParams1.put("bar", "bar");
    DataMap dataMapQueryParams2 = new DataMap(dataMapQueryParams1);

    Request<CollectionResponse<RecordTemplateWithPrimitiveKey>> request1 =
        buildRequest(new URI(BASE_URI_TEMPLATE + "/baz=baz?q=bar&bar=bar"),
                     method,
                     null,
                     decoder,
                     buildResourceSpec(),
                     dataMapQueryParams1);

    Request<CollectionResponse<RecordTemplateWithPrimitiveKey>> request2 =
        buildRequest(new URI(BASE_URI_TEMPLATE + "/baz=baz?q=bar&bar=bar"),
                     method,
                     null,
                     decoder,
                     buildResourceSpec(),
                     dataMapQueryParams2);

    data.add(Arrays.asList(request1, request2, true));

    DataMap dataMapQueryParams3 = new DataMap(dataMapQueryParams1);

    Request<CollectionResponse<RecordTemplateWithPrimitiveKey>> request3 =
        buildRequest(new URI(BASE_URI_TEMPLATE + "/baz=BAZ?q=bar&bar=bar"),
                     method,
                     null,
                     decoder,
                     buildResourceSpec(),
                     dataMapQueryParams3);

    data.add(Arrays.asList(request1, request3, false));

    // old-new combinations
    Request<CollectionResponse<RecordTemplateWithPrimitiveKey>> newConsRequest1 =
        new Request<CollectionResponse<RecordTemplateWithPrimitiveKey>>(method,
                                                                        null,
                                                                        Collections.<String, String>emptyMap(),
                                                                        decoder,
                                                                        buildResourceSpec(),
                                                                        queryParams1,
                                                                        "bar",
                                                                        BASE_URI_TEMPLATE,
                                                                        Collections.<String, Object>emptyMap(),
                                                                        RestliRequestOptions.DEFAULT_OPTIONS);
    data.add(Arrays.asList(request1, newConsRequest1, false));
    data.add(Arrays.asList(newConsRequest1, request1, false));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void addPartialUpdateRequestData(List<List<Object>> data)
      throws URISyntaxException
  {
    ResourceMethod method = ResourceMethod.PARTIAL_UPDATE;

    // new constructor

    RecordTemplateWithPrimitiveKey record1 = new RecordTemplateWithPrimitiveKey().setId(1L).setBody("foo");
    RecordTemplateWithPrimitiveKey record2 = new RecordTemplateWithPrimitiveKey().setId(1L).setBody("bar");
    PatchRequest<RecordTemplateWithPrimitiveKey> patchRequest = PatchGenerator.diff(record1, record2);

    PartialUpdateRequest<RecordTemplateWithPrimitiveKey> partialUpdateRequest1 =
        new PartialUpdateRequest<RecordTemplateWithPrimitiveKey>(patchRequest,
                                                                 Collections.<String, String>emptyMap(),
                                                                 buildResourceSpec(),
                                                                 Collections.<String, Object>emptyMap(),
                                                                 BASE_URI_TEMPLATE,
                                                                 Collections.<String, Object>singletonMap("id", 1L),
                                                                 RestliRequestOptions.DEFAULT_OPTIONS,
                                                                 1L);
    PartialUpdateRequest<RecordTemplateWithPrimitiveKey> partialUpdateRequest2 =
        new PartialUpdateRequest<RecordTemplateWithPrimitiveKey>(patchRequest,
                                                                 Collections.<String, String>emptyMap(),
                                                                 buildResourceSpec(),
                                                                 Collections.<String, Object>emptyMap(),
                                                                 BASE_URI_TEMPLATE,
                                                                 Collections.<String, Object>singletonMap("id", 1L),
                                                                 RestliRequestOptions.DEFAULT_OPTIONS,
                                                                 1L);

    data.add(Arrays.asList(partialUpdateRequest1, partialUpdateRequest2, true));

    PartialUpdateRequest<RecordTemplateWithPrimitiveKey> partialUpdateRequest3 =
        new PartialUpdateRequest<RecordTemplateWithPrimitiveKey>(patchRequest,
                                                                 Collections.<String, String>emptyMap(),
                                                                 buildResourceSpec(),
                                                                 Collections.<String, Object>emptyMap(),
                                                                 BASE_URI_TEMPLATE,
                                                                 Collections.<String, Object>singletonMap("id", 2L),
                                                                 RestliRequestOptions.DEFAULT_OPTIONS,
                                                                 2L);

    data.add(Arrays.asList(partialUpdateRequest1, partialUpdateRequest3, false));

    // old constructor
    RestResponseDecoder decoder = new EmptyResponseDecoder();

    Request<EmptyRecord> request1 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"),
                                                 method,
                                                 patchRequest,
                                                 decoder,
                                                 buildResourceSpec(),
                                                 null);
    Request<EmptyRecord> request2 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"),
                                                 method,
                                                 patchRequest,
                                                 decoder,
                                                 buildResourceSpec(),
                                                 null);

    data.add(Arrays.asList(request1, request2, true));

    Request<EmptyRecord> request3 = buildRequest(new URI(BASE_URI_TEMPLATE + "/2"),
                                                 method,
                                                 patchRequest,
                                                 decoder,
                                                 buildResourceSpec(),
                                                 null);

    data.add(Arrays.asList(request1, request3, false));

    // old-new combinations

    Request<EmptyRecord> newConsRequest1 = new Request<EmptyRecord>(method,
                                                                    patchRequest,
                                                                    Collections.<String, String>emptyMap(),
                                                                    decoder,
                                                                    buildResourceSpec(),
                                                                    Collections.<String, Object>emptyMap(),
                                                                    null,
                                                                    BASE_URI_TEMPLATE,
                                                                    Collections.<String, Object>singletonMap("id", 1L),
                                                                    RestliRequestOptions.DEFAULT_OPTIONS);

    data.add(Arrays.asList(newConsRequest1, request1, false));
    data.add(Arrays.asList(request1, newConsRequest1, false));
  }

  private void addUpdateRequestData(List<List<Object>> data)
      throws URISyntaxException
  {
    ResourceMethod method = ResourceMethod.UPDATE;

    // new constructor

    RecordTemplateWithPrimitiveKey record1 = new RecordTemplateWithPrimitiveKey().setId(1L).setBody("foo");
    RecordTemplateWithPrimitiveKey record2 = new RecordTemplateWithPrimitiveKey(record1.data());

    UpdateRequest<RecordTemplateWithPrimitiveKey> updateRequest1 =
        new UpdateRequest<RecordTemplateWithPrimitiveKey>(record1,
                                                          Collections.<String, String>emptyMap(),
                                                          buildResourceSpec(),
                                                          Collections.<String, Object>emptyMap(),
                                                          BASE_URI_TEMPLATE,
                                                          Collections.<String, Object>singletonMap("id", 1L),
                                                          RestliRequestOptions.DEFAULT_OPTIONS,
                                                          1L);
    UpdateRequest<RecordTemplateWithPrimitiveKey> updateRequest2 =
        new UpdateRequest<RecordTemplateWithPrimitiveKey>(record2,
                                                          Collections.<String, String>emptyMap(),
                                                          buildResourceSpec(),
                                                          Collections.<String, Object>emptyMap(),
                                                          BASE_URI_TEMPLATE,
                                                          Collections.<String, Object>singletonMap("id", 1L),
                                                          RestliRequestOptions.DEFAULT_OPTIONS,
                                                          1L);

    data.add(Arrays.asList(updateRequest1, updateRequest2, true));

    UpdateRequest<RecordTemplateWithPrimitiveKey> updateRequest3 =
        new UpdateRequest<RecordTemplateWithPrimitiveKey>(record2,
                                                          Collections.<String, String>emptyMap(),
                                                          buildResourceSpec(),
                                                          Collections.<String, Object>emptyMap(),
                                                          BASE_URI_TEMPLATE,
                                                          Collections.<String, Object>singletonMap("id", 2L),
                                                          RestliRequestOptions.DEFAULT_OPTIONS,
                                                          2L);

    data.add(Arrays.asList(updateRequest1, updateRequest3, false));

    // old constructor

    EmptyResponseDecoder decoder = new EmptyResponseDecoder();

    Request<EmptyRecord> request1 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"),
                                                 method,
                                                 record1,
                                                 decoder,
                                                 buildResourceSpec(),
                                                 null);
    Request<EmptyRecord> request2 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"),
                                                 method,
                                                 record2,
                                                 decoder,
                                                 buildResourceSpec(),
                                                 null);

    data.add(Arrays.asList(request1, request2, true));

    Request<EmptyRecord> request3 = buildRequest(new URI(BASE_URI_TEMPLATE + "/1"),
                                                 method,
                                                 new RecordTemplateWithPrimitiveKey(new DataMap(record1.data())).setBody("bar"),
                                                 decoder,
                                                 buildResourceSpec(),
                                                 null);

    data.add(Arrays.asList(request1, request3, false));

    // old-new combinations

    Request<EmptyRecord> newConsRequest1 = new Request<EmptyRecord>(method,
                                                                    record1,
                                                                    Collections.<String, String>emptyMap(),
                                                                    decoder,
                                                                    buildResourceSpec(),
                                                                    Collections.<String, Object>emptyMap(),
                                                                    null,
                                                                    BASE_URI_TEMPLATE,
                                                                    Collections.<String, Object>singletonMap("id", 1L),
                                                                    RestliRequestOptions.DEFAULT_OPTIONS);
    data.add(Arrays.asList(newConsRequest1, request1, false));
    data.add(Arrays.asList(request1, newConsRequest1, false));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void addRequestData(List<List<Object>> data)
  {
    // NOTE: the arguments to the Request constructor here might not make sense. The point is to verify that the equals
    //       method works and nothing more.

    ResourceMethod method = ResourceMethod.GET;
    RecordTemplate input = new RecordTemplateWithPrimitiveKey().setBody("foo").setId(1L);
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("foo", "bar");
    RestResponseDecoder decoder = new EntityResponseDecoder(GetRequest.class);
    ResourceSpec resourceSpec = buildResourceSpec();
    Map<String, Object> queryParams = new HashMap<String, Object>();
    queryParams.put("foo", "bar");
    String methodName = "baz";
    String baseUriTemplate = BASE_URI_TEMPLATE;
    Map<String, Object> pathKeys = new HashMap<String, Object>();
    pathKeys.put("foo", "bar");
    RestliRequestOptions requestOptions = RestliRequestOptions.DEFAULT_OPTIONS;

    Request request1 = new Request(method,
                                   input,
                                   headers,
                                   decoder,
                                   resourceSpec,
                                   queryParams,
                                   methodName,
                                   baseUriTemplate,
                                   pathKeys,
                                   requestOptions);

    Request request2 = new Request(ResourceMethod.ACTION,
                                   input,
                                   headers,
                                   decoder,
                                   resourceSpec,
                                   queryParams,
                                   methodName,
                                   baseUriTemplate,
                                   pathKeys,
                                   requestOptions);
    data.add(Arrays.asList(request1, request2, false));

    Request request3 = new Request(method,
                           new RecordTemplateWithPrimitiveKey().setBody("bar"),
                           headers,
                           decoder,
                           resourceSpec,
                           queryParams,
                           methodName,
                           baseUriTemplate,
                           pathKeys,
                           requestOptions);
    data.add(Arrays.asList(request1, request3, false));

    Request request4 = new Request(method,
                                   input,
                                   Collections.singletonMap("FOO", "BAR"),
                                   decoder,
                                   resourceSpec,
                                   queryParams,
                                   methodName,
                                   baseUriTemplate,
                                   pathKeys,
                                   requestOptions);
    data.add(Arrays.asList(request1, request4, false));

    ResourceSpec resourceSpec1 = new ResourceSpecImpl(EnumSet.of(ResourceMethod.GET,
                                                                 ResourceMethod.PARTIAL_UPDATE,
                                                                 ResourceMethod.UPDATE),
                                                      null,
                                                      null,
                                                      Long.class,
                                                      RecordTemplateWithPrimitiveKey.class,
                                                      Collections.<String, Object>emptyMap());
    Request request5 = new Request(method,
                                   input,
                                   headers,
                                   decoder,
                                   resourceSpec1,
                                   queryParams,
                                   methodName,
                                   baseUriTemplate,
                                   pathKeys,
                                   requestOptions);
    data.add(Arrays.asList(request1, request5, false));

    Map<String, Object> queryParams1 = new HashMap<String, Object>();
    queryParams1.put("FOO", "BAR");
    Request request6 = new Request(method,
                                   input,
                                   headers,
                                   decoder,
                                   resourceSpec,
                                   queryParams1,
                                   methodName,
                                   baseUriTemplate,
                                   pathKeys,
                                   requestOptions);
    data.add(Arrays.asList(request1, request6, false));

    Request request7 = new Request(method,
                                   input,
                                   headers,
                                   decoder,
                                   resourceSpec,
                                   queryParams,
                                   "BAZ",
                                   baseUriTemplate,
                                   pathKeys,
                                   requestOptions);
    data.add(Arrays.asList(request1, request7, false));

    Request request8 = new Request(method,
                                   input,
                                   headers,
                                   decoder,
                                   resourceSpec,
                                   queryParams,
                                   methodName,
                                   "http://localhost",
                                   pathKeys,
                                   requestOptions);
    data.add(Arrays.asList(request1, request8, false));

    Map<String, Object> pathKeys1 = new HashMap<String, Object>();
    pathKeys1.put("FOO", "BAR");
    Request request9 = new Request(method,
                                   input,
                                   headers,
                                   decoder,
                                   resourceSpec,
                                   queryParams,
                                   methodName,
                                   baseUriTemplate,
                                   pathKeys1,
                                   requestOptions);
    data.add(Arrays.asList(request1, request9, false));

    RestliRequestOptions requestOptions1 = new RestliRequestOptionsBuilder().
        setProtocolVersionOption(ProtocolVersionOption.FORCE_USE_LATEST).
        build();
    Request request10 = new Request(method,
                                   input,
                                   headers,
                                   decoder,
                                   resourceSpec,
                                   queryParams,
                                   methodName,
                                   baseUriTemplate,
                                   pathKeys,
                                   requestOptions1);
    data.add(Arrays.asList(request1, request10, false));
  }
}

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

package com.linkedin.restli.internal.server;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.RestLiResponseDataException;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import static com.linkedin.restli.common.ResourceMethod.ACTION;
import static com.linkedin.restli.common.ResourceMethod.BATCH_CREATE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_DELETE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_GET;
import static com.linkedin.restli.common.ResourceMethod.BATCH_PARTIAL_UPDATE;
import static com.linkedin.restli.common.ResourceMethod.BATCH_UPDATE;
import static com.linkedin.restli.common.ResourceMethod.CREATE;
import static com.linkedin.restli.common.ResourceMethod.FINDER;
import static com.linkedin.restli.common.ResourceMethod.GET;
import static com.linkedin.restli.common.ResourceMethod.GET_ALL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class TestAugmentedRestLiResponseData
{

  @Test(dataProvider = "responseDataProvider")
  public void testResponse(ResourceMethod type, boolean entityException, boolean collectionException,
                           boolean batchException) throws Exception
  {
    AugmentedRestLiResponseData responseData = new AugmentedRestLiResponseData.Builder(type).build();
    RecordTemplate rt = Foo.createFoo("foo", "bar");
    try
    {
      responseData.setEntityResponse(rt);
      if (entityException)
      {
        fail();
      }
    }
    catch (RestLiResponseDataException e)
    {
      if (!entityException)
      {
        throw e;
      }
    }
    try
    {
      responseData.setCollectionResponse(Arrays.asList(rt));
      if (collectionException)
      {
        fail();
      }
    }
    catch (RestLiResponseDataException e)
    {
      if (!collectionException)
      {
        throw e;
      }
    }
    try
    {
      responseData.setCollectionResponseCustomMetadata(rt);
      if (collectionException)
      {
        fail();
      }
    }
    catch (RestLiResponseDataException e)
    {
      if (!collectionException)
      {
        throw e;
      }
    }
    try
    {
      responseData.setCollectionResponsePaging(new CollectionMetadata());
      if (collectionException)
      {
        fail();
      }
    }
    catch (RestLiResponseDataException e)
    {
      if (!collectionException)
      {
        throw e;
      }
    }
    try
    {
      responseData.setBatchKeyResponseMap(ImmutableMap.of("foo", rt));
      if (batchException)
      {
        fail();
      }
    }
    catch (RestLiResponseDataException e)
    {
      if (!batchException)
      {
        throw e;
      }
    }
  }

  @Test(dataProvider = "builderDataProvider")
  public void testBuilder(ResourceMethod methodType, RecordTemplate entity, RestLiServiceException exception,
                          List<? extends RecordTemplate> entities, CollectionMetadata collectionPaging,
                          RecordTemplate collectionMetadata, Map<?, ? extends RecordTemplate> batchEntities,
                          Map<String, String> headers, HttpStatus status, boolean expectException)
  {
    AugmentedRestLiResponseData.Builder builder =
        new AugmentedRestLiResponseData.Builder(methodType).status(status).headers(headers);
    try
    {
      if (entity != null)
      {
        builder.entity(entity);
      }
      if (exception != null)
      {
        builder.serviceException(exception);
      }
      if (entities != null)
      {
        builder.collectionEntities(entities);
      }
      if (batchEntities != null)
      {
        builder.batchKeyEntityMap(batchEntities);
      }
      if (collectionMetadata != null)
      {
        builder.collectionCustomMetadata(collectionMetadata);
      }
      if (collectionPaging != null)
      {
        builder.collectionResponsePaging(collectionPaging);
      }
      AugmentedRestLiResponseData responseData = builder.build();
      if (expectException)
      {
        fail();
      }
      assertEquals(responseData.getStatus(), status);
      assertEquals(responseData.getHeaders(), headers);

      if (entity != null)
      {
        assertTrue(responseData.isEntityResponse());
        assertFalse(responseData.isErrorResponse());
        assertFalse(responseData.isBatchResponse());
        assertFalse(responseData.isCollectionResponse());
        assertEquals(responseData.getEntityResponse(), entity);
        assertNull(responseData.getServiceException());
        assertNull(responseData.getCollectionResponse());
        assertNull(responseData.getBatchResponseMap());
      }
      if (entities != null)
      {
        assertFalse(responseData.isEntityResponse());
        assertFalse(responseData.isErrorResponse());
        assertFalse(responseData.isBatchResponse());
        assertTrue(responseData.isCollectionResponse());
        assertEquals(responseData.getCollectionResponse(), entities);
        assertEquals(responseData.getCollectionResponseCustomMetadata(), collectionMetadata);
        assertEquals(responseData.getCollectionResponsePaging(), collectionPaging);
        assertNull(responseData.getServiceException());
        assertNull(responseData.getEntityResponse());
        assertNull(responseData.getBatchResponseMap());
      }
      if (batchEntities != null)
      {
        assertFalse(responseData.isEntityResponse());
        assertFalse(responseData.isErrorResponse());
        assertTrue(responseData.isBatchResponse());
        assertFalse(responseData.isCollectionResponse());
        assertEquals(responseData.getBatchResponseMap(), batchEntities);
        assertNull(responseData.getServiceException());
        assertNull(responseData.getEntityResponse());
        assertNull(responseData.getCollectionResponse());
      }
      if (exception != null)
      {
        assertFalse(responseData.isEntityResponse());
        assertTrue(responseData.isErrorResponse());
        assertFalse(responseData.isBatchResponse());
        assertFalse(responseData.isCollectionResponse());
        assertEquals(responseData.getServiceException(), exception);
        assertNull(responseData.getBatchResponseMap());
        assertNull(responseData.getEntityResponse());
        assertNull(responseData.getCollectionResponse());
      }
    }
    catch (IllegalArgumentException e)
    {
      if (!expectException)
      {
        throw e;
      }
    }
  }

  @DataProvider(name = "builderDataProvider")
  public Object[][] builderDataProvider()
  {
    List<List<?>> ret = new ArrayList<List<?>>();
    buildSimpleResponse(ret);
    buildCollectionResponseWithPagingAndCustomMetadata(ret);
    buildCollectionResponseWithoutPagingAndCustomMetadata(ret);
    buildBatchResponse(ret);
    buildExceptionResponse(ret);
    return listToArray(ret);
  }

  @DataProvider(name = "responseDataProvider")
  public Object[][] responseDataProvider()
  {
    List<List<?>> ret = new ArrayList<List<?>>();
    List<ResourceMethod> validEntityTypes = Arrays.asList(GET, ACTION, CREATE);
    List<ResourceMethod> validCollectionTypes = Arrays.asList(FINDER, GET_ALL, BATCH_CREATE);
    List<ResourceMethod> validBatchTypes = Arrays.asList(BATCH_DELETE, BATCH_GET, BATCH_PARTIAL_UPDATE, BATCH_UPDATE);
    for (ResourceMethod type : ResourceMethod.values())
    {
      ret.add(Arrays.<Object>asList(type, !validEntityTypes.contains(type), !validCollectionTypes.contains(type),
                                    !validBatchTypes.contains(type)));
    }
    return listToArray(ret);
  }


  private void buildExceptionResponse(List<List<? extends Object>> ret)
  {
    for (ResourceMethod type : ResourceMethod.values())
    {
      ret.add(Arrays.asList(type, null, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND), null, null, null, null,
                            ImmutableMap.of("key", "value"), HttpStatus.S_404_NOT_FOUND, false));
    }
  }

  private void buildSimpleResponse(List<List<? extends Object>> ret)
  {
    List<ResourceMethod> validTypes = Arrays.asList(GET, ACTION, CREATE);
    for (ResourceMethod type : ResourceMethod.values())
    {
      ret.add(Arrays.asList(type, Foo.createFoo("foo", "bar"), null, null, null, null, null,
                            ImmutableMap.of("key", "value"), HttpStatus.S_201_CREATED, !validTypes.contains(type)));
    }
  }

  private void buildCollectionResponseWithPagingAndCustomMetadata(List<List<? extends Object>> ret)
  {
    List<ResourceMethod> validTypes = Arrays.asList(FINDER, GET_ALL);
    for (ResourceMethod type : ResourceMethod.values())
    {
      ret.add(Arrays.asList(type, null, null, Arrays.asList(Foo.createFoo("foo", "bar")), new CollectionMetadata(),
                            Foo.createFoo("md", "val"), null, ImmutableMap.of("key", "value"),
                            HttpStatus.S_202_ACCEPTED, !validTypes.contains(type)));
    }
  }

  private void buildCollectionResponseWithoutPagingAndCustomMetadata(List<List<? extends Object>> ret)
  {
    List<ResourceMethod> validTypes = Arrays.asList(FINDER, GET_ALL, BATCH_CREATE);
    for (ResourceMethod type : ResourceMethod.values())
    {
      ret.add(Arrays.asList(type, null, null, Arrays.asList(Foo.createFoo("foo", "bar")), null,
                            null, null, ImmutableMap.of("key", "value"),
                            HttpStatus.S_202_ACCEPTED, !validTypes.contains(type)));
    }
  }

  private void buildBatchResponse(List<List<?>> ret)
  {
    List<ResourceMethod> validTypes = Arrays.asList(BATCH_DELETE, BATCH_GET, BATCH_PARTIAL_UPDATE, BATCH_UPDATE);
    for (ResourceMethod type : ResourceMethod.values())
    {
      ret.add(Arrays.asList(type, null, null, null, null, null, ImmutableMap.of("foo", Foo.createFoo("foo", "bar")),
                            ImmutableMap.of("key", "value"), HttpStatus.S_203_NON_AUTHORITATIVE_INFORMATION,
                            !validTypes.contains(type)));
    }
  }

  private Object[][] listToArray(List<List<?>> input)
  {

    Object[][] ret = new Object[input.size()][input.get(0).size()];
    for (int i = 0; i < input.size(); ++i)
    {
      ret[i] = input.get(i).toArray();
    }
    return ret;
  }

  private static class Foo extends RecordTemplate
  {
    private Foo(DataMap map)
    {
      super(map, null);
    }

    public static Foo createFoo(String key, String value)
    {
      DataMap dataMap = new DataMap();
      dataMap.put(key, value);
      return new Foo(dataMap);
    }
  }
}

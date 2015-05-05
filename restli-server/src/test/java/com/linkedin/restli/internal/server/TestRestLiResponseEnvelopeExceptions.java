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


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateCollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.CollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.EmptyResponseEnvelope;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestRestLiResponseEnvelopeExceptions
{
  @DataProvider(name = "restliResponseEnvelope")
  public Object[][] provideRestLiResponseEnvelope()
  {
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    RecordResponseEnvelope recordResponse = new RecordResponseEnvelope(exception, Collections.<String, String>emptyMap());
    CollectionResponseEnvelope collectionResponse = new CollectionResponseEnvelope(exception, Collections.<String, String>emptyMap());
    CreateCollectionResponseEnvelope collectionCreateResponse = new CreateCollectionResponseEnvelope(exception, Collections.<String, String>emptyMap());
    BatchResponseEnvelope batchResponse = new BatchResponseEnvelope(exception, Collections.<String, String>emptyMap());
    EmptyResponseEnvelope emptyResponse = new EmptyResponseEnvelope(exception, Collections.<String, String>emptyMap());

    return new Object[][]{
        {recordResponse},
        {collectionResponse},
        {collectionCreateResponse},
        {batchResponse},
        {emptyResponse}
    };
  }

  @Test(dataProvider = "restliResponseEnvelope")
  public void testUnsupportedOperations(RestLiResponseEnvelope data)
  {
    //Ensure that only the supported operations are permitted.
    ResponseType type = data.getResponseType();
    try
    {
      data.getRecordResponseEnvelope();
      if (type != ResponseType.SINGLE_ENTITY)
      {
        Assert.fail();
      }
      else
      {
        Assert.assertEquals(data.getRecordResponseEnvelope(), data);
      }
    }
    catch (UnsupportedOperationException e)
    {
      if (type == ResponseType.SINGLE_ENTITY) Assert.fail();
    }

    try
    {
      data.getCollectionResponseEnvelope();
      if (type != ResponseType.GET_COLLECTION)
      {
        Assert.fail();
      }
      else
      {
        Assert.assertEquals(data.getCollectionResponseEnvelope(), data);
      }
    }
    catch (UnsupportedOperationException e)
    {
      if (type == ResponseType.GET_COLLECTION) Assert.fail();
    }

    try
    {
      data.getCreateCollectionResponseEnvelope();
      if (type != ResponseType.CREATE_COLLECTION)
      {
        Assert.fail();
      }
      else
      {
        Assert.assertEquals(data.getCreateCollectionResponseEnvelope(), data);
      }
    }
    catch (UnsupportedOperationException e)
    {
      if (type == ResponseType.CREATE_COLLECTION) Assert.fail();
    }

    try
    {
      data.getBatchResponseEnvelope();
      if (type != ResponseType.BATCH_ENTITIES)
      {
        Assert.fail();
      }
      else
      {
        Assert.assertEquals(data.getBatchResponseEnvelope(), data);
      }
    }
    catch (UnsupportedOperationException e)
    {
      if (type == ResponseType.BATCH_ENTITIES) Assert.fail();
    }

    try
    {
      data.getEmptyResponseEnvelope();
      if (type != ResponseType.STATUS_ONLY)
      {
        Assert.fail();
      }
      else
      {
        Assert.assertEquals(data.getEmptyResponseEnvelope(), data);
      }
    }
    catch (UnsupportedOperationException e)
    {
      if (type == ResponseType.STATUS_ONLY) Assert.fail();
    }
  }
}

package com.linkedin.restli.internal.server;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateCollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.CollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.EmptyResponseEnvelope;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author erli
 */
public class TestRestLiResponseEnvelope
{
  private final RestLiServiceException exception500 = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  private final RestLiServiceException exception503 = new RestLiServiceException(HttpStatus.S_503_SERVICE_UNAVAILABLE);

  // Tests for the exception/status invariant of RestLiResponseEnvelope class.
  @Test (dataProvider = "baseClassOperations")
  public void testRestLiResponseEnvelopeInvariant(RestLiResponseEnvelope responseEnvelope)
  {
    // Headers
    Map<String, String> headers = new HashMap<String, String>();
    Assert.assertEquals(responseEnvelope.getHeaders(), headers);
    String headerKey = "testKey";
    String headerValue = "testValue";
    responseEnvelope.getHeaders().put(headerKey, headerValue);
    Assert.assertEquals(responseEnvelope.getHeaders().get(headerKey), headerValue);

    // Exceptions
    if (responseEnvelope.isErrorResponse())
    {
      Assert.assertNotNull(responseEnvelope.getServiceException());
      Assert.assertEquals(responseEnvelope.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
      responseEnvelope.setException(exception503);
      Assert.assertEquals(responseEnvelope.getServiceException().getStatus(), HttpStatus.S_503_SERVICE_UNAVAILABLE);

      // Make sure conversion to normal works
      responseEnvelope.setStatus(HttpStatus.S_200_OK);
      Assert.assertFalse(responseEnvelope.isErrorResponse());
      Assert.assertEquals(responseEnvelope.getStatus(), HttpStatus.S_200_OK);
    }
    else
    {
      Assert.assertNull(responseEnvelope.getServiceException());
      Assert.assertEquals(responseEnvelope.getStatus(), HttpStatus.S_200_OK);
      responseEnvelope.setStatus(HttpStatus.S_201_CREATED);
      Assert.assertEquals(responseEnvelope.getStatus(), HttpStatus.S_201_CREATED);
    }
  }

  @DataProvider(name = "baseClassOperations")
  public Object[][] provideAllBaseObjects()
  {
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    return new Object[][]
    {
      {new RecordResponseEnvelope(exception, Collections.<String, String>emptyMap())},
      {new BatchResponseEnvelope(exception, Collections.<String, String>emptyMap())},
      {new CreateCollectionResponseEnvelope(exception, Collections.<String, String>emptyMap())},
      {new CollectionResponseEnvelope(exception, Collections.<String, String>emptyMap())},
      {new EmptyResponseEnvelope(exception, Collections.<String, String>emptyMap())},

      {new RecordResponseEnvelope(HttpStatus.S_200_OK, new EmptyRecord(), Collections.<String, String>emptyMap())},
      {new BatchResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(), Collections.<String, String>emptyMap())},
      {new CreateCollectionResponseEnvelope(Collections.<CreateCollectionResponseEnvelope.CollectionCreateResponseItem>emptyList(), Collections.<String, String>emptyMap())},
      {new CollectionResponseEnvelope(Collections.<EmptyRecord>emptyList(), new CollectionMetadata(), null, Collections.<String, String>emptyMap())},
      {new EmptyResponseEnvelope(HttpStatus.S_200_OK, Collections.<String, String>emptyMap())}
    };
  }

  @Test
  public void testRecordResponseEnvelopeUpdates()
  {
    RecordResponseEnvelope record = new RecordResponseEnvelope(HttpStatus.S_200_OK, new EmptyRecord(), Collections.<String, String>emptyMap());
    Assert.assertFalse(record.isErrorResponse());
    Assert.assertEquals(record.getRecord(), new EmptyRecord());

    // Swap to exception
    record.setException(exception500);
    Assert.assertNull(record.getRecord());
    Assert.assertEquals(record.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    Assert.assertEquals(record.getServiceException(), exception500);

    // Swap back
    record = new RecordResponseEnvelope(HttpStatus.S_200_OK, new EmptyRecord(), Collections.<String, String>emptyMap());
    Assert.assertFalse(record.isErrorResponse());
    Assert.assertEquals(record.getRecord(), new EmptyRecord());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCollectionResponseEnvelopeUpdates()
  {
    CollectionResponseEnvelope response = new CollectionResponseEnvelope(Collections.<EmptyRecord>emptyList(),
                                                                         new CollectionMetadata(),
                                                                         new EmptyRecord(),
                                                                         Collections.<String, String>emptyMap());
    Assert.assertFalse(response.isErrorResponse());
    Assert.assertEquals(response.getCollectionResponse(), Collections.<EmptyRecord>emptyList());
    Assert.assertEquals(response.getCollectionResponsePaging(), new CollectionMetadata());
    Assert.assertEquals(response.getCollectionResponseCustomMetadata(), new EmptyRecord());

    // Swap to exception
    response.setException(exception500);
    Assert.assertNull(response.getCollectionResponse());
    Assert.assertNull(response.getCollectionResponseCustomMetadata());
    Assert.assertNull(response.getCollectionResponsePaging());
    Assert.assertEquals(response.getServiceException(), exception500);
    Assert.assertEquals(response.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    // Swap back
    response.setCollectionResponse(HttpStatus.S_200_OK,
                                   new ArrayList<RecordTemplate>(),
                                   new CollectionMetadata(),
                                   new EmptyRecord());
    Assert.assertFalse(response.isErrorResponse());
    Assert.assertEquals(response.getCollectionResponse(), Collections.<EmptyRecord>emptyList());
    Assert.assertEquals(response.getCollectionResponsePaging(), new CollectionMetadata());
    Assert.assertEquals(response.getCollectionResponseCustomMetadata(), new EmptyRecord());

    // Check mutability when available
    List<EmptyRecord> temp = (List<EmptyRecord>) response.getCollectionResponse();
    temp.add(new EmptyRecord());
    Assert.assertEquals(response.getCollectionResponse().size(), 1);
  }

  @Test
  public void testCreateCollectionResponseEnvelopeUpdates()
  {
    CreateCollectionResponseEnvelope response = new CreateCollectionResponseEnvelope(Collections.<CreateCollectionResponseEnvelope.CollectionCreateResponseItem>emptyList(),
                                                                                     Collections.<String, String>emptyMap());
    Assert.assertNull(response.getServiceException());
    Assert.assertEquals(response.getCreateResponses(), Collections.emptyList());
    Assert.assertFalse(response.isErrorResponse());

    response.setException(exception500);
    Assert.assertNull(response.getCreateResponses());

    response.setCreateResponse(HttpStatus.S_200_OK, new ArrayList<CreateCollectionResponseEnvelope.CollectionCreateResponseItem>());
    Assert.assertNull(response.getServiceException());

    Assert.assertEquals(response.getCreateResponses().size(), 0);
    response.getCreateResponses().add(new CreateCollectionResponseEnvelope.CollectionCreateResponseItem(new CreateIdStatus<Object>(new DataMap(), new Object())));
    response.getCreateResponses().add(new CreateCollectionResponseEnvelope.CollectionCreateResponseItem(exception500, 2));
    Assert.assertEquals(response.getCreateResponses().size(), 2);
  }

  @Test
  public void testBatchResponseEnvelopeUpdates()
  {
    BatchResponseEnvelope response = new BatchResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(), Collections.<String, String>emptyMap());
    Assert.assertFalse(response.isErrorResponse());
    Assert.assertNull(response.getServiceException());

    response.setException(exception500);
    Assert.assertNull(response.getBatchResponseMap());

    Map<Object, BatchResponseEnvelope.BatchResponseEntry> targetMap = new HashMap<Object, BatchResponseEnvelope.BatchResponseEntry>();
    response.setBatchResponseMap(HttpStatus.S_200_OK, targetMap);
    Assert.assertNull(response.getServiceException());
    targetMap.put("key", new BatchResponseEnvelope.BatchResponseEntry(null, new EmptyRecord()));
    Assert.assertEquals(response.getBatchResponseMap().size(), 1);
    Assert.assertEquals(response.getBatchResponseMap().get("key").getRecord(), new EmptyRecord());
  }
}
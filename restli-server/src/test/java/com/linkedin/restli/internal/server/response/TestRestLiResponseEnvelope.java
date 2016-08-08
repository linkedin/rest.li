package com.linkedin.restli.internal.server.response;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.server.RestLiServiceException;
import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestRestLiResponseEnvelope
{
  @Test(dataProvider = "resourceMethodProvider")
  public void testBuildBlankResponseEnvelope(ResourceMethod resourceMethod)
  {
    RestLiResponseEnvelope responseEnvelope = EnvelopeBuilderUtil.buildBlankResponseEnvelope(resourceMethod, null);
    Assert.assertNotNull(responseEnvelope);
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);

    switch (responseType)
    {
      case SINGLE_ENTITY:
        RecordResponseEnvelope recordResponseEnvelope = (RecordResponseEnvelope)responseEnvelope;
        Assert.assertNotNull(recordResponseEnvelope.getRecord());
        Assert.assertTrue(recordResponseEnvelope.getRecord().getClass().isAssignableFrom(EmptyRecord.class));
        break;
      case GET_COLLECTION:
        CollectionResponseEnvelope collectionResponseEnvelope = (CollectionResponseEnvelope)responseEnvelope;
        Assert.assertNotNull(collectionResponseEnvelope.getCollectionResponse());
        Assert.assertNotNull(collectionResponseEnvelope.getCollectionResponseCustomMetadata());
        Assert.assertNull(collectionResponseEnvelope.getCollectionResponsePaging());
        Assert.assertTrue(collectionResponseEnvelope.getCollectionResponse().isEmpty());
        Assert.assertTrue(collectionResponseEnvelope.getCollectionResponseCustomMetadata().getClass()
                              .isAssignableFrom(EmptyRecord.class));
        break;
      case CREATE_COLLECTION:
        BatchCreateResponseEnvelope batchCreateResponseEnvelope =
            (BatchCreateResponseEnvelope)responseEnvelope;
        Assert.assertNotNull(batchCreateResponseEnvelope.getCreateResponses());
        Assert.assertTrue(batchCreateResponseEnvelope.getCreateResponses().isEmpty());
        break;
      case BATCH_ENTITIES:
        BatchResponseEnvelope batchResponseEnvelope = (BatchResponseEnvelope)responseEnvelope;
        Assert.assertNotNull(batchResponseEnvelope.getBatchResponseMap());
        Assert.assertTrue(batchResponseEnvelope.getBatchResponseMap().isEmpty());
        break;
      case STATUS_ONLY:
        // status only envelopes are blank by default since they have no data fields
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @DataProvider
  private Object[][] resourceMethodProvider()
  {
    ResourceMethod[] resourceMethods = ResourceMethod.values();
    Object[][] resourceMethodData = new Object[resourceMethods.length][1];
    for (int i = 0; i < resourceMethodData.length; i++)
    {
      resourceMethodData[i][0] = resourceMethods[i];
    }
    return resourceMethodData;
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testEnvelopeResourceMethodType(RestLiResponseEnvelope responseEnvelope, ResourceMethod resourceMethod)
  {
    Assert.assertEquals(responseEnvelope.getResourceMethod(), resourceMethod);
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testEnvelopeResponseType(RestLiResponseEnvelope responseEnvelope, ResourceMethod resourceMethod)
  {
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);
    Assert.assertEquals(responseEnvelope.getResponseType(), responseType);
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testSetNewEnvelopeData(RestLiResponseEnvelope responseEnvelope, ResourceMethod resourceMethod)
  {
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);

    switch (responseType)
    {
      case SINGLE_ENTITY:
        RecordResponseEnvelope recordResponseEnvelope = (RecordResponseEnvelope)responseEnvelope;
        RecordTemplate oldRecord = recordResponseEnvelope.getRecord();
        RecordTemplate newRecord = new AnyRecord(new DataMap());
        newRecord.data().put("test", "testing");
        recordResponseEnvelope.setRecord(newRecord, HttpStatus.S_200_OK);
        Assert.assertNotEquals(recordResponseEnvelope.getRecord(), oldRecord);
        break;
      case GET_COLLECTION:
        CollectionResponseEnvelope collectionResponseEnvelope = (CollectionResponseEnvelope)responseEnvelope;
        List<? extends RecordTemplate> oldResponses = collectionResponseEnvelope.getCollectionResponse();
        RecordTemplate oldResponseMetadata = collectionResponseEnvelope.getCollectionResponseCustomMetadata();
        CollectionMetadata oldPagingMetadata = collectionResponseEnvelope.getCollectionResponsePaging();

        RecordTemplate newResponseMetadata = new AnyRecord(new DataMap());
        newResponseMetadata.data().put("test", "testing");
        CollectionMetadata newResponsesPaging = new CollectionMetadata();
        List<? extends RecordTemplate> newResponses = Arrays.asList(new AnyRecord(new DataMap()));

        collectionResponseEnvelope.setCollectionResponse(newResponses,
                                                         newResponsesPaging,
                                                         newResponseMetadata,
                                                         HttpStatus.S_200_OK);

        Assert.assertNotEquals(collectionResponseEnvelope.getCollectionResponse(), oldResponses);
        Assert.assertNotEquals(collectionResponseEnvelope.getCollectionResponseCustomMetadata(), oldResponseMetadata);
        Assert.assertNotEquals(collectionResponseEnvelope.getCollectionResponsePaging(), oldPagingMetadata);

        Assert.assertEquals(collectionResponseEnvelope.getCollectionResponse(), newResponses);
        Assert.assertEquals(collectionResponseEnvelope.getCollectionResponseCustomMetadata(), newResponseMetadata);
        Assert.assertEquals(collectionResponseEnvelope.getCollectionResponsePaging(), newResponsesPaging);
        break;
      case CREATE_COLLECTION:
        BatchCreateResponseEnvelope batchCreateResponseEnvelope = (BatchCreateResponseEnvelope)responseEnvelope;
        List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> oldCreateResponses =
            batchCreateResponseEnvelope.getCreateResponses();

        CreateIdStatus<String> newCreateIdStatus = new CreateIdStatus<String>(new DataMap(), "key");
        RestLiServiceException newException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        BatchCreateResponseEnvelope.CollectionCreateResponseItem successCreateItem
            = new BatchCreateResponseEnvelope.CollectionCreateResponseItem(newCreateIdStatus);
        BatchCreateResponseEnvelope.CollectionCreateResponseItem exceptionCreateItem
            = new BatchCreateResponseEnvelope.CollectionCreateResponseItem(newException, "id2");

        List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> newCreateResponses =
            Arrays.asList(successCreateItem, exceptionCreateItem);

        batchCreateResponseEnvelope.setCreateResponse(newCreateResponses, HttpStatus.S_200_OK);

        Assert.assertNotEquals(batchCreateResponseEnvelope.getCreateResponses(), oldCreateResponses);
        Assert.assertEquals(batchCreateResponseEnvelope.getCreateResponses(), newCreateResponses);

        BatchCreateResponseEnvelope.CollectionCreateResponseItem firstItem =
            batchCreateResponseEnvelope.getCreateResponses().get(0);
        Assert.assertNull(firstItem.getId());
        Assert.assertEquals(firstItem.getRecord(), newCreateIdStatus);
        Assert.assertFalse(firstItem.isErrorResponse());
        Assert.assertNull(firstItem.getException());

        BatchCreateResponseEnvelope.CollectionCreateResponseItem secondItem =
            batchCreateResponseEnvelope.getCreateResponses().get(1);
        Assert.assertEquals(secondItem.getId(), "id2");
        Assert.assertNull(secondItem.getRecord());
        Assert.assertTrue(secondItem.isErrorResponse());
        Assert.assertEquals(secondItem.getException(), newException);
        break;
      case BATCH_ENTITIES:
        BatchResponseEnvelope batchResponseEnvelope = (BatchResponseEnvelope)responseEnvelope;
        Map<?, BatchResponseEnvelope.BatchResponseEntry> oldBatchResponses =
            batchResponseEnvelope.getBatchResponseMap();

        RecordTemplate newResponseRecord = new EmptyRecord();
        RestLiServiceException newResponseException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        Map<String, BatchResponseEnvelope.BatchResponseEntry> newBatchResponses =
            new HashMap<String, BatchResponseEnvelope.BatchResponseEntry>();
        newBatchResponses.put("id1", new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK,
                                                                                  newResponseRecord));
        newBatchResponses.put("id2",
                              new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                                           newResponseException));


        batchResponseEnvelope.setBatchResponseMap(newBatchResponses, HttpStatus.S_200_OK);

        Map<?, BatchResponseEnvelope.BatchResponseEntry> envelopeMap = batchResponseEnvelope.getBatchResponseMap();
        Assert.assertNotEquals(envelopeMap, oldBatchResponses);
        Assert.assertEquals(envelopeMap, newBatchResponses);

        BatchResponseEnvelope.BatchResponseEntry id1Entry = envelopeMap.get("id1");
        Assert.assertEquals(id1Entry.getStatus(), HttpStatus.S_200_OK);
        Assert.assertEquals(id1Entry.getRecord(), newResponseRecord);
        Assert.assertFalse(id1Entry.hasException());
        Assert.assertNull(id1Entry.getException());

        BatchResponseEnvelope.BatchResponseEntry id2Entry = envelopeMap.get("id2");
        Assert.assertEquals(id2Entry.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        Assert.assertNull(id2Entry.getRecord());
        Assert.assertTrue(id2Entry.hasException());
        Assert.assertEquals(id2Entry.getException(), newResponseException);
        break;
      case STATUS_ONLY:
        // status only envelopes are blank by default since they have no data fields
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @Test(dataProvider = "resourceMethodProvider")
  public void testEnvelopeSetDataNull(ResourceMethod resourceMethod)
  {
    // create an envelope and set all the data to null
    RestLiResponseEnvelope responseEnvelope = EnvelopeBuilderUtil.buildBlankResponseEnvelope(resourceMethod, null);
    responseEnvelope.clearData();
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);

    // extract the correct response envelope based on the data type and verify the data fields are all null
    switch (responseType)
    {
      case SINGLE_ENTITY:
        RecordResponseEnvelope recordResponseEnvelope = (RecordResponseEnvelope)responseEnvelope;
        Assert.assertNull(recordResponseEnvelope.getRecord());
        break;
      case GET_COLLECTION:
        CollectionResponseEnvelope collectionResponseEnvelope = (CollectionResponseEnvelope)responseEnvelope;
        Assert.assertNull(collectionResponseEnvelope.getCollectionResponse());
        Assert.assertNull(collectionResponseEnvelope.getCollectionResponseCustomMetadata());
        Assert.assertNull(collectionResponseEnvelope.getCollectionResponsePaging());
        break;
      case CREATE_COLLECTION:
        BatchCreateResponseEnvelope batchCreateResponseEnvelope =
            (BatchCreateResponseEnvelope)responseEnvelope;
        Assert.assertNull(batchCreateResponseEnvelope.getCreateResponses());
        break;
      case BATCH_ENTITIES:
        BatchResponseEnvelope batchResponseEnvelope = (BatchResponseEnvelope)responseEnvelope;
        Assert.assertNull(batchResponseEnvelope.getBatchResponseMap());
        break;
      case STATUS_ONLY:
        // status only envelopes don't have data fields
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @DataProvider
  private Object[][] envelopeResourceMethodDataProvider()
  {
    RestLiResponseDataImpl responseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                     Collections.<String, String>emptyMap(),
                                                                     Collections.<HttpCookie>emptyList());
    ResourceMethod[] resourceMethods = ResourceMethod.values();
    Object[][] envelopeResourceMethods = new Object[resourceMethods.length][2];
    for (int i = 0; i < resourceMethods.length; i++)
    {
      RestLiResponseEnvelope responseEnvelope = EnvelopeBuilderUtil.buildBlankResponseEnvelope(resourceMethods[i],
                                                                                               responseData);
      responseData.setResponseEnvelope(responseEnvelope);
      envelopeResourceMethods[i][0] = responseEnvelope;
      envelopeResourceMethods[i][1] = resourceMethods[i];
    }
    return envelopeResourceMethods;
  }
}

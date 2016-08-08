package com.linkedin.restli.internal.server.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.HttpCookie;
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
 * @author gye
 */
public class TestRestLiResponseData
{
  private final RestLiServiceException exception500 = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  private final RestLiServiceException exception503 = new RestLiServiceException(HttpStatus.S_503_SERVICE_UNAVAILABLE);

  // Tests for the exception/status invariant of RestLiResponseEnvelope class.
  @Test (dataProvider = "baseClassOperations")
  public void testRestLiResponseEnvelopeInvariant(RestLiResponseDataImpl responseData)
  {
    // Headers
    Map<String, String> headers = new HashMap<String, String>();
    Assert.assertEquals(responseData.getHeaders(), headers);
    String headerKey = "testKey";
    String headerValue = "testValue";
    responseData.getHeaders().put(headerKey, headerValue);
    Assert.assertEquals(responseData.getHeaders().get(headerKey), headerValue);

    // Cookies
    List<HttpCookie> cookies = Collections.<HttpCookie>emptyList();
    Assert.assertEquals(responseData.getCookies(), cookies);

    // Exceptions
    if (responseData.isErrorResponse())
    {
      Assert.assertNotNull(responseData.getServiceException());
      Assert.assertEquals(responseData.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
      responseData.setException(exception503);
      Assert.assertEquals(responseData.getServiceException().getStatus(), HttpStatus.S_503_SERVICE_UNAVAILABLE);

      // Make sure conversion to normal works
      responseData.setStatus(HttpStatus.S_200_OK);
      Assert.assertFalse(responseData.isErrorResponse());
      Assert.assertEquals(responseData.getStatus(), HttpStatus.S_200_OK);
    }
    else
    {
      Assert.assertNull(responseData.getServiceException());
      Assert.assertEquals(responseData.getStatus(), HttpStatus.S_200_OK);
      responseData.setStatus(HttpStatus.S_201_CREATED);
      Assert.assertEquals(responseData.getStatus(), HttpStatus.S_201_CREATED);
    }
  }

  @DataProvider(name = "baseClassOperations")
  public Object[][] provideAllBaseObjects()
  {
    RestLiServiceException exception = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    ResourceMethod[] methods = ResourceMethod.class.getEnumConstants();

    Object[][] sampleResponseData = new Object[methods.length * 2][1];
    for (int i = 0; i < methods.length; i++)
    {
      RestLiResponseDataImpl successResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                              Collections.<String, String>emptyMap(),
                                                                              Collections.<HttpCookie>emptyList());
      RestLiResponseDataImpl errorResponseData = new RestLiResponseDataImpl(exception,
                                                                            Collections.<String, String>emptyMap(),
                                                                            Collections.<HttpCookie>emptyList());
      successResponseData.setResponseEnvelope(EnvelopeBuilderUtil.buildBlankResponseEnvelope(methods[i], successResponseData));
      errorResponseData.setResponseEnvelope(EnvelopeBuilderUtil.buildBlankResponseEnvelope(methods[i], errorResponseData));

      sampleResponseData[i * 2] = new Object[] { successResponseData };
      sampleResponseData[i * 2 + 1] = new Object[] { errorResponseData };
    }

    return sampleResponseData;
  }

  @Test(dataProvider = "recordResponseEnvelopesProvider")
  public void testRecordResponseEnvelopeUpdates(RestLiResponseDataImpl responseData)
  {

    RecordResponseEnvelope recordResponseEnvelope = responseData.getRecordResponseEnvelope();

    Assert.assertFalse(responseData.isErrorResponse());
    Assert.assertEquals(recordResponseEnvelope.getRecord(), new EmptyRecord());

    // Swap to exception
    responseData.setException(exception500);
    Assert.assertNull(recordResponseEnvelope.getRecord());
    Assert.assertEquals(responseData.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    Assert.assertEquals(responseData.getServiceException(), exception500);
    Assert.assertEquals(responseData.getServiceException(), exception500);

    // Swap back
    recordResponseEnvelope.setRecord(new EmptyRecord(), HttpStatus.S_200_OK);

    Assert.assertFalse(responseData.isErrorResponse());
    Assert.assertEquals(recordResponseEnvelope.getRecord(), new EmptyRecord());
  }

  @DataProvider
  public static Object[][] recordResponseEnvelopesProvider()
  {
    RestLiResponseDataImpl getResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                     Collections.<String, String>emptyMap(),
                                                                     Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl createResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                           Collections.<String, String>emptyMap(),
                                                                           Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl actionResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                           Collections.<String, String>emptyMap(),
                                                                           Collections.<HttpCookie>emptyList());
    getResponseData.setResponseEnvelope(new GetResponseEnvelope(new EmptyRecord(), getResponseData));
    createResponseData.setResponseEnvelope(new CreateResponseEnvelope(new EmptyRecord(), createResponseData));
    actionResponseData.setResponseEnvelope(new ActionResponseEnvelope(new EmptyRecord(), actionResponseData));
    return new Object[][]{
        { getResponseData },
        { createResponseData },
        { actionResponseData }
    };
  }

  @Test(dataProvider = "collectionResponseEnvelopesProvider")
  @SuppressWarnings("unchecked")
  public void testCollectionResponseEnvelopeUpdates(RestLiResponseDataImpl responseData)
  {
    CollectionResponseEnvelope responseEnvelope = responseData.getCollectionResponseEnvelope();

    Assert.assertFalse(responseData.isErrorResponse());
    Assert.assertEquals(responseEnvelope.getCollectionResponse(), Collections.<EmptyRecord>emptyList());
    Assert.assertEquals(responseEnvelope.getCollectionResponsePaging(), new CollectionMetadata());
    Assert.assertEquals(responseEnvelope.getCollectionResponseCustomMetadata(), new EmptyRecord());

    // Swap to exception
    responseData.setException(exception500);
    Assert.assertNull(responseEnvelope.getCollectionResponse());
    Assert.assertNull(responseEnvelope.getCollectionResponseCustomMetadata());
    Assert.assertNull(responseEnvelope.getCollectionResponsePaging());
    Assert.assertEquals(responseData.getServiceException(), exception500);
    Assert.assertEquals(responseData.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    // Swap back
    responseEnvelope.setCollectionResponse(new ArrayList<RecordTemplate>(), new CollectionMetadata(), new EmptyRecord(),
                                           HttpStatus.S_200_OK);
    Assert.assertFalse(responseData.isErrorResponse());
    Assert.assertEquals(responseEnvelope.getCollectionResponse(), Collections.<EmptyRecord>emptyList());
    Assert.assertEquals(responseEnvelope.getCollectionResponsePaging(), new CollectionMetadata());
    Assert.assertEquals(responseEnvelope.getCollectionResponseCustomMetadata(), new EmptyRecord());

    // Check mutability when available
    List<EmptyRecord> temp = (List<EmptyRecord>) responseEnvelope.getCollectionResponse();
    temp.add(new EmptyRecord());
    Assert.assertEquals(responseEnvelope.getCollectionResponse().size(), 1);
  }

  @DataProvider
  public static Object[][] collectionResponseEnvelopesProvider()
  {
    RestLiResponseDataImpl getAllResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                           Collections.<String, String>emptyMap(),
                                                                           Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl finderResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                           Collections.<String, String>emptyMap(),
                                                                           Collections.<HttpCookie>emptyList());
    getAllResponseData.setResponseEnvelope(new GetAllResponseEnvelope(Collections.<EmptyRecord>emptyList(),
                                                                      new CollectionMetadata(),
                                                                      new EmptyRecord(), getAllResponseData));
    finderResponseData.setResponseEnvelope(new FinderResponseEnvelope(Collections.<EmptyRecord>emptyList(),
                                                                      new CollectionMetadata(),
                                                                      new EmptyRecord(), finderResponseData));
    return new Object[][]{
        { getAllResponseData },
        { finderResponseData }
    };
  }

  @Test(dataProvider = "createCollectionResponseEnvelopesProvider")
  public void testCreateCollectionResponseEnvelopeUpdates(RestLiResponseDataImpl responseData)
  {

    BatchCreateResponseEnvelope responseEnvelope = responseData.getBatchCreateResponseEnvelope();

    Assert.assertNull(responseData.getServiceException());
    Assert.assertEquals(responseEnvelope.getCreateResponses(), Collections.emptyList());
    Assert.assertFalse(responseData.isErrorResponse());

    responseData.setException(exception500);
    Assert.assertNull(responseEnvelope.getCreateResponses());

    responseEnvelope.setCreateResponse(new ArrayList<BatchCreateResponseEnvelope.CollectionCreateResponseItem>(),
                                       HttpStatus.S_200_OK);
    Assert.assertNull(responseData.getServiceException());

    Assert.assertEquals(responseEnvelope.getCreateResponses().size(), 0);
    responseEnvelope.getCreateResponses()
        .add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(
            new CreateIdStatus<Object>(new DataMap(), new Object())));
    responseEnvelope.getCreateResponses()
        .add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(exception500, 2));
    Assert.assertEquals(responseEnvelope.getCreateResponses().size(), 2);
  }

  @DataProvider
  private Object[][] createCollectionResponseEnvelopesProvider()
  {
    RestLiResponseDataImpl responseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                     Collections.<String, String>emptyMap(),
                                                                     Collections.<HttpCookie>emptyList());
    responseData.setResponseEnvelope(
        new BatchCreateResponseEnvelope(Collections.<BatchCreateResponseEnvelope.CollectionCreateResponseItem>emptyList(),
                                        responseData));
    return new Object[][] {
        { responseData }
    };
  }

  @Test(dataProvider = "batchResponseEnvelopesProvider")
  public void testBatchResponseEnvelopeUpdates(RestLiResponseDataImpl responseData)
  {
    BatchResponseEnvelope responseEnvelope = responseData.getBatchResponseEnvelope();

    Assert.assertFalse(responseData.isErrorResponse());
    Assert.assertNull(responseData.getServiceException());

    responseData.setException(exception500);
    Assert.assertNull(responseEnvelope.getBatchResponseMap());

    Map<Object, BatchResponseEnvelope.BatchResponseEntry> targetMap = new HashMap<Object, BatchResponseEnvelope.BatchResponseEntry>();
    responseEnvelope.setBatchResponseMap(targetMap, HttpStatus.S_200_OK);
    Assert.assertNull(responseData.getServiceException());
    targetMap.put("key", new BatchResponseEnvelope.BatchResponseEntry(null, new EmptyRecord()));
    Assert.assertEquals(responseEnvelope.getBatchResponseMap().size(), 1);
    Assert.assertEquals(responseEnvelope.getBatchResponseMap().get("key").getRecord(), new EmptyRecord());
  }

  @DataProvider
  public static Object[][] batchResponseEnvelopesProvider()
  {
    RestLiResponseDataImpl batchGetResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                             Collections.<String, String>emptyMap(),
                                                                             Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl batchUpdateResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                                Collections.<String, String>emptyMap(),
                                                                                Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl batchPartialUpdateResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                                       Collections.<String, String>emptyMap(),
                                                                                       Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl batchDeleteResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                                Collections.<String, String>emptyMap(),
                                                                                Collections.<HttpCookie>emptyList());
    batchGetResponseData.setResponseEnvelope(
        new BatchGetResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(),
                                     batchGetResponseData));
    batchUpdateResponseData.setResponseEnvelope(
        new BatchUpdateResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(),
                                        batchUpdateResponseData));
    batchPartialUpdateResponseData.setResponseEnvelope(
        new BatchPartialUpdateResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(),
                                               batchPartialUpdateResponseData));
    batchDeleteResponseData.setResponseEnvelope(
        new BatchDeleteResponseEnvelope(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(),
                                        batchDeleteResponseData));
    return new Object[][] {
        { batchGetResponseData },
        { batchUpdateResponseData },
        { batchPartialUpdateResponseData },
        { batchDeleteResponseData }
    };
  }

  @Test(dataProvider = "emptyResponseEnvelopesProvider")
  public void testEmptyResponseEnvelopeUpdates(RestLiResponseDataImpl responseData)
  {
    Assert.assertFalse(responseData.isErrorResponse());

    responseData.setException(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    Assert.assertTrue(responseData.isErrorResponse());
    Assert.assertEquals(responseData.getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    responseData.setStatus(HttpStatus.S_200_OK);
    Assert.assertFalse(responseData.isErrorResponse());
    Assert.assertEquals(responseData.getStatus(), HttpStatus.S_200_OK);
  }

  @DataProvider
  private Object[][] emptyResponseEnvelopesProvider()
  {
    RestLiResponseDataImpl partialUpdateResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                                  Collections.<String, String>emptyMap(),
                                                                                  Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl updateResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                               Collections.<String, String>emptyMap(),
                                                                               Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl deleteResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                               Collections.<String, String>emptyMap(),
                                                                               Collections.<HttpCookie>emptyList());
    RestLiResponseDataImpl optionsResponseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                            Collections.<String, String>emptyMap(),
                                                                            Collections.<HttpCookie>emptyList());
    partialUpdateResponseData.setResponseEnvelope(new UpdateResponseEnvelope(partialUpdateResponseData));
    updateResponseData.setResponseEnvelope(new UpdateResponseEnvelope(updateResponseData));
    deleteResponseData.setResponseEnvelope(new DeleteResponseEnvelope(deleteResponseData));
    optionsResponseData.setResponseEnvelope(new OptionsResponseEnvelope(optionsResponseData));
    return new Object[][] {
        { partialUpdateResponseData },
        { updateResponseData },
        { deleteResponseData },
        { optionsResponseData }
    };
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testResourceMethod(RestLiResponseData responseData, ResourceMethod resourceMethod)
  {
    Assert.assertEquals(responseData.getResourceMethod(), resourceMethod);
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testResponseType(RestLiResponseData responseData, ResourceMethod resourceMethod)
  {
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);
    Assert.assertEquals(responseData.getResponseType(), responseType);
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testSetNullExceptions(RestLiResponseDataImpl responseData, ResourceMethod resourceMethod)
  {
    try
    {
      responseData.setException(null);
      Assert.fail();
    }
    catch (UnsupportedOperationException e)
    {
      // expected
    }
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testSetNullStatus(RestLiResponseData responseData, ResourceMethod resourceMethod)
  {
    ResponseType responseType = ResponseType.fromMethodType(resourceMethod);
    try
    {
      switch (responseType)
      {
        case SINGLE_ENTITY:
          responseData.getRecordResponseEnvelope().setRecord(new EmptyRecord(), null);
          Assert.fail();
          break;
        case BATCH_ENTITIES:
          responseData.getBatchResponseEnvelope()
              .setBatchResponseMap(Collections.<Object, BatchResponseEnvelope.BatchResponseEntry>emptyMap(), null);
          Assert.fail();
          break;
        case CREATE_COLLECTION:
          responseData.getBatchCreateResponseEnvelope()
              .setCreateResponse(Collections.<BatchCreateResponseEnvelope.CollectionCreateResponseItem>emptyList(),
                                 null);
          Assert.fail();
          break;
        case GET_COLLECTION:
          responseData.getCollectionResponseEnvelope()
              .setCollectionResponse(Collections.<RecordTemplate>emptyList(), new CollectionMetadata(),
                                     new EmptyRecord(), null);
          break;
        case STATUS_ONLY:
          responseData.getEmptyResponseEnvelope().setStatus(null);
          break;
        default:
          Assert.fail();
      }
      Assert.fail();
    }
    catch (UnsupportedOperationException e)
    {
      // expected
    }
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testRestLiResponseEnvelopesGetter(RestLiResponseData responseData, ResourceMethod resourceMethod)
  {
    // make sure the correct response envelope is returned for the resource method
    switch(resourceMethod)
    {
      case GET:
        Assert.assertTrue(responseData.getGetResponseEnvelope().getClass().equals(GetResponseEnvelope.class));
        break;
      case CREATE:
        Assert.assertTrue(responseData.getCreateResponseEnvelope().getClass().equals(CreateResponseEnvelope.class));
        break;
      case ACTION:
        Assert.assertTrue(responseData.getActionResponseEnvelope().getClass().equals(ActionResponseEnvelope.class));
        break;
      case FINDER:
        Assert.assertTrue(responseData.getFinderResponseEnvelope().getClass().equals(FinderResponseEnvelope.class));
        break;
      case GET_ALL:
        Assert.assertTrue(responseData.getGetAllResponseEnvelope().getClass().equals(GetAllResponseEnvelope.class));
        break;
      case BATCH_CREATE:
        Assert.assertTrue(responseData.getBatchCreateResponseEnvelope().getClass()
                              .equals(BatchCreateResponseEnvelope.class));
        break;
      case BATCH_GET:
        Assert.assertTrue(responseData.getBatchGetResponseEnvelope().getClass().equals(BatchGetResponseEnvelope.class));
        break;
      case BATCH_UPDATE:
        Assert.assertTrue(responseData.getBatchUpdateResponseEnvelope().getClass()
                              .equals(BatchUpdateResponseEnvelope.class));
        break;
      case BATCH_PARTIAL_UPDATE:
        Assert.assertTrue(responseData.getBatchPartialUpdateResponseEnvelope().getClass()
                              .equals(BatchPartialUpdateResponseEnvelope.class));
        break;
      case BATCH_DELETE:
        Assert.assertTrue(responseData.getBatchDeleteResponseEnvelope().getClass()
                              .equals(BatchDeleteResponseEnvelope.class));
        break;
      case UPDATE:
        Assert.assertTrue(responseData.getUpdateResponseEnvelope().getClass().equals(UpdateResponseEnvelope.class));
        break;
      case DELETE:
        Assert.assertTrue(responseData.getDeleteResponseEnvelope().getClass().equals(DeleteResponseEnvelope.class));
        break;
      case PARTIAL_UPDATE:
        Assert.assertTrue(responseData.getPartialUpdateResponseEnvelope().getClass()
                              .equals(PartialUpdateResponseEnvelope.class));
        break;
      case OPTIONS:
        Assert.assertTrue(responseData.getOptionsResponseEnvelope().getClass().equals(OptionsResponseEnvelope.class));
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testRestLiResponseEnvelopesGetterException(RestLiResponseData responseData, ResourceMethod method)
  {
    ResponseType responseType = ResponseType.fromMethodType(method);
    try
    {
      responseData.getRecordResponseEnvelope();
      if (responseType != ResponseType.SINGLE_ENTITY) Assert.fail();
    }
    catch (UnsupportedOperationException e)
    {
      if (responseType == ResponseType.SINGLE_ENTITY) Assert.fail();
    }

    try
    {
      responseData.getCollectionResponseEnvelope();
      if (responseType != ResponseType.GET_COLLECTION) Assert.fail();
    }
    catch (UnsupportedOperationException e)
    {
      if (responseType == ResponseType.GET_COLLECTION) Assert.fail();
    }

    try
    {
      responseData.getBatchCreateResponseEnvelope();
      if (responseType != ResponseType.CREATE_COLLECTION) Assert.fail();
    }
    catch (UnsupportedOperationException e)
    {
      if (responseType == ResponseType.CREATE_COLLECTION) Assert.fail();
    }

    try
    {
      responseData.getBatchResponseEnvelope();
      if (responseType != ResponseType.BATCH_ENTITIES) Assert.fail();
    }
    catch (UnsupportedOperationException e)
    {
      if (responseType == ResponseType.BATCH_ENTITIES) Assert.fail();
    }

    try
    {
      responseData.getEmptyResponseEnvelope();
      if (responseType != ResponseType.STATUS_ONLY) Assert.fail();
    }
    catch (UnsupportedOperationException e)
    {
      if (responseType == ResponseType.STATUS_ONLY) Assert.fail();
    }
    try
    {
      responseData.getActionResponseEnvelope();
      if (method != ResourceMethod.ACTION) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.ACTION) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getBatchCreateResponseEnvelope();
      if (method != ResourceMethod.BATCH_CREATE) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.BATCH_CREATE) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getBatchDeleteResponseEnvelope();
      if (method != ResourceMethod.BATCH_DELETE) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.BATCH_DELETE) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getBatchGetResponseEnvelope();
      if (method != ResourceMethod.BATCH_GET) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.BATCH_GET) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getBatchPartialUpdateResponseEnvelope();
      if (method != ResourceMethod.BATCH_PARTIAL_UPDATE) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.BATCH_PARTIAL_UPDATE) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getBatchUpdateResponseEnvelope();
      if (method != ResourceMethod.BATCH_UPDATE) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.BATCH_UPDATE) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getCreateResponseEnvelope();
      if (method != ResourceMethod.CREATE) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.CREATE) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getDeleteResponseEnvelope();
      if (method != ResourceMethod.DELETE) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.DELETE) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getFinderResponseEnvelope();
      if (method != ResourceMethod.FINDER) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.FINDER) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getGetAllResponseEnvelope();
      if (method != ResourceMethod.GET_ALL) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.GET_ALL) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getOptionsResponseEnvelope();
      if (method != ResourceMethod.OPTIONS) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.OPTIONS) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getGetResponseEnvelope();
      if (method != ResourceMethod.GET) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.GET) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getPartialUpdateResponseEnvelope();
      if (method != ResourceMethod.PARTIAL_UPDATE) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.PARTIAL_UPDATE) Assert.fail("Should not throw UnsupportedOperationException");
    }
    try
    {
      responseData.getUpdateResponseEnvelope();
      if (method != ResourceMethod.UPDATE) Assert.fail("Did not throw UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e)
    {
      if (method == ResourceMethod.UPDATE) Assert.fail("Should not throw UnsupportedOperationException");
    }
  }

  @DataProvider
  private Object[][] envelopeResourceMethodDataProvider()
  {
    ResourceMethod[] resourceMethods = ResourceMethod.values();

    Object[][] envelopeResourceMethods = new Object[resourceMethods.length][2];
    for (int i = 0; i < envelopeResourceMethods.length; i++)
    {
      RestLiResponseDataImpl responseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK,
                                                                       Collections.<String, String>emptyMap(),
                                                                       Collections.<HttpCookie>emptyList());
      responseData.setResponseEnvelope(EnvelopeBuilderUtil.buildBlankResponseEnvelope(resourceMethods[i], responseData));
      envelopeResourceMethods[i][0] = responseData;
      envelopeResourceMethods[i][1] = resourceMethods[i];
    }

    return envelopeResourceMethods;
  }
}
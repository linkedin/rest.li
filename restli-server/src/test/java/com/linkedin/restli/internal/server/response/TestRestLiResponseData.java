package com.linkedin.restli.internal.server.response;


import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.common.AllProtocolVersions;
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
import org.testng.SkipException;
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
  public void testRestLiResponseEnvelopeInvariant(RestLiResponseData<?> responseData)
  {
    // Headers
    Map<String, String> headers = new HashMap<>();
    Assert.assertEquals(responseData.getHeaders(), headers);
    String headerKey = "testKey";
    String headerValue = "testValue";
    responseData.getHeaders().put(headerKey, headerValue);
    Assert.assertEquals(responseData.getHeaders().get(headerKey), headerValue);

    // Cookies
    List<HttpCookie> cookies = Collections.emptyList();
    Assert.assertEquals(responseData.getCookies(), cookies);

    // Exceptions
    if (responseData.getResponseEnvelope().isErrorResponse())
    {
      Assert.assertNotNull(responseData.getResponseEnvelope().getException());
      Assert.assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
      responseData.getResponseEnvelope().setExceptionInternal(exception503);
      Assert.assertEquals(responseData.getResponseEnvelope().getException().getStatus(), HttpStatus.S_503_SERVICE_UNAVAILABLE);

      // Make sure conversion to normal works
      responseData.getResponseEnvelope().setStatus(HttpStatus.S_200_OK);
      Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());
      Assert.assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_200_OK);
    }
    else
    {
      Assert.assertNull(responseData.getResponseEnvelope().getException());
      Assert.assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_200_OK);
      responseData.getResponseEnvelope().setStatus(HttpStatus.S_201_CREATED);
      Assert.assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_201_CREATED);
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
      RestLiResponseData<?> successResponseData = buildResponseData(methods[i],
                                                                    HttpStatus.S_200_OK);
      RestLiResponseData<?> errorResponseData = ErrorResponseBuilder.buildErrorResponseData(methods[i],
                                                                                          exception,
                                                                                          Collections.emptyMap(),
                                                                                          Collections.emptyList());

      sampleResponseData[i * 2] = new Object[] { successResponseData };
      sampleResponseData[i * 2 + 1] = new Object[] { errorResponseData };
    }

    return sampleResponseData;
  }

  @Test(dataProvider = "recordResponseEnvelopesProvider")
  public void testRecordResponseEnvelopeUpdates(RestLiResponseData<? extends RecordResponseEnvelope> responseData)
  {

    RecordResponseEnvelope recordResponseEnvelope = responseData.getResponseEnvelope();

    Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());
    Assert.assertEquals(recordResponseEnvelope.getRecord(), new EmptyRecord());

    // Swap to exception
    responseData.getResponseEnvelope().setExceptionInternal(exception500);
    Assert.assertNull(recordResponseEnvelope.getRecord());
    Assert.assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    Assert.assertEquals(responseData.getResponseEnvelope().getException(), exception500);
    Assert.assertEquals(responseData.getResponseEnvelope().getException(), exception500);

    // Swap back
    recordResponseEnvelope.setRecord(new EmptyRecord(), HttpStatus.S_200_OK);

    Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());
    Assert.assertEquals(recordResponseEnvelope.getRecord(), new EmptyRecord());
  }

  @DataProvider
  public static Object[][] recordResponseEnvelopesProvider()
  {
    RestLiResponseData<?> getResponseData = ResponseDataBuilderUtil.buildGetResponseData(HttpStatus.S_200_OK, new EmptyRecord());
    RestLiResponseData<?> createResponseData = ResponseDataBuilderUtil.buildCreateResponseData(HttpStatus.S_200_OK, new EmptyRecord());
    RestLiResponseData<?> actionResponseData = ResponseDataBuilderUtil.buildActionResponseData(HttpStatus.S_200_OK, new EmptyRecord());

    return new Object[][]{
        { getResponseData },
        { createResponseData },
        { actionResponseData }
    };
  }

  @Test(dataProvider = "collectionResponseEnvelopesProvider")
  @SuppressWarnings("unchecked")
  public void testCollectionResponseEnvelopeUpdates(RestLiResponseData<? extends CollectionResponseEnvelope> responseData)
  {
    CollectionResponseEnvelope responseEnvelope = responseData.getResponseEnvelope();

    Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());
    Assert.assertEquals(responseEnvelope.getCollectionResponse(), Collections.emptyList());
    Assert.assertEquals(responseEnvelope.getCollectionResponsePaging(), new CollectionMetadata());
    Assert.assertEquals(responseEnvelope.getCollectionResponseCustomMetadata(), new EmptyRecord());

    // Swap to exception
    responseData.getResponseEnvelope().setExceptionInternal(exception500);
    Assert.assertNull(responseEnvelope.getCollectionResponse());
    Assert.assertNull(responseEnvelope.getCollectionResponseCustomMetadata());
    Assert.assertNull(responseEnvelope.getCollectionResponsePaging());
    Assert.assertEquals(responseData.getResponseEnvelope().getException(), exception500);
    Assert.assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    // Swap back
    responseEnvelope.setCollectionResponse(new ArrayList<>(), new CollectionMetadata(), new EmptyRecord(),
                                           HttpStatus.S_200_OK);
    Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());
    Assert.assertEquals(responseEnvelope.getCollectionResponse(), Collections.emptyList());
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
    RestLiResponseData<?> getAllResponseData = ResponseDataBuilderUtil.buildGetAllResponseData(HttpStatus.S_200_OK,
        Collections.emptyList(), new CollectionMetadata(), new EmptyRecord());
    RestLiResponseData<?> finderResponseData = ResponseDataBuilderUtil.buildFinderResponseData(HttpStatus.S_200_OK,
        Collections.emptyList(),
        new CollectionMetadata(), new EmptyRecord());
    return new Object[][]{
        { getAllResponseData },
        { finderResponseData }
    };
  }

  @Test(dataProvider = "createCollectionResponseEnvelopesProvider")
  public void testCreateCollectionResponseEnvelopeUpdates(RestLiResponseData<BatchCreateResponseEnvelope> responseData)
  {

    BatchCreateResponseEnvelope responseEnvelope = responseData.getResponseEnvelope();

    Assert.assertNull(responseData.getResponseEnvelope().getException());
    Assert.assertEquals(responseEnvelope.getCreateResponses(), Collections.emptyList());
    Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());

    responseData.getResponseEnvelope().setExceptionInternal(exception500);
    Assert.assertNull(responseEnvelope.getCreateResponses());

    responseEnvelope.setCreateResponse(new ArrayList<>(),
                                       HttpStatus.S_200_OK);
    Assert.assertNull(responseData.getResponseEnvelope().getException());

    Assert.assertEquals(responseEnvelope.getCreateResponses().size(), 0);
    responseEnvelope.getCreateResponses()
        .add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(
            new CreateIdStatus<>(HttpStatus.S_201_CREATED.getCode(),
                                 new Object(),
                                 null,
                                 AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion())));
    responseEnvelope.getCreateResponses()
        .add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(exception500));
    Assert.assertEquals(responseEnvelope.getCreateResponses().size(), 2);
  }

  @DataProvider
  private Object[][] createCollectionResponseEnvelopesProvider()
  {
    RestLiResponseData<?> responseData = ResponseDataBuilderUtil.buildBatchCreateResponseData(HttpStatus.S_200_OK,
        Collections.emptyList());

    return new Object[][] {
        { responseData }
    };
  }

  @Test(dataProvider = "batchResponseEnvelopesProvider")
  public void testBatchResponseEnvelopeUpdates(RestLiResponseData<? extends BatchResponseEnvelope> responseData)
  {
    BatchResponseEnvelope responseEnvelope = responseData.getResponseEnvelope();

    Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());
    Assert.assertNull(responseData.getResponseEnvelope().getException());

    responseData.getResponseEnvelope().setExceptionInternal(exception500);
    Assert.assertNull(responseEnvelope.getBatchResponseMap());

    Map<Object, BatchResponseEnvelope.BatchResponseEntry> targetMap = new HashMap<>();
    responseEnvelope.setBatchResponseMap(targetMap, HttpStatus.S_200_OK);
    Assert.assertNull(responseData.getResponseEnvelope().getException());
    targetMap.put("key", new BatchResponseEnvelope.BatchResponseEntry(null, new EmptyRecord()));
    Assert.assertEquals(responseEnvelope.getBatchResponseMap().size(), 1);
    Assert.assertEquals(responseEnvelope.getBatchResponseMap().get("key").getRecord(), new EmptyRecord());
  }

  @DataProvider
  public static Object[][] batchResponseEnvelopesProvider()
  {
    RestLiResponseData<?> batchGetResponseData = ResponseDataBuilderUtil.buildBatchGetResponseData(HttpStatus.S_200_OK, Collections.emptyMap());
    RestLiResponseData<?> batchUpdateResponseData = ResponseDataBuilderUtil.buildBatchUpdateResponseData(HttpStatus.S_200_OK, Collections.emptyMap());
    RestLiResponseData<?> batchPartialUpdateResponseData = ResponseDataBuilderUtil.buildBatchPartialUpdateResponseData(HttpStatus.S_200_OK, Collections.emptyMap());
    RestLiResponseData<?> batchDeleteResponseData = ResponseDataBuilderUtil.buildBatchDeleteResponseData(HttpStatus.S_200_OK, Collections.emptyMap());

    return new Object[][] {
        { batchGetResponseData },
        { batchUpdateResponseData },
        { batchPartialUpdateResponseData },
        { batchDeleteResponseData }
    };
  }

  @Test(dataProvider = "emptyResponseEnvelopesProvider")
  public void testEmptyResponseEnvelopeUpdates(RestLiResponseData<?> responseData)
  {
    Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());

    responseData.getResponseEnvelope().setExceptionInternal(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR));
    Assert.assertTrue(responseData.getResponseEnvelope().isErrorResponse());
    Assert.assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);

    responseData.getResponseEnvelope().setStatus(HttpStatus.S_200_OK);
    Assert.assertFalse(responseData.getResponseEnvelope().isErrorResponse());
    Assert.assertEquals(responseData.getResponseEnvelope().getStatus(), HttpStatus.S_200_OK);
  }

  @DataProvider
  private Object[][] emptyResponseEnvelopesProvider()
  {
    RestLiResponseData<?> partialUpdateResponseData = ResponseDataBuilderUtil.buildPartialUpdateResponseData(HttpStatus.S_200_OK);
    RestLiResponseData<?> updateResponseData = ResponseDataBuilderUtil.buildUpdateResponseData(HttpStatus.S_200_OK);
    RestLiResponseData<?> deleteResponseData = ResponseDataBuilderUtil.buildDeleteResponseData(HttpStatus.S_200_OK);
    RestLiResponseData<?> optionsResponseData = ResponseDataBuilderUtil.buildOptionsResponseData(HttpStatus.S_200_OK);

    return new Object[][] {
        { partialUpdateResponseData },
        { updateResponseData },
        { deleteResponseData },
        { optionsResponseData }
    };
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testResourceMethod(RestLiResponseData<?> responseData, ResourceMethod resourceMethod)
  {
    Assert.assertEquals(responseData.getResourceMethod(), resourceMethod);
  }

  /**
   * Skips testing resource methods with dynamically determined response types.
   * See {@link #testDynamicallyDeterminedResponseType} for equivalent test.
   */
  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testResponseType(RestLiResponseData<?> responseData, ResourceMethod resourceMethod)
  {
    if (!ResponseTypeUtil.isDynamicallyDetermined(resourceMethod))
    {
      ResponseType responseType = ResponseTypeUtil.fromMethodType(resourceMethod);
      Assert.assertEquals(responseData.getResponseType(), responseType);
    }
  }

  @Test(dataProvider = "getDynamicallyDeterminedResponseTypeData")
  public void testDynamicallyDeterminedResponseType(RestLiResponseData<?> responseData, ResponseType expectedResponseType)
  {
    Assert.assertEquals(responseData.getResponseType(), expectedResponseType);
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  public void testSetNullExceptions(RestLiResponseData<?> responseData, ResourceMethod resourceMethod)
  {
    try
    {
      responseData.getResponseEnvelope().setExceptionInternal(null);
      Assert.fail();
    }
    catch (AssertionError e)
    {
      // expected
    }
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  @SuppressWarnings("deprecation")
  public void testSetNullStatus(RestLiResponseData<?> responseData, ResourceMethod resourceMethod)
  {
    try
    {
      // If response type is dynamically determined, set HTTP status through resource method response envelope
      if (ResponseTypeUtil.isDynamicallyDetermined(resourceMethod))
      {
        switch (resourceMethod)
        {
          case PARTIAL_UPDATE:
            responseData.getPartialUpdateResponseEnvelope().setStatus(null);
            break;
          default:
            Assert.fail();
        }
      }
      // Otherwise, set HTTP status through response type response envelope
      else
      {
        ResponseType responseType = ResponseTypeUtil.fromMethodType(resourceMethod);
        switch (responseType)
        {
          case SINGLE_ENTITY:
            responseData.getRecordResponseEnvelope().setRecord(new EmptyRecord(), null);
            Assert.fail();
            break;
          case BATCH_ENTITIES:
            responseData.getBatchResponseEnvelope()
                .setBatchResponseMap(Collections.emptyMap(), null);
            Assert.fail();
            break;
          case CREATE_COLLECTION:
            responseData.getBatchCreateResponseEnvelope()
                .setCreateResponse(Collections.emptyList(),
                    null);
            Assert.fail();
            break;
          case GET_COLLECTION:
            responseData.getCollectionResponseEnvelope()
                .setCollectionResponse(Collections.emptyList(), new CollectionMetadata(),
                    new EmptyRecord(), null);
            break;
          case STATUS_ONLY:
            responseData.getEmptyResponseEnvelope().setStatus(null);
            break;
          default:
            Assert.fail();
        }
      }
      Assert.fail();
    }
    catch (AssertionError e)
    {
      // expected
    }
  }

  @Test(dataProvider = "envelopeResourceMethodDataProvider")
  @SuppressWarnings("deprecation")
  public void testRestLiResponseEnvelopesGetter(RestLiResponseData<?> responseData, ResourceMethod resourceMethod)
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
  @SuppressWarnings("deprecation")
  public void testRestLiResponseEnvelopesGetterException(RestLiResponseData<?> responseData, ResourceMethod method)
  {
    ResponseType responseType;
    if (ResponseTypeUtil.isDynamicallyDetermined(method))
    {
      // If ResponseType is dynamically determined for this resource method, it should be left null so that the test
      // expects it to fail for all calls except the direct getter of the resource method response envelope.
      responseType = null;
    }
    else
    {
      responseType = ResponseTypeUtil.fromMethodType(method);
    }

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
      RestLiResponseData<?> responseData = buildResponseData(resourceMethods[i],
                                                             HttpStatus.S_200_OK);
      envelopeResourceMethods[i][0] = responseData;
      envelopeResourceMethods[i][1] = resourceMethods[i];
    }

    return envelopeResourceMethods;
  }

  private static RestLiResponseData<?> buildResponseData(ResourceMethod method, HttpStatus status)
  {
    switch (method)
    {
      case GET:
        return ResponseDataBuilderUtil.buildGetResponseData(status, new EmptyRecord());
      case CREATE:
        return ResponseDataBuilderUtil.buildCreateResponseData(status, new EmptyRecord());
      case ACTION:
        return ResponseDataBuilderUtil.buildActionResponseData(status, new EmptyRecord());
      case GET_ALL:
        return ResponseDataBuilderUtil.buildGetAllResponseData(status, Collections.emptyList(), new CollectionMetadata(), new EmptyRecord());
      case FINDER:
        return ResponseDataBuilderUtil.buildFinderResponseData(status, Collections.emptyList(), new CollectionMetadata(), new EmptyRecord());
      case BATCH_CREATE:
        return ResponseDataBuilderUtil.buildBatchCreateResponseData(status, Collections.emptyList());
      case BATCH_GET:
        return ResponseDataBuilderUtil.buildBatchGetResponseData(status, Collections.emptyMap());
      case BATCH_UPDATE:
        return ResponseDataBuilderUtil.buildBatchUpdateResponseData(status, Collections.emptyMap());
      case BATCH_PARTIAL_UPDATE:
        return ResponseDataBuilderUtil.buildBatchPartialUpdateResponseData(status, Collections.emptyMap());
      case BATCH_DELETE:
        return ResponseDataBuilderUtil.buildBatchDeleteResponseData(status, Collections.emptyMap());
      case PARTIAL_UPDATE:
        return ResponseDataBuilderUtil.buildPartialUpdateResponseData(status);
      case UPDATE:
        return ResponseDataBuilderUtil.buildUpdateResponseData(status);
      case DELETE:
        return ResponseDataBuilderUtil.buildDeleteResponseData(status);
      case OPTIONS:
        return ResponseDataBuilderUtil.buildOptionsResponseData(status);
      default:
        throw new IllegalArgumentException("Unexpected Rest.li resource method: " + method);
    }
  }

  @DataProvider
  private Object[][] getDynamicallyDeterminedResponseTypeData()
  {
    return new Object[][]
    {
        { ResponseDataBuilderUtil.buildPartialUpdateResponseData(HttpStatus.S_200_OK), ResponseType.STATUS_ONLY },
        { ResponseDataBuilderUtil.buildPartialUpdateResponseData(HttpStatus.S_200_OK, new EmptyRecord()), ResponseType.SINGLE_ENTITY }
    };
  }
}
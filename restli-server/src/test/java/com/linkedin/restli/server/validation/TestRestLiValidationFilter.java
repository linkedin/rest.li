/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.server.validation;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.filter.FilterResourceModelImpl;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiAnnotationReader;
import com.linkedin.restli.internal.server.response.ActionResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchCreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchPartialUpdateResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchUpdateResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetResponseEnvelope;
import com.linkedin.restli.internal.server.response.PartialUpdateResponseEnvelope;
import com.linkedin.restli.internal.server.response.ResponseDataBuilderUtil;
import com.linkedin.restli.internal.server.response.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.response.UpdateResponseEnvelope;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiRequestDataImpl;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.TestRecordWithValidation;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResponseContext;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import java.util.Collections;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import static com.linkedin.restli.common.ResourceMethod.*;
import static org.mockito.Mockito.*;


/**
 * Tests for {@link RestLiValidationFilter}.
 *
 * @author Evan Williams
 */
public class TestRestLiValidationFilter
{
  private static final String WHITELISTED_FIELD_NAME = "$URN";
  @Mock
  private FilterRequestContext filterRequestContext;

  @Mock
  private FilterResponseContext filterResponseContext;

  @RestLiActions(name="fooActions")
  private static class ActionsResource
  {
  }

  @RestLiCollection(name = "fooCollection")
  private static class CollectionResource extends CollectionResourceTemplate<Long, TestRecord>
  {
  }

  @RestLiSimpleResource(name = "fooSimple")
  private static class SimpleResource extends SimpleResourceTemplate<TestRecord>
  {
  }

  @RestLiAssociation(name = "fooAssociation", assocKeys={@Key(name="groupID", type=int.class), @Key(name="memberID", type=int.class)})
  private static class AssociationResource extends AssociationResourceTemplate<TestRecord>
  {
  }

  @BeforeMethod
  public void setUpMocks()
  {
    MockitoAnnotations.initMocks(this);
    when(filterRequestContext.getFilterResourceModel()).thenReturn(new FilterResourceModelImpl(RestLiAnnotationReader.processResource(CollectionResource.class)));
    when(filterRequestContext.getCustomAnnotations()).thenReturn(new DataMap());
    when(filterRequestContext.isReturnEntityMethod()).thenReturn(false);
  }

  /**
   * Ensures that the validation filter safely and correctly reacts to projections given a variety of resource types,
   * resource methods, and projection masks. This was motivated by a bug that caused an NPE in the validation filter
   * when the resource being queried was a {@link RestLiActions} resource and thus had no value class.
   */
  @Test(dataProvider = "validateWithProjectionData")
  @SuppressWarnings({"unchecked"})
  public void testHandleProjection(ResourceModel resourceModel, RestLiResponseData<RestLiResponseEnvelope> responseData, MaskTree projectionMask, boolean expectError)
  {
    ResourceMethod resourceMethod = responseData.getResourceMethod();

    when(filterRequestContext.getRequestData()).thenReturn(new RestLiRequestDataImpl.Builder().entity(makeTestRecord()).build());
    when(filterRequestContext.getMethodType()).thenReturn(resourceMethod);
    when(filterRequestContext.getFilterResourceModel()).thenReturn(new FilterResourceModelImpl(resourceModel));
    when(filterRequestContext.getProjectionMask()).thenReturn(projectionMask);
    when(filterResponseContext.getResponseData()).thenReturn((RestLiResponseData) responseData);

    RestLiValidationFilter validationFilter = new RestLiValidationFilter();

    try
    {
      validationFilter.onRequest(filterRequestContext);

      if (expectError)
      {
        Assert.fail("Expected an error to be thrown on request in the validation filter, but none was thrown.");
      }
    }
    catch (RestLiServiceException e)
    {
      if (expectError)
      {
        Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST);
        return;
      }
      else
      {
        Assert.fail("An unexpected exception was thrown on request in the validation filter.", e);
      }
    }

    validationFilter.onResponse(filterRequestContext, filterResponseContext);
  }

  @DataProvider(name = "validateWithPdscValidation")
  public Object[][] validateWithPdscValidation()
  {
    String validValue = "aaaaa";
    String invalidValue = "aaaaaaaaaaaaaaaa";
    RestLiResponseData<CreateResponseEnvelope> createResponseData = ResponseDataBuilderUtil.buildCreateResponseData(HttpStatus.S_201_CREATED, new IdResponse<>(123L));
    RestLiResponseData<UpdateResponseEnvelope> updateResponseData = ResponseDataBuilderUtil.buildUpdateResponseData(HttpStatus.S_200_OK);

    RestLiResponseData<BatchCreateResponseEnvelope> batchCreateResponseData = ResponseDataBuilderUtil.buildBatchCreateResponseData(HttpStatus.S_200_OK,
        Collections.singletonList(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(
            new CreateIdEntityStatus<>(HttpStatus.S_201_CREATED.getCode(), 1L, makeTestRecord(), null, new ProtocolVersion(2, 0, 0)))));

    RestLiResponseData<BatchUpdateResponseEnvelope> batchUpdateResponseData = ResponseDataBuilderUtil.buildBatchUpdateResponseData(HttpStatus.S_200_OK, Collections.emptyMap());

    return new Object[][]
        {
            // Resource model
            // Resource method
            // RestLi request data
            // RestLi response data
            // Expect error?
            { RestLiAnnotationReader.processResource(CollectionResource.class), CREATE,
                new RestLiRequestDataImpl.Builder().entity(makeTestRecordWithValidation(invalidValue)).build(), null, true },
            { RestLiAnnotationReader.processResource(CollectionResource.class), CREATE,
                new RestLiRequestDataImpl.Builder().entity(makeTestRecordWithValidation(validValue)).build(), createResponseData, false },
            { RestLiAnnotationReader.processResource(CollectionResource.class), UPDATE,
                new RestLiRequestDataImpl.Builder().entity(makeTestRecordWithValidation(invalidValue)).build(), null, true },
            { RestLiAnnotationReader.processResource(CollectionResource.class), UPDATE,
                new RestLiRequestDataImpl.Builder().entity(makeTestRecordWithValidation(validValue)).build(), updateResponseData, false },
            { RestLiAnnotationReader.processResource(CollectionResource.class), BATCH_CREATE,
                new RestLiRequestDataImpl.Builder().batchEntities(Collections.singleton(makeTestRecordWithValidation(invalidValue))).build(), null, true },
            { RestLiAnnotationReader.processResource(CollectionResource.class), BATCH_CREATE,
                new RestLiRequestDataImpl.Builder().batchEntities(Collections.singleton(makeTestRecordWithValidation(validValue))).build(), batchCreateResponseData, false },
            { RestLiAnnotationReader.processResource(CollectionResource.class), BATCH_UPDATE,
                new RestLiRequestDataImpl.Builder().batchKeyEntityMap(Collections.singletonMap("Key", makeTestRecordWithValidation(invalidValue))).build(), null, true },
            { RestLiAnnotationReader.processResource(CollectionResource.class), BATCH_UPDATE,
                new RestLiRequestDataImpl.Builder().batchKeyEntityMap(Collections.singletonMap("Key", makeTestRecordWithValidation(validValue))).build(), batchUpdateResponseData, false }
        };
  }

  /**
   * Ensures that the validation filter correctly validates input entity given a variety of resource types,
   * resource methods and RestLi request data.
   */
  @Test(dataProvider = "validateWithPdscValidation")
  @SuppressWarnings({"unchecked"})
  public void testEntityValidateOnRequest(ResourceModel resourceModel, ResourceMethod resourceMethod,
      RestLiRequestData restLiRequestData, RestLiResponseData<RestLiResponseEnvelope> responseData, boolean expectError)
  {
    when(filterRequestContext.getRequestData()).thenReturn(restLiRequestData);
    when(filterRequestContext.getMethodType()).thenReturn(resourceMethod);
    when(filterRequestContext.getFilterResourceModel()).thenReturn(new FilterResourceModelImpl(resourceModel));
    when(filterRequestContext.getRestliProtocolVersion()).thenReturn(AllProtocolVersions.LATEST_PROTOCOL_VERSION);
    when(filterResponseContext.getResponseData()).thenReturn((RestLiResponseData) responseData);

    RestLiValidationFilter validationFilter = new RestLiValidationFilter(Collections.emptyList(), new MockValidationErrorHandler());

    try
    {
      validationFilter.onRequest(filterRequestContext);

      if (expectError)
      {
        Assert.fail("Expected an error to be thrown on request in the validation filter, but none was thrown.");
      }
    }
    catch (RestLiServiceException ex)
    {
      if (expectError)
      {
        Assert.assertEquals(ex.getStatus(), HttpStatus.S_422_UNPROCESSABLE_ENTITY);
        return;
      }
      else
      {
        Assert.fail("An unexpected exception was thrown on request in the validation filter.", ex);
      }
    }

    validationFilter.onResponse(filterRequestContext, filterResponseContext);
  }

  @DataProvider(name = "validateWithProjectionData")
  public Object[][] validateWithProjectionData()
  {
    RestLiResponseData<GetResponseEnvelope> getResponseData = ResponseDataBuilderUtil.buildGetResponseData(HttpStatus.S_200_OK, makeTestRecord());
    RestLiResponseData<CreateResponseEnvelope> createResponseData = ResponseDataBuilderUtil.buildCreateResponseData(HttpStatus.S_201_CREATED, new IdResponse<>(123L));
    RestLiResponseData<ActionResponseEnvelope> actionResponseData = ResponseDataBuilderUtil.buildActionResponseData(HttpStatus.S_200_OK, new EmptyRecord());

    return new Object[][]
    {
        // Resource model                                                     Response data         Projection mask                        Expect error?
        { RestLiAnnotationReader.processResource(ActionsResource.class),      actionResponseData,   null,                                  false },
        { RestLiAnnotationReader.processResource(ActionsResource.class),      actionResponseData,   new MaskTree(),                        false },
        { RestLiAnnotationReader.processResource(ActionsResource.class),      actionResponseData,   makeMask("ignoreMePlease"),   false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData,      null,                                  false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData,      new MaskTree(),                        false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData,      makeMask("nonexistentField"), true  },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData,      makeMask("intField"),         false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   actionResponseData,   null,                                  false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   actionResponseData,   new MaskTree(),                        false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   actionResponseData,   makeMask("ignoreMePlease"),   false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       getResponseData,      null,                                  false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       getResponseData,      new MaskTree(),                        false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       getResponseData,      makeMask("nonexistentField"), true  },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       getResponseData,      makeMask("intField"),         false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       createResponseData,   null,                                  false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       createResponseData,   new MaskTree(),                        false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       createResponseData,   makeMask("nonexistentField"), true  },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       createResponseData,   makeMask("intField"),         false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       actionResponseData,   null,                                  false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       actionResponseData,   new MaskTree(),                        false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       actionResponseData,   makeMask("ignoreMePlease"),   false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  getResponseData,      null,                                  false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  getResponseData,      new MaskTree(),                        false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  getResponseData,      makeMask("nonexistentField"), true  },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  getResponseData,      makeMask("intField"),         false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  actionResponseData,   null,                                  false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  actionResponseData,   new MaskTree(),                        false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  actionResponseData,   makeMask("ignoreMePlease"),   false }
    };
  }

  @Test(dataProvider = "projectionDataWithWhitelistFields")
  @SuppressWarnings({"unchecked"})
  public void testAllowWhitelistedFieldsInMask(ResourceModel resourceModel, RestLiResponseData<RestLiResponseEnvelope> responseData, MaskTree projectionMask, boolean expectError)
  {
    ResourceMethod resourceMethod = responseData.getResourceMethod();

    when(filterRequestContext.getRequestData()).thenReturn(new RestLiRequestDataImpl.Builder().entity(makeTestRecord()).build());
    when(filterRequestContext.getMethodType()).thenReturn(resourceMethod);
    when(filterRequestContext.getFilterResourceModel()).thenReturn(new FilterResourceModelImpl(resourceModel));
    when(filterRequestContext.getProjectionMask()).thenReturn(projectionMask);
    when(filterResponseContext.getResponseData()).thenReturn((RestLiResponseData) responseData);

    RestLiValidationFilter validationFilter = new RestLiValidationFilter(
        Lists.newArrayList(WHITELISTED_FIELD_NAME));

    try
    {
      validationFilter.onRequest(filterRequestContext);

      if (expectError)
      {
        Assert.fail("Expected an error to be thrown on request in the validation filter, but none was thrown.");
      }
    }
    catch (RestLiServiceException e)
    {
      if (expectError)
      {
        Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST);
        return;
      }
      else
      {
        Assert.fail("An unexpected exception was thrown on request in the validation filter.", e);
      }
    }
    validationFilter.onResponse(filterRequestContext, filterResponseContext);
  }

  @DataProvider(name = "projectionDataWithWhitelistFields")
  public Object[][] projectionDataWithWhitelistFields()
  {
    RestLiResponseData<GetResponseEnvelope> getResponseData = ResponseDataBuilderUtil.buildGetResponseData(HttpStatus.S_200_OK, makeTestRecord());

    return new Object[][]
    {
        // Resource model                                                     Response data         Projection mask                                                   Expect error?
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData,      makeMask(WHITELISTED_FIELD_NAME),                                 false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData,      makeMask(WHITELISTED_FIELD_NAME, "nonexistentField"), true  },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData,      makeMask(WHITELISTED_FIELD_NAME, "intField"),         false }
    };
  }

  /**
   * Ensures that validation appropriately occurs on response for "return entity" methods, and that validation does not
   * occur on response for methods that are not "return entity" methods.
   */
  @Test(dataProvider = "returnEntityValidateOnResponseData")
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testReturnEntityValidateOnResponse(ResourceMethod resourceMethod, RestLiResponseData responseData, boolean isReturnEntityMethod)
  {
    when(filterRequestContext.getMethodType()).thenReturn(resourceMethod);
    when(filterRequestContext.isReturnEntityMethod()).thenReturn(isReturnEntityMethod);
    when(filterResponseContext.getResponseData()).thenReturn(responseData);

    RestLiValidationFilter validationFilter = new RestLiValidationFilter();

    try
    {
      // Check if validation occurred by catching exceptions for invalid entities
      validationFilter.onResponse(filterRequestContext, filterResponseContext);

      if (isReturnEntityMethod)
      {
        Assert.fail("Expected validation to occur and cause an exception, but no exception was encountered.");
      }
    }
    catch (RestLiServiceException e)
    {
      if (!isReturnEntityMethod)
      {
        Assert.fail("Expected validation to be skipped without exceptions, but encountered exception: " + e.getMessage());
      }

      Assert.assertEquals(e.getStatus().getCode(), HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode(), "Expected HTTP status code 500 for this validation failure.");
      Assert.assertTrue(e.getMessage().contains("/intField :: notAnInt cannot be coerced to Integer"), "Expected validation error for field \"intField\", but found another error.");
    }
  }

  @DataProvider(name = "returnEntityValidateOnResponseData")
  private Object[][] provideReturnEntityValidateOnResponseData()
  {
    RestLiResponseData<CreateResponseEnvelope> createResponseData = ResponseDataBuilderUtil.buildCreateResponseData(HttpStatus.S_201_CREATED, makeInvalidTestRecord());
    RestLiResponseData<PartialUpdateResponseEnvelope> partialUpdateResponseData = ResponseDataBuilderUtil.buildPartialUpdateResponseData(HttpStatus.S_200_OK, makeInvalidTestRecord());
    RestLiResponseData<BatchCreateResponseEnvelope> batchCreateResponseData = ResponseDataBuilderUtil.buildBatchCreateResponseData(HttpStatus.S_200_OK,
        Collections.singletonList(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(
            new CreateIdEntityStatus<>(HttpStatus.S_201_CREATED.getCode(), 1L, makeInvalidTestRecord(), null, new ProtocolVersion(2, 0, 0)))));
    RestLiResponseData<BatchPartialUpdateResponseEnvelope> batchPartialUpdateResponseData = ResponseDataBuilderUtil.buildBatchPartialUpdateResponseData(HttpStatus.S_200_OK,
        Collections.singletonMap(1L, new BatchResponseEnvelope.BatchResponseEntry(HttpStatus.S_200_OK,
            new UpdateEntityStatus<>(HttpStatus.S_200_OK.getCode(), makeInvalidTestRecord()))));

    // The last argument indicates whether the resource method is a "return entity" method,
    // but is also used to determine if validation is expected on response.
    return new Object[][]
        {
            { CREATE, createResponseData, true },
            { CREATE, createResponseData, false },
            { PARTIAL_UPDATE, partialUpdateResponseData, true },
            { PARTIAL_UPDATE, partialUpdateResponseData, false },
            { BATCH_CREATE, batchCreateResponseData, true },
            { BATCH_CREATE, batchCreateResponseData, false },
            { BATCH_PARTIAL_UPDATE, batchPartialUpdateResponseData, true },
            { BATCH_PARTIAL_UPDATE, batchPartialUpdateResponseData, false }
        };
  }

  private TestRecord makeTestRecord()
  {
    return new TestRecord().setIntField(123).setLongField(456L).setFloatField(7.89F).setDoubleField(1.2345);
  }

  private TestRecordWithValidation makeTestRecordWithValidation(String value)
  {
    return new TestRecordWithValidation().setStringField(value);
  }

  private TestRecord makeInvalidTestRecord()
  {
    DataMap dataMap = new DataMap();
    dataMap.put("intField", "notAnInt");
    dataMap.put("longField", 123L);
    dataMap.put("floatField", 4.56F);
    dataMap.put("doubleField", 7.89);

    return new TestRecord(dataMap);
  }

  private MaskTree makeMask(String... segments)
  {
    PathSpec[] pathSpecs = new PathSpec[segments.length];
    for (int i = 0; i < segments.length; i++)
    {
      pathSpecs[i] = new PathSpec(segments[i]);
    }
    return MaskCreator.createPositiveMask(pathSpecs);
  }
}

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
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.internal.server.filter.FilterResourceModelImpl;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiAnnotationReader;
import com.linkedin.restli.internal.server.response.ActionResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateResponseEnvelope;
import com.linkedin.restli.internal.server.response.GetResponseEnvelope;
import com.linkedin.restli.internal.server.response.ResponseDataBuilderUtil;
import com.linkedin.restli.internal.server.response.RestLiResponseEnvelope;
import com.linkedin.restli.server.RestLiRequestDataImpl;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.TestRecord;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


/**
 * Tests for {@link RestLiValidationFilter}.
 *
 * @author Evan Williams
 */
public class TestRestLiValidationFilter
{
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

  @BeforeTest
  public void setUpMocks()
  {
    MockitoAnnotations.initMocks(this);
    when(filterRequestContext.getRequestData()).thenReturn(new RestLiRequestDataImpl.Builder().entity(makeTestRecord()).build());
    when(filterRequestContext.getCustomAnnotations()).thenReturn(new DataMap());
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
    when(filterRequestContext.getMethodType()).thenReturn(responseData.getResourceMethod());
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
        Assert.fail("An unexpected exception was thrown on request in the validation filter.");
      }
    }

    validationFilter.onResponse(filterRequestContext, filterResponseContext);
  }

  @DataProvider(name = "validateWithProjectionData")
  public Object[][] validateWithProjectionData()
  {
    return new Object[][]
    {
        { RestLiAnnotationReader.processResource(ActionsResource.class),      actionResponseData(),  null,                                  false },
        { RestLiAnnotationReader.processResource(ActionsResource.class),      actionResponseData(),  makeMask("ignoreMePlease"),   false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData(),     null,                                  false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData(),     makeMask("nonexistentField"), true },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   getResponseData(),     makeMask("intField"),         false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   actionResponseData(),  null,                                  false },
        { RestLiAnnotationReader.processResource(CollectionResource.class),   actionResponseData(),  makeMask("ignoreMePlease"),   false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       getResponseData(),     null,                                  false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       getResponseData(),     makeMask("nonexistentField"), true },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       getResponseData(),     makeMask("intField"),         false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       createResponseData(),  null,                                  false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       createResponseData(),  makeMask("nonexistentField"), true },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       createResponseData(),  makeMask("intField"),         false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       actionResponseData(),  null,                                  false },
        { RestLiAnnotationReader.processResource(SimpleResource.class),       actionResponseData(),  makeMask("ignoreMePlease"),   false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  getResponseData(),     null,                                  false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  getResponseData(),     makeMask("nonexistentField"), true },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  getResponseData(),     makeMask("intField"),         false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  actionResponseData(),  null,                                  false },
        { RestLiAnnotationReader.processResource(AssociationResource.class),  actionResponseData(),  makeMask("ignoreMePlease"),   false },

    };
  }

  private RestLiResponseData<GetResponseEnvelope> getResponseData()
  {
    return ResponseDataBuilderUtil.buildGetResponseData(HttpStatus.S_200_OK, makeTestRecord());
  }

  private RestLiResponseData<CreateResponseEnvelope> createResponseData()
  {
    return ResponseDataBuilderUtil.buildCreateResponseData(HttpStatus.S_201_CREATED, new IdResponse<>(123L));
  }

  private RestLiResponseData<ActionResponseEnvelope> actionResponseData()
  {
    return ResponseDataBuilderUtil.buildActionResponseData(HttpStatus.S_200_OK, new EmptyRecord());
  }

  private TestRecord makeTestRecord()
  {
    return new TestRecord().setIntField(123).setLongField(456L).setFloatField(7.89F).setDoubleField(1.2345);
  }

  private MaskTree makeMask(String segment) {
    return MaskCreator.createPositiveMask(new PathSpec(segment));
  }
}

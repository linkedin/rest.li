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

package com.linkedin.restli.examples;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.CustomMetadataProjectionsBuilders;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Karim Vidhani
 */
public class TestCustomMetadataProjection extends RestLiIntegrationTest
{
  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  //The following tests operate on rootAutomaticMetadataAutomatic

  /**
   * Calls the resource method rootAutomaticMetadataAutomatic with a single metadata field and a single root object
   * entity field
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataAutomaticSingleRootSingleMeta(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("rootAutomaticMetadataAutomatic")
            .fields(Greeting.fields().tone())
            .metadataFields(Greeting.fields().message())
            .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, false /*hasMessage*/ , false /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootAutomaticMetadataAutomatic with no fields specified in any projection
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataAutomaticNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> findRequest = builders.findBy("rootAutomaticMetadataAutomatic")
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(findRequest).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootAutomaticMetadataAutomatic with all custom metadata fields and a single
   * root object entity projection field
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataAutomaticAllFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> findRequest = builders.findBy("rootAutomaticMetadataAutomatic")
        .fields(Greeting.fields().tone())
        .metadataFields(Greeting.fields().tone(), Greeting.fields().id(), Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(findRequest).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, false /*hasMessage*/, false /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootAutomaticMetadataAutomatic with and a single root object and empty fields list for
   * metadata. This will leave just the query string parameter by itself in the URI, e.g "...&metadataFields&...
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataAutomaticSingleRootEmptyMeta(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("rootAutomaticMetadataAutomatic")
        .fields(Greeting.fields().tone())
        .metadataFields()
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, false /*hasMessage*/, false /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //Nothing should be sent back here
    assertCustomMetadata(metadataGreeting, false /*hasTone*/, false /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following tests operate on the resource method rootAutomaticMetadataManual

  /**
   * Calls the resource method rootAutomaticMetadataManual with a single metadata field and a single root object
   * entity field
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataManualSingleRootSingleMeta(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("rootAutomaticMetadataManual")
        .fields(Greeting.fields().tone())
        .metadataFields(Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, false /*hasMessage*/, false /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //Note that the server here will intentionally leave the tone in the greeting
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootAutomaticMetadataManual with no fields specified in any projection
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataManualNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> findRequest = builders.findBy("rootAutomaticMetadataManual")
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(findRequest).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //Note here the resource method is doing manual projection, but not removing anything, so we should get all
    //the fields back
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootAutomaticMetadataManual with all custom metadata fields and a single
   * root object entity projection field
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataManualAllFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> findRequest = builders.findBy("rootAutomaticMetadataManual")
        .fields(Greeting.fields().tone())
        .metadataFields(Greeting.fields().tone(), Greeting.fields().id(), Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(findRequest).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, false /*hasMessage*/, false /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //Note here the resource method is doing manual projection, but not removing anything, so we should get all
    //the fields back
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootAutomaticMetadataManual with a single root object entity field and a single
   * custom metadata field which does not exist in Greeting. This is a negative test.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataManualSingleRootNonexistentMeta(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("rootAutomaticMetadataManual")
        .metadataFields(Group.fields().description()) //Note the use of Group here instead of Greeting
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //Note that we are performing a manual projection in the resource method, but not removing anything because
    //there exists an invalid field in the metadata projection mask
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following tests operate on the resource method rootManualMetadataAutomatic

  /**
   * Calls the resource method rootManualMetadataAutomatic with a single metadata field and a single root object
   * entity field
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootManualMetadataAutomaticSingleRootSingleMeta(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("rootManualMetadataAutomatic")
        .fields(Greeting.fields().message())
        .metadataFields(Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    //Note the server behavior here is to preserve the tone and the message even though the projection only specified
    //message
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootManualMetadataAutomatic with no fields specified in any projection
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootManualMetadataAutomaticNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> findRequest = builders.findBy("rootManualMetadataAutomatic")
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(findRequest).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootManualMetadataAutomatic with all custom metadata fields and a single
   * root object entity projection field
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootManualMetadataAutomaticAllFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> findRequest = builders.findBy("rootManualMetadataAutomatic")
        .fields(Greeting.fields().tone())
        .metadataFields(Greeting.fields().tone(), Greeting.fields().id(), Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(findRequest).getResponse();
    //Manual projection on the resource method only works if it sees the message field in the projection
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following tests operate on the resource method rootManualMetadataManual

  /**
   * Calls the resource method rootManualMetadataManual with a single metadata field and a single root object
   * entity field
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootManualMetadataManualSingleRootSingleMeta(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("rootManualMetadataManual")
        .fields(Greeting.fields().message())
        .metadataFields(Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    //Note the following for the resulting entity elements and metadata:
    //The resource method should ideally only preserve only the message, but we intentionally also preserve
    //the tone to verify that it is the resource method who performs the projection and not restli
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootManualMetadataManual with no fields specified in any projection
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootManualMetadataManualNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> findRequest = builders.findBy("rootManualMetadataManual")
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(findRequest).getResponse();
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootManualMetadataManual with all custom metadata fields and a single
   * root object entity projection field
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootManualMetadataManualAllFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> findRequest = builders.findBy("rootManualMetadataManual")
        .fields(Greeting.fields().tone())
        .metadataFields(Greeting.fields().tone(), Greeting.fields().id(), Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(findRequest).getResponse();
    //Manual projection on the resource method only works if it sees the message field in the projection
    assertEntityElements(response.getEntity().getElements(), true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following are single tests against individual resource methods

  /**
   * Calls the resource method getAllRootAutomaticMetadataManual with a single metadata field and a single root object
   * entity field. This is the only test for this method and used to make sure that the GET_ALL code path within restli
   * for metadata projection calculation also works as intended.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGetAllRootAutomaticMetadataManual(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.getAll()
        .fields(Greeting.fields().message())
        .metadataFields(Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    //Since restli does the projection here for the root entity records, only the message field will exist
    assertEntityElements(response.getEntity().getElements(), false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //Since the resource method does the projection here for the meta data, the message and the tone will both
    //exist since the resource method intentionally chooses to preserve both.
    assertCustomMetadata(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  /**
   * Calls the resource method rootAutomaticMetadataAutomaticNull with a single metadata field and a single root object
   * entity field. The purpose of this test is to make sure that restli can handle projections on null metadata on the
   * server.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testRootAutomaticMetadataAutomaticNullSingleRootSingleMeta(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("rootAutomaticMetadataAutomaticNull")
        .fields(Greeting.fields().message())
        .metadataFields(Greeting.fields().message())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    assertEntityElements(response.getEntity().getElements(), false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertNull(response.getEntity().getMetadataRaw());
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertAllPagingFieldsExist(response.getEntity().getPaging());
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][]
    {
        {
            new RootBuilderWrapper<Long, Greeting>(new CustomMetadataProjectionsBuilders())
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new CustomMetadataProjectionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS))
        }
    };
  }

  //Notice for all the assertions below:
  //I could have compacted my assertions, for example:
  //assertEquals(greeting.hasTone(), hasTone, "message") instead of the if-else way I am currently doing it.
  //However I chose to do it this way so that the assertion messages are more clear.
  private static void assertEntityElements(final List<Greeting> entityElementsList, boolean hasTone, boolean hasMessage,
      boolean hasID)
  {
    Assert.assertEquals(2, entityElementsList.size(), "We should have gotten the expected number" +
        " of greetings back!");
    for (final Greeting greeting : entityElementsList)
    {
      assertGreeting(greeting, hasTone, hasMessage, hasID, "Root entity");
    }
  }

  private static void assertCustomMetadata(final Greeting customMetadata, boolean hasTone, boolean hasMessage,
      boolean hasID)
  {
    assertGreeting(customMetadata, hasTone, hasMessage, hasID, "Custom metadata");
  }

  private static void assertGreeting(final Greeting greeting, boolean hasTone, boolean hasMessage,
      boolean hasID, final String type)
  {
    if (hasTone)
    {
      Assert.assertTrue(greeting.hasTone(), type + " object element must have a tone!");
    }
    else
    {
      Assert.assertFalse(greeting.hasTone(), type + " object element must not have a tone!");
    }

    if (hasMessage)
    {
      Assert.assertTrue(greeting.hasMessage(), type + " object element must have a message!");
    }
    else
    {
      Assert.assertFalse(greeting.hasMessage(), type + " object element must not have a message!");
    }

    if (hasID)
    {
      Assert.assertTrue(greeting.hasId(), type + " object element must have an ID!");
    }
    else
    {
      Assert.assertFalse(greeting.hasId(), type + " object element must not have an ID!");
    }
  }

  private static void assertAllPagingFieldsExist(final CollectionMetadata paging)
  {
    Assert.assertTrue(paging.hasTotal(), "Paging must have total!");
    Assert.assertTrue(paging.hasCount(), "Paging must have count!");
    Assert.assertTrue(paging.hasStart(), "Paging must have start!");
    Assert.assertTrue(paging.hasLinks(), "Paging must have links!");
  }
}
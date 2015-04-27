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


import com.linkedin.data.schema.PathSpec;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.PagingMetadataProjectionsBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Karim Vidhani
 */
public class TestPagingProjection extends RestLiIntegrationTest
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

  //The following tests operate on the resource method metadataAutomaticPagingFullyAutomatic

  /**
   * Calls the resource method metadataAutomaticPagingFullyAutomatic with a single metadata field and a single paging
   * field for 'count'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingFullyAutomaticSingleMetaPagingCount(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingFullyAutomatic")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().count())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, true /*hasCount*/, false /*hasLinks*/);
  }

  /**
   * Calls the resource method metadataAutomaticPagingFullyAutomatic with a single metadata field and a single paging
   * field for 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingFullyAutomaticSingleMetaPagingTotal(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingFullyAutomatic")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  /**
   * Calls the resource method metadataAutomaticPagingFullyAutomatic with a single metadata field and multiple paging
   * fields using 'links' and 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingFullyAutomaticSingleMetaMultiplePaging(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingFullyAutomatic")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total(), CollectionMetadata.fields().links())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, true /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  /**
   * Calls the resource method metadataAutomaticPagingFullyAutomatic without any projection fields specified
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingFullyAutomaticNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingFullyAutomatic")
        .metadataFields()
        .pagingFields()
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    //nothing should be sent back for both below
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, false /*hasMessage*/, false /*hasID*/);
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following tests operate on the resource method metadataAutomaticPagingAutomaticPartialNull

  /**
   * Calls the resource method metadataAutomaticPagingAutomaticPartialNull with a single metadata field and a single paging
   * field for 'count'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingAutomaticPartialNullSingleMetaPagingCount(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingAutomaticPartialNull")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().count())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, true /*hasCount*/, false /*hasLinks*/);
  }

  /**
   * Calls the resource method metadataAutomaticPagingAutomaticPartialNull with a single metadata field and a single paging
   * field for 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingAutomaticPartialNullSingleMetaPagingTotal(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingAutomaticPartialNull")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  /**
   * Calls the resource method metadataAutomaticPagingAutomaticPartialNull with a single metadata field and multiple paging
   * fields using 'links' and 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingAutomaticPartialNullSingleMetaMultiplePaging(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingAutomaticPartialNull")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total(), CollectionMetadata.fields().links())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, true /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  /**
   * Calls the resource method metadataAutomaticPagingAutomaticPartialNull without any projection fields specified
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingAutomaticPartialNullNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingAutomaticPartialNull")
        .metadataFields()
        .pagingFields()
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //nothing should be sent back for both below
    assertGreeting(metadataGreeting, false /*hasTone*/, false /*hasMessage*/, false /*hasID*/);
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following tests operate on the resource method metadataAutomaticPagingAutomaticPartialNullIncorrect

  /**
   * Calls the resource method metadataAutomaticPagingAutomaticPartialNullIncorrect with a single metadata field
   * and a single paging field for 'count'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingAutomaticPartialNullIncorrectSingleMetaPagingCount(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingAutomaticPartialNullIncorrect")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().count())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, true /*hasCount*/, false /*hasLinks*/);
  }

  /**
   * Calls the resource method metadataAutomaticPagingAutomaticPartialNullIncorrect with a single metadata field and a single paging
   * field for 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingAutomaticPartialNullIncorrectSingleMetaPagingTotal(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingAutomaticPartialNullIncorrect")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    //Resource method on the server specified null so we shouldn't get anything back
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
    CollectionMetadata paging = response.getEntity().getPaging();
    final int pagingTotal = paging.getTotal();
    Assert.assertEquals(pagingTotal, 0, "We should still get the default of 0");
  }

  /**
   * Calls the resource method metadataAutomaticPagingAutomaticPartialNullIncorrect with a single metadata field and multiple paging
   * fields using 'links' and 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingAutomaticPartialNullIncorrectSingleMetaMultiplePaging(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingAutomaticPartialNullIncorrect")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total(), CollectionMetadata.fields().links())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, false /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, true /*hasLinks*/);
  }

  /**
   * Calls the resource method metadataAutomaticPagingAutomaticPartialNullIncorrect without any projection fields specified
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataAutomaticPagingAutomaticPartialNullIncorrectNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataAutomaticPagingAutomaticPartialNullIncorrect")
        .metadataFields()
        .pagingFields()
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //nothing should be sent back for both below
    assertGreeting(metadataGreeting, false /*hasTone*/, false /*hasMessage*/, false /*hasID*/);
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following tests operate on the resource method metadataManualPagingFullyAutomatic

  /**
   * Calls the resource method metadataManualPagingFullyAutomatic with a single metadata field and a single paging
   * field for 'count'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingFullyAutomaticSingleMetaPagingCount(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingFullyAutomatic")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().count())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //We should ideally only get back the message field in our metadata, but the resource method here intentionally
    //sends back the message and the ID to verify that restli doesn't interfere with a manual metadata projection.
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, true /*hasCount*/, false /*hasLinks*/);
  }

  /**
   * Calls the resource method metadataManualPagingFullyAutomatic with a single metadata field and a single paging
   * field for 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingFullyAutomaticSingleMetaPagingTotal(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingFullyAutomatic")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //We should ideally only get back the message field in our metadata, but the resource method here intentionally
    //sends back the message and the ID to verify that restli doesn't interfere with a manual metadata projection.
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  /**
   * Calls the resource method metadataManualPagingFullyAutomatic with a single metadata field and multiple paging
   * fields using 'links' and 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingFullyAutomaticSingleMetaMultiplePaging(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingFullyAutomatic")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total(), CollectionMetadata.fields().links())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //We should ideally only get back the message field in our metadata, but the resource method here intentionally
    //sends back the message and the ID to verify that restli doesn't interfere with a manual metadata projection.
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, true /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  /**
   * Calls the resource method metadataManualPagingFullyAutomatic without any projection fields specified
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingFullyAutomaticNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingFullyAutomatic")
        .metadataFields()
        .pagingFields()
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //This is manual on the server, so everything should be there
    assertGreeting(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following tests operate on the resource method metadataManualPagingAutomaticPartialNull

  /**
   * Calls the resource method metadataManualPagingAutomaticPartialNull with a single metadata field and a single paging
   * field for 'count'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingAutomaticPartialNullSingleMetaPagingCount(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingAutomaticPartialNull")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().count())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //We should ideally only get back the message field in our metadata, but the resource method here intentionally
    //sends back the message and the ID to verify that restli doesn't interfere with a manual metadata projection.
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, true /*hasCount*/, false /*hasLinks*/);
  }

  /**
   * Calls the resource method metadataManualPagingAutomaticPartialNull with a single metadata field and a single paging
   * field for 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingAutomaticPartialNullSingleMetaPagingTotal(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingAutomaticPartialNull")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //We should ideally only get back the message field in our metadata, but the resource method here intentionally
    //sends back the message and the ID to verify that restli doesn't interfere with a manual metadata projection.
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  /**
   * Calls the resource method metadataManualPagingAutomaticPartialNull with a single metadata field and multiple paging
   * fields using 'links' and 'total'.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingAutomaticPartialNullSingleMetaMultiplePaging(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingAutomaticPartialNull")
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total(), CollectionMetadata.fields().links())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, true /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  /**
   * Calls the resource method metadataManualPagingAutomaticPartialNull without any projection fields specified
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingAutomaticPartialNullNoFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingAutomaticPartialNull")
        .metadataFields()
        .pagingFields()
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    //Manual projection forces everything to be present in the metadata
    assertGreeting(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, false /*hasCount*/, false /*hasLinks*/);
  }

  /**
   * Calls the resource method metadataManualPagingAutomaticPartialNull without even specifying projection fields
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testMetadataManualPagingAutomaticPartialNullNonExistentFields(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("metadataManualPagingAutomaticPartialNull")
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, true /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    //Resource method returned null here for total, so only the total is missing
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, true /*hasStart*/, true /*hasCount*/, true /*hasLinks*/);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //The following are single tests against individual resource methods

  /**
   * Calls the resource method getAllMetadataManualPagingAutomaticPartialNull with a single metadata field and multiple
   * paging fields. This is the only test for this method and used to make sure that the GET_ALL code path within restli
   * for paging projection calculation also works as intended.
   */
  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testGetAllMetadataManualPagingAutomaticPartialNull(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.getAll()
        .metadataFields(Greeting.fields().message())
        .pagingFields(CollectionMetadata.fields().total(), CollectionMetadata.fields().count(),
                      CollectionMetadata.fields().links())
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    final Greeting metadataGreeting = new Greeting(response.getEntity().getMetadataRaw());
    assertGreeting(metadataGreeting, false /*hasTone*/, true /*hasMessage*/, true /*hasID*/);
    Assert.assertTrue(response.getEntity().hasPaging(), "We must have paging!");
    assertPaging(response.getEntity().getPaging(), true /*hasTotal*/, false /*hasStart*/, true /*hasCount*/, true /*hasLinks*/);
    final int totalCount = response.getEntity().getPaging().getTotal();
    Assert.assertEquals(totalCount, 2, "We must have 2 in our count");
  }

  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  public void testProjectionPagingLinkField(RootBuilderWrapper<Long, Greeting> builders)
      throws RemoteInvocationException
  {
    final Request<CollectionResponse<Greeting>> request = builders.findBy("searchWithLinksResult")
        .pagingFields(CollectionMetadata.fields().count(), new PathSpec("links", PathSpec.WILDCARD, "rel"))
        .build();

    final Response<CollectionResponse<Greeting>> response = getClient().sendRequest(request).getResponse();
    //Assert that no metadata was sent back
    Assert.assertNull(response.getEntity().getMetadataRaw(), "We should get no metadata back");
    assertPaging(response.getEntity().getPaging(), false /*hasTotal*/, false /*hasStart*/, true /*hasCount*/, true /*hasLinks*/);
    final CollectionMetadata paging = response.getEntity().getPaging();
    Assert.assertEquals(paging.getLinks().size(), 1, "We should only have one links field");
    Assert.assertFalse(paging.getLinks().get(0).hasHref(), "We should NOT have href in our link!");
    Assert.assertTrue(paging.getLinks().get(0).hasRel(), "We should have rel in our link!");
    Assert.assertFalse(paging.getLinks().get(0).hasTitle(), "We should NOT have title in our link!");
    Assert.assertFalse(paging.getLinks().get(0).hasType(), "We should NOT have type in our link!");
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestBuilderDataProvider")
  private static Object[][] requestBuilderDataProvider()
  {
    return new Object[][]
    {
        {
            new RootBuilderWrapper<Long, Greeting>(new PagingMetadataProjectionsBuilders())
        },
        {
            new RootBuilderWrapper<Long, Greeting>(new PagingMetadataProjectionsBuilders(TestConstants.FORCE_USE_NEXT_OPTIONS))
        }
    };
  }

  private static void assertGreeting(final Greeting greeting, boolean hasTone, boolean hasMessage,
      boolean hasID)
  {
    if (hasTone)
    {
      Assert.assertTrue(greeting.hasTone(), "Custom metadata greeting must have a tone!");
    }
    else
    {
      Assert.assertFalse(greeting.hasTone(), "Custom metadata greeting must not have a tone!");
    }

    if (hasMessage)
    {
      Assert.assertTrue(greeting.hasMessage(), "Custom metadata greeting must have a message!");
    }
    else
    {
      Assert.assertFalse(greeting.hasMessage(), "Custom metadata greeting must not have a message!");
    }

    if (hasID)
    {
      Assert.assertTrue(greeting.hasId(), "Custom metadata greeting must have an ID!");
    }
    else
    {
      Assert.assertFalse(greeting.hasId(), "Custom metadata greeting must not have an ID!");
    }
  }

  private static void assertPaging(final CollectionMetadata paging, boolean hasTotal, boolean hasStart, boolean hasCount,
      boolean hasLinks)
  {
    if (hasTotal)
    {
      Assert.assertTrue(paging.hasTotal(), "Paging must have total!");
    }
    else
    {
      Assert.assertFalse(paging.hasTotal(), "Paging must not have total!");
    }

    if(hasStart)
    {
      Assert.assertTrue(paging.hasStart(), "Paging must have start!");
    }
    else
    {
      Assert.assertFalse(paging.hasStart(), "Paging must not have start!");
    }

    if(hasCount)
    {
      Assert.assertTrue(paging.hasCount(), "Paging must have count!");
    }
    else
    {
      Assert.assertFalse(paging.hasCount(), "Paging must not have count!");
    }

    if(hasLinks)
    {
      Assert.assertTrue(paging.hasLinks(), "Paging must have links!");
    }
    else
    {
      Assert.assertFalse(paging.hasLinks(), "Paging must not have links!");
    }
  }
}
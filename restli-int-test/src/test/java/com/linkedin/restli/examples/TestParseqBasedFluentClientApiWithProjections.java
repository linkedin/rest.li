/*
   Copyright (c) 2021 LinkedIn Corp.

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

import com.linkedin.parseq.ParSeqUnitTestHelper;
import com.linkedin.restli.client.ParSeqRestliClient;
import com.linkedin.restli.client.ParSeqRestliClientBuilder;
import com.linkedin.restli.client.ParSeqRestliClientConfigBuilder;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingCriteria;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.examples.greetings.api.ValidationDemo;
import com.linkedin.restli.examples.greetings.api.myItem;
import com.linkedin.restli.examples.greetings.api.myRecord;
import com.linkedin.restli.examples.greetings.client.AutoValidationWithProjection;
import com.linkedin.restli.examples.greetings.client.AutoValidationWithProjectionFluentClient;
import com.linkedin.restli.examples.greetings.client.Batchfinders;
import com.linkedin.restli.examples.greetings.client.BatchfindersFluentClient;
import com.linkedin.restli.examples.greetings.client.CreateGreeting;
import com.linkedin.restli.examples.greetings.client.CreateGreetingFluentClient;
import com.linkedin.restli.examples.greetings.client.Greetings;
import com.linkedin.restli.examples.greetings.client.GreetingsFluentClient;
import com.linkedin.restli.examples.greetings.client.ManualProjections;
import com.linkedin.restli.examples.greetings.client.ManualProjectionsFluentClient;
import com.linkedin.restli.examples.greetings.client.PagingMetadataProjections;
import com.linkedin.restli.examples.greetings.client.PagingMetadataProjectionsFluentClient;
import com.linkedin.restli.examples.greetings.client.PartialUpdateGreeting;
import com.linkedin.restli.examples.greetings.client.PartialUpdateGreetingFluentClient;
import com.linkedin.restli.server.validation.RestLiValidationFilter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.collections.Sets;


/**
 * Test fluent client bindings with projections.
 */
public class TestParseqBasedFluentClientApiWithProjections extends RestLiIntegrationTest
{
  public static final String MESSAGE = "Create a new greeting";
  ParSeqUnitTestHelper _parSeqUnitTestHelper;
  ParSeqRestliClient _parSeqRestliClient;

  @BeforeClass
  void setUp() throws Exception
  {
    super.init(Arrays.asList(new RestLiValidationFilter()));
    _parSeqUnitTestHelper = new ParSeqUnitTestHelper();
    _parSeqUnitTestHelper.setUp();
    _parSeqRestliClient = new ParSeqRestliClientBuilder()
        .setClient(getClient())
        .setConfig(new ParSeqRestliClientConfigBuilder().build())
        .build();
  }

  @AfterClass
  void tearDown() throws Exception
  {
    if (_parSeqUnitTestHelper != null)
    {
      _parSeqUnitTestHelper.tearDown();
    }
    else
    {
      throw new RuntimeException("Tried to shut down Engine but it either has not even been created or has "
          + "already been shut down");
    }
    super.shutdown();
  }

  // Test Get request with simple projection
  @Test
  public void testGetWithProjection() throws Exception
  {
    ManualProjections projections = new ManualProjectionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Greeting> result = projections.get(1L,
        optionalParams -> optionalParams.withMask(mask -> mask.withMessage()));
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Greeting greeting = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertFalse(greeting.hasId());
    Assert.assertFalse(greeting.hasTone());
    Assert.assertTrue(greeting.hasMessage());
  }

  @Test
  public void testBatchGetWithProjection() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Set<Long> ids = Sets.newHashSet(Arrays.asList(1L, 2L, 3L));
    CompletionStage<Map<Long, EntityResponse<Greeting>>> result = greetings.batchGet(ids,
        optionalParams -> optionalParams.withMask(mask -> mask.withTone()));
    CompletableFuture<Map<Long, EntityResponse<Greeting>>> future = result.toCompletableFuture();
    Map<Long, EntityResponse<Greeting>> resultMap = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(resultMap.size(), ids.size());
    for (Long id : ids)
    {
      EntityResponse<Greeting> g = resultMap.get(id);
      Assert.assertNotNull(g);
      Assert.assertTrue(g.hasEntry());
      Assert.assertFalse(g.getEntity().hasId());
      Assert.assertFalse(g.getEntity().hasMessage());
      Assert.assertTrue(g.getEntity().hasTone());
    }
  }

  @Test
  public void testCreateAndGetWithProjection() throws Exception
  {
    CreateGreeting greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    String msg = Double.toString(Math.random());
    CompletionStage<IdEntityResponse<Long, Greeting>> result = greetings.createAndGet(getGreeting(msg),
        optionalParams -> optionalParams.withMask(mask -> mask.withMessage()));
    CompletableFuture<IdEntityResponse<Long, Greeting>> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
    Assert.assertFalse(future.get().getEntity().hasId());
    Assert.assertFalse(future.get().getEntity().hasTone());
    Assert.assertEquals(msg, future.get().getEntity().getMessage());
  }

  @Test
  public void testBatchCreateAndGetWithProjection() throws Exception
  {
    CreateGreeting greetings = new CreateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    String msg1 = Double.toString(Math.random());
    String msg2 = Double.toString(Math.random());
    CompletionStage<List<CreateIdEntityStatus<Long, Greeting>>>
        result = greetings.batchCreateAndGet(Arrays.asList(getGreeting(msg1), getGreeting(msg2)),
        optionalParams -> optionalParams.withMask(mask -> mask.withId()));
    CompletableFuture<List<CreateIdEntityStatus<Long, Greeting>>> future = result.toCompletableFuture();
    List<CreateIdEntityStatus<Long, Greeting>> entities = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(entities.size(), 2);
    Assert.assertNotNull(entities.get(0).getEntity());
    Assert.assertFalse(entities.get(0).getEntity().hasMessage());
    Assert.assertTrue(entities.get(0).getEntity().hasId());
    Assert.assertNotNull(entities.get(1).getEntity());
    Assert.assertFalse(entities.get(1).getEntity().hasMessage());
    Assert.assertTrue(entities.get(1).getEntity().hasId());
  }

  @Test
  public void testPartialUpdateAndGetWithProjection() throws Exception
  {
    PartialUpdateGreeting
        greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    CompletionStage<Greeting> result = greetings.partialUpdateAndGet(21L, PatchGenerator.diff(original, update),
        optionalParams -> optionalParams.withMask(mask -> mask.withId()));
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Greeting greeting = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertFalse(greeting.hasMessage());
    Assert.assertTrue(greeting.hasId());
  }

  @Test
  public void testBatchPartialUpdateAndGetWithProjection() throws Exception
  {
    PartialUpdateGreeting greetings = new PartialUpdateGreetingFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    Map<Long, PatchRequest<Greeting>> inputs = new HashMap<>();
    Greeting original = getGreeting();
    String message = "Edited message: fluent api test partialUpdateAndGet";
    Greeting update = getGreeting(message);
    inputs.put(21L, PatchGenerator.diff(original, update));
    inputs.put(22L, PatchGenerator.diff(original, update));
    CompletionStage<Map<Long, UpdateEntityStatus<Greeting>>> result = greetings.batchPartialUpdateAndGet(inputs,
        optionalParams -> optionalParams.withMask(mask -> mask.withId().withMessage().withTone()));
    CompletableFuture<Map<Long, UpdateEntityStatus<Greeting>>> future = result.toCompletableFuture();
    Assert.assertNotNull(future.get(5000, TimeUnit.MILLISECONDS));
    Assert.assertEquals(future.get().get(21L).getEntity().getId().longValue(), 21L);
    Assert.assertEquals(future.get().get(21L).getEntity().getMessage(), message);
    Assert.assertEquals(future.get().get(22L).getEntity().getId().longValue(), 22L);
    Assert.assertEquals(future.get().get(22L).getEntity().getMessage(), message);
  }

  @Test
  public void testGetAllWithFieldProjection() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    // Create some greetings with "GetAll" in message so they will be returned by getAll test method..
    CompletionStage<List<CreateIdStatus<Long>>> createResult = greetings.batchCreate(
        Arrays.asList(getGreeting("GetAll").setId(200L), getGreeting("GetAll").setId(201L)));

    CompletionStage<CollectionResponse<Greeting>> result = createResult.thenCompose(ids -> greetings.getAll(
        optionalParams -> optionalParams.withMask(mask -> mask.withMessage())));
    CompletableFuture<CollectionResponse<Greeting>> future = result.toCompletableFuture();
    List<Greeting> greetingList = future.get(5000, TimeUnit.MILLISECONDS).getElements();
    Assert.assertTrue(greetingList.size() >= 2);
    for (Greeting greeting :greetingList)
    {
      Assert.assertFalse(greeting.hasId());
      Assert.assertTrue(greeting.getMessage().contains("GetAll"));
    }
  }

  @Test
  public void testGetAllWithPagingProjection() throws Exception
  {
    PagingMetadataProjections
        greetings = new PagingMetadataProjectionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<CollectionResponse<Greeting>> result = greetings.getAll(
        params -> params.withPagingMask(pageMask -> pageMask.withTotal().withStart())
    );
    CompletableFuture<CollectionResponse<Greeting>> future = result.toCompletableFuture();
    CollectionResponse<Greeting> greetingResult = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greetingResult.hasPaging());
    Assert.assertFalse(greetingResult.getPaging().hasCount());
    Assert.assertTrue(greetingResult.getPaging().hasStart());
    Assert.assertEquals((int) greetingResult.getPaging().getStart(), 0);
    Assert.assertFalse(greetingResult.getPaging().hasLinks());
    Assert.assertTrue(greetingResult.getPaging().getTotal() > 0);

    // Same request without projection.
    result = greetings.getAll();
    future = result.toCompletableFuture();
    greetingResult = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greetingResult.hasPaging());
    Assert.assertTrue(greetingResult.getPaging().hasCount());
    Assert.assertTrue(greetingResult.getPaging().hasStart());
    Assert.assertTrue(greetingResult.getPaging().hasLinks());
    // The resource applies manual projection and returns total only when it is explicitly projected. So no total field here.
  }

  @Test
  public void testGetAllWithMetadataProjection() throws Exception
  {
    PagingMetadataProjections greetings = new PagingMetadataProjectionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<CollectionResponse<Greeting>> result = greetings.getAll(
        params -> params.withMetadataMask(metadataMask -> metadataMask.withMessage())
    );
    CompletableFuture<CollectionResponse<Greeting>> future = result.toCompletableFuture();
    CollectionResponse<Greeting> greetingResult = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(greetingResult.getMetadataRaw());
    Greeting metadata = new Greeting(greetingResult.getMetadataRaw());
    Assert.assertTrue(metadata.hasMessage());
    Assert.assertFalse(metadata.hasTone());

    // Same request without projection.
    result = greetings.findByMetadataAutomaticPagingFullyAutomatic();
    future = result.toCompletableFuture();
    greetingResult = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(greetingResult.getMetadataRaw());
    metadata = new Greeting(greetingResult.getMetadataRaw());
    Assert.assertTrue(metadata.hasMessage());
    Assert.assertTrue(metadata.hasTone());
  }

  @Test
  public void testFinderWithProjection() throws Exception
  {
    Greetings greetings = new GreetingsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<CollectionResponse<Greeting>> result = greetings.findBySearch(
        params -> params.setTone(Tone.FRIENDLY)
            .withMask(fieldsMask -> fieldsMask.withTone().withMessage())
    );
    CompletableFuture<CollectionResponse<Greeting>> future = result.toCompletableFuture();
    List<Greeting> greetingList = future.get(5000, TimeUnit.MILLISECONDS).getElements();
    Assert.assertTrue(greetingList.size() > 0);
    for (Greeting greeting : greetingList)
    {
      Assert.assertEquals(greeting.getTone(), Tone.FRIENDLY);
      Assert.assertFalse(greeting.hasId());
      Assert.assertTrue(greeting.hasMessage());
    }
  }

  @Test
  public void testFinderWithPagingProjection() throws Exception
  {
    PagingMetadataProjections greetings = new PagingMetadataProjectionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<CollectionResponse<Greeting>> result = greetings.findByMetadataAutomaticPagingFullyAutomatic(
        params -> params.withPagingMask(pageMask -> pageMask.withTotal())
    );
    CompletableFuture<CollectionResponse<Greeting>> future = result.toCompletableFuture();
    CollectionResponse<Greeting> greetingResult = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greetingResult.hasPaging());
    Assert.assertFalse(greetingResult.getPaging().hasCount());
    Assert.assertFalse(greetingResult.getPaging().hasStart());
    Assert.assertFalse(greetingResult.getPaging().hasLinks());
    Assert.assertTrue(greetingResult.getPaging().hasTotal());
    Assert.assertTrue(greetingResult.getPaging().getTotal() > 0);

    // Same request without projection.
    result = greetings.findByMetadataAutomaticPagingFullyAutomatic();
    future = result.toCompletableFuture();
    greetingResult = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greetingResult.hasPaging());
    Assert.assertTrue(greetingResult.getPaging().hasCount());
    Assert.assertTrue(greetingResult.getPaging().hasStart());
    Assert.assertTrue(greetingResult.getPaging().hasLinks());
    Assert.assertTrue(greetingResult.getPaging().hasTotal());
    Assert.assertTrue(greetingResult.getPaging().getTotal() > 0);
  }

  @Test
  public void testFinderWithMetadataProjection() throws Exception
  {
    PagingMetadataProjections greetings = new PagingMetadataProjectionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<CollectionResponse<Greeting>> result = greetings.findByMetadataAutomaticPagingFullyAutomatic(
        params -> params.withMetadataMask(metadataMask -> metadataMask.withId())
    );
    CompletableFuture<CollectionResponse<Greeting>> future = result.toCompletableFuture();
    CollectionResponse<Greeting> greetingResult = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(greetingResult.getMetadataRaw());
    Greeting metadata = new Greeting(greetingResult.getMetadataRaw());
    Assert.assertFalse(metadata.hasMessage());
    Assert.assertFalse(metadata.hasTone());
    Assert.assertTrue(metadata.hasId());

    // Same request without projection.
    result = greetings.findByMetadataAutomaticPagingFullyAutomatic();
    future = result.toCompletableFuture();
    greetingResult = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(greetingResult.getMetadataRaw());
    metadata = new Greeting(greetingResult.getMetadataRaw());
    Assert.assertTrue(metadata.hasMessage());
    Assert.assertTrue(metadata.hasTone());
    Assert.assertTrue(metadata.hasId());
  }

  @Test
  public void testBatchFinderWithProjection() throws Exception
  {
    Batchfinders batchFinders = new BatchfindersFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    GreetingCriteria c1 = new GreetingCriteria().setId(1L).setTone(Tone.SINCERE);
    GreetingCriteria c2 = new GreetingCriteria().setId(2L).setTone(Tone.FRIENDLY);
    CompletionStage<BatchCollectionResponse<Greeting>> result = batchFinders.findBySearchGreetings(
        Arrays.asList(c1, c2), "hello world",
        params -> params.withMask(mask -> mask.withTone().withId()));
    CompletableFuture<BatchCollectionResponse<Greeting>> future = result.toCompletableFuture();
    List<BatchFinderCriteriaResult<Greeting>> batchResult = future.get(5000, TimeUnit.MILLISECONDS).getResults();

    List<Greeting> greetings1 = batchResult.get(0).getElements();
    Assert.assertTrue(greetings1.get(0).hasTone());
    Assert.assertEquals(greetings1.get(0).getTone(), Tone.SINCERE);
    Assert.assertTrue(greetings1.get(0).hasId());
    Assert.assertFalse(greetings1.get(0).hasMessage());

    List<Greeting> greetings2 = batchResult.get(1).getElements();
    Assert.assertTrue(greetings2.get(0).hasId());
    Assert.assertEquals(greetings2.get(0).getTone(), Tone.FRIENDLY);
    Assert.assertTrue(greetings2.get(0).hasId());
    Assert.assertFalse(greetings2.get(0).hasMessage());
  }

  @Test
  public void testBatchFinderWithPagingProjection() throws Exception
  {
    Batchfinders batchFinders = new BatchfindersFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    GreetingCriteria c1 = new GreetingCriteria().setId(1L).setTone(Tone.SINCERE);
    GreetingCriteria c2 = new GreetingCriteria().setId(2L).setTone(Tone.FRIENDLY);
    CompletionStage<BatchCollectionResponse<Greeting>> result = batchFinders.findBySearchGreetings(
        Arrays.asList(c1, c2), "hello world",
        params -> params.withPagingMask(mask -> mask.withStart())
            .withMask(fieldMask -> fieldMask.withId().withTone())
    );
    CompletableFuture<BatchCollectionResponse<Greeting>> future = result.toCompletableFuture();
    List<BatchFinderCriteriaResult<Greeting>> batchResult = future.get(5000, TimeUnit.MILLISECONDS).getResults();

    for (BatchFinderCriteriaResult<?> singleCriteria : batchResult)
    {
      CollectionMetadata paging = singleCriteria.getPaging();
      Assert.assertTrue(paging.hasStart());
      Assert.assertFalse(paging.hasCount());
      Assert.assertFalse(paging.hasTotal());
      Assert.assertFalse(paging.hasLinks());
    }

    // Same request without paging mask (setting field mask to avoid validation error on server)
    result = batchFinders.findBySearchGreetings(Arrays.asList(c1, c2), "hello world",
        params -> params.withMask(fieldMask -> fieldMask.withId().withTone())
    );
    future = result.toCompletableFuture();
    batchResult = future.get(5000, TimeUnit.MILLISECONDS).getResults();

    for (BatchFinderCriteriaResult<?> singleCriteria : batchResult)
    {
      CollectionMetadata paging = singleCriteria.getPaging();
      Assert.assertTrue(paging.hasStart());
      Assert.assertTrue(paging.hasCount());
      Assert.assertTrue(paging.hasTotal());
      Assert.assertTrue(paging.hasLinks());
    }
  }

  // Test Get request without projection
  @Test
  public void testGetFull() throws Exception
  {
    ManualProjections projections = new ManualProjectionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Greeting> result = projections.get(1L);
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Greeting greeting = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(greeting.hasId());
    Assert.assertTrue(greeting.hasTone());
    Assert.assertTrue(greeting.hasMessage());
  }

  // Test using optional parameter that disables projection
  @Test
  public void testGetWithProjectionDisabled() throws Exception
  {
    ManualProjections projections = new ManualProjectionsFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<Greeting> result = projections.get(1L,
        optionalParams -> optionalParams.withMask(mask -> mask.withMessage())
            .setIgnoreProjection(true));
    CompletableFuture<Greeting> future = result.toCompletableFuture();
    Greeting greeting = future.get(5000, TimeUnit.MILLISECONDS);
    // these fields would have been excluded by the framework if automatic projection was enabled
    Assert.assertTrue(greeting.hasId());
    Assert.assertTrue(greeting.hasTone());
  }

  @Test
  public void testValidationWithNoProjection() throws Exception
  {
    AutoValidationWithProjection
        validationDemos = new AutoValidationWithProjectionFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());
    CompletionStage<ValidationDemo> result = validationDemos.get(1);
    try {
      CompletableFuture<ValidationDemo> future = result.toCompletableFuture();
      future.get(5000, TimeUnit.MILLISECONDS);
      Assert.fail("Request should have failed validation");
    } catch (ExecutionException e) {
      Assert.assertTrue(e.getCause() instanceof RestLiResponseException);
      RestLiResponseException responseException = (RestLiResponseException) e.getCause();
      Assert.assertEquals(responseException.getServiceErrorMessage(),
          TestRestLiValidationWithProjection.EXPECTED_VALIDATION_DEMO_FAILURE_MESSAGE);
    }
  }

  @Test
  public void testValidationWithOnlyValidFieldsProjected() throws Exception
  {
    AutoValidationWithProjection validationDemos = new AutoValidationWithProjectionFluentClient(_parSeqRestliClient, _parSeqUnitTestHelper.getEngine());

    CompletionStage<ValidationDemo> result = validationDemos.get(1,
        optionalParams -> optionalParams.withMask(mask -> mask.withStringB()
            .withIncludedB()
            .withUnionFieldWithInlineRecord(m1 -> m1.withMyRecord(myRecord.ProjectionMask::withFoo2))
            .withArrayWithInlineRecord(itemMask -> itemMask.withItems(myItem.ProjectionMask::withBar1))
            .withMapWithTyperefs(m -> m.withValues(Greeting.ProjectionMask::withId))
            .withValidationDemoNext(ValidationDemo.ProjectionMask::withIntB)));
    CompletableFuture<ValidationDemo> future = result.toCompletableFuture();
    ValidationDemo validationDemo = future.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(validationDemo);
  }

  private Greeting getGreeting()
  {
    return getGreeting(MESSAGE);
  }

  private Greeting getGreeting(String message)
  {
    return new Greeting().setMessage(message).setTone(Tone.FRIENDLY);
  }
}

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

package com.linkedin.restli.examples.greetings.server;


import com.google.common.collect.ImmutableList;
import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.MetadataProjectionParam;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.PagingProjectionParam;
import com.linkedin.restli.server.annotations.ProjectionParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.ArrayList;
import java.util.List;


/**
 * Resource methods to apply a mixture of automatic/manual projection for root object entities as well as the custom
 * metadata entity returned in a CollectionResult.
 * Note that we intentionally pass in MaskTrees for root object projection, custom metadata projection and paging
 * projection to verify RestliAnnotationReader's ability to properly construct the correct arguments when
 * reflectively calling resource methods.
 *
 * @author Karim Vidhani
 */
@RestLiCollection(name = "customMetadataProjections", namespace = "com.linkedin.restli.examples.greetings.client")
public class CustomMetadataProjectionResource extends CollectionResourceTemplate<Long, Greeting>
{
  private static final Greeting GREETING_ONE;
  private static final Greeting GREETING_TWO;
  private static final List<Greeting> LIST;
  private static final Greeting CUSTOM_METADATA_GREETING;

  //Note that we clone our Greeting objects for responses so we don't tamper with our original data below
  static
  {
    GREETING_ONE = new Greeting();
    GREETING_ONE.setTone(Tone.INSULTING);
    GREETING_ONE.setId(18l);
    GREETING_ONE.setMessage("I don't like you!");

    GREETING_TWO = new Greeting();
    GREETING_TWO.setTone(Tone.FRIENDLY);
    GREETING_TWO.setId(15l);
    GREETING_TWO.setMessage("I really like you!");

    LIST = new ArrayList<Greeting>();
    LIST.add(GREETING_ONE);
    LIST.add(GREETING_TWO);

    CUSTOM_METADATA_GREETING = new Greeting();
    CUSTOM_METADATA_GREETING.setTone(Tone.SINCERE);
    CUSTOM_METADATA_GREETING.setId(40l);
    CUSTOM_METADATA_GREETING.setMessage("I genuinely feel neutral about you!");
  }

  /**
   * This resource method performs automatic projection for the root object entities and also the custom metadata.
   */
  @Finder("rootAutomaticMetadataAutomatic")
  public CollectionResult<Greeting, Greeting> rootAutomaticMetadataAutomatic(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection)
  {
    return new CollectionResult<Greeting, Greeting>(LIST, 2, CUSTOM_METADATA_GREETING);
  }

  /**
   * This resource method performs automatic projection for the root object entities and manual projection for the
   * custom metadata.
   */
  @Finder("rootAutomaticMetadataManual")
  public CollectionResult<Greeting, Greeting> rootAutomaticMetadataManual(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection) throws CloneNotSupportedException
  {
    super.getContext().setMetadataProjectionMode(ProjectionMode.MANUAL);
    return new CollectionResult<Greeting, Greeting>(LIST, 2, applyMetadataProjection(metadataProjection));
  }

  /**
   * This resource method performs manual projection for the root object entities and automatic projection for the
   * custom metadata.
   */
  @Finder("rootManualMetadataAutomatic")
  public CollectionResult<Greeting, Greeting> rootManualMetadataAutomatic(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection) throws CloneNotSupportedException
  {
    super.getContext().setProjectionMode(ProjectionMode.MANUAL);
    return new CollectionResult<Greeting, Greeting>(applyRootObjectProjection(rootObjectProjection),
        2, CUSTOM_METADATA_GREETING);
  }

  /**
   * This resource method performs manual projection for the root object entities and manual projection for the
   * custom metadata. Comments excluded since its combining behavior from the previous tests.
   */
  @Finder("rootManualMetadataManual")
  public CollectionResult<Greeting, Greeting> rootManualMetadataManual(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection) throws CloneNotSupportedException
  {
    super.getContext().setMetadataProjectionMode(ProjectionMode.MANUAL);
    super.getContext().setProjectionMode(ProjectionMode.MANUAL);
    return new CollectionResult<Greeting, Greeting>(applyRootObjectProjection(rootObjectProjection),
        2, applyMetadataProjection(metadataProjection));
  }

  /**
   * This resource method is a variant of the rootAutomaticMetadataManual finder above, except it uses GET_ALL.
   * This test is to make sure that GET_ALL observes the same code path in restli as FINDER does for projection.
   * Redundant comments excluded for the sake of brevity.
   */
  @RestMethod.GetAll
  public CollectionResult<Greeting, Greeting> getAllRootAutomaticMetadataManual(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection) throws CloneNotSupportedException
  {
    super.getContext().setMetadataProjectionMode(ProjectionMode.MANUAL);
    return new CollectionResult<Greeting, Greeting>(LIST, 2, applyMetadataProjection(metadataProjection));
  }

  /**
   * This resource method performs automatic projection for the root object entities and automatic on the metadata
   * as well. The caveat here is that the metadata returned by the resource method is null. We want to make sure
   * restli doesn't fall over when it sees the null later on.
   */
  @Finder("rootAutomaticMetadataAutomaticNull")
  public CollectionResult<Greeting, Greeting> rootAutomaticMetadataAutomaticNull(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection)
  {
    return new CollectionResult<Greeting, Greeting>(LIST, 2, null);
  }

  private List<Greeting> applyRootObjectProjection(final MaskTree rootObjectProjection) throws CloneNotSupportedException
  {
    final Greeting clonedGreetingOne = GREETING_ONE.clone();
    final Greeting clonedGreetingTwo = GREETING_TWO.clone();
    if (rootObjectProjection != null && rootObjectProjection.getOperations().size() == 1 &&
        rootObjectProjection.getOperations().get(Greeting.fields().message()) == MaskOperation.POSITIVE_MASK_OP)
    {
      clonedGreetingOne.removeId();
      clonedGreetingTwo.removeId();
      //Note that technically the correct behavior here would be to remove not only the ID, but also the tone.
      //However since we are testing to make sure that manual root object projection works as intended, we will
      //intentionally apply an incorrect projection by hand to verify restli doesn't interfere with it.
    }
    return ImmutableList.of(clonedGreetingOne, clonedGreetingTwo);
  }

  private Greeting applyMetadataProjection(final MaskTree metadataProjection) throws CloneNotSupportedException
  {
    final Greeting clonedGreeting = CUSTOM_METADATA_GREETING.clone();
    if (metadataProjection != null && metadataProjection.getOperations().size() == 1 &&
        metadataProjection.getOperations().get(Greeting.fields().message()) == MaskOperation.POSITIVE_MASK_OP)
    {
      clonedGreeting.removeId();
      //Note that technically the correct behavior here would be to remove not only the ID, but also the tone.
      //However since we are testing to make sure that manual custom metadata projection works as intended, we will
      //intentionally apply an incorrect projection by hand to verify restli doesn't interfere with it.
    }
    return clonedGreeting;
  }
}
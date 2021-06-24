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


import com.linkedin.data.transform.filter.request.MaskOperation;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.examples.greetings.api.Empty;
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
 * Resource methods for automatic projection for paging in addition to a mixture of automatic/manual projection for
 * custom metadata.
 * Note that we intentionally pass in MaskTrees for root object entity projection, custom metadata projection and paging
 * projection to verify RestliAnnotationReader's ability to properly construct the correct arguments when
 * reflectively calling resource methods.
 * Also note that resource methods cannot project paging (CollectionMetadata) with the exception of
 * intentionally setting total to NULL when constructing CollectionResult.
 *
 * @author Karim Vidhani
 */
@RestLiCollection(name = "pagingMetadataProjections", namespace = "com.linkedin.restli.examples.greetings.client")
public class PagingProjectionResource extends CollectionResourceTemplate<Long, Greeting>
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

    LIST = new ArrayList<>();
    LIST.add(GREETING_ONE);
    LIST.add(GREETING_TWO);

    CUSTOM_METADATA_GREETING = new Greeting();
    CUSTOM_METADATA_GREETING.setTone(Tone.SINCERE);
    CUSTOM_METADATA_GREETING.setId(40l);
    CUSTOM_METADATA_GREETING.setMessage("I genuinely feel neutral about you!");
  }

  /**
   * This resource method performs automatic projection for the custom metadata and complete automatic projection
   * for paging. This means that it will provide a total in its construction of CollectionResult.
   */
  @Finder("metadataAutomaticPagingFullyAutomatic")
  public CollectionResult<Greeting, Greeting> metadataAutomaticPagingFullyAutomatic(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection)
  {
    return new CollectionResult<>(LIST, 2, CUSTOM_METADATA_GREETING);
  }

  /**
   * This resource method performs automatic projection for the custom metadata and automatic projection
   * for paging. This particular resource method also varies on what it sets total to.
   */
  @Finder("metadataAutomaticPagingAutomaticPartialNull")
  public CollectionResult<Greeting, Greeting> metadataAutomaticPagingAutomaticPartialNull(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection)
  {
    return new CollectionResult<>(LIST, calculateTotal(pagingProjection), CUSTOM_METADATA_GREETING);
  }

  /**
   * This resource method performs automatic projection for the custom metadata and automatic projection
   * for paging. This particular resource method also varies on what it sets total to.
   * The caveat with this test is that it incorrectly assigns a non null value for the total
   * even though the MaskTree says to exclude it.
   */
  @Finder("metadataAutomaticPagingAutomaticPartialNullIncorrect")
  public CollectionResult<Greeting, Greeting> metadataAutomaticPagingAutomaticPartialNullIncorrect(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection)
  {
    final Integer total;
    //We then inspect the paging mask tree and incorrectly apply the total projection
    if (pagingProjection != null &&
        pagingProjection.getOperations().get(CollectionMetadata.fields().total()) == null)
    {
      total = 2;
    }
    else
    {
      total = null;
    }
    //Restli should correctly strip away the total (because its not in the MaskTree) even though the resource
    //method returned it here
    return new CollectionResult<>(LIST, total, CUSTOM_METADATA_GREETING);
  }

  /**
   * This resource method performs manual projection for the custom metadata and automatic projection
   * for Paging.
   */
  @Finder("metadataManualPagingFullyAutomatic")
  public CollectionResult<Greeting, Greeting> metadataManualPagingFullyAutomatic(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection) throws CloneNotSupportedException
  {
    super.getContext().setMetadataProjectionMode(ProjectionMode.MANUAL);
    return new CollectionResult<>(LIST, 2, applyMetadataProjection(metadataProjection));
  }

  /**
   * This resource method performs manual projection for the custom metadata and automatic projection
   * for paging. This particular resource method also varies on what it sets total to.
   * Comments excluded since its combining behavior from the previous tests.
   */
  @Finder("metadataManualPagingAutomaticPartialNull")
  public CollectionResult<Greeting, Greeting> metadataManualPagingAutomaticPartialNull(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection) throws CloneNotSupportedException
  {
    super.getContext().setMetadataProjectionMode(ProjectionMode.MANUAL);
    return new CollectionResult<>(LIST,
        calculateTotal(pagingProjection), applyMetadataProjection(metadataProjection));
  }

  /**
   * Same as the test above except that this test is to make sure that GET_ALL observes the same code path in
   * restli as FINDER does for custom metadata and paging projection.
   * Redundant comments excluded for the sake of brevity.
   */
  @RestMethod.GetAll
  public CollectionResult<Greeting, Greeting> getAllMetadataManualPagingAutomaticPartialNull(
      final @PagingContextParam PagingContext ctx,
      final @ProjectionParam MaskTree rootObjectProjection,
      final @MetadataProjectionParam MaskTree metadataProjection,
      final @PagingProjectionParam MaskTree pagingProjection) throws CloneNotSupportedException
  {
    super.getContext().setMetadataProjectionMode(ProjectionMode.MANUAL);
    return new CollectionResult<>(LIST, calculateTotal(pagingProjection),
        applyMetadataProjection(metadataProjection));
  }

  private Integer calculateTotal(final MaskTree pagingProjection)
  {
    final Integer total;
    if (pagingProjection != null &&
        pagingProjection.getOperations().get(CollectionMetadata.fields().total()) == MaskOperation.POSITIVE_MASK_OP)
    {
      total = 2;
    }
    else
    {
      total = null;
    }
    return total;
  }

  private Greeting applyMetadataProjection(final MaskTree metadataProjection) throws CloneNotSupportedException
  {
    //We then inspect the mask tree and apply an arbitrary projection
    final Greeting clonedGreeting = CUSTOM_METADATA_GREETING.clone();
    if (metadataProjection != null && metadataProjection.getOperations().size() == 1
        && metadataProjection.getOperations().get(Greeting.fields().message()) == MaskOperation.POSITIVE_MASK_OP)
    {
      clonedGreeting.removeTone();
      //Note that technically the correct behavior here would be to remove not only the tone, but also the ID.
      //However since we are testing to make sure that manual custom metadata projection works as intended, we will
      //intentionally apply an incorrect projection by hand to verify restli doesn't interfere with it.
    }
    return clonedGreeting;
  }

  /**
   * This resource method is used to create additional paging metadata for fields such as links. Client side
   * tests can use this method to potentially project on fields inside of links.
   */
  @Finder("searchWithLinksResult")
  public CollectionResult<Greeting, Empty> searchWithLinksResult(@PagingContextParam PagingContext ctx)
  {
    List<Greeting> greetings = new ArrayList<>();
    for (int i = 0; i<5; i++)
    {
      greetings.add(GREETING_ONE);
      greetings.add(GREETING_TWO);
    }

    return new CollectionResult<>(greetings, 50);
  }
}

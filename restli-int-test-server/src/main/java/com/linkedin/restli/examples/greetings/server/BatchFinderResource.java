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

package com.linkedin.restli.examples.greetings.server;

import com.linkedin.data.ByteString;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Empty;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingCriteria;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.Arrays;
import java.util.Collections;



/**
 * This resource models a collection resource that exposes both a finder and a batch finder method.
 *
 * @author Maxime Lamure
 */
@RestLiCollection(name = "batchfinders", namespace = "com.linkedin.restli.examples.greetings.client")
public class BatchFinderResource extends CollectionResourceTemplate<Long, Greeting>
{
  private static final Greeting g1 = new Greeting().setId(1).setTone(Tone.SINCERE);
  private static final Greeting g2 = new Greeting().setId(2).setTone(Tone.FRIENDLY);

  @BatchFinder(value = "searchGreetings", batchParam = "criteria")
  public BatchFinderResult<GreetingCriteria, Greeting, Empty> searchGreetings(@PagingContextParam PagingContext context,
                                                                @QueryParam("criteria") GreetingCriteria[] criteria,
                                                                @QueryParam("message") String message)
  {
    BatchFinderResult<GreetingCriteria, Greeting, Empty> batchFinderResult = new BatchFinderResult<>();

    for (GreetingCriteria currentCriteria: criteria) {
      if (currentCriteria.getId() == 1L) {
        // on success
        CollectionResult<Greeting, Empty> c1 = new CollectionResult<>(Arrays.asList(g1), 1);
        batchFinderResult.putResult(currentCriteria, c1);
      } else if (currentCriteria.getId() == 2L) {
        CollectionResult<Greeting, Empty> c2 = new CollectionResult<>(Arrays.asList(g2), 1);
        batchFinderResult.putResult(currentCriteria, c2);
      } else if (currentCriteria.getId() == 100L){
        // on error: to construct error response for test
        batchFinderResult.putError(currentCriteria, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND, "Fail to find Greeting!"));
      }
    }

    return batchFinderResult;
  }

  @Finder("searchWithMetadata")
  public CollectionResult<Greeting, SearchMetadata> searchWithMetadata()
  {
    return new CollectionResult<>(Collections.<Greeting>emptyList(),
        0,
        new SearchMetadata());
  }

  @Finder("searchWithByteStringArray")
  public CollectionResult<Greeting, SearchMetadata> searchWithByteStringArray(@QueryParam("byteStrings") ByteString[] byteStrings) {
    return new CollectionResult<>(Collections.<Greeting>emptyList(),
        0,
        new SearchMetadata());
  }
}

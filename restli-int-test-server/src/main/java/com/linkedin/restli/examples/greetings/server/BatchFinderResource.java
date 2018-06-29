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

import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.GreetingCriteria;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import java.util.Collections;


/**
 * This resource models a collection resource that exposes both a finder and a batch finder method.
 *
 * @author Maxime Lamure
 */
@RestLiCollection(name = "batchfinders", namespace = "com.linkedin.restli.examples.greetings.client")
public class BatchFinderResource extends CollectionResourceTemplate<Long, Greeting>
{

  @BatchFinder(value = "findUsers", batchParam = "criteria")
  public BatchFinderResult<GreetingCriteria, Greeting, EmptyRecord> findUsers(@PagingContextParam PagingContext context,
                                                                @QueryParam("criteria") GreetingCriteria[] criteria,
                                                                @QueryParam("first_name") String firstName)
  {
    return new BatchFinderResult<GreetingCriteria, Greeting, EmptyRecord>();
  }

  @Finder("searchWithMetadata")
  public CollectionResult<Greeting, SearchMetadata> searchWithMetadata()
  {
    return new CollectionResult<Greeting, SearchMetadata>(Collections.<Greeting>emptyList(),
                                                    0,
                                                          new SearchMetadata());
  }
}

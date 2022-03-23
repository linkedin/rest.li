/*
   Copyright (c) 2012 LinkedIn Corp.

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
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.SearchMetadata;
import com.linkedin.restli.server.BasicCollectionResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.NoMetadata;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.Collections;


/**
 * @author Keren Jin
 */
@RestLiCollection(name="finders", namespace = "com.linkedin.restli.examples.greetings.client")
public class FindersResource extends CollectionResourceTemplate<Long, Greeting> implements FinderInterface<Greeting, NoMetadata>
{
  @Override
  @Finder("searchWithoutMetadata")
  public CollectionResult<Greeting, NoMetadata> search()
  {
    return new CollectionResult<>(Collections.<Greeting>emptyList(), 0, null);
  }

  @Finder("searchWithMetadata")
  public CollectionResult<Greeting, SearchMetadata> searchWithMetadata()
  {
    return new CollectionResult<>(Collections.<Greeting>emptyList(),
        0,
        new SearchMetadata());
  }

  @Finder("basicSearch")
  public BasicCollectionResult<Greeting> basicSearch()
  {
    return new BasicCollectionResult<>(Collections.<Greeting>emptyList(), 0);
  }

  @Finder("predefinedSearch")
  public GreetingFinderResult predefinedSearch()
  {
    return new GreetingFinderResult(Collections.<Greeting>emptyList(), 0);
  }
}

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

package com.linkedin.restli.restspec;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.MockRecord;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.TestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import org.testng.annotations.Optional;

import java.util.List;


/**
 * @author Keren Jin
 *
 * @deprecated This is a deprecation documentation string for a resource.
 */
@SuppressWarnings({"deprecation", "dep-ann"})
@Deprecated
@RestLiCollection(name = "testDeprecationAnnotation", namespace = "com.linkedin.restli.restspec")
public class TestDeprecationAnnotationResource extends CollectionResourceTemplate<Long, MockRecord>
{
  @RestMethod.Get
  public GetResult<MockRecord> getWithResult(Long id,
                                             @QueryParam("extra") @UnnamedAnnotation(123) String extraParam)
  {
    return new GetResult<MockRecord>(null, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  }

  /**
   *
   * @deprecated Please use something else instead.
   */
  @SuppressWarnings({"deprecation", "dep-ann"})
  @Deprecated
  @Override
  public CreateResponse create(MockRecord empty)
  {
    return new CreateResponse(HttpStatus.S_200_OK);
  }

  /**
   * @deprecated Please use something else instead.
   */
  @SuppressWarnings({"deprecation", "dep-ann"})
  @Finder("testFinder")
  public List<MockRecord> testFinder(@Context PagingContext pagingContext,
                                     @QueryParam("title") @Optional String criterion)
  {
    return null;
  }

  @SuppressWarnings({"deprecation", "dep-ann"})
  @Deprecated
  @Action(name = "testAction", resourceLevel = ResourceLevel.COLLECTION)
  public int testAction(@ActionParam("num") int num)
  {
    return num;
  }
}

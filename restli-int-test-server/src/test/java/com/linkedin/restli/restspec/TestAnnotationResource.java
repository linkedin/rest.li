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
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.TestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import org.testng.annotations.Optional;

import java.util.List;


/**
 * @author Keren Jin
 */
@RestLiCollection(name = "testAnnotation", namespace = "com.linkedin.restli.restspec")
@NamedAnnotation(stringField = "class-level annotation")
public class TestAnnotationResource extends CollectionResourceTemplate<Long, MockRecord>
{
  @RestMethod.Get
  @NamedAnnotation(stringField = "resource method annotation",
                   byteField = 17,
                   floatField = 4.2F,
                   byteStringField = { 2, 7, 9 })
  public GetResult<MockRecord> getWithResult(Long id,
                                              @QueryParam("extra") @UnnamedAnnotation(123) String extraParam)
  {
    return new GetResult<MockRecord>(null, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  }

  @Override
  @NamedAnnotation(stringField = "resource method annotation 2",
                   longField = 21L,
                   intArrayField = {3, 2, 1})
  public CreateResponse create(MockRecord empty)
  {
    return new CreateResponse(HttpStatus.S_200_OK);
  }

  @Finder("testFinder")
  @NamedAnnotation(stringField = "finder annotation",
                   booleanField = false,
                   enumField = NamedAnnotation.AnnotationEnum.ENUM_MEMBER_2,
                   complexAnnotationArrayField = {@PartialExclusiveAnnotation(used1 = 111, used2 = 222),
                                                  @PartialExclusiveAnnotation(used1 = 333, used2 = 444)})
  @PartialExclusiveAnnotation(used1 = 11, unused = "this value is also ununsed")
  public List<MockRecord> testFinder(@PagingContextParam PagingContext pagingContext,
                                      @QueryParam("title") @Optional @NamedAnnotation(stringField = "finder parameter annotation") String criterion)
  {
    return null;
  }

  @TestMethod(doc = "For integration testing only.")
  @Action(name = "testAction", resourceLevel = ResourceLevel.COLLECTION)
  @NamedAnnotation(stringField = "action annotation",
                   classField = TestAnnotationResource.class,
                   simpleAnnotationArrayField = {@UnnamedAnnotation(7), @UnnamedAnnotation(27), @UnnamedAnnotation()},
                   normalAnnotationField = @NormalAnnotation(included = "included", excluded = "excluded"))
  public int testAction(@ActionParam("num") @UnnamedAnnotation(456) int num)
  {
    return num;
  }
}

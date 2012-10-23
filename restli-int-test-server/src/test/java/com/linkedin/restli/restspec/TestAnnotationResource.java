package com.linkedin.restli.restspec;


import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
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
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import org.testng.annotations.Optional;

import java.util.List;


/**
 * @author Keren Jin
 */
@NamedAnnotation(stringField = "class-level annotation")
@PartialInclusiveAnnotation(used = 1, unused = "this value is ununsed")
@EmptyAnnotation
@RestLiCollection(name = "testAnnotation", namespace = "com.linkedin.restli.restspec")
public class TestAnnotationResource extends CollectionResourceTemplate<Long, EmptyRecord>
{
  @RestMethod.Get
  @NamedAnnotation(stringField = "resource method annotation",
                   byteField = 17,
                   floatField = 4.2F,
                   byteStringField = { 2, 7, 9 })
  public GetResult<EmptyRecord> getWithResult(Long id,
                                              @QueryParam("extra") @UnnamedAnnotation(123) String extraParam)
  {
    return new GetResult<EmptyRecord>(null, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  }

  @Override
  @NamedAnnotation(stringField = "resource method annotation 2",
                   longField = 21L,
                   intArrayField = {3, 2, 1})
  public CreateResponse create(EmptyRecord empty)
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
  public List<EmptyRecord> testFinder(@Context PagingContext pagingContext,
                                      @QueryParam("title") @Optional @NamedAnnotation(stringField = "finder parameter annotation") String criterion)
  {
    return null;
  }

  @Action(name = "testAction", resourceLevel = ResourceLevel.COLLECTION)
  @NamedAnnotation(stringField = "action annotation",
                   classField = TestAnnotationResource.class,
                   simpleAnnotationArrayField = {@UnnamedAnnotation(7), @UnnamedAnnotation(27), @UnnamedAnnotation()})
  public int testAction(@ActionParam("num") @UnnamedAnnotation(456) int num)
  {
    return num;
  }
}

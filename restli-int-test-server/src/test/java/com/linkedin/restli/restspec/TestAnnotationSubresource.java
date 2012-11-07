package com.linkedin.restli.restspec;


import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;


/**
 * @author Keren Jin
 */
@RestLiCollection(name = "testAnnotationSub",
                  namespace = "com.linkedin.restli.restspec",
                  parent = TestAnnotationResource.class)
@EmptyAnnotation
public class TestAnnotationSubresource extends CollectionResourceTemplate<Long, EmptyRecord>
{
  @RestMethod.Delete
  @PartialInclusiveAnnotation(used = 1, unused = "this value is ununsed")
  public UpdateResponse delete(Long key)
  {
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }
}

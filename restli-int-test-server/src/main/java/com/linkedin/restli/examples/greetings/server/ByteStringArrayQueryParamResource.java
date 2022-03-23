package com.linkedin.restli.examples.greetings.server;

import com.linkedin.data.ByteString;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.BasicCollectionResult;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.annotations.RestLiCollection;
import java.util.Collections;


@RestLiCollection(name = "byteStringArray", namespace = "com.linkedin.restli.examples.greetings.client")
public class ByteStringArrayQueryParamResource implements KeyValueResource<Long, Greeting> {
  @Finder("searchWithByteStringArray")
  public BasicCollectionResult<Greeting> searchWithByteStringArray(@QueryParam("byteStrings") ByteString[] byteStrings) {
    return new BasicCollectionResult<Greeting>(Collections.<Greeting>emptyList());
  }
}
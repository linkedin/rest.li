package com.linkedin.r2.message.rest;

import com.linkedin.data.ByteString;
import java.util.Collections;


/**
 * Factory methods for {@link RestResponse}.
 */
public final class RestResponseFactory
{
  private static final RestResponse NO_RESPONSE = new RestResponseImpl(
      ByteString.empty(), Collections.<String, String>emptyMap(), Collections.<String>emptyList(), 0);

  /**
   * Returns an empty response.
   *
   * This is intended only for use in tests, hence the status code of 0.
   *
   * @return an instance of an empty response
   */
  public static RestResponse noResponse()
  {
    return NO_RESPONSE;
  }

  private RestResponseFactory()
  {
  }
}

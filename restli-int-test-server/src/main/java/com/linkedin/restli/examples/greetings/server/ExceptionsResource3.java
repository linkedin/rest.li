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


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

import java.util.Map;


@RestLiCollection(name = "exceptions3",
                  namespace = "com.linkedin.restli.examples.greetings.client")
public class ExceptionsResource3 extends CollectionResourceTemplate<Long, Greeting>
{
  public static final String TEST1_HEADER = "Test1-Header";
  public static final String TEST2_HEADER = "Test2-Header";
  public static final String TEST1_VALUE = "Test1-Value";
  public static final String TEST2_VALUE = "Test2-Value";

  @Override
  public Greeting get(Long key)
  {
    return null;
  }

  @Override
  public UpdateResponse update(final Long key, final Greeting entity)
  {
    return new UpdateResponse(HttpStatus.S_404_NOT_FOUND);
  }

  @Override
  public CreateResponse create(Greeting greeting)
  {
    Map<String, String> requestHeaders = getContext().getRequestHeaders();
    if (!requestHeaders.get(TEST1_HEADER).equals(TEST1_VALUE)
        || !requestHeaders.get(TEST2_HEADER).equals(TEST2_VALUE))
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Request headers are not modified from filter.");
    }
    return new CreateResponse(1234L);
  }
}

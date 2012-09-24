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

/**
 * $Id: $
 */

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

@RestLiCollection(name = "exceptions",
                  namespace = "com.linkedin.restli.examples.greetings.client")
public class ExceptionsResource extends CollectionResourceTemplate<Long, Greeting>
{
  @Override
  public Greeting get(Long key)
  {
    try
    {
      String s = (new String[0])[42];
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      Greeting details = new Greeting().setMessage("Hello, Sorry for the mess");

      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, "error processing request", e)
              .setServiceErrorCode(42).setErrorDetails(details.data());
    }
    return null;
  }
}

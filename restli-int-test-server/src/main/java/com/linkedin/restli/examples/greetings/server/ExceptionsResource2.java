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
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;


/**
 * @author Keren Jin
 */
@RestLiCollection(name = "exceptions2",
                  namespace = "com.linkedin.restli.examples.greetings.client")
public class ExceptionsResource2 extends CollectionResourceTemplate<Long, Greeting>
{
  @RestMethod.Get
  public GetResult<Greeting> getWithResult(Long key)
  {
    final Greeting value = new Greeting().setMessage("Hello, sorry for the mess");
    return new GetResult<Greeting>(value, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  }

  /**
   * Action that responds HTTP 500 with integer value
   */
  @Action(name = "exceptionWithValue")
  public ActionResult<Integer> exceptionWithValue()
  {
    return new ActionResult<Integer>(42, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  }

  /**
   * Action that responds HTTP 500 without value
   */
  @Action(name = "exceptionWithoutValue")
  public ActionResult<Void> exceptionWithoutValue()
  {
    return new ActionResult<Void>(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  }
}

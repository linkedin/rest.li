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

package com.linkedin.restli.server.twitter;


import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.GetResult;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;


/**
 * @author Keren Jin
 */
@RestLiCollection(name = "exceptions",
                  namespace = "com.linkedin.restli.examples.greetings.client")
public class ExceptionsResource extends CollectionResourceTemplate<Long, Status>
{
  @RestMethod.Get
  public GetResult<Status> getWithResult(Long key)
  {
    return new GetResult<>(new Status(), HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  }

  @Action(name = "exception")
  public ActionResult<Integer> actionWithResult()
  {
    return new ActionResult<>(100, HttpStatus.S_500_INTERNAL_SERVER_ERROR);
  }
}

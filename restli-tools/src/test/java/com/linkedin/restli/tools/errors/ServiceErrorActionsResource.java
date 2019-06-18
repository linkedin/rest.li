/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.restli.tools.errors;

import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.ServiceErrorDef;
import com.linkedin.restli.server.annotations.ServiceErrors;

import static com.linkedin.restli.tools.errors.DummyServiceError.Codes.*;

/**
 * Actions resource to test IDL generation with defined service errors.
 * This resource also tests that multiple resource-level service errors can be defined.
 *
 * @author Evan Williams
 */
@RestLiActions(name = "actions")
@ServiceErrorDef(DummyServiceError.class)
@ServiceErrors({RESOURCE_LEVEL_ERROR, YET_ANOTHER_RESOURCE_LEVEL_ERROR})
public class ServiceErrorActionsResource
{
  /**
   * Ensures that action methods can specify a method-level service error.
   */
  @Action(name = "doAction")
  @ServiceErrors(METHOD_LEVEL_ERROR)
  public int doAction()
  {
    return 2147;
  }

  /**
   * This is included as a finder method with no method-level service errors.
   */
  @Action(name = "iWillNeverFail")
  public int iWillNeverFail(@ActionParam("who") String who)
  {
    return 777;
  }

  /**
   * Ensures that service errors without error detail types can be used.
   */
  @Action(name = "missingErrorDetailType")
  @ServiceErrors(NO_DETAIL_TYPE_ERROR)
  public String missingErrorDetailType()
  {
    return "I have no idea what or where the error detail type is";
  }

  /**
   * Ensures that an empty list of service errors can be used at the method-level.
   */
  @Action(name = "noErrorsDefined")
  @ServiceErrors()
  public String noErrorsDefined()
  {
    return "Look at this empty error list";
  }
}

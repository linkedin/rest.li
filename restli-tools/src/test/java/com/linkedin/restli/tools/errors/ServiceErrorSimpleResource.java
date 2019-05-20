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

import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ServiceErrorDef;
import com.linkedin.restli.server.annotations.ServiceErrors;
import com.linkedin.restli.server.annotations.SuccessResponse;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyRecord;
import com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyServiceError;

import static com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyServiceError.Codes.*;

/**
 * Simple resource to test IDL generation with defined service errors.
 *
 * @author Evan Williams
 */
@RestLiSimpleResource(name = "simple")
@ServiceErrorDef(DummyServiceError.class)
@ServiceErrors(RESOURCE_LEVEL_ERROR)
public class ServiceErrorSimpleResource extends SimpleResourceTemplate<DummyRecord>
{
  /**
   * This ensures that annotation-specified CRUD methods can specify a method-level service error.
   */
  @RestMethod.Get
  @ServiceErrors(METHOD_LEVEL_ERROR)
  public DummyRecord get()
  {
    return null;
  }

  /**
   * This ensures that template CRUD methods can specify a method-level service error in conjunction with
   * success statuses. Also uses an error code with a unique error detail type.
   */
  @Override
  @ServiceErrors(ILLEGAL_ACTION)
  @SuccessResponse(statuses = { 204 })
  public UpdateResponse update(DummyRecord dummyRecord)
  {
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  /**
   * Ensures that action methods can specify a method-level service error.
   * Also ensures that service errors without messages can be used.
   */
  @Action(name = "doAction")
  @ServiceErrors({ METHOD_LEVEL_ERROR, NO_MESSAGE_ERROR })
  public int doAction()
  {
    return 2147;
  }
}

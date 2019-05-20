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

import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.ParamError;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.ServiceErrorDef;
import com.linkedin.restli.server.annotations.ServiceErrors;
import com.linkedin.restli.server.annotations.SuccessResponse;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyRecord;
import com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyServiceError;
import java.util.ArrayList;
import java.util.List;

import static com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyServiceError.Codes.*;

/**
 * Association resource to test IDL generation with defined service errors.
 * This resource also tests that an empty list of service errors can be defined.
 *
 * @author Evan Williams
 */
@RestLiAssociation(
    name = "association",
    assocKeys = {
        @Key(name = "keyA", type = Long.class),
        @Key(name = "keyB", type = Long.class)
    }
)
@ServiceErrorDef(DummyServiceError.class)
@ServiceErrors()
public class ServiceErrorAssociationResource extends AssociationResourceTemplate<DummyRecord>
{
  /**
   * Ensures that template CRUD methods can specify a method-level service error.
   */
  @Override
  @ServiceErrors(METHOD_LEVEL_ERROR)
  public List<DummyRecord> getAll(@PagingContextParam PagingContext pagingContext)
  {
    return new ArrayList<>();
  }

  /**
   * Ensures that a method-level service error can specify a parameter.
   */
  @Finder(value = "ctrlF")
  @ParamError(code = PARAMETER_ERROR, parameterNames = { "param" })
  public List<DummyRecord> finder(@QueryParam("param") String param)
  {
    return new ArrayList<>();
  }

  /**
   * Ensures that multiple success statuses can be specified.
   */
  @Action(name = "hasSuccessStatuses")
  @SuccessResponse(statuses = { 200, 201, 204 })
  public String hasSuccessStatuses()
  {
    return "I wish I were as successful as this method";
  }
}

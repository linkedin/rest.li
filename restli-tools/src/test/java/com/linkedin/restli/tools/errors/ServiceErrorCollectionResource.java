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
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.ParamError;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ServiceErrorDef;
import com.linkedin.restli.server.annotations.ServiceErrors;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyRecord;
import com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyServiceError;
import java.util.ArrayList;
import java.util.List;

import static com.linkedin.restli.tools.errors.ServiceErrorTestDataModels.DummyServiceError.Codes.*;


/**
 * Collection resource to test IDL generation with defined service errors.
 * This resource also tests that service errors can be defined only at the method level.
 *
 * @author Evan Williams
 */
@RestLiCollection(name = "collection")
@ServiceErrorDef(DummyServiceError.class)
public class ServiceErrorCollectionResource extends CollectionResourceTemplate<Long, DummyRecord>
{
  /**
   * This ensures that template CRUD methods can specify a method-level service error.
   */
  @Override
  @ServiceErrors(METHOD_LEVEL_ERROR)
  public DummyRecord get(Long id)
  {
    return null;
  }

  /**
   * This ensures that annotation-specified CRUD methods can specify method-level service errors.
   * It also ensures that multiple method-level service errors can be specified.
   */
  @RestMethod.Create
  @ServiceErrors({ METHOD_LEVEL_ERROR, YET_ANOTHER_METHOD_LEVEL_ERROR })
  public CreateResponse create(DummyRecord dummyRecord)
  {
    return new CreateResponse(2147L);
  }

  /**
   * This is included as a template CRUD method with no service errors.
   */
  @Override
  public UpdateResponse delete(Long id)
  {
    return new UpdateResponse(HttpStatus.S_204_NO_CONTENT);
  }

  /**
   * This is included as an annotation-specified CRUD method with no service errors.
   */
  @RestMethod.GetAll
  public List<DummyRecord> getAll()
  {
    return new ArrayList<>();
  }

  /**
   * This is included as an action method with no service errors.
   */
  @Action(name = "errorProneAction")
  public String errorProneAction()
  {
    return "Protect from errors: [on] off";
  }

  /**
   * This ensures that a method-level service error can specify one parameter.
   * It also ensures that a subset of parameters can be specified.
   */
  @Finder(value = "ctrlF")
  @ParamError(code = PARAMETER_ERROR, parameterNames = { "param" })
  public List<DummyRecord> finder(@QueryParam("param") String param, @QueryParam("ignoreMe") Integer ignoreMe)
  {
    return new ArrayList<>();
  }

  /**
   * This ensures that a method-level service error can specify multiple parameters.
   * It also ensures that service error parameter names are matched against the
   * {@link QueryParam} annotation rather than the actual method arguments.
   */
  @Finder(value = "altF4")
  @ParamError(code = DOUBLE_PARAMETER_ERROR, parameterNames = { "param1", "param2" })
  public List<DummyRecord> finder2(@QueryParam("param1") String akaParamA, @QueryParam("param2") String akaParamB)
  {
    return new ArrayList<>();
  }

  /**
   * This ensures that two method-level service errors specifying parameters can be used in conjunction
   * with a method-level service error with no parameters.
   */
  @Finder(value = "ctrlAltDelete")
  @ServiceErrors({ METHOD_LEVEL_ERROR })
  @ParamError(code = PARAMETER_ERROR, parameterNames = { "param" })
  @ParamError(code = DOUBLE_PARAMETER_ERROR, parameterNames = { "param1", "param2" })
  public List<DummyRecord> finder3(@QueryParam("param") String param, @QueryParam("param1") String param1,
      @QueryParam("param2") String param2)
  {
    return new ArrayList<>();
  }
}

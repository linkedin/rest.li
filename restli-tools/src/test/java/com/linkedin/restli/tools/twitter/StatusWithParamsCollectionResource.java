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

package com.linkedin.restli.tools.twitter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.tools.twitter.TwitterTestDataModels.Status;

/**
 * CollectionResource containing all statuses
 *
 * @author dellamag
 */
@RestLiCollection(name="statusesParams",
                    keyName="statusID")
public class StatusWithParamsCollectionResource implements KeyValueResource<Long,Status>
{
  /**
   * Creates a new Status
   *
   * @param locale query parameter has same name as method parameter
   * @param javaAuth query parameter has different name from method parameter
   */
  @RestMethod.Create
  public CreateResponse create(Status entity, @QueryParam("locale") @Optional("en_US") String locale,
      @QueryParam("auth") Long javaAuth)
  {
    return null;
  }

  /**
   * Gets a batch of statuses
   *
   * @param locale query parameter has same name as method parameter
   * @param javaAuth query parameter has different name from method parameter
   */
  @RestMethod.BatchGet
  public Map<Long, Status> batchGet(Set<Long> ids, @QueryParam("locale") @Optional("en_US") String locale,
      @QueryParam("auth") Long javaAuth)
  {
    return null;
  }

  /**
   * Gets a single status resource
   *
   * @param locale query parameter has same name as method parameter
   * @param javaAuth query parameter has different name from method parameter
   */
  @RestMethod.Get
  public Status get(Long key, @QueryParam("locale") @Optional("en_US") String locale,
      @QueryParam("auth") Long javaAuth)
  {
    return null;
  }

  /**
   * Deletes a status resource
   *
   * @param locale query parameter has same name as method parameter
   * @param javaAuth query parameter has different name from method parameter
   */
  @RestMethod.Delete
  public UpdateResponse delete(Long key, @QueryParam("locale") @Optional("en_US") String locale,
      @QueryParam("auth") Long javaAuth)
  {
    return null;
  }

  /**
   * Updates a single status resource
   *
   * @param locale query parameter has same name as method parameter
   * @param javaAuth query parameter has different name from method parameter
   */
  @RestMethod.PartialUpdate
  public UpdateResponse update(Long key, PatchRequest<Status> request,
      @QueryParam("locale") @Optional("en_US") String locale, @QueryParam("auth") Long javaAuth)
  {
    return null;
  }

  /**
   * @param locale query parameter has same name as method parameter
   * @param javaAuth query parameter has different name from method parameter
   */
  @RestMethod.GetAll
  public List<Status> getAll(@QueryParam("locale") @Optional("en_US") String locale,
      @QueryParam("auth") Long javaAuth)
  {
    return null;
  }

  /**
   * @param locale query parameter has same name as method parameter
   * @param javaAuth query parameter has different name from method parameter
   */
  @Finder("criteria")
  public List<Status> findByCriteria(@QueryParam("locale") @Optional("en_US") String locale,
      @QueryParam("auth") Long javaAuth)
  {
    return null;
  }

  /**
   * @param to action parameter has same name as method parameter
   * @param javaAuth action parameter has different name from method parameter
   */
  @Action(name="forward")
  public void forward(@ActionParam("to") long to, @ActionParam("auth") Long javaAuth)
  {
  }
}

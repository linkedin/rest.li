/*
   Copyright (c) 2014 LinkedIn Corp.

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


import com.linkedin.parseq.Task;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.KeyValueResource;


/**
 * Task based implementation of a CollectionResource containing all statuses
 */
@RestLiCollection(name="taskstatuses", keyName="statusID")
public class TaskStatusCollectionResource implements KeyValueResource<Long, TwitterTestDataModels.Status>
{
  /**
   * Gets a single status resource
   */
  @RestMethod.Get
  public Task<TwitterTestDataModels.Status> get(Long key)
  {
    throw new AssertionError("should be mocked");
  }
}

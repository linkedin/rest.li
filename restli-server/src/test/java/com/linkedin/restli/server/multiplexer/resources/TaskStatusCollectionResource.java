/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.server.multiplexer.resources;

import com.linkedin.data.DataMap;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.Tasks;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.multiplexer.resources.TestDataModels.User;
import com.linkedin.restli.server.resources.KeyValueResource;

import java.util.concurrent.Callable;


@RestLiCollection(name="users", keyName="userID")
public class TaskStatusCollectionResource implements KeyValueResource<Long, User>
{
  @RestMethod.Get
  public Task<User> get(Long key)
  {
    return Tasks.callable("get: " + key, new Callable<User>()
    {
      @Override
      public User call() throws Exception
      {
        return new User(new DataMap());
      }
    });
  }
}

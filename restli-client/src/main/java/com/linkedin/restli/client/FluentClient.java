/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * A common interface for client that implements FluentAPIs
 */
public interface FluentClient
{

  ThreadLocal<List<ExecutionGroup>> _executionGroup = new ThreadLocal<List<ExecutionGroup>>()
  {
    @Override
    public List<ExecutionGroup> initialValue()
    {
      return new LinkedList<>();
    }
  };

  default void setExecutionGroup(ExecutionGroup eg)
  {
    _executionGroup.get().add(eg);
  }

  default ExecutionGroup getExecutionGroupFromContext()
  {
    List<ExecutionGroup> groupList = _executionGroup.get();
    if (groupList.size() == 0)
    {
      return null;
    }
    return groupList.get(groupList.size() - 1);
  }

  default void removeExecutionGroup()
  {
    List<ExecutionGroup> groupList = _executionGroup.get();
    groupList.remove(groupList.size() - 1);
  }
}

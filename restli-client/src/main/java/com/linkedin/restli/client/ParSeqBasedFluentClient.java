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

import com.linkedin.parseq.Engine;
import java.util.LinkedList;
import java.util.List;


/**
 * A common interface for client that implements FluentAPIs
 *
 * Note currently the FluentClient is ParSeq based and the execution
 * of request is coupled with ParSeq {@link Engine} and {@link com.linkedin.parseq.Task}
 *
 */
public interface ParSeqBasedFluentClient
{

  ThreadLocal<List<ExecutionGroup>> _executionGroup = new ThreadLocal<List<ExecutionGroup>>()
  {
    @Override
    public List<ExecutionGroup> initialValue()
    {
      return new LinkedList<>();
    }
  };

  /**
   * Add the specified {@link ExecutionGroup} to the tail of the ThreadLocal list
   * @param eg the {@link ExecutionGroup} instance to add to the ThreadLocal List;
   */
  default void setExecutionGroup(ExecutionGroup eg)
  {
    _executionGroup.get().add(eg);
  }

  /**
   * Try to fetch an ExecutionGroup instance from the ThreadLocal context
   *
   * Since the ExecutionGroup can be stacked recursively, this method will get the one from the most recent layer
   * i.e. from the tail of the ThreadLocal list.
   *
   * @return the {@link ExecutionGroup} instance if there is one in the context; Otherwise return null
   */
  default ExecutionGroup getExecutionGroupFromContext()
  {
    List<ExecutionGroup> groupList = _executionGroup.get();
    if (groupList.size() == 0)
    {
      return null;
    }
    return groupList.get(groupList.size() - 1);
  }

  /**
   * Remove the most recent ExecutionGroup form the ThreadLocal list
   */
  default void removeExecutionGroup()
  {
    List<ExecutionGroup> groupList = _executionGroup.get();
    if(groupList.size() > 0)
    {
      groupList.remove(groupList.size() - 1);
    }
  }

  /**
   * Generate an {@link ExecutionGroup} instance
   *
   * @return an {@link ExecutionGroup} instance
   */
  default ExecutionGroup generateExecutionGroup()
  {
    return new ExecutionGroup(getEngine());
  }

  Engine getEngine();

  /**
   * This method will generate an {@link ExecutionGroup} instance and run its
   * {@link ExecutionGroup#batchOn(Runnable, ParSeqBasedFluentClient...)} method, with this fluentClient being the only {@link ParSeqBasedFluentClient}
   * in the argument.
   *
   * @param runnable the runnable that executes user's logic
   * @throws Exception the exceptions encountered when running the runnable
   */
  void runBatchOnClient(Runnable runnable) throws Exception;

}

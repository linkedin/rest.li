package com.linkedin.restli.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * A common interface for client that implements FluentAPIs
 */
interface FluentClient
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
    return groupList.get(groupList.size() - 1);
  }

  default void removeExecutionGroup()
  {
    List<ExecutionGroup> groupList = _executionGroup.get();
    groupList.remove(groupList.size() - 1);
  }
}

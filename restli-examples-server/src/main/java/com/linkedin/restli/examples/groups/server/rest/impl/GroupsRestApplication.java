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

package com.linkedin.restli.examples.groups.server.rest.impl;

import com.linkedin.restli.examples.groups.server.api.GroupMembershipMgr;
import com.linkedin.restli.examples.groups.server.api.GroupMgr;

/**
 * @author dellamag
 */
public class GroupsRestApplication
{
  private final GroupMgr _groupMgr;
  private final GroupMembershipMgr _membershipMgr;

  public GroupsRestApplication(GroupMgr groupMgr, GroupMembershipMgr membershipMgr)
  {
    super();
    _groupMgr = groupMgr;
    _membershipMgr = membershipMgr;
  }

  public GroupMgr getGroupMgr()
  {
    return _groupMgr;
  }

  public GroupMembershipMgr getMembershipMgr()
  {
    return _membershipMgr;
  }


  public static class Config
  {
    private GroupMembershipMgr _membershipMgr;
    private GroupMgr _groupMgr;

    public GroupMgr getGroupMgr()
    {
      return _groupMgr;
    }
    public void setGroupMgr(GroupMgr groupMgr)
    {
      _groupMgr = groupMgr;
    }
    public GroupMembershipMgr getMembershipMgr()
    {
      return _membershipMgr;
    }
    public void setMembershipMgr(GroupMembershipMgr membershipMgr)
    {
      _membershipMgr = membershipMgr;
    }
  }
}

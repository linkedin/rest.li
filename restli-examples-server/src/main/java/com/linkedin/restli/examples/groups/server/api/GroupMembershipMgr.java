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

package com.linkedin.restli.examples.groups.server.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.api.MembershipLevel;

/**
 * Interface to Groups backend (not RESTful, no JAX-RS dependencies)
 *
 * @author dellamag
 */
public interface GroupMembershipMgr
{
  public GroupMembership get(CompoundKey key);

  public Map<Integer, GroupMembership> batchGetByGroup(int groupID, Set<Integer> memberIDs);
  public Map<Integer, GroupMembership> batchGetByMember(int memberID, Set<Integer> groupIDs);

  public GroupMembership save(GroupMembership membership);

  public boolean delete(CompoundKey key);

  public List<GroupMembership> search(GroupMembershipSearchQuery query);

  public List<GroupMembership> getByMember(int memberID, MembershipLevel level, int start, int count);
}

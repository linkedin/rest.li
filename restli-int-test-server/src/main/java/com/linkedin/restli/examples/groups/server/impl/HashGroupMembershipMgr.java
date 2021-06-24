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

/**
 * $Id: $
 */

package com.linkedin.restli.examples.groups.server.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.api.MembershipLevel;
import com.linkedin.restli.examples.groups.server.api.GroupMembershipMgr;
import com.linkedin.restli.examples.groups.server.api.GroupMembershipSearchQuery;
import com.linkedin.restli.examples.groups.server.api.GroupsKeys;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class HashGroupMembershipMgr implements GroupMembershipMgr
{
  public static final String MEMBER_ID = GroupsKeys.MEMBER_ID;
  public static final String GROUP_ID = GroupsKeys.GROUP_ID;

  private final Map<CompoundKey, GroupMembership> _data;

  public HashGroupMembershipMgr()
  {
    _data = new HashMap<>();
  }

  @Override
  public GroupMembership get(CompoundKey key)
  {
    return _data.get(key);
  }

  @Override
  public Map<Integer, GroupMembership> batchGetByGroup(int groupID, Set<Integer> memberIDs)
  {
    Map<Integer, GroupMembership> result = new HashMap<>();
    for (Map.Entry<CompoundKey, GroupMembership> entry : _data.entrySet())
    {
      CompoundKey key = entry.getKey();
      if(key.getPartAsInt(GROUP_ID) == groupID &&
         memberIDs.contains(key.getPartAsInt(MEMBER_ID)))
      {
        result.put(key.getPartAsInt(MEMBER_ID), entry.getValue());
      }
    }
    return result;
  }

  @Override
  public Map<Integer, GroupMembership> batchGetByMember(int memberID, Set<Integer> groupIDs)
  {
    Map<Integer, GroupMembership> result = new HashMap<>();
    for (Map.Entry<CompoundKey, GroupMembership> entry : _data.entrySet())
    {
      CompoundKey key = entry.getKey();
      if(key.getPartAsInt(MEMBER_ID) == memberID &&
              groupIDs.contains(key.getPartAsInt(GROUP_ID)))
      {
        result.put(key.getPartAsInt(GROUP_ID), entry.getValue());
      }
    }
    return result;
  }

  private static CompoundKey buildKey(int groupID, int memberID)
  {
    return new CompoundKey().append(GROUP_ID, groupID).append(MEMBER_ID, memberID);
  }

  @Override
  public GroupMembership save(GroupMembership membership)
  {
    int groupID = membership.getGroupID();
    int memberID = membership.getMemberID();

    CompoundKey key = buildKey(groupID, memberID);
    membership.setId(URIParamUtils.encodeKeyForBody(key, true, AllProtocolVersions.BASELINE_PROTOCOL_VERSION));

    _data.put(key, membership);
    return membership;
  }

  @Override
  public boolean delete(CompoundKey key)
  {
    return _data.remove(key) != null;
  }

  @Override
  public List<GroupMembership> search(GroupMembershipSearchQuery query)
  {
    List<GroupMembership> result = new ArrayList<>();

    int counter = 0;
    for (Map.Entry<CompoundKey, GroupMembership> entry : _data.entrySet())
    {
      if (query.getStart() > counter)
      {
        counter++;
        continue;
      }
      if (query.getCount() > 0 && query.getStart() + query.getCount() < counter)
      {
        break;
      }

      GroupMembership value = entry.getValue();

      boolean match =
          (query.getGroupID() == GroupMembershipSearchQuery.WILDCARD || query.getGroupID() == value.getGroupID());
      if (query.getFirstName() != null)
      {
        match = match && query.getFirstName().equals(value.getFirstName());
      }
      if (query.getLastName() != null)
      {
        match = match && query.getLastName().equals(value.getLastName());
      }
      if (query.getMembershipLevel() != null)
      {
        match = match && query.getMembershipLevel().equals(value.getMembershipLevel());
      }

      if (match)
      {
        result.add(value);
      }
      counter++;
    }
    return result;
  }

  @Override
  public List<GroupMembership> getByMember(int memberID, MembershipLevel level, int start,
                                           int count)
  {
    List<GroupMembership> result = new ArrayList<>();
    int idx = 0;
    for (GroupMembership value : _data.values())
    {
      if (value.getMemberID().equals(memberID) &&
              value.getMembershipLevel().equals(level))
      {
        if (idx >= start && idx < start + count)
        {
          result.add(value);
        }
        ++idx;
      }
    }
    return result;
  }
}

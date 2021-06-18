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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.GroupContact;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.server.api.GroupMembershipMgr;
import com.linkedin.restli.examples.groups.server.api.GroupMgr;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class HashMapGroupMgr implements GroupMgr
{
  private final Map<Integer, Group> _data;
  private final AtomicInteger _sequence;
  private final GroupMembershipMgr _membershipMgr;

  public HashMapGroupMgr(GroupMembershipMgr membershipMgr)
  {
    _data = new HashMap<>();
    _sequence = new AtomicInteger();
    _membershipMgr = membershipMgr;
  }

  @Override
  public Map<Integer, Group> batchGet(Set<Integer> ids)
  {
    Map<Integer, Group> result = new HashMap<>();
    for (Integer id : ids)
    {
      Group g = _data.get(id);
      if (g != null)
      {
        result.put(id, g);
      }
    }
    return result;
  }

  @Override
  public Group create(Group group)
  {
    Integer id = _sequence.incrementAndGet();
    group.setId(id);
    _data.put(id, group);

    GroupMembership m = group.getOwner();
    m.setGroupID(id);
    _membershipMgr.save(m);

    return group;
  }

  @Override
  public boolean update(Integer groupID, Group group)
  {
    return _data.put(groupID, group) != null;
  }

  @Override
  public boolean delete(Integer groupID)
  {
    return _data.remove(groupID) != null;
  }

  @Override
  public Map<Integer, GroupContact> getGroupContacts(Set<Integer> ids)
  {
    return Collections.emptyMap();
  }

  @Override
  public List<Group> findByEmailDomain(String emailDomain, int start, int count)
  {
    List<Group> result = new ArrayList<>();

    int idx = 0;
    for (Group g : _data.values())
    {
      if (g.getPreApprovedEmailDomains().contains(emailDomain))
      {
        if (idx > start && idx < start + count)
        {
          result.add(g);
        }
        ++idx;
      }
    }
    return result;
  }

  @Override
  public List<Group> findByManager(int managerMemberID, int start, int count)
  {
    return Collections.emptyList();
  }

  @Override
  public List<Group> search(String keywords, String nameKeywords, Integer groupID, int start,
                            int count)
  {
    List<Group> result = new ArrayList<>();

    int idx = 0;
    for (Group g : _data.values())
    {
      if (g.getDescription().contains(keywords) && g.getName().contains(nameKeywords) &&
              (groupID == null || g.getId().equals(groupID)))
      {
        if (idx > start && idx < start + count)
        {
          result.add(g);
        }
        ++idx;
      }
    }
    return result;
  }
}

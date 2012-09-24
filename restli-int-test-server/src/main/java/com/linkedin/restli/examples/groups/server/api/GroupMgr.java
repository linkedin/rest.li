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

import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.GroupContact;

/**
 * Interface to Groups backend (not RESTful, no JAX-RS dependencies)
 *
 * @author dellamag
 */
public interface GroupMgr
{
  public Map<Integer, Group> batchGet(Set<Integer> ids);
  public Group create(Group group);
  public boolean update(Integer groupID, Group group);
  public boolean delete(Integer groupID);

  public Map<Integer, GroupContact> getGroupContacts(Set<Integer> ids);

  // FINDERS
  public List<Group> findByEmailDomain(String emailDomain, int start, int count);
  public List<Group> findByManager(int managerMemberID, int start, int count);

  public List<Group> search(String keywords, String nameKeywords, Integer groupID, int start, int count);
}

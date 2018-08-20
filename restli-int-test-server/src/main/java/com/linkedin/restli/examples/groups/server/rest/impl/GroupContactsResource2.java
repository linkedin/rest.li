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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.linkedin.restli.common.HttpStatus.*;

import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.groups.api.GroupContact;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;

/**
 * TODO Not implemented in MongoDB yet
 *
 * @author dellamag
 */
@RestLiCollection(parent=GroupsResource2.class,
                  name="contacts",
                  namespace = "com.linkedin.restli.examples.groups.client",
                  keyName="contactID")
public class GroupContactsResource2 extends CollectionResourceTemplate<Integer, GroupContact>

{
  @Override
  public CreateResponse create(GroupContact entity)
  {
    return new CreateResponse(S_201_CREATED);
  }

  @Override
  public Map<Integer, GroupContact> batchGet(Set<Integer> ids)
  {
    Map<Integer, GroupContact> map = new HashMap<>();
    for (int id : ids)
    {
      map.put(id, createContact(id));
    }
    return map;
  }

  protected static GroupContact createContact(Integer id)
  {
    return new GroupContact()
      .setContactID(id).setCreatedAt(System.currentTimeMillis()).setFirstName("Bob")
      .setGroupID(1).setIsInvited(true).setIsPreapproved(true).setLastName("Smith")
      .setUpdatedAt(System.currentTimeMillis());
  }

  @Override
  public GroupContact get(Integer id)
  {
    return createContact(id);
  }


  @Override
  public UpdateResponse update(Integer id, PatchRequest<GroupContact> request)
  {
    return new UpdateResponse(S_204_NO_CONTENT);
  }

  @Override
  public UpdateResponse delete(Integer id)
  {
    return new UpdateResponse(S_204_NO_CONTENT);
  }

  @Action(name="spamContacts", resourceLevel=ResourceLevel.COLLECTION)
  public void spamContacts()
  {
  }
}

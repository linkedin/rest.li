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


import com.google.common.collect.Sets;
import com.linkedin.data.ByteString;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.GroupMembershipParam;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.examples.groups.server.api.GroupMgr;
import com.linkedin.restli.examples.typeref.api.Fixed16;
import com.linkedin.restli.examples.typeref.api.Union;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO Derive path, resourceClass and keyName from class names (GroupsResource => /groups, GroupResource.class, "groupId")

 * @author dellamag
 */
@RestLiCollection(name = "groups",
                  namespace = "com.linkedin.restli.examples.groups.client",
                  keyName = "groupID")
public class GroupsResource2 extends CollectionResourceTemplate<Integer, Group>
{
  @Inject
  @Named("GroupsRestApplication")
  GroupsRestApplication _app;

  public GroupsResource2()
  {
    super();
  }

  public GroupMgr getGroupMgr()
  {
    return _app.getGroupMgr();
  }

  @Override
  public CreateResponse create(Group group)
  {
    Group createdGroup = getGroupMgr().create(group);

    return new CreateResponse(createdGroup.getId());
  }

  @Override
  public Map<Integer, Group> batchGet(Set<Integer> ids)
  {
    // TODO Need to eventually handle three id types: group_id, entity_id and vanity_name
    return getGroupMgr().batchGet(ids);
  }

  @Finder("emailDomain")
  public List<Group> findByEmailDomain(@Context PagingContext pagingContext,
                                       @QueryParam("emailDomain") String emailDomain)
  {
    return getGroupMgr().findByEmailDomain(emailDomain, pagingContext.getStart(), pagingContext.getCount());
  }

  @Finder("manager")
  public List<Group> findByManager(@Context PagingContext pagingContext,
                                   @QueryParam("managerMemberID") int memberID)
  {
    return getGroupMgr().findByManager(memberID, pagingContext.getStart(), pagingContext.getCount());
  }

  // TODO SearchResults response
  @Finder("search")
  public List<Group> search(@Context PagingContext pagingContext,
                            @QueryParam("keywords") @Optional String keywords,
                            @QueryParam("nameKeywords") @Optional String nameKeywords,
                            @QueryParam("groupID") @Optional Integer groupID)
  {
    return getGroupMgr().search(keywords, nameKeywords, groupID, pagingContext.getStart(), pagingContext.getCount());
  }

  /**
   * Test the default value for various types
   */
  @Finder("complexCircuit")
  public List<Group> complexCircuit(@Context PagingContext pagingContext,
                                    @QueryParam("nativeArray") @Optional("[1.1, 2.2, 3.3]") double[] nativeArray,
                                    @QueryParam("coercedArray") @Optional("[1.1, 2.2, 3.3]") int[] coercedArray,
                                    @QueryParam("wrappedArray") @Optional("[2, 3, 4]") IntegerArray wrappedArray,
                                    @QueryParam("wrappedMap") @Optional("{\"A\": 1, \"B\": 2}") IntegerMap wrappedMap,
                                    @QueryParam("bytes") @Optional("\u0007") ByteString bytes,
                                    @QueryParam("fixed") @Optional("\u0001\u0002\u0003\u0004" +
                                                                   "\u0005\u0006\u0007\u0008" +
                                                                   "\u0009\n\u000B\u000C" +
                                                                   "\r\u000E\u000F\u0010") Fixed16 fixed,
                                    @QueryParam("union") @Optional("{\"string\": \"I'm String\"}") Union union,
                                    @QueryParam("record") @Optional("{\"intParameter\": 7, \"stringParameter\": \"success\"}") GroupMembershipParam record,
                                    @QueryParam("records") @Optional("[{\"intParameter\": 7, \"stringParameter\": \"success\"}]") GroupMembershipParam[] records)
  {
    @SuppressWarnings("serial")
    final Map<String, Integer> testMap = new HashMap<String, Integer>() {{ put("A", 1); put("B", 2); }};
    if (!Arrays.equals(nativeArray, new double[] {1.1D, 2.2D, 3.3D}) ||
        !Arrays.equals(coercedArray, new int[] {1, 2, 3}) ||
        !wrappedArray.equals(Arrays.asList(2, 3, 4)) ||
        !wrappedMap.equals(testMap) ||
        !bytes.asAvroString().equals("\u0007") ||
        !fixed.bytes().asAvroString().equals("\u0001\u0002\u0003\u0004" +
                                             "\u0005\u0006\u0007\u0008" +
                                             "\u0009\n\u000B\u000C" +
                                             "\r\u000E\u000F\u0010") ||
        !"I'm String".equals(union.getString()) ||
        record.getIntParameter() != 7 || !record.getStringParameter().equals("success"))
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
    }

    return Collections.emptyList();
  }

  @Override
  public Group get(Integer id)
  {
    Group group = getGroupMgr().batchGet(Sets.newHashSet(id)).get(id);
    return group;

  }

  @Override
  public UpdateResponse update(Integer id, PatchRequest<Group> patch)
  {
    Group group = get(id);
    try
    {
      PatchApplier.applyPatch(group, patch);
    }
    catch (DataProcessingException e)
    {
      return new UpdateResponse(HttpStatus.S_400_BAD_REQUEST);
    }
    boolean wasUpdated = getGroupMgr().update(id, group);
    return new UpdateResponse(wasUpdated ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
  }

  @Override
  public UpdateResponse delete(Integer id)
  {
    boolean deleted = getGroupMgr().delete(id);
    return new UpdateResponse(deleted ? HttpStatus.S_204_NO_CONTENT : HttpStatus.S_404_NOT_FOUND);
  }

  // =====================
  // Action methods
  // =====================

  @Action(name="transferOwnership",
          resourceLevel=ResourceLevel.ENTITY)
  public void transferOwnership(@ActionParam("request") TransferOwnershipRequest request)
  {
  }

  @Action(name="sendTestAnnouncement",
          resourceLevel= ResourceLevel.ENTITY)
  public void sendTestAnnouncement(@ActionParam("subject") String subject,
                                   @ActionParam("message") String message,
                                   @ActionParam("emailAddress") String emailAddress)
  {
  }
}

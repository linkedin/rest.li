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


import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.api.MembershipLevel;
import com.linkedin.restli.examples.groups.api.MembershipSortOrder;
import com.linkedin.restli.examples.groups.server.api.GroupMembershipSearchQuery;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.AssocKey;
import com.linkedin.restli.server.annotations.Context;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.resources.AssociationResource;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import static com.linkedin.restli.common.HttpStatus.S_204_NO_CONTENT;
import static com.linkedin.restli.common.HttpStatus.S_400_BAD_REQUEST;
import static com.linkedin.restli.common.HttpStatus.S_404_NOT_FOUND;
import static com.linkedin.restli.examples.groups.server.api.GroupsKeys.GROUP_ID;
import static com.linkedin.restli.examples.groups.server.api.GroupsKeys.MEMBER_ID;

/**
 * Association between members and groups
 *
 * @author dellamag
 */
@RestLiAssociation(name="groupMemberships",
                   namespace = "com.linkedin.restli.examples.groups.client",
                   assocKeys={@Key(name="groupID", type=int.class),
                              @Key(name="memberID", type=int.class)})
public class GroupMembershipsResource2 extends AssociationResourceTemplate<GroupMembership>
{
  @Inject
  @Named("GroupsRestApplication")
  GroupsRestApplication _app;

  /**
  * @see GroupMembershipsResource2#batchGet(Set)
  */
  @Override
  public BatchResult<CompoundKey, GroupMembership> batchGet(Set<CompoundKey> ids)
  {
    Map<CompoundKey, GroupMembership> result = new HashMap<CompoundKey, GroupMembership>(ids.size());
    Map<CompoundKey, RestLiServiceException> errors = new HashMap<CompoundKey, RestLiServiceException>();
    Iterator<CompoundKey> iterator = ids.iterator();
    while (iterator.hasNext()) {
      CompoundKey key = iterator.next();
      GroupMembership membership = _app.getMembershipMgr().get(key);
      if (membership != null)
      {
        result.put(key, membership);
      }
      else
      {
        errors.put(key, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
      }
    }
    return new BatchResult<CompoundKey, GroupMembership>(result, errors);
  }

  // TODO Better search interface (needs parameter binding to Query object, results object w/total)
  @Finder("group")
  public List<GroupMembership> getMemberships(@Context PagingContext pagingContext,
                                              @AssocKey("groupID") int groupID,
                                              // TODO Bind friendly parameter values to enum values
                                              @QueryParam("level") @Optional String level,
                                              @QueryParam("firstName") @Optional String firstName,
                                              @QueryParam("lastName") @Optional String lastName,
                                              @QueryParam("email") @Optional String email,
                                              @QueryParam("sort") @Optional MembershipSortOrder sortOrder)
  {
    GroupMembershipSearchQuery query = new GroupMembershipSearchQuery(groupID,
                                                                      pagingContext.getStart(),
                                                                      pagingContext.getCount());
    if (firstName != null) query.setFirstName(firstName);
    if (lastName != null) query.setLastName(lastName);
    if (email != null) query.setEmailAddress(email);
    if (sortOrder != null) query.setSortOrder(sortOrder);
    if (level != null) query.setMembershipLevel(MembershipLevel.valueOf(level));

    return _app.getMembershipMgr().search(query);
  }

  @Finder("member")
  public List<GroupMembership> getMemberships(@Context PagingContext pagingContext,
                                              @AssocKey("memberID") int memberID)
  {
    return _app.getMembershipMgr().getByMember(memberID,
                                              MembershipLevel.MEMBER,
                                              pagingContext.getStart(),
                                              pagingContext.getCount());
  }

  /**
     * @see AssociationResource#get
     */
  @Override
  public GroupMembership get(CompoundKey id)
  {
    return _app.getMembershipMgr().get(id);
  }

  /**
     * @see AssociationResource#delete
     */
  @Override
  public UpdateResponse delete(CompoundKey id)
  {
    boolean deleted = _app.getMembershipMgr().delete(id);
    return new UpdateResponse(deleted ? S_204_NO_CONTENT : S_404_NOT_FOUND);
  }

  /** @see com.linkedin.restli.server.resources.AssociationResourceTemplate#batchUpdate(com.linkedin.restli.server.BatchUpdateRequest) */
  @Override
  public BatchUpdateResult<CompoundKey, GroupMembership> batchUpdate(BatchUpdateRequest<CompoundKey, GroupMembership> entities)
  {
    Map<CompoundKey, UpdateResponse> results = new HashMap<CompoundKey, UpdateResponse>();
    for (Map.Entry<CompoundKey, GroupMembership> entry : entities.getData().entrySet())
    {
      CompoundKey id = entry.getKey();
      GroupMembership membership = entry.getValue();

      membership.setId(URIParamUtils.encodeKeyForBody(id, true, AllProtocolVersions.BASELINE_PROTOCOL_VERSION));
      membership.setGroupID(((Integer)id.getPart(GROUP_ID)));
      membership.setMemberID(((Integer)id.getPart(MEMBER_ID)));
      _app.getMembershipMgr().save(membership);
      results.put(id, new UpdateResponse(S_204_NO_CONTENT));
    }
    return new BatchUpdateResult<CompoundKey, GroupMembership>(results);
  }

  @Override
  public BatchUpdateResult<CompoundKey, GroupMembership> batchUpdate(BatchPatchRequest<CompoundKey, GroupMembership> patches)
  {
    Map<CompoundKey, UpdateResponse> results = new HashMap<CompoundKey, UpdateResponse>();
    for (Map.Entry<CompoundKey, PatchRequest<GroupMembership>> entry: patches.getData().entrySet())
    {
      CompoundKey key = entry.getKey();
      PatchRequest<GroupMembership> patch = entry.getValue();

      GroupMembership groupMembership = _app.getMembershipMgr().get(key);

      if (groupMembership == null)
      {
        results.put(key, new UpdateResponse(HttpStatus.S_404_NOT_FOUND));
      }
      else
      {
        try
        {
          PatchApplier.applyPatch(groupMembership, patch);
          _app.getMembershipMgr().save(groupMembership);
          results.put(key, new UpdateResponse(HttpStatus.S_204_NO_CONTENT));
        }
        catch (DataProcessingException e)
        {
          results.put(key, new UpdateResponse(HttpStatus.S_400_BAD_REQUEST));
        }
      }
    }
    return new BatchUpdateResult<CompoundKey, GroupMembership>(results);
  }

  /** @see com.linkedin.restli.server.resources.AssociationResourceTemplate#batchDelete(com.linkedin.restli.server.BatchDeleteRequest) */
  @Override
  public BatchUpdateResult<CompoundKey, GroupMembership> batchDelete(BatchDeleteRequest<CompoundKey, GroupMembership> ids)
  {
    Map<CompoundKey, UpdateResponse> results = new HashMap<CompoundKey, UpdateResponse>();
    for (CompoundKey key: ids.getKeys())
    {
      results.put(key, delete(key));
    }
    return new BatchUpdateResult<CompoundKey, GroupMembership>(results);
  }

  /**
     * @see AssociationResource#update
     */
  @Override
  public UpdateResponse update(CompoundKey id, PatchRequest<GroupMembership> patch)
  {
    GroupMembership membership = _app.getMembershipMgr().get(id);
    try
    {
      PatchApplier.applyPatch(membership, patch);
    }
    catch (DataProcessingException e)
    {
      return new UpdateResponse(S_400_BAD_REQUEST);
    }

    validate(membership);

    // we set groupID, memberID based on the URI
    membership.setId(URIParamUtils.encodeKeyForBody(id, true, AllProtocolVersions.BASELINE_PROTOCOL_VERSION));
    membership.setGroupID(getContext().getPathKeys().getAsInt(GROUP_ID));
    membership.setMemberID(getContext().getPathKeys().getAsInt(MEMBER_ID));

    _app.getMembershipMgr().save(membership);

    return new UpdateResponse(S_204_NO_CONTENT);
  }


  /**
   * @see com.linkedin.restli.server.resources.AssociationResourceTemplate#update(com.linkedin.restli.common.CompoundKey, com.linkedin.data.template.RecordTemplate)
   */
  @Override
  public UpdateResponse update(CompoundKey key, GroupMembership membership) {
    validate(membership);

    // we set groupID, memberID based on the URI
    membership.setId(URIParamUtils.encodeKeyForBody(key, true, AllProtocolVersions.BASELINE_PROTOCOL_VERSION));
    membership.setGroupID(getContext().getPathKeys().getAsInt(GROUP_ID));
    membership.setMemberID(getContext().getPathKeys().getAsInt(MEMBER_ID));

    _app.getMembershipMgr().save(membership);

    return new UpdateResponse(S_204_NO_CONTENT);
  }

  private void validate(GroupMembership membership)
  {
    if (membership.hasGroupID() || membership.hasMemberID())
    {
      throw new RestLiServiceException(S_400_BAD_REQUEST,
                                  "groupID and memberID fields cannot be set while saving/updating a GroupMembership. " +
                                  "They are derived from the resource identifier");
    }
  }

  @Override
  public List<GroupMembership> getAll(@Context PagingContext pagingContext)
  {
    return _app.getMembershipMgr()
               .search(new GroupMembershipSearchQuery(GroupMembershipSearchQuery.WILDCARD,
                                                      pagingContext.getStart(),
                                                      pagingContext.getCount()));
  }
}

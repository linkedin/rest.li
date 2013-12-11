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

import static com.linkedin.restli.common.HttpStatus.S_204_NO_CONTENT;
import static com.linkedin.restli.common.HttpStatus.S_400_BAD_REQUEST;
import static com.linkedin.restli.common.HttpStatus.S_404_NOT_FOUND;
import static com.linkedin.restli.examples.groups.server.api.GroupsKeys.GROUP_ID;
import static com.linkedin.restli.examples.groups.server.api.GroupsKeys.MEMBER_ID;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.linkedin.data.template.GetMode;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.examples.groups.api.ComplexKeyGroupMembership;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.api.GroupMembershipKey;
import com.linkedin.restli.examples.groups.api.GroupMembershipParam;
import com.linkedin.restli.examples.groups.api.GroupMembershipQueryParamArray;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchResult;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.AssociationResource;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;
import com.linkedin.restli.server.util.PatchApplier;

@RestLiCollection(name="groupMembershipsComplex",
namespace = "com.linkedin.restli.examples.groups.client")
public class GroupMembershipsResource3 extends ComplexKeyResourceTemplate<GroupMembershipKey, GroupMembershipParam, ComplexKeyGroupMembership>
{
  @Inject
  @Named("GroupsRestApplication")
  GroupsRestApplication _app;

  /** @see com.linkedin.restli.server.resources.ComplexKeyResourceTemplate#create(com.linkedin.data.template.RecordTemplate) */
  @Override
  public CreateResponse create(ComplexKeyGroupMembership groupMembership)
  {
    // For create construct a key based on the memberID and groupID in the membership
    // object
    if (!groupMembership.getId().hasMemberID() || !groupMembership.getId().hasGroupID())
    {
      throw new RestLiServiceException(S_400_BAD_REQUEST,
                                       "groupID and memberID fields must be set while creating a ComplexKeyGroupMembership.");
    }
    GroupMembershipKey groupMembershipKey = new GroupMembershipKey();
    groupMembershipKey.setMemberID(groupMembership.getId().getMemberID());
    groupMembershipKey.setGroupID(groupMembership.getId().getGroupID());
    ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> complexResourceKey =
        new ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>(groupMembershipKey,
                                                                         new GroupMembershipParam());
    groupMembership.setId(complexResourceKey.getKey());
    _app.getMembershipMgr().save(toGroupMembership(groupMembership));
    return new CreateResponse(complexResourceKey, HttpStatus.S_201_CREATED);
  }

  /** @see com.linkedin.restli.server.resources.ComplexKeyResourceTemplate#batchUpdate(com.linkedin.restli.server.BatchUpdateRequest) */
  @Override
  public BatchUpdateResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> batchUpdate(BatchUpdateRequest<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> entities)
  {
    Map<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, UpdateResponse> results =
        new HashMap<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, UpdateResponse>();
    for (Map.Entry<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> entry : entities.getData()
                                                                                                                            .entrySet())
    {
      results.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership>(results);
  }

  /** @see com.linkedin.restli.server.resources.ComplexKeyResourceTemplate#batchUpdate(com.linkedin.restli.server.BatchPatchRequest) */
  @Override
  public BatchUpdateResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> batchUpdate(BatchPatchRequest<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> patches)
  {
    Map<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, UpdateResponse> results =
        new HashMap<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, UpdateResponse>();
    for (Map.Entry<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, PatchRequest<ComplexKeyGroupMembership>> entry : patches.getData()
                                                                                                                               .entrySet())
    {
      results.put(entry.getKey(), update(entry.getKey(), entry.getValue()));
    }
    return new BatchUpdateResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership>(results);
  }

  /** @see com.linkedin.restli.server.resources.ComplexKeyResourceTemplate#batchCreate(com.linkedin.restli.server.BatchCreateRequest) */
  @Override
  public BatchCreateResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> batchCreate(BatchCreateRequest<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> groupMemberships)
  {
    List<CreateResponse> list = new LinkedList<CreateResponse>();
    for (ComplexKeyGroupMembership groupMembership : groupMemberships.getInput())
    {
      list.add(create(groupMembership));
    }
    return new BatchCreateResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership>(list);
  }

  /** @see com.linkedin.restli.server.resources.ComplexKeyResourceTemplate#batchDelete(com.linkedin.restli.server.BatchDeleteRequest) */
  @Override
  public BatchUpdateResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> batchDelete(BatchDeleteRequest<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> ids)
  {
    Map<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, UpdateResponse> results =
        new HashMap<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, UpdateResponse>();
    for (ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> key : ids.getKeys())
    {
      results.put(key, delete(key));
    }
    return new BatchUpdateResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership>(results);
  }

  /**
  * @see GroupMembershipsResource2#batchGet(Set)
  */
  @Override
  public BatchResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> batchGet(Set<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>> ids)
  {
    Map<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> result =
        new HashMap<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership>(ids.size());
    Map<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, RestLiServiceException> errors =
        new HashMap<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, RestLiServiceException>();
    Iterator<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>> iterator =
        ids.iterator();
    while (iterator.hasNext())
    {
      ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> key = iterator.next();
      ComplexKeyGroupMembership membership =
          fromGroupMembership(_app.getMembershipMgr().get(complexKeyToCompoundKey(key)));
      if (membership != null)
      {
        result.put(key, membership);
      }
      else
      {
        errors.put(key, new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
      }
    }
    return new BatchResult<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership>(result,
                                                                                                          errors);
  }
  /**
     * @see AssociationResource#get
     */
  @RestMethod.Get
  public ComplexKeyGroupMembership get(ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> id,
                                       @Optional @QueryParam("testParam") GroupMembershipParam param,
                                       @Optional @QueryParam("testParamArray") GroupMembershipQueryParamArray paramArray)
  {
    // For the purpose of test, if param is present, must contain both int and string
    // parts and the int param string value must equal the string parameter value.
    if (param != null)
    {
      String stringParam = param.getStringParameter();
      Integer intParam = param.getIntParameter();
      if (stringParam == null || intParam == null || !stringParam.equals(intParam.toString()))
      {
        throw new RestLiServiceException(S_400_BAD_REQUEST, "The values of testParam parameter don't match");
      }
    }
    return fromGroupMembership(_app.getMembershipMgr().get(complexKeyToCompoundKey(id)));
  }

  /**
     * @see AssociationResource#delete
     */
  @Override
  public UpdateResponse delete(ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> id)
  {
    boolean deleted = _app.getMembershipMgr().delete(complexKeyToCompoundKey(id));
    return new UpdateResponse(deleted ? S_204_NO_CONTENT : S_404_NOT_FOUND);
  }

  /**
     * @see AssociationResource#update
     */
  @Override
  public UpdateResponse update(ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> id, PatchRequest<ComplexKeyGroupMembership> patch)
  {
    
    ComplexKeyGroupMembership membership =
        fromGroupMembership(_app.getMembershipMgr().get(complexKeyToCompoundKey(id)));
    try
    {
      PatchApplier.applyPatch(membership, patch);
    }
    catch (DataProcessingException e)
    {
      return new UpdateResponse(S_400_BAD_REQUEST);
    }

    //validate(membership);

    // we set groupID, memberID based on the URI
    membership.setId(id.getKey());

    _app.getMembershipMgr().save(toGroupMembership(membership));

    return new UpdateResponse(S_204_NO_CONTENT);
  }


  /**
   * @see com.linkedin.restli.server.resources.AssociationResourceTemplate#update(com.linkedin.restli.common.CompoundKey, com.linkedin.data.template.RecordTemplate)
   */
  @Override
  public UpdateResponse update(ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> key, ComplexKeyGroupMembership membership) {
    membership.setId(key.getKey());

    _app.getMembershipMgr().save(toGroupMembership(membership));

    return new UpdateResponse(S_204_NO_CONTENT);
  }

  private static CompoundKey complexKeyToCompoundKey(ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> id) {
    GroupMembershipKey key = id.getKey();
    CompoundKey compoundKey = new CompoundKey();
    compoundKey.append(GROUP_ID, key.getGroupID(GetMode.NULL));
    compoundKey.append(MEMBER_ID, key.getMemberID(GetMode.NULL));
    return compoundKey;
  }
  
  // This is a hack for the sample resource. So as not to write a separate persistence for this resource,
  // convert from and to GroupMembership.
  private static GroupMembership toGroupMembership(ComplexKeyGroupMembership complexKeyMembership)
  {
    GroupMembership groupMembership = new GroupMembership(complexKeyMembership.data());
    GroupMembershipKey complexKey = complexKeyMembership.getId();
    CompoundKey compoundKey =
        new CompoundKey().append(GROUP_ID, complexKey.getGroupID())
                         .append(MEMBER_ID, complexKey.getMemberID());
    groupMembership.setId(URIParamUtils.encodeKeyForBody(compoundKey,
                                                         true,
                                                         AllProtocolVersions.BASELINE_PROTOCOL_VERSION));
    groupMembership.setMemberID(complexKey.getMemberID());
    groupMembership.setGroupID(complexKey.getGroupID());
    return groupMembership;
  }
  
  private static ComplexKeyGroupMembership fromGroupMembership(GroupMembership groupMembership)
  {
    if (groupMembership == null)
    {
      return null;
    }
    ComplexKeyGroupMembership complexKeyGroupMembership = new ComplexKeyGroupMembership(groupMembership.data());
    GroupMembershipKey groupMembershipKey = new GroupMembershipKey();
    groupMembershipKey.setGroupID(groupMembership.getGroupID());
    groupMembershipKey.setMemberID(groupMembership.getMemberID());
    complexKeyGroupMembership.setId(groupMembershipKey);
    return complexKeyGroupMembership;
  }
}

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

package com.linkedin.restli.examples.groups;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.BatchGetRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.GroupContact;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.api.MembershipSortOrder;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.examples.groups.client.ContactsBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsFindByGroupBuilder;
import com.linkedin.restli.examples.groups.client.GroupMembershipsFindByMemberBuilder;
import com.linkedin.restli.examples.groups.client.GroupsBatchGetBuilder;
import com.linkedin.restli.examples.groups.client.GroupsBuilders;
import com.linkedin.restli.examples.groups.client.GroupsFindByEmailDomainBuilder;
import com.linkedin.restli.examples.groups.client.GroupsFindByManagerBuilder;
import com.linkedin.restli.examples.groups.client.GroupsFindBySearchBuilder;
import com.linkedin.restli.internal.client.ActionResponseDecoder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;
import com.linkedin.restli.internal.client.EmptyResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;

/**
 * Groups REST request builder unit test.
 *
 * @author Eran Leshem
 */

public class TestGroupsRequestBuilders
{
  private static final GroupsBuilders GROUPS_BUILDERS = new GroupsBuilders();
  private static final ContactsBuilders CONTACTS_BUILDERS = new ContactsBuilders();
  private static final GroupMembershipsBuilders GROUP_MEMBERSHIPS_BUILDERS = new GroupMembershipsBuilders();

  @Test
  public void testEntityGet() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1

    String expectedUri = "groups/1";

    Request<Group> request = GROUPS_BUILDERS.get().id(1).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Group.class, expectedUri, null);

  }

  @Test
  public void testEntityGetWithFields() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1?fields=badge

    String expectedUri = "groups/1?fields=badge";

    Request<Group> request = GROUPS_BUILDERS.get().id(1).fields(Group.fields().badge()).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test
  public void testEntityCreate() throws IOException, RestException
  {
    // curl -v -X POST -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d @group.json http://localhost:1338/groups/

    String expectedUri = "groups";

    Request<EmptyRecord>  request = GROUPS_BUILDERS.create().input(new Group()).build();

    checkRequestBuilder(request, ResourceMethod.CREATE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, new Group());
  }

  @Test
  public void testEntityUpdate() throws IOException, RestException
  {
    //Example: curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d "{\"name\":\"New Name\"}" http://localhost:1338/groups/1
    //This test example: curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d "{}" http://localhost:1338/groups/1

    String expectedUri = "groups/1";

    Request<EmptyRecord>  request = GROUPS_BUILDERS.partialUpdate().id(1).input(new PatchRequest<Group>()).build();

    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, new Group());
  }

  @Test
  public void testEntityDelete() throws IOException, RestException
  {
    //curl -v -X DELETE -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" http://localhost:1338/groups/1

    String expectedUri = "groups/1";

    Request<EmptyRecord>  request = GROUPS_BUILDERS.delete().id(1).build();

    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, null);
  }

  @Test
  public void testCollectionFinderByEmailDomainWithFields() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0

    String expectedUri = "groups?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0";

    // Find by email domain with some debug, pagination and projection
    GroupsFindByEmailDomainBuilder findRequestBuilder =
       GROUPS_BUILDERS
      .findByEmailDomain()
      .emailDomainParam("foo.com")
      .paginate(0, 10)
      .fields(Group.fields().state(), Group.fields().locale());

    Request<CollectionResponse<Group>> request = findRequestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test
  public void testCollectionFinderByManagerId() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?managerMemberID=1&q=manager

    String expectedUri = "groups?managerMemberID=1&q=manager";

    // Find by email domain with some debug, pagination and projection
    GroupsFindByManagerBuilder findRequestBuilder =
       GROUPS_BUILDERS.findByManager().managerMemberIdParam(1);
    Request<CollectionResponse<Group>> request = findRequestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test
  public void testCollectionFinderBySearch_AllValues() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?groupID=1&keywords=linkedin&nameKeywords=test&q=search

    String expectedUri = "groups?groupID=1&keywords=linkedin&nameKeywords=test&q=search";

    GroupsFindBySearchBuilder findRequestBuilder =
        GROUPS_BUILDERS.findBySearch()
                       .keywordsParam("linkedin")
                       .nameKeywordsParam("test")
                       .groupIdParam(1);
    Request<CollectionResponse<Group>> request = findRequestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test
  public void testCollectionFinderBySearchWithOptionalParamsTest1() throws IOException, RestException, URISyntaxException
  {
    // curl -v -X GET http://localhost:1338/groups/?keywords=linkedin&q=search

    String expectedUri = "groups?keywords=linkedin&q=search";

    GroupsFindBySearchBuilder findRequestBuilder =
        GROUPS_BUILDERS.findBySearch().keywordsParam("linkedin");
    Request<CollectionResponse<Group>> request = findRequestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test
  public void testCollectionFinderBySearchWithOptionalParamsTest2() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?keywords=linkedin&nameKeywords=test&q=search

    String expectedUri = "groups?keywords=linkedin&nameKeywords=test&q=search";

    GroupsFindBySearchBuilder findRequestBuilder =
        GROUPS_BUILDERS.findBySearch()
                       .keywordsParam("linkedin")
                       .nameKeywordsParam("test");
    Request<CollectionResponse<Group>> request = findRequestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test
  public void testCollectionFinderBySearchWithOptionalParamsTest3() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?groupID=1&q=search

    String expectedUri = "groups?groupID=1&q=search";

    GroupsFindBySearchBuilder findRequestBuilder = GROUPS_BUILDERS.findBySearch().groupIdParam(1);
    Request<CollectionResponse<Group>> request = findRequestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test
  public void testCollectionFinderOmittingRequiredParams() throws IOException, RestException
  {
    try
    {
      GROUPS_BUILDERS.findByManager().managerMemberIdParam(null);
      fail("should have thrown IllegalArgumentException");
    }
    catch (NullPointerException e)
    {
      //expected
    }
  }

  @Test
  public void testCollectionEntityOmittingRequiredIdParam() throws IOException, RestException
  {
    try
    {
      CONTACTS_BUILDERS.get().id(1).build();
      fail("should have thrown IllegalStateException");
    }
    catch (IllegalStateException e)
    {
      //expected
    }
  }

  @Test
  public void testCollectionEntityNullIdParam() throws IOException, RestException
  {
    try
    {
      CONTACTS_BUILDERS.get().groupIdKey(null).id(1).build();
      fail("should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException e)
    {
      //expected
    }
  }

  @Test
  public void testBatchGet() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?fields=approvalModes&ids=1&ids=3

    String expectedUri = "groups?fields=approvalModes&ids=1&ids=3";

    GroupsBatchGetBuilder batchRequestBuilder =
            GROUPS_BUILDERS.batchGet().ids(1, 3).fields(Group.fields().approvalModes());
    Request<BatchResponse<Group>> request = batchRequestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test
  public void testBatchGetWithSelectedNullValues() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?fields=approvalModes&ids=1&ids&ids=3
    try
    {
      GROUPS_BUILDERS.batchGet().ids(1, null, 3).fields(Group.fields().approvalModes());
      fail("should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException e)
    {
      //expected
    }
  }

  @Test
  public void testSubResourceGet() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/1

    String expectedUri = "groups/1/contacts/1";

    Request<GroupContact> request = CONTACTS_BUILDERS.get().groupIdKey(1).id(1).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, GroupContact.class, expectedUri, null);
  }

  @Test
  public void testSubResourceGetParamReordering() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/1

    String expectedUri = "groups/1/contacts/1";

    Request<GroupContact> request = CONTACTS_BUILDERS.get().id(1).groupIdKey(1).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, GroupContact.class, expectedUri, null);
  }


  @Test
  public void testSubResourceGetWithFields() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/1?fields=firstName,lastName

    String expectedUri = "groups/1/contacts/1?fields=lastName,firstName";

    Request<GroupContact> request = CONTACTS_BUILDERS.get()
            .groupIdKey(1)
            .id(1)
            .fields(GroupContact.fields().firstName(), GroupContact.fields().lastName())
            .build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, GroupContact.class, expectedUri, null);
  }

  @Test
  public void testSubResourceBatchGet() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/?ids=1&ids=3

    String expectedUri = "groups/1/contacts?ids=1&ids=3";

    BatchGetRequest<GroupContact> request = CONTACTS_BUILDERS.batchGet().groupIdKey(1).ids(1,3).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, GroupContact.class, expectedUri, null);
  }

  @Test
  public void testSubResourceBatchGetWithSelectedNullValues() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/?ids=1&ids&ids=3
    try
    {
      CONTACTS_BUILDERS.batchGet().groupIdKey(1).ids(1, null, 3).build();
      Assert.fail("should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException e)
    {
      //expected
    }
  }

  @Test
  public void testSubResourceCreate() throws IOException, RestException
  {
    // curl -v -X POST -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 3" -d "{\"contactID\":\"3\",\"groupID\":\"1\",\"memberID\":\"3\",\"firstName\":\"Laura\",\"lastName\":\"Smith\",\"isPreapproved\":\"true\",\"isInvited\":\"true\",\"createdAt\":\"1300005945801\",\"updatedAt\":\"1300005945803\" }" http://localhost:1338/groups/1/contacts/

    GroupContact contact = new GroupContact();
    contact.setContactID(3);
    contact.setGroupID(1);
    contact.setMemberID(3);
    contact.setFirstName("Laura");
    contact.setLastName("Smith");
    contact.setIsPreapproved(true);
    contact.setIsInvited(true);

    Request<EmptyRecord>  request = CONTACTS_BUILDERS.create().groupIdKey(1).input(contact).build();

    assertEquals(request.getInput().data().size(),7);
    String expectedUri = "groups/1/contacts";
    checkRequestBuilder(request, ResourceMethod.CREATE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, contact);
  }

  @Test
  public void testSubResourceUpdate() throws IOException, RestException
  {
    // curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 3" -d "{\"lastName\":\"Anderson\"}" http://localhost:1338/groups/1/contacts/3

    GroupContact contact = new GroupContact();
    contact.setLastName("Anderson");
    PatchRequest<GroupContact> patch = PatchGenerator.diffEmpty(contact);

    Request<EmptyRecord>  request = CONTACTS_BUILDERS.partialUpdate().groupIdKey(1).id(3).input(patch).build();

    String expectedUri = "groups/1/contacts/3";
    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class,
                        EmptyRecord.class, expectedUri, patch);
  }

  @Test
  public void testSubResourceDelete() throws IOException, RestException
  {
    // curl -v -X DELETE -H "Expect:" -H "X-LinkedIn-Auth-Member: 3" http://localhost:1338/groups/1/contacts/3

    String expectedUri = "groups/1/contacts/3";

    Request<EmptyRecord>  request = CONTACTS_BUILDERS.delete().groupIdKey(1).id(3).build();

    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, null);
  }

  // Actions tests are covered in TestGroupsClient.java
  @Test
  public void testAction()
  {
    String testEmail = "test@test.com";
    TransferOwnershipRequest ownershipRequest = new TransferOwnershipRequest();
    ownershipRequest.setNewOwnerContactEmail(testEmail);
    int testId = 9999;
    ownershipRequest.setNewOwnerMemberID(testId);
    ActionRequest<Void> request = GROUPS_BUILDERS.actionTransferOwnership()
            .id(1)
            .paramRequest(ownershipRequest)
            .build();


    Map<FieldDef<?> , Object> parameters = new HashMap<FieldDef<?> , Object>(1);
    parameters.put(new FieldDef<TransferOwnershipRequest>("request", TransferOwnershipRequest.class,
                                                          DataTemplateUtil.getSchema(TransferOwnershipRequest.class)), ownershipRequest);
    DynamicRecordTemplate requestInput = createDynamicRecordTemplate("transferOwnership", parameters);
    String expectedUri = "groups/1?action=transferOwnership";
    checkRequestBuilder(request, ResourceMethod.ACTION, ActionResponseDecoder.class, null, expectedUri, requestInput);
  }

  private static DynamicRecordTemplate createDynamicRecordTemplate(String schemaName, Map<FieldDef<?>, Object> fieldDefs)
  {
    RecordDataSchema recordDataSchema = DynamicRecordMetadata.buildSchema(schemaName, fieldDefs.keySet());
    return new DynamicRecordTemplate(recordDataSchema, fieldDefs);
  }

  @Test
  public void testAssociationEntityGet() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/groupID=7\&memberID=1

    String expectedUri = "groupMemberships/groupID=7&memberID=1";

    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);
    Request<GroupMembership> request = GROUP_MEMBERSHIPS_BUILDERS.get().id(key).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class,
                        GroupMembership.class, expectedUri, null);
  }

  @Test
  public void testAssociationBatchGetByAssociationMultipleCompoundKeys2() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/groupID=1&memberID=1,groupID=2&memberID=1,groupID=2&memberID=2

    String expectedUri = "groupMemberships?ids=groupID%3D1%26memberID%3D1&ids=groupID%3D2%26memberID%3D1&ids=groupID%3D2%26memberID%3D2";

    GroupMembershipsBuilders.Key key1 = new GroupMembershipsBuilders.Key().setGroupId(1).setMemberId(1);
    GroupMembershipsBuilders.Key key2 = new GroupMembershipsBuilders.Key().setGroupId(2).setMemberId(1);
    GroupMembershipsBuilders.Key key3 = new GroupMembershipsBuilders.Key().setGroupId(2).setMemberId(2);

    Request<BatchResponse<GroupMembership>> request = GROUP_MEMBERSHIPS_BUILDERS.batchGet().ids(key1, key2, key3).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class,
                        GroupMembership.class, expectedUri, null);

  }

  @Test
  public void testAssociationEntityUpdate() throws IOException, RestException
  {
    // curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d "{\"lastName\":\"Anderson\"}" http://localhost:1338/groupMemberships/groupID=7\&memberID=1

    GroupMembership membership = new GroupMembership();
    membership.setLastName("Anderson");
    PatchRequest<GroupMembership> patch = PatchGenerator.diffEmpty(membership);

    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);

    Request<EmptyRecord>  request = GROUP_MEMBERSHIPS_BUILDERS.partialUpdate().id(key).input(patch).build();

    String expectedUri = "groupMemberships/groupID=7&memberID=1";
    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class,
                        EmptyRecord.class, expectedUri, patch);
  }

  @Test
  public void testAssociationEntityDelete() throws IOException, RestException
  {
    // curl -v -X DELETE -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" http://localhost:1338/groupMemberships/groupID=7\&memberID=1

    String expectedUri = "groupMemberships/groupID=7&memberID=1";

    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);

    Request<EmptyRecord>  request = GROUP_MEMBERSHIPS_BUILDERS.delete().id(key).build();

    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, null);
  }

  @Test
  public void testAssociationFinderByMemberID() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/memberID=1?q=member

    String expectedUri = "groupMemberships/memberID=1?q=member";

    GroupMembershipsFindByMemberBuilder requestBuilder = GROUP_MEMBERSHIPS_BUILDERS.findByMember().memberIdKey(1);
    Request<CollectionResponse<GroupMembership>> request = requestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, GroupMembership.class, expectedUri, null);
  }

  @Test
  public void testAssociationFinderByGroup() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/groupID=1?email=bruce@test.linkedin.com&firstName=Bruce&lastName=Willis&level=MEMBER&q=group&sort=lastNameAsc

    String expectedUri = "groupMemberships/groupID=1?email=bruce@test.linkedin.com&firstName=Bruce&lastName=Willis&level=MEMBER&q=group&sort=LAST_NAME_ASC";

    GroupMembershipsFindByGroupBuilder requestBuilder = GROUP_MEMBERSHIPS_BUILDERS.findByGroup()
            .groupIdKey(1)
            .levelParam("MEMBER")
            .firstNameParam("Bruce")
            .lastNameParam("Willis")
            .emailParam("bruce@test.linkedin.com")
            .sortParam(MembershipSortOrder.LAST_NAME_ASC);
    Request<CollectionResponse<GroupMembership>> request = requestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, GroupMembership.class, expectedUri, null);
  }

  @Test
  public void testAssociationFinderByGroupWithSomeOptionalParameters() throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/groupID=1?firstName=Bruce&q=group

    String expectedUri = "groupMemberships/groupID=1?firstName=Bruce&q=group";

    GroupMembershipsFindByGroupBuilder requestBuilder =
                    GROUP_MEMBERSHIPS_BUILDERS.findByGroup().groupIdKey(1).firstNameParam("Bruce");
    Request<CollectionResponse<GroupMembership>> request = requestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class,
                        GroupMembership.class, expectedUri, null);
  }

  @Test
  public void testRequestHeaderSupport()
  {
    final String X_LI_D2_TARGET_HOST = "X-LI-D2-Target-Host";
    final String TEST_HOST_VALUE = "http://test.linkedin.com/";
    Request<Group> request = GROUPS_BUILDERS.get().id(1).header(X_LI_D2_TARGET_HOST,
                                                                TEST_HOST_VALUE).build();

    Assert.assertTrue(request.getHeaders().containsKey(X_LI_D2_TARGET_HOST));
    assertEquals(request.getHeaders().get(X_LI_D2_TARGET_HOST), TEST_HOST_VALUE);
  }

  @Test
  public void testResourceNameOverrides()
  {
    GroupsBuilders groupsBuilders = new GroupsBuilders("SpecialGroups");
    Request<?> request = groupsBuilders.get().id(42).build();

    String expectedUri = "SpecialGroups/42";

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Group.class,
                        expectedUri, null);

    ContactsBuilders contactsBuilders = new ContactsBuilders("SpecialGroups");
    request = contactsBuilders.get().id(42).groupIdKey(1).build();

    expectedUri = "SpecialGroups/1/contacts/42";
    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, GroupContact.class,
                        expectedUri, null);
  }

  @Test
  public void testActionOnSubresource()
  {
    ActionRequest<Void> request = CONTACTS_BUILDERS.actionSpamContacts().groupIdKey(42).build();

    Map<FieldDef<?> , Object> parameters = new HashMap<FieldDef<?> , Object>(1);
    DynamicRecordTemplate requestInput = createDynamicRecordTemplate("spamContacts", parameters);

    String expectedUri = "groups/42/contacts?action=spamContacts";
    checkRequestBuilder(request, ResourceMethod.ACTION, ActionResponseDecoder.class, null, expectedUri, requestInput);
  }

  @SuppressWarnings({"rawtypes"})
  private static void checkRequestBuilder(Request<?> request, ResourceMethod resourceMethod,
                                   Class<? extends RestResponseDecoder> responseDecoderClass, Class<?> templateClass,
                                   String expectedUri, RecordTemplate requestInput)
  {
    assertEquals(request.getInput(), requestInput);
    assertEquals(request.getMethod(), resourceMethod);
    assertEquals(request.getResponseDecoder().getClass(), responseDecoderClass);
    assertEquals(request.getUri().toString(), expectedUri);
  }


}

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


import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RequestBuilder;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.GroupContact;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.api.MembershipSortOrder;
import com.linkedin.restli.examples.groups.api.TransferOwnershipRequest;
import com.linkedin.restli.examples.groups.client.ContactsBuilders;
import com.linkedin.restli.examples.groups.client.ContactsRequestBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsRequestBuilders;
import com.linkedin.restli.examples.groups.client.GroupsBuilders;
import com.linkedin.restli.examples.groups.client.GroupsRequestBuilders;
import com.linkedin.restli.internal.client.ActionResponseDecoder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;
import com.linkedin.restli.internal.client.EmptyResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Groups REST request builder unit test.
 *
 * @author Eran Leshem
 */

public class TestGroupsRequestBuilders
{
  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testEntityGet(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1

    String expectedUri = "groups/1";

    Request<Group> request = builders.get().id(1).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Group.class, expectedUri, null);

  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testEntityGetWithFields(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1?fields=badge

    String expectedUri = "groups/1?fields=badge";

    Request<Group> request = builders.get().id(1).fields(Group.fields().badge()).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testEntityCreate(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X POST -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d @group.json http://localhost:1338/groups/

    String expectedUri = "groups";

    Request<EmptyRecord>  request = builders.create().input(new Group()).build();

    checkRequestBuilder(request, ResourceMethod.CREATE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, new Group());
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testEntityUpdate(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    //Example: curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d "{\"name\":\"New Name\"}" http://localhost:1338/groups/1
    //This test example: curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d "{}" http://localhost:1338/groups/1

    String expectedUri = "groups/1";

    Request<EmptyRecord>  request = builders.partialUpdate().id(1).input(new PatchRequest<Group>()).build();

    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, new Group());
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testEntityDelete(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    //curl -v -X DELETE -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" http://localhost:1338/groups/1

    String expectedUri = "groups/1";

    Request<EmptyRecord>  request = builders.delete().id(1).build();

    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testCollectionFinderByEmailDomainWithFields(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0

    String expectedUri = "groups?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0";

    // Find by email domain with some debug, pagination and projection
    Request<CollectionResponse<Group>> request =
       builders
      .findBy("EmailDomain")
      .setQueryParam("emailDomain", "foo.com")
      .paginate(0, 10)
      .fields(Group.fields().state(), Group.fields().locale())
      .build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testCollectionFinderByManagerId(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?managerMemberID=1&q=manager

    String expectedUri = "groups?managerMemberID=1&q=manager";

    // Find by email domain with some debug, pagination and projection
    Request<CollectionResponse<Group>> request =
       builders.findBy("Manager").setQueryParam("managerMemberId", 1).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testCollectionFinderBySearch_AllValues(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?groupID=1&keywords=linkedin&nameKeywords=test&q=search

    String expectedUri = "groups?groupID=1&keywords=linkedin&nameKeywords=test&q=search";

    RequestBuilder<? extends Request<CollectionResponse<Group>>> findRequestBuilder =
        builders.findBy("Search")
                       .setQueryParam("keywords", "linkedin")
                       .setQueryParam("nameKeywords", "test")
                       .setQueryParam("groupId", 1).getBuilder();
    Request<CollectionResponse<Group>> request = findRequestBuilder.build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testCollectionFinderBySearchWithOptionalParamsTest1(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException, URISyntaxException
  {
    // curl -v -X GET http://localhost:1338/groups/?keywords=linkedin&q=search

    String expectedUri = "groups?keywords=linkedin&q=search";

    Request<CollectionResponse<Group>> request = builders.findBy("Search").setQueryParam("keywords", "linkedin").build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testCollectionFinderBySearchWithOptionalParamsTest2(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?keywords=linkedin&nameKeywords=test&q=search

    String expectedUri = "groups?keywords=linkedin&nameKeywords=test&q=search";

    Request<CollectionResponse<Group>> request = builders.findBy("Search")
      .setQueryParam("keywords", "linkedin")
      .setQueryParam("nameKeywords", "test")
      .build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testCollectionFinderBySearchWithOptionalParamsTest3(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?groupID=1&q=search

    String expectedUri = "groups?groupID=1&q=search";

    Request<CollectionResponse<Group>> request = builders.findBy("Search").setQueryParam("groupId", 1).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilder", expectedExceptions = NullPointerException.class)
  public void testCollectionFinderOmittingRequiredParams(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    builders.findBy("Manager").setQueryParam("managerMemberId", null);
  }

  @Test(dataProvider = "requestContactsBuilder", expectedExceptions = IllegalStateException.class)
  public void testCollectionEntityOmittingRequiredIdParam(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    builders.get().id(1).build();
  }

  @Test(dataProvider = "requestContactsBuilder", expectedExceptions = IllegalStateException.class)
  public void testCollectionEntityNullIdParam(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    builders.get().setPathKey("groupId", null).id(1).build();
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testBatchGet(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?fields=approvalModes&ids=1&ids=3

    String expectedUri = "groups?fields=approvalModes&ids=1&ids=3";

    Request<BatchResponse<Group>> request =
            builders.batchGet().ids(1, 3).fields(Group.fields().approvalModes()).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, Group.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilder", expectedExceptions = IllegalArgumentException.class)
  public void testBatchGetWithSelectedNullValues(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/?fields=approvalModes&ids=1&ids&ids=3
    builders.batchGet().ids(1, null, 3).fields(Group.fields().approvalModes());
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider")
  public void testSubResourceGet(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/1

    String expectedUri = "groups/1/contacts/1";

    Request<GroupContact> request = builders.get().setPathKey("groupId", 1).id(1).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, GroupContact.class, expectedUri, null);
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider")
  public void testSubResourceGetParamReordering(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/1

    String expectedUri = "groups/1/contacts/1";

    Request<GroupContact> request = builders.get().id(1).setPathKey("groupId", 1).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, GroupContact.class, expectedUri, null);
  }


  @Test(dataProvider = "requestContactsBuilderDataProvider")
  public void testSubResourceGetWithFields(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/1?fields=firstName,lastName

    String expectedUri = "groups/1/contacts/1?fields=lastName,firstName";

    Request<GroupContact> request = builders.get()
            .setPathKey("groupId", 1)
            .id(1)
            .fields(GroupContact.fields().firstName(), GroupContact.fields().lastName())
            .build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, GroupContact.class, expectedUri, null);
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider")
  public void testSubResourceBatchGet(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/?ids=1&ids=3

    String expectedUri = "groups/1/contacts?ids=1&ids=3";

    Request<BatchResponse<GroupContact>> request = builders.batchGet().setPathKey("groupId", 1).ids(1, 3).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, GroupContact.class, expectedUri, null);
  }

  @Test(dataProvider = "requestContactsBuilder", expectedExceptions = IllegalArgumentException.class)
  public void testSubResourceBatchGetWithSelectedNullValues(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groups/1/contacts/?ids=1&ids&ids=3
    builders.batchGet().setPathKey("groupId", 1).ids(1, null, 3).build();
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider")
  public void testSubResourceCreate(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
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

    Request<EmptyRecord>  request = builders.create().setPathKey("groupId", 1).input(contact).build();

    String expectedUri = "groups/1/contacts";
    checkRequestBuilder(request, ResourceMethod.CREATE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, contact);
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider")
  public void testSubResourceUpdate(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    // curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 3" -d "{\"lastName\":\"Anderson\"}" http://localhost:1338/groups/1/contacts/3

    GroupContact contact = new GroupContact();
    contact.setLastName("Anderson");
    PatchRequest<GroupContact> patch = PatchGenerator.diffEmpty(contact);

    Request<EmptyRecord>  request = builders.partialUpdate().setPathKey("groupId", 1).id(3).input(patch).build();

    String expectedUri = "groups/1/contacts/3";
    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class,
                        EmptyRecord.class, expectedUri, patch);
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider")
  public void testSubResourceDelete(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    // curl -v -X DELETE -H "Expect:" -H "X-LinkedIn-Auth-Member: 3" http://localhost:1338/groups/1/contacts/3

    String expectedUri = "groups/1/contacts/3";

    Request<EmptyRecord>  request = builders.delete().setPathKey("groupId", 1).id(3).build();

    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, null);
  }

  // Actions tests are covered in TestGroupsClient.java
  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testAction(RootBuilderWrapper<Integer, Group> builders)
  {
    String testEmail = "test@test.com";
    TransferOwnershipRequest ownershipRequest = new TransferOwnershipRequest();
    ownershipRequest.setNewOwnerContactEmail(testEmail);
    int testId = 9999;
    ownershipRequest.setNewOwnerMemberID(testId);
    Request<Void> request = builders.<Void>action("TransferOwnership")
            .id(1)
            .setActionParam("Request", ownershipRequest)
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

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationEntityGet(RootBuilderWrapper<CompoundKey, GroupMembership> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/groupID=7\&memberID=1

    String expectedUri = "groupMemberships/groupID=7&memberID=1";

    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);
    Request<GroupMembership> request = builders.get().id(key).build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class,
                        GroupMembership.class, expectedUri, null);
  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationBatchGetByAssociationMultipleCompoundKeys2(RootBuilderWrapper<CompoundKey, GroupMembership> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/groupID=1&memberID=1,groupID=2&memberID=1,groupID=2&memberID=2

    String expectedUri = "groupMemberships?ids=groupID%3D1%26memberID%3D1&ids=groupID%3D2%26memberID%3D1&ids=groupID%3D2%26memberID%3D2";

    GroupMembershipsBuilders.Key key1 = new GroupMembershipsBuilders.Key().setGroupId(1).setMemberId(1);
    GroupMembershipsBuilders.Key key2 = new GroupMembershipsBuilders.Key().setGroupId(2).setMemberId(1);
    GroupMembershipsBuilders.Key key3 = new GroupMembershipsBuilders.Key().setGroupId(2).setMemberId(2);

    Request<BatchResponse<GroupMembership>> request = builders.batchGet().ids(key1, key2, key3).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class,
                        GroupMembership.class, expectedUri, null);

  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationEntityUpdate(RootBuilderWrapper<CompoundKey, GroupMembership> builders) throws IOException, RestException
  {
    // curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d "{\"lastName\":\"Anderson\"}" http://localhost:1338/groupMemberships/groupID=7\&memberID=1

    GroupMembership membership = new GroupMembership();
    membership.setLastName("Anderson");
    PatchRequest<GroupMembership> patch = PatchGenerator.diffEmpty(membership);

    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);

    Request<EmptyRecord>  request = builders.partialUpdate().id(key).input(patch).build();

    String expectedUri = "groupMemberships/groupID=7&memberID=1";
    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class,
                        EmptyRecord.class, expectedUri, patch);
  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationEntityDelete(RootBuilderWrapper<CompoundKey, GroupMembership> builders) throws IOException, RestException
  {
    // curl -v -X DELETE -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" http://localhost:1338/groupMemberships/groupID=7\&memberID=1

    String expectedUri = "groupMemberships/groupID=7&memberID=1";

    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);

    Request<EmptyRecord>  request = builders.delete().id(key).build();

    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, EmptyRecord.class, expectedUri, null);
  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationFinderByMemberID(RootBuilderWrapper<CompoundKey, GroupMembership> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/memberID=1?q=member

    String expectedUri = "groupMemberships/memberID=1?q=member";

    Request<CollectionResponse<GroupMembership>> request = builders.findBy("Member").setPathKey("memberId", 1).build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, GroupMembership.class, expectedUri, null);
  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationFinderByGroup(RootBuilderWrapper<CompoundKey, GroupMembership> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/groupID=1?email=bruce@test.linkedin.com&firstName=Bruce&lastName=Willis&level=MEMBER&q=group&sort=lastNameAsc

    String expectedUri = "groupMemberships/groupID=1?email=bruce@test.linkedin.com&firstName=Bruce&lastName=Willis&level=MEMBER&q=group&sort=LAST_NAME_ASC";

    Request<CollectionResponse<GroupMembership>> request = builders.findBy("Group")
            .setPathKey("groupId", 1)
            .setQueryParam("level", "MEMBER")
            .setQueryParam("firstName", "Bruce")
            .setQueryParam("lastName", "Willis")
            .setQueryParam("email", "bruce@test.linkedin.com")
            .setQueryParam("sort", MembershipSortOrder.LAST_NAME_ASC)
      .build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, GroupMembership.class, expectedUri, null);
  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationFinderByGroupWithSomeOptionalParameters(RootBuilderWrapper<CompoundKey, GroupMembership> builders) throws IOException, RestException
  {
    // curl -v -X GET http://localhost:1338/groupMemberships/groupID=1?firstName=Bruce&q=group

    String expectedUri = "groupMemberships/groupID=1?firstName=Bruce&q=group";

    Request<CollectionResponse<GroupMembership>> request =
                    builders.findBy("Group").setPathKey("groupId", 1).setQueryParam("firstName", "Bruce").build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, GroupMembership.class, expectedUri, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testRequestHeaderSupport(RootBuilderWrapper<Integer, Group> builders)
  {
    final String X_LI_D2_TARGET_HOST = "X-LI-D2-Target-Host";
    final String TEST_HOST_VALUE = "http://test.linkedin.com/";
    Request<Group> request = builders.get().id(1).setHeader(X_LI_D2_TARGET_HOST, TEST_HOST_VALUE).build();

    Assert.assertTrue(request.getHeaders().containsKey(X_LI_D2_TARGET_HOST));
    assertEquals(request.getHeaders().get(X_LI_D2_TARGET_HOST), TEST_HOST_VALUE);
  }

  @Test(dataProvider = "requestSpecialBuilderDataProvider")
  public void testResourceNameOverrides(RootBuilderWrapper<Integer, Group> groupsBuilders, RootBuilderWrapper<Integer, GroupContact> contactsBuilders)
  {
    Request<Group> groupRequest = groupsBuilders.get().id(42).build();

    String expectedUri = "SpecialGroups/42";

    checkRequestBuilder(groupRequest, ResourceMethod.GET, EntityResponseDecoder.class, Group.class,
                        expectedUri, null);

    Request<GroupContact> contactRequest = contactsBuilders.get().id(42).setPathKey("groupId", 1).build();

    expectedUri = "SpecialGroups/1/contacts/42";
    checkRequestBuilder(contactRequest, ResourceMethod.GET, EntityResponseDecoder.class, GroupContact.class,
                        expectedUri, null);
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider")
  public void testActionOnSubresource(RootBuilderWrapper<Integer, GroupContact> builders)
  {
    Request<Void> request = builders.<Void>action("SpamContacts").setPathKey("groupId", 42).build();

    Map<FieldDef<?> , Object> parameters = new HashMap<FieldDef<?> , Object>(1);
    DynamicRecordTemplate requestInput = createDynamicRecordTemplate("spamContacts", parameters);

    String expectedUri = "groups/42/contacts?action=spamContacts";
    checkRequestBuilder(request, ResourceMethod.ACTION, ActionResponseDecoder.class, null, expectedUri, requestInput);
  }

  @SuppressWarnings({"rawtypes", "deprecation"})
  private static void checkRequestBuilder(Request<?> request, ResourceMethod resourceMethod,
                                   Class<? extends RestResponseDecoder> responseDecoderClass, Class<?> templateClass,
                                   String expectedUri, RecordTemplate requestInput)
  {
    testInput(request, requestInput);
    assertEquals(request.getMethod(), resourceMethod);
    assertEquals(request.getResponseDecoder().getClass(), responseDecoderClass);
    assertEquals(RestliUriBuilderUtil.createUriBuilder(request).build().toString(), expectedUri);
    assertEquals(request.getUri().toString(), expectedUri);
  }

  @SuppressWarnings("deprecation")
  private static void testInput(Request request, RecordTemplate expectedInput)
  {
    assertEquals(request.getInputRecord(), expectedInput);
    assertEquals(request.getInput(), expectedInput);
  }

  @DataProvider
  private static Object[][] requestGroupsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new GroupsBuilders()) },
      { new RootBuilderWrapper(new GroupsRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestContactsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new ContactsBuilders()) },
      { new RootBuilderWrapper(new ContactsRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestMembershipsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new GroupMembershipsBuilders()) },
      { new RootBuilderWrapper(new GroupMembershipsRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestSpecialBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper(new GroupsBuilders("SpecialGroups")), new RootBuilderWrapper(new ContactsBuilders("SpecialGroups")) },
      { new RootBuilderWrapper(new GroupsRequestBuilders("SpecialGroups")), new RootBuilderWrapper(new ContactsRequestBuilders("SpecialGroups")) },
    };
  }
}

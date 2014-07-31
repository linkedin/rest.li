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


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RequestBuilder;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.EntityResponse;
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
import com.linkedin.restli.internal.client.BatchEntityResponseDecoder;
import com.linkedin.restli.internal.client.BatchKVResponseDecoder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;
import com.linkedin.restli.internal.client.CreateResponseDecoder;
import com.linkedin.restli.internal.client.EmptyResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.IdResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.testutils.URIDetails;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderEntity")
  public void testEntityGet(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<Group> request = builders.get().id(1).build();
    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderEntityWithFields")
  public void testEntityGetWithFields(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails) throws IOException, RestException
  {
    Request<Group> request = builders.get().id(1).fields(Group.fields().badge()).build();
    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderNonEntity")
  public void testEntityCreate(URIDetails expectedURIDetails) throws IOException, RestException
  {
    Request<EmptyRecord> request = new GroupsBuilders().create().input(new Group()).build();
    checkRequestBuilder(request, ResourceMethod.CREATE, CreateResponseDecoder.class, expectedURIDetails, new Group());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderNonEntity")
  public void testEntityCreateId(URIDetails expectedURIDetails) throws IOException, RestException
  {
    CreateIdRequest<Integer, Group> request = new GroupsRequestBuilders().create().input(new Group()).build();
    checkRequestBuilder(request, ResourceMethod.CREATE, IdResponseDecoder.class, expectedURIDetails, new Group());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderEntity")
  public void testEntityUpdate(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<EmptyRecord>  request = builders.partialUpdate().id(1).input(new PatchRequest<Group>()).build();
    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class, expectedURIDetails, new Group());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderEntity")
  public void testEntityDelete(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<EmptyRecord>  request = builders.delete().id(1).build();
    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderFindByEmailDomainWithFields")
  public void testCollectionFinderByEmailDomainWithFields(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetailsDetails)
    throws IOException, RestException
  {
    // Find by email domain with some debug, pagination and projection
    Request<CollectionResponse<Group>> request =
      builders
        .findBy("EmailDomain")
        .setQueryParam("emailDomain", "foo.com")
        .paginate(0, 10)
        .fields(Group.fields().state(), Group.fields().locale())
        .build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetailsDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderFindByManagerId")
  public void testCollectionFinderByManagerId(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    // Find by email domain with some debug, pagination and projection
    Request<CollectionResponse<Group>> request = builders.findBy("Manager").setQueryParam("managerMemberId", 1).build();
    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderSearch")
  public void testCollectionFinderBySearch_AllValues(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    RequestBuilder<? extends Request<CollectionResponse<Group>>> findRequestBuilder =
      builders.findBy("Search")
        .setQueryParam("keywords", "linkedin")
        .setQueryParam("nameKeywords", "test")
        .setQueryParam("groupId", 1).getBuilder();
    Request<CollectionResponse<Group>> request = findRequestBuilder.build();
    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderSearchWithOptional1")
  public void testCollectionFinderBySearchWithOptionalParamsTest1(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
    throws IOException, RestException, URISyntaxException
  {
    Request<CollectionResponse<Group>> request = builders.findBy("Search").setQueryParam("keywords", "linkedin").build();
    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderSearchWithOptional2")
  public void testCollectionFinderBySearchWithOptionalParamsTest2(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<CollectionResponse<Group>> request = builders.findBy("Search")
      .setQueryParam("keywords", "linkedin")
      .setQueryParam("nameKeywords", "test")
      .build();
    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderSearchWithOptional3")
  public void testCollectionFinderBySearchWithOptionalParamsTest3(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<CollectionResponse<Group>> request = builders.findBy("Search").setQueryParam("groupId", 1).build();
    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider", expectedExceptions = NullPointerException.class)
  public void testCollectionFinderOmittingRequiredParams(RootBuilderWrapper<Integer, Group> builders) throws IOException, RestException
  {
    builders.findBy("Manager").setQueryParam("managerMemberId", null);
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider", expectedExceptions = IllegalStateException.class)
  public void testCollectionEntityOmittingRequiredIdParam(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    builders.get().id(1).build();
  }

  @Test(dataProvider = "requestContactsBuilderDataProvider", expectedExceptions = IllegalStateException.class)
  public void testCollectionEntityNullIdParam(RootBuilderWrapper<Integer, GroupContact> builders) throws IOException, RestException
  {
    builders.get().setPathKey("groupId", null).id(1).build();
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBatchDataProvider")
  public void testBatchGet(URIDetails expectedURIDetails) throws IOException, RestException
  {
    Request<BatchResponse<Group>> request = new GroupsBuilders().batchGet().ids(1, 3).fields(Group.fields().approvalModes()).build();
    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBatchDataProvider")
  public void testBatchGetKV(URIDetails expectedURIDetails) throws IOException, RestException
  {
    Request<BatchKVResponse<Integer, Group>> request = new GroupsBuilders().batchGet().ids(1, 3).fields(Group.fields().approvalModes()).buildKV();
    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchKVResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBatchDataProvider")
  public void testBatchGetEntity(URIDetails expectedURIDetails) throws IOException, RestException
  {
    Request<BatchKVResponse<Integer, EntityResponse<Group>>> request = new GroupsRequestBuilders().batchGet().ids(1, 3).fields(Group.fields().approvalModes()).build();
    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchEntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBatchGetWithSelectedNullValues() throws IOException, RestException
  {
    new GroupsBuilders().batchGet().ids(1, null, 3).fields(Group.fields().approvalModes());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBatchGetEntityWithSelectedNullValues() throws IOException, RestException
  {
    new GroupsRequestBuilders().batchGet().ids(1, null, 3).fields(Group.fields().approvalModes());
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderEntity")
  public void testSubResourceGet(RootBuilderWrapper<Integer, GroupContact> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<GroupContact> request = builders.get().setPathKey("groupId", 1).id(1).build();
    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderEntity")
  public void testSubResourceGetParamReordering(RootBuilderWrapper<Integer, GroupContact> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<GroupContact> request = builders.get().id(1).setPathKey("groupId", 1).build();
    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderEntityWithFields")
  public void testSubResourceGetWithFields(RootBuilderWrapper<Integer, GroupContact> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<GroupContact> request = builders.get()
      .setPathKey("groupId", 1)
      .id(1)
      .fields(GroupContact.fields().firstName(), GroupContact.fields().lastName())
      .build();

    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBatchDataProvider")
  public void testSubResourceBatchGet(URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<BatchResponse<GroupContact>> request = new ContactsBuilders().batchGet().groupIdKey(1).ids(1, 3).build();
    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBatchDataProvider")
  public void testSubResourceBatchGetKV(URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<BatchKVResponse<Integer, GroupContact>> request = new ContactsBuilders().batchGet().groupIdKey(1).ids(1, 3).buildKV();
    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchKVResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBatchDataProvider")
  public void testSubResourceBatchGetEntity(URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<BatchKVResponse<Integer, EntityResponse<GroupContact>>> request = new ContactsRequestBuilders().batchGet().groupIdKey(1).ids(1, 3).build();
    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchEntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubResourceBatchGetWithSelectedNullValues() throws IOException, RestException
  {
    new ContactsBuilders().batchGet().groupIdKey(1).ids(1, null, 3).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubResourceBatchGetKVWithSelectedNullValues() throws IOException, RestException
  {
    new ContactsBuilders().batchGet().groupIdKey(1).ids(1, null, 3).buildKV();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubResourceBatchGetEntityWithSelectedNullValues() throws IOException, RestException
  {
    new ContactsRequestBuilders().batchGet().groupIdKey(1).ids(1, null, 3).build();
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderNonEntity")
  public void testSubResourceCreate(URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    GroupContact contact = new GroupContact();
    contact.setContactID(3);
    contact.setGroupID(1);
    contact.setMemberID(3);
    contact.setFirstName("Laura");
    contact.setLastName("Smith");
    contact.setIsPreapproved(true);
    contact.setIsInvited(true);

    Request<EmptyRecord> oldRequest = new ContactsBuilders().create().groupIdKey(1).input(contact).build();
    checkRequestBuilder(oldRequest, ResourceMethod.CREATE, CreateResponseDecoder.class, expectedURIDetails, contact);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderNonEntity")
  public void testSubResourceCreateId(URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    GroupContact contact = new GroupContact();
    contact.setContactID(3);
    contact.setGroupID(1);
    contact.setMemberID(3);
    contact.setFirstName("Laura");
    contact.setLastName("Smith");
    contact.setIsPreapproved(true);
    contact.setIsInvited(true);

    CreateIdRequest<Integer, GroupContact> newRequest = new ContactsRequestBuilders().create().groupIdKey(1).input(contact).build();
    checkRequestBuilder(newRequest, ResourceMethod.CREATE, IdResponseDecoder.class, expectedURIDetails, contact);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderEntity")
  public void testSubResourceUpdate(RootBuilderWrapper<Integer, GroupContact> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    GroupContact contact = new GroupContact();
    contact.setLastName("Anderson");
    PatchRequest<GroupContact> patch = PatchGenerator.diffEmpty(contact);

    Request<EmptyRecord>  request = builders.partialUpdate().setPathKey("groupId", 1).id(1).input(patch).build();
    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class, expectedURIDetails, patch);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderEntity")
  public void testSubResourceDelete(RootBuilderWrapper<Integer, GroupContact> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<EmptyRecord>  request = builders.delete().setPathKey("groupId", 1).id(1).build();
    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, expectedURIDetails, null);
  }

  // Actions tests are covered in TestGroupsClient.java

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderEntityAction")
  public void testAction(RootBuilderWrapper<Integer, Group> builders, URIDetails expectedURIDetails)
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
    checkRequestBuilder(request, ResourceMethod.ACTION, ActionResponseDecoder.class, expectedURIDetails, requestInput);
  }

  private static DynamicRecordTemplate createDynamicRecordTemplate(String schemaName, Map<FieldDef<?>, Object> fieldDefs)
  {
    RecordDataSchema recordDataSchema = DynamicRecordMetadata.buildSchema(schemaName, fieldDefs.keySet());
    return new DynamicRecordTemplate(recordDataSchema, fieldDefs);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntity")
  public void testAssociationEntityGet(RootBuilderWrapper<CompoundKey, GroupMembership> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);
    Request<GroupMembership> request = builders.get().id(key).build();
    checkRequestBuilder(request, ResourceMethod.GET, EntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBatchDataProvider")
  public void testAssociationBatchGetByAssociationMultipleCompoundKeys2(URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    GroupMembershipsBuilders.Key key1 = new GroupMembershipsBuilders.Key().setGroupId(1).setMemberId(1);
    GroupMembershipsBuilders.Key key2 = new GroupMembershipsBuilders.Key().setGroupId(2).setMemberId(1);
    GroupMembershipsBuilders.Key key3 = new GroupMembershipsBuilders.Key().setGroupId(2).setMemberId(2);

    Request<BatchKVResponse<CompoundKey, GroupMembership>> request =
        new GroupMembershipsBuilders().batchGet().ids(key1, key2, key3).buildKV();
    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchKVResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBatchDataProvider")
  public void testAssociationBatchGetEntityByAssociationMultipleCompoundKeys2(URIDetails expectedURIDetails)
    throws IOException, RestException, URISyntaxException
  {
    GroupMembershipsBuilders.Key key1 = new GroupMembershipsBuilders.Key().setGroupId(1).setMemberId(1);
    GroupMembershipsBuilders.Key key2 = new GroupMembershipsBuilders.Key().setGroupId(2).setMemberId(1);
    GroupMembershipsBuilders.Key key3 = new GroupMembershipsBuilders.Key().setGroupId(2).setMemberId(2);

    Request<BatchKVResponse<CompoundKey, EntityResponse<GroupMembership>>> request = new GroupMembershipsRequestBuilders().batchGet().ids(key1, key2, key3).build();

    checkRequestBuilder(request, ResourceMethod.BATCH_GET, BatchEntityResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntity")
  public void testAssociationEntityUpdate(RootBuilderWrapper<CompoundKey, GroupMembership> builders, URIDetails expectedURIDetails) throws IOException, RestException
  {
    GroupMembership membership = new GroupMembership();
    membership.setLastName("Anderson");
    PatchRequest<GroupMembership> patch = PatchGenerator.diffEmpty(membership);
    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);

    Request<EmptyRecord>  request = builders.partialUpdate().id(key).input(patch).build();
    checkRequestBuilder(request, ResourceMethod.PARTIAL_UPDATE, EmptyResponseDecoder.class, expectedURIDetails, patch);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntity")
  public void testAssociationEntityDelete(RootBuilderWrapper<CompoundKey, GroupMembership> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    GroupMembershipsBuilders.Key key = new GroupMembershipsBuilders.Key().setGroupId(7).setMemberId(1);
    Request<EmptyRecord>  request = builders.delete().id(key).build();
    checkRequestBuilder(request, ResourceMethod.DELETE, EmptyResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntityFinderByMember")
  public void testAssociationFinderByMemberID(RootBuilderWrapper<CompoundKey, GroupMembership> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<CollectionResponse<GroupMembership>> request = builders.findBy("Member").setPathKey("memberId", 1).build();
    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntityFinderByGroup")
  public void testAssociationFinderByGroup(RootBuilderWrapper<CompoundKey, GroupMembership> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<CollectionResponse<GroupMembership>> request = builders.findBy("Group")
      .setPathKey("groupId", 1)
      .setQueryParam("level", "MEMBER")
      .setQueryParam("firstName", "Bruce")
      .setQueryParam("lastName", "Willis")
      .setQueryParam("email", "bruce@test.linkedin.com")
      .setQueryParam("sort", MembershipSortOrder.LAST_NAME_ASC)
      .build();

    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetails, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntityFinderByGroupWithOptional")
  public void testAssociationFinderByGroupWithSomeOptionalParameters(RootBuilderWrapper<CompoundKey, GroupMembership> builders, URIDetails expectedURIDetails)
    throws IOException, RestException
  {
    Request<CollectionResponse<GroupMembership>> request =
      builders.findBy("Group").setPathKey("groupId", 1).setQueryParam("firstName", "Bruce").build();
    checkRequestBuilder(request, ResourceMethod.FINDER, CollectionResponseDecoder.class, expectedURIDetails, null);
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

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSpecialBuilderDataProvider")
  public void testResourceNameOverrides(RootBuilderWrapper<Integer, Group> groupsBuilders,
                                        RootBuilderWrapper<Integer, GroupContact> contactsBuilders,
                                        URIDetails expectedURIDetails1,
                                        URIDetails expectedURIDetails2)
  {
    Request<Group> groupRequest = groupsBuilders.get().id(42).build();
    checkRequestBuilder(groupRequest, ResourceMethod.GET, EntityResponseDecoder.class,
                        expectedURIDetails1, null);

    Request<GroupContact> contactRequest = contactsBuilders.get().id(42).setPathKey("groupId", 1).build();
    checkRequestBuilder(contactRequest, ResourceMethod.GET, EntityResponseDecoder.class,
                        expectedURIDetails2, null);
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderAction")
  public void testActionOnSubresource(RootBuilderWrapper<Integer, GroupContact> builders, URIDetails expectedURIDetails)
  {
    Request<Void> request = builders.<Void>action("SpamContacts").setPathKey("groupId", 42).build();

    Map<FieldDef<?> , Object> parameters = new HashMap<FieldDef<?> , Object>(1);
    DynamicRecordTemplate requestInput = createDynamicRecordTemplate("spamContacts", parameters);
    checkRequestBuilder(request, ResourceMethod.ACTION, ActionResponseDecoder.class, expectedURIDetails, requestInput);
  }

  @SuppressWarnings("rawtypes")
  private static void checkRequestBuilder(Request<?> request,
                                          ResourceMethod resourceMethod,
                                          Class<? extends RestResponseDecoder> responseDecoderClass,
                                          URIDetails expectedURIDetails,
                                          RecordTemplate requestInput)
  {
    testInput(request, requestInput);
    assertEquals(request.getMethod(), resourceMethod);
    assertEquals(request.getResponseDecoder().getClass(), responseDecoderClass);
    URIDetails.testUriGeneration(request, expectedURIDetails);
  }

  private static void testInput(Request<?> request, RecordTemplate expectedInput)
  {
    assertEquals(request.getInputRecord(), expectedInput);
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderEntity")
  private static Object[][] requestGroupsBuilderDataProviderEntity()
  {
    //Sample URIs:
    //"groups/1"
    //"groups/1"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups/1",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups/1",
        null, null, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderEntityWithFields")
  private static Object[][] requestGroupsBuilderDataProviderEntityWithFields()
  {
    //Sample URIs:
    //"groups/1?fields=badge"
    //"groups/1?fields=badge"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("badge");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups/1",
        null, null, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups/1",
        null, null, fieldSet);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderNonEntity")
  private static Object[][] requestGroupsBuilderDataProviderNonEntity()
  {
    //Sample URIs:
    //"groups"
    //"groups"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups",
        null, null, null);

    return new Object[][] {
      { uriDetails1 },
      { uriDetails2 },
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderFindByEmailDomainWithFields")
  private static Object[][] requestGroupsBuilderDataProviderFindByEmailDomainWithFields()
  {
    //Sample URIs:
    //"groups?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0"
    //"groups?count=10&emailDomain=foo.com&fields=locale,state&q=emailDomain&start=0"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("locale");
    fieldSet.add("state");

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("count", "10");
    queryParamsMap.put("emailDomain", "foo.com");
    queryParamsMap.put("q", "emailDomain");
    queryParamsMap.put("start", "0");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, fieldSet);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderFindByManagerId")
  private static Object[][] requestGroupsBuilderDataProviderFindByManagerId()
  {
    //Sample URIs:
    //"groups?managerMemberID=1&q=manager"
    //"groups?managerMemberID=1&q=manager"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("managerMemberID", "1");
    queryParamsMap.put("q", "manager");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderSearch")
  private static Object[][] requestGroupsBuilderDataProviderSearch()
  {
    //Sample URIs:
    //"groups?groupID=1&keywords=linkedin&nameKeywords=test&q=search"
    //"groups?groupID=1&keywords=linkedin&nameKeywords=test&q=search"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("groupID", "1");
    queryParamsMap.put("keywords", "linkedin");
    queryParamsMap.put("nameKeywords", "test");
    queryParamsMap.put("q", "search");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderSearchWithOptional1")
  private static Object[][] requestGroupsBuilderDataProviderSearchWithOptional1()
  {
    //Sample URIs:
    //"groups?keywords=linkedin&q=search"
    //"groups?keywords=linkedin&q=search"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("keywords", "linkedin");
    queryParamsMap.put("q", "search");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderSearchWithOptional2")
  private static Object[][] requestGroupsBuilderDataProviderSearchWithOptional2()
  {
    //Sample URIs:
    //"groups?keywords=linkedin&nameKeywords=test&q=search"
    //"groups?keywords=linkedin&nameKeywords=test&q=search"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("keywords", "linkedin");
    queryParamsMap.put("nameKeywords", "test");
    queryParamsMap.put("q", "search");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderSearchWithOptional3")
  private static Object[][] requestGroupsBuilderDataProviderSearchWithOptional3()
  {
    //Sample URIs:
    //"groups?groupID=1&q=search"
    //"groups?groupID=1&q=search"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("groupID", "1");
    queryParamsMap.put("q", "search");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups",
        null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBatchDataProvider")
  private static Object[][] requestGroupsBatchDataProviderBatch()
  {
    //Sample URIs:
    //"groups?fields=approvalModes&ids=1&ids=3"
    //"groups?fields=approvalModes&ids=List(1,3)"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("approvalModes");

    final Set<String> idSet = new HashSet<String>();
    idSet.add("1");
    idSet.add("3");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups",
        idSet, null, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups",
        idSet, null, fieldSet);

    return new Object[][] {
      { uriDetails1 },
      { uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderEntity")
  private static Object[][] requestContactsBuilderDataProviderEntity()
  {
    //Sample URIs:
    //"groups/1/contacts/1"
    //"groups/1/contacts/1"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups/1/contacts/1",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups/1/contacts/1",
        null, null, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderEntityWithFields")
  private static Object[][] requestContactsBuilderDataProviderEntityWithFields()
  {
    //Sample URIs:
    //"groups/1/contacts/1?fields=lastName,firstName"
    //"groups/1/contacts/1?fields=lastName,firstName"

    final Set<String> fieldSet = new HashSet<String>();
    fieldSet.add("lastName");
    fieldSet.add("firstName");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups/1/contacts/1",
        null, null, fieldSet);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups/1/contacts/1",
        null, null, fieldSet);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBatchDataProvider")
  private static Object[][] requestContactsBatchDataProvider()
  {
    //Sample URIs:
    //"groups/1/contacts?ids=1&ids=3"
    //"groups/1/contacts?ids=List(1,3)"

    final Set<String> idSet = new HashSet<String>();
    idSet.add("1");
    idSet.add("3");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups/1/contacts",
        idSet, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups/1/contacts",
        idSet, null, null);

    return new Object[][] {
      { uriDetails1 },
      { uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderNonEntity")
  private static Object[][] requestContactsBuilderDataProviderNonEntity()
  {
    //Sample URIs:
    //"groups/1/contacts"
    //"groups/1/contacts"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups/1/contacts",
        null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups/1/contacts",
        null, null, null);

    return new Object[][] {
      { uriDetails1 },
      { uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestGroupsBuilderDataProviderEntityAction")
  private static Object[][] requestGroupsBuilderDataProviderEntityAction()
  {
    //Sample URIs:
    //"groups/1?action=transferOwnership"
    //"groups/1?action=transferOwnership"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "transferOwnership");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groups/1",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groups/1",
        null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntity")
  private static Object[][] requestMembershipsBuilderDataProviderEntity()
  {
    //Sample URIs:
    //"groupMemberships/groupID=7&memberID=1"
    //"groupMemberships/(groupID:7,memberID:1)"

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "groupMemberships/groupID=7&memberID=1", null, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "groupMemberships/(groupID:7,memberID:1)", null, null, null);

    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBatchDataProvider")
  private static Object[][] requestMembershipsBatchDataProviderBatch()
  {
    //Sample URIs:
    //"groupMemberships?ids=groupID%3D1%26memberID%3D1&ids=groupID%3D2%26memberID%3D1&ids=groupID%3D2%26memberID%3D2"
    //"groupMemberships?ids=List((groupID:2,memberID:1),(groupID:2,memberID:2),(groupID:1,memberID:1))"

    //Note that we need two different ID sets, one for V1 and one for V2 since batch operations on compound keys
    //are unique.
    final Set<String> idSetV1 = new HashSet<String>();
    idSetV1.add("groupID=2&memberID=1");
    idSetV1.add("groupID=2&memberID=2");
    idSetV1.add("groupID=1&memberID=1");

    final Set<DataMap> idSetV2 = new HashSet<DataMap>();
    final DataMap id1 = new DataMap();
    id1.put("groupID", "2");
    id1.put("memberID", "1");
    final DataMap id2 = new DataMap();
    id2.put("groupID", "2");
    id2.put("memberID", "2");
    final DataMap id3 = new DataMap();
    id3.put("groupID", "1");
    id3.put("memberID", "1");
    idSetV2.add(id1);
    idSetV2.add(id2);
    idSetV2.add(id3);

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groupMemberships",
        idSetV1, null, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groupMemberships",
        idSetV2, null, null);

    return new Object[][] {
      { uriDetails1 },
      { uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntityFinderByMember")
  private static Object[][] requestMembershipsBuilderDataProviderEntityFinderByMember()
  {
    //Sample URIs:
    //"groupMemberships/memberID=1?q=member"
    //"groupMemberships/(memberID:1)?q=member"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("q", "member");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "groupMemberships/memberID=1", null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "groupMemberships/(memberID:1)", null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntityFinderByGroup")
  private static Object[][] requestMembershipsBuilderDataProviderEntityFinderByGroup()
  {
    //Sample URIs:
    //"groupMemberships/groupID=1?email=bruce@test.linkedin.com&firstName=Bruce&lastName=Willis&level=MEMBER&q=group&sort=LAST_NAME_ASC"
    //"groupMemberships/(groupID:1)?email=bruce@test.linkedin.com&firstName=Bruce&lastName=Willis&level=MEMBER&q=group&sort=LAST_NAME_ASC"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("email", "bruce@test.linkedin.com");
    queryParamsMap.put("firstName", "Bruce");
    queryParamsMap.put("lastName", "Willis");
    queryParamsMap.put("level", "MEMBER");
    queryParamsMap.put("q", "group");
    queryParamsMap.put("sort", "LAST_NAME_ASC");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groupMemberships/groupID=1",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groupMemberships/(groupID:1)",
        null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestMembershipsBuilderDataProviderEntityFinderByGroupWithOptional")
  private static Object[][] requestMembershipsBuilderDataProviderEntityFinderByGroupWithOptional()
  {
    //Sample URIs:
    //"groupMemberships/groupID=1?firstName=Bruce&q=group"
    //"groupMemberships/(groupID:1)?firstName=Bruce&q=group"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("firstName", "Bruce");
    queryParamsMap.put("q", "group");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), "groupMemberships/groupID=1",
        null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(), "groupMemberships/(groupID:1)",
        null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestSpecialBuilderDataProvider")
  private static Object[][] requestSpecialBuilderDataProvider()
  {
    //Sample URIs:
    //"SpecialGroups/42"
    //"SpecialGroups/1/contacts/42"
    //"SpecialGroups/42"
    //"SpecialGroups/1/contacts/42"

    final URIDetails uriDetailsV1_1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "SpecialGroups/42", null, null, null);

    final URIDetails uriDetailsV1_2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "SpecialGroups/1/contacts/42", null, null, null);

    final URIDetails uriDetailsV2_1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "SpecialGroups/42", null, null, null);

    final URIDetails uriDetailsV2_2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(),
        "SpecialGroups/1/contacts/42", null, null, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders("SpecialGroups")), new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders("SpecialGroups")),
        uriDetailsV1_1, uriDetailsV1_2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders("SpecialGroups")), new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders("SpecialGroups")),
        uriDetailsV2_1, uriDetailsV2_2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders("SpecialGroups")), new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders("SpecialGroups")),
        uriDetailsV1_1, uriDetailsV1_2 },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders("SpecialGroups")), new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders("SpecialGroups")),
        uriDetailsV2_1, uriDetailsV2_2},
    };
  }

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "requestContactsBuilderDataProviderAction")
  private static Object[][] requestContactsBuilderDataProviderAction()
  {
    //Sample URIs:
    //"groups/42/contacts?action=spamContacts"
    //"groups/42/contacts?action=spamContacts"

    final Map<String, String> queryParamsMap = new HashMap<String, String>();
    queryParamsMap.put("action", "spamContacts");

    final URIDetails uriDetails1 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion(), 
        "groups/42/contacts", null, queryParamsMap, null);

    final URIDetails uriDetails2 = new URIDetails(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion(),
        "groups/42/contacts", null, queryParamsMap, null);

    return new Object[][] {
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders()), uriDetails2 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders()), uriDetails1 },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders()), uriDetails2 }
    };
  }

  @DataProvider
  private static Object[][] requestGroupsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()) },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestContactsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsBuilders()) },
      { new RootBuilderWrapper<Integer, GroupContact>(new ContactsRequestBuilders()) }
    };
  }
}

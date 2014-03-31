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

package com.linkedin.restli.examples;


import com.linkedin.data.template.IntegerArray;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.BatchGetKVRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.examples.groups.api.ComplexKeyGroupMembership;
import com.linkedin.restli.examples.groups.api.EmailDigestFrequency;
import com.linkedin.restli.examples.groups.api.Group;
import com.linkedin.restli.examples.groups.api.GroupMembership;
import com.linkedin.restli.examples.groups.api.GroupMembershipKey;
import com.linkedin.restli.examples.groups.api.GroupMembershipParam;
import com.linkedin.restli.examples.groups.api.GroupMembershipParamArray;
import com.linkedin.restli.examples.groups.api.GroupMembershipQueryParam;
import com.linkedin.restli.examples.groups.api.GroupMembershipQueryParamArray;
import com.linkedin.restli.examples.groups.api.MembershipLevel;
import com.linkedin.restli.examples.groups.api.WriteLevel;
import com.linkedin.restli.examples.groups.client.GroupMembershipsBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsComplexBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsComplexRequestBuilders;
import com.linkedin.restli.examples.groups.client.GroupMembershipsRequestBuilders;
import com.linkedin.restli.examples.groups.client.GroupsBuilders;
import com.linkedin.restli.examples.groups.client.GroupsRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * This test class is currently an integration test, requiring that you have the groups server running.
 * todo: mock rap layer, so this becomes a unit test.
 *
 * Sample URLs from Nick:
 * The files referenced in the POSTs/PUTs are under groups-rest-proto/src/test/resources.
 * Do note the {group_id} template variables in the puts need to be replaced with a real groupID in your MongoDB
 *
 * -- alfred creates a group
 * curl -v -X POST -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d @group.json http://localhost:1337/groups
 *
 * -- bruce requests to join
 * curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 2" -d @membership-bruce.json http://localhost:1337/groups/{group_id}/members/2
 *
 * -- alfred approves bruce
 * curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 1" -d @membership-update-state-change.json http://localhost:1337/groups/{group_id}/members/2
 *
 * -- bruce leaves
 * curl -v -X PUT -H "Content-Type: application/json" -H "Expect:" -H "X-LinkedIn-Auth-Member: 2" -d @membership-leave.json http://localhost:1337/groups/{group_id}/members/2
 *
 * @author rrangana
 * @author Eran Leshem
 */
public class TestGroupsClient extends RestLiIntegrationTest
{
  private static final Client CLIENT = new TransportClientAdapter(new HttpClientFactory().getClient(Collections.<String, String>emptyMap()));
  private static final String URI_PREFIX = "http://localhost:1338/";
  private static final RestClient REST_CLIENT = new RestClient(CLIENT, URI_PREFIX);

  @BeforeClass
  public void initClass() throws Exception
  {
    super.init();
  }

  @AfterClass
  public void shutDown() throws Exception
  {
    super.shutdown();
  }

  @Test(dataProvider = "requestGroupsAndMembershipsBuilderDataProvider")
  public void testCollectionCreateGetUpdateDelete(RootBuilderWrapper<Integer, Group> groupBuilders,
                                                  RootBuilderWrapper<CompoundKey, GroupMembership> membershipBuilders)
    throws RemoteInvocationException
  {
    // find with optional params
    Group group = new Group();
    String name = "test";
    int memberID = 1;
    group.setName(name);
    group.setOwner(buildGroupMembership(memberID, "a@a.a", "f", "l"));
    GroupMembershipParam param = new GroupMembershipParam();
    param.setIntParameter(1);
    param.setStringParameter("String");
    // Create
    Response<EmptyRecord> response = REST_CLIENT.sendRequest(groupBuilders.create()
                                                               .input(group)
                                                               .build()).getResponse();
    Assert.assertEquals(response.getStatus(), 201);
    Assert.assertNotNull(response.getId());

    // Get newly created group and verify name
    Integer createdId = Integer.valueOf(response.getId());
    Assert.assertEquals(REST_CLIENT.sendRequest(groupBuilders.get()
                                                  .id(createdId)
                                                  .build())
                          .getResponse()
                          .getEntity()
                          .getName(),
                        name);

    // Partial update - change name
    String newName = "new name";
    group.setName(newName);
    PatchRequest<Group> patch = PatchGenerator.diffEmpty(group);
    ResponseFuture<EmptyRecord> responseFuture = REST_CLIENT.sendRequest(groupBuilders.partialUpdate()
                                                                           .id(createdId)
                                                                           .input(patch)
                                                                           .build());
    Assert.assertEquals(204, responseFuture.getResponse().getStatus());

    // Get updated group and verify name
    Assert.assertEquals(REST_CLIENT.sendRequest(groupBuilders.get()
                                                               .id(createdId)
                                                               .build())
                                   .getResponse()
                                   .getEntity()
                                   .getName(),
                        newName);

    // Delete
    responseFuture = REST_CLIENT.sendRequest(groupBuilders.delete().id(createdId).build());
    Assert.assertEquals(204, responseFuture.getResponse().getStatus());

    // Verify deleted
    try
    {
      REST_CLIENT.sendRequest(groupBuilders.get().id(createdId).build()).getResponse();
      Assert.fail("Expected RestLiResponseException");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), 404);
    }

    // Cleanup - delete the owner's membership that was created along with the group
    responseFuture =
        REST_CLIENT.sendRequest(membershipBuilders.delete()
                                  .id(buildCompoundKey(memberID, createdId))
                                  .build());
    Assert.assertEquals(204, responseFuture.getResponse().getStatus());
  }

  private static GroupMembership buildGroupMembership(Integer id,
                                                      String contactEmail,
                                                      String firstName,
                                                      String lastName)
  {
    GroupMembership groupMembership = new GroupMembership();
    if (id != null)
    {
      groupMembership.setMemberID(id);
    }
    groupMembership.setContactEmail(contactEmail);
    groupMembership.setFirstName(firstName);
    groupMembership.setLastName(lastName);
    // These fields must be set when creating a stand-alone group membership,
    // lest the membership be considered incomplete.
    groupMembership.setAllowMessagesFromMembers(true);
    groupMembership.setEmailAnnouncementsFromManagers(true);
    groupMembership.setEmailDigestFrequency(EmailDigestFrequency.WEEKLY);
    groupMembership.setEmailForEveryNewPost(true);
    groupMembership.setIsPublicized(true);
    groupMembership.setMembershipLevel(MembershipLevel.MEMBER);
    groupMembership.setWriteLevel(WriteLevel.DEFAULT);

    return groupMembership;
  }

  private static ComplexKeyGroupMembership buildComplexKeyGroupMembership(GroupMembershipKey id,
                                                                          String contactEmail,
                                                                          String firstName,
                                                                          String lastName)
  {
    ComplexKeyGroupMembership groupMembership = new ComplexKeyGroupMembership();
    groupMembership.setId(id);
    groupMembership.setContactEmail(contactEmail);
    groupMembership.setFirstName(firstName);
    groupMembership.setLastName(lastName);
    // These fields must be set when creating a stand-alone group membership,
    // lest the membership be considered incomplete.
    groupMembership.setAllowMessagesFromMembers(true);
    groupMembership.setEmailAnnouncementsFromManagers(true);
    groupMembership.setEmailDigestFrequency(EmailDigestFrequency.WEEKLY);
    groupMembership.setEmailForEveryNewPost(true);
    groupMembership.setIsPublicized(true);
    groupMembership.setMembershipLevel(MembershipLevel.MEMBER);
    groupMembership.setWriteLevel(WriteLevel.DEFAULT);

    return groupMembership;
  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationBatchCreateGetUpdatePatchDelete(RootBuilderWrapper<CompoundKey, GroupMembership> membershipBuilders)
    throws RemoteInvocationException
  {
    // Setup - batch create two group memberships
    CompoundKey key1 = buildCompoundKey(1, 1);
    CompoundKey key2 = buildCompoundKey(2, 1);
    GroupMembership groupMembership1 =
        buildGroupMembership(null, "alfred@test.linkedin.com", "Alfred", "Hitchcock");
    GroupMembership groupMembership2 =
        buildGroupMembership(null, "bruce@test.linkedin.com", "Bruce", "Willis");
    Map<CompoundKey, UpdateStatus> results = REST_CLIENT.sendRequest(membershipBuilders.batchUpdate()
                                                      .input(key1, groupMembership1)
                                                      .input(key2, groupMembership2)
                                                      .build()).getResponse().getEntity().getResults();
    Assert.assertEquals(results.get(key1).getStatus().intValue(), 204);
    Assert.assertEquals(results.get(key2).getStatus().intValue(), 204);

    // BatchGet memberships
    Request<BatchResponse<GroupMembership>> request = membershipBuilders.batchGet()
                                  .ids(key1, key2)
                                  .fields(GroupMembership.fields().contactEmail())
                                  .build();
    Map<String, GroupMembership> groupMemberships =
        REST_CLIENT.sendRequest(request).getResponse().getEntity().getResults();
    Assert.assertTrue(groupMemberships.containsKey(key1.toString()));
    Assert.assertEquals(groupMemberships.get(key1.toString()).getContactEmail(), "alfred@test.linkedin.com");
    Assert.assertTrue(groupMemberships.containsKey(key2.toString()));
    Assert.assertEquals(groupMemberships.get(key2.toString()).getContactEmail(), "bruce@test.linkedin.com");

    // Batch partial update
    GroupMembership patchedGroupMembership1 = buildGroupMembership(null, "ALFRED@test.linkedin.com", "ALFRED", "Hitchcock");
    GroupMembership patchedGroupMembership2 = buildGroupMembership(null, "BRUCE@test.linkedin.com", "BRUCE", "Willis");

    Map<CompoundKey, PatchRequest<GroupMembership>> patchInputs = new HashMap<CompoundKey, PatchRequest<GroupMembership>>();
    patchInputs.put(key1, PatchGenerator.diff(groupMembership1, patchedGroupMembership1));
    patchInputs.put(key2, PatchGenerator.diff(groupMembership2, patchedGroupMembership2));

    Map<CompoundKey, UpdateStatus> patchResults = REST_CLIENT.sendRequest(membershipBuilders
                                                                            .batchPartialUpdate()
                                                                            .patchInputs(patchInputs)
                                                                            .build())
      .getResponse().getEntity().getResults();
    Assert.assertEquals(patchResults.get(key1).getStatus().intValue(), 204);
    Assert.assertEquals(patchResults.get(key2).getStatus().intValue(), 204);

    // Batch get to make sure our patch applied
    BatchGetKVRequest<CompoundKey, GroupMembership> batchGetRequest =
        membershipBuilders.batchGet()
            .ids(key1, key2)
            .fields(GroupMembership.fields().contactEmail(), GroupMembership.fields().firstName())
            .buildKV();
    BatchKVResponse<CompoundKey, GroupMembership> entity =
        REST_CLIENT.sendRequest(batchGetRequest).getResponse().getEntity();
    Assert.assertEquals(entity.getErrors().size(), 0);
    Assert.assertEquals(entity.getResults().size(), 2);
    Assert.assertEquals(entity.getResults().get(key1).getContactEmail(), "ALFRED@test.linkedin.com");
    Assert.assertEquals(entity.getResults().get(key1).getFirstName(), "ALFRED");
    Assert.assertEquals(entity.getResults().get(key2).getContactEmail(), "BRUCE@test.linkedin.com");
    Assert.assertEquals(entity.getResults().get(key2).getFirstName(), "BRUCE");

    // GetAll memberships
    Request<CollectionResponse<GroupMembership>> getAllRequest = membershipBuilders.getAll().paginate(1, 2)
                                  .fields(GroupMembership.fields().contactEmail())
                                  .build();
    List<GroupMembership> elements =
        REST_CLIENT.sendRequest(getAllRequest).getResponse().getEntity().getElements();
    Assert.assertEquals(elements.size(), 1);

    // Delete the newly created group memberships
    Map<CompoundKey, UpdateStatus> deleteResult =
        REST_CLIENT.sendRequest(membershipBuilders.batchDelete()
                                                          .ids(key1, key2)
                                                          .build())
                   .getResponse()
                   .getEntity()
                   .getResults();
    Assert.assertEquals(deleteResult.get(key1).getStatus().intValue(), 204);
    Assert.assertEquals(deleteResult.get(key2).getStatus().intValue(), 204);

    // Make sure they are gone
    BatchResponse<GroupMembership> getResponse = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertTrue(getResponse.getResults().isEmpty());
    Assert.assertTrue(getResponse.getErrors().containsKey(key1.toString())); // Why .toString() here?
    Assert.assertTrue(getResponse.getErrors().containsKey(key2.toString()));
    Assert.assertEquals(getResponse.getErrors().get(key1.toString()).getStatus().intValue(), 404);
    Assert.assertEquals(getResponse.getErrors().get(key2.toString()).getStatus().intValue(), 404);
  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationCreateGetDelete(RootBuilderWrapper<CompoundKey, GroupMembership> membershipBuilders)
    throws RemoteInvocationException
  {
    // Setup - create group memberships
    CompoundKey key1 = buildCompoundKey(1, 1);
    GroupMembership groupMembership1 =
        buildGroupMembership(null, "alfred@test.linkedin.com", "Alfred", "Hitchcock");

    Response<EmptyRecord> response = REST_CLIENT.sendRequest(membershipBuilders.update().id(key1)
                                                      .input(groupMembership1)
                                                      .build()).getResponse();
    Assert.assertEquals(response.getStatus(), 204);

    // Get membership
    Request<GroupMembership> getRequest = membershipBuilders.get()
                                  .id(key1)
                                  .fields(GroupMembership.fields().contactEmail())
                                  .build();
    GroupMembership groupMembership = REST_CLIENT.sendRequest(getRequest).getResponse().getEntity();
    Assert.assertEquals(groupMembership.getContactEmail(), "alfred@test.linkedin.com");

    // Delete the newly created group membership
    Response<EmptyRecord> deleteResponse = REST_CLIENT.sendRequest(membershipBuilders.delete().id(key1).build()).getResponse();
    Assert.assertEquals(deleteResponse.getStatus(), 204);

    // Make sure it is gone
    try
    {
      REST_CLIENT.sendRequest(getRequest).getResponse();
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), 404);
    }
  }

  @Test(dataProvider = "requestComplexBuilderDataProvider")
  public void testComplexKeyCreateGetUpdateDelete(RootBuilderWrapper<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> builders)
    throws RemoteInvocationException
  {
    // Create a new complex key resource
    ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> complexKey =
        buildComplexKey(1, 1, 10, "String1");
    ComplexKeyGroupMembership groupMembership =
        buildComplexKeyGroupMembership(complexKey.getKey(),
                                       "alfred@test.linkedin.com",
                                       "alfred",
                                       "hitchcock");

    Request<EmptyRecord> createRequest = builders.create().input(groupMembership).build();
    Response<EmptyRecord> createResponse = REST_CLIENT.sendRequest(createRequest).getResponse();
    Assert.assertEquals(createResponse.getStatus(), 201);

    GroupMembershipParam param = new GroupMembershipParam();
    param.setIntParameter(1);
    param.setStringParameter("1");

    GroupMembershipQueryParam groupMembershipQueryParam1 = new GroupMembershipQueryParam();
    groupMembershipQueryParam1.setIntParameter(1);
    groupMembershipQueryParam1.setStringParameter("1");
    GroupMembershipQueryParam groupMembershipQueryParam2 = new GroupMembershipQueryParam();
    groupMembershipQueryParam2.setIntParameter(2);
    groupMembershipQueryParam2.setStringParameter("2");
    GroupMembershipQueryParamArray queryParamArray = new GroupMembershipQueryParamArray(Arrays.asList(groupMembershipQueryParam1, groupMembershipQueryParam2));
    // Get the resource back and check state
    Request<ComplexKeyGroupMembership> request = builders.get()
                                          .id(complexKey)
                                          .fields(GroupMembership.fields().contactEmail())
                                          .setQueryParam("testParam", param)
                                          .setQueryParam("testParamArray", queryParamArray)
                                          .build();
    ComplexKeyGroupMembership groupMembership1 = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertNotNull(groupMembership1);
    Assert.assertEquals(groupMembership1.getContactEmail(), "alfred@test.linkedin.com");

    // Test the same with optional complex parameters
    request = builders.get().id(complexKey).fields(GroupMembership.fields().contactEmail()).build();
    groupMembership1 = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertNotNull(groupMembership1);
    Assert.assertEquals(groupMembership1.getContactEmail(), "alfred@test.linkedin.com");

    // Update contact email and verify
    groupMembership.setContactEmail("alphred@test.linkedin.com");
    Request<EmptyRecord> updateRequest = builders.update().id(complexKey).input(groupMembership).build();
    Response<EmptyRecord> updateResponse = REST_CLIENT.sendRequest(updateRequest).getResponse();
    Assert.assertEquals(updateResponse.getStatus(), 204);

    groupMembership1 = REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Assert.assertEquals(groupMembership1.getContactEmail(), "alphred@test.linkedin.com");

    // Delete and verify
    Request<EmptyRecord> deleteRequest = builders.delete().id(complexKey).build();
    Response<EmptyRecord> deleteResponse = REST_CLIENT.sendRequest(deleteRequest).getResponse();
    Assert.assertEquals(deleteResponse.getStatus(), 204);
    try
    {
      REST_CLIENT.sendRequest(request).getResponse().getEntity();
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), 404);
    }
  }

  @Test(dataProvider = "requestComplexBuilderDataProvider")
  public void testComplexKeyBatchCreateGetUpdateDelete(RootBuilderWrapper<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership> builders)
    throws RemoteInvocationException
  {
    ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> complexKey1 =
        buildComplexKey(1, 1, 10, "String1");
    ComplexKeyGroupMembership groupMembership1 =
        buildComplexKeyGroupMembership(complexKey1.getKey(), "alfred@test.linkedin.com", "alfred", "hitchcock");
    ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> complexKey2 =
        buildComplexKey(2, 1, 20, "String2");
    ComplexKeyGroupMembership groupMembership2 =
        buildComplexKeyGroupMembership(complexKey2.getKey(), "bruce@test.linkedin.com", "bruce", "willis");
    ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> complexKey3 =
        buildComplexKey(3, 1, 30, "String3");
    ComplexKeyGroupMembership groupMembership3 =
        buildComplexKeyGroupMembership(complexKey3.getKey(),
                                       "carole@test.linkedin.com",
                                       "carole",
                                       "bouquet");

    Request<CollectionResponse<CreateStatus>> createRequest = builders.batchCreate()
                                          .input(groupMembership1)
                                          .input(groupMembership2)
                                          .input(groupMembership3)
                                          .build();
    Response<CollectionResponse<CreateStatus>> createResponse =
        REST_CLIENT.sendRequest(createRequest).getResponse();
    Assert.assertEquals(createResponse.getStatus(), 200);

    @SuppressWarnings("unchecked")
    Request<BatchResponse<ComplexKeyGroupMembership>> request = builders.batchGet()
                                          .ids(complexKey1, complexKey2, complexKey3)
                                          .fields(GroupMembership.fields().contactEmail())
                                          .build();
    BatchResponse<ComplexKeyGroupMembership> groupMemberships =
        REST_CLIENT.sendRequest(request).getResponse().getEntity();
    Map<String, ComplexKeyGroupMembership> results = groupMemberships.getResults();
    ComplexKeyGroupMembership groupMembership1_ = results.get(complexKey1.toString());
    ComplexKeyGroupMembership groupMembership2_ = results.get(complexKey2.toString());
    ComplexKeyGroupMembership groupMembership3_ = results.get(complexKey3.toString());
    Assert.assertNotNull(groupMembership1_);
    Assert.assertEquals(groupMembership1_.getContactEmail(), "alfred@test.linkedin.com");
    Assert.assertNotNull(groupMembership2_);
    Assert.assertEquals(groupMembership2_.getContactEmail(), "bruce@test.linkedin.com");
    Assert.assertNotNull(groupMembership3_);
    Assert.assertEquals(groupMembership3_.getContactEmail(), "carole@test.linkedin.com");

    // Update and verify
    groupMembership1.setContactEmail("alfred+@test.linkedin.com");
    groupMembership2.setContactEmail("bruce+@test.linkedin.com");
    groupMembership3.setContactEmail("carole+@test.linkedin.com");
    Request<BatchKVResponse<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, UpdateStatus>> updateRequest = builders.batchUpdate()
                                          .input(complexKey1, groupMembership1)
                                          .input(complexKey2, groupMembership2)
                                          .input(complexKey3, groupMembership3)
                                          .build();
    int status = REST_CLIENT.sendRequest(updateRequest).getResponse().getStatus();
    Assert.assertEquals(status, 200);
  }

  @Test(dataProvider = "requestMembershipsBuilderDataProvider")
  public void testAssociationBatchGetCompoundKeyResponse(RootBuilderWrapper<CompoundKey, GroupMembership> membershipBuilders)
    throws RemoteInvocationException
  {
    CompoundKey key1 = buildCompoundKey(1, 1);
    CompoundKey key2 = buildCompoundKey(2, 1);
    Set<CompoundKey> allRequestedKeys = new HashSet<CompoundKey>(Arrays.asList(key1, key2));

    BatchGetKVRequest<CompoundKey, GroupMembership> request = membershipBuilders.batchGet()
                    .ids(key1, key2)
                    .fields(GroupMembership.fields().contactEmail())
                    .buildKV();
    BatchKVResponse<CompoundKey, GroupMembership> groupMemberships = REST_CLIENT.sendRequest(request).getResponse().getEntity();

    Assert.assertTrue(allRequestedKeys.containsAll(groupMemberships.getResults().keySet()));
    Assert.assertTrue(allRequestedKeys.containsAll(groupMemberships.getErrors().keySet()));
    Set<CompoundKey> allResponseKeys = new HashSet<CompoundKey>(groupMemberships.getResults().keySet());
    allResponseKeys.addAll(groupMemberships.getErrors().keySet());
    Assert.assertEquals(allResponseKeys, allRequestedKeys);
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testDefaultValue(RootBuilderWrapper<Integer, Group> groupBuilders)
    throws RemoteInvocationException
  {
    // use server side default value for the RecordTemplate
    REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").build()).getResponse();

    try
    {
      // specifying an instance of the RecordTemplate which mismatches the default will fail the request
      final GroupMembershipParam newValue = new GroupMembershipParam();
      newValue.setIntParameter(0);
      newValue.setStringParameter("fail");
      REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").setQueryParam("record", newValue).build()).getResponse();
      Assert.fail("Expect exception when specifying the \"record\" query parameter different from the default");
    }
    catch (RestLiResponseException e)
    {
      Assert.assertEquals(e.getStatus(), 500);
    }
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testSimpleArrayParameter(RootBuilderWrapper<Integer, Group> groupBuilders)
    throws RemoteInvocationException
  {
    final Collection<Integer> coll = Arrays.asList(1, 2, 3);
    final IntegerArray array = new IntegerArray(coll);

    REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").setQueryParam("coercedArray", coll).build()).getResponse();
    REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").setQueryParam("coercedArray", array).build()).getResponse();
  }

  @Test(dataProvider = "requestGroupsBuilderDataProvider")
  public void testComplexArrayParameter(RootBuilderWrapper<Integer, Group> groupBuilders)
    throws RemoteInvocationException
  {
    final GroupMembershipParam elem = new GroupMembershipParam();
    elem.setIntParameter(7);
    elem.setStringParameter("success");

    final Collection<GroupMembershipParam> array = Arrays.asList(elem, elem);

    REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").setQueryParam("records", Arrays.asList(elem)).build()).getResponse();
    REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").setQueryParam("records", array).build()).getResponse();
    REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").setQueryParam("records", new GroupMembershipParamArray(array)).build()).getResponse();

    REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").addQueryParam("Records", elem).build()).getResponse();
    REST_CLIENT.sendRequest(groupBuilders.findBy("ComplexCircuit").addQueryParam("Records", elem).addQueryParam("Records", elem).build()).getResponse();
  }

  private static ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> buildComplexKey(int memberID,
                                                                                              int groupID,
                                                                                              int intParam,
                                                                                              String stringParam)
  {
    ComplexResourceKey<GroupMembershipKey, GroupMembershipParam> complexKey =
        new ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>(new GroupMembershipKey(),
                                                                         new GroupMembershipParam());
    complexKey.getKey().setMemberID(memberID);
    complexKey.getKey().setGroupID(groupID);
    complexKey.getParams().setIntParameter(intParam);
    complexKey.getParams().setStringParameter(stringParam);
    return complexKey;
  }

  private static CompoundKey buildCompoundKey(int memberID, int groupID)
  {
    CompoundKey compoundKey = new CompoundKey();
    compoundKey.append("memberID", memberID);
    compoundKey.append("groupID", groupID);
    return compoundKey;
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
  private static Object[][] requestMembershipsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()) },
      { new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestGroupsAndMembershipsBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()) },
      { new RootBuilderWrapper<Integer, Group>(new GroupsBuilders()), new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()) },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsBuilders()) },
      { new RootBuilderWrapper<Integer, Group>(new GroupsRequestBuilders()), new RootBuilderWrapper<CompoundKey, GroupMembership>(new GroupMembershipsRequestBuilders()) }
    };
  }

  @DataProvider
  private static Object[][] requestComplexBuilderDataProvider()
  {
    return new Object[][] {
      { new RootBuilderWrapper<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership>(new GroupMembershipsComplexBuilders()) },
      { new RootBuilderWrapper<ComplexResourceKey<GroupMembershipKey, GroupMembershipParam>, ComplexKeyGroupMembership>(new GroupMembershipsComplexRequestBuilders()) }
    };
  }
}

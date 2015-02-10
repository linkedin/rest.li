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

package com.linkedin.restli.server.test;


import com.google.common.collect.Sets;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.pegasus.generator.test.LongRef;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.ResourceType;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.combined.CombinedResources;
import com.linkedin.restli.server.combined.CombinedResources.CombinedAssociationResource;
import com.linkedin.restli.server.combined.CombinedResources.CombinedCollectionResource;
import com.linkedin.restli.server.combined.CombinedResources.CombinedCollectionWithSubresources;
import com.linkedin.restli.server.combined.CombinedResources.SubCollectionResource;
import com.linkedin.restli.server.combined.CombinedTestDataModels.Foo;
import com.linkedin.restli.server.invalid.InvalidActions;
import com.linkedin.restli.server.invalid.InvalidResources;
import com.linkedin.restli.server.twitter.AsyncDiscoveredItemsResource;
import com.linkedin.restli.server.twitter.AsyncFollowsAssociativeResource;
import com.linkedin.restli.server.twitter.AsyncStatusCollectionResource;
import com.linkedin.restli.server.twitter.ExceptionsResource;
import com.linkedin.restli.server.twitter.FollowsAssociativeResource;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import com.linkedin.restli.server.twitter.TwitterAccountsResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.DiscoveredItem;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Followed;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.StatusType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModel;
import static com.linkedin.restli.server.test.RestLiTestHelper.buildResourceModels;
import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;


/**
 * @author dellamag
 */
public class TestRestLiResourceModels
{
  @Test
  public void testTwitterStatusModel() throws Exception
  {
    ResourceModel statusResourceModel =
      buildResourceModel(StatusCollectionResource.class);

    checkCollectionModel(statusResourceModel,
                         "statuses", Long.class, "statusID", Status.class, false,
                         StatusCollectionResource.class);

    assertHasMethods(statusResourceModel,
                     ResourceMethod.CREATE, ResourceMethod.FINDER, ResourceMethod.BATCH_GET);

    ResourceMethodDescriptor methodDescriptor;
    methodDescriptor = checkFinderMethod(statusResourceModel, "public_timeline", 1);
    methodDescriptor = checkFinderMethod(statusResourceModel, "user_timeline", 2);

    methodDescriptor = checkFinderMethod(statusResourceModel, "search", 3);
    checkParam(methodDescriptor, "keywords", String.class, null);
    checkParam(methodDescriptor, "since", long.class, -1L);
    checkParam(methodDescriptor, "type", StatusType.class, null);
    assertNull(methodDescriptor.getParameter("foo"));

    assertNull(statusResourceModel.findActionMethod("foo", ResourceLevel.COLLECTION));

    assertNotNull(statusResourceModel.findMethod(ResourceMethod.BATCH_GET));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.CREATE));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.BATCH_CREATE));


    checkEntityModel(statusResourceModel,
                       Long.class, "statusID", Status.class,
                       Collections.<String, Class<?>>emptyMap());

    assertNotNull(statusResourceModel.findMethod(ResourceMethod.GET));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.FINDER));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.BATCH_UPDATE));
  }

  @Test
  public void testTwitterAsyncStatusModel() throws Exception
  {
    ResourceModel statusResourceModel =
      buildResourceModel(AsyncStatusCollectionResource.class);

    checkCollectionModel(statusResourceModel,
                         "asyncstatuses", Long.class, "statusID", Status.class, false,
                         AsyncStatusCollectionResource.class);

    assertHasMethods(statusResourceModel,
                     ResourceMethod.CREATE, ResourceMethod.FINDER, ResourceMethod.BATCH_GET);

    ResourceMethodDescriptor methodDescriptor;
    methodDescriptor = checkFinderMethod(statusResourceModel, "public_timeline", 2);
    methodDescriptor = checkFinderMethod(statusResourceModel, "user_timeline", 3);

    methodDescriptor = checkFinderMethod(statusResourceModel, "search", 5);
    checkParam(methodDescriptor, "keywords", String.class, null);
    checkParam(methodDescriptor, "since", long.class, -1L);
    checkParam(methodDescriptor, "type", StatusType.class, null);
    assertNull(methodDescriptor.getParameter("foo"));

    assertNull(statusResourceModel.findActionMethod("foo", ResourceLevel.COLLECTION));

    assertNotNull(statusResourceModel.findMethod(ResourceMethod.BATCH_GET));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.CREATE));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.BATCH_CREATE));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.BATCH_DELETE));


    checkEntityModel(statusResourceModel,
                       Long.class, "statusID", Status.class,
                       Collections.<String, Class<?>>emptyMap());

    assertNotNull(statusResourceModel.findMethod(ResourceMethod.GET));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.PARTIAL_UPDATE));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.FINDER));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.BATCH_UPDATE));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE));
    assertNotNull(statusResourceModel.findMethod(ResourceMethod.GET_ALL));
  }


  @Test
  public void testTwitterFollowsModel() throws Exception
  {
    ResourceModel followsModel = buildResourceModel(FollowsAssociativeResource.class);

    checkAssociationModel(followsModel,
                          "follows",
                          Sets.<String>newHashSet("followerID", "followeeID"),
                          Followed.class,
                          false,
                          FollowsAssociativeResource.class,
                          FollowsAssociativeResource.class);


    assertNotNull(followsModel.findMethod(ResourceMethod.BATCH_GET));
    assertNotNull(followsModel.findMethod(ResourceMethod.PARTIAL_UPDATE));
    assertNull(followsModel.findMethod(ResourceMethod.DELETE));
  }

  @Test
  public void testTwitterFollowsAsyncModel() throws Exception
  {
    ResourceModel followsModel = buildResourceModel(AsyncFollowsAssociativeResource.class);

    checkAssociationModel(followsModel,
                          "asyncfollows",
                          Sets.<String>newHashSet("followerID", "followeeID"),
                          Followed.class,
                          false,
                          AsyncFollowsAssociativeResource.class,
                          AsyncFollowsAssociativeResource.class);

    assertNotNull(followsModel.findMethod(ResourceMethod.PARTIAL_UPDATE));
    assertNotNull(followsModel.findMethod(ResourceMethod.UPDATE));
    assertNotNull(followsModel.findMethod(ResourceMethod.GET));
    assertNotNull(followsModel.findMethod(ResourceMethod.BATCH_DELETE));
    assertNotNull(followsModel.findMethod(ResourceMethod.BATCH_GET));
    assertNotNull(followsModel.findMethod(ResourceMethod.BATCH_UPDATE));
    assertNotNull(followsModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE));

    assertNull(followsModel.findMethod(ResourceMethod.DELETE));
    assertNull(followsModel.findMethod(ResourceMethod.CREATE));
    assertNull(followsModel.findMethod(ResourceMethod.BATCH_CREATE));
  }

  @Test
  public void testInvalidSingleAssociation() throws Exception
  {
    expectConfigException(InvalidResources.SingleAssociation.class, "requires more than 1 key");
  }

  @Test
  public void testInvalidFinders() throws Exception
  {
    expectConfigException(InvalidResources.FinderNonListReturnType.class, "has an unsupported return type");
    expectConfigException(InvalidResources.FinderNonRecordTemplateReturnType.class, "but found List<");
    expectConfigException(InvalidResources.FinderNonMatchingRecordTemplateReturnType.class, "but found List<");
    expectConfigException(InvalidResources.FinderNonMatchingRecordTemplateReturnTypeCollectionResult.class, "but found CollectionResult<");
    expectConfigException(InvalidResources.FinderInvalidParameters.class, "type is invalid or requires typeref");
    expectConfigException(InvalidResources.FinderInvalidParameters2.class, "type is invalid or requires typeref");
    expectConfigException(InvalidResources.FinderUnannotatedParameters.class, "must annotate each parameter");
    expectConfigException(InvalidResources.FinderTwoDefaultsInOneClass.class, "duplicate @Finder");
    expectConfigException(InvalidResources.FinderTwoNamedInOneClass.class, "duplicate @Finder");
    expectConfigException(InvalidResources.FinderNonExistingAssocKey.class, "Non-existing assocKey");
    expectConfigException(InvalidResources.GetAllNonExistingAssocKey.class, "Non-existing assocKey");
  }

  @Test
  public void testInvalidCustomObjectQueryParams() throws Exception
  {
    expectConfigException(InvalidResources.FinderIncompatibleCustomObjectParameters.class, "is not compatible with");
  }

  @Test
  public void testInvalidCrud() throws Exception
  {
    expectConfigException(InvalidResources.DuplicateGetMethod.class, "duplicate methods of type 'get'");
  }

  @Test
  public void testInvalidMethodsOnSimpleResource() throws Exception
  {
    expectConfigException(InvalidResources.SimpleResourceWithInvalidMethodTypes.class,
                          "is a simple resource but it contains a method of type");
  }

  @Test
  public void testSimpleResourceWithInvalidAction() throws Exception
  {
    expectConfigException(InvalidResources.SimpleResourceWithInvalidAction.class,
                          "is a simple resource, it cannot contain actions at resource level \"COLLECTION\"");
  }

  @Test
  public void testSimpleResourceTemplateWithAllMethods() throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.SimpleResourceAllMethods.class);
    checkSimpleResourceModel(model, "test", Foo.class, false, CombinedResources.SimpleResourceAllMethods.class);
    checkEntityModel(model, null, null, Foo.class, Collections.<String, Class<?>>emptyMap());
    assertHasMethods(model,
                     ResourceMethod.GET,
                     ResourceMethod.UPDATE,
                     ResourceMethod.PARTIAL_UPDATE,
                     ResourceMethod.DELETE,
                     ResourceMethod.ACTION);
  }

  @Test
  public void testActionsResource() throws Exception
  {
    ResourceModel resourceModel = buildResourceModel(TwitterAccountsResource.class);
    assertEquals(resourceModel.getResourceType(), ResourceType.ACTIONS);

    assertEquals(resourceModel.getResourceMethodDescriptors().size(), 5);

    ResourceMethodDescriptor methodDescriptor = resourceModel.findActionMethod("register", ResourceLevel.COLLECTION);
    assertNotNull(methodDescriptor);
    assertEquals(methodDescriptor.getActionName(), "register");
    assertNull(methodDescriptor.getFinderName());
    assertNotNull(methodDescriptor.getMethod());

    Parameter<?> firstParam = methodDescriptor.getParameter("first");
    assertNotNull(firstParam);
    assertEquals(firstParam.getName(), "first");
    assertEquals(firstParam.getType(), String.class);
    assertFalse(firstParam.isOptional());
    assertFalse(firstParam.hasDefaultValue());
    assertNull(firstParam.getDefaultValue());

    Parameter<?> marketingParam = methodDescriptor.getParameter("openToMarketingEmails");
    assertTrue(marketingParam.isOptional());
    assertTrue(marketingParam.hasDefaultValue());
    assertEquals(marketingParam.getDefaultValue(), true);

    methodDescriptor = resourceModel.findActionMethod("closeAccounts", ResourceLevel.COLLECTION);
    Parameter<?> optionsParam  = methodDescriptor.getParameter("options");
    assertTrue(optionsParam.isOptional());
    assertFalse(optionsParam.hasDefaultValue());
    assertNull(optionsParam.getDefaultValue());
  }

  @Test
  public void testActionResourceDisambiguation() throws Exception
  {
    ResourceModel collectionModel = buildResourceModel(StatusCollectionResource.class);
    assertEquals(collectionModel.getResourceType(), ResourceType.COLLECTION);

    assertEquals(0, countActions(collectionModel, ResourceLevel.COLLECTION));
    assertEquals(1, countActions(collectionModel, ResourceLevel.ENTITY));
    assertNotNull(collectionModel.findActionMethod("forward", ResourceLevel.ENTITY));
  }

  @Test
  public void testCombinedResourceClasses() throws Exception
  {
    // #1 collection
    ResourceModel collectionModel = buildResourceModel(CombinedResources.CombinedCollectionResource.class);
    checkCollectionModel(collectionModel, "test", String.class, "testId", Foo.class, false, CombinedCollectionResource.class);

    checkEntityModel(collectionModel, String.class, "testId", Foo.class, Collections.<String, Class<?>>emptyMap());

    // #2 simple resource
    ResourceModel simpleResourceModel = buildResourceModel(CombinedResources.CombinedSimpleResource.class);
    checkSimpleResourceModel(simpleResourceModel,
                             "test",
                             Foo.class,
                             false,
                             CombinedResources.CombinedSimpleResource.class);

    checkEntityModel(simpleResourceModel, null, null, Foo.class, Collections.<String, Class<?>>emptyMap());

    // #3 collection with sub-collection and sub simple resource
    Map<String, ResourceModel> modelMap = buildResourceModels(CombinedResources.CombinedCollectionWithSubresources.class,
                                                                  CombinedResources.SubCollectionResource.class,
                                                                  CombinedResources.SubsimpleResource.class);
    collectionModel = modelMap.get("/test");
    checkCollectionModel(collectionModel, "test", String.class, "testId", Foo.class, false, CombinedCollectionWithSubresources.class);

    checkEntityModel(collectionModel, String.class, "testId", Foo.class, Collections.<String, Class<?>>emptyMap());

    ResourceModel subCollectionModel = collectionModel.getSubResource("sub");
    assertNotNull(subCollectionModel);
    checkCollectionModel(subCollectionModel, "sub", String.class, "subId", Foo.class, true, SubCollectionResource.class);

    checkEntityModel(subCollectionModel, String.class, "subId", Foo.class, Collections.<String, Class<?>>emptyMap());

    ResourceModel subCollectionModel2 = collectionModel.getSubResource("sub2");
    assertNotNull(subCollectionModel2);
    checkSimpleResourceModel(subCollectionModel2, "sub2", Foo.class, true, CombinedResources.SubsimpleResource.class);

    checkEntityModel(subCollectionModel2, null, null, Foo.class, Collections.<String, Class<?>>emptyMap());

    // #4 simple resource with sub collection and sub simple resource
    Map<String, ResourceModel> modelMap2 =
        buildResourceModels(CombinedResources.CombinedSimpleResourceWithSubresources.class,
                            CombinedResources.SubCollectionOfSimpleResource.class,
                            CombinedResources.SubsimpleResourceOfSimpleResource.class);
    simpleResourceModel = modelMap2.get("/test");
    checkSimpleResourceModel(simpleResourceModel,
                             "test",
                             Foo.class,
                             false,
                             CombinedResources.CombinedSimpleResourceWithSubresources.class);

    checkEntityModel(simpleResourceModel, null, null, Foo.class, Collections.<String, Class<?>>emptyMap());

    subCollectionModel = simpleResourceModel.getSubResource("sub");
    assertNotNull(subCollectionModel);
    checkCollectionModel(subCollectionModel,
                         "sub",
                         String.class,
                         "subId",
                         Foo.class,
                         true,
                         CombinedResources.SubCollectionOfSimpleResource.class);

    checkEntityModel(subCollectionModel, String.class, "subId", Foo.class, Collections.<String, Class<?>>emptyMap());

    subCollectionModel2 = simpleResourceModel.getSubResource("sub2");
    assertNotNull(subCollectionModel2);
    checkSimpleResourceModel(subCollectionModel2,
                             "sub2",
                             Foo.class,
                             true,
                             CombinedResources.SubsimpleResourceOfSimpleResource.class);

    checkEntityModel(subCollectionModel2, null, null, Foo.class, Collections.<String, Class<?>>emptyMap());

    // #5 association
    ResourceModel assocModel = buildResourceModel(CombinedResources.CombinedAssociationResource.class);
    checkAssociationModel(assocModel, "test", Sets.newHashSet("foo", "bar"), Foo.class, false,
                          CombinedAssociationResource.class, CombinedAssociationResource.class);

    checkEntityModel(assocModel, CompoundKey.class, "id", Foo.class,
                     Collections.<String, Class<?>>emptyMap());

  }

  @Test
  public void testAnnotatedCrudMethods() throws Exception
  {
    // #1 Verify collection with annotated CRUD methods.
    ResourceModel collectionModelAnnotatedCrud = buildResourceModel(CombinedResources.CollectionWithAnnotatedCrudMethods.class);
    checkCollectionModel(collectionModelAnnotatedCrud, "test", String.class, "testId", Foo.class, false,
                         CombinedResources.CollectionWithAnnotatedCrudMethods.class);
    checkEntityModel(collectionModelAnnotatedCrud, String.class, "testId", Foo.class, Collections.<String, Class<?>>emptyMap());
    assertHasMethods(collectionModelAnnotatedCrud,
                     ResourceMethod.GET,
                     ResourceMethod.CREATE,
                     ResourceMethod.UPDATE,
                     ResourceMethod.PARTIAL_UPDATE,
                     ResourceMethod.DELETE,
                     ResourceMethod.BATCH_GET,
                     ResourceMethod.BATCH_CREATE,
                     ResourceMethod.BATCH_DELETE,
                     ResourceMethod.BATCH_UPDATE,
                     ResourceMethod.BATCH_PARTIAL_UPDATE);

    // #2 Verify simple resource with annotated CRUD methods.
    ResourceModel simpleResourceModelAnnotatedCrud = buildResourceModel(CombinedResources.SimpleResourceWithAnnotatedCrudMethods.class);
    checkSimpleResourceModel(simpleResourceModelAnnotatedCrud,
                             "test",
                             Foo.class,
                             false,
                             CombinedResources.SimpleResourceWithAnnotatedCrudMethods.class);
    checkEntityModel(simpleResourceModelAnnotatedCrud, null, null, Foo.class, Collections.<String, Class<?>>emptyMap());
    assertHasMethods(simpleResourceModelAnnotatedCrud,
                     ResourceMethod.GET,
                     ResourceMethod.UPDATE,
                     ResourceMethod.PARTIAL_UPDATE,
                     ResourceMethod.DELETE,
                     ResourceMethod.ACTION);

    // #3 Verify complex key collection with annotated CRUD methods.
    ResourceModel complexKeyCollectionModelAnnotatedCrud = buildResourceModel(
        CombinedResources.ComplexKeyResourceWithAnnotatedCrudMethods.class);
    checkCollectionModel(complexKeyCollectionModelAnnotatedCrud, "test",
                         ComplexResourceKey.class,
                         "testId", Foo.class, false,
                         CombinedResources.ComplexKeyResourceWithAnnotatedCrudMethods.class);
    checkEntityModel(complexKeyCollectionModelAnnotatedCrud,
                     ComplexResourceKey.class,
                     "testId",
                     Foo.class,
                     Collections.<String, Class<?>>emptyMap());

    assertHasMethods(complexKeyCollectionModelAnnotatedCrud,
                     ResourceMethod.GET,
                     ResourceMethod.CREATE,
                     ResourceMethod.UPDATE,
                     ResourceMethod.PARTIAL_UPDATE,
                     ResourceMethod.DELETE,
                     ResourceMethod.BATCH_GET,
                     ResourceMethod.BATCH_CREATE,
                     ResourceMethod.BATCH_DELETE,
                     ResourceMethod.BATCH_UPDATE,
                     ResourceMethod.BATCH_PARTIAL_UPDATE);
  }

  @Test
  public void testCustomCrudParams() throws Exception
  {
    ResourceModel model = buildResourceModel(CombinedResources.CollectionWithCustomCrudParams.class);
    checkCollectionModel(model, "test", String.class, "testId", Foo.class, false,
                         CombinedResources.CollectionWithCustomCrudParams.class);
    checkEntityModel(model, String.class, "testId", Foo.class, Collections.<String, Class<?>>emptyMap());

    ResourceMethod[] crudMethods = {
            ResourceMethod.GET,
            ResourceMethod.CREATE,
            ResourceMethod.UPDATE,
            ResourceMethod.PARTIAL_UPDATE,
            ResourceMethod.DELETE,
            ResourceMethod.BATCH_GET,
            ResourceMethod.BATCH_CREATE,
            ResourceMethod.BATCH_DELETE,
            ResourceMethod.BATCH_UPDATE,
            ResourceMethod.BATCH_PARTIAL_UPDATE
    };

    assertHasMethods(model, crudMethods);

    for (ResourceMethod method : crudMethods)
    {
      ResourceMethodDescriptor descriptor = model.findMethod(method);
      List<Parameter<?>> params = descriptor.getParameters();
      boolean foundIntParam = false;
      boolean foundStringParam = false;
      for (Parameter<?> param : params)
      {
        if (param.getName().equals("intParam"))
        {
          foundIntParam = true;
          Assert.assertEquals(param.getType(), int.class);
          Assert.assertTrue(param.isOptional());
          Assert.assertEquals(param.getDefaultValue(), Integer.valueOf(42));
        }
        else if (param.getName().equals("stringParam"))
        {
          foundStringParam = true;
          Assert.assertEquals(param.getType(), String.class);
          Assert.assertFalse(param.isOptional());
          Assert.assertFalse(param.hasDefaultValue());
        }
      }
      Assert.assertTrue(foundIntParam);
      Assert.assertTrue(foundStringParam);
    }
  }

  @Test
  public void testCollectionComplexKey()
  {
    ResourceModel model = buildResourceModel(CombinedResources.CombinedComplexKeyResource.class);
    checkCollectionModel(model, "complexKeyCollection", ComplexResourceKey.class, "complexKeyCollectionId", Foo.class, false,
                         CombinedResources.CombinedComplexKeyResource.class);
  }

  @Test
  public void testInvalidActions() throws Exception
  {
    expectConfigException(InvalidActions.ActionUnannotatedParameters.class, "must annotate each parameter");
    expectConfigException(InvalidActions.ActionInvalidParameterTypes.class, "type is invalid or requires typeref");
    expectConfigException(InvalidActions.ActionInvalidReturnType.class, "invalid return type");
    expectConfigException(InvalidActions.ActionNameConflict.class, "Found duplicate");
    expectConfigException(InvalidActions.ActionInvalidReturnType2.class, "invalid return type");
    expectConfigException(InvalidActions.ActionInvalidBytesParam.class, "type is invalid or requires typeref");
  }

  @Test
  public void testExceptionMethods() throws Exception
  {
    final ResourceModel resourceModel = buildResourceModel(ExceptionsResource.class);
    assertEquals(resourceModel.getResourceType(), ResourceType.COLLECTION);

    assertEquals(resourceModel.getResourceMethodDescriptors().size(), 2);

    final ResourceMethodDescriptor getMethod = resourceModel.findMethod(ResourceMethod.GET);
    assertNotNull(getMethod);

    final ResourceMethodDescriptor actionMethod = resourceModel.findActionMethod("exception", ResourceLevel.COLLECTION);
    assertNotNull(actionMethod);
    final Class<?> returnClass = actionMethod.getActionReturnType();
    assertSame(returnClass, Integer.class);
  }

  @Test
  public void testInvalidComplexKeyInCollectionResourceTemplate() throws Exception
  {
    expectConfigException(InvalidResources.ComplexKeyInCollectionResourceTemplate.class,
                          "' should implement 'ComplexKeyResource' as a complex key '");
  }

  @Test
  public void testAsyncDiscoveredItemsResource()
  {
    ResourceModel asyncDiscoveredModel = buildResourceModel(AsyncDiscoveredItemsResource.class);

    // TODO more extensive check on the key rather than just checking if it is an instance of ComplexResourceKey
    checkCollectionModel(asyncDiscoveredModel,
                         "asyncdiscovereditems",
                         ComplexResourceKey.class,
                         "asyncDiscoveredItemId",
                         DiscoveredItem.class,
                         false,
                         AsyncDiscoveredItemsResource.class);

    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.GET));
    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.CREATE));
    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.DELETE));
    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.UPDATE));
    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.PARTIAL_UPDATE));

    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.BATCH_GET));
    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.BATCH_CREATE));
    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.BATCH_UPDATE));
    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.BATCH_PARTIAL_UPDATE));
    assertNotNull(asyncDiscoveredModel.findMethod(ResourceMethod.GET_ALL));
  }

  @Test
  public void testTyperefKey()
  {
    expectConfigException(InvalidResources.TyperefKeyResource.class, "Typeref '" + LongRef.class.getName() + "' cannot be key type for class '" + InvalidResources.TyperefKeyResource.class.getName() + "'.");
    expectConfigException(InvalidResources.TyperefKeyCollection.class, "Typeref '" + LongRef.class.getName() + "' cannot be key type for class '" + InvalidResources.TyperefKeyCollection.class.getName() + "'.");

  }

  @Test
  public void testValidRestLiDataAnnotations()
  {
    ResourceModel resourceModel = buildResourceModel(CombinedResources.DataAnnotationTestResource.class);
    DataMap annotations = resourceModel.getCustomAnnotationData();
    Assert.assertEquals(((DataMap) annotations.get("readOnly")).get("value"), new DataList(Arrays.asList("intField", "longField")));
    Assert.assertEquals(((DataMap) annotations.get("createOnly")).get("value"), new DataList(Arrays.asList("floatField")));
  }

  @Test
  public void testInvalidRestLiDataAnnotations()
  {
    expectConfigException(InvalidResources.DataAnnotationOnNonexistentField.class, "CreateOnly annotation asdf is not a valid path for TestRecord");
    expectConfigException(InvalidResources.DuplicateDataAnnotation.class, "mapA is marked as ReadOnly multiple times");
    expectConfigException(InvalidResources.RedundantDataAnnotation1.class, "mapA/intField is marked as CreateOnly, but is contained in a CreateOnly field mapA");
    expectConfigException(InvalidResources.RedundantDataAnnotation2.class, "mapA is marked as both ReadOnly and CreateOnly");
    expectConfigException(InvalidResources.RedundantDataAnnotation3.class, "mapA/doubleField is marked as CreateOnly, but is contained in a ReadOnly field mapA");
    expectConfigException(InvalidResources.RedundantDataAnnotation4.class, "mapA/doubleField is marked as ReadOnly, but is contained in a CreateOnly field mapA");
  }

  // ************************
  // Helper methods
  // ************************

  private int countActions(ResourceModel resourceModel, ResourceLevel resourceLevel)
  {
    int numActions = 0;
    for (ResourceMethodDescriptor method : resourceModel.getResourceMethodDescriptors())
    {
      if (method.getType().equals(ResourceMethod.ACTION) && method.getActionResourceLevel().equals(resourceLevel))
      {
        numActions++;
      }
    }
    return numActions;
  }

  private void expectConfigException(Class<?> resourceClass, String expectedMessageSubstring)
  {
    try
    {
      buildResourceModel(resourceClass);
      fail("expected ResourceConfigException with message '" + expectedMessageSubstring + "' for class " + resourceClass.getName());
    }
    catch (ResourceConfigException e)
    {
      assertTrue(e.getMessage().contains(expectedMessageSubstring),
                 "expected ResourceConfigException with message '" + expectedMessageSubstring + "' for class " + resourceClass.getName() +
                 ", but was '" + e.getMessage() + "'");
    }
  }


  private void checkAssociationModel(ResourceModel model,
                                     String name,
                                     Set<String> assocKeyNames,
                                     Class<? extends RecordTemplate> valueClass,
                                     boolean hasParentResource,
                                     Class<?> associationResourceClass,
                                     Class<?> entityResourceClass)
  {
    assertNotNull(model);
    checkBaseModel(model,
                   name,
                   CompoundKey.class,
                   valueClass,
                   hasParentResource,
                   entityResourceClass);

    assertEquals(model.getKeyNames(), assocKeyNames);
  }


  private void checkEntityModel(ResourceModel model,
                                Class<?> keyClass,
                                String keyName,
                                Class<? extends RecordTemplate> valueClass,
                                Map<String, Class<?>> subResourceMap)
  {
    assertNotNull(model);
    assertEquals(model.getKeyClass(), keyClass);
    assertEquals(model.getValueClass(), valueClass);
    if (model.getParentResourceModel()==null)
    {
      assertTrue(model.isRoot());
    }

    for (String subResourcePath : subResourceMap.keySet())
    {
      ResourceModel subResourceModel = model.getSubResource(subResourcePath);
      assertNotNull(subResourceModel);
      assertEquals(subResourceModel.getResourceClass(), subResourceMap.get(subResourcePath));
    }
  }

  private void checkCollectionModel(ResourceModel model,
                                    String name,
                                    Class<?> keyClass,
                                    String keyName,
                                    Class<? extends RecordTemplate> valueClass,
                                    boolean hasParentResource,
                                    Class<?> collectionResourceClass)
  {
    assertNotNull(model);
    assertEquals(model.getResourceType(), ResourceType.COLLECTION);
    checkBaseModel(model, name, keyClass, valueClass, hasParentResource, collectionResourceClass);

    assertEquals(model.getKeyName(), keyName, "Wrong key name");
  }

   private void checkBaseModel(ResourceModel model,
                              String name,
                              Class<?> keyClass,
                              Class<? extends RecordTemplate> valueClass,
                              boolean hasParentResource,
                              Class<?> resourceClass)
  {
    assertNotNull(model);
    assertEquals(model.getName(), name);
    assertEquals(model.getKeyClass(), keyClass);
    assertEquals(model.getValueClass(), valueClass);
    assertTrue(!(hasParentResource ^ model.getParentResourceModel() != null));
    if (!hasParentResource)
    {
      assertTrue(model.isRoot());
    }
    assertEquals(model.getResourceClass(), resourceClass);
  }

  private void checkSimpleResourceModel(ResourceModel model,
                                        String name,
                                        Class<? extends RecordTemplate> valueClass,
                                        boolean hasParentResource,
                                        Class<?> resourceClass)
  {
    assertNotNull(model);
    assertEquals(model.getResourceType(), ResourceType.SIMPLE);
    checkBaseModel(model, name, null/*keyClass*/, valueClass, hasParentResource, resourceClass);
  }

  private void assertHasMethods(ResourceModel model,
                                ResourceMethod... types)
  {
    for (ResourceMethod type : types)
    {
      assertNotNull(model.findMethod(type), "ResourceMethod '" + type.toString() + "' should be present");
    }
  }

  private ResourceMethodDescriptor checkFinderMethod(ResourceModel model,
                                           String finderName,
                                           int numParameters)
  {
    ResourceMethodDescriptor methodDescriptor = model.findNamedMethod(finderName);
    assertNotNull(methodDescriptor);
    assertNull(methodDescriptor.getActionName());
    assertEquals(finderName, methodDescriptor.getFinderName());
    assertNotNull(methodDescriptor.getMethod());
    assertEquals(methodDescriptor.getParameters().size(), numParameters);
    assertEquals(methodDescriptor.getResourceModel().getResourceClass().getClass(), model.getResourceClass().getClass());
    assertEquals(methodDescriptor.getType(), ResourceMethod.FINDER);

    return methodDescriptor;
  }
  private <T> Parameter<T> checkParam(ResourceMethodDescriptor methodDescriptor,
                                      String name,
                                      Class<T> type,
                                      Object defaultValue)
  {
    Parameter<T> param = methodDescriptor.getParameter(name);
    assertNotNull(param);
    assertEquals(param.getName(), name);
    assertEquals(param.getType(), type);
    assertTrue(!(param.hasDefaultValue() ^ defaultValue != null));

    return param;
  }


}

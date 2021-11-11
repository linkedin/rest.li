/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.model;

import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.LongDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.StringDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.TyperefInfo;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.Link;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.PathKeyParam;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.restli.internal.server.model.SampleResources.*;


public class TestRestLiAnnotationReader
{

  @Test(description = "verifies actions resource method annotations for required and optional actions param")
  public void actionRootResource()
  {
    final String expectedNamespace = "com.linkedin.model.actions";
    final String expectedName = "actionsMethod";

    final ResourceModel model = RestLiAnnotationReader.processResource(ActionsOnlyResource.class);

    Assert.assertNotNull(model);

    Assert.assertTrue(model.isRoot());
    Assert.assertEquals(expectedName, model.getName());
    Assert.assertEquals(expectedNamespace, model.getNamespace());

    Assert.assertNull(model.getParentResourceClass());
    Assert.assertNull(model.getParentResourceModel());

    // keys
    Assert.assertEquals(0, model.getKeys().size());
    Assert.assertEquals(0, model.getKeyNames().size());
    Assert.assertEquals(0, model.getKeyClasses().size());

    // primary key
    Assert.assertNull(model.getPrimaryKey());

    // alternative key
    Assert.assertTrue(model.getAlternativeKeys().isEmpty());

    // model
    Assert.assertNull(model.getValueClass());

    final Map<String, ResourceMethodDescriptor> nameToDescriptor = model.getResourceMethodDescriptors()
        .stream()
        .collect(Collectors.toMap(ResourceMethodDescriptor::getMethodName, Function.identity()));

    // method: sum
    final ResourceMethodDescriptor method = nameToDescriptor.get("addValues");
    Assert.assertNotNull(method);

    final Action methodParam = method.getAnnotation(Action.class);
    Assert.assertNotNull(methodParam);
    Assert.assertEquals("addValues", methodParam.name());

    final Map<String, Parameter<?>> methodParams = method.getParameters()
        .stream()
        .collect(Collectors.toMap(Parameter::getName, Function.identity()));

    Assert.assertEquals(2, methodParams.size());

    final AnnotationSet leftParamAnnotations = methodParams.get("left").getAnnotations();
    Assert.assertNotNull(leftParamAnnotations);
    Assert.assertTrue(leftParamAnnotations.contains(ActionParam.class));

    final AnnotationSet rightParamAnnotations = methodParams.get("right").getAnnotations();
    Assert.assertNotNull(rightParamAnnotations);
    Assert.assertTrue(rightParamAnnotations.contains(ActionParam.class));
    Assert.assertTrue(rightParamAnnotations.contains(Optional.class));
    Assert.assertEquals("55", rightParamAnnotations.get(Optional.class).value());
  }

  @Test(description = "verifies return types of action resource methods", dataProvider = "actionReturnTypeData")
  public void actionResourceMethodReturnTypes(final Class<?> resourceClass, final Class<?> expectedActionReturnType)
  {
    final ResourceModel model = RestLiAnnotationReader.processResource(resourceClass);
    Assert.assertNotNull(model);

    for (final ResourceMethodDescriptor methodDescriptor : model.getResourceMethodDescriptors())
    {
      final Class<?> expectedReturnType = methodDescriptor.getActionReturnType();
      Assert.assertEquals(expectedReturnType, expectedActionReturnType);
    }
  }

  @Test(description = "verifies that custom method level annotations and members are processed correctly")
  public void collectionRootResourceWithCustomMethodAnnotation()
  {
    final String expectedNamespace = "";

    final String expectedName = "customAnnotatedMethod";
    final Class<? extends RecordTemplate> expectedValueClass = EmptyRecord.class;

    final String expectedKeyName = "customAnnotatedMethodId";
    final Class<?> expectedKeyClass = Long.class;

    final ResourceModel model = RestLiAnnotationReader.processResource(CustomAnnotatedMethodResource.class);

    Assert.assertNotNull(model);

    Assert.assertTrue(model.isRoot());
    Assert.assertEquals(expectedName, model.getName());
    Assert.assertEquals(expectedNamespace, model.getNamespace());

    Assert.assertNull(model.getParentResourceClass());
    Assert.assertNull(model.getParentResourceModel());

    // keys
    Assert.assertEquals(1, model.getKeys().size());
    Assert.assertEquals(1, model.getKeyNames().size());
    Assert.assertEquals(1, model.getKeyClasses().size());
    Assert.assertEquals(expectedKeyName, model.getKeyName());
    Assert.assertEquals(expectedKeyName, model.getKeyNames().iterator().next());
    Assert.assertEquals(expectedKeyClass, model.getKeyClasses().get(expectedKeyName));

    // primary key
    Assert.assertNotNull(model.getPrimaryKey());
    Assert.assertNotNull(expectedKeyName, model.getPrimaryKey().getName());
    Assert.assertEquals(expectedKeyClass, model.getPrimaryKey().getType());
    Assert.assertEquals(model.getPrimaryKey(), model.getKeys().iterator().next());
    Assert.assertTrue(model.getPrimaryKey().getDataSchema() instanceof LongDataSchema);

    // alternative key
    Assert.assertTrue(model.getAlternativeKeys().isEmpty());

    // model
    Assert.assertNotNull(model.getValueClass());
    Assert.assertEquals(expectedValueClass, model.getValueClass());

    // custom method annotation
    Assert.assertEquals(model.getResourceMethodDescriptors().size(), 1);
    final ResourceMethodDescriptor getMethod = model.getResourceMethodDescriptors().get(0);

    Assert.assertNotNull(getMethod.getCustomAnnotationData());
    Assert.assertTrue(getMethod.getCustomAnnotationData().size() > 0);
    final DataMap versionAnnotation = getMethod.getCustomAnnotationData();

    Assert.assertTrue(versionAnnotation.get("Versioned") instanceof DataMap);
    final DataMap versionAnnotationFields = versionAnnotation.getDataMap("Versioned");

    Assert.assertNotNull(versionAnnotationFields.get("fromVersion"));
    Assert.assertTrue(versionAnnotationFields.get("fromVersion") instanceof Integer);
    Assert.assertEquals((int) versionAnnotationFields.getInteger("fromVersion"), 10);

    Assert.assertNotNull(versionAnnotationFields.get("toVersion"));
    Assert.assertTrue(versionAnnotationFields.get("toVersion") instanceof Integer);
    Assert.assertEquals((int) versionAnnotationFields.getInteger("toVersion"), Integer.MAX_VALUE);
  }

  @Test(description = "verifies collection resource for keys and value class")
  public void collectionRootResource()
  {
    final String expectedNamespace = "";

    final String expectedName = "foo";
    final Class<? extends RecordTemplate> expectedValueClass = EmptyRecord.class;

    final String expectedKeyName = "fooId";
    final Class<?> expectedKeyClass = Long.class;

    final ResourceModel model = RestLiAnnotationReader.processResource(FooResource1.class);

    Assert.assertNotNull(model);

    Assert.assertTrue(model.isRoot());
    Assert.assertEquals(expectedName, model.getName());
    Assert.assertEquals(expectedNamespace, model.getNamespace());

    Assert.assertNull(model.getParentResourceClass());
    Assert.assertNull(model.getParentResourceModel());

    // keys
    Assert.assertEquals(1, model.getKeys().size());
    Assert.assertEquals(1, model.getKeyNames().size());
    Assert.assertEquals(1, model.getKeyClasses().size());
    Assert.assertEquals(expectedKeyName, model.getKeyName());
    Assert.assertEquals(expectedKeyName, model.getKeyNames().iterator().next());
    Assert.assertEquals(expectedKeyClass, model.getKeyClasses().get(expectedKeyName));

    // primary key
    Assert.assertNotNull(model.getPrimaryKey());
    Assert.assertNotNull(expectedKeyName, model.getPrimaryKey().getName());
    Assert.assertEquals(expectedKeyClass, model.getPrimaryKey().getType());
    Assert.assertEquals(model.getPrimaryKey(), model.getKeys().iterator().next());
    Assert.assertTrue(model.getPrimaryKey().getDataSchema() instanceof LongDataSchema);

    // alternative key
    Assert.assertTrue(model.getAlternativeKeys().isEmpty());

    // model
    Assert.assertNotNull(model.getValueClass());
    Assert.assertEquals(expectedValueClass, model.getValueClass());
  }

  @Test(description = "verifies path key and path keys parameters for entity level actions")
  public void collectionRootResourceMethodPathKeyParameters()
  {
    final ResourceModel model = RestLiAnnotationReader.processResource(PathKeyParamAnnotationsResource.class);
    Assert.assertNotNull(model);

    final Map<String, ResourceMethodDescriptor> nameToDescriptor = model.getResourceMethodDescriptors()
        .stream()
        .collect(Collectors.toMap(ResourceMethodDescriptor::getMethodName, Function.identity()));

    // first method
    final ResourceMethodDescriptor withPathKeyParamMethod = nameToDescriptor.get("withPathKeyParam");
    Assert.assertNotNull(withPathKeyParamMethod);

    final Action withPathKeyParamMethodAction = withPathKeyParamMethod.getAnnotation(Action.class);
    Assert.assertNotNull(withPathKeyParamMethodAction);
    Assert.assertEquals("withPathKeyParam", withPathKeyParamMethodAction.name());

    final List<Parameter<?>> withPathKeyParamMethodParams = withPathKeyParamMethod.getParameters();
    Assert.assertEquals(1, withPathKeyParamMethodParams.size());

    final AnnotationSet withPathKeyParamMethodParamAnnotations = withPathKeyParamMethodParams.get(0).getAnnotations();
    Assert.assertNotNull(withPathKeyParamMethodParamAnnotations);
    Assert.assertTrue(withPathKeyParamMethodParamAnnotations.contains(PathKeyParam.class));

    final PathKeyParam pathKeyParam = withPathKeyParamMethodParamAnnotations.get(PathKeyParam.class);
    Assert.assertNotNull(pathKeyParam);
    Assert.assertEquals("pathKeyParamAnnotationsId", pathKeyParam.value());

    // second method
    final ResourceMethodDescriptor withPathKeysParamMethod = nameToDescriptor.get("withPathKeysParam");
    Assert.assertNotNull(withPathKeysParamMethod);

    final Action withPathKeysParamMethodAction = withPathKeysParamMethod.getAnnotation(Action.class);
    Assert.assertNotNull(withPathKeysParamMethodAction);
    Assert.assertEquals("withPathKeysParam", withPathKeysParamMethodAction.name());

    final List<Parameter<?>> withPathKeysParamMethodParams = withPathKeysParamMethod.getParameters();
    Assert.assertEquals(1, withPathKeysParamMethodParams.size());

    final AnnotationSet withPathKeysParamMethodParamAnnotations = withPathKeysParamMethodParams.get(0).getAnnotations();
    Assert.assertNotNull(withPathKeysParamMethodParamAnnotations);
    Assert.assertTrue(withPathKeysParamMethodParamAnnotations.contains(PathKeysParam.class));

    final PathKeysParam pathKeysParam = withPathKeysParamMethodParamAnnotations.get(PathKeysParam.class);
    Assert.assertNotNull(pathKeysParam);
  }

  @Test(description = "verifies collection resources for parent/child relationship")
  public void collectionSubresource()
  {

    final String expectedNamespace = "com.linkedin.restli.internal.server.model";

    final String expectedName = "TestResource";
    final Class<? extends RecordTemplate> expectedValueClass = EmptyRecord.class;

    final String expectedKeyName = "TestResourceId";
    final Class<?> expectedKeyClass = String.class;

    final ResourceModel parent = RestLiAnnotationReader.processResource(ParentResource.class);
    final ResourceModel model = RestLiAnnotationReader.processResource(TestResource.class, parent);

    Assert.assertNotNull(model);

    Assert.assertFalse(model.isRoot());
    Assert.assertEquals(expectedName, model.getName());
    Assert.assertEquals(expectedNamespace, model.getNamespace());

    // child resource
    Assert.assertNotNull(model.getParentResourceClass());
    Assert.assertEquals(parent, model.getParentResourceModel());

    // keys
    Assert.assertEquals(1, model.getKeys().size());
    Assert.assertEquals(1, model.getKeyNames().size());
    Assert.assertEquals(1, model.getKeyClasses().size());
    Assert.assertEquals(expectedKeyName, model.getKeyName());
    Assert.assertEquals(expectedKeyName, model.getKeyNames().iterator().next());
    Assert.assertEquals(expectedKeyClass, model.getKeyClasses().get(expectedKeyName));

    // primary key
    Assert.assertNotNull(model.getPrimaryKey());
    Assert.assertNotNull(expectedKeyName, model.getPrimaryKey().getName());
    Assert.assertEquals(expectedKeyClass, model.getPrimaryKey().getType());
    Assert.assertEquals(model.getPrimaryKey(), model.getKeys().iterator().next());
    Assert.assertTrue(model.getPrimaryKey().getDataSchema() instanceof StringDataSchema);

    // alternative key
    Assert.assertTrue(model.getAlternativeKeys().isEmpty());

    // model
    Assert.assertNotNull(model.getValueClass());
    Assert.assertEquals(expectedValueClass, model.getValueClass());
  }

  @Test(description = "verifies collection resources for complex keys")
  public void complexKeyCollectionResource()
  {

    final String expectedNamespace = "";

    final String expectedName = "collectionComplexKey";
    final Class<? extends RecordTemplate> expectedValueClass = EmptyRecord.class;

    final String expectedKeyName = "collectionComplexKeyId";
    final Class<?> expectedKeyClass = ComplexResourceKey.class;

    final Class<? extends RecordTemplate> expectedKeyKeyClass = EmptyRecord.class;
    final Class<? extends RecordTemplate> expectedKeyParamsClass = EmptyRecord.class;

    final ResourceModel model = RestLiAnnotationReader.processResource(FinderSupportedComplexKeyDataResource.class);

    Assert.assertNotNull(model);

    Assert.assertTrue(model.isRoot());
    Assert.assertEquals(expectedName, model.getName());
    Assert.assertEquals(expectedNamespace, model.getNamespace());

    // child resource
    Assert.assertNull(model.getParentResourceClass());
    Assert.assertNull(model.getParentResourceModel());

    // keys
    Assert.assertEquals(1, model.getKeys().size());
    Assert.assertEquals(1, model.getKeyNames().size());
    Assert.assertEquals(1, model.getKeyClasses().size());
    Assert.assertEquals(expectedKeyName, model.getKeyName());
    Assert.assertEquals(expectedKeyName, model.getKeyNames().iterator().next());
    Assert.assertEquals(expectedKeyClass, model.getKeyClasses().get(expectedKeyName));
    Assert.assertEquals(expectedKeyKeyClass, model.getKeyKeyClass());
    Assert.assertEquals(expectedKeyParamsClass, model.getKeyParamsClass());

    // primary key
    Assert.assertNotNull(model.getPrimaryKey());
    Assert.assertNotNull(expectedKeyName, model.getPrimaryKey().getName());
    Assert.assertEquals(expectedKeyClass, model.getPrimaryKey().getType());
    Assert.assertEquals(model.getPrimaryKey(), model.getKeys().iterator().next());
    Assert.assertNull(model.getPrimaryKey().getDataSchema());

    // alternative key
    Assert.assertTrue(model.getAlternativeKeys().isEmpty());

    // model
    Assert.assertNotNull(model.getValueClass());
    Assert.assertEquals(expectedValueClass, model.getValueClass());
  }

  @Test(description = "verifies simple resource for main properties")
  public void simpleRootResource()
  {
    final String expectedNamespace = "";

    final String expectedName = "foo";
    final Class<? extends RecordTemplate> expectedValueClass = EmptyRecord.class;

    final ResourceModel model = RestLiAnnotationReader.processResource(FooResource3.class);

    Assert.assertNotNull(model);

    Assert.assertTrue(model.isRoot());
    Assert.assertEquals(expectedName, model.getName());
    Assert.assertEquals(expectedNamespace, model.getNamespace());

    Assert.assertNull(model.getParentResourceClass());
    Assert.assertNull(model.getParentResourceModel());

    // keys
    Assert.assertEquals(0, model.getKeys().size());
    Assert.assertEquals(0, model.getKeyNames().size());
    Assert.assertEquals(0, model.getKeyClasses().size());

    // primary key
    Assert.assertNull(model.getPrimaryKey());

    // alternative key
    Assert.assertTrue(model.getAlternativeKeys().isEmpty());

    // model
    Assert.assertNotNull(model.getValueClass());
    Assert.assertEquals(expectedValueClass, model.getValueClass());
  }

  // ----------------------------------------------------------------------
  // negative cases
  // ----------------------------------------------------------------------

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnDuplicateActionMethod() {

    @RestLiCollection(name = "duplicateActionMethod")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @Action(name = "duplicate")
      public EmptyRecord getThis(@ActionParam("id") Long id) {
        return new EmptyRecord();
      }

      @Action(name = "duplicate")
      public EmptyRecord getThat(@ActionParam("id") Long id) {
        return new EmptyRecord();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#getActionReturnClass should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnAssociationResourceWithNoKeys() {

    @RestLiAssociation(name = "associationWithNoKeys", assocKeys = { })
    class LocalClass extends AssociationResourceTemplate<EmptyRecord> {
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateAssociation should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnDuplicateBatchFinderMethod() {

    @RestLiCollection(name = "duplicateBatchFinderMethod")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @BatchFinder(value = "duplicate", batchParam = "criteria")
      public BatchFinderResult<EmptyRecord, EmptyRecord, EmptyRecord> batchFindThis(@QueryParam("criteria") EmptyRecord[] criteria) {
        return new BatchFinderResult<>();
      }

      @BatchFinder(value = "duplicate", batchParam = "criteria")
      public BatchFinderResult<EmptyRecord, EmptyRecord, EmptyRecord> batchFindThat(@QueryParam("criteria") EmptyRecord[] criteria) {
        return new BatchFinderResult<>();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateCrudMethods should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnDuplicateCrudMethod() {

    @RestLiCollection(name = "duplicateCrudMethod")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @RestMethod.Get
      public EmptyRecord getThis(Long id) {
        return new EmptyRecord();
      }

      @RestMethod.Get
      public EmptyRecord getThat(Long id) {
        return new EmptyRecord();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateCrudMethods should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnDuplicateFinderMethod() {

    @RestLiCollection(name = "duplicateFinderMethod")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @Finder(value = "duplicate")
      public List<EmptyRecord>  findThis(@QueryParam("criteria") String criteria) {
        return Collections.emptyList();
      }

      @Finder(value = "duplicate")
      public List<EmptyRecord> findThat(@QueryParam("criteria") String criteria) {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateFinderMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnEmptyBatchFinderMethodBatchParamParameter() {

    @RestLiCollection(name = "batchFinderWithEmptyBatchParam")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @BatchFinder(value = "batchFinderWithEmptyBatchParam", batchParam = "")
      public List<EmptyRecord> batchFinderWithEmptyBatchParam(@QueryParam("criteria") EmptyRecord[] criteria) {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateBatchFinderMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInconsistentMethodWithCallbackAndNonVoidReturn() {

    @RestLiCollection(name = "callbackAndResult")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @Action(name = "callbackAndResult")
      public List<EmptyRecord> callbackAndResult(@CallbackParam Callback<EmptyRecord> callback) {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#getInterfaceType should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInconsistentMethodWithTooManyCallbackParams() {

    @RestLiCollection(name = "tooManyCallbacks")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @Action(name = "tooManyCallbacks")
      public void tooManyCallbacks(@CallbackParam Callback<EmptyRecord> callback1, @CallbackParam Callback<EmptyRecord> callback2) {
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#getParamIndex should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidActionParamAnnotationTypeRef() {

    @RestLiCollection(name = "brokenParam")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord> {
      @Action(name = "brokenParam")
      public void brokenParam(@ActionParam(value = "someId", typeref = BrokenTypeRef.class) BrokenTypeRef typeRef) {
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#buildActionParam should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidActionReturnType() {

    @RestLiCollection(name = "invalidReturnType")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord> {
      @Action(name = "invalidReturnType")
      public Object invalidReturnType(@ActionParam(value = "someId") String someId) {
        return null;
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateActionReturnType should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidActionReturnTypeRef() {

    @RestLiCollection(name = "invalidReturnTypeRef")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord> {
      @Action(name = "invalidReturnTypeRef", returnTyperef = StringRef.class)
      public Long invalidReturnTypeRef(@ActionParam(value = "someId") String someId) {
        return null;
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#getActionTyperefDataSchema should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidBatchFinderMethodBatchParamParameterType() {

    @RestLiCollection(name = "batchFinderWithInvalidBatchParamType")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @BatchFinder(value = "batchFinderWithInvalidBatchParamType", batchParam = "criteria")
      public List<EmptyRecord> batchFinderWithInvalidBatchParamType(@QueryParam("criteria") String[] criteria) {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateBatchFinderMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidBatchFinderMethodReturnType() {

    @RestLiCollection(name = "batchFinderWithInvalidReturnType")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @BatchFinder(value = "batchFinderWithInvalidReturnType", batchParam = "criteria")
      public List<EmptyRecord> batchFinderWithInvalidReturnType(@QueryParam("criteria") EmptyRecord[] criteria) {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateBatchFinderMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidBatchFinderMethodReturnEntityType() {

    @RestLiCollection(name = "batchFinderWithInvalidReturnEntityType")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @BatchFinder(value = "batchFinderWithInvalidReturnEntityType", batchParam = "criteria")
      public BatchFinderResult<EmptyRecord, Link, EmptyRecord> batchFinderWithInvalidReturnEntityType(@QueryParam("criteria") EmptyRecord[] criteria) {
        return new BatchFinderResult<>();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateBatchFinderMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidFinderMethodReturnType() {

    @RestLiCollection(name = "finderWithInvalidReturnType")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @Finder("finderWithInvalidReturnType")
      public Map<Long, EmptyRecord> finderWithInvalidReturnType(@QueryParam("arg") long arg) {
        return Collections.emptyMap();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateFinderMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidFinderMethodNonEntityReturnType() {

    @RestLiCollection(name = "finderWithInvalidNonEntityReturnType")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @Finder("finderWithInvalidNonEntityReturnType")
      public List<Long> finderWithInvalidNonEntityReturnType(@QueryParam("arg") long arg) {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateFinderMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnInvalidQueryParamAnnotationTypeRef() {

    @RestLiCollection(name = "brokenParam")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord> {
      @Finder("brokenParam")
      public void brokenParam(@QueryParam(value = "someId", typeref = BrokenTypeRef.class) BrokenTypeRef typeRef) {
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#buildQueryParam should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = NullPointerException.class, description = "hard fails with NPE on missing criteria parameter")
  public void failsOnMissingBatchFinderMethodBatchParamParameter() {

    @RestLiCollection(name = "batchFinderWithMissingBatchParam")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @BatchFinder(value = "batchFinderWithMissingBatchParam", batchParam = "criteria")
      public List<EmptyRecord> batchFinderWithMissingBatchParam() {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateBatchFinderMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnMissingParamAnnotation() {

    @RestLiCollection(name = "noParamAnnotation")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord> {
      @Action(name = "noParamAnnotation")
      public void noParamAnnotation(String someId) {
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#getParameters should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnNonInstantiableActionReturnTypeRef() {

    @RestLiCollection(name = "invalidActionReturnType")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord> {
      @Action(name = "nonInstantiableTypeRef", returnTyperef = BrokenTypeRef.class)
      public BrokenTypeRef nonInstantiableTypeRef(@ActionParam(value = "someId") String someId) {
        return null;
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#getActionTyperefDataSchema should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnNonPublicActionMethod() {

    @RestLiCollection(name = "nonPublicActionMethod")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @Action(name = "protectedAction")
      protected void protectedAction(@ActionParam("actionParam") String actionParam) {
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#addActionResourceMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnNonPublicBatchFinderMethod() {

    @RestLiCollection(name = "nonPublicBatchFinderMethod")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @BatchFinder(value = "protected", batchParam = "criteria")
      List<EmptyRecord> protectedBatchFind(String someOtherId, List<String> criteria) {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#addBatchFinderResourceMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnNonPublicCreateMethod()
  {

    @RestLiCollection(name = "nonPublicCreateMethod")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @RestMethod.Create
      CreateResponse protectedCreate(EmptyRecord entity)
      {
        return new CreateResponse(HttpStatus.S_200_OK);
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#addCrudResourceMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnNonPublicFinderMethod() {

    @RestLiCollection(name = "nonPublicFinderMethod")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @Finder("protected")
      List<EmptyRecord> protectedFind(String someOtherId) {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#addFinderResourceMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnTooManyMethodAnnotations() {

    @RestLiCollection(name = "tooManyMethodAnnotations")
    class LocalClass extends CollectionResourceTemplate<Long, EmptyRecord>
    {
      @RestMethod.Create
      @RestMethod.Get
      public void doubleAnnotationMethod(EmptyRecord model) {
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#addCrudResourceMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnSimpleResourceWithCollectionLevelAction() {

    @RestLiSimpleResource(name = "simpleResourceWithUnsupportedMethod")
    class LocalClass extends SimpleResourceTemplate<EmptyRecord>
    {
      @Action(name = "badAction", resourceLevel = ResourceLevel.COLLECTION)
      public void badAction(@ActionParam("someId") String someId) {
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#addActionResourceMethod should fail throwing a ResourceConfigException");
  }

  @Test(expectedExceptions = ResourceConfigException.class)
  public void failsOnSimpleResourceWithInvalidMethod() {

    @RestLiSimpleResource(name = "simpleResourceWithUnsupportedMethod")
    class LocalClass extends SimpleResourceTemplate<EmptyRecord>
    {
      @RestMethod.GetAll
      public List<EmptyRecord> getAll() {
        return Collections.emptyList();
      }
    }

    RestLiAnnotationReader.processResource(LocalClass.class);
    Assert.fail("#validateSimpleResource should fail throwing a ResourceConfigException");
  }

  // ----------------------------------------------------------------------
  // helper types used in tests
  // ----------------------------------------------------------------------

  private static final class BrokenTypeRef extends TyperefInfo {
    private BrokenTypeRef() {
      super(new TyperefDataSchema(new Name()));
    }
  }

  private static final class StringRef extends TyperefInfo {

    private final static TyperefDataSchema SCHEMA = ((TyperefDataSchema) DataTemplateUtil.parseSchema("namespace com.linkedin.restli.internal.server.model typeref StringRef = string", SchemaFormatType.PDL));

    public StringRef() {
      super(SCHEMA);
    }

    public static TyperefDataSchema dataSchema() {
      return SCHEMA;
    }
  }

  // ----------------------------------------------------------------------
  // data providers
  // ----------------------------------------------------------------------

  @DataProvider(name = "actionReturnTypeData")
  private Object[][] provideActionReturnTypeData()
  {
    return new Object[][]{
        {ActionReturnTypeVoidResource.class, Void.TYPE},
        {ActionReturnTypeIntegerResource.class, Integer.class},
        {ActionReturnTypeRecordResource.class, EmptyRecord.class}};
  }
}
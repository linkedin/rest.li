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

package com.linkedin.restli.server.combined;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.data.ByteString;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.validation.CreateOnly;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.server.BatchCreateRequest;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.MapWithTestRecord;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestAnnotations;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.combined.CombinedTestDataModels.DummyKeyPart;
import com.linkedin.restli.server.combined.CombinedTestDataModels.DummyParamsPart;
import com.linkedin.restli.server.combined.CombinedTestDataModels.Foo;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.server.resources.SingleObjectResource;

/**
 * @author dellamag
 */
public class CombinedResources
{
  @RestLiCollection(name="test")
  public static class CombinedCollectionResource extends CollectionResourceTemplate<String,Foo>
  {
    @Override
    public Foo get(String key)
    {
      return null;
    }
  }
  
  
  @RestLiCollection(name="complexKeyCollection")
  public static class CombinedComplexKeyResource extends ComplexKeyResourceTemplate<DummyKeyPart, DummyParamsPart, Foo>
  {
    @RestMethod.Update
    public UpdateResponse update(ComplexResourceKey<DummyKeyPart, DummyParamsPart> key,
                                 Foo entity, @Optional @QueryParam("testParam") DummyParamsPart param)
    {
      return super.update(key, entity);
    }

    @Override
    public Foo get(ComplexResourceKey<DummyKeyPart, DummyParamsPart> key)
    {
      return null;
    }
  }

  @RestLiAssociation(name="test",
                     assocKeys={@Key(name="foo", type=String.class),
                                @Key(name="bar", type=String.class)})
  public static class CombinedAssociationResource extends AssociationResourceTemplate<Foo>
  {
    @Override
    public Foo get(CompoundKey key)
    {
      return null;
    }

    @Override
    public Map<CompoundKey, Foo> batchGet(Set<CompoundKey> key)
    {
      return null;
    }

    @RestMethod.Update
    public UpdateResponse update1(CompoundKey key, Foo entity, @Optional(RestAnnotations.DEFAULT) @QueryParam("testParam") Integer test)
    {
      return super.update(key, entity);
    }
  }

  @RestLiAssociation(name="test",
                     assocKeys ={@Key(name="foo", type=String.class),
                                @Key(name="bar", type=String.class),
                                @Key(name="baz", type=String.class)})
  public static class CombinedNKeyAssociationResource extends AssociationResourceTemplate<Foo>
  {
    @Override
    public Foo get(CompoundKey key)
    {
      return null;
    }

    @Override
    public Map<CompoundKey, Foo> batchGet(Set<CompoundKey> key)
    {
      return null;
    }

    @Finder("find")
    public List<Foo> find(@PagingContextParam PagingContext context, @AssocKeyParam("foo") int foo, @AssocKeyParam("bar") int bar)
    {
      return new ArrayList<Foo>();
    }
  }

  @RestLiSimpleResource(name="test")
  public static class CombinedSimpleResource extends SimpleResourceTemplate<Foo>
  {
    @Override
    public Foo get()
    {
      return null;
    }
  }

  @RestLiCollection(name="test")
  public static class CombinedCollectionWithSubresources extends CollectionResourceTemplate<String,Foo>
  {
    @Override
    public Foo get(String key)
    {
      return null;
    }
  }

  @RestLiCollection(parent=CombinedCollectionWithSubresources.class, name="sub")
  public static class SubCollectionResource extends CollectionResourceTemplate<String,Foo>
  {
    @Override
    public Foo get(String key)
    {
      return null;
    }
  }

  @RestLiSimpleResource(parent=CombinedCollectionWithSubresources.class, name="sub2")
  public static class SubsimpleResource extends SimpleResourceTemplate<Foo>
  {
    @Override
    public Foo get()
    {
      return null;
    }
  }

  @RestLiSimpleResource(name="test")
  public static class CombinedSimpleResourceWithSubresources extends SimpleResourceTemplate<Foo>
  {
    @Override
    public Foo get()
    {
      return null;
    }
  }

  @RestLiCollection(parent=CombinedSimpleResourceWithSubresources.class, name="sub")
  public static class SubCollectionOfSimpleResource extends CollectionResourceTemplate<String,Foo>
  {
    @Override
    public Foo get(String key)
    {
      return null;
    }
  }

  @RestLiSimpleResource(parent=CombinedSimpleResourceWithSubresources.class, name="sub2")
  public static class SubsimpleResourceOfSimpleResource extends SimpleResourceTemplate<Foo>
  {
    @Override
    public Foo get()
    {
      return null;
    }
  }

  @RestLiSimpleResource(name="test")
  public static class SimpleResourceAllMethods extends SimpleResourceTemplate<Foo>
  {
    @Override
    public Foo get()
    {
      return null;
    }

    @Override
    public UpdateResponse update(Foo foo)
    {
      return null;
    }

    @Override
    public UpdateResponse update(PatchRequest<Foo> patch)
    {
      return null;
    }

    @Override
    public UpdateResponse delete()
    {
      return null;
    }

    @Action(name="myAction")
    public void myAction(@ActionParam("intParam") int a)
    {

    }
  }

  @RestLiActions(name = "test")
  public static class TestActionsResource
  {
    @Action(name="intParam")
    public void intParam(@ActionParam("intParam") int foo)
    {

    }

    @Action(name="longParam")
    public void longParam(@ActionParam("longParam") long foo)
    {

    }

    @Action(name="byteStringParam")
    public void byteStringParam(@ActionParam("byteStringParam") ByteString foo)
    {

    }

    @Action(name="floatParam")
    public void floatParam(@ActionParam("floatParam") float foo)
    {

    }

    @Action(name="doubleParam")
    public void doubleParam(@ActionParam("doubleParam") double foo)
    {

    }

    @Action(name="recordParam")
    public void recordParam(@ActionParam("recordParam") TestRecord foo)
    {

    }
  }

  @RestLiCollection(name="test")
  public static class CollectionWithAnnotatedCrudMethods implements KeyValueResource<String,Foo>
  {
    @RestMethod.Create
    public CreateResponse myCreate(Foo entity)
    {
      return null;
    }

    @RestMethod.BatchGet
    public Map<String, Foo> myBatchGet(Set<String> ids)
    {
      return null;
    }

    @RestMethod.Get
    public Foo myGet(String key)
    {
      return null;
    }

    @RestMethod.Update
    public UpdateResponse myUpdate(String key, Foo entity)
    {
      return null;
    }

    @RestMethod.PartialUpdate
    public UpdateResponse myUpdate(String key, PatchRequest<Foo> patch)
    {
      return null;
    }

    @RestMethod.Delete
    public UpdateResponse myDelete(String key)
    {
      return null;
    }

    @RestMethod.BatchUpdate
    public BatchUpdateResult<String, Foo> myBatchUpdate(BatchUpdateRequest<String, Foo> entities)
    {
      return null;
    }

    @RestMethod.BatchPartialUpdate
    public BatchUpdateResult<String, Foo> myBatchUpdate(BatchPatchRequest<String, Foo> patches)
    {
      return null;
    }

    @RestMethod.BatchCreate
    public BatchCreateResult<String, Foo> myBatchCreate(BatchCreateRequest<String, Foo> entities)
    {
      return null;
    }

    @RestMethod.BatchDelete
    public BatchUpdateResult<String, Foo> myBatchDelete(BatchDeleteRequest<String, Foo> ids)
    {
      return null;
    }

  }

  @RestLiCollection(name="test", keyName = "testId")
  public static class CollectionWithCustomCrudParams implements KeyValueResource<String,Foo>
  {
    @RestMethod.Create
    public CreateResponse myCreate(Foo entity, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.BatchGet
    public Map<String, Foo> myBatchGet(Set<String> ids, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.Get
    public Foo myGet(String key, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.Update
    public UpdateResponse myUpdate(String key, Foo entity, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.PartialUpdate
    public UpdateResponse myUpdate(String key, PatchRequest<Foo> patch, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.Delete
    public UpdateResponse myDelete(String key, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.BatchUpdate
    public BatchUpdateResult<String, Foo> myBatchUpdate(BatchUpdateRequest<String, Foo> entities, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.BatchPartialUpdate
    public BatchUpdateResult<String, Foo> myBatchUpdate(BatchPatchRequest<String, Foo> patches, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.BatchCreate
    public BatchCreateResult<String, Foo> myBatchCreate(BatchCreateRequest<String, Foo> entities, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.BatchDelete
    public BatchUpdateResult<String, Foo> myBatchDelete(BatchDeleteRequest<String, Foo> ids, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

  }

  @RestLiSimpleResource(name="test")
  public static class SimpleResourceWithAnnotatedCrudMethods implements SingleObjectResource<Foo>
  {
    @RestMethod.Get
    public Foo myGet()
    {
      return null;
    }

    @RestMethod.Update
    public UpdateResponse myUpdate(Foo entity)
    {
      return null;
    }

    @RestMethod.PartialUpdate
    public UpdateResponse myUpdate(PatchRequest<Foo> patch)
    {
      return null;
    }

    @RestMethod.Delete
    public UpdateResponse myDelete()
    {
      return null;
    }

    @Action(name="myAction")
    public void myAction(@ActionParam("intParam") int a)
    {

    }
  }

  @RestLiSimpleResource(name="test")
  public static class SimpleResourceWithCustomCrudParams implements SingleObjectResource<Foo>
  {
    @RestMethod.Get
    public Foo myGet(@QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.Update
    public UpdateResponse myUpdate(Foo entity, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.PartialUpdate
    public UpdateResponse myPartialUpdate(PatchRequest<Foo> patch, @QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }

    @RestMethod.Delete
    public UpdateResponse myDelete(@QueryParam("intParam") @Optional("42") int intParam, @QueryParam("stringParam") String stringParam)
    {
      return null;
    }
  }

  @RestLiCollection(name="test")
  public static class ComplexKeyResourceWithAnnotatedCrudMethods
      implements KeyValueResource<ComplexResourceKey<DummyKeyPart, DummyParamsPart>, Foo>
  {
    @RestMethod.Create
    public CreateResponse myCreate(Foo entity)
    {
      return null;
    }

    @RestMethod.BatchGet
    public Map<ComplexResourceKey<DummyKeyPart, DummyParamsPart>, Foo> myBatchGet(
        Set<ComplexResourceKey<DummyKeyPart, DummyParamsPart>> ids)
    {
      return null;
    }

    @RestMethod.Get
    public Foo myGet(ComplexResourceKey<DummyKeyPart, DummyParamsPart> key)
    {
      return null;
    }

    @RestMethod.Update
    public UpdateResponse myUpdate(ComplexResourceKey<DummyKeyPart, DummyParamsPart> key, Foo entity)
    {
      return null;
    }

    @RestMethod.PartialUpdate
    public UpdateResponse myUpdate(ComplexResourceKey<DummyKeyPart, DummyParamsPart> key, PatchRequest<Foo> patch)
    {
      return null;
    }

    @RestMethod.Delete
    public UpdateResponse myDelete(ComplexResourceKey<DummyKeyPart, DummyParamsPart> key)
    {
      return null;
    }

    @RestMethod.BatchUpdate
    public BatchUpdateResult<String, Foo> myBatchUpdate(
        BatchUpdateRequest<ComplexResourceKey<DummyKeyPart, DummyParamsPart>, Foo> entities)
    {
      return null;
    }

    @RestMethod.BatchPartialUpdate
    public BatchUpdateResult<String, Foo> myBatchUpdate(
        BatchPatchRequest<ComplexResourceKey<DummyKeyPart, DummyParamsPart>, Foo> patches)
    {
      return null;
    }

    @RestMethod.BatchCreate
    public BatchCreateResult<String, Foo> myBatchCreate(
        BatchCreateRequest<ComplexResourceKey<DummyKeyPart, DummyParamsPart>, Foo> entities)
    {
      return null;
    }

    @RestMethod.BatchDelete
    public BatchUpdateResult<String, Foo> myBatchDelete(
        BatchDeleteRequest<ComplexResourceKey<DummyKeyPart, DummyParamsPart>, Foo> ids)
    {
      return null;
    }
  }

  @ReadOnly({"intField", "longField"})
  @CreateOnly("floatField")
  @RestLiSimpleResource(name="foo")
  public class DataAnnotationTestResource extends SimpleResourceTemplate<TestRecord>
  {
  }
}

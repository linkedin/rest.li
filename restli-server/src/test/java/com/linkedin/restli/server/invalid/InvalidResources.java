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

package com.linkedin.restli.server.invalid;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.pegasus.generator.test.LongRef;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.validation.CreateOnly;
import com.linkedin.restli.common.validation.ReadOnly;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CustomLongRef;
import com.linkedin.restli.server.CustomStringRef;
import com.linkedin.restli.server.MapWithTestRecord;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ResourceLevel;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.annotations.BatchFinder;
import com.linkedin.restli.server.annotations.MetadataProjectionParam;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.PagingProjectionParam;
import com.linkedin.restli.server.annotations.ProjectionParam;
import com.linkedin.restli.server.custom.types.CustomString;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.server.resources.SingleObjectResource;
import com.linkedin.restli.server.twitter.TwitterTestDataModels;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Followed;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.Status;
import com.linkedin.restli.server.twitter.TwitterTestDataModels.User;

import java.util.Collections;
import java.util.List;

/**
 * @author dellamag
 */
public class InvalidResources
{

  @RestLiCollection(name="foo")
  public static class ComplexKeyInCollectionResourceTemplate extends CollectionResourceTemplate<Status, Status>
  {
    // empty
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderNonListReturnType extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public String find() {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderNonRecordTemplateReturnType extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public List<String> find() {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderNonMatchingRecordTemplateReturnType extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public List<Followed> find() {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderNonMatchingRecordTemplateReturnTypeCollectionResult extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public CollectionResult<Followed, RecordTemplate> find() {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderTwoDefaultsInOneClass extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public List<Status> find() {return null;}

    @Finder("find")
    public List<Status> findSomethingElse() {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderTwoNamedInOneClass extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("foo")
    public List<Status> find() {return null;}

    @Finder("foo")
    public List<Status> findSomethingElse() {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderInvalidParameters extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public List<Status> find(@QueryParam("o") Object o) {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderInvalidParameters2 extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public List<Status> find(@QueryParam("p1") Integer p1,
                             @QueryParam("p2") int p2,
                             @QueryParam("p3")  List<String> p3) {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderIncompatibleCustomObjectParameters extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public List<Status> find(@QueryParam(value="s", typeref=CustomLongRef.class) CustomString s) {return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderCustomTypeArrayParams extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public List<Status> find(@QueryParam(value="strings", typeref= CustomStringRef.class) CustomString[] strings) { return null;}
  }

  @RestLiCollection(name="foo", keyName="foo")
  public static class FinderUnannotatedParameters extends CollectionResourceTemplate<Long, Status>
  {
    @Finder("find")
    public List<Status> find(String s) {return null;}
  }

  @RestLiAssociation(name="foo",
      assocKeys={@Key(name="key1", type=String.class),
                 @Key(name="key2", type=String.class)})
  public class FinderNonExistingAssocKey extends AssociationResourceTemplate<Record>
  {
    @Finder("assocKeyFinder")
    public List<Record> assocKeyFinder(@AssocKeyParam("key3") String s)
    {
      return null;
    }
  }

  @RestLiAssociation(name="foo",
      assocKeys={@Key(name="key1", type=String.class),
                 @Key(name="key2", type=String.class)})
  public class GetAllNonExistingAssocKey extends AssociationResourceTemplate<Record>
  {
    public List<Record> getAll(@AssocKeyParam("key3") String s)
    {
      return null;
    }
  }

  @RestLiCollection(name = "foo", keyName="foo")
  public static class DuplicateGetMethod implements KeyValueResource<Long, Status>
  {
    @RestMethod.Get
    public Status get1(Long key)
    {
      return null;
    }

    @RestMethod.Get
    public Status get2(Long key)
    {
      return null;
    }
  }

  @RestLiCollection(name="typeref")
  public static class TyperefKeyResource implements KeyValueResource<LongRef, Status>
  {
  }

  @RestLiCollection(name="typeref")
  public static class TyperefKeyCollection extends CollectionResourceTemplate<LongRef, Status>
  {
  }

  public static class Record extends RecordTemplate
  {
    public Record(DataMap map)
    {
      super(map, null);
    }
  }

  @RestLiAssociation(name="SingleAssociation",
          assocKeys={@Key(name="key", type=int.class)})
  public class SingleAssociation extends AssociationResourceTemplate<Record>
  {
    //nothing to do as the validation will fail.
  }

  @RestLiSimpleResource(name="foo")
  public class SimpleResourceWithInvalidMethodTypes implements SingleObjectResource<Record>
  {
    @Finder(value="myFinder")
    public List<Record> myFinder() {return null;}
  }

  @RestLiSimpleResource(name="foo")
  public class SimpleResourceWithInvalidAction extends SimpleResourceTemplate<Record>
  {
    @Action(name="myAction", resourceLevel = ResourceLevel.COLLECTION)
    public void myAction() {}
  }

  @CreateOnly("asdf")
  @RestLiSimpleResource(name="foo")
  public class DataAnnotationOnNonexistentField extends SimpleResourceTemplate<TestRecord>
  {
  }

  @ReadOnly({"mapA", "mapA"})
  @RestLiSimpleResource(name="foo")
  public class DuplicateDataAnnotation extends SimpleResourceTemplate<MapWithTestRecord>
  {
  }

  @CreateOnly({"mapA", "mapA/*/intField"})
  @RestLiSimpleResource(name="foo")
  public class RedundantDataAnnotation1 extends SimpleResourceTemplate<MapWithTestRecord>
  {
  }

  @ReadOnly("mapA")
  @CreateOnly("mapA")
  @RestLiSimpleResource(name="foo")
  public class RedundantDataAnnotation2 extends SimpleResourceTemplate<MapWithTestRecord>
  {
  }

  @ReadOnly("mapA")
  @CreateOnly("mapA/*/doubleField")
  @RestLiSimpleResource(name="foo")
  public class RedundantDataAnnotation3 extends SimpleResourceTemplate<MapWithTestRecord>
  {
  }

  @ReadOnly("mapA/*/doubleField")
  @CreateOnly("mapA")
  @RestLiSimpleResource(name="foo")
  public class RedundantDataAnnotation4 extends SimpleResourceTemplate<MapWithTestRecord>
  {
  }

  @RestLiCollection(name = "foo", keyName="foo")
  public static class MissingLinkedBatchFinder extends CollectionResourceTemplate<Long, Status>
  {
    @RestMethod.Get
    public Status get(Long key)
    {
      return null;
    }

    @Finder(value = "find", linkedBatchFinderName = "batchFind")
    public List<Status> find(@QueryParam("statusName") String statusName)
    {
      return Collections.emptyList();
    }
  }

  public static class EmptyCriteria extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("EmptyCriteria", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public EmptyCriteria()
    {
      super(new DataMap(), SCHEMA);
    }

    public EmptyCriteria(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  @RestLiCollection(name = "foo", keyName="foo")
  public static class LinkedBatchFinderMissingFieldInCriteria extends CollectionResourceTemplate<Long, Status>
  {
    @RestMethod.Get
    public Status get(Long key)
    {
      return null;
    }

    @Finder(value = "find", linkedBatchFinderName = "batchFind")
    public List<Status> find(@QueryParam("statusName") String statusName)
    {
      return Collections.emptyList();
    }

    @BatchFinder(value = "batchFind", batchParam = "criteria")
    public BatchFinderResult<EmptyCriteria, Status, Followed> batchFind(
        @QueryParam("criteria") EmptyCriteria[] criteria)
    {
      return new BatchFinderResult<>();
    }
  }

  @RestLiAssociation(name="associations",
      namespace = "com.linkedin.restli.server.invalid",
      assocKeys={@Key(name="src", type=String.class), @Key(name="dest", type=String.class)})
  public static class LinkedBatchFinderAssocKeyFieldInCriteria extends AssociationResourceTemplate<Status>
  {
    @Override
    public Status get(CompoundKey key) {
      return null;
    }

    @Finder(value = "find", linkedBatchFinderName = "batchFind")
    public List<Status> find(@AssocKeyParam("src") String src)
    {
      return Collections.emptyList();
    }

    @BatchFinder(value = "batchFind", batchParam = "criteria")
    public BatchFinderResult<EmptyCriteria, Status, Followed> batchFind(
        @QueryParam("criteria") EmptyCriteria[] criteria)
    {
      return new BatchFinderResult<>();
    }
  }

  public static class LongStatusCriteria extends RecordTemplate
  {
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
        "{\"type\":\"record\",\"name\":\"LongStatusCriteria\",\"namespace\":\"com.example.test\",\"fields\":[{\"name\":\"statusName\",\"type\":\"long\"}]}"));

    public LongStatusCriteria()
    {
      super(new DataMap(), SCHEMA);
    }

    public LongStatusCriteria(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  @RestLiCollection(name = "foo", keyName="foo")
  public static class LinkedBatchFinderMismatchedFieldTypeInCriteria extends CollectionResourceTemplate<Long, Status>
  {
    @RestMethod.Get
    public Status get(Long key)
    {
      return null;
    }

    @Finder(value = "find", linkedBatchFinderName = "batchFind")
    public List<Status> find(@QueryParam("statusName") String statusName)
    {
      return Collections.emptyList();
    }

    @BatchFinder(value = "batchFind", batchParam = "criteria")
    public BatchFinderResult<LongStatusCriteria, Status, Followed> batchFind(
        @QueryParam("criteria") LongStatusCriteria[] criteria)
    {
      return new BatchFinderResult<>();
    }
  }

  public static class OptionalStringStatusCriteria extends RecordTemplate
  {
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
        "{\"type\":\"record\",\"name\":\"OptionalStringStatusCriteria\",\"namespace\":\"com.example.test\",\"fields\":[{\"name\":\"statusName\",\"type\":\"string\",\"optional\": true}]}"));

    public OptionalStringStatusCriteria()
    {
      super(new DataMap(), SCHEMA);
    }

    public OptionalStringStatusCriteria(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  @RestLiCollection(name = "foo", keyName="foo")
  public static class LinkedBatchFinderMismatchedFieldOptionalityInCriteria extends CollectionResourceTemplate<Long, Status>
  {
    @RestMethod.Get
    public Status get(Long key)
    {
      return null;
    }

    @Finder(value = "find", linkedBatchFinderName = "batchFind")
    public List<Status> find(@QueryParam("statusName") String statusName)
    {
      return Collections.emptyList();
    }

    @BatchFinder(value = "batchFind", batchParam = "criteria")
    public BatchFinderResult<OptionalStringStatusCriteria, Status, Followed> batchFind(
        @QueryParam("criteria") OptionalStringStatusCriteria[] criteria)
    {
      return new BatchFinderResult<>();
    }
  }

  public static class ExtraFieldsCriteria extends RecordTemplate
  {
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
        "{\"type\":\"record\",\"name\":\"ExtraFieldsCriteria\",\"namespace\":\"com.example.test\",\"fields\":[{\"name\":\"statusName\",\"type\":\"string\"}, {\"name\":\"random\",\"type\":\"long\"}]}"));

    public ExtraFieldsCriteria()
    {
      super(new DataMap(), SCHEMA);
    }

    public ExtraFieldsCriteria(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  @RestLiCollection(name = "foo", keyName="foo")
  public static class LinkedBatchFinderExtraFieldsInCriteria extends CollectionResourceTemplate<Long, Status>
  {
    @RestMethod.Get
    public Status get(Long key)
    {
      return null;
    }

    @Finder(value = "find", linkedBatchFinderName = "batchFind")
    public List<Status> find(@QueryParam("statusName") String statusName)
    {
      return Collections.emptyList();
    }

    @BatchFinder(value = "batchFind", batchParam = "criteria")
    public BatchFinderResult<ExtraFieldsCriteria, Status, Followed> batchFind(
        @QueryParam("criteria") ExtraFieldsCriteria[] criteria)
    {
      return new BatchFinderResult<>();
    }
  }

  public static class CorrectCriteria extends RecordTemplate
  {
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema(
        "{\"type\":\"record\",\"name\":\"CorrectCriteria\",\"namespace\":\"com.example.test\",\"fields\":[{\"name\":\"statusName\",\"type\":\"string\"}]}"));

    public CorrectCriteria()
    {
      super(new DataMap(), SCHEMA);
    }

    public CorrectCriteria(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  @RestLiCollection(name = "foo", keyName="foo")
  public static class LinkedBatchFinderMetadataMismatch extends CollectionResourceTemplate<Long, Status>
  {
    @RestMethod.Get
    public Status get(Long key)
    {
      return null;
    }

    @Finder(value = "find", linkedBatchFinderName = "batchFind")
    public CollectionResult<Status, User> find(@QueryParam("statusName") String statusName)
    {
      return new CollectionResult<>(Collections.emptyList());
    }

    @BatchFinder(value = "batchFind", batchParam = "criteria")
    public BatchFinderResult<CorrectCriteria, Status, Followed> batchFind(
        @QueryParam("criteria") CorrectCriteria[] criteria)
    {
      return new BatchFinderResult<>();
    }
  }

  @RestLiCollection(name = "foo", keyName="foo")
  public static class LinkedBatchFinderUnsupportedPaging extends CollectionResourceTemplate<Long, Status>
  {
    @RestMethod.Get
    public Status get(Long key)
    {
      return null;
    }

    @Finder(value = "find", linkedBatchFinderName = "batchFind")
    public CollectionResult<Status, User> find(@QueryParam("statusName") String statusName,
        @PagingContextParam PagingContext paging)
    {
      return new CollectionResult<>(Collections.emptyList());
    }

    @BatchFinder(value = "batchFind", batchParam = "criteria")
    public BatchFinderResult<CorrectCriteria, Status, User> batchFind(
        @QueryParam("criteria") CorrectCriteria[] criteria)
    {
      return new BatchFinderResult<>();
    }
  }
}

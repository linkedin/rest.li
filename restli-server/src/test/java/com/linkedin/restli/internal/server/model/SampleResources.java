/*
   Copyright (c) 2019 LinkedIn Corp.

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
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.BatchDeleteRequest;
import com.linkedin.restli.server.BatchUpdateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.UnstructuredDataReactiveReader;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.AssocKeyParam;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.MetadataProjectionParam;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.PagingProjectionParam;
import com.linkedin.restli.server.annotations.ParSeqContextParam;
import com.linkedin.restli.server.annotations.ParamError;
import com.linkedin.restli.server.annotations.PathKeyParam;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.ProjectionParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.ResourceContextParam;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiAttachmentsParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ServiceErrorDef;
import com.linkedin.restli.server.annotations.ServiceErrors;
import com.linkedin.restli.server.annotations.SuccessResponse;
import com.linkedin.restli.server.annotations.UnstructuredDataReactiveReaderParam;
import com.linkedin.restli.server.errors.ServiceError;
import com.linkedin.restli.server.resources.AssociationResourceAsyncTemplate;
import com.linkedin.restli.server.resources.AssociationResourceTaskTemplate;
import com.linkedin.restli.server.resources.AssociationResourceTemplate;
import com.linkedin.restli.server.resources.CollectionResourceAsyncTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTaskTemplate;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceAsyncTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceTaskTemplate;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.SimpleResourceAsyncTemplate;
import com.linkedin.restli.server.resources.SimpleResourceTaskTemplate;
import com.linkedin.restli.server.resources.SimpleResourceTemplate;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataCollectionResourceReactiveTemplate;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataCollectionResourceTemplate;
import com.linkedin.restli.server.resources.unstructuredData.UnstructuredDataSimpleResourceTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Sample resources used for testing in this package.
 *
 * @author Evan Williams
 */
@SuppressWarnings({"unused", "InnerClassMayBeStatic"})
class SampleResources
{
  /**
   * Sample service errors defined for these resources to use.
   */
  enum SampleServiceError implements ServiceError
  {
    ERROR_A,
    ERROR_B,
    FORBIDDEN_ERROR_DETAIL_TYPE(ErrorDetails.class);

    private Class<? extends RecordTemplate> _errorDetailType;

    SampleServiceError()
    {
      this(null);
    }

    SampleServiceError(Class<? extends RecordTemplate> errorDetailType)
    {
      _errorDetailType = errorDetailType;
    }

    interface Codes
    {
      String ERROR_A = "ERROR_A";
      String ERROR_B = "ERROR_B";
      String FORBIDDEN_ERROR_DETAIL_TYPE = "FORBIDDEN_ERROR_DETAIL_TYPE";
    }

    @Override
    public HttpStatus httpStatus()
    {
      return HttpStatus.S_400_BAD_REQUEST;
    }

    @Override
    public String code()
    {
      return name();
    }

    @Override
    public Class<? extends RecordTemplate> errorDetailType()
    {
      return _errorDetailType;
    }
  }

  /**
   * The following resources are used by {@link TestRestLiApiBuilder}.
   */

  @RestLiCollection(name = "foo")
  static class FooResource1 extends CollectionResourceTemplate<Long, EmptyRecord> {}

  @RestLiCollection(name = "foo")
  static class FooResource2 extends CollectionResourceTemplate<Long, EmptyRecord> {}

  @RestLiSimpleResource(name = "foo")
  static class FooResource3 extends SimpleResourceTemplate<EmptyRecord> {}

  @RestLiActions(name = "foo")
  static class FooResource4 {}

  @RestLiCollection(name = "bar")
  static class BarResource extends CollectionResourceTemplate<Long, EmptyRecord> {}

  @RestLiCollection(name = "FOO")
  static class FOOResource extends CollectionResourceTemplate<Long, EmptyRecord> {}

  @RestLiCollection(
    name = "TestResource",
    namespace = "com.linkedin.restli.internal.server.model",
    parent = ParentResource.class
  )
  class TestResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Action(name = "testResourceAction")
    public void takeAction() {}
  }

  @RestLiCollection(
    name = "ParentResource",
    namespace = "com.linkedin.restli.internal.server.model"
  )
  class ParentResource extends CollectionResourceTemplate<String, EmptyRecord> {}

  @RestLiCollection(
    name = "BadResource",
    namespace = "com.linkedin.restli.internal.server.model"
  )
  class BadResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Action(name = "badResourceAction")
    public void takeAction(@PathKeyParam("bogusKey") String bogusKey) {}
  }

  /**
   * The following resources are used by {@link TestRestLiApiBuilder#testMisconfiguredServiceErrors(Class, String)}.
   */

  @RestLiCollection(name = "unknownServiceErrorCode")
  @ServiceErrorDef(SampleServiceError.class)
  @ServiceErrors("MADE_UP_ERROR")
  class UnknownServiceErrorCodeResource implements KeyValueResource<Long, EmptyRecord> {}

  @RestLiCollection(name = "duplicateServiceErrorCodes")
  @ServiceErrorDef(SampleServiceError.class)
  @ServiceErrors({SampleServiceError.Codes.ERROR_A, SampleServiceError.Codes.ERROR_A})
  class DuplicateServiceErrorCodesResource implements KeyValueResource<Long, EmptyRecord> {}

  @RestLiCollection(name = "missingServiceErrorDef")
  @ServiceErrors(SampleServiceError.Codes.ERROR_A)
  class MissingServiceErrorDefResource implements KeyValueResource<Long, EmptyRecord> {}

  @RestLiCollection(name = "forbiddenErrorDetailType")
  @ServiceErrorDef(SampleServiceError.class)
  @ServiceErrors(SampleServiceError.Codes.FORBIDDEN_ERROR_DETAIL_TYPE)
  class ForbiddenErrorDetailTypeResource implements KeyValueResource<Long, EmptyRecord> {}

  @RestLiCollection(name = "unknownServiceErrorParameter")
  @ServiceErrorDef(SampleServiceError.class)
  class UnknownServiceErrorParameterResource implements KeyValueResource<Long, EmptyRecord>
  {
    @Finder(value = "query")
    @ParamError(code = SampleServiceError.Codes.ERROR_A, parameterNames = { "spacestamp" })
    public List<EmptyRecord> query(@QueryParam("timestamp") String timestamp)
    {
      return new ArrayList<>();
    }
  }

  @RestLiCollection(name = "emptyServiceErrorParameters")
  @ServiceErrorDef(SampleServiceError.class)
  class EmptyServiceErrorParametersResource implements KeyValueResource<Long, EmptyRecord>
  {
    @Finder(value = "query")
    @ParamError(code = SampleServiceError.Codes.ERROR_A, parameterNames = {})
    public List<EmptyRecord> query(@QueryParam("timestamp") String timestamp)
    {
      return new ArrayList<>();
    }
  }

  @RestLiCollection(name = "duplicateServiceErrorParameters")
  @ServiceErrorDef(SampleServiceError.class)
  class DuplicateServiceErrorParametersResource implements KeyValueResource<Long, EmptyRecord>
  {
    @Finder(value = "query")
    @ParamError(code = SampleServiceError.Codes.ERROR_A, parameterNames = { "param", "param" })
    public List<EmptyRecord> query(@QueryParam("param") Integer param)
    {
      return new ArrayList<>();
    }
  }

  @RestLiCollection(name = "duplicateServiceErrorParamErrorCodes")
  @ServiceErrorDef(SampleServiceError.class)
  class DuplicateServiceErrorParamErrorCodesResource implements KeyValueResource<Long, EmptyRecord>
  {
    @Finder(value = "query")
    @ParamError(code = SampleServiceError.Codes.ERROR_A, parameterNames = { "param" })
    @ParamError(code = SampleServiceError.Codes.ERROR_A, parameterNames = { "param2" })
    public List<EmptyRecord> query(@QueryParam("param") Integer param, @QueryParam("param2") String param2)
    {
      return new ArrayList<>();
    }
  }

  @RestLiCollection(name = "redundantServiceErrorCodeWithParameter")
  @ServiceErrorDef(SampleServiceError.class)
  class RedundantServiceErrorCodeWithParameterResource implements KeyValueResource<Long, EmptyRecord>
  {
    @Finder(value = "query")
    @ServiceErrors({ SampleServiceError.Codes.ERROR_A })
    @ParamError(code = SampleServiceError.Codes.ERROR_A, parameterNames = { "param" })
    public List<EmptyRecord> query(@QueryParam("param") Integer param)
    {
      return new ArrayList<>();
    }
  }

  @RestLiCollection(name = "invalidSuccessStatusesResource")
  @ServiceErrorDef(SampleServiceError.class)
  class InvalidSuccessStatusesResource implements KeyValueResource<Long, EmptyRecord>
  {
    @RestMethod.Get
    @SuccessResponse(statuses = { HttpStatus.S_200_OK, HttpStatus.S_500_INTERNAL_SERVER_ERROR })
    public EmptyRecord get(Long id)
    {
      return new EmptyRecord();
    }
  }

  @RestLiCollection(name = "emptySuccessStatusesResource")
  @ServiceErrorDef(SampleServiceError.class)
  class EmptySuccessStatusesResource implements KeyValueResource<Long, EmptyRecord>
  {
    @RestMethod.Get
    @SuccessResponse(statuses = {})
    public EmptyRecord get(Long id)
    {
      return new EmptyRecord();
    }
  }

  @RestLiActions(name = "actionReturnTypeInteger")
  class ActionReturnTypeIntegerResource
  {
    @Action(name = "int")
    public int doInt()
    {
      return 1;
    }

    @Action(name = "actionResultInt")
    public ActionResult<Integer> doActionResultInt()
    {
      return new ActionResult<>(1);
    }

    @Action(name = "taskActionResultInt")
    public Task<ActionResult<Integer>> doTaskActionResultInt()
    {
      return Task.value(new ActionResult<>(1));
    }

    @Action(name = "taskInt")
    public Task<Integer> doTaskInt()
    {
      return Task.value(1);
    }

    @Action(name = "promiseInt")
    public Promise<Integer> doPromiseInt()
    {
      return null;
    }

    @Action(name = "callbackActionResultInt")
    public void doCallbackActionResultInt(@CallbackParam Callback<ActionResult<Integer>> callback) {}
  }

  @RestLiActions(name = "actionReturnTypeRecord")
  class ActionReturnTypeRecordResource
  {
    @Action(name = "record")
    public EmptyRecord doRecord()
    {
      return new EmptyRecord();
    }

    @Action(name = "actionResultRecord")
    public ActionResult<EmptyRecord> doActionResultRecord()
    {
      return new ActionResult<>(new EmptyRecord());
    }

    @Action(name = "taskActionResultRecord")
    public Task<ActionResult<EmptyRecord>> doTaskActionResultRecord()
    {
      return Task.value(new ActionResult<>(new EmptyRecord()));
    }

    @Action(name = "callbackRecord")
    public void doCallbackRecord(@CallbackParam Callback<EmptyRecord> callback) {}
  }

  @RestLiActions(name = "actionReturnTypeVoid")
  class ActionReturnTypeVoidResource
  {
    @Action(name = "void")
    public void doVoid() {}
  }

  /**
   * The following resources are used by {@link TestRestLiParameterAnnotations}.
   */

  @RestLiCollection(name = "CollectionFinderAttachmentParams")
  class CollectionFinderAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("attachmentsFinder")
    public List<EmptyRecord> AttachmentsFinder(@RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name = "CollectionGetAttachmentParams")
  class CollectionGetAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.Get
    public EmptyRecord get(String key, @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name = "CollectionBatchGetAttachmentParams")
  class CollectionBatchGetAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.BatchGet
    public Map<String, EmptyRecord> batchGet(Set<String> keys, @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name = "UnstructuredDataParams")
  class UnstructuredDataParams extends UnstructuredDataCollectionResourceReactiveTemplate<String> {
    @Override
    public void get(String key, @CallbackParam Callback<UnstructuredDataReactiveResult> callback) { }

    @Override
    public void create(@UnstructuredDataReactiveReaderParam UnstructuredDataReactiveReader reader, @CallbackParam Callback<CreateResponse> callback) { }
  }

  @RestLiCollection(name = "CollectionDeleteAttachmentParams")
  class CollectionDeleteAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.Delete
    public UpdateResponse delete(String key, @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name = "CollectionBatchDeleteAttachmentParams")
  class CollectionBatchDeleteAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.BatchDelete
    public BatchUpdateResult<String, EmptyRecord> batchDelete(BatchDeleteRequest<String, EmptyRecord> ids,
        @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name = "CollectionGetAllAttachmentParams")
  class CollectionGetAllAttachmentParams extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.GetAll
    public List<EmptyRecord> getAll(@PagingContextParam PagingContext pagingContext, @RestLiAttachmentsParam RestLiAttachmentReader reader)
    {
      return null;
    }
  }

  @RestLiCollection(name = "collectionMultipleAttachmentParamsFailureResource")
  class CollectionMultipleAttachmentParamsFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Action(name = "MultipleAttachmentParams")
    public void MultipleAttachmentParams(@RestLiAttachmentsParam RestLiAttachmentReader attachmentReaderA,
        @RestLiAttachmentsParam RestLiAttachmentReader attachmentReaderB)
    {
    }
  }

  @RestLiCollection(name = "paramsNotAnnotatedFailureResource")
  class ParamsNotAnnotatedFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @RestMethod.Get
    public ParamsNotAnnotatedFailureResource get(String key, Long dummyParam) { return null; }
  }

  @RestLiCollection(name = "collectionSuccessResource")
  class CollectionSuccessResource extends CollectionResourceTemplate<String, EmptyRecord>
  {

    @Finder("PagingContextParamFinder")
    public List<EmptyRecord> PagingContextParamNewTest(@PagingContextParam PagingContext pagingContext)
    {
      return Collections.emptyList();
    }

    @Finder("PathKeysParamFinder")
    public List<EmptyRecord> PathKeysParamNewTest(@PathKeysParam PathKeys keys)
    {
      return Collections.emptyList();
    }

    @Finder("ProjectionParamFinder")
    public List<EmptyRecord> ProjectionParamDeprecatedTest(@ProjectionParam MaskTree projectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("MetadataProjectionParamFinder")
    public List<EmptyRecord> MetadataProjectionParamNewTest(@MetadataProjectionParam MaskTree metadataProjectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("PagingProjectionParamFinder")
    public List<EmptyRecord> PagingProjectionParamNewTest(@PagingProjectionParam MaskTree pagingProjectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("ResourceContextParamFinder")
    public List<EmptyRecord> ResourceContextParamNewTest(@ResourceContextParam ResourceContext resourceContext)
    {
      return Collections.emptyList();
    }

    public Promise<? extends String> ParseqContextParamNewTest(@ParSeqContextParam com.linkedin.parseq.Context parseqContext)
    {
      return null;
    }
  }

  @RestLiCollection(name = "collectionPagingContextParamFailureResource")
  class CollectionPagingContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("PagingContextParamIncorrectDataTypeFinder")
    public List<EmptyRecord> PagingContextParamIncorrectDataTypeTest(@PagingContextParam String pagingContext)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name = "collectionPathKeysFailureResource")
  class CollectionPathKeysFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("PathKeysParamIncorrectDataTypeFinder")
    public List<EmptyRecord> PathKeysParamIncorrectDataTypeTest(@PathKeysParam String keys)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name = "collectionProjectionParamFailureResource")
  class CollectionProjectionParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("ProjectionParamIncorrectDataTypeFinder")
    public List<EmptyRecord> ProjectionParamIncorrectDataTypeTest(@ProjectionParam String projectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("MetadataProjectionParamIncorrectDataTypeFinder")
    public List<EmptyRecord> MetadataProjectionParamIncorrectDataTypeTest(@MetadataProjectionParam String metadataProjectionParam)
    {
      return Collections.emptyList();
    }

    @Finder("PagingProjectionParamIncorrectDataTypeFinder")
    public List<EmptyRecord> PagingProjectionParamIncorrectDataTypeTest(@PagingProjectionParam String pagingProjectionParam)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name = "collectionResourceContextParamFailureResource")
  class CollectionResourceContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Finder("ResourceContextParamIncorrectDataTypeFinder")
    public List<EmptyRecord> ResourceContextParamIncorrectDataTypeTest(@ResourceContextParam String resourceContext)
    {
      return Collections.emptyList();
    }
  }

  @RestLiCollection(name = "collectionParseqContextParamFailureResource")
  class CollectionParseqContextParamFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    public Promise<? extends String> ParseqContextParamNewTest(@ParSeqContextParam String parseqContext)
    {
      return null;
    }
  }

  @RestLiCollection(name = "collectionAttachmentParamsFailureResource")
  class CollectionAttachmentParamsFailureResource extends CollectionResourceTemplate<String, EmptyRecord>
  {
    @Action(name = "AttachmentParamsIncorrectDataTypeAction")
    public void AttachmentParamsIncorrectDataTypeAction(@RestLiAttachmentsParam String attachmentReader)
    {
    }
  }

  @RestLiAssociation(name = "associationAsyncSuccessResource", assocKeys = {
    @Key(name = "AssocKey_Deprecated", type=String.class),
    @Key(name = "AssocKeyParam_New", type=String.class)
  })
  class AssociationAsyncSuccessResource extends AssociationResourceTemplate<EmptyRecord>
  {
    @Finder("assocKeyParamFinder")
    public List<EmptyRecord> assocKeyParamNewTest(@AssocKeyParam("AssocKeyParam_New") long key)
    {
      return Collections.emptyList();
    }
  }

  /**
   * The following resources are used by {@link TestRestLiTemplate}, and some by {@link TestResourceModel}.
   */

  @RestLiCollection(name="collectionCollection")
  class CollectionCollectionResource extends CollectionResourceTemplate<String, EmptyRecord> {}

  @RestLiCollection(name="collectionCollectionAsync")
  class CollectionCollectionAsyncResource extends CollectionResourceAsyncTemplate<String, EmptyRecord> {}

  @RestLiCollection(name="collectionComplexKey")
  class CollectionComplexKeyResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
    // Use full-qualified classname here since we cannot add @SuppressWarnings("deprecation") in import
  }

  @RestLiCollection(name="collectionComplexKeyAsync")
  class CollectionComplexKeyAsyncResource extends ComplexKeyResourceAsyncTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiCollection(name="collectionAssociation")
  class CollectionAssociationResource extends AssociationResourceTemplate<EmptyRecord> {}

  @RestLiCollection(name="collectionAssociationAsync")
  class CollectionAssociationAsyncResource extends AssociationResourceAsyncTemplate<EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiCollection(name="collectionAssociationPromise")
  class CollectionAssociationPromiseResource extends com.linkedin.restli.server.resources.AssociationResourcePromiseTemplate<EmptyRecord> {}

  @RestLiCollection(name="collectionAssociationTask")
  class CollectionAssociationTaskResource extends AssociationResourceTaskTemplate<EmptyRecord> {}

  @RestLiCollection(name="collectionSimple")
  class CollectionSimpleResource extends SimpleResourceTemplate<EmptyRecord> {}

  @RestLiCollection(name="collectionSimpleAsync")
  class CollectionSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord> {}

  @RestLiCollection(name="collectionSimpleTask")
  class CollectionSimpleTaskResource extends SimpleResourceTaskTemplate<EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiCollection(name="collectionSimplePromise")
  class CollectionSimplePromiseResource extends com.linkedin.restli.server.resources.SimpleResourcePromiseTemplate<EmptyRecord> {}

  @RestLiAssociation(name="associationCollection", assocKeys = {})
  class AssociationCollectionResource extends CollectionResourceTemplate<String, EmptyRecord> {}

  @RestLiAssociation(name="associationCollectionAsync", assocKeys = {})
  class AssociationCollectionAsyncResource extends CollectionResourceAsyncTemplate<String, EmptyRecord> {}

  @RestLiAssociation(name="associationCollectionTask", assocKeys = {})
  class AssociationCollectionTaskResource extends CollectionResourceTaskTemplate<String, EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiAssociation(name="associationCollectionPromise", assocKeys = {})
  class AssociationCollectionPromiseResource extends com.linkedin.restli.server.resources.CollectionResourcePromiseTemplate<String, EmptyRecord> {}

  @RestLiAssociation(name="associationComplexKey", assocKeys = {})
  class AssociationComplexKeyResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiAssociation(name="associationComplexKeyAsync", assocKeys = {})
  class AssociationComplexKeyAsyncResource extends ComplexKeyResourceAsyncTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiAssociation(name="associationComplexKeyPromise", assocKeys = {})
  class AssociationComplexKeyPromiseResource extends com.linkedin.restli.server.resources.ComplexKeyResourcePromiseTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiAssociation(name="associationComplexKeyTask", assocKeys = {})
  class AssociationComplexKeyTaskResource extends ComplexKeyResourceTaskTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiAssociation(name="associationAssociation", assocKeys = {
    @Key(name="src", type=String.class),
    @Key(name="dest", type=String.class)
  })
  class AssociationAssociationResource extends AssociationResourceTemplate<EmptyRecord> {}

  @RestLiAssociation(name="associationAssociationAsync", assocKeys = {
    @Key(name="src", type=String.class),
    @Key(name="dest", type=String.class)
  })
  class AssociationAssociationAsyncResource extends AssociationResourceAsyncTemplate<EmptyRecord> {}


  @RestLiAssociation(name="associationAssociationTask", assocKeys = {
    @Key(name="src", type=String.class),
    @Key(name="dest", type=String.class)
  })
  class AssociationAssociationTaskResource extends AssociationResourceTaskTemplate<EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiAssociation(name="associationAssociationPromise", assocKeys = {
    @Key(name="src", type=String.class),
    @Key(name="dest", type=String.class)
  })
  class AssociationAssociationPromiseResource extends com.linkedin.restli.server.resources.AssociationResourcePromiseTemplate<EmptyRecord> {}

  @RestLiAssociation(name="associationSimple", assocKeys = {})
  class AssociationSimpleResource extends SimpleResourceTemplate<EmptyRecord> {}

  @RestLiAssociation(name="associationSimpleAsync", assocKeys = {})
  class AssociationSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord> {}

  @RestLiAssociation(name="associationSimpleTask", assocKeys = {})
  class AssociationSimpleTaskResource extends SimpleResourceTaskTemplate<EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiAssociation(name="associationSimplePromise", assocKeys = {})
  class AssociationSimplePromiseResource extends com.linkedin.restli.server.resources.SimpleResourcePromiseTemplate<EmptyRecord> {}

  @RestLiSimpleResource(name="simpleCollection")
  class SimpleCollectionResource extends CollectionResourceTemplate<String, EmptyRecord> {}

  @RestLiSimpleResource(name="simpleCollectionAsync")
  class SimpleCollectionAsyncResource extends CollectionResourceAsyncTemplate<String, EmptyRecord> {}

  @RestLiSimpleResource(name="simpleCollectionTask")
  class SimpleCollectionTaskResource extends CollectionResourceTaskTemplate<String, EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiSimpleResource(name="simpleCollectionPromise")
  class SimpleCollectionPromiseResource extends com.linkedin.restli.server.resources.CollectionResourcePromiseTemplate<String, EmptyRecord> {}

  @RestLiSimpleResource(name="simpleComplexKey")
  class SimpleComplexKeyResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiSimpleResource(name="simpleComplexKeyAsync")
  class SimpleComplexKeyAsyncResource extends ComplexKeyResourceAsyncTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiSimpleResource(name="simpleComplexKeyPromise")
  class SimpleComplexKeyPromiseResource extends com.linkedin.restli.server.resources.ComplexKeyResourcePromiseTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiSimpleResource(name="simpleComplexKeyTask")
  class SimpleComplexKeyTaskResource extends ComplexKeyResourceTaskTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiSimpleResource(name="simpleAssociation")
  class SimpleAssociationResource extends AssociationResourceTemplate<EmptyRecord> {}

  @RestLiSimpleResource(name="simpleAssociationAsync")
  class SimpleAssociationAsyncResource extends AssociationResourceAsyncTemplate<EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiSimpleResource(name="simpleAssociationPromise")
  class SimpleAssociationPromiseResource extends com.linkedin.restli.server.resources.AssociationResourcePromiseTemplate<EmptyRecord> {}

  @RestLiSimpleResource(name="simpleAssociationTask")
  class SimpleAssociationTaskResource extends AssociationResourceTaskTemplate<EmptyRecord> {}

  @RestLiSimpleResource(name="simpleSimple")
  class SimpleSimpleResource extends SimpleResourceTemplate<EmptyRecord> {}

  @RestLiSimpleResource(name="simpleSimpleAsync")
  class SimpleSimpleAsyncResource extends SimpleResourceAsyncTemplate<EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiSimpleResource(name="simpleSimplePromise")
  class SimpleSimplePromiseResource extends com.linkedin.restli.server.resources.SimpleResourcePromiseTemplate<EmptyRecord> {}

  @RestLiSimpleResource(name="simpleSimpleTask")
  class SimpleSimpleTaskResource extends SimpleResourceTaskTemplate<EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiCollection(name = "collectionPromise")
  class CollectionCollectionPromise extends com.linkedin.restli.server.resources.CollectionResourcePromiseTemplate<String, EmptyRecord> {}

  @RestLiCollection(name = "collectionTask")
  class CollectionCollectionTask extends CollectionResourceTaskTemplate<String, EmptyRecord> {}

  @SuppressWarnings("deprecation")
  @RestLiCollection(name = "collectionComplexKeyPromise")
  class CollectionComplexKeyPromise extends com.linkedin.restli.server.resources.ComplexKeyResourcePromiseTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiCollection(name = "collectionComplexKeyTask")
  class CollectionComplexKeyTask extends ComplexKeyResourceTaskTemplate<EmptyRecord, EmptyRecord, EmptyRecord> {}

  @RestLiCollection(name = "lucky", keyName = "dayOfWeek")
  public class FinderUnsupportedKeyUnstructuredDataResource extends UnstructuredDataCollectionResourceTemplate<Integer>
  {
    @Finder("key")
    public List<String> findLucky(@PagingContextParam final PagingContext context,
                                 @QueryParam("dayOfWeek") Integer dayOfWeek) throws Exception
    {
      return Collections.singletonList("finderReturns");
    }
  }

  @RestLiSimpleResource(name="single")
  public class FinderUnsupportedSingleUnstructuredDataResource extends UnstructuredDataSimpleResourceTemplate
  {
    @Finder("single")
    public List<EmptyRecord> findLucky(@PagingContextParam final PagingContext context,
                                      @QueryParam("dayOfWeek") Integer dayOfWeek) throws Exception
    {
      return Collections.singletonList(new EmptyRecord());
    }
  }

  @RestLiAssociation(
      name="associate",
      assocKeys={@Key(name="followerID", type=long.class), @Key(name="followeeID", type=long.class)})
  public class FinderSupportedAssociationDataResource extends AssociationResourceTemplate<EmptyRecord>
  {
    @Finder("associate")
    public List<EmptyRecord> find(@PagingContextParam final PagingContext context,
        @QueryParam("dayOfWeek") Integer dayOfWeek) throws Exception
    {
      return Collections.singletonList(new EmptyRecord());
    }
  }

  @RestLiCollection(name="collectionComplexKey")
  public class FinderSupportedComplexKeyDataResource extends ComplexKeyResourceTemplate<EmptyRecord, EmptyRecord, EmptyRecord>
  {
    @Finder("complex")
    public List<EmptyRecord> find(@PagingContextParam final PagingContext context,
                                  @QueryParam("dayOfWeek") Integer dayOfWeek) throws Exception
    {
      return Collections.singletonList(new EmptyRecord());
    }
  }
}

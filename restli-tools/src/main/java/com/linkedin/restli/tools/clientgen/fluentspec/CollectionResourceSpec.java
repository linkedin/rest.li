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

package com.linkedin.restli.tools.clientgen.fluentspec;

import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.pegasus.generator.TemplateSpecGenerator;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.restli.client.BatchCreateIdEntityRequest;
import com.linkedin.restli.client.BatchCreateIdRequest;
import com.linkedin.restli.client.BatchDeleteRequest;
import com.linkedin.restli.client.BatchGetEntityRequest;
import com.linkedin.restli.client.BatchPartialUpdateEntityRequest;
import com.linkedin.restli.client.BatchPartialUpdateRequest;
import com.linkedin.restli.client.BatchUpdateRequest;
import com.linkedin.restli.client.CreateIdEntityRequest;
import com.linkedin.restli.client.CreateIdRequest;
import com.linkedin.restli.client.DeleteRequest;
import com.linkedin.restli.client.GetAllRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.PartialUpdateEntityRequest;
import com.linkedin.restli.client.PartialUpdateRequest;
import com.linkedin.restli.client.UpdateRequest;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchCreateIdEntityResponse;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.KeyValueRecord;
import com.linkedin.restli.common.KeyValueRecordFactory;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.client.BatchCreateIdDecoder;
import com.linkedin.restli.internal.client.BatchCreateIdEntityDecoder;
import com.linkedin.restli.internal.client.BatchEntityResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.IdEntityResponseDecoder;
import com.linkedin.restli.internal.client.IdResponseDecoder;
import com.linkedin.restli.restspec.ResourceSchema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class CollectionResourceSpec extends BaseResourceSpec
{
  private ClassTemplateSpec _keyClass;
  private ClassTemplateSpec _entityClass;
  public CollectionResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver)
  {
    super(resourceSchema, templateSpecGenerator, sourceIdlName, schemaResolver);
  }

  public List<ActionMethodSpec> getActions()
  {
    if (getResource().getCollection().getActions() == null)
    {
      return Collections.emptyList();
    }
    List<ActionMethodSpec> actions = new ArrayList<>(getResource().getCollection().getActions().size());
    getResource().getCollection().getActions().forEach(actionSchema -> actions.add(new ActionMethodSpec(actionSchema, this)));
    return actions;
  }

  public List<RestMethodSpec> getRestMethods()
  {
    List<RestMethodSpec> methods = new ArrayList<>(getResource().getCollection().getMethods().size());
    getResource().getCollection().getMethods().forEach(restMethodSchema -> methods.add(new RestMethodSpec(restMethodSchema, this)));
    return methods;
  }

  public ClassTemplateSpec getKeyClass()
  {
    if (_keyClass == null)
    {
      _keyClass = classToTemplateSpec(getResource().getCollection().getIdentifier().getType());
    }
    return _keyClass;
  }

  public String getKeyClassName()
  {
    return SpecUtils.getClassName(getKeyClass());
  }

  public ClassTemplateSpec getEntityClass()
  {
    if (_entityClass == null)
    {
      _entityClass = classToTemplateSpec(getResource().getSchema());
    }
    return _entityClass;
  }

  public String getEntityClassName()
  {
    return getEntityClass().getClassName();
  }

  public String getIdName()
  {
    return getResource().getCollection().getIdentifier().getName();
  }

  public Set<String> getImportsForRestMethods()
  {
    Set<String> imports = new TreeSet<>();
    for(RestMethodSpec methodSpec : getRestMethods())
    {
      ResourceMethod method = ResourceMethod.fromString(methodSpec.getMethod());
      switch (method)
      {
        case GET:
          imports.add(GetRequest.class.getName());
          break;
        case BATCH_GET:
          imports.add(BatchGetEntityRequest.class.getName());
          imports.add(BatchKVResponse.class.getName());
          imports.add(EntityResponse.class.getName());
          imports.add(BatchEntityResponseDecoder.class.getName());
          break;
        case CREATE:
          imports.add(CreateIdRequest.class.getName());
          imports.add(IdResponse.class.getName());
          imports.add(IdResponseDecoder.class.getName());
          if (methodSpec.returnsEntity())
          {
            imports.add(CreateIdEntityRequest.class.getName());
            imports.add(IdEntityResponse.class.getName());
            imports.add(IdEntityResponseDecoder.class.getName());
          }
          break;
        case BATCH_CREATE:
          imports.add(CollectionRequest.class.getName());
          imports.add(BatchCreateIdRequest.class.getName());
          imports.add(CreateIdStatus.class.getName());
          imports.add(BatchCreateIdResponse.class.getName());
          imports.add(BatchCreateIdDecoder.class.getName());
          if (methodSpec.returnsEntity())
          {
            imports.add(BatchCreateIdEntityRequest.class.getName());
            imports.add(CreateIdEntityStatus.class.getName());
            imports.add(BatchCreateIdEntityResponse.class.getName());
            imports.add(BatchCreateIdEntityDecoder.class.getName());
          }
          break;
        case PARTIAL_UPDATE:
          imports.add(PatchRequest.class.getName());
          imports.add(PartialUpdateRequest.class.getName());
          if (methodSpec.returnsEntity())
          {
            imports.add(PartialUpdateEntityRequest.class.getName());
            imports.add(EntityResponseDecoder.class.getName());
          }
          break;
        case BATCH_PARTIAL_UPDATE:
          imports.add(PatchRequest.class.getName());
          imports.add(BatchPartialUpdateRequest.class.getName());
          imports.add(CollectionRequest.class.getName());
          imports.add(UpdateStatus.class.getName());
          imports.add(BatchKVResponse.class.getName());
          imports.add(KeyValueRecordFactory.class.getName());
          imports.add(KeyValueRecord.class.getName());
          if (methodSpec.returnsEntity())
          {
            imports.add(BatchPartialUpdateEntityRequest.class.getName());
            imports.add(UpdateEntityStatus.class.getName());
          }
          break;
        case UPDATE:
          imports.add(UpdateRequest.class.getName());
          break;
        case BATCH_UPDATE:
          imports.add(BatchUpdateRequest.class.getName());
          imports.add(BatchKVResponse.class.getName());
          imports.add(KeyValueRecordFactory.class.getName());
          imports.add(KeyValueRecord.class.getName());
          break;
        case DELETE:
          imports.add(DeleteRequest.class.getName());
          break;
        case BATCH_DELETE:
          imports.add(BatchDeleteRequest.class.getName());
          imports.add(UpdateStatus.class.getName());
          break;
        case GET_ALL:
          imports.add(GetAllRequest.class.getName());
          imports.add(CollectionResponse.class.getName());
          break;
        default:
          break;
      }
    }
    return imports;
  }
}

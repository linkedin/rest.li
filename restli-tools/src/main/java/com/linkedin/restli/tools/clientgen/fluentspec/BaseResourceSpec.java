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

import com.linkedin.data.ByteString;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
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
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.util.CustomTypeUtil;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class BaseResourceSpec
{
  final ResourceSchema _resource;
  final TemplateSpecGenerator _templateSpecGenerator;
  final String _sourceIdlName;
  final DataSchemaLocation _currentSchemaLocation;
  final DataSchemaResolver _schemaResolver;
  ClassTemplateSpec _entityClass = null;
  // In case resource name and entity class are conflicting, will need to use full entity class name
  final boolean _resourceNameConflictWithEntityClass;


  public BaseResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver)
  {
    _resource = resourceSchema;
    _templateSpecGenerator = templateSpecGenerator;
    _sourceIdlName = sourceIdlName;
    _schemaResolver = schemaResolver;
    _currentSchemaLocation = new FileDataSchemaLocation(new File(_sourceIdlName));
    _resourceNameConflictWithEntityClass = getEntityClass().getClassName().equals(getClassName());
  }

  public ResourceSchema getResource()
  {
    return _resource;
  }

  public ResourceSchemaArray getSubResources()
  {
    return _resource.getEntity().getSubResources();
  }

  public TemplateSpecGenerator getTemplateSpecGenerator()
  {
    return _templateSpecGenerator;
  }

  public String getSourceIdlName()
  {
    return _sourceIdlName;
  }

  public String getClassName()
  {
    return RestLiToolsUtils.nameCapsCase(_resource.getName());
  }

  public String getNamespace()
  {
    return _resource.hasNamespace() ? _resource.getNamespace() : "";
  }

  protected ClassTemplateSpec classToTemplateSpec(String classname)
  {
    if (classname == null || "Void".equals(classname))
    {
      return null;
    }
    else
    {
      final DataSchema typeSchema = RestSpecCodec.textToSchema(classname, _schemaResolver);
      return schemaToTemplateSpec(typeSchema);
    }
  }

  protected ClassTemplateSpec schemaToTemplateSpec(DataSchema dataSchema)
  {
    // convert from DataSchema to ClassTemplateSpec
    return _templateSpecGenerator.generate(dataSchema, _currentSchemaLocation);
  }

  // For Collection/Simple/Association/ActionSet specific resource imports
  protected Set<String> getResourceSpecificImports(Set<String> imports)
  {
    return imports;
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
          imports.add(CollectionRequest.class.getName());
          imports.add(UpdateStatus.class.getName());
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
    return getResourceSpecificImports(imports);
  }

  // get the class representing the record entity of this resource
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
    return _resourceNameConflictWithEntityClass ? getEntityClass().getBindingName() : getEntityClass().getClassName();
  }

  public List<RestMethodSpec> getRestMethods()
  {
    return Collections.emptyList();
  }

  public List<ActionMethodSpec> getActions()
  {
    return Collections.emptyList();
  }

  protected String getJavaBindTypeName(String typeSchema)
  {
    DataSchema dataschema = RestSpecCodec.textToSchema(typeSchema, _schemaResolver, SchemaFormatType.PDL);
    return getJavaBindTypeName(dataschema);
  }

  /**
   * Given a data schema, get the java bind class name, typeref schemas will be de-referenced.
   */
  protected String getJavaBindTypeName(DataSchema dataschema)
  {
    if (dataschema instanceof TyperefDataSchema)
    {
      final TyperefDataSchema typerefDataSchema = (TyperefDataSchema) dataschema;
      if (typerefDataSchema.getDereferencedDataSchema().getType() != DataSchema.Type.UNION)
      {
        final String javaClassNameFromSchema = CustomTypeUtil.getJavaCustomTypeClassNameFromSchema(typerefDataSchema);
        if (javaClassNameFromSchema != null)
        {
          return javaClassNameFromSchema;
        }
        else
        {
          return getJavaBindTypeName(typerefDataSchema.getRef());
        }
      }
    }
    return getClassRefNameForSchema(dataschema);
  }

  protected String getClassRefNameForSchema(String schema)
  {
    DataSchema dataschema = RestSpecCodec.textToSchema(schema, _schemaResolver, SchemaFormatType.PDL);
    return getClassRefNameForSchema(dataschema);
  }

  /**
   * Given a schema, get the type that represents it
   */
  protected String getClassRefNameForSchema(DataSchema schema)
  {
    if (schema instanceof NamedDataSchema)
    {
      return ((NamedDataSchema) schema).getBindingName();
    }
    else if (schema instanceof PrimitiveDataSchema)
    {
      String primitiveBoxedType;
      switch (schema.getType())
      {
        case INT:
          primitiveBoxedType = Integer.class.getName();
          break;

        case DOUBLE:
          primitiveBoxedType = Double.class.getName();
          break;

        case BOOLEAN:
          primitiveBoxedType = Boolean.class.getName();
          break;

        case STRING:
          primitiveBoxedType = String.class.getName();
          break;

        case LONG:
          primitiveBoxedType = Long.class.getName();
          break;

        case FLOAT:
          primitiveBoxedType = Float.class.getName();
          break;

        case BYTES:
          primitiveBoxedType = ByteString.class.getName();
          break;

        default:
          throw new RuntimeException("Not supported primitive: " + schema.getType().name());
      }

      return primitiveBoxedType;
    }
    else
    {
      return schemaToTemplateSpec(schema).getBindingName();
    }
  }

  public boolean isResourceNameConflictWithEntityClass()
  {
    return _resourceNameConflictWithEntityClass;
  }

}

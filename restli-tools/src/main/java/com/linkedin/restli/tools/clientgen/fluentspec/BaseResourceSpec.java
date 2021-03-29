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
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.FieldDef;
import com.linkedin.pegasus.generator.TemplateSpecGenerator;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.restli.client.ActionRequest;
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
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.BatchCreateIdEntityResponse;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
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
import com.linkedin.restli.internal.client.ActionResponseDecoder;
import com.linkedin.restli.internal.client.BatchCreateIdDecoder;
import com.linkedin.restli.internal.client.BatchCreateIdEntityDecoder;
import com.linkedin.restli.internal.client.BatchEntityResponseDecoder;
import com.linkedin.restli.internal.client.EntityResponseDecoder;
import com.linkedin.restli.internal.client.IdEntityResponseDecoder;
import com.linkedin.restli.internal.client.IdResponseDecoder;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.ResourceSchemaArray;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.util.CustomTypeUtil;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;


public class BaseResourceSpec
{
  final ResourceSchema _resource;
  final TemplateSpecGenerator _templateSpecGenerator;
  final String _sourceIdlName;
  final DataSchemaLocation _currentSchemaLocation;
  final DataSchemaResolver _schemaResolver;
  ClassTemplateSpec _entityClass = null;
  private String _entityClassName = null;
  protected Set<String> _imports;
  // This map contains a mapping from those used short names to its full java binding name
  // In case of naming conflict, since resource name will be using shortened name,
  // others (entity name, complex key, etc) will be using full qualified name
  protected Map<String, String> _importCheckConflict;
  // sub-resources of this resource
  protected List<BaseResourceSpec> _childSubResourceSpecs;
  // All of the direct ancestors of this resource
  protected List<BaseResourceSpec> _ancestorResourceSpecs;
  protected List<String> _pathKeys;
  protected Map<String, String> _pathKeyTypes;
  protected Map<String, List<Pair<String,String>>> _pathToAssocKeys;

  public BaseResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver)
  {
    _resource = resourceSchema;
    _templateSpecGenerator = templateSpecGenerator;
    _sourceIdlName = sourceIdlName;
    _schemaResolver = schemaResolver;
    _currentSchemaLocation = new FileDataSchemaLocation(new File(_sourceIdlName));
    // In case any other class name conflicting with resource name
    // it will need to use full class name
    _importCheckConflict = new HashMap<>();
    _importCheckConflict.put(getClassName(), getBindingName());
  }

  public ResourceSchema getResource()
  {
    return _resource;
  }

  /**
   * Only Collection, Simple and AssociationResource could have subResources
   */
  public ResourceSchemaArray getSubResources()
  {
    return null;
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

  /**
   * To concatenate namespace and class name
   */
  public String getBindingName()
  {
    return getNamespace().equals("")?  getClassName(): getNamespace() + "." + getClassName();
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
    for (BaseResourceSpec spec : _ancestorResourceSpecs)
    {
      // Import Compound key if any of the descendents or ancestors are Association Resource;
      if (spec instanceof AssociationResourceSpec)
      {
        imports.add(CompoundKey.class.getName());
      }
      // Import Complex key if any of the descendents or ancestors are Collection resource and has Complex Key;
      else if ((spec instanceof CollectionResourceSpec) && ((CollectionResourceSpec) spec).hasComplexKey())
      {
        imports.add(ComplexResourceKey.class.getName());
      }
    }

    for (BaseResourceSpec spec : _childSubResourceSpecs)
    {
        spec.getResourceSpecificImports(imports);
    }
    return imports;
  }

  public Set<String> getImportsForMethods()
  {
    if (_imports == null) {
      Set<String> imports = new TreeSet<>();
      if (getActions().size() > 0) {
        imports.add(ActionRequest.class.getName());
        imports.add(ActionResponse.class.getName());
        imports.add(ActionResponseDecoder.class.getName());
        imports.add(DynamicRecordTemplate.class.getName());
        imports.add(FieldDef.class.getName());
      }
      for (RestMethodSpec methodSpec : getRestMethods()) {
        ResourceMethod method = ResourceMethod.fromString(methodSpec.getMethod());
        switch (method) {
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
            if (methodSpec.returnsEntity()) {
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
            if (methodSpec.returnsEntity()) {
              imports.add(BatchCreateIdEntityRequest.class.getName());
              imports.add(CreateIdEntityStatus.class.getName());
              imports.add(BatchCreateIdEntityResponse.class.getName());
              imports.add(BatchCreateIdEntityDecoder.class.getName());
            }
            break;
          case PARTIAL_UPDATE:
            imports.add(PatchRequest.class.getName());
            imports.add(PartialUpdateRequest.class.getName());
            if (methodSpec.returnsEntity()) {
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
            if (methodSpec.returnsEntity()) {
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

      // Entity class has a higher priority to use short name
      // than complex key, etc.
      if (_entityClassName == null) {
        if (SpecUtils.checkIfShortNameConflictWithImports(_importCheckConflict, getEntityClass().getClassName(),
            getEntityClass().getBindingName())) {
          _entityClassName = getEntityClass().getFullName();
        } else {
          _importCheckConflict.put(getEntityClass().getClassName(), getEntityClass().getBindingName());
          imports.add(getEntityClass().getFullName());
          _entityClassName = getEntityClass().getClassName();
        }
      }

      // Sub resources are handled recursively
      _imports = getResourceSpecificImports(imports);
    }
    return _imports;
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
    if (_entityClassName == null)
    {
      // Need to initialize by checking all the import chain
      getImportsForMethods();
    }
    return _entityClassName;
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
    DataSchema dataschema = RestSpecCodec.textToSchema(typeSchema, _schemaResolver);
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
    DataSchema dataschema = RestSpecCodec.textToSchema(schema, _schemaResolver);
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

  /**
   * Use to store all subResource specs
   */
  public List<BaseResourceSpec> getChildSubResourceSpecs()
  {
    return _childSubResourceSpecs;
  }

  public void setChildSubResourceSpecs(List<BaseResourceSpec> childSubResourceSpecs)
  {
    this._childSubResourceSpecs = childSubResourceSpecs;
  }

  /**
   * For subResources, to keep a link to all the parent specs
   */
  public List<BaseResourceSpec> getAncestorResourceSpecs()
  {
    return _ancestorResourceSpecs;
  }

  public void setAncestorResourceSpecs(List<BaseResourceSpec> ancestorResourceSpecs)
  {
    this._ancestorResourceSpecs = ancestorResourceSpecs;
    // If ancestor resource has same name as Entity class name,
    // then also use full entity class name, because the interface file
    // need to have all ancestors and descendants' resource name, along with
    // entity name.
    _ancestorResourceSpecs.stream()
        .forEach(v -> _importCheckConflict.put(v.getClassName(), v.getBindingName()));
  }

  private boolean hasParent()
  {
    return getAncestorResourceSpecs().size() != 0;
  }

  public BaseResourceSpec getParent()
  {
    List<BaseResourceSpec> parents = getAncestorResourceSpecs();
    if (parents.size() == 0)
    {
      return null;
    }
    return parents.get(parents.size() - 1);
  }

  /**
   * During interface file rendering,
   * this method is used to check whether this resource's namespace conflicts with its immediate parent's.
   *
   * Check {@link com.linkedin.restli.tools.clientgen.FluentApiGenerator} for rules when subResource does not
   * use same namespace with its ancestors.
   */
  public String getParentNamespace()
  {
    return hasParent()? getParent().getNamespace(): "";
  }

  /**
   * For fluentClients, the sub-resources' interfaces
   * should be nested in their root parent resource interface file.
   *
   * Unless one subResource and its ancestors do not always have same namespace.
   * In this case, the namespace in the IDL will be used to generate
   * this conflicting subresource interface file.
   *
   * In this way, the fluentClient and the interface it is implementing will be in same namespace
   *
   * @return A proper name for the interface that FluentClient should be implementing, e.g.
   *         the "SuperSuper.Super.Base.Sub" in
   *         <code>SubFluentClient implements SuperSuper.Super.Base.Sub</code>
   */
  public String getToImplementInterfaceName()
  {
    List<BaseResourceSpec>  toCheck = new LinkedList<>(getAncestorResourceSpecs());
    toCheck.add(this);
    List<BaseResourceSpec> lineage = new LinkedList<>();

    for (BaseResourceSpec spec : toCheck)
    {
      if (lineage.size() > 0 &&
          !lineage.get(lineage.size() - 1).getNamespace().equals(spec.getNamespace()))
      {
        lineage.clear();
      }
      lineage.add(spec);
    }
    return lineage.stream().map(BaseResourceSpec::getClassName).collect(Collectors.joining("."));
  }

  /**
   * For sub-resources:
   * DiffKey is the pathKey segment
   * from this subResource's direct parent to this subResource
   *
   * This method will be called when constructing APIs for the subResource.
   * No diffKey implies that the parent is a simple resource
   */
  public String getDiffPathKey()
  {
    if (!hasParent())
    {
      return null;
    }
    BaseResourceSpec parent = getParent();

    List<String> pathKeys = getPathKeys();
    if (pathKeys.size() == parent.getPathKeys().size())
    {
      return null;
    }
    // PathKeys are sorted, return last one
    return pathKeys.get(pathKeys.size() - 1);
  }

  public List<String> getPathKeys()
  {
    return _pathKeys;
  }

  public void setPathKeys(List<String> pathKeys)
  {
    _pathKeys = pathKeys;
  }

  /**
   * Deduce pathKey to key types mapping from the ancestors
   */
  public Map<String, String> getPathKeyTypes()
  {
    if (_pathKeyTypes == null)
    {
      _pathKeyTypes = new HashMap<>();
      if (!hasParent())
      {
        return _pathKeyTypes;
      }
      else
      {
        for (BaseResourceSpec spec : _ancestorResourceSpecs)
        {
          if (spec instanceof CollectionResourceSpec )
          {
            _pathKeyTypes.put(((CollectionResourceSpec) spec).getIdName(),
                ((CollectionResourceSpec) spec).getKeyClassName());
          }
          else if (spec instanceof AssociationResourceSpec)
          {
            _pathKeyTypes.put(((AssociationResourceSpec) spec).getIdName(),
                CompoundKey.class.getSimpleName());

          }
        }
      }
    }
    return _pathKeyTypes;
  }

  /**
   * Deduce pathKey to assocKey binding types mapping
   */
  public Map<String, List<Pair<String, String>>> getPathToAssocKeys()
  {
    if(_pathToAssocKeys == null)
    {
      _pathToAssocKeys = new HashMap<>();
    }
    //TODO: similar to getPathKeyTypes
    return _pathToAssocKeys;
  }
}

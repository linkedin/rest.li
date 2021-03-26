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

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.pegasus.generator.TemplateSpecGenerator;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.AssocKeySchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.ResourceSchemaArray;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import com.linkedin.restli.restspec.RestSpecCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.ClassUtils.getShortClassName;


public class AssociationResourceSpec extends BaseResourceSpec
{
  private final CompoundKeySpec _compoundKeySpec;
  private Set<String> assockeyTypeImports = new LinkedHashSet<>(4); // import assocKeyTYpe if not primitive
  private List<ActionMethodSpec> _resourceActions;
  private List<ActionMethodSpec> _entityActions;


  public AssociationResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver)
  {
    super(resourceSchema, templateSpecGenerator, sourceIdlName, schemaResolver);

    _compoundKeySpec = new CompoundKeySpec();

    for (AssocKeySchema assocKey: getResource().getAssociation().getAssocKeys())
    {
      String assocKeyType = assocKey.getType();
      DataSchema typeSchema = RestSpecCodec.textToSchema(assocKeyType, _schemaResolver);
      String javaBindTypeFull = getJavaBindTypeName(typeSchema);
      String declaredTypeFull = getClassRefNameForSchema(typeSchema);
      _compoundKeySpec.addAssocKeySpec(
          assocKey.getName(),
          assocKeyType,
          addToImportsAndShorten(javaBindTypeFull, assockeyTypeImports),
          addToImportsAndShorten(declaredTypeFull, assockeyTypeImports));
    }
  }

  @Override
  public List<RestMethodSpec> getRestMethods()
  {
    RestMethodSchemaArray methodSchemaArray = getResource().getAssociation().getMethods();
    if (methodSchemaArray == null)
    {
      return Collections.emptyList();
    }

    List<RestMethodSpec> methods = new ArrayList<>(getResource().getAssociation().getMethods().size());
    for (RestMethodSchema methodSchema : getResource().getAssociation().getMethods())
    {
      String methodType = methodSchema.getMethod().toUpperCase();
      if (methodType.equals(ResourceMethod.CREATE.name()) || methodType.equals(ResourceMethod.BATCH_CREATE.name()))
      {
        // Association resource never supports create and batch_create
        // create and batch_create in association resource will be skipped for now
        continue;
      }
      methods.add(new RestMethodSpec(methodSchema, this));
    }
    return methods;
  }

  @Override
  public List<ActionMethodSpec> getActions()
  {
    return Stream.concat(getResourceActions().stream(), getEntityActions().stream())
                             .collect(Collectors.toList());
  }

  public List<ActionMethodSpec> getResourceActions()
  {
    if (_resourceActions == null)
    {
      if (getResource().getAssociation().getActions() == null)
      {
        _resourceActions = Collections.emptyList();
      }

      _resourceActions = new ArrayList<>(getResource().getAssociation().getActions().size());
      getResource().getAssociation()
        .getActions().forEach(actionSchema -> _resourceActions.add(new ActionMethodSpec(actionSchema, this, false)));
    }
    return _resourceActions;
  }

  /**
   * get action methods for entities in this association resource
   */
  public List<ActionMethodSpec> getEntityActions()
  {
    if (_entityActions == null)
    {
      ActionSchemaArray actionSchemaArray = getResource().getAssociation().getEntity().getActions();
      if (actionSchemaArray == null)
      {
        _entityActions = Collections.emptyList();
      }
      _entityActions = new ArrayList<>(actionSchemaArray.size());
      actionSchemaArray.forEach(actionSchema -> _entityActions.add(new ActionMethodSpec(actionSchema, this, true)));
    }

    return _entityActions;
  }

  public String getIdName()
  {
    return getResource().getAssociation().getIdentifier();
  }

  public String getKeyClassName()
  {
    return getShortClassName(CompoundKey.class.getName());
  }

  @Override
  public Set<String> getResourceSpecificImports(Set<String> imports)
  {
    imports = super.getResourceSpecificImports(imports);
    imports.add(CompoundKey.class.getName());
    imports.addAll(assockeyTypeImports);
    return imports;
  }

  public CompoundKeySpec getCompoundKeySpec()
  {
    return _compoundKeySpec;
  }

  /**
   * To return the shortened type name; If the type is not primitive,
   * this method will also add that to the imports set.
   * @param fullType the full type name being checked
   * @param imports the imports set to add to
   * @return shorted type name
   */
  private String addToImportsAndShorten(String fullType, Set<String> imports)
  {
    if (!fullType.startsWith("java.lang"))
    {
      imports.add(fullType);
    }
    return getShortClassName(fullType);
  }

  @Override
  public ResourceSchemaArray getSubResources()
  {
    return getResource().getAssociation().getEntity().getSubresources();
  }
}

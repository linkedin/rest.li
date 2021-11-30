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
import org.apache.commons.lang.ClassUtils;

import static org.apache.commons.lang3.ClassUtils.getShortClassName;


public class AssociationResourceSpec extends BaseResourceSpec
{
  private final CompoundKeySpec _compoundKeySpec;
  private Set<String> assockeyTypeImports = new LinkedHashSet<>(4); // import assocKeyTYpe if not primitive
  private List<ActionMethodSpec> _resourceActions;
  private List<ActionMethodSpec> _entityActions;
  private List<RestMethodSpec> _restMethods;
  private List<FinderMethodSpec> _finders;
  private List<BatchFinderMethodSpec> _batchFinders;

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
          addToImportsAndTryToShorten(javaBindTypeFull, assockeyTypeImports),
          addToImportsAndTryToShorten(declaredTypeFull, assockeyTypeImports));
    }
  }

  @Override
  public List<RestMethodSpec> getRestMethods()
  {
    if (_restMethods == null)
    {
      RestMethodSchemaArray methodSchemaArray = getResource().getAssociation().getMethods();
      if (methodSchemaArray == null)
      {
        _restMethods = Collections.emptyList();
        return _restMethods;
      }
      _restMethods = new ArrayList<>(methodSchemaArray.size());
      for (RestMethodSchema methodSchema : methodSchemaArray)
      {
        String methodType = methodSchema.getMethod().toUpperCase();
        if (methodType.equals(ResourceMethod.CREATE.name()) || methodType.equals(ResourceMethod.BATCH_CREATE.name()))
        {
          // Association resource never supports create and batch_create
          // create and batch_create in association resource will be skipped for now
          continue;
        }
        _restMethods.add(new RestMethodSpec(methodSchema, this));
      }
    }
    return _restMethods;
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
      else
      {
        _resourceActions = new ArrayList<>(getResource().getAssociation().getActions().size());
        getResource().getAssociation()
          .getActions().forEach(actionSchema -> _resourceActions.add(new ActionMethodSpec(actionSchema, this, false)));
      }
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
      else
      {
        _entityActions = new ArrayList<>(actionSchemaArray.size());
        actionSchemaArray.forEach(actionSchema -> _entityActions.add(new ActionMethodSpec(actionSchema, this, true)));
      }
    }

    return _entityActions;
  }

  public List<FinderMethodSpec> getFinders()
  {
    if (_finders == null)
    {
      if (getResource().getAssociation().getFinders() == null)
      {
        _finders =  Collections.emptyList();
        return _finders;
      }
      _finders = new ArrayList<>(getResource().getAssociation().getFinders().size());
      getResource().getAssociation()
          .getFinders()
          .forEach(finderSchema -> _finders.add(new FinderMethodSpec(finderSchema, this)));
    }
    return _finders;
  }

  public List<BatchFinderMethodSpec> getBatchFinders()
  {
    if (_batchFinders == null)
    {
      if (getResource().getAssociation().getBatchFinders() == null)
      {
        _batchFinders = Collections.emptyList();
        return _batchFinders;
      }
      _batchFinders = new ArrayList<>(getResource().getAssociation().getBatchFinders().size());
      getResource().getAssociation()
          .getBatchFinders()
          .forEach(finderSchema -> _batchFinders.add(new BatchFinderMethodSpec(finderSchema, this)));
    }
    return _batchFinders;
  }

  public String getIdName()
  {
    return getResource().getAssociation().getIdentifier();
  }

  public String getKeyClassName()
  {
    return getShortClassName(CompoundKey.class.getName());
  }

  public String getKeyClassDisplayName()
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
   * To return the shortened type name after attempting to shorten it.
   * If shortened, the full binding type will be added to the imports set
   *
   * Note: If the type is primitive, this method will also add that to the imports set now, but
   *       will be eventually filtered out in later stage
   * @param fullType the full type name being checked
   * @param assockeyTypeImports the imports set that the full type of assocKey part would be added to
   * @return shorted type name if shorten is allowed, otherwise full name
   */
  private String addToImportsAndTryToShorten(String fullType, Set<String> assockeyTypeImports)
  {
    String shortName = getShortClassName(fullType);
    if(!SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
        shortName, fullType))
    {
      assockeyTypeImports.add(fullType);
      return shortName;
    }
    else
    {
      return fullType;
    }
  }

  @Override
  public ResourceSchemaArray getSubResources()
  {
    return getResource().getAssociation().getEntity().getSubresources();
  }
}

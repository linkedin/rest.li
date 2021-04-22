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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.ResourceSchemaArray;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.ClassUtils;


public class CollectionResourceSpec extends BaseResourceSpec
{
  private ClassTemplateSpec _keyClass;
  private Boolean _useShortKeyClassName;
  private String _keyTypeRefClassName;
  private Boolean _useShortKeyTypeRefClassName;
  private final ComplexKeySpec _complexKeySpec;
  private List<ActionMethodSpec> _resourceActions;
  private List<ActionMethodSpec> _entityActions;
  private List<RestMethodSpec> _restMethods;
  private List<FinderMethodSpec> _finders;
  private List<BatchFinderMethodSpec> _batchFinders;


  public CollectionResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver, String keyParamTypeSchema)
  {
    super(resourceSchema, templateSpecGenerator, sourceIdlName, schemaResolver);
    _complexKeySpec = keyParamTypeSchema == null? null: new ComplexKeySpec(
        getResource().getCollection().getIdentifier().getType(),
        keyParamTypeSchema,
        this
    );

    String declaredKeyClassName = getClassRefNameForSchema(getResource().getCollection().getIdentifier().getType());
    String keyClassName = getKeyClassName();

    if (!hasComplexKey() &&
        !SpecUtils.checkIsSameClass(keyClassName, declaredKeyClassName))
    {
      _keyTypeRefClassName = declaredKeyClassName;
    }
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
      if (getResource().getCollection().getActions() == null)
      {
        _resourceActions = Collections.emptyList();
      }
      else
      {
        _resourceActions = new ArrayList<>(getResource().getCollection().getActions().size());
        getResource().getCollection().getActions()
            .forEach(actionSchema -> _resourceActions.add(new ActionMethodSpec(actionSchema, this, false)));
      }
    }
    return _resourceActions;
  }

  /**
   * get action methods for entities in this collection resource
   */
  public List<ActionMethodSpec> getEntityActions()
  {
    if (_entityActions == null)
    {
      ActionSchemaArray actionSchemaArray = getResource().getCollection().getEntity().getActions();
      if (actionSchemaArray == null)
      {
        _entityActions = Collections.emptyList();
      }
      else
      {
        _entityActions = new ArrayList<>(actionSchemaArray.size());
        actionSchemaArray
            .forEach(actionSchema -> _entityActions.add(new ActionMethodSpec(actionSchema, this, true)));
      }

    }

    return _entityActions;
  }

  public List<RestMethodSpec> getRestMethods()
  {
    if (_restMethods == null)
    {
      RestMethodSchemaArray methodSchemaArray = getResource().getCollection().getMethods();
      if (methodSchemaArray == null)
      {
        _restMethods = Collections.emptyList();
        return _restMethods;
      }
      _restMethods = new ArrayList<>(methodSchemaArray.size());
      getResource().getCollection()
          .getMethods()
          .forEach(restMethodSchema -> _restMethods.add(new RestMethodSpec(restMethodSchema, this)));
    }
    return _restMethods;
  }

  public List<FinderMethodSpec> getFinders()
  {
    if (_finders == null)
    {
      if (getResource().getCollection().getFinders() == null)
      {
        _finders =  Collections.emptyList();
        return _finders;
      }
      _finders = new ArrayList<>(getResource().getCollection().getFinders().size());
      getResource().getCollection()
          .getFinders()
          .forEach(finderSchema -> _finders.add(new FinderMethodSpec(finderSchema, this)));
    }
    return _finders;
  }

  public List<BatchFinderMethodSpec> getBatchFinders()
  {
    if (_batchFinders == null)
    {
      if (getResource().getCollection().getBatchFinders() == null)
      {
        _batchFinders = Collections.emptyList();
        return _batchFinders;
      }
      _batchFinders = new ArrayList<>(getResource().getCollection().getBatchFinders().size());
      getResource().getCollection()
          .getBatchFinders()
          .forEach(finderSchema -> _batchFinders.add(new BatchFinderMethodSpec(finderSchema, this)));
    }
    return _batchFinders;
  }

  // For simple key
  // Note: this will dereference TypeRef
  public ClassTemplateSpec getKeyClass()
  {
    if (_keyClass == null)
    {
      _keyClass = classToTemplateSpec(getResource().getCollection().getIdentifier().getType());
    }
    return _keyClass;
  }

  public boolean hasComplexKey()
  {
    return _complexKeySpec != null;
  }

  public boolean hasKeyTypeRef()
  {
    return  _keyTypeRefClassName != null;
  }

  public String getKeyClassName()
  {
    return getKeyClassName(true);
  }

  public String getKeyClassName(boolean parameterized)
  {
    if (hasComplexKey())
    {
      if (parameterized)
      {
        return _complexKeySpec.getParameterizedSignature();
      }
      else
      {
      return ComplexResourceKey.class.getSimpleName();
      }
    }
    // for simple key
    return SpecUtils.getClassName(getKeyClass());
  }

  public String getKeyClassDisplayName()
  {
    return getKeyClassDisplayName(true);
  }

  public String getKeyClassDisplayName(boolean parameterized)
  {
    if (hasComplexKey())
    {
      // Note: ClassUtils cannot shorten parameterized class
      return getKeyClassName(parameterized);
    }

    if(_useShortKeyClassName == null)
    {
      // Need to check here  && while importing due to
      // undeterministic order in template resolving
      _useShortKeyClassName = !SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
          ClassUtils.getShortClassName(getKeyClassName()), getKeyClassName());
    }
    return _useShortKeyClassName ? ClassUtils.getShortClassName(getKeyClassName(parameterized))
        : getKeyClassName(parameterized);
  }


  public String getKeyTypeRefClassName()
  {
    return _keyTypeRefClassName;
  }

  public String getKeyTypeRefClassDisplayName()
  {
    if (_useShortKeyTypeRefClassName == null)
    {
      // Need to check here  && while importing due to
      // undeterministic order in template resolving
      _useShortKeyTypeRefClassName = !SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
          ClassUtils.getShortClassName(getKeyClassName()), getKeyClassName());
    }
    return _useShortKeyTypeRefClassName? ClassUtils.getShortClassName(_keyTypeRefClassName)
        : _keyTypeRefClassName;
  }

  public String getIdName()
  {
    return getResource().getCollection().getIdentifier().getName();
  }

  @Override
  public Set<String> getResourceSpecificImports(Set<String> imports)
  {
    imports = super.getResourceSpecificImports(imports);
    if (hasKeyTypeRef())
    {
      if(!SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
          ClassUtils.getShortClassName(_keyTypeRefClassName), _keyTypeRefClassName))
      {
        imports.add(_keyTypeRefClassName);
        _useShortKeyTypeRefClassName = true;
      }

    }

    if (hasComplexKey())
    {
      imports.add(ComplexResourceKey.class.getName());
      if(!SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
          ClassUtils.getShortClassName(_complexKeySpec.getKeyKeyClassName()), _complexKeySpec.getKeyKeyClassName()))
      {
        imports.add(_complexKeySpec.getKeyKeyClassName());
        _complexKeySpec.setUseShortKeyKeyClassName(true);
      }
      if(!SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
          ClassUtils.getShortClassName(_complexKeySpec.getParamKeyClassName()), _complexKeySpec.getParamKeyClassName()))
      {
        imports.add(_complexKeySpec.getParamKeyClassName());
        _complexKeySpec.setUseShortParamKeyClassName(true);
      }
    }
    else
    {
      if(!SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
          ClassUtils.getShortClassName(getKeyClassName()), getKeyClassName()))
      {
        _useShortKeyClassName = true;
        imports.add(getKeyClassName());
      }

    }

    return imports;
  }
  @Override
  public ResourceSchemaArray getSubResources()
  {
    return getResource().getCollection().getEntity().getSubresources();
  }


  public ComplexKeySpec getComplexKeySpec() {
    return _complexKeySpec;
  }

}

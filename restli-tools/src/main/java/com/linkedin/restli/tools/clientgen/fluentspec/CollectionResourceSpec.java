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
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.ResourceSchemaArray;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.ClassUtils;


public class CollectionResourceSpec extends BaseResourceSpec
{
  private ClassTemplateSpec _keyClass;
  private final ComplexKeySpec _complexKeySpec;

  public CollectionResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver, String keyParamTypeSchema)
  {
    super(resourceSchema, templateSpecGenerator, sourceIdlName, schemaResolver);
    _complexKeySpec = keyParamTypeSchema == null? null: new ComplexKeySpec(
        getResource().getCollection().getIdentifier().getType(),
        keyParamTypeSchema,
        this
    );
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
    RestMethodSchemaArray methodSchemaArray = getResource().getCollection().getMethods();
    if (methodSchemaArray == null)
    {
      return Collections.emptyList();
    }

    List<RestMethodSpec> methods = new ArrayList<>(methodSchemaArray.size());
    getResource().getCollection().getMethods().forEach(restMethodSchema -> methods.add(new RestMethodSpec(restMethodSchema, this)));
    return methods;
  }

  // For simple key
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
        return _complexKeySpec.getParameterizedSignature(_importCheckConflict);
      }
      else
      {
      return ComplexResourceKey.class.getSimpleName();
      }
    }
    // for simple key
    return SpecUtils.getClassName(getKeyClass());
  }

  public String getIdName()
  {
    return getResource().getCollection().getIdentifier().getName();
  }

  @Override
  public Set<String> getResourceSpecificImports(Set<String> imports)
  {
    imports = super.getResourceSpecificImports(imports);
    // TODO: Handle keyTypeRefClass imports for ComplexKey
    if (hasComplexKey())
    {
      imports.add(ComplexResourceKey.class.getName());
      if(!SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
          ClassUtils.getShortClassName(_complexKeySpec.getKeyKeyClassName()), _complexKeySpec.getKeyKeyClassName()))
      {
        imports.add(_complexKeySpec.getKeyKeyClassName());
      }
      if(!SpecUtils.checkIfShortNameConflictAndUpdateMapping(_importCheckConflict,
          ClassUtils.getShortClassName(_complexKeySpec.getParamKeyClassName()), _complexKeySpec.getParamKeyClassName()))
      {
        imports.add(_complexKeySpec.getParamKeyClassName());
      }
    }

    return imports;
  }
  @Override
  public ResourceSchemaArray getSubResources()
  {
    return getResource().getCollection().getEntity().getSubresources();
  }
}

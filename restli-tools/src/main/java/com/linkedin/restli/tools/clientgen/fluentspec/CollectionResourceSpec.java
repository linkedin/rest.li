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
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.ResourceSchema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
}

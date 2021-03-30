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
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.ResourceSchemaArray;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SimpleResourceSpec extends BaseResourceSpec
{

  private ClassTemplateSpec _entityClass;

  // Simple resource only supports get, update/partial_update, delete and actions
  public SimpleResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver)
  {
    super(resourceSchema, templateSpecGenerator, sourceIdlName, schemaResolver);
  }

  @Override
  public List<RestMethodSpec> getRestMethods()
  {
    RestMethodSchemaArray methodSchemaArray = getResource().getSimple().getMethods();
    if (methodSchemaArray == null)
    {
      return Collections.emptyList();
    }

    List<RestMethodSpec> methods = new ArrayList<>(getResource().getSimple().getMethods().size());
    getResource().getSimple().getMethods().forEach(restMethodSchema -> methods.add(new RestMethodSpec(restMethodSchema, this)));
    return methods;
  }

  @Override
  public List<ActionMethodSpec> getActions()
  {
    if (getResource().getSimple().getActions() == null)
    {
      return Collections.emptyList();
    }
    List<ActionMethodSpec> actions = new ArrayList<>(getResource().getSimple().getActions().size());
    getResource().getSimple().getActions().forEach(actionSchema -> actions.add(new ActionMethodSpec(actionSchema, this)));
    return actions;
  }

  @Override
  public ResourceSchemaArray getSubResources()
  {
    return getResource().getSimple().getEntity().getSubresources();
  }
}

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ActionSetResourceSpec extends BaseResourceSpec
{
  private List<ActionMethodSpec> _resourceActions;

  public ActionSetResourceSpec(ResourceSchema resourceSchema, TemplateSpecGenerator templateSpecGenerator,
      String sourceIdlName, DataSchemaResolver schemaResolver)
  {
    super(resourceSchema, templateSpecGenerator, sourceIdlName, schemaResolver);
  }

  public List<ActionMethodSpec> getResourceActions()
  {
    if (_resourceActions == null) {
      if (getResource().getActionsSet().getActions() == null) {
        _resourceActions = Collections.emptyList();
      }

      _resourceActions = new ArrayList<>(getResource().getActionsSet().getActions().size());
      getResource().getActionsSet()
          .getActions()
          .forEach(actionSchema -> _resourceActions.add(new ActionMethodSpec(actionSchema, this, false)));
    }
    return _resourceActions;
  }

  @Override
  public List<ActionMethodSpec> getActions()
  {
    return getResourceActions();
  }

  @Override
  public ClassTemplateSpec getEntityClass()
  {
    return null;
  }

  public String getEntityClassName()
  {
    return Void.class.getName();
  }
}

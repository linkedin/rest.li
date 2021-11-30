/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.tools.clientgen.builderspec;


import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.ActionsSetSchema;

import java.util.ArrayList;
import java.util.List;


/**
 * Root Request builder metadata specification for {@link ActionsSetSchema}.
 *
 * @author Min Chen
 */
public class ActionSetRootBuilderSpec extends RootBuilderSpec
{
  private List<RootBuilderMethodSpec> _resourceActions;

  public ActionSetRootBuilderSpec(ResourceSchema resource)
  {
    super(resource);
  }

  public ActionSetRootBuilderSpec(String packageName, String className, String baseClassName, ResourceSchema resource)
  {
    super(packageName, className, baseClassName, resource);
  }

  public List<RootBuilderMethodSpec> getResourceActions()
  {
    return _resourceActions;
  }

  public void setResourceActions(List<RootBuilderMethodSpec> resourceActions)
  {
    _resourceActions = resourceActions;
  }

  @Override
  public List<RootBuilderMethodSpec> getMethods()
  {
    List<RootBuilderMethodSpec> methods = new ArrayList<>();
    if (_resourceActions != null)
    {
      methods.addAll(_resourceActions);
    }
    return methods;
  }
}

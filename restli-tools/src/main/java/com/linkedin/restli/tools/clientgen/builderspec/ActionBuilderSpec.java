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


import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ResourceSchema;

import java.util.ArrayList;
import java.util.List;


/**
 * Request builder metadata specification for {@link ActionSchema}.
 *
 * @author Min Chen
 */
public class ActionBuilderSpec extends RequestBuilderSpec
{
  private String _actionName;
  private List<ActionParamBindingMethodSpec> _actionParamMethods = new ArrayList<>();

  public ActionBuilderSpec(String actionName)
  {
    _actionName = actionName;
  }

  public ActionBuilderSpec(String packageName, String className, String baseClassName, ResourceSchema resource, String actionName)
  {
    super(packageName, className, baseClassName, resource);
    _actionName = actionName;
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.ACTION;
  }

  public String getActionName()
  {
    return _actionName;
  }

  public List<ActionParamBindingMethodSpec> getActionParamMethods()
  {
    return _actionParamMethods;
  }

  public void addActionParamMethod(ActionParamBindingMethodSpec actionParamMethod)
  {
    _actionParamMethods.add(actionParamMethod);
  }

  @Override
  public boolean hasBindingMethods()
  {
    return super.hasBindingMethods() || !_actionParamMethods.isEmpty();
  }
}

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
import com.linkedin.restli.restspec.SimpleSchema;

import java.util.ArrayList;
import java.util.List;


/**
 * Root Request builder metadata specification for {@link SimpleSchema}.
 *
 * @author Min Chen
 */
public class SimpleRootBuilderSpec extends RootBuilderSpec
{
  private List<RootBuilderMethodSpec> _restMethods;
  private List<RootBuilderMethodSpec> _resourceActions;
  private List<RootBuilderSpec> _subresources;

  public SimpleRootBuilderSpec(ResourceSchema resource)
  {
    super(resource);
  }

  public SimpleRootBuilderSpec(String packageName, String className, String baseClassName, ResourceSchema resource)
  {
    super(packageName, className, baseClassName, resource);
  }

  public List<RootBuilderMethodSpec> getRestMethods()
  {
    return _restMethods;
  }

  public void setRestMethods(List<RootBuilderMethodSpec> restMethods)
  {
    _restMethods = restMethods;
  }

  public List<RootBuilderMethodSpec> getResourceActions()
  {
    return _resourceActions;
  }

  public void setResourceActions(List<RootBuilderMethodSpec> resourceActions)
  {
    _resourceActions = resourceActions;
  }

  public List<RootBuilderSpec> getSubresources()
  {
    return _subresources;
  }

  public void setSubresources(List<RootBuilderSpec> subresources)
  {
    _subresources = subresources;
  }

  @Override
  public List<RootBuilderMethodSpec> getMethods()
  {
    List<RootBuilderMethodSpec> methods = new ArrayList<>();
    if (_restMethods != null)
    {
      methods.addAll(_restMethods);
    }
    if (_resourceActions != null)
    {
      methods.addAll(_resourceActions);
    }
    return methods;
  }
}

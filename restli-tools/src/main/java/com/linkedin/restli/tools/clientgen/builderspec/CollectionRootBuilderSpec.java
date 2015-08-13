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


import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.ResourceSchema;

import java.util.ArrayList;
import java.util.List;


/**
 * Root Request builder metadata specification for {@link CollectionSchema}.
 *
 * @author Min Chen
 */
public class CollectionRootBuilderSpec extends RootBuilderSpec
{
  private List<RootBuilderMethodSpec> _restMethods;
  private List<RootBuilderMethodSpec> _finders;
  private List<RootBuilderMethodSpec> _resourceActions;
  private List<RootBuilderMethodSpec> _entityActions;
  private List<RootBuilderSpec> _subresources;

  public CollectionRootBuilderSpec(ResourceSchema resource)
  {
    super(resource);
  }

  public CollectionRootBuilderSpec(String packageName, String className, String baseClassName, ResourceSchema resource, CollectionSchema collection)
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

  public List<RootBuilderMethodSpec> getFinders()
  {
    return _finders;
  }

  public void setFinders(List<RootBuilderMethodSpec> finders)
  {
    _finders = finders;
  }

  public List<RootBuilderMethodSpec> getResourceActions()
  {
    return _resourceActions;
  }

  public void setResourceActions(List<RootBuilderMethodSpec> resourceActions)
  {
    _resourceActions = resourceActions;
  }

  public List<RootBuilderMethodSpec> getEntityActions()
  {
    return _entityActions;
  }

  public void setEntityActions(List<RootBuilderMethodSpec> entityActions)
  {
    _entityActions = entityActions;
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
    List<RootBuilderMethodSpec> methods = new ArrayList<RootBuilderMethodSpec>();
    if (_restMethods != null)
    {
      methods.addAll(_restMethods);
    }
    if (_finders != null)
    {
      methods.addAll(_finders);
    }
    if (_resourceActions != null)
    {
      methods.addAll(_resourceActions);
    }
    if (_entityActions != null)
    {
      methods.addAll(_entityActions);
    }
    return methods;
  }
}

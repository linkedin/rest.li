/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.server.filter.FilterResourceModel;


/**
 * @author nshankar
 */
public class FilterResourceModelImpl implements FilterResourceModel
{
  private final com.linkedin.restli.internal.server.model.ResourceModel _resourceModel;

  public FilterResourceModelImpl(final com.linkedin.restli.internal.server.model.ResourceModel model)
  {
    _resourceModel = model;
  }

  @Override
  public boolean isRootResource()
  {
    return _resourceModel.isRoot();
  }

  @Override
  public String getResourceNamespace()
  {
    return _resourceModel.getNamespace();
  }

  @Override
  public String getResourceName()
  {
    return _resourceModel.getName();
  }

  @Override
  public Class<?> getResourceClass()
  {
    return _resourceModel.getResourceClass();
  }

  @Override
  public Class<? extends RecordTemplate> getValueClass()
  {
    return _resourceModel.getValueClass();
  }

  @Override
  public FilterResourceModel getParentResourceModel()
  {
    if (_resourceModel.getParentResourceModel() != null)
    {
      return new FilterResourceModelImpl(_resourceModel.getParentResourceModel());
    }
    return null;
  }

  @Override
  public String getKeyName()
  {
    return _resourceModel.getKeyName();
  }
}
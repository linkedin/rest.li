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

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Base class for root request builder metadata specification, each root request builder is corresponding to
 * a type of resource schema, like SimpleSchema, CollectionSchema, AssociationSchema, and ActionSetSchema.
 *
 * @author Min Chen
 */
public abstract class RootBuilderSpec extends BuilderSpec
{
  protected ResourceSchema _resource;
  protected String _sourceIdlName;
  protected String _resourcePath;
  protected List<String> _pathKeys;
  protected Map<String, String> _keyPathTypes;

  public RootBuilderSpec(ResourceSchema resource)
  {
    this._resource = resource;
    this._pathKeys = Collections.emptyList();
    this._keyPathTypes = Collections.emptyMap();
  }

  public RootBuilderSpec(String packageName, String className, String baseClassName, ResourceSchema resource)
  {
    super(packageName, className, baseClassName);
    _resource = resource;
  }

  public ResourceSchema getResource()
  {
    return _resource;
  }

  public void setResource(ResourceSchema resource)
  {
    _resource = resource;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public void setNamespace(String packageName)
  {
    _namespace = packageName;
  }

  public String getClassName()
  {
    return _className;
  }

  public void setClassName(String className)
  {
    _className = className;
  }

  public String getSourceIdlName()
  {
    return _sourceIdlName;
  }

  public void setSourceIdlName(String sourceIdl)
  {
    _sourceIdlName = sourceIdl;
  }

  public String getResourcePath()
  {
    return _resourcePath;
  }

  public void setResourcePath(String resourcePath)
  {
    _resourcePath = resourcePath;
  }

  public String getResourceName()
  {
    return _resource.getName();
  }

  public List<String> getPathKeys()
  {
    return _pathKeys;
  }

  public void setPathKeys(List<String> pathKeys)
  {
    _pathKeys = pathKeys;
  }

  public Map<String, String> getKeyPathTypes()
  {
    return _keyPathTypes;
  }

  public void setKeyPathTypes(Map<String, String> keyPathTypes)
  {
    _keyPathTypes = keyPathTypes;
  }

  public abstract List<RootBuilderMethodSpec> getMethods();
}

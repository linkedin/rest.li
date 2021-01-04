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

import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.server.annotations.ReturnEntity;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class RestMethodSpec
{
  private final RestMethodSchema _schema;
  private final BaseResourceSpec _root;
  private List<ParameterSpec> _requiredParams;
  private List<ParameterSpec> _optionalParams;

  public RestMethodSpec(RestMethodSchema schema, BaseResourceSpec root)
  {
    _schema = schema;
    _root = root;
  }

  public RestMethodSchema getSchema()
  {
    return this._schema;
  }

  public String getMethod()
  {
    return _schema.getMethod();
  }

  public boolean hasParams()
  {
    return _schema.getParameters() != null && !_schema.getParameters().isEmpty();
  }

  public List<ParameterSpec> getRequiredParams()
  {
    if (_requiredParams != null)
    {
      return _requiredParams;
    }
    if (_schema.getParameters() == null)
    {
      return Collections.emptyList();
    }
    _requiredParams = _schema.getParameters().stream().filter(p -> !p.hasOptional() || !p.isOptional())
        .map(p -> new ParameterSpec(p, _root))
        .collect(Collectors.toList());
    return _requiredParams;
  }

  public List<ParameterSpec> getOptionalParams()
  {
    if (_optionalParams != null)
    {
      return _optionalParams;
    }
    if (_schema.getParameters() == null)
    {
      return Collections.emptyList();
    }
    _optionalParams = _schema.getParameters().stream().filter(p -> p.hasOptional() && p.isOptional())
        .map(p -> new ParameterSpec(p, _root))
        .collect(Collectors.toList());
    return _optionalParams;
  }

  public boolean returnsEntity()
  {
    return _schema.getAnnotations() != null && _schema.getAnnotations().containsKey(ReturnEntity.NAME);
  }
}

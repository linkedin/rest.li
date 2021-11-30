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

import com.linkedin.data.DataMapBuilder;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.server.annotations.ReturnEntity;


public class RestMethodSpec extends MethodSpec
{
  private final RestMethodSchema _schema;

  public RestMethodSpec(RestMethodSchema schema, BaseResourceSpec root)
  {
    super(root);
    this._schema = schema;
  }

  public RestMethodSchema getSchema()
  {
    return this._schema;
  }

  @Override
  public String getMethod()
  {
    return _schema.getMethod();
  }

  @Override
  public ParameterSchemaArray getParameters()
  {
    return _schema.getParameters() == null ? new ParameterSchemaArray() : _schema.getParameters();
  }

  @Override
  public boolean isPagingSupported()
  {
    return getMethod().equals("get_all") && _schema.hasPagingSupported() && _schema.isPagingSupported();
  }

  public boolean returnsEntity()
  {
    return _schema.getAnnotations() != null && _schema.getAnnotations().containsKey(ReturnEntity.NAME);
  }

  @Override
  protected MetadataSchema getMetadata()
  {
    return _schema.getMetadata();
  }

  @Override
  public int getQueryParamMapSize()
  {
    int params = getParameters().size();
    if (returnsEntity())
    {
      params++;
    }
    params += getSupportedProjectionParams().size();
    switch (ResourceMethod.fromString(_schema.getMethod()))
    {
      case BATCH_PARTIAL_UPDATE:
      case BATCH_UPDATE:
      case BATCH_DELETE:
      case BATCH_GET:
        // Batch requests send ids as query parameter.
        params++;
    }
    return DataMapBuilder.getOptimumHashMapCapacityFromSize(params);
  }
}


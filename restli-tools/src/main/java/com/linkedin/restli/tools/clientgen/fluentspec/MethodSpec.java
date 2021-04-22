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

import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class MethodSpec
{
  protected final BaseResourceSpec _resourceSpec;
  private List<ParameterSpec> _requiredParams;
  private List<ParameterSpec> _optionalParams;
  private List<ParameterSpec> _methodParameters;
  private static final ParameterSchema START_SCHEMA = new ParameterSchema().setOptional(true)
      .setName(RestConstants.START_PARAM)
      .setType(DataSchemaConstants.INTEGER_TYPE);
  private static final ParameterSchema COUNT_SCHEMA = new ParameterSchema().setOptional(true)
      .setName(RestConstants.COUNT_PARAM)
      .setType(DataSchemaConstants.INTEGER_TYPE);
  private static final String FIELDS_MASK_METHOD_NAME = "Mask";
  private static final String METADATA_MASK_METHOD_NAME = "MetadataMask";
  private static final String PAGING_MASK_METHOD_NAME = "PagingMask";

  public MethodSpec(BaseResourceSpec resourceSpec)
  {
    _resourceSpec = resourceSpec;
  }

  public BaseResourceSpec getResourceSpec()
  {
    return _resourceSpec;
  }

  public abstract String getMethod();

  public String getMethodName()
  {
    return getMethod();
  }

  public abstract ParameterSchemaArray getParameters();

  public boolean isPagingSupported()
  {
    return false;
  }

  /**
   * Returns the optimum size to initialize the maps for query param and query param classes.
   */
  public int getQueryParamMapSize()
  {
    return 0;
  }

  public boolean returnsEntity()
  {
    return false;
  }

  protected MetadataSchema getMetadata()
  {
    return null;
  }

  public boolean hasParams()
  {
    return !getParameters().isEmpty() || getSupportedProjectionParams().size() > 0;
  }

  public List<ParameterSpec> getRequiredParameters()
  {
    if (_requiredParams != null)
    {
      return _requiredParams;
    }
    _requiredParams = getParameters().stream().filter(p -> !isOptionalParam(p))
        .map(p -> new ParameterSpec(p, _resourceSpec))
        .collect(Collectors.toList());
    return _requiredParams;
  }

  public List<ParameterSpec> getOptionalParameters()
  {
    if (_optionalParams != null)
    {
      return _optionalParams;
    }
    _optionalParams = getParameters().stream().filter(this::isOptionalParam)
        .map(p -> new ParameterSpec(p, _resourceSpec))
        .collect(Collectors.toList());
    if (isPagingSupported())
    {
      _optionalParams.add(new ParameterSpec(START_SCHEMA, _resourceSpec));
      _optionalParams.add(new ParameterSpec(COUNT_SCHEMA, _resourceSpec));
    }
    return _optionalParams;
  }

  public List<ParameterSpec> getAllParameters()
  {
    if (_methodParameters == null)
    {
      if (getParameters().size() > 0 || hasProjectionParams())
      {
        _methodParameters = new ArrayList<>(getParameters().size() + getSupportedProjectionParams().size());
        _methodParameters.addAll(getRequiredParameters());
        _methodParameters.addAll(getOptionalParameters());
        _methodParameters.addAll(getSupportedProjectionParams());
      }
      else
      {
        _methodParameters = Collections.emptyList();
      }
    }
    return _methodParameters;
  }

  public boolean hasRequiredParams()
  {
    return getRequiredParameters().size() > 0;
  }

  public boolean hasOptionalParams()
  {
    return getOptionalParameters().size() > 0;
  }

  private boolean isOptionalParam(ParameterSchema param)
  {
    return param.hasOptional() || param.hasDefault();
  }

  public Set<ProjectionParameterSpec> getSupportedProjectionParams()
  {
    switch (ResourceMethod.fromString(getMethod()))
    {
      case GET:
      case BATCH_GET:
        return Collections.singleton(new ProjectionParameterSpec(RestConstants.FIELDS_PARAM,
            FIELDS_MASK_METHOD_NAME, _resourceSpec.getEntityClass(), _resourceSpec));
      case CREATE:
      case BATCH_CREATE:
      case PARTIAL_UPDATE:
      case BATCH_PARTIAL_UPDATE:
        if (returnsEntity())
        {
          return Collections.singleton(new ProjectionParameterSpec(RestConstants.FIELDS_PARAM,
              FIELDS_MASK_METHOD_NAME, _resourceSpec.getEntityClass(), _resourceSpec));
        }
        else
        {
          return Collections.emptySet();
        }
      case FINDER:
      case BATCH_FINDER:
      case GET_ALL:
        Set<ProjectionParameterSpec> collectionParts = new HashSet<>();
        collectionParts.add(new ProjectionParameterSpec(RestConstants.FIELDS_PARAM, FIELDS_MASK_METHOD_NAME,
            _resourceSpec.getEntityClass(), _resourceSpec));
        if (getMetadata() != null)
        {
          collectionParts.add(new ProjectionParameterSpec(RestConstants.METADATA_FIELDS_PARAM,
              METADATA_MASK_METHOD_NAME,
              _resourceSpec.classToTemplateSpec(getMetadata().getType()),
              _resourceSpec));
        }
        if (isPagingSupported())
        {
          collectionParts.add(new ProjectionParameterSpec(RestConstants.PAGING_FIELDS_PARAM,
              PAGING_MASK_METHOD_NAME,
              _resourceSpec.classToTemplateSpec(CollectionMetadata.dataSchema().getFullName()),
              _resourceSpec));
        }
        return collectionParts;
      default:
        return Collections.emptySet();
    }
  }

  public boolean hasProjectionParams()
  {
    return getSupportedProjectionParams().size() > 0;
  }
}


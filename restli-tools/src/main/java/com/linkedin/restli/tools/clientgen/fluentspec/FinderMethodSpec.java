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
import com.linkedin.pegasus.generator.CodeUtil;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.MetadataSchema;
import com.linkedin.restli.restspec.ParameterSchemaArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FinderMethodSpec extends MethodSpec
{
  private final FinderSchema _finderSchema;

  public FinderMethodSpec(FinderSchema finderSchema, BaseResourceSpec root)
  {
    super(root);
    _finderSchema = finderSchema;
  }

  public String getName()
  {
    return _finderSchema.getName();
  }

  @Override
  public String getMethod()
  {
    return ResourceMethod.FINDER.name();
  }

  public String getMethodName()
  {
    return "findBy" + CodeUtil.capitalize(getName());
  }

  public ParameterSchemaArray getParameters()
  {
    return _finderSchema.getParameters() == null ? new ParameterSchemaArray() : _finderSchema.getParameters();
  }

  public List<CompoundKeySpec.AssocKeySpec> getAssocKeys()
  {
    if (_finderSchema.hasAssocKey())
    {
      String assocKey = _finderSchema.getAssocKey();
      CompoundKeySpec.AssocKeySpec keySpec = getAssocKeySpec(assocKey);
      return Collections.singletonList(keySpec);
    }
    if (_finderSchema.hasAssocKeys())
    {
      List<CompoundKeySpec.AssocKeySpec> keySpecs = new ArrayList<>(_finderSchema.getAssocKeys().size());
      for (String assocKey : _finderSchema.getAssocKeys())
      {
        keySpecs.add(getAssocKeySpec(assocKey));
      }
      return keySpecs;
    }
    return Collections.emptyList();
  }

  private CompoundKeySpec.AssocKeySpec getAssocKeySpec(String assocKey)
  {
    return ((AssociationResourceSpec) _resourceSpec).getCompoundKeySpec().getAssocKeyByName(assocKey);
  }

  @Override
  public boolean isPagingSupported()
  {
    return _finderSchema.hasPagingSupported() && _finderSchema.isPagingSupported();
  }

  @Override
  public int getQueryParamMapSize()
  {
    int params = getParameters().size();
    params += getSupportedProjectionParams().size();
    return DataMapBuilder.getOptimumHashMapCapacityFromSize(params);
  }

  @Override
  public boolean returnsEntity()
  {
    return true;
  }

  @Override
  protected MetadataSchema getMetadata()
  {
    return _finderSchema.getMetadata();
  }
}

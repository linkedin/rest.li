/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.internal.client;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;

/**
 * Converts a raw RestResponse into a type-bound entity response.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class EntityResponseDecoder<T extends RecordTemplate> extends RestResponseDecoder<T>
{
  private final Class<? extends T> _entityClass;

  public EntityResponseDecoder(Class<? extends T> templateClass)
  {
    _entityClass = templateClass;
  }

  @Override
  public Class<?> getEntityClass()
  {
    return _entityClass;
  }

  @Override
  public T wrapResponse(DataMap dataMap, Map<String, String> headers, ProtocolVersion version)
                  throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
  {
    return dataMap == null ? null : _entityClass.getConstructor(DataMap.class).newInstance(dataMap);
  }
}

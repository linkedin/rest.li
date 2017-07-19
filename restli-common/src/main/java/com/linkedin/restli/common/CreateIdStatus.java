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

package com.linkedin.restli.common;


import com.linkedin.data.DataMap;
import com.linkedin.restli.internal.common.URIParamUtils;


/**
 * CreateStatus that keeps track of a strongly typed version of the returned key.
 *
 * @author Moira Tagle
 */
public class CreateIdStatus<K> extends CreateStatus
{
  private final K _key;

  /**
   * The id field of the dataMap should match the given key.
   * This method is for internal use only.  Others should use {@link com.linkedin.restli.common.CreateIdStatus#CreateIdStatus(int, Object, ErrorResponse, ProtocolVersion)}.
   *
   * @see {@link com.linkedin.restli.internal.common.CreateIdStatusDecoder}
   * @param dataMap the underlying DataMap of the CreateIdStatus response. This Data should fit the {@link com.linkedin.restli.common.CreateStatus} schema.
   * @param key The strongly typed key.  Can be null.
   */
  public CreateIdStatus(DataMap dataMap, K key)
  {
    super(dataMap);
    _key = key;
  }

  /**
   * @param status the individual http status
   * @param key the key; can be null
   * @param error the {@link ErrorResponse}; can be null
   * @param version the {@link com.linkedin.restli.common.ProtocolVersion}
   */
  public CreateIdStatus(int status, K key, ErrorResponse error, ProtocolVersion version)
  {
    super(createDataMap(status, key, null, error, version));
    _key = key;
  }

  /**
   * @param status the individual http status
   * @param key the key; can be null
   * @param location location url
   * @param error the {@link ErrorResponse}; can be null
   * @param version the {@link com.linkedin.restli.common.ProtocolVersion}
   */
  public CreateIdStatus(int status, K key, String location, ErrorResponse error, ProtocolVersion version)
  {
    super(createDataMap(status, key, location, error, version));
    _key = key;
  }

  /**
   * create a DataMap matching the schema of {@link com.linkedin.restli.common.CreateStatus} with the given data
   * @param status the individual http status
   * @param key the key; can be null
   * @param error the {@link ErrorResponse}; can be null
   * @param version the the {@link com.linkedin.restli.common.ProtocolVersion}, used to serialize the key
   * @return a {@link com.linkedin.data.DataMap} containing the given data
   */
  protected static DataMap createDataMap(int status, Object key, String location, ErrorResponse error, ProtocolVersion version)
  {
    CreateStatus createStatus = new CreateStatus();
    createStatus.setStatus(status);
    if (key != null)
    {
      @SuppressWarnings("deprecation")
      CreateStatus c = createStatus.setId(URIParamUtils.encodeKeyForBody(key, false, version));
    }
    if (location != null)
    {
      createStatus.setLocation(location);
    }
    if (error != null)
    {
      createStatus.setError(error);
    }
    return createStatus.data();
  }

  /**
   * @return a serialized version of the key of the created record.
   * @deprecated serialization format may change depending on the used {@link com.linkedin.restli.common.ProtocolVersion}.
   *             Call {@link #getKey()} instead
   */
  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public String getId()
  {
    if (_key instanceof CompoundKey || _key instanceof ComplexResourceKey)
    {
      throw new UnsupportedOperationException("Do not call getId to get a CompoundKey or a ComplexKey, the serialized format may be inconsistent; call getKey instead");
    }
    return super.getId();
  }

  /**
   * @return the strongly typed key associated with this create.
   */
  public K getKey()
  {
    return _key;
  }
}
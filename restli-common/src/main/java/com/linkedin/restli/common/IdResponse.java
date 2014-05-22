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


import com.linkedin.data.template.RecordTemplate;


/**
 * Response that contains a single typed id.
 *
 * @author Moira Tagle
 */
public class IdResponse<K> extends RecordTemplate
{
  private K _key;

  /**
   * TODO
   * we cannot presently serialize the id in the response body, because old clients will fail upon seeing data in what
   * they expect to be an empty response. Once clients have moved away from old versions of EmptyResponseDecoder, we can
   * begin serializing the key in the body by passing in a DataMap to the
   * {@link com.linkedin.data.template.RecordTemplate} constructor
   *
   * To allow filters to access a non-serialized key, we provide {@link #getId()}.
   */

  public IdResponse(K key)
  {
    super(null, null);
    _key = key;
  }

  public K getId()
  {
    return _key;
  }

  // need to override toString, equals, and hashCode because that is normally done through the dataMap, which is null.

  @Override
  public String toString()
  {
    return (this._key == null)? "" : this._key.toString();
  }

  @Override
  public boolean equals(Object that)
  {
    if (that instanceof IdResponse)
    {
      IdResponse<?> thatIdResponse = (IdResponse<?>) that;
      return (this._key == null)? thatIdResponse._key == null : this._key.equals(thatIdResponse._key);
    }
    else
    {
      return false;
    }
  }

  @Override
  public int hashCode()
  {
    return _key == null? 0 : _key.hashCode();
  }
}

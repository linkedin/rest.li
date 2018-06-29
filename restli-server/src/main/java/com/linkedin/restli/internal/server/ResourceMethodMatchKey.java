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

package com.linkedin.restli.internal.server;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ResourceMethodMatchKey
{
  private final String  _httpMethod;
  private final String  _restliMethod;
  private final boolean _hasActionParam;
  private final boolean _hasQueryParam;
  private final boolean _hasBatchFinderParam;
  private final boolean _hasBatchKeys;
  private final boolean _hasEntitySegment;

  /**
   * Constructor.
   */
  public ResourceMethodMatchKey(final String httpMethod,
                                final String restliMethod,
                                final boolean hasActionParam,
                                final boolean hasQueryParam,
                                final boolean hasBatchFinderParam,
                                final boolean hasBatchKeys,
                                final boolean hasEntitySegment)
  {
    _httpMethod = httpMethod.toUpperCase();
    _restliMethod = restliMethod.toUpperCase();
    _hasActionParam = hasActionParam;
    _hasQueryParam = hasQueryParam;
    _hasBatchKeys = hasBatchKeys;
    _hasEntitySegment = hasEntitySegment;
    _hasBatchFinderParam = hasBatchFinderParam;
  }

  @Override
  public boolean equals(final Object oref)
  {
    if (!(oref instanceof ResourceMethodMatchKey))
    {
      return false;
    }

    ResourceMethodMatchKey ref = (ResourceMethodMatchKey)oref;

    return _httpMethod.equals(ref._httpMethod) &&
            _restliMethod.equals(ref._restliMethod) &&
            _hasActionParam == ref._hasActionParam &&
            _hasQueryParam == ref._hasQueryParam &&
            _hasBatchFinderParam == ref._hasBatchFinderParam &&
            _hasBatchKeys == ref._hasBatchKeys &&
            _hasEntitySegment == ref._hasEntitySegment;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = _httpMethod.hashCode();
    result = prime * result + (_restliMethod.hashCode());
    result = prime * result + (_hasActionParam ? 0 : 1);
    result = prime * result + (_hasQueryParam ? 0 : 1);
    result = prime * result + (_hasBatchFinderParam ? 0 : 1);
    result = prime * result + (_hasBatchKeys ? 0 : 1);
    result = prime * result + (_hasEntitySegment ? 0 : 1);
    return result;
  }
}

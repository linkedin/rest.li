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

package com.linkedin.r2.message;

import java.util.HashMap;
import java.util.Map;

/**
 * RequestContext should not be shared across requests.
 *
 * @author Josh Walker
 * @version $Revision: $
 */
public class RequestContext
{
  private final Map<String, Object> _localAttrs;

  /**
   * Construct a new instance with an empty set of attributes.
   */
  public RequestContext()
  {
    _localAttrs = new HashMap<String, Object>();
  }

  /**
   * Construct a new instance with a copy of the attributes from the
   * specified {@link RequestContext}.
   *
   * @param other the {@link RequestContext} from which the attributes should be obtained.
   */
  public RequestContext(RequestContext other)
  {
    _localAttrs = new HashMap<String, Object>(other._localAttrs);
  }

  private RequestContext(Map<String, Object> localAttrs)
  {
    _localAttrs = localAttrs;
  }

  /**
   * Return the attributes from this object.
   *
   * @return the attributes contained by this object.
   */
  public Map<String, Object> getLocalAttrs()
  {
    return _localAttrs;
  }

  /**
   * Return a specific attribute from this object.
   *
   * @param key the key for the attribute to be obtained.
   * @return the value of the specified attribute, or null if the attribute does not exist.
   */
  public Object getLocalAttr(String key)
  {
    return _localAttrs.get(key);
  }

  /**
   * Set the value of a specific attribute in this object.
   *
   * @param key the key for the attribute to be set.
   * @param value the value for the attribute to be set.
   */
  public void putLocalAttr(String key, Object value)
  {
    _localAttrs.put(key, value);
  }

  /**
   * Remove a specific attribute from this object.
   *
   * @param key the key for the attribute to be removed.
   * @return the previous value of the attribute, or null if the attribute does not exist.
   */
  public Object removeLocalAttr(String key)
  {
    return _localAttrs.remove(key);
  }

  @Override
  public RequestContext clone()
  {
    Map<String, Object> localAttrs = new HashMap<String, Object>();
    localAttrs.putAll(this._localAttrs);
    return new RequestContext(localAttrs);
  }

  @Override
  public boolean equals(Object o)
  {
    return (o instanceof RequestContext) &&
            ((RequestContext)o)._localAttrs.equals(this._localAttrs);
  }

  @Override
  public int hashCode()
  {
    return _localAttrs.hashCode();
  }

  @Override
  public String toString()
  {
    return _localAttrs.toString();
  }
}

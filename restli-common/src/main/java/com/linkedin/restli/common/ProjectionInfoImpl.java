/*
   Copyright (c) 2018 LinkedIn Corp.

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

import java.util.Objects;

/**
 * Request projection meta data that is mutable. Currently this class only contains a flag indicating whether a request
 * has projection specified or not, later on this can be extended to contain other projection related
 * details. We pass this data through request context among R2 and Rest.li filters to emit it through
 * Service Call Event.
 *
 * @author mnchen
 */
public class ProjectionInfoImpl implements MutableProjectionInfo
{
  private boolean _hasProjection = false;

  public ProjectionInfoImpl()
  {
  }

  public ProjectionInfoImpl(boolean _hasProjection)
  {
    this._hasProjection = _hasProjection;
  }

  @Override
  public void setProjectionPresent(boolean projectionPresent)
  {
    this._hasProjection = projectionPresent;
  }

  @Override
  public boolean isProjectionPresent()
  {
    return _hasProjection;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectionInfoImpl that = (ProjectionInfoImpl) o;
    return _hasProjection == that._hasProjection;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_hasProjection);
  }
}

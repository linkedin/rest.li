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

package com.linkedin.d2.balancer.properties;

import java.beans.ConstructorProperties;
import javax.management.MXBean;


/**
 * Contains properties for a partition.
 */
@MXBean
public class PartitionData
{
  private final double _weight;

  @ConstructorProperties({"weight"})
  public PartitionData(double weight)
  {
    _weight = weight;
  }

  public double getWeight()
  {
    return _weight;
  }

  @Override
  public String toString()
  {
    return "[ weight =" + _weight + " ]";
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PartitionData other = (PartitionData) obj;
    return _weight == other.getWeight();
  }

  @Override
  public int hashCode()
  {
    return ((Double)_weight).hashCode();
  }

}
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

/**
 * For non partitioned clusters
 */
public class NullPartitionProperties implements PartitionProperties
{
  // eager initialization here; the cost is really low and we probably need this anyway
  private static final NullPartitionProperties _instance = new NullPartitionProperties();

  @Override
  public PartitionType getPartitionType()
  {
    return PartitionType.NONE;
  }

  public static NullPartitionProperties getInstance()
  {
    return _instance;
  }

  private NullPartitionProperties()
  {
  }
}

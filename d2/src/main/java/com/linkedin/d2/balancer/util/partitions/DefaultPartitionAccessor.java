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

package com.linkedin.d2.balancer.util.partitions;

import java.net.URI;

public class DefaultPartitionAccessor implements PartitionAccessor
{
  // eager initialization here; the cost is really low and we probably need this anyway
  private static final DefaultPartitionAccessor _instance = new DefaultPartitionAccessor();

  public static final int DEFAULT_PARTITION_ID = 0;
  @Override
  public int getPartitionId(URI uri)
  {
    return DEFAULT_PARTITION_ID;
  }
  @Override
  public int getPartitionId(String key)
  {
    return DEFAULT_PARTITION_ID;
  }
  @Override
  public int getMaxPartitionId()
  {
    return DefaultPartitionAccessor.DEFAULT_PARTITION_ID;
  }

  private DefaultPartitionAccessor()
  {
  }

  public static DefaultPartitionAccessor getInstance()
  {
    return _instance;
  }
}

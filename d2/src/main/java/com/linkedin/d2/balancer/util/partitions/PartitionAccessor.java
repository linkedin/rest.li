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

/**
 * This is the accessor for partition properties. It is here because we want to keep partition properties
 * as data storage only and move the logic of manipulating the data to a separate place.
 * To get a PartitionAccessor, one should use {@link PartitionAccessorFactory}
 */
public interface PartitionAccessor
{
  int getPartitionId(URI uri) throws PartitionAccessException;
  int getPartitionId(String key) throws PartitionAccessException;
  int getMaxPartitionId();
}


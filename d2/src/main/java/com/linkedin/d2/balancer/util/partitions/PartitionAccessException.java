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

// This exception is meant to force us be aware of problems in accessing partitions (obtaining partition id)
// and handle it appropriately. Our code should handle this exception whenever possible
public class PartitionAccessException extends Exception
{
  private static final long serialVersionUID = 69954L;
  public PartitionAccessException(String msg)
  {
    super(msg);
  }

  public PartitionAccessException(Exception ex)
  {
    super(ex);
  }

  public PartitionAccessException(String msg, Exception ex)
  {
    super(msg, ex);
  }
}

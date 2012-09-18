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

package com.linkedin.d2.discovery;

/**
 * Denotes a data format or other error occured during serialization or deserialization.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class PropertySerializationException extends Exception
{
  private static final long serialVersionUID = 0L;

  public PropertySerializationException()
  {
  }

  public PropertySerializationException(String message)
  {
    super(message);
  }

  public PropertySerializationException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public PropertySerializationException(Throwable cause)
  {
    super(cause);
  }
}

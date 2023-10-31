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

package com.linkedin.d2.discovery;

import com.google.protobuf.ByteString;


public interface PropertySerializer<T>
{
  byte[] toBytes(T property);

  T fromBytes(byte[] bytes) throws PropertySerializationException;

  default T fromBytes(byte[] bytes, long version) throws PropertySerializationException
  {
    return fromBytes(bytes);
  }

  default T fromBytes(ByteString bytes) throws PropertySerializationException
  {
    // This default implementation provides no benefit over calling fromByte(byte[]) as it creates a copy of the
    // underlying byte array. More efficient implementations should use ByteString#newInput to read the underlying
    // buffer without copying it.
    return fromBytes(bytes.toByteArray());
  }

  default T fromBytes(ByteString bytes, long version) throws PropertySerializationException
  {
    return fromBytes(bytes);
  }
}

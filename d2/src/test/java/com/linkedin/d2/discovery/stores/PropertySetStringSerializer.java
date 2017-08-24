/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.discovery.stores;

import com.linkedin.d2.discovery.PropertySerializer;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PropertySetStringSerializer implements PropertySerializer<Set<String>>
{
  // let's assume this is a character that won't be used inside data in a test environment
  private static final String SEPARATOR = "~~~";

  @Override
  public Set<String> fromBytes(byte[] bytes)
  {
    try
    {
      return new HashSet<>(Arrays.asList(new String(bytes, "UTF-8").split(SEPARATOR)));
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] toBytes(Set<String> property)
  {
    try
    {
      return String.join(SEPARATOR, property).getBytes("UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }
}

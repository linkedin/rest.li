/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.examples;


import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.Tone;
import java.util.HashMap;
import java.util.Map;


/**
 * @author kparikh
 */
public class AssociationResourceHelpers
{
  public static CompoundKey URL_COMPOUND_KEY =
      new CompoundKey().append("src", StringTestKeys.URL).append("dest", StringTestKeys.URL2);
  public static Message URL_MESSAGE =
      new Message().setId(URL_COMPOUND_KEY.getPartAsString("src") + " " + URL_COMPOUND_KEY.getPartAsString("dest"))
                   .setMessage("I need some %20")
                   .setTone(Tone.SINCERE);

  public static CompoundKey SIMPLE_COMPOUND_KEY =
      new CompoundKey().append("src", StringTestKeys.SIMPLEKEY).append("dest", StringTestKeys.SIMPLEKEY2);
  public static Message SIMPLE_MESSAGE =
      new Message().setId(SIMPLE_COMPOUND_KEY.getPartAsString("src") + " " + SIMPLE_COMPOUND_KEY.getPartAsString("dest"))
                   .setMessage("src1-dest1")
                   .setTone(Tone.SINCERE);

  public static Map<CompoundKey, Message> DB = new HashMap<>();
  static
  {
    DB.put(URL_COMPOUND_KEY, URL_MESSAGE);
    DB.put(SIMPLE_COMPOUND_KEY, SIMPLE_MESSAGE);
  }
}

/*
   Copyright (c) 2019 LinkedIn Corp.

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
package com.linkedin.restli.examples.greetings.server.altkey;

import com.linkedin.data.template.InvalidAlternativeKeyException;
import com.linkedin.data.template.KeyCoercer;
import com.linkedin.restli.common.CompoundKey;

/**
 * A coercer to coerce a string type Alternative Key to a CompoundKey type primary key and vise-versa
 */
public class StringCompoundKeyCoercer implements KeyCoercer<String, CompoundKey>
{
  /**
   * Coerce String Alternative Key : "messageaXgreetingId1"
   * to CompoundKey ：message=a&greetingId=1
   * @param object the alternative key.
   * @return primary key - CompoundKey
   * @throws InvalidAlternativeKeyException
   */
  @Override
  public CompoundKey coerceToKey(String object) throws InvalidAlternativeKeyException
  {
    CompoundKey compoundKey = new CompoundKey();
    compoundKey.append("message", object.substring(7, 8));
    compoundKey.append("greetingId", Long.parseLong(object.substring(19, 20)));
    return compoundKey;
  }

  /**
   * Coerce CompoundKey : message=a&greetingId=1
   * to String Alternative Key : "messageaXgreetingId1"
   * @param object the primary key.
   * @return String Alternative Key
   */
  @Override
  public String coerceFromKey(CompoundKey object)
  {
    return "message" + object.getPart("message") + "x" + "greetingId" + object.getPart("greetingId");
  }
}
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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;

/**
 * A coercer to coerce a string type Alternative Key to a ComplexKey type primary key and vise-versa
 */
public class StringComplexKeyCoercer implements KeyCoercer<String, ComplexResourceKey<TwoPartKey, TwoPartKey>>
{
  /**
   * Coerce String Alternative Key : "majorxKEY 1xminorxKEY 2"
   * to ComplexKey ：major=KEY 1&minor=KEY 2
   * @param object the alternative key.
   * @return primary key - ComplexKey
   * @throws InvalidAlternativeKeyException
   */
  @Override
  public ComplexResourceKey<TwoPartKey, TwoPartKey> coerceToKey(String object) throws InvalidAlternativeKeyException
  {
    String[] keys = object.split("x");
    return new ComplexResourceKey<TwoPartKey, TwoPartKey>(
        new TwoPartKey().setMajor(keys[1]).setMinor(keys[3]),
        new TwoPartKey());
  }

  /**
   * Coerce ComplexKey : major=KEY 1&minor=KEY 2
   * to String Alternative Key : "majorxKEY 1xminorxKEY 2"
   * @param object the primary key.
   * @return String Alternative Key
   */
  @Override
  public String coerceFromKey(ComplexResourceKey<TwoPartKey, TwoPartKey> object)
  {
    return "major" + "x" +  object.getKey().getMajor() + "x" + "minor" + "x" + object.getKey().getMinor();
  }
}
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

package com.linkedin.restli.examples.greetings.server;


import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.examples.greetings.api.TwoPartKey;
import com.linkedin.restli.examples.typeref.api.TyperefRecord;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.ComplexKeyResourceTemplate;


/**
 * Demonstrates a resource with a complex key that consists of a field of Bytes typeref.
 * @author mnchen
 *
 */
@RestLiCollection(
  name = "complexByteKeys",
  namespace = "com.linkedin.restli.examples.greetings.client",
  keyName="keys"
  )
public class ComplexByteKeysResource extends ComplexKeyResourceTemplate<TyperefRecord, TwoPartKey, TyperefRecord>
{
  private static ComplexKeysDataProvider _dataProvider = new ComplexKeysDataProvider();

  @Override
  public TyperefRecord get(final ComplexResourceKey<TyperefRecord, TwoPartKey> complexKey)
  {
    TyperefRecord key = complexKey.getKey();
    return new TyperefRecord().setBytes(key.getBytes());
  }
}

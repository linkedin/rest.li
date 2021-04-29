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

import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.api.Tone;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class provides some predefined test data : Greetings and CompoundKeys,
 * which can be used by different methods in CollectionAltKeyResource and AssociationAltKeyResource.
 *
 * @author Yingjie Bi
 */
public class AltKeyDataProvider
{
  private Map<Long, Greeting> _db1;

  private Map<CompoundKey, Greeting> _db2;

  public AltKeyDataProvider()
  {
    _db1 = new HashMap<>();
    _db2 = new HashMap<>();
    CompoundKey key1 = new CompoundKey();
    key1.append("message", "a");
    key1.append("greetingId", 1L);

    CompoundKey key2 = new CompoundKey();
    key2.append("message", "b");
    key2.append("greetingId", 2L);

    CompoundKey key3 = new CompoundKey();
    key3.append("message", "c");
    key3.append("greetingId", 3L);

    Greeting greeting1 = new Greeting();
    greeting1.setTone(Tone.INSULTING);
    greeting1.setId(1l);
    greeting1.setMessage("a");

    Greeting greeting2 = new Greeting();
    greeting2.setTone(Tone.FRIENDLY);
    greeting2.setId(2l);
    greeting2.setMessage("b");

    Greeting greeting3 = new Greeting();
    greeting3.setTone(Tone.FRIENDLY);
    greeting3.setId(3l);
    greeting3.setMessage("c");

    _db1.put(1L, greeting1);
    _db1.put(2L, greeting2);
    _db2.put(key1, greeting1);
    _db2.put(key2, greeting2);
    _db2.put(key3, greeting3);
  }
  private void create(CompoundKey id, Greeting entity)
  {
    _db2.put(id, entity);
  }

  public Greeting get(Long id)
  {
    return _db1.get(id);
  }

  public Greeting get(CompoundKey id)
  {
    return _db2.get(id);
  }

  public Map<Long, Greeting> batchGet(Set<Long> ids)
  {
    Map<Long, Greeting> result = new HashMap<>();
    for(Long id : ids)
    {
      result.put(id, _db1.get(id));
    }
    return result;
  }

  public Map<CompoundKey, Greeting> compoundKeyBatchGet(Set<CompoundKey> ids)
  {
    Map<CompoundKey, Greeting> result = new HashMap<>();
    for(CompoundKey id : ids)
    {
      result.put(id, _db2.get(id));
    }
    return result;
  }

  public void update(Long key, Greeting entity)
  {
    _db1.put(key, entity);
  }

  public void update(CompoundKey key, Greeting entity)
  {
    _db2.put(key, entity);
  }
}

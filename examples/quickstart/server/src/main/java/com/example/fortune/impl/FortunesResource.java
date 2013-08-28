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

package com.example.fortune.impl;

import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import com.example.fortune.Fortune;
import java.util.HashMap;
import java.util.Map;

/**
 * Very simple RestLi Resource that serves up a fortune cookie.
 *
 * @author Doug Young
 */
@RestLiCollection(name = "fortunes", namespace = "com.example.fortune")

public class FortunesResource extends CollectionResourceTemplate<Long, Fortune>
{
  // Create trivial db for fortunes
  static Map<Long, String> fortunes = new HashMap<Long, String>();
  static {
    fortunes.put(1L, "Today is your lucky day.");
    fortunes.put(2L, "There's no time like the present.");
    fortunes.put(3L, "Don't worry, be happy.");
  }

  @Override
  public Fortune get(Long key)
  {
    // Retrieve the requested fortune
    String fortune = fortunes.get(key);
    if(fortune == null)
      fortune = "Your luck has run out. No fortune for id="+key;

    // return an object that represents the fortune cookie
    return new Fortune().setFortune(fortune);
  }
}

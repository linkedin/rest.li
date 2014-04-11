/*
   Copyright (c) 2014 LinkedIn Corp.

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


package com.linkedin.d2.balancer.util;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


/**
 * A pair of a collection of keys and an ordered collection of hosts
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public class KeysAndHosts<K>
{
  private final Collection<K> _keys;
  //this list is ranked based on priority for load balancing, aka higher priority host is put on top of lower priority ones
  private final List<URI> _hosts;

  public KeysAndHosts(Collection<K> keys, List<URI> hosts)
  {
    _keys = keys;
    _hosts = hosts;
  }

  public Collection<K> getKeys()
  {
    return Collections.unmodifiableCollection(_keys);
  }

  public List<URI> getHosts()
  {
    return Collections.unmodifiableList(_hosts);
  }

}

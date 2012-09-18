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

package com.linkedin.d2.discovery.event;

import java.util.HashMap;
import java.util.Map;

import com.linkedin.d2.discovery.event.PropertyEventSubscriber;

/**
* @author Chris Riccomini
* @version $Revision: $
*/
public class PropertyEventTestSubscriber implements PropertyEventSubscriber<String>
{
  public Map<String, String> properties;

  public PropertyEventTestSubscriber()
  {
    properties = new HashMap<String, String>();
  }

  @Override
  public void onAdd(String propertyName, String propertyValue)
  {
    properties.put("add-" + propertyName, propertyValue);
  }

  @Override
  public void onInitialize(String propertyName, String propertyValue)
  {
    properties.put("init-" + propertyName, propertyValue);
  }

  @Override
  public void onRemove(String propertyName)
  {
    properties.remove("add-" + propertyName);
    properties.remove("init-" + propertyName);
  }
}

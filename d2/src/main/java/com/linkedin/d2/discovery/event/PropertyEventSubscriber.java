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

package com.linkedin.d2.discovery.event;

/**
 * PropertyEventSubscriber is an event driven interface for listening to property updates.
 * Whenever a property value change is published, the subscriber will receive an notification.
 *
 * <br/>
 * <br/>
 *
 *   @see PropertyEventBus
 *
 * @author criccomini
 *
 */
public interface PropertyEventSubscriber<T>
{
  /**
   * Invoked exactly once for each property subscribed to by name, with the current
   * value of the property.
   *
   * @see PropertyEventBus#register(java.util.Set, PropertyEventSubscriber)
   *
   * @param propertyName
   * @param propertyValue
   */
  void onInitialize(String propertyName, T propertyValue);

  /**
   * Invoked whenever the subscriber publishes an add event
   * @param propertyName
   * @param propertyValue
   */
  void onAdd(String propertyName, T propertyValue);

  /**
   * Invoked whene the subscriber publishes a remove event
   * @param propertyName
   */
  void onRemove(String propertyName);
}

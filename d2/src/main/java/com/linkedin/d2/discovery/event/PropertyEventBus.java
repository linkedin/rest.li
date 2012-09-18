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

import java.util.Set;

/**
 * The PropertyEventBus provides a mechanism to publish and subscribe to changes in the values
 * of a set of properties.
 *
 * The bus imposes a total order on events by ensuring that one event is processed by all
 * subscribers before processing of the next event starts.

 * The subscriber callbacks:
 *
 * @see PropertyEventSubscriber#onInitialize(String, Object)
 * @see PropertyEventSubscriber#onAdd(String, Object)
 * @see PropertyEventSubscriber#onRemove(String)
 *
 * and the publisher callbacks:
 *
 * @see PropertyEventPublisher#startPublishing(String)
 * @see PropertyEventPublisher#stopPublishing(String)
 *
 * are always called (with one exception described below) from a separate thread with no locks held.
 * At most one callback will be executing at any given time, simplifying concurrency considerations
 * for the publishers and subscribers.
 *
 * There is one exception: if a particular property has been previously initialized by the
 * publisher, the subscriber's onInitialize callback may be invoked by the thread that called
 * register, before register returns.
 *
 * It should be safe to call all the bus methods while holding locks, since the bus will not
 * synchronously invoke callbacks, with the one exception noted above.
 *
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public interface PropertyEventBus<T>
{

  /**
   * Register a subscriber to receive notification of all property events.
   * @param subscriber subscriber to be notified
   */
  void register(PropertyEventSubscriber<T> subscriber);

  /**
   * Unregister a subscriber previously registered via register(PropertyEventSubscriber).
   * @param subscriber subscriber to be unsubscribed
   */
  void unregister(PropertyEventSubscriber<T> subscriber);

  /**
   * Register a subscriber to receive notification for events pertaining to the specified property.
   * The subscriber will receive exactly one onInitialize call with the current value of the
   * property (which may be null), followed by zero or more onAdd/onRemove calls as the value
   * of the property changes.
   * names.
   * @param propertyNames Property names to subscribe to
   * @param subscriber subscriber to be notified
   */
  void register(Set<String> propertyNames, PropertyEventSubscriber<T> subscriber);

  /**
   * Unregister a subcriber, previously notified via register(Set,PropertyEventSubscriber), from
   * receiving notification of the specified property names.
   * @param propertyNames Property names to unsubscribe from
   * @param subscriber subscriber to be unsubscribed
   */
  void unregister(Set<String> propertyNames, PropertyEventSubscriber<T> subscriber);

  /**
   * Sets the publisher for the bus.  There can only be one publisher at a time.  The bus
   * notifies the publisher when new properties are subscribed to, or when there are no longer
   * any listeners to a particular property.
   * @param publisher
   */
  void setPublisher(PropertyEventPublisher<T> publisher);

  /**
   * Publishes initialization of a property to the bus.
   * @param prop property name
   * @param value property value
   */
  void publishInitialize(String prop, T value);

  /**
   * Publishes a property value to the bus.
   * @param prop property name
   * @param value property value
   */
  void publishAdd(String prop, T value);

  /**
   * Publishes a property removal to the bus.  Property removal means that the named property
   * no longer has a value according to the publisher.
   * @param prop the name of the property
   */
  void publishRemove(String prop);
}

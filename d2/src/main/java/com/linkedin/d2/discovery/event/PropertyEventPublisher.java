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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public interface PropertyEventPublisher<T>
{
  /**
   * Set the bus to which the publisher should publish events.
   * @param bus the event sink
   */
  void setBus(PropertyEventBus<T> bus);

  /**
   * Notifies the publisher that at least one subscriber is interested in a property.  The
   * publisher may need to take some action such as listening to some other data source.
   * @param prop the property name
   */
  void startPublishing(String prop);

  /**
   * Notifies the publisher that no subscriber is interested in the named property.  However,
   * there may be subscribers to "all properties" still active.
   * @param prop the property name
   */
  void stopPublishing(String prop);

  void start(Callback<None> callback);

  void shutdown(PropertyEventThread.PropertyEventShutdownCallback callback);

}

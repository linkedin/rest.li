/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.common.util;

import java.util.function.Supplier;

/**
 * The Notifier allows different implementations to determine how errors/exceptions are reported, such as logging, throwing exceptions,
 * ignore, or rate limiting.
 */
public interface Notifier
{
  /**
   * Reports the exception to the notifier implementation
   * @param ex the exception to notify on
   */
  void notify(RuntimeException ex);

  /**
   * Reports the exception to the notifier implementation, possibly delaying or avoiding the exception's creation.
   * @param supplier the supplier instance that will create the exception if it's needed
   */
  default void notify(Supplier<RuntimeException> supplier)
  {
    notify(supplier.get());
  }
}

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

package com.linkedin.data;


import java.util.Random;


/**
 * Hash code for a data complex object. The objects of this class are assigned a hash code
 * on demand. This implementation aims to provide the same functionality as System.identityHashCode method.
 * The java implementation of identity hash code by default causes high latency and low throughput in
 * multi-threaded scenarios. The algorithm used by the java implementation can be changed by using
 * -XX:hashCode JVM argument. Since we cannot guarantee the usage of JVM args, an internal
 * implementation is necessary. In this hash code implementation a thread local variable is used to assign
 * new hash code values to objects. Thread local is picked to get the best performance in multi-threaded scenarios.
 * It is observed to scale better than plain int, volatile int and AtomicInteger.
 */
class DataComplexHashCode
{
  //Using a thread local counter to assign hash code for data complex objects. We do not
  //care about hash code collisions on different threads.
  private static ThreadLocal<IntegerWrapper> COUNTER = new ThreadLocal<IntegerWrapper>()
  {
    @Override
    protected IntegerWrapper initialValue()
    {
      return new IntegerWrapper();
    }
  };

  static int nextHashCode()
  {
    return COUNTER.get().getAndIncrement();
  }

  private static class IntegerWrapper
  {
    private int _counter = 0;

    int getAndIncrement()
    {
      // The hash code 0 is reserved for representing
      // uninitialized state. If the current counter value is
      // 0, we assign a random integer to it. A random integer is picked.
      // to minimize hash code collisions among different threads.
      while (_counter == 0)
      {
        Random rand = new Random();
        _counter = rand.nextInt();
      }

      int result = _counter;
      ++_counter;
      assert(result != 0);
      return result;
    }
  }
}

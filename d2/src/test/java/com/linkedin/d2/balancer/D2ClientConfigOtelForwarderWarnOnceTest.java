/*
   Copyright (c) 2026 LinkedIn Corp.

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
package com.linkedin.d2.balancer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/** Tests {@code D2ClientConfig.PRE_STRATEGY_OTEL_FORWARDER_WARNED} warn-once guard. */
public class D2ClientConfigOtelForwarderWarnOnceTest
{
  private static final String GUARD_FIELD_NAME = "PRE_STRATEGY_OTEL_FORWARDER_WARNED";

  @Test
  public void testGuardFieldIsPrivateStaticFinalAtomicBoolean() throws NoSuchFieldException
  {
    Field guard = D2ClientConfig.class.getDeclaredField(GUARD_FIELD_NAME);
    int modifiers = guard.getModifiers();
    assertTrue(Modifier.isPrivate(modifiers),
        GUARD_FIELD_NAME + " must remain private; otherwise external code can flip the guard "
            + "and suppress the deprecation warning entirely");
    assertTrue(Modifier.isStatic(modifiers),
        GUARD_FIELD_NAME + " must remain static; the warn-once contract is per-JVM, not "
            + "per-instance");
    assertTrue(Modifier.isFinal(modifiers),
        GUARD_FIELD_NAME + " must remain final so the reference can't be reseated after "
            + "JVM startup");
    assertEquals(guard.getType(), AtomicBoolean.class,
        GUARD_FIELD_NAME + " must remain an AtomicBoolean — a plain boolean is not safe under "
            + "concurrent first-time callers and would let the warning fire twice in a race");
  }

  @Test
  public void testGuardEmitsExactlyOnceAcrossRepeatedCompareAndSet() throws Exception
  {
    Field guardField = D2ClientConfig.class.getDeclaredField(GUARD_FIELD_NAME);
    guardField.setAccessible(true);
    AtomicBoolean guard = (AtomicBoolean) guardField.get(null);

    boolean originalState = guard.get();
    try
    {
      guard.set(false);

      assertTrue(guard.compareAndSet(false, true),
          "First compareAndSet on a fresh guard must return true so the deprecation warning is "
              + "emitted exactly once");
      assertFalse(guard.compareAndSet(false, true),
          "Subsequent compareAndSet must return false so the deprecation warning is suppressed");
      assertFalse(guard.compareAndSet(false, true),
          "Subsequent compareAndSet must remain false; the once-per-JVM contract is sticky");
    }
    finally
    {
      guard.set(originalState);
    }
  }

  @Test
  public void testGuardIsNotAnInstanceField() throws NoSuchFieldException
  {
    Field guard;
    try
    {
      guard = D2ClientConfig.class.getDeclaredField(GUARD_FIELD_NAME);
    }
    catch (NoSuchFieldException expected)
    {
      fail(GUARD_FIELD_NAME + " must exist on D2ClientConfig");
      return;
    }
    assertTrue(Modifier.isStatic(guard.getModifiers()),
        GUARD_FIELD_NAME + " must be static so the warn-once contract is per-JVM");
  }
}

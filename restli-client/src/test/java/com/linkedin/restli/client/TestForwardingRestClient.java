/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.client;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


/**
 * Tests using a bunch of reflection to make sure ForwardingRestClient and RestClient behave as expected. It should only
 * fail if RestClient or ForwardingRestClient was changed in a way that is incorrect.
 *
 * @author Gil Cottle
 */
public class TestForwardingRestClient {
  /**
   * Used to detect bad state
   */
  private static final Object SENTINEL = new Object();
  private static final String DEFAULT_STRING_RESPONSE = "BANANAS";

  /**
   * This will catch changes in RestClient that aren't made in ForwardingRestClient like making a method private or
   * package-private in RestClient
   */
  @Test
  public void validatePublicApiIdentical() {
    Set<MethodPrototype> restClientMethods = getPublicApiPrototypes(RestClient.class.getDeclaredMethods());
    Set<MethodPrototype> forwardingClientMethods =
        getPublicApiPrototypes(ForwardingRestClient.class.getDeclaredMethods());
    Assert.assertEquals(forwardingClientMethods, restClientMethods);
  }

  @Test(dataProvider = "clientApiDeclaredByRestClientMethods")
  public void validateClientCallDelegated(MethodArgHolder holder) throws Exception {
    validateCallDelegated(holder, true);
  }

  @Test(dataProvider = "restClientOnlyApiMethods")
  public void validateRestOnlyClientCallDelegated(MethodArgHolder holder) throws Exception {
    validateCallDelegated(holder, false);
  }

  /**
   * Makes sure the method call is delegated to the appropriate object. ForwardingRestClient shouldn't do any logic
   * or parameter manipulation.
   *
   * @param holder holder for arguments
   * @param useClient true if we should get calls to Client, false if this call should be passed to RestClient
   */
  private void validateCallDelegated(MethodArgHolder holder, boolean useClient) throws Exception {
    final Method m = holder._m;
    final Object[] args = holder._args;
    assertNoSentinels(args);
    AtomicInteger clientCallCount = new AtomicInteger();
    AtomicReference<Object> clientReturn = new AtomicReference<>(SENTINEL);

    Answer<Object> singleAnswer = invocation -> {
      Assert.assertEquals(new MethodPrototype(invocation.getMethod()), new MethodPrototype(m),
          "method called by ForwardingRestClient not the same");
      Assert.assertEquals(invocation.getArguments(), args, "arguments passed to ForwardingRestClient not the same");
      Assert.assertEquals(clientCallCount.incrementAndGet(), 1, "method called more than once");
      // Call and arguments are identical
      Class<?> returnType = invocation.getMethod().getReturnType();
      final Object returnValue = createMock(returnType);
      clientReturn.set(returnValue);
      return returnValue;
    };
    Answer<Object> errorAnswer = invocation -> {
      Assert.fail("Called " + invocation.getMock().getClass() + " but this wasn't expected");
      return null;
    };
    final Client client;
    final RestClient restClient;
    if (useClient) {
      client = mock(Client.class, singleAnswer);
      restClient = mock(RestClient.class, errorAnswer);
    } else {
      client = mock(Client.class, errorAnswer);
      restClient = mock(RestClient.class, singleAnswer);
    }
    @SuppressWarnings("deprecation")
    ForwardingRestClient forwardingRestClient = new ForwardingRestClient(client, restClient);
    Method forwardingMethod = getMethod(forwardingRestClient, m);
    Object response = forwardingMethod.invoke(forwardingRestClient, args);

    Assert.assertNotEquals(response, SENTINEL, "Delegate method not called");
    Assert.assertEquals(clientCallCount.get(), 1);
    Assert.assertEquals(response, clientReturn.get());
  }

  private static void assertNoSentinels(Object[] args) {
    if (args != null && args.length > 0) {
      for (Object arg : args) {
        Assert.assertNotEquals(arg, SENTINEL,
            "Sentinel value found, this means an error happened during dataProvider creation. Check the logs for details");
      }
    }
  }

  /**
   * @return get the equivalent method from the passed-in object
   */
  private static Method getMethod(Object o, Method m) throws NoSuchMethodException {
    return o.getClass().getMethod(m.getName(), m.getParameterTypes());
  }

  @DataProvider
  public static Object[][] clientApiDeclaredByRestClientMethods() {
    Set<MethodPrototype> restClientMethods = getPublicApiPrototypes(RestClient.class.getDeclaredMethods());
    // all methods of Client, not just declared, are potentially overriden in RestClient
    return getPublicApiPrototypes(Client.class.getMethods()).stream()
        .filter(restClientMethods::contains) // filters out default methods that aren't implemented by RestClient
        .map(MethodPrototype::getMethod)
        .map(m -> new Object[]{new MethodArgHolder(m, createMockParams(m))})
        .toArray(Object[][]::new);
  }

  @DataProvider
  public static Object[][] restClientOnlyApiMethods() {
    Set<MethodPrototype> clientMethods = getPublicApiPrototypes(Client.class.getMethods());
    return getPublicApiPrototypes(RestClient.class.getDeclaredMethods()).stream()
        .filter(m -> !clientMethods.contains(m))
        .map(MethodPrototype::getMethod)
        .map(m -> new Object[]{new MethodArgHolder(m, createMockParams(m))})
        .toArray(Object[][]::new);
  }

  private static Object[] createMockParams(Method m) {
    return Arrays.stream(m.getParameterTypes())
        .map(TestForwardingRestClient::createMock)
        .toArray();
  }

  private static Object createMock(Class<?> clazz) {
    try {
      if (clazz.isEnum()) {
        // pick first item from enum
        Class<?>[] emptyClassArgs = null;
        Method m = clazz.getMethod("values", emptyClassArgs);
        Object[] emptyObjectArgs = null;
        Object[] values = (Object[]) m.invoke(null, emptyObjectArgs);
        if (values.length > 0) {
          return values[0];
        } else {
          return null;
        }
      }
      if (clazz == Void.class || clazz == void.class) {
        return null;
      }
      if (clazz == String.class) {
        return DEFAULT_STRING_RESPONSE;
      }
      return mock(clazz);
    } catch (Exception e) {
      // If an error is thrown during dataProvider creation the test relying on the dataProvider is silently skipped.
      // Prevent this by throwing the error in the test instead.
      // If you're looking at this code, chances are you need to add primitive support because there's now a primitive
      // param in the Client method, something like: if (clazz == Boolean.class || clazz == boolean.class) return false;
      e.printStackTrace();
      return SENTINEL;
    }
  }

  /**
   * @return all public API Methods as MethodPrototypes
   */
  private static Set<MethodPrototype> getPublicApiPrototypes(Method[] methods) {
    return Arrays.stream(methods)
        .filter(m -> Modifier.isPublic(m.getModifiers()))
        .map(MethodPrototype::new)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Holds a Method and does equality based on the prototype: name, return type, and arguments.
   */
  private static class MethodPrototype {
    private final Method _m;

    private MethodPrototype(Method m) {
      _m = m;
    }

    @Override
    public boolean equals(Object o) {
      if (o.getClass() != MethodPrototype.class) {
        return false;
      }
      Method m1 = _m;
      Method m2 = ((MethodPrototype) o)._m;
      // We don't check class, we just want to know if a method is defined the same way
      return m1.getName().equals(m2.getName())
          && m1.getReturnType() == m2.getReturnType()
          && Arrays.equals(m1.getParameterTypes(), m2.getParameterTypes());
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          _m.getName(),
          _m.getReturnType(),
          Arrays.hashCode(_m.getParameterTypes())
      );
    }

    @Override
    public String toString() {
      return _m.toString();
    }

    public Method getMethod() {
      return _m;
    }
  }

  /**
   * testng won't let you pass {@link Method} arguments from a dataProvider. It passes the test method's name when you
   * do so. Use a data holder class instead for the arguments.
   */
  private static class MethodArgHolder {
    final Method _m;
    final Object[] _args;

    private MethodArgHolder(Method m, Object[] args) {
      _m = m;
      _args = args;
    }

    @Override
    public String toString() {
      return _m.toString();
    }
  }
}

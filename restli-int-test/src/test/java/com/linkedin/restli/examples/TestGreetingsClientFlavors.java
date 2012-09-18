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

package com.linkedin.restli.examples;

import org.testng.annotations.Test;

/**
 * Test various flavors of the greetings client.
 *
 * @author jnwang
 */
public class TestGreetingsClientFlavors
{
  /**
   * Test the promise version of the greetings client, GreetingsResourcePromise.
   */
  public static class TestPromise extends TestGreetingsClient
  {
    public TestPromise()
    {
      super("greetingsPromise");
    }

    @Test
    public void dummyTest()
    {
      // make TestNG execute subclass test cases
    }
  }

  /**
   * Test the callback version of the greetings client, GreetingsResourceCallback.
   */
  public static class TestCallback extends TestGreetingsClient
  {
    public TestCallback()
    {
      super("greetingsCallback");
    }

    @Test
    public void dummyTest()
    {
      // make TestNG execute subclass test cases
    }
  }

  /**
   * Test the promise/context version of the greetings client, GreetingsResourcePromiseCtx
   */
  public static class TestPromiseCtx extends TestGreetingsClient
  {
    public TestPromiseCtx()
    {
      super("greetingsPromiseCtx");
    }

    @Test
    public void dummyTest()
    {
      // make TestNG execute subclass test cases
    }
  }

  /**
   * Test the synchronous version of the greetings client, GreetingsResource
   */
  public static class TestSync extends TestGreetingsClient
  {
    public TestSync()
    {
      super("greetings");
    }

    @Test
    public void dummyTest()
    {
      // make TestNG execute subclass test cases
    }
  }

  /**
   * Test the task version of the greetings client, GreetingsResourceTask
   */
  public static class TestTask extends TestGreetingsClient
  {
    public TestTask()
    {
      super("greetingsTask");
    }

    @Test
    public void dummyTest()
    {
      // make TestNG execute subclass test cases
    }
  }
}

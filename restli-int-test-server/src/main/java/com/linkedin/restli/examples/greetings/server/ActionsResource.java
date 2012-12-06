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

package com.linkedin.restli.examples.greetings.server;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.linkedin.common.callback.Callback;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.Tasks;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.restli.examples.greetings.api.Message;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.ParSeqContext;
import com.linkedin.restli.server.annotations.RestLiActions;

/**
 * Various action tasks that demonstrate usual behavior, timeout, and exceptions.
 *
 * @author Josh Walker
 * @version $Revision: $
 */
@RestLiActions(name = "actions",
               namespace = "com.linkedin.restli.examples.greetings.client")
public class ActionsResource
{
  private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final static int                      DELAY     = 100;

  @Action(name="returnInt")
  public int returnPrimitive()
  {
    return 0;
  }

  @Action(name="returnBool")
  public boolean returnBool()
  {
    return true;
  }

  @Action(name="ultimateAnswer")
  public Integer testAction()
  {
    return 42;
  }

  @Action(name="get")
  public String get()
  {
    return "Hello, World";
  }

  @Action(name="echo")
  public String echo(@ActionParam("input") final String input)
  {
    return input;
  }

  @Action(name="echoMessage")
  public Message echoMessage(@ActionParam("message") final Message message)
  {
    return message;
  }

  @Action(name = "timeout")
  public Promise<Void> timeout()
  {
    return Promises.settable();
    // do nothing
  }

  private static Task<String> makeTaskA(final int a) {
    return new BaseTask<String>("geta")
    {
      @Override
      protected Promise<? extends String> run(final Context context) throws Exception
      {
        final SettablePromise<String> result = Promises.settable();
        Runnable requestHandler = new Runnable()
        {
          public void run()
          {
            result.done(Integer.toBinaryString(a));
          }
        };
        scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
        return result;
      }
    };
  }
  private static Task<String> makeTaskB(final String b) {
    return new BaseTask<String>("getb")
    {
      @Override
      protected Promise<? extends String> run(final Context context) throws Exception
      {
        final SettablePromise<String> result = Promises.settable();
        Runnable requestHandler = new Runnable()
        {
          public void run()
          {
            result.done(b.toUpperCase());
          }
        };
        scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
        return result;
      }
    };
  }
  private static Task<String> makeTaskC(final boolean c) {
    return new BaseTask<String>("getc")
    {
      @Override
      protected Promise<? extends String> run(final Context context) throws Exception
      {
        final SettablePromise<String> result = Promises.settable();
        Runnable requestHandler = new Runnable()
        {
          public void run()
          {
            result.done(Boolean.toString(c));
          }
        };
        scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
        return result;
      }
    };
  }
  private static Task<String> makeConcatTask(final Task<?>... tasks) {
    return Tasks.callable("concat", new Callable<String>()
    {
      @Override
      public String call() throws Exception
      {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final Task<?> t : tasks)
        {
          if (!first)
            sb.append(" ");
          first = false;
          sb.append(t.get());
        }
        return sb.toString();
      }
    });
  }

  /**
   * Performs three "slow" tasks and collects the results. This uses the passed context
   * parameter to execute tasks. The position of the context argument is arbitrary.
   *
   * @return concatenation of binary representation of a, all caps of b, and string value
   *         of c
   */
  @Action(name = "parseq")
  public Promise<String> parseqAction(@ActionParam("a") final int a,
                                      @ActionParam("b") final String b,
                                      @ParSeqContext com.linkedin.parseq.Context ctx,
                                      @ActionParam("c") final boolean c)
  {
    final Task<String> t1 = makeTaskA(a);
    final Task<String> t2 = makeTaskB(b);
    final Task<String> t3 = makeTaskC(c);
    final Task<String> collect = makeConcatTask(t1, t2, t3);

    ctx.after(t1, t2, t3).run(collect);
    ctx.run(t1, t2, t3);
    return collect;
  }

  /**
   * Performs three "slow" tasks and collects the results. This returns a task and lets
   * the RestLi server invoke it.
   *
   * @return concatenation of binary representation of a, all caps of b, and string value
   *         of c
   */
  @Action(name = "parseq2")
  public Task<String> parseqAction2(@ActionParam("a") final int a,
                                    @ActionParam("b") final String b,
                                    @ActionParam("c") final boolean c)
  {
    final Task<String> t1 = makeTaskA(a);
    final Task<String> t2 = makeTaskB(b);
    final Task<String> t3 = makeTaskC(c);
    final Task<String> collect = makeConcatTask(t1, t2, t3);

    return Tasks.seq(Tasks.par(t1, t2, t3), collect);
  }

  /**
   * Action that fails by throwing an exception, returning a promise
   */
  @Action(name = "failPromiseThrow")
  public Promise<Void> failPromiseThrow()
  {
    throw new RuntimeException("This is an error.");
  }

  /**
   * Action that fails by throwing an exception, returning a task
   */
  @Action(name = "failTaskThrow")
  public Task<Void> failTaskThrow()
  {
    throw new RuntimeException("This is an error.");
  }

  /**
   * Action that fails by calling SettablePromise.fail
   */
  @Action(name = "failPromiseCall")
  public Promise<Void> failPromiseCall()
  {
    final SettablePromise<Void> result = Promises.settable();
    final Runnable requestHandler = new Runnable()
    {
      public void run()
      {
        result.fail(new Exception("Passing error to SettablePromise.fail"));
      }
    };
    scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
    return result;
  }

  /**
   * Action that fails by calling SettablePromise.fail promise in a task
   */
  @Action(name = "failTaskCall")
  public Task<Void> failTaskCall()
  {
    return new BaseTask<Void>("failTaskCall")
    {
      @Override
      protected Promise<? extends Void> run(final Context context) throws Exception
      {
        final SettablePromise<Void> result = Promises.settable();
        Runnable requestHandler = new Runnable()
        {
          public void run()
          {
            result.fail(new Exception("Passing error to SettablePromise.fail in Task"));
          }
        };
        scheduler.schedule(requestHandler, DELAY, TimeUnit.MILLISECONDS);
        return result;
      }
    };
  }

  /**
   * Action that fails by throwing an exception in the task
   */
  @Action(name = "failThrowInTask")
  public Task<Void> failThrowInTask()
  {
    return new BaseTask<Void>("failThrowInTask")
    {
      @Override
      protected Promise<? extends Void> run(final Context context) throws Exception
      {
        throw new RuntimeException("Throwing exception in task");
      }
    };
  }

  @Action(name = "nullTask")
  public Task<String> nullTask()
  {
    return null;
  }

  @Action(name = "nullPromise")
  public Promise<String> nullPromise()
  {
    return null;
  }

  @Action(name = "timeoutCallback")
  public void timeout(@CallbackParam final Callback<Void> callback)
  {
    // do nothing
  }

  /**
   * Action that fails by throwing an exception
   */
  @Action(name = "failCallbackThrow")
  public void failThrow(@CallbackParam final Callback<Void> callback)
  {
    throw new RuntimeException("This is an error.");
  }

  /**
   * Action that fails by calling the callback
   */
  @Action(name = "failCallbackCall")
  public void failCall(@CallbackParam final Callback<Void> callback)
  {
    callback.onError(new Exception("Passing error to callback.onError"));
  }
}

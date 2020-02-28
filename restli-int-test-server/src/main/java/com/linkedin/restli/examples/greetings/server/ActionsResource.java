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


import com.linkedin.restli.examples.custom.types.CustomLong;
import com.linkedin.restli.examples.typeref.api.CustomLongRef;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.ActionResult;
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
import com.linkedin.restli.examples.greetings.api.Tone;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.ParSeqContextParam;
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

  @Action(name="returnVoid")
  public ActionResult<Void> returnVoid()
  {
    return new ActionResult<>(HttpStatus.S_200_OK);
  }

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

  @Action(name="echoStringArray")
  public String[] echoStringArray(@ActionParam("strings") final String[] inputs)
  {
    return inputs;
  }

  @Action(name="echoMessage")
  public Message echoMessage(@ActionParam("message") final Message message)
  {
    return message;
  }

  @Action(name="echoMessageArray")
  public Message[] echoMessage(@ActionParam("messages") final Message[] messages)
  {
    return messages;
  }

  @Action(name="echoToneArray")
  public Tone[] echoToneArray(@ActionParam("tones") final Tone[] tones)
  {
    return tones;
  }

  @Action(name = "customTypeRef", returnTyperef= CustomLongRef.class)
  public CustomLong customTypeRef(@ActionParam(value="customLong", typeref=CustomLongRef.class) CustomLong customLong)
  {
    return customLong;
  }

  @Action(name = "timeout")
  public Promise<Void> timeout()
  {
    return Promises.settable();
    // do nothing
  }

  @Action(name="returnIntOptionalParam")
  public int returnIntOptionalParam(@Optional @ActionParam ("param") final Integer param)
  {
    return (param == null? 0 : param);
  }

  @Action(name="returnBoolOptionalParam")
  public boolean returnBoolOptionalParam(@Optional @ActionParam("param") final Boolean param)
  {
    return (param == null? Boolean.TRUE : param);
  }

  private static <T> Task<T> delayedTask(String name, final long delay, final T result)
  {
    return new BaseTask<T>(name)
    {
      @Override
      protected Promise<T> run(Context context)
      {
        final SettablePromise<T> promise = Promises.settable();
        Runnable requestHandler = new Runnable()
        {
          public void run()
          {
            promise.done(result);
          }
        };
        scheduler.schedule(requestHandler, delay, TimeUnit.MILLISECONDS);
        return promise;
      }
    };
  }

  private static Task<String> makeTaskA(final int a) {
    return delayedTask("geta", DELAY, Integer.toBinaryString(a));
  }

  private static Task<String> makeTaskB(final String b) {
    return delayedTask("getb", DELAY, b.toUpperCase());
  }
  private static Task<String> makeTaskC(final boolean c) {
    return delayedTask("getc", DELAY, Boolean.toString(c));
  }

  private static Task<String> makeConcatTask(final Task<?>... tasks) {
    return Task.callable("concat", new Callable<String>()
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
                                      @ParSeqContextParam com.linkedin.parseq.Context ctx,
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

//  This test cannot be compiled until we build with Java 8 by default.
//  /**
//   * Performs three "slow" tasks and collects the results. This uses the passed context
//   * parameter to execute tasks. The position of the context argument is arbitrary.
//   *
//   * @return concatenation of binary representation of a, all caps of b, and string value
//   *         of c
//   */
//  @Action(name = "parseq2")
//  @SuppressWarnings("deprecation")
//  public Promise<String> parseqAction2(
//      @ActionParam("a") final int a,
//      @com.linkedin.restli.server.annotations.ParSeqContext com.linkedin.parseq.Context ctx,
//      @ActionParam("b") final String b,
//      @ActionParam("c") final boolean c)
//  {
//    final Task<String> t1 = makeTaskA(a);
//    final Task<String> t2 = makeTaskB(b);
//    final Task<String> t3 = makeTaskC(c);
//    final Task<String> collect = makeConcatTask(t1, t2, t3);
//
//    ctx.after(t1, t2, t3).run(collect);
//    ctx.run(t1, t2, t3);
//    return collect;
//  }

  /**
   * Performs three "slow" tasks and collects the results. This returns a task and lets
   * the RestLi server invoke it.
   *
   * @return concatenation of binary representation of a, all caps of b, and string value
   *         of c
   */
  @Action(name = "parseq3")
  public Task<String> parseqAction3(@ActionParam("a") final int a,
                                    @ActionParam("b") final String b,
                                    @ActionParam("c") final boolean c)
  {
    final Task<String> t1 = makeTaskA(a);
    final Task<String> t2 = makeTaskB(b);
    final Task<String> t3 = makeTaskC(c);
    final Task<String> collect = makeConcatTask(t1, t2, t3);

    return Task.par(t1, t2, t3).andThen(collect);
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

  @Action(name = "arrayPromise")
  public Promise<Integer[]> arrayPromise()
  {
    return null;
  }

  /**
   * Simulates a delay in an asynchronous resource caused by ParSeq execution plan creation. The delay is simulated as
   * {@link Thread#sleep(long)} because execution plan creation is a synchronous operation.
   * @param delayMillis the number of milliseconds it will take this resource to create an execution plan
   * @return nothing
   */
  @Action(name = "taskCreationDelay")
  public Task<Void> taskCreationDelay(@ActionParam("delay") int delayMillis) throws InterruptedException
  {
    // simulate a long running blocking computation representing time taken for plan creation
    Thread.sleep(delayMillis);
    return delayedTask("taskCreationDelayTask", 0, null);
  }

  /**
   * Simulates a delay in an asynchronous resource. The delay is simulated using a scheduled task (asynchronously).
   * That is how a typical async resource looks like in terms of delays.
   * @param delayMillis the number of milliseconds it will take this resource to create an execution plan
   * @return nothing
   */
  @Action(name = "taskExecutionDelay")
  public Task<Void> taskExecutionDelay(@ActionParam("delay") final int delayMillis)
  {
    return delayedTask("taskExecutionDelayTask", delayMillis, null);
  }
}

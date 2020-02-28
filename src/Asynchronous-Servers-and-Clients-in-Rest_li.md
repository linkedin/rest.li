---
layout: guide
title: Asynchronous Servers and Clients In Rest.li
permalink: /Asynchronous-Servers-and-Clients-in-Rest_li
excerpt: Rest.li is asynchronous and non-blocking under the hood. This section covers asynchronous servers and slients in Rest.li
---

# Asynchronous Servers And Clients In Rest.li

## Contents

* [Introduction](#introduction)
* [Async Server Implementations](#async-server-implementations)
* [Async Client Implementations](#async-client-implementations)

This section assumes you are familiar with [ParSeq](https://github.com/linkedin/parseq) and its [key concepts](https://github.com/linkedin/parseq/wiki/User's-Guide#key-concepts).

## Introduction

Rest.li is asynchronous and non-blocking under the hood:

* **R2** - On the client side, R2 uses a [Netty](http://netty.io/) based asynchronous client. On the server side if you are using our experimental Netty server, it is async and non-blocking. If you are using [Jetty](http://www.eclipse.org/jetty/), you need to [configure it](#server-configuration) to run in async mode.
* **D2** - All communication with [ZooKeeper](http://zookeeper.apache.org/) uses the async APIs.
* **Rest.li** - Rest.li does not handle I/O. All I/O work is done by R2, which is async and non-blocking as explained above. Rest.li uses [ParSeq](https://github.com/linkedin/parseq) to interact with and delegate to server application code. The `RestClient` used to make Rest.li requests on the client-side has several options in order to write async non-blocking code.

## Async Server Implementations

As shown above the Rest.li framework is async and non-blocking under the hood. R2 uses non-blocking I/O via both Netty and Jetty to serve client traffic using Java NIO. Resource methods are invoked in an event driven fashion. 

As a result of this, if you do any blocking work in your method implementation, it can negatively impact your application throughput as threads are held up by your application that are needed by Rest.li **(if you enable async mode in your Rest.li server)**. It is the responsibility of application developers to ensure that their method implementations are async and use non-blocking I/O.

There are two main options available to write async server implementations in Rest.li:

1. Using a `com.linkedin.common.Callback` (included in Rest.li)
2. Using ParSeq

### Async Server Templates

Rest.li also includes templates to make writing async resources slightly easier. There are templates that use `Callback`s and `Task`s for each type of Rest.li resource. For example, for collection resources with primitive keys we have `com.linkedin.restli.server.resources.CollectionResourceAsyncTemplate` and `com.linkedin.restli.server.resources.CollectionResourceTaskTemplate`. There are similar templates for complex key resources, association resources, and simple resources.

### Server Configuration

Async request handling is available for Servlet API 3.0 or greater and is enabled by default for 3.0+.

To us async mode in Servlet containers, `async-supported` must be set to `true` in your `web.xml`. 

Example:

```xml
<servlet>
  ...
  <async-supported>true</async-supported>
  ...
</servlet>
```

[Here](https://github.com/linkedin/rest.li/blob/master/examples/quickstart/server/src/main/webapp/WEB-INF/web.xml#L13) is an example configuration for our quick start example server.

Async can be further configured by the Rest.li `uscAsync` servlet param (which defaults to `true` for servlet API 3.0+ servlet containers). The `asyncTimeout` param can be set to a desired maximum timeout value in milliseconds. 

Example:
 
```xml
<init-param>
  <param-name>useAysnc</param-name>
  <param-value>true</param-value>
</init-param>
<init-param>
  <param-name>asyncTimeout</param-name>
  <param-value>30000</param-value>
</init-param>
```
 
### Using Callbacks

Consider the following implementation of an async GET method using a `Callback`. In this example, we fetch data from ZooKeeper asynchronously. Based on the data that we get back, we either return a `404` to the user or build a `Greeting` `RecordTemplate`. We will use this example to understand how to write `Callback` based Rest.li async method implementations.

Example:

```java
@RestMethod.Get
public void get(final Long id, @CallbackParam final Callback<Greeting> callback) {
  String path = "/data/" + id;
  // _zkClient is a regular ZooKeeper client
  _zkClient.getData(path, false, new DataCallback() {
    public void processResult(int i, String s, Object o, byte[] b, Stat st) {
      if (b.length == 0) {
        callback.onError(new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
      }
      else {
        callback.onSuccess(buildGreeting(b));
      }
    }
  }, null);
}
```

#### Signature

```java
@RestMethod.Get
public void get(final Long id, @CallbackParam final Callback<Greeting> callback) {
```

In order to use a `Callback`, we need to set the return type of the function to be `void` and pass in a `Callback<T>` as a parameter to the function. `T` here is whatever type you would have returned from a synchronous implementation of the same function. In this case, the synchronous implementation would have returned a `Greeting`, which is why we are returning a `Callback<Greeting>` here. The `@CallbackParam` is required.

#### Function Body

```java
_zkClient.getData(path, false, new DataCallback() {
  public void processResult(int i, String s, Object o, byte[] b, Stat st) {
    if (b.length == 0) {
      callback.onError(new RestLiServiceException(HttpStatus.S_404_NOT_FOUND));
    }
    else {
      callback.onSuccess(buildGreeting(b));
    }
  }
}, null);
```

We use the async ZooKeeper `getData` API call to fetch data from ZooKeeper. Based on the data we get back from ZooKeeper in the `DataCallback` (which is a **ZooKeeper** construct), we invoke either the `onError` or the `onSuccess` method on the `Callback` interface.

`onError` is used to signify that something went wrong. In this case, we invoke `onError` with a `RestliServiceException` when the length of data that we get back from ZooKeeper is 0. The Rest.li framework translates this `Exception` into an appropriate REST response to send back to the client.

In case we get back data that has non-zero length, we build a `Greeting` object from it in the `buildGreeting` method (not shown here) and return that to the client by invoking the `onSuccess` method. 

All you have to do is invoke the `onError` or `onSuccess` method within your method, and the Rest.li framework will return data back to the client. This is why the return type for the method is `void`, since the `Callback` is used to return values back to the client.

#### Callback Execution

It is up to the application developer to execute the `Callback`. **Rest.li does not execute the `Callback` for you**. In the above example, the `Callback` was executed by the ZooKeeper thread, which is why we didn't have to explicitly execute it. However, once the `onError` or `onSuccess` method has been invoked in the `Callback`, Rest.li will translate that into a response to send back to the client. In other words, `Callback` execution is up to the application developer, but sending a response back once the `Callback` has been executed is handled by Rest.li.

### Using ParSeq

Consider the following example of an async GET implementation that uses ParSeq:

```java
@RestMethod.Get
public Task<Greeting> get(final Long id) {
  final Task<FileData> fileDataTask = buildFileDataTask();
  final Task<Greeting> mainTask = Tasks.callable("main", new Callable<Greeting>() {
    @Override
    public Greeting call() throws Exception {
      FileData fileData = fileDataTask.get();
      return buildGreetingFromFileData(id, fileData);
    }
  });
  return Tasks.seq(fileDataTask, mainTask);
}
```

`buildFileDataTask` (implementation not shown here) reads some file on disk using async I/O and returns a `Task<FileData>`, where `FileData` (implementation not shown here) is some abstraction for the data being read. We use `FileData` to build a `Greeting` to return to the client. 

#### Signature

```java
@RestMethod.Get
public Task<Greeting> get(final Long id) {
```

In order to use ParSeq the function implementation must return a ParSeq `Task<T>`. `T` here is whatever type you would have returned from a synchronous implementation of the same function. In this case the synchronous implementation would have returned a `Greeting`, which is why we are returning a `Task<Greeting>` here.

#### Function Body

```java
final Task<FileData> fileDataTask = buildFileDataTask();
final Task<Greeting> mainTask = Tasks.callable("main", new Callable<Greeting>() {
  @Override
  public Greeting call() throws Exception {
    FileData fileData = fileDataTask.get();
    return buildGreetingFromFileData(id, fileData);
  }
});
return Tasks.seq(fileDataTask, mainTask);
```

The basic idea to use ParSeq for Rest.li async method implementations is to return a `Task`.

`fileDataTask` is a `Task` for the `FileData` that we read from disk. We want to transform this `FileData` into a `Greeting` to return to the user. We define a new `Task`, called `mainTask` in the example above, to do this.

Within the `call` method of `mainTask`, we obtain the `FileData` from the `fileDataTask`. This is a non-blocking call because of the way we assemble our final call graph (more on this in a bit). Finally, we build a `Greeting` in the `buildGreetingFromFileData` (implementation not shown here) method.

So we have two `Task`s now, `fileDataTask` and `mainTask`, with `mainTask` depending on the result of `fileDataTask`. `mainTask` also builds the `Greeting` object that we want to return to the client. In order to build this dependency between the two `Task`s, we use the `Tasks.seq` method.

#### Task Execution

It is suggested to return a `Task` from your method. In the above example, `Task.seq(fileDataTask, mainTask)` returns a new `Task` that is **executed for you automatically** using the ParSeq engine within Rest.li. In other words, you do not have to provide a separate ParSeq execution engine to run this `Task`. Rest.li runs the `Task` for you and returns an appropriate response to the client.

#### Promise Execution

Even though the example above returns a `Task`, it is worth pointing out the difference between returning a `Task` and returning a `Promise` from an async method implementation. If you return a `Promise` from a resource method, Rest.li creates a `Task` for you to resolve the `Promise`. This `Task` is then run on the underlying ParSeq engine within Rest.li.

One thing to keep in mind is that if you are returning a `Task` from your method you should **never** return it as a `Promise`! This is because the Rest.li framework will wrap this `Promise` in a new `Task` to execute it, and thus two ParSeq `Task`s are created. In our performance benchmarks, we have noticed that this one extra `Task` can lead to a noticeable increase in latency, especially in high QPS scenarios.

#### Promise or Task?

If you are using ParSeq, you can return either a `Promise` or a `Task` from your resource method implementation. In general, you should return a `Task`. The `Promise` templates are deprecated and should not be used for new development.

## Async Client Implementations

There are two main options available to make async requests using Rest.li:

1. Using a `com.linkedin.common.Callback`
2. Using a `com.linkedin.restli.client.ParSeqRestClient` (included in Rest.li)

### Using Callbacks

Here is a partial example of making a GET request to the /greetings resource and then using the result asynchronously:

```java
Callback<Response<Greeting>> cb = new Callback() {
  void onSuccess(Response<Greeting> response) {
    // do something with the returned Greeting
  }
  void onError(Throwable e) {
    // whoops
  }
}

Request<Greeting> getRequest = BUILDERS.get().id(1L).build();

_restClient.sendRequest(getRequest, new RequestContext(), cb);

```

#### Defining the Callback

```java
Callback<Response<Greeting>> cb = new Callback() {
  void onSuccess(Response<Greeting> response) {
    // do something with the returned Greeting
  }
  void onError(Throwable e) {
    // whoops
  }
}
```

We need to define the `Callback` that will be executed when we get back a `Response` from the server. `onSuccess` will be invoked by Rest.li on getting a non-exception result (i.e. a `Response`), while `onError` will be invoked by Rest.li in case an `Exception` is thrown. The key concept to note here is that **Rest.li invokes the `Callback` from you once we get a `Response` or an `Exception` is thrown**.

#### Sending the Request

```java
Request<Greeting> getRequest = BUILDERS.get().id(1L).build();
_restClient.sendRequest(getRequest, new RequestContext(), cb);
```

We pass the `Callback` we defined previously as the last parameter of the `sendRequest` call. This calls returns right away, because as stated above, Rest.li invokes the `Callback` for you appropriately. 

### Using a ParSeqRestClient

A `ParSeqRestclient` is simply a wrapper around the standard `RestClient` that returns a ParSeq `Task` or `Promise`. Here is an example that uses a `ParSeqRestClient` to make two asynchronous Rest.li requests in parallel and then print out the results:

```java
Request<Greeting> greetingsRequest = GREETINGS_BUILDERS.get().id(1).build();
Request<Fortune> fortunesRequest = FORTUNES_BUILDERS.get().id(1).build();

Task<Response<Greeting>> greetingsResponseTask = _parseqRestClient.createTask(greetingsRequest);
Task<Response<Fortune>> fortunesResponseTask = _parseqRestClient.createTask(fortunesRequest);

Task<Void> printTask = Tasks.callable("printTask", new Callable<Void> {
  @Override
  public Void call() throws Exception {
    Greeting greeting = greetingsResponseTask.get().getEntity();
    Fortune fortune = fotunesResponseTask.get().getEntity();
    System.out.println(greeting + ", " + fortune);
    return null;
  }
});

_engine.run(Tasks.seq(Tasks.par(greetingsResponseTask, fortunesResponseTask), printTask));
```

#### Task Creation

The `ParSeqRestClient` includes APIs to get back a `Task` or `Promise` corresponding to the `Response` of a `Request`. In the example above, we build out a `Request` for a `Greeting` and `Fortune` using the standard generated builders. We then use the `createTask` API to get back a `Task<Response<Greeting>>` and `Task<Response<Fortune>>`. **This does not send the request out over the network!** It simply gives you back a `Task`, which, when run on a ParSeq engine, would give you back a `Response<Greeting>` or `Response<Fortune>`. 

To print out the result we get back from the server, we define a third `Task` named `printTask`. In this `Task`, we get the `Greeting` and `Fortune` from the previous `Task`s and print it out. 

#### Building the Call Graph and Running the Tasks

Now that we have the `Task`s that we want to run, we need to build the call graph and execute the `Task`s on a ParSeq engine.

We want to make our Rest.li requests in parallel. To do this we use the `Tasks.par` method to run `greetingsResponseTask` and `fortunesResponseTask` in parallel. We want to run `printTask` after both these `Task`s have completed, which is why we use the `Tasks.seq` method `Tasks.seq(Tasks.par(greetingsResponseTask, fortunesResponseTask), printTask)`. This gives us the final `Task` that we run on our ParSeq engine using the `run` method.

#### Using Promises

`ParSeqRestClient` also has a `Promise` based API. It is very similar to the `Callback` based approach of sending a request. The main difference is that instead of passing in a `Callback` to the `sendRequest` call we attach a `PromiseListener` that is invoked asynchronously when we get a result from the server. Here is the [Callback example](#using-callbacks-1) implemented using the `Promise` API:

```java
Request<Greeting> getRequest = BUILDERS.get().id(1L).build();

// this call sends the request over the wire and returns a Promise
Promise<Response<Greeting>> greetingPromise = _parseqRestClient.sendRequest(getRequest);

greetingPromise.addListener(new PromiseListener<Response<Greeting>>() {
  @Override
  public void onResolved(Promise<Response<Greeting>> promise) {
    if (promise.isFailed()) {
      Throwable t = promise.getError();
      // something went wrong
    }
    else {
      Response<Greeting> response = promise.get();
      // do something with the returned Greeting
    }
  }
});
```
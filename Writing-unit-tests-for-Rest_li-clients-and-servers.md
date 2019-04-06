---
layout: guide
title: Writing-unit-tests-for-Rest.li-clients-and-servers
permalink: /Writing-unit-tests-for-Rest_li-clients-and-servers
---
# Writing Unit Tests for Rest.li

## Contents

* [Introduction](#introduction)
* [Writing Unit Tests for Rest.li Clients](#writing-unit-tests-for-restli-clients)
* [Writing Unit Tests for Rest.li Servers](#writing-unit-tests-for-restli-servers)
* [Writing Unit Tests for Rest.li Data (RecordTemplates and DataMaps)](#writing-unit-tests-for-restli-data-recordtemplates-and-datamaps)

## Introduction

The Rest.li team added classes to Rest.li (starting with Rest.li 1.14.7) to make writing unit tests for Rest.li clients and servers easier. These classes are spread across three modules: `restli-client-testutils`, `restli-common-testutils`, and `restli-server-testutils`.

## Writing Unit Tests for Rest.li Clients

The classes that help writing unit tests for Rest.li clients are present in `restli-client-testutils`. These classes are mainly builders and factories that help in creating different types of `com.linkedin.restli.client.Response` objects. The expected use case for these classes is where you want to test that your code is able to process a `Response` object that it receives from making a Rest.li request. 

Suppose you want to test a method that makes a GET request which returns a `Greeting` entity and then this method returns the property `message` from this entity. Your code that you want to test might look something like this:

```java
public class MyApplication
{
  ...
  
  public String getMessage(long id) throws Exception  
  {
    GetRequest<Greeting> getRequest = GREETINGS_BUILDERS.get().id(id).build();
    return _restClient.sendRequest(getRequest).getResponseEntity().getMessage();
  }
  
  ...
}
```

To test this method, your code might look like this:

```java
GetRequest<Greeting> expectedRequest = GREETINGS_BUILDERS.get().id(id).build();

// use the test utilities to construct a response that is processed by the method
// we are testing
Greeting mockGreeting = new Greeting().setId(1L).setMessage("test message");
MockSuccessfulResponseFutureBuilder<Greeting> responseFutureBuilder = 
  new MockSuccessfulResponseFutureBuilder<Greeting>().setEntity(expectedGreeting);
ResponseFuture<Greeting> mockResponse = responseFutureBuilder.build();

// assume myApplication is an instance of MyApplication
// assume mockClient is a mock RestClient that has been created using EasyMock
EasyMock.expect(mockClient.sendRequest(expectedRequest)).andReturn(mockResponse);
Assert.assertEquals(myClass.getMessage(1L), "test message");
```

#### MockResponseBuilder

`MockResponseBuilder` is a builder that can be used to easily construct `Response` objects. You can use it to set the headers, entity, HTTP response code, and so on, of the `Response` object. The use case that we envision for this class is when you are testing code that uses a version of `sendRequest` in `RestClient` that takes in a `CallBack<Response<T>>` as one of it's parameters. The `CallBack` argument can be filled in using this builder.

#### MockSuccessfulResponseFutureBuilder

The `MockSuccessfulResponseFutureBuilder` class is used to construct `ResponseFuture`s that represent a successful Rest.li call, for example, no exceptions are thrown and the HTTP status code is 2xx. This builder can be used to set the entity, HTTP response code, and so on, of the response.

#### MockFailedResponseFutureBuilder

The `MockFailedResponseFutureBuilder` class is used to construct `ResponseFuture`s that represent either a failed Rest.li call. This failed call can be of two types:

1. The call failed completely and the server did not return any result. For example, you make a GET request but there is no entity for that ID, resulting in a 404.
2. The call failed but the server still sent a response. For example you make a GET request and the server returns a partially constructed entity with some fields not filled out because of an internal server problem. In this case, there is an entity in the response as well as an error.

Please see the JavaDoc on this class for a detailed explanation on how each of the scenarios above can be modeled. This builder also lets us define how server exceptions should be treated using the `setErrorHandlingBehavior(ErrorHandlingBehavior errorHandlingBehavior)` method. Again, please see the JavaDoc for more details.

#### MockBatchCreateIdResponseFactory, MockBatchKVResponseFactory, MockBatchEntityResponseFactory, MockBatchResponseFactory, and MockCollectionResponseFactory

The other classes in this module, namely `MockBatchCreateIdResponseFactory`, `MockBatchKVResponseFactory`, `MockBatchEntityResponseFactory`, `MockBatchResponseFactory`, and `MockCollectionResponseFactory` are used to construct specific types of `RecordTemplate` objects. The use case for these classes is that they will be used to construct a specific type of entity which can then be used in the `MockResponseBuilder`, `MockSuccessfulResponseFutureBuilder`, or `MockFailedResponseFutureBuilder` as the entity object. For example, say you are making a request to a Rest.li finder on a `/greetings` resource. You can model the fact that this request returns a `CollectionResponse<Greeting>` as follows:

```java
// assume "data" is what we want to return
CollectionResponse<Greeting> mockResponse = MockCollectionResponseFactory.create(Greetings.class, data);
MockSuccessfulResponseFutureBuilder<CollectionResponse<Greeting>> responseFutureBuilder = 
  new MockSuccessfulResponseFutureBuilder<CollectionResponse<Greeting>>().setEntity(expectedGreeting);
// use the ResponseFuture that you will get from the builder in tests
``` 

## Writing Unit Tests for Rest.li Servers

#### MockHttpServerFactory

The `MockHttpServerFactory` class is used to create a stand-alone Rest.li server easily that you can then write tests against. The primary use case for this class would be to bring up a Rest.li server at the start of the test, and then each test can test a different endpoint that this server supports. This factory also allows us to pass in beans that would have been injected into your Rest.li server by the Rest.li framework. For more information on bean injection in a Rest.li server, see the [documentation on Dependency Injection](/rest.li/user_guide/restli_server#dependency-injection). 

Suppose we want to test that we have implemented the GET method correctly for our resource `GreetingsResource`. Let's assume that `GreetingsResource` needs one bean named "db" of type `DataBase` to be injected. Here is what our test might look like (assuming we are using TestNG):

```java

public class TestGreetingsResource
{
  private HttpServer _testServer;
  private Client _restClient;
  
  private static final int PORT = 7777;
  private static final String HOST = "http://localhost:" + PORT;
  private static final GreetingsBuilders GREETINGS_BUILDERS = new GreetingsBuilders();
  
  @BeforeTest
  public void init()
  {
    DataBase testDataBase = new TestDataBase();
    Map<String, Object> beans = new HashMap<String, Object>();
    beans.put("db", testDataBase);
    Set<Class> resourceClasses = new HashSet<Class>();
    resourceClasses.put(GreetingsResource.class);
    
    // use the factory to create a test server
    _testServer = MockHttpServerFactory.create(PORT, resourceClasses, beans, true);

    // start the server
    _testServer.start();
    
    // initialize other members
  }
  
  @Test
  public void testGet()
  {
    GetRequest<Greeting> getRequest = GREETINGS_BUILDERS.get().id(1L).build();
    Greeting expectedGreeting = new Greeting().setId(1L).setMessage("test greeting");
    // the request sent by the _restClient goes to our _testServer. 
    Greeting actualGreeting = _restClient.sendRequest(getRequest).getResponseEntity();
    Assert.assertEquals(actualGreeting, expectedGreeting);
  }
  
  // other tests
  
  @AfterTest
  public void cleanup()
  {
    _testServer.stop();
  }
}

```

## Writing Unit Tests for Rest.li Data (RecordTemplates and DataMaps)

The `restli-common-testutils` module contains `DataAssert`, a class which allows you to compare `DataMap`s and `RecordTemplate`s.

#### DataAssert

The `DataAssert` class contains methods that allow you to compare two `DataMap`s, `RecordTemplate`s or `Collection`s of `RecordTemplate`s. These methods print out an easy to read output in case of an inequality, with the error message containing only the field(s) which makes the passed in objects not equal to each other. This is better than using a regular JUnit or TestNG `Assert.assertEquals` method as it might be hard to figure which field(s) caused the inequality, since the vanilla `Assert.assertEquals` print out the `toString()` of both objects which can be hard to read if there are many fields. Internally, this class uses TestNG to run assertions.

You can also specify `ValidationOptions` that will be applied to the passed in `RecordTemplate`s before any comparison takes place. Please see the Javadoc for more details. Similarly, when comparing two `DataMap`s you can also specify that `null` is the same as an empty `DataList` or `DataMap`. Again, please see the Javadoc for more details.

Here is an example on how to use this class:

```java
// we make a request to our server for which we know the expected result.
// we then compare the actual result with the expected result.
Greeting actualGreeting = _restClient.sendRequest(request).getResponseEntity();
Greeting expectedGreeting = new Greeting().setId(1L).setMessage("Greeting 1");
// we do not want to perform fix-up or coercion, hence we pass in null as the last argument.
DataAssert.assertRecordTemplateDataEqual(actualGreeting, expectedGreeting, null);
```


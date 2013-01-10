What is Rest.li?
================
Rest.li is a REST+JSON framework for building robust, scalable service
architectures using dynamic discovery and simple asynchronous APIs.

Rest.li fills a niche for building RESTful service architectures at scale,
offering a developer workflow for defining data and REST APIs that promotes
uniform interfaces, consistent data modeling, type-safety, and compatibility
checked API evolution.

# No, Really.  What is Rest.li?

Oh, you want to see some code, don't you?

Basically, rest.li is a framework where you define schema's for your data:

```json
{
  "name" : "Greeting", "namespace" : "com.example.greetings", "type" : "record",
  "fields" : [
    { "name" : "message", "type" : "string" }
  ]
}
```

Write servers:

```java
@RestLiCollection(name = "greetings")
class GreetingsResource extends CollectionResourceTemplate<Long, Message> {
  public Greeting get(Long key) {
    return new Greeting().setMessage("Good morning!");
  }
}
```

And then write clients:

```java
Response<Greeting> response = restClient.sendRequest(new GreetingsBuilders.get().id(1L).build()).get();
System.out.println(response.getEntity().getMessage());
```

And get all the benefits of a robust, scalable REST+JSON framework.

# Full Documentation

See our [wiki](http://github.com/linkedin/rest.li/wiki) for full documentation and examples.

Quickstart Guides and Examples
------------------------------

* [Quickstart - a step-by-step tutorial on the basics](http://github.com/linkedin/rest.li/wiki/Quickstart:-A-Tutorial-Introduction-to-RestLi)
* [Guided walkthrough of an example application](http://github.com/linkedin/rest.li/wiki/Quick-Start-Guide)

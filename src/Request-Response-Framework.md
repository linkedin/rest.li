---
layout: api_reference
title: Rest.li Request Response Framework
permalink: /Request-Response-Framework
excerpt: Pegasus's request/response framework, often called R2,  includes abstractions for REST and RPC requests and responses, filter chains for customized processing, and transport abstraction.
---

# Rest.li Request Response Framework

Pegasus's request/response framework, often called R2,  includes abstractions for REST and RPC requests and responses, filter chains for customized processing, and transport abstraction. 

R2 can be used in conjunction with the Dynamic Discovery system (also known as D2). The combined stack can be referred to as "R2D2". R2 can be used independently as well.

### Filters
The R2 framework in Rest.li contains a filter chain layer. This allows developers to process and modify the content, the associated wire attributes, and the local attributes for each request/response.

To implement a filter, simply implement the relevant Filter interface (for REST, this is `RestFilter`; for RPC, use `RpcFilter`).

To use a filter, instantiate a Rest.li server/client with a `FilterChain` that contains the filters you want to use in the order that you would like to use them in. Note that the order of processing is as follows:
* Requests are processed **starting from the beginning of the chain and move towards the end** of the filter chain.
* Responses are processed **from the end of the filter chain and move back towards the beginning** of the filter chain.

### Filter Example

Consider the example given in the Rest.li client tutorial:

```java
final HttpClientFactory http = new HttpClientFactory();
final Client r2Client = new TransportClientAdapter(
                                      http.getClient(Collections.<String, String>emptyMap()));
```

Suppose your filter was implemented in some class `MyClientFilter`. To add this filter, you might do something like this:

```java
FilterChain fc = FilterChains.empty().addFilter(new MyClientFilter());
final HttpClientFactory http = new HttpClientFactory(fc);
final Client r2Client = new TransportClientAdapter(
                                      http.getClient(Collections.<String, String>emptyMap()));
```

So how would one go about writing a filter?
As an example, suppose we wanted to use filters to compress the responses we receive from the server. 

A client filter could do this:
* Add an HTTP Accept-Encoding header to its outbound requests.
* On inbound responses, the filter would read the Content-Encoding header and decompress the payload accordingly.

A corresponding server might do this:
* The server's compression filter can read the inbound request's Accept-Encoding header and store it as a local attribute.
* When the server response is ready to go, the filter can intercept the outbound response, compress it, and send the compressed payload off with the corresponding HTTP Content-Encoding header.

See an implementation example here: https://github.com/linkedin/rest.li/tree/master/r2/src/main/java/com/linkedin/r2/filter/compression

For a full list of filters, see: List-of-R2-filters
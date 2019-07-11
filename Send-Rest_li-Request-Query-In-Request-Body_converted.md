---
layout: guide
title: Send Rest.li request query in request Body
permalink: /Send-Rest_li-Request-Query-In-Request-Body
excerpt: Rest.li protocol specifies what HTTP method will be used for each type of Rest.li request. However, sometimes due to security constraint or jetty buffer limitation, it may be required to customize the HTTP method used to send a particular Rest.li request to a Rest.li server.
---

# Send Rest.li request query in request Body

## Contents

  - [Introduction](#introduction)
  - [ClientQueryTunnelFilter and
    ServerQueryTunnelFilter](#clientquerytunnelfilter-and-serverquerytunnelfilter)
  - [Request Without Body](#request-without-body)
  - [Request With Body](#request-with-body)

## Introduction

The [Rest.li protocol](spec/protocol) specifies what HTTP method will be used for each type of Rest.li request. However, sometimes due to security constraint  (i.e. not wanting to send some sensitive information in URI) or jetty buffer limitation (i.e. there may pose a threshold on the longest query that can go through), it may be required to customize the HTTP method used to send a particular Rest.li request to a Rest.li server.

## ClientQueryTunnelFilter and ServerQueryTunnelFilter

Rest.li has provided
[ClientQueryTunnelFilter](https://github.com/linkedin/rest.li/blob/master/r2-core/src/main/java/com/linkedin/r2/filter/transport/ClientQueryTunnelFilter.java)
(R2 client filter) and
[ServerQueryTunnelFilter](https://github.com/linkedin/rest.li/blob/master/r2-core/src/main/java/com/linkedin/r2/filter/transport/ServerQueryTunnelFilter.java)
(R2 server filter) to support such HTTP method customization. These two
filters behaves as follows:

  - On sending a rest request from client, ClientQueryTunnelFilter will
    invoke
    [QueryTunnelUtil](https://github.com/linkedin/rest.li/blob/master/r2-core/src/main/java/com/linkedin/r2/message/QueryTunnelUtil.java)
    encoding function to encode a standard non-POST rest.li request by
    moving the query param line into the body, and reformulating the
    request as a POST. The original method is specified by the
    `X-HTTP-Method-Override` header. This header is important to
    indicate that on the server side we should invoke QueryTunnelUtil
    decoding function to get back original request so that this
    conversion looks completely transparent to users. User can indicate
    whether they want to perform such encoding in two ways:

<!-- end list -->

1.  Either by specifying a queryPostThreshold in initializing
    ClientQueryTunnelFilter. That means if the rest request raw query
    length is greater than this specified queryPostTreshold,
    ClientQueryTunnelFilter will automatically convert this request to
    POST.
2.  Or by forcing request conversion to specify in request context
        ```
           requestContext.putLocalAttr(R2Constants.FORCE_QUERY_TUNNEL, true);
        ```

<!-- end list -->

  - On receiving a rest request at server, ServerQueryTunnelFilter will
    invoke QueryTunnelUtil.decode to convert request back to its
    original form based on a special header set by
    ClientQueryTunnelFilter, that is, `X-HTTP-Method-Override`.

## Request Without Body

For a rest.li request without body, for example, a BATCH_GET request
like this `http://localhost?ids=List(1,2,3)`, the transformed POST
request is x-www-form-urlencoded with query params stored in the body,
as follows:

```bash
curl -X POST http://localhost \
  -H "X-HTTP-Method-Override: GET" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data $'ids=1,2,3' 
```

  
Basically,

  - change GET to POST (`-X POST`)
  - add X-HTTP-Method-Override header for original HTTP method (`-H
    "X-HTTP-Method-Override: GET"`) 
  - add Content-Type header (`-H "Content-Type:
    application/x-www-form-urlencoded"`)
  - move all the query string to body (@—data $’ids=List(1,2,3)’)  
    Note that if QueryTunnelUtil need to do actual encoding or decoding,
    the request would be fully buffered first,  
    even if it’s streaming.  
    We believe QueryTunnelUtil is almost exclusively for GET requests,
    practically no use case would require  
    excessively long query for POST & PUT, and we’d be surprised if
    anyone is using QueryTunnelUtil for PUT & GET.  
    Hence, fully buffering request that has to be encoded/decoded is
    practically not a problem and gives up the best  
    return for the investment of our efforts. 

## Request With Body

For a rest.li request with body, for example, a BATCH_UPDATE request
like this `http://localhost?ids=List(1,2,3)`, the transformed POST
request will be of Content-Type of multipart/mixed with 2 sections:

1.  The first section should be of type x-www-form-urlencoded and
    contain the query params 
2.  The second should contain what would have been the original body,
    along with it’s associated content-type. 

It will look as follows:

```bash
curl -X POST http://localhost \
  -H "X-HTTP-Method-Override: PUT" \
  -H "Content-Type: multipart/mixed; boundary=xyz" \
  --data $'--xyz\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\nids=List(1,2,3)\r\n--xyz\r\n 
         Content-Type: application/json\r\n\r\n{"foo":"bar"}\r\n--xyz--' 
```

  
Here,

  - change GET to POST (`-X POST`)
  - add X-HTTP-Method-Override header for original HTTP method (`-H
    "X-HTTP-Method-Override: GET"`) 
  - add Content-Type header (`-H "Content-Type: multipart/mixed;
    boundary=xyz"`). Note that here we need to specify a boundary
    delimiter (here we use `xyz` for illustration) for multipart body,
    this delimiter needs to be unique and not appearing in your request
    content body or url.
  - move all the query string and original request body to body as two
    sections explained above.

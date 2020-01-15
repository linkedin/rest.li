---
layout: api_reference
title: Rest.li Protocol
permalink: /spec/protocol
index: 2
excerpt: Rest.li Protocol
---

# Rest.li Protocol

## Contents

  - [URI Syntax](#uri-syntax)
  - [Online Documentation](#online-documentation)
  - [Content Types](#content-types)
  - [Rest.li Protocol 2.0 Object and List/Array Representation](#restli-protocol-20-object-and-listarray-representation)
  - [Complex types, complex keys, and compound keys in the Rest.li 2.0 Protocol](#complex-types-complex-keys-and-compound-keys-in-the-restli-20-protocol)
  - [Collection Resources](#collection-resources)
  - [Simple Resources](#simple-resources)
  - [Association Resources](#association-resources)
  - [Finders](#finders)
  - [BatchFinders](#batch-finders)
  - [Actions](#actions)
  - [URI Modifiers](#uri-modifiers)
  - [Response Status Codes](#response-status-codes)
  - [Message Headers](#message-headers)
  - [Request Message Body](#request-message-body)
  - [Response Message Body](#response-message-body)
  - [Complex Types](#complex-types)
  - [Empty List Parameters](#empty-string-parameters)

**Note** - Any time there is a difference between Rest.li protocol 1.0
and Rest.li protocol 2.0 it will be explicitly mentioned. If nothing is
said, that means that there is no difference.

## URI Syntax

URIs are described using URI templates as defined in the [IETF Draft
Spec](http://tools.ietf.org/html/draft-gregorio-uritemplate-07).

## Online Documentation

Rest.li provides [online
documentation](/rest.li/user_guide/restli_server#online-documentation)
for any loaded resource. The documentation shows example request and
response for the resource methods, finders, batchFinders and actions. Use it to
document the Rest.li protocols.

## Content Types

The content types of Rest.li data are `application/json` and
`application/pson`. PSON is a compressed version of JSON.

## Rest.li Protocol 2.0 Object and List/Array Representation

### Definitions

An object, as used in this document, is a map or a dictionary. It is
simply a collection of key-value pairs, where the keys are strings and
the values are primitives, arrays, or an object. The terms object and
map are used interchangeably throughout this document to refer to the
same concept.

Consider the following functions:

<code>encoded(v)</code> is defined as follows -

  - if `v` is a primitive then `encoded(v)` = the URL encoded value for
    `v`
  - if `v` is a map then `encoded(v)` = the URL encoding for a map as
    described in the [Rest.li 2.0 protocol object URL
    representation](#url-representation)
  - if `v` is an array then `encoded(v)` = the URL encoding for an array
    as described in the [Rest.li 2.0 protocol array URL
    representation](#url-representation-1)

`reducedEncoded(v)` is defined as follows -

  - if `v` is a primitive then `reducedEncoded(v)` = `v` with the
    characters `,`, `(`, `)`, `'`, and `:` URL encoded
  - if `v` is a map then `reducedEncoded(v)` = the HTTP body/header
    encoding for a map as described in the [Rest.li 2.0 protocol object
    HTTP body and headers
    representation](#http-body-and-headers-representation)
  - if `v` is an array then `reducedEncoded(v)` = the HTTP body/header
    encoding for an array as described in the [Rest.li 2.0 protocol
    array HTTP body and headers
    representation](#http-body-and-headers-representation-1)

`encoded` and `reducedEncoded` will be used in the sections below.

### JSON Serialization of Data Schema

Rest.li objects defined using using pegasus data schemas are serialized
in a JSON representation for transportation over the wire. For detailed
transport serialization, please see
[How Data is Serialized for Transport](/rest.li/how_data_is_serialized_for_transport).

### Rest.li Protocol 2.0 Object Representation

#### URL Representation

In Rest.li 2.0, the way to represent an object in the URL is using
<code>key:value</code> pairs. More concretely -

    (encoded(k1):encoded(v1),encoded(k2):encoded(v2),...)

Note that all keys are strings.

#### HTTP body and headers representation

##### Header representation

An object can be present as values for the following headers -

  - <code>Location</code> - if present here we simply use the
    [Rest.li 2.0 protocol object URL
    representation](#url-representation)
  - <code>X-RestLi-Id</code> or <code>X-LinkedIn-Id</code> - we use the
    [Rest.li 2.0 protocol object HTTP body and headers
    representation](#http-body-and-headers-representation)

##### Body representation

If present in the HTTP body a `key:value` representation is used. More
concretely -

    (reducedEncoded(k1):reducedEncoded(v1),reducedEncoded(k2):reducedEncoded(v2),...)

### Rest.li 2.0 Protocol Array Notation

#### URL Representation

An array <code>\[a1, a2, a3, …\]</code> is encoded in the URL as -

    List(encoded(a1),encoded(a2),encoded(a3),...)

#### HTTP body and headers representation

##### Header representation

An array can be present as values for the following headers -

  - <code>Location</code> - if present here we simply use the
    [Rest.li 2.0 protocol array URL
    representation](#url-representation-1)
  - <code>X-RestLi-Id</code> or <code>X-LinkedIn-Id</code> - we use the
    [Rest.li 2.0 protocol array HTTP body and headers
    representation](#http-body-and-headers-representation-1)

##### Body representation

An array `[a1, a2, a3, ...]` is encoded as follows -

    List(reducedEncoded(a1),reducedEncoded(a2),reducedEncoded(a3),...)

### Example

Consider the object, which we will call `exampleObject`, expressed in a
JSON notation here -

    {
      "k1": "v1",
      "k2": "value with spaces",
      "k3": [1, 2, 3],
      "k4": "value:with:reserved:char"
      "k5":
      {
        "k51": "v51",
        "k52": "v52"
      }
    }


Here is how `exampleObject` would look if present in the URL -

    (k1:v1,k2:value%20with%20spaces,k3:List(1,2,3),k4:value%3Awith%3Areserved%3Achar,k5:(k51:v51,k52:v52))

Here is how `exampleObject` would look if present in the HTTP headers -

    Location: ...(k1:v1,k2:value%20with%20spaces,k3:List(1,2,3),k4:value%3Awith%3Areserved%3Achar,k5:(k51:v51,k52:v52))
    X-RestLi-Id: (k1:v1,k2:value with spaces,k3:List(1,2,3),k4:value%3Awith%3Areserved%3Achar,k5:(k51:v51,k52:v52))

If we were doing a BATCH\_GET request and `exampleObject` was one of the
keys requested here is how the HTTP body would look -

    ...
    "entities": {
      "(k1:v1,k2:value with spaces,k3:List(1,2,3),k4:value%3Awith%3Areserved%3Achar,k5:(k51:v51,k52:v52))": {
        ...
      }
      ...
    }
    ...

## Complex types, complex keys, and compound keys in the Rest.li 2.0 Protocol

A complex type is simply a map.

A complex key is a key made up of two parts, `key` and `$params`, each
of which is a complex type.

A compound key is a complex type with the restriction that all the
values are primitives. A compound key cannot have maps or arrays as
values for the keys making up its map.

Complex and compound keys have a similar structure: they are essentially
a collection of key-value pairs. Because of this similarity in structure
we decided to represent both using the Rest.li 2.0 object notation. Any
list present as a value in the complex key uses the Rest.li 2.0 list
notation. Details can be found in the [Association
keys](#association-keys)
and [complex
keys](#complex-types-as-keys-in-protocol-20)

## Collection Resources

The URI templates below assume variables with types as follows:

    collection : simple string or "complex key"
    entity_id : simple string
    ids : list
    finder : simple string
    batch_finder : simple string
    params : associative array

### Collection URIs

| Resource | URI Template |Example |Method |Semantics|
|Collection|/{collection}|/statuses|POST |CREATE - creates an entity in the collection
|Collection|/{collection}/{entity_id}|/statuses/1|GET|READ - returns the referenced entity
|Collection|/{collection}/{entity_id}|/statuses/1|PUT|UPDATE - updates the referenced entity
|Collection|/{collection}/{entity_id}|/statuses/1|POST|PARTIAL UPDATE - partially updates the referenced entity
|Collection|/{collection}/{entity_id}|/statuses/1|DELETE|DELETE - deletes the referenced entity
|Collection|/{collection}?{ids}|Protocol 1.0 - /statuses?ids=1&amp;ids=2&amp;ids=3<br />Protocol 2.0 - /statuses?ids=List(1,2,3)|GET|BATCH_GET - returns a map containing the referenced entities
|Collection|/{collection}|/statuses|GET|GET_ALL - returns all entities in the collection
|Collection|/{collection}?q={finder}|/statuses?q=search|GET|FINDER - returns a list containing entities satisfying the query
|Collection|/{collection}?q={finder}{&amp;params*}|/statuses?q=search&amp;keywords=linkedin|GET|FINDER - returns a list containing entities satisfying the query
|Collection|/{collection}?bq={batch_finder}|/statuses?bq=search|GET|BATCH_FINDER - returns a list containing entities satisfying the query
|Collection|/{collection}?bq={batch_finder}{&amp;params*}|/statuses?bq=search&amp;criteria=List((id:1, title:bar),(id:2, title:foo))|GET|BATCH_FINDER - returns a list containing entities satisfying the query
|Collection|/{collection}?action={action}|/statuses?action=purge|POST|ACTION - some operation, rest.li does not specify any standard behavior


## Simple Resources

The URI templates below assume variables with types as follows:

    simple : simple string
    params : associative array


### Simple URIs

| Resource | URI Template              | Example                          | Method | Semantics                                                               |
| -------- | ------------------------- | -------------------------------- | ------ | ----------------------------------------------------------------------- |
| Simple   | /{simple}                 | /selectedItem                    | GET    | READ - returns the entity                                               |
| Simple   | /{simple}                 | /selectedItem                    | PUT    | UPDATE - updates the entity                                             |
| Simple   | /{simple}                 | /selectedItem                    | DELETE | DELETE - deletes the entity                                             |
| Simple   | /{simple}?action={action} | /selectedItem?action=investigate | POST   | ACTION - some operation, rest.li does not specify any standard behavior |

## Association Resources

Associations contain entities referenced by compound keys, referred to
here as assockeys

The URI templates below assume variables with types as follows:

    firstkey : associative array containing a single element
    keys : associative array
    association : simple string
    assockey : simple string conforming to the assockey syntax described below
    assockeys : list of strings conforming to the assockey syntax
    finder : simple string
    batch_finder : simple string
    params : associative array

### Association Keys

Association keys are composed of one or more named assocKey parts.

In protocol 1.0 the key is represented over the wire in the form:

    {firstkey}{&keys*}

In protocol 2.0 the key is represented using the protocol 2.0 object
notation, with each assocKey being a key in the map.

For example, a two part key identifying an edge in a following graph in
protocol 1.0 might be:

    followerID=1&followeeID=3

In protocol 2.0 the key would be

    (followerID:1,followeeID:3)

Here’s an example association GET request/response. In protocol 1.0:

    GET /associations/src=KEY1&desk=KEY2
    {
        "message": "Hi!",
        "id": "1"
    }

In protocol 2.0:

    GET /associations/(src:KEY1,desk:KEY2)
    {
        "message": "Hi!",
        "id": "1"
    }

In finders, only some keys from the full association key might be
required. For example, in protocol 1.0:

    followerID=1

In protocol 2.0:

    (followerID:1)

All string values for association keys are url encoded. E.g. for the
association key composed of code=“1=2b” and widget=“xyz widget”, a GET
request in protocol 1.0 using the key would be:

    GET /resourceName/code=1%32b&widget=xyz%20widget

In protocol 2.0 it would be:

    GET /resourceName/(code:1%32b,widget:xyz%20widget)

When association keys are used in a batch operation, each key is url
encoded. For protocol 1.0 the form is:

    ids=urlencoded(associationKey1)&ids=urlencoded(associationKey2)...

For protocol 2.0 is ids use the protocol 2.0 array notation.

    ids=List((encoded(associationKey1)),(encoded(associationKey2)),...)

For example, in protocol 1.0 a batch get for the keys:
<code>src=KEY1\&dest=KEY3</code> and <code>src=KEY1\&dest=KEY2</code>,
would
    be:

    GET /associations?ids=src%3DKEY1%26dest%3DKEY2&ids=src%3DKEY1%26dest%3DKEY3
    
    {
      "errors": {},
      "results": {
          "dest=KEY3&src=KEY1": {
              "message": "Hi!",
              "id": "1"
          },
          "dest=KEY2&src=KEY1": {
              "message": "Hello!",
              "id": "2"
          }
      }
    }

In protocol 2.0 a batch get for the keys:
<code>(src:KEY1,dest:KEY3)</code> and <code>(src:KEY1,dest:KEY2)</code>,
would
    be:

    GET /associations?ids=List((src:KEY1,dest:KEY3),(src:KEY1,dest:KEY2))
    
    {
      "errors": {},
      "results": {
          "(dest:KEY3,src:KEY1)": {
              "message": "Hi!",
              "id": "1"
          },
          "(dest:KEY2,src:KEY1)": {
              "message": "Hello!",
              "id": "2"
          }
      }
    }

Here’s the basic form of a batch update request using association keys.
In protocol 1.0:

    PUT /resourceName?ids=urlencoded(key1=urlencoded(value)&key2=urlencoded(value)&...)&ids=...
    
    {
      "entities": {
        "key=urlencoded(value)&key2...": { ... },
        ...
      }
    }

Note that in the URL the ids are url encoded AND any strings values for
the assocKey parts are double url encoded.

In protocol 2.0 the protocol 2.0 array representation is used for the
ids.

For example, for a batch update for the association keys: (code=“1=2b”,
name=“xyz widget”) and (code=“567”, name=“rachet”)

The batch update request in protocol 1.0 would be:

    PUT /widgets?ids=code%3D1%2532b%26widget%3Dxyz%2520widget&ids=code%3D567%26widget%3Drachet
    
    {
      "entities": {
         "code=1%32b&name=xyz%20widget": {...},
         "code=567&name=rachet": {...}
      }
    }

In protocol 2.0 the request would be:

    PUT /widgets?ids=List((code:1%202b,name:xyz%20widget),(code:567,name:rachet))
    
    {
      "entities": {
         "(code:1=2b,name:xyz widget)": {...},
         "(code:567,name:rachet)": {...}
      }
    }

### Association URIs


|Resource|URI Template|Example|Method|Semantics|
|Association|/{association}/{+assockey}|Protocol 1.0 - /follows/followerID=1&amp;followeeID=1<br /> Protocol 2.0 - /follows/(followerID:1,followeeID:1)|GET|READ - returns the referenced association entity
|Association|/{association}/{+assockey}|Protocol 1.0 - /follows/followerID=1&amp;followeeID=1<br />Protocol 2.0 - /follows/(followerID:1,followeeID:1)|PUT|UPDATE - updates the referenced association entity
|Association|/{association}/{+assockey}|Protocol 1.0 - /follows/followerID=1&amp;followeeID=1<br />Protocol 2.0 - /follows/(followerID:1,followeeID:1)|DELETE|DELETE - deletes the referenced association entity
|Association|/{association}/{+assockeys*}|Protocol 1.0 - /follows/?ids=followerID%3D1%26followeeID%3D1&amp; ids=followerID%3D1%26followeeID%3D3&amp;ids=followerID%3D1%26followeeID%3D2 \\<br />Note: followerID%3D1%26followeeID%3D1 unescapes to followerID=1&amp;followeeID=1<br />Protocol 2.0 - /follows/?ids=List((followerID:1,followeeID:1),(followerID:1,followeeID:2))|GET|BATCH_GET - returns a map containing the referenced association entities
|Association|/{association}|/follows|GET|GET_ALL - returns all the association entities
|Association|/{association}?q={finder}|/follows?q=search|GET|FINDER - returns a list containing entities satisfying the query
|Association|/{association}?q={finder}{&amp;params*}|/follows?q=followers&amp;userID=1|GET|FINDER - returns a list containing entities satisfying the query
|Association|/{association}/{+assockey}|Protocol 1.0 - /follows/followerID=1?q=other<br />Protocol 2.0 - /follows/(followerID:1)?q=other|GET|FINDER - returns a list containing the entities satisfying the query
|Association|/{association}/{+assockey}?q={finder}{&amp;params*}|Protocol 1.0 - /follows/followerID=1?q=other&amp;someParam=value<br />Protocol 2.0 - /follows/(followerID:1)?q=other&amp;someParam=value|GET|FINDER - returns a list containing the entities satisfying the query
|Association|/{association}?bq={batch_finder}|/statuses?bq=search|GET|BATCH_FINDER - returns a list containing entities satisfying the query
|Association|/{association}?bq={batch_finder}{&amp;params*}|/statuses?bq=search&amp;criteria=List((id:1, title:bar),(id:2, title:foo))|GET|BATCH_FINDER - returns a list containing entities satisfying the query
|Association|/{association}/{+assockey}|Protocol 1.0 - /follows/followerID=1?bq=other<br />Protocol 2.0 - /follows/(followerID:1)?bq=other|GET|BATCH_FINDER - returns a list containing the entities satisfying the query
|Association|/{association}/{+assockey}?bq={batch_finder}{&amp;params*}|Protocol 2.0 - /follows/(followerID:1)?q=other&amp;someParam=List((id:1, title:bar),(id:2, title:foo))|GET|BATCH_FINDER - returns a list containing the entities satisfying the query
|Association|/{association}?action={action}|/follows?action=purge|POST|ACTION - some operation, Rest.li does not specify any standard behavior

## Finders

The URI templates below assume variables with types as follows:

    finder : simple string identifying a finder

### Finder URIs

| Resource                           | URI Template          | Example                   | Method | Semantics                    |
| ---------------------------------- | --------------------- | ------------------------- | ------ | ---------------------------- |
| Collection, Association, ActionSet | {resource}?q={finder} | /accounts?q=keywordSearch | GET    | invokes the specified finder |

## Batch Finders
### Batch Finder URIs
The URI templates below assume variables with types as follows:

    batch_finder : simple string identifying a batch_finder method name
    resource : simple string identifying a resource 
    search_criteria : simple string identifying the criteria filter name

| Resource                           | URI Template          | Example                   | Method | Semantics                    |
| ---------------------------------- | --------------------- | ------------------------- | ------ | ---------------------------- |
| Collection, Association            | {resource}?bq={batch_finder}&{search_criteria}=| /PhotoResource?bq=searchPhotos&photoCriteria=List((id:1, format:JPG),(id:2, format:BMP)) | GET    | invokes the specified batch_finder |

At least, 2 query parameters will have to be set for a batch finder:

- The "bq" query parameter is reserved for passing the batch finder method name
- A second query parameter will be used to pass a set of different search criteria. The name of this query parameter is set in the [BatchFinder method annotation](/rest.li/batch_finder_resource_method#method-annotation-and-parameters).
For example, with @BatchFinder(value="findUsers", batchParam="batchCriteria"), the batch query parameter name is "batchCriteria". 
The type of this query parameter is a List.

Different data type has different representation in Rest.li protocol 1.0 and 2.0. See more details in [Rest.li Protocol](/rest.li/spec/protocol).

Eg.
In Rest.li protocol 1.0

    curl "http://localhost:8080/userSearchResults?bq=findUsers&batchCriteria[0].firstName=pauline&batchCriteria[0].age=12&batchCriteria[1].lastName=iglou" --globoff

In Rest.li protocol 2.0

    curl --header "X-RestLi-Protocol-Version: 2.0.0" "http://localhost:10546/userSearchResults?q=findUsers&batchCriteria=List((firstName:pauline, age:12),(lastName:iglou))"

The other query parameters will be applied as common filters across all batch requests.

Here is an example batch request with two individual finders using the following criteria:

- filter by first name and age
- filter by last name and age

Eg.

    curl "http://localhost:8080/userSearchResults?bq=findUsers&batchCriteria=List((firstName:pauline),(lastName:iglou))&age=21" -X GET --header "X-RestLi-Protocol-Version: 2.0.0"

### Pagination support
#### 1) Common pagination for all search criteria  
The developer can pass additional parameters to specify a common pagination. It will be more efficient than adding a pagination context inside each criteria object.  
Eg.

    curl "http://localhost:8080/userSearchResults?q=findUsers&batchCriteria=List((firstName:pauline, age:12),(lastName:iglou))&firstName=max&start=10&count=10" -X GET  --header "X-RestLi-Protocol-Version: 2.0.0"

The "start" and "count" params will be automatically mapped to a `PagingContext` object that will be passed to the resource method. 
```java
public BatchFinderResult<SearchCriteria, User, EmptyRecord> findUsers(@PagingContextParam PagingContext context, 
                                                                      @QueryParam("batchCriteria") SearchCriteria[] criteria, 
                                                                      @QueryParam("firstName") String firstName)
```

#### 2) Custom pagination per criteria object 
If the developer wants to apply a custom pagination for each search criteria, the pagination information can be passed into the the search criteria object itself.  
**Caution:** Rest.li doesn't validate how the developer models the pagination in the Search criteria RecordTemple. For consistency purpose, we recommend to use a `PagingContext`.  
It's the developer responsibility to apply the right pagination (common or custom) based on its need in the resource method implementation.

## Actions

The URI templates below assume variables with types as follows:

    action : simple string

### Action URIs

| Resource                                   | URI Template               | Example                   | Method | Semantics                    |
| ------------------------------------------ | -------------------------- | ------------------------- | ------ | ---------------------------- |
| Collection, Simple, Association, ActionSet | {resource}?action={action} | /accounts?action=register | POST   | invokes the specified action |

## URI Modifiers

The URI templates below assume variables with types as follows:

    finder_uri : simple string ...
    batch_finder_uri : simple string
    base_uri : simple string generated via one of the uri templates above
    start : simple string
    count : simple string
    fields : list

### URIs

| Feature       | Base URI Type         | URI Template                  | Example                                                                                                               |
| ------------- | --------------------- | ----------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Paging        | Finder, BatchFinder   | {+finder\_uri}{\&start,count} </br> {+batch_finder\_uri}{\&start,count}| /statuses?q=search\&start=0\&count=10 </br> /statuses?bq=search\&start=0\&count=10                                                                                |
| Projection    | Get, BatchGet, Finder, BatchFinder | {+base\_uri}{\&fields}        | Protocol 1 - /groups?q=emailDomain\&fields=locale,state Protocol 2 - /groups?q=emailDomain\&fields=List(locale,state) |
| Schema Return | Any                   | {+base\_uri}\&metaDesc        |                                                                                                                       |
| Links         | Any                   | {+base\_uri}\&metaLinks       |                                                                                                                       |

## Response Status Codes

Status codes should be interpreted according to the [HTTP
specification](http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html)

Common status codes used in Rest.li:

  - 200 OK
  - 201 Created
  - 204 No Content
  - 400 Bad Request
  - 404 Not Found
  - 405 Method Not Allowed
  - 500 Internal Server
Error

## Message Headers

| Message Type         | Header                    | Semantics                                                                                                                                                                                               | Notes                                                                                                                                                                                   |
| -------------------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Response             | X-LinkedIn-Error-Response | indicates whether the message body contains a JSON-serialized ErrorResponse object                                                                                                                      | The header value is set to “true” when an error response is returned. The header is omitted otherwise. **Only used in protocol 1.0**                                                    |
| Response             | X-RestLi-Error-Response   | indicates whether the message body contains a JSON-serialized ErrorResponse object                                                                                                                      | The header value is set to “true” when an error response is returned. The header is omitted otherwise. **Only used in protocol 2.0**                                                    |
| Response             | X-LinkedIn-Id             | indicates the id assigned by the server to a new entity created in a collection.                                                                                                                        | set on response messages resulting from a successful POST request to create an entity. The header value is set to the entity id, represented as a string. **Only used in protocol 1.0** |
| Response             | X-RestLi-Id               | indicates the id assigned by the server to a new entity created in a collection.                                                                                                                        | set on response messages resulting from a successful POST request to create an entity. The header value is set to the entity id, represented as a string. **Only used in protocol 2.0** |
| Response             | Location                  | indicates the URI of a new entity created in a collection.                                                                                                                                              | Location is set on response messages resulting from a successful POST request to create an entity. The header value is set to a URI referencing the newly created entity                |
| Response             | Content-Type              |                                                                                                                                                                                                         | The Content-Type is always set to “application/json”                                                                                                                                    |
| Request              | X-RestLi-Method           | Set whenever content is POSTed. Can be “get\_all”, “get”, “batch\_get”, “create”, “batch\_create”, “update”, “partial\_update”, “delete”, “batch\_delete”, “action”, “finder”, "batch\_finder", “batch\_partial\_update” | Is only required for “batch\_create”, “batch\_partial\_update”, all other method types can be inferred by a RestLi server from the URI string and HTTP Method.                          |
| Request and Response | X-RestLi-Protocol-Version | Version of the Rest.li protocol used to generate the request or response. Example value: “2.0.0”                                                                                                        | The version that we get back in the response is dictated by the version sent in the request. They will always be the same.                                                              |

## Request Message Body

### Single Entity

Single entities are sent as the JSON serialized DataMap representing
that entity. The `"Content-Type: application/json"` header may be
included for PUT and POST requests, if omitted, the server will assume
the request is content type is `"application/json"`.

Create:

    POST /widgets
    Content-Type: application/json
    
    {"widgetName":"Lever"}

Read:

for collection and association resources:


    GET /widgets/1

for simple resources:

    GET /currentWidget


Update:

for collection and association resources:

    PUT /widgets/1
    Content-Type: application/json
    
    {"widgetName":"Lever"}

for simple resources:

    PUT /currentWidget
    Content-Type: application/json
    
    {"widgetName":"Lever"}


Delete:

for collection and association resources:

    DELETE /widgets/1

for simple resources:

    DELETE /currentWidget

### Batch Create

A DataMap with field

  - “elements”: A JSON array of the resources to batch create/update and
    the objects are the json serialized values of each resource to
    create.

E.g.

    POST /widgets HTTP/1.1
    Content-Type: application/json
    X-RestLi-Method: batch_create
    
    {
      "elements": [
        {"widgetName":"Ratchet"},
        {"widgetName":"Cog"},
        {"widgetName":"!@&%@$#"}
      ]
    }

Response:

    {
      "elements": [
        {
          "status": 201,
          "id": "100"
        },
        {
          "status": 201,
          "id": "101"
        },
        {
          "status": 406,
          "error": {
            "status": 406,
            "stackTrace": "...",
            "errorDetails": { ... },
            "serviceErrorCode": 999, 
            "exceptionClass": "...", 
            "message": "..."
          }
      ]
    }

Responses are associated to the request by array index. E.g. “Rachet”
was assigned id 100, “Cog” was assigned id 101 and “\!`&%`$\#” resulted
on a 406 response code and was not assigned an id.

Note: Batch create requests must include the HTTP Header:

```X-RestLi-Method: batch_create```

### Batch Update

A DataMap with field

  - “entities”: A JSON serialized map where the keys are keys of the
    resources to update and the objects are the json serialized
    replacement values of each resource to update.

E.g.

    PUT /widgets?ids=1&ids=2 HTTP/1.1
    Content-Type: application/json
    X-RestLi-Method: batch_update
    
    {
      "entities": {
        "1": {"widgetName":"Trebuchet"},
        "2": {"widgetName":"Gear"}
       }
    }

Response:


    {
      "errors": {},
      "results": {
        "1": {
          "status": 204
        },
        "2": {
          "status": 204
        }
      ]
    }
    

### Partial Update

Partial update is a set of operations on data object, which is also an
instance of DataMap. Operations are expressed using fields with reserved
word names. Every operation relates to the object that contains it,
i.e., it’s parent. The following is an example of setting the `zipCode`
of the `businessAddress`, setting `name` and `homeAddress`, and deleting
`note` and `birthday` of a record.

E.g.


    POST /widgets/1 HTTP/1.1
    Content-Type: application/json
    
    {
      "patch": {
        "businessAddress": {
          "$set": {
            "zipCode": "94086"
          }
        },
        "$set": {
          "name": "John",
          "homeAddress": {
            "street": "10th",
            "city": "Sunnyvale"
          }
        },
        "$delete": ["note", "birthday"]
      }
    }
    

#### Patch

A patch object describes the operations needed to change one map to
another, which are the underlying data structure for record, union and
map. A patch object is represented by a map itself. It has three types
keys.

  - “$set” indicates a set operation. Its value is a map, indicating the
    keys and values to be set. This is usually used to set fields in a
    record.
  - “$delete” indicates a delete operation. Its value is a string array,
    indicating the map entries to be deleted. This is usually used to
    delete fields in a record.
  - Other key indicates a patch operation. It must be an existing key,
    whose value must be a map, of the map to be changed. Its value is
    another patch object. This is usually used to update a field in a
    nested record.

### Batch Partial Update

See Partial Update and Batch Update above for details, here the two are
combined.

E.g.

    POST /widgets?ids=1&ids=2 HTTP/1.1
    Content-Type: application/json
    X-RestLi-Method: batch_partial_update
    
    {
      "entities": {
        "1": {"patch": { "$set": { "name":"Sam"}}},
        "2": {"patch": { "$delete": ["name"]}}
       }
    }
    

<a id="ActionRequest"></a>

### Action

Action params are provided in the request body which must contain a data
map keyed by param names. E.g.


    POST /widgets?action=purge HTTP/1.1
    Content-Type: application/json
    {
      "reason": "spam",
      "purgedByAdminId": 1
    }
    

The `X-RestLi-Method: action` may optionally be included, but is not
required because rest.li is able to determine the request is an action
based on the “action” query param key.

## Response Message Body

### Single Entity

Single entities are returned as the JSON serialized DataMap representing
that entity

### List of Entities

Lists of entities are returned in a com.linkedin.rest.CollectionResponse
wrapper. They are returned by finders and getAll.

CollectionResponse fields:

  - “elements” : JSON serialized list of entity types
  - (optional) “paging” : JSON serialized CollectionMetadata object

E.g.


    {
      "elements: [
        { "id": 1, "message": "Good morning!", "tone": "FRIENDLY" }
        // ...
      ],
      "metadata" { // only returned by finders that define a metadata schema as part of the interface
        // ...
      },
      "paging": {
        "count": 10,
        "links": [
          "href": "/greetings?count=10&start=10&q=search",
          "rel": "next",
          "type": "application/json"
        ],
        "start": 0
      }
    }

### Map of Entities

Maps of entities are returned in a
<code>com.linkedin.restli.common.BatchResponse</code> or
<code>com.linkedin.restli.client.response.BatchKVResponse</code>
wrapper.

BatchResponse fields:

  - “results” : JSON object containing name/value pairs
      - name is the string value of each map key. This is serialized
        according to the protocol version.
      - value is the JSON serialized entity for each map value. This is
        serialized according to the protocol version.
  - “errors”: another map mapping keys which failed to generate a 2xx
    response to the reason, which is a <code>ErrorResponse</code>

Protocol 1 example -


    GET /fortunes?ids=1&ids=2&ids=unacceptableKey&ids=faultyKey
    {
      "errors": {
         "unacceptableKey": {
           "status": 416,
           "message": "Not Acceptable"
         },
         "faultyKey": {
           "status": 500,
           "exceptionClass": "org.example.SomeServerException",
           "stacktrace": "SomeServerException at org.example.SomeClass.someMethod(SomeClass.java:1)\n..."
         }
       },
      "results": {
        "1": {...},
        "2": {...}
        }
      }
    }
    

Protocol 2 example -


    GET /fortunes?ids=List(1,2,unacceptableKey,faultyKey)
    {
      "errors": {
         "unacceptableKey": {
           "status": 416,
           "message": "Not Acceptable"
         },
         "faultyKey": {
           "status": 500,
           "exceptionClass": "org.example.SomeServerException",
           "stacktrace": "SomeServerException at org.example.SomeClass.someMethod(SomeClass.java:1)\n..."
         }
       },
      "results": {
        "1": {...},
        "2": {...}
        }
      }
    }
    

### Collection Metadata


    {"type":"record",
     "name":"CollectionMetadata",
     "namespace":"com.linkedin.common.rest",
     "doc":"Metadata and pagination links for this collection",
     "fields":[
     {
       "name":"start",
       "type":"int",
       "doc":"The start index of this collection"
     },{
       "name":"count",
       "type":"int",
       "doc":"The number of elements in this collection segment"
     },{
       "name":"total",
       "type":"int",
       "doc":"The total number of elements in the entire collection (not just this segment)",
       "default":0
     },{
       "name":"links",
       "type":{
         "type":"array",
         "items":{
           "type":"record",
           "name":"Link",
           "doc":"A atom:link-inspired link",
           "fields":[
           {
             "name":"rel",
             "type":"string",
             "doc":"The link relation e.g. 'self' or 'next'"
           },{
             "name":"href",
             "type":"string",
             "doc":"The link URI"
           },{
             "name":"type",
             "type":"string",
             "doc":"The type (media type) of the resource"
           }]
         }
       },
       "doc":"Previous and next links for this collection"
    }]}
    

### Error Response


    {
      "type":"record",
      "name":"ErrorResponse",
      "namespace":"com.linkedin.common.rest",
      "doc":"A generic ErrorResponse",
      "fields":[
      {
        "name":"status",
        "type":"int",
        "doc":"The HTTP status code"
      },{
        "name":"serviceErrorCode",
        "type":"int",
        "doc":"An service-specific error code (documented in prose)"
      },{
        "name":"message",
        "type":"string",
        "doc":"A human-readable explanation of the error"
      },{
        "name":"exceptionClass",
        "type":"string",
        "doc":"The FQCN of the exception thrown by the server (included the case of a server fault)"
      },{
        "name":"stackTrace",
        "type":"string",
        "doc":"The full (??) stack trace (included the case of a server fault)"
      }]
    }
    

<a id="ActionResponse"></a>

### Action Response

Actions may optionally return a response body. If they do, it must
contain a data map with a single “value” key, where the value of the key
is either a primitive type, e.g.:


    {
      "value": 1
    }
    

or complex data type, e.g.:


    {
      "value": {
        "firstName": "John",
        "lastName": "Smith"
      }
    }
    
### Batch Collection Response
A list of `BatchFinderCriteriaResult` are returned in a `BatchCollectionResponse` wrapper. 
It is used for returning an ordered, variable-length, navigable collection of resources for BATCH_FINDER.
This means, `BatchFinderCriteriaResult` objects are expected to be returned in the same order and position as the respective input search criteria.

For each batchFinder search criteria, it will either return a successful `CollectionResponse` which contains a list of entities Or 
an `ErrorResponse` in failing case. Such 2 kinds cases are wrapped into `BatchFinderCriteriaResult` corresponding to 
each search criteria.

`BatchFinderCriteriaResult` fields:

- (optional) "elements" : JSON serialized list of entity types (in success case)
- (optional) "metadata":
- (optional) "paging" : JSON serialized CollectionMetadata object
- (optional) "error" : it's an ErrorResponse which fail to get a list of entities to corresponding search criteria(in failure)
- "isError" : which indicates whether the result is a successful case or not

E.g.
    
    GET http://localhost:7279/photos?bq=searchPhotos&criteria=List((format:JPG,title:bar),(format:PNG,title:bar))&exif=() HTTP/1.1
    HTTP/1.1 200 OK Content-Type: application/jsonX-RestLi-Protocol-Version: 2.0.0
    
    {
      "elements" : [ {
        "elements" : [ { // in success case: return a list of entities
          "urn" : "foo",
          "format" : "JPG",
          "id" : 9,
          "title" : "baz",
          "exif" : { }
        }, {
          "urn" : "foo",
          "format" : "JPG",
          "id" : 10,
          "title" : "bar",
          "exif" : { }
        } ],
        "paging" : {
          "total" : 2,
          "count" : 10,
          "start" : 0,
          "links" : [ 
          {           
            "href": "/PhotoResource?PhotoCriteria=List((urn:foo, format:JPG))&start=1&count=1&bq=searchPhotos",
            "type": "application/json",
            "rel": "next"
          ]
        }
      }, { // in failure : return an ErrorResponse
        "isError" : true,
        "elements" : [ ],
        "error" : {
          "exceptionClass" : "com.linkedin.restli.server.RestLiServiceException",
          "stackTrace" : "com.linkedin.restli.server.RestLiServiceException [HTTP Status:404]: The server didn't find a representation for this criteria\n\tat......",
          "message" : "The server didn't find a representation for this criteria",
          "status" : 404
        }
      } ]
    }

## Complex Types

### Complex types as keys in protocol 1.0

The serialized form of a complex key uses path keys to specify the
values of data elements in a complex data type. For example, given the
complex data:


    {
      "key": {
        "x": [
          "a1",
          "a2"
        ],
        "y": 123,
        "key.with.dots": "val"
      }
    }
    

Its serialized form is:


    // Complex key as a serialized string:
    key.x[0]=a1&key.x[1]=a2&key.y=123&key~2Ewith~2Edots=val
    

If this serialized form is put into a URI (as it usually is), the ‘\[’
must be escaped as ‘%5B’ and the ‘\]’ must be escaped as ‘%5D’ (URIs
require this), so you have the URI form:


    // Complex key as a serialized string, escaped for URI:
    key.x%5B0%5D=a1&key.x%5B1%5D=a2&key.y=123&key~2Ewith~2Edots=val
    

Where, in the values of the query params, the chars ‘.\[\]’ are “~
encoded” to their ascii values. This encoding is the same as “%
encoding” except that the escape char is ‘~’ and the only reserved
chars are ‘.\[\]’.


    . -> ~2E
    [ -> ~5B
    ] -> ~5D
    ~ -> ~7E
    

The `Params` of a `ComplexResourceKey` are always prefixed with
“$params.” when represented in a URI, e.g.:


    $params.x=a1
    

### Complex types as keys in protocol 2.0

The serialized form of a complex key uses the [Rest.li 2.0 protocol
object
notation](/rest.li/spec/protocol#restli-protocol-20-object-representation)
. For example, given the complex data:


    {
      "key": {
        "x": [
          "a1",
          "a2"
        ],
        "y": 123,
        "key.with.dots": "val"
      }
    }
    

Its serialized form is:


    // Complex key as a serialized string:
    (key:(x:List(a1,a2)),y:123,key.with.dots:val)
    

The `Params` of a `ComplexResourceKey` are always prefixed with
“$params.” when represented in a URI, e.g.:


    $params:(x:a1)
    

### Complex keys in batch requests in protocol 1.0

If used in batch requests, each key in the batch is represented as a
element in an array, the complex data representation is:


    [
      { <complekey1> }, { <complexkey2> }
    ]
    

And it’s serialized representation is just the list flattened using the
same rules as with any complex key, and with the same “~ encoding”
applied.

For example,


    [
      { "keypart1":"v1", "keypart2":"v2" }, { "keypart1":"v3", "keypart2":"v4" }
    ]
    

It’s serialized form is:


    // Complex key as a serialized string:
    ids[0].keypart1=v1&ids[0].keypart2=v2&ids[1].keypart1=v3&ids[1].keypart2=v4
    

If this serialized form is put into a URI (as it usually is), the ‘\[’
must be escaped as ‘%5B’ and the ‘\]’ must be escaped as ‘%5D’ (URIs
require this), so you have the URI form:


    // Complex key as a serialized string, escaped for URI:
    ids%5B0%5D.keypart1=v1&ids%5B0%5D.keypart2=v2&ids%5B1%5D.keypart1=v3&ids%5B1%5D.keypart2=v4
    

If $params are in a batch complex key key, they are also prefixed by
their key’s position in the ids array, e.g.
“ids\[0\].$params.parmkeypart1=v5”

When complex keys are used in batch requests, they are often included
both in the URI and in the json body.

For example, a batch update request has the ids in the URI as well as
the “entities” part of the body:


    PUT /widgets?ids%5B0%5D.keypart1=v1&ids%5B0%5D.keypart2=v2&ids%5B1%5D.keypart1=v3&ids%5B1%5D.keypart2=v4
    
    {
      "entities": {
        "keypart1=v1&keypart2=v2": { <content to put for v1,v2> }
        "keypart1=v3&keypart2=v4": { <content to put for v3,v4> }
      }
    }
    

Note how the paths for the keys in the URI are prefixed by an ids array
position, but the paths for the keys in the JSON body are not.

### Complex keys in batch requests in protocol 2.0

If used in batch requests, each key in the batch is represented as an
element in the <code>ids</code> array using the [protocol 2.0 array
notation](#restli-20-protocol-array-notation)
.

For example,


    [
      { "keypart1":"v1", "keypart2":"v2" }, { "keypart1":"v3", "keypart2":"v4" }
    ]
    

It’s serialized form is:


    // Complex key as a serialized string:
    ids=List((keypart1:v1,keypart2:v2),(keypart1:v3,keypart2:v4))
    

If <code>$params</code> are in a batch complex key key, they included in
the same object as their id portion. e.g.


    ids=List(($params:(parmkeypart1:v5),keypart1:v1,keypart2:v2),($params:(parmkeypart1:v55),keypart1:v11,keypart2:v22))
    

When complex keys are used in batch requests, they are often included
both in the URI and in the JSON body.

For example, a batch update request has the ids in the URI as well as
the “entities” part of the body:


    PUT /widgets?ids=List((keypart1:v1,keypart2:v2),(keypart1:v3,keypart2:v4))
    
    {
      "entities": {
        "(keypart1:v1,keypart2:v2)": { <content to put for v1,v2> }
        "(keypart1:v3,keypart2:v4)": { <content to put for v3,v4> }
      }
    }
    

As long as the [protocol 2.0 array
notation](#restli-20-protocol-array-notation)
is being adhered to no addional escaping is required for complex keys in
a batch request.

### Query parameters as complex types in protocol 1.0

Complex types work the same for query params as they do for keys. See
the above sections for details. E.g. for a complex type:


    {
      "a": 1,
      "b": 2
    }
    

In protocol 1.0 a query param named “param1” of this type would be:


    ...?param1.a=1&param1.b=2
    

In protocol 2.0 a query param named “param1” of this type would be:


    ...?param1=(a:1,b:2)
    

## Empty List Parameters

Suppose you have a finder called <code>search</code> with a parameter
called <code>filters</code> which is a list of some type.

In protocol 1, if a client attempted to send an empty list the URL would
look as follows -


    /resource?q=search
    

Note that the parameter is lost in this case, and on the server side it
would be treated as a null or uninitialized field. This was a bug in
protocol 1\!

We have fixed this in protocol 2. Here is how the URL looks in protocol
2 when you send in an empty list -


    /resource?q=search&filters=List()
    

In other words, <code>List()</code> denotes an empty list in protocol 2.
This applies to all cases in protocol 2 where lists are being used.

## Empty map parameters

Suppose you have a finder called <code>search</code> with a parameter
called <code>preferences</code> which is a map of some type.

In protocol 1, if a client attempted to send an empty map the URL would
look as follows -


    /resource?q=search

Note that the parameter is lost in this case, and on the server side it
would be treated as a null or uninitialized field. This was a bug in
protocol 1\!

We have fixed this in protocol 2. Here is how the URL looks in protocol
2 when you send in an empty map -


    /resource?q=search&preferences=()
    

In other words, <code>()</code> denotes an empty map in protocol 2. This
applies to all cases in protocol 2 where maps are being used.

## Empty string parameters

Empty strings are a special case in Rest.li Protocol 2.0. They must be
represented with two single quotes <code>’’</code>. This applies if the
empty string is a value or a map key. In Rest.li 1.0, empty strings
would simply appear as
nothing.

| Request Details                                                                       | Protocol 1.0 URL                                | Protocol 2.0 URL                                 |
| ------------------------------------------------------------------------------------- | ----------------------------------------------- | ------------------------------------------------ |
| FINDER with string param; client attempts to send empty string                        | /resource?q=finderWithString\&myStringParam=    | /resource?q=finderWithString\&myStringParam=’’   |
| FINDER with list param; client attempts to send list containing a single empty string | /resource?q=finderWithList\&myListParam\[ 0 \]= | /resource?q=finderWithList\&myListParam=List(’’) |

---
layout: guide
title: Modeling Resources with Rest.li
permalink: /modeling/modeling
excerpt: This document will describe how to model a resource using Rest.li.
index: 2
---

# Modeling Resources with Rest.li

## Contents

  - [Rest.li’s Uniform Interface](#restlis-uniform-interface)
  - [Rest.li Modeling Tips](#restli-modeling-tips)
  - [Common Modeling Challenges](#common-modeling-challenges)
  - [Options for Modeling Entity Relationships](#options-for-modeling-entity-relationships)
  - [General Tips](#general-tips)
  - [When All Else Fails: Non-Uniform Interfaces](#when-all-else-fails-non-uniform-interfaces)

This document will describe how to model a resource using Rest.li. Before reading this page, you may want to get a feel for how to write and build code with Rest.li by reading the [Tutorial to Create a Server and Client](/rest.li/start/step_by_step).

## Rest.li’s Uniform Interface

Rest.li is intended to foster the design of Uniform Interfaces. The idea
of Uniform Interfaces is core to REST because it establishes standard
patterns and behaviors that allow clients to explore and make sense of
interfaces that they have never seen before.

Rest.li’s approach to uniform interface is based on REST principles and
comprises the following:

  - Resource Identifiers
  - Self-Descriptive Messages
  - Resource Methods
  - Resource Types

### Resource Identifiers

Some resource types have an identifier, a name by which it can be
accessed. Rest.li resource identifiers are URIs that follow specific
patterns.

### Self-Descriptive Messages

Rest.li messages are built with a JSON-encoded body, which provides a
self-describing structure for the data in the message. The Rest.li
framework provides convenient access to messages using Pegasus
RecordTemplates.

Since Rest.li is built on HTTP, messages may also contain metadata in
HTTP headers.

### Resource Methods

Resources are the nouns in the Rest.li world, and Resource Methods are
the verbs. Rest.li provides a standard set of Resource Methods that
describe what clients may do with the resources. These verbs are
different than the HTTP verbs (GET, POST, PUT, DELETE, and so on) but
map onto them (CREATE is a POST, UPDATE is a PUT, FIND is a GET).

  - CREATE is used to create an entity. Create requires that the
    resource identifier is assigned by the server. (HTTP verb: POST)
  - GET is used to read an entity. Get may require the client to provide
    the resource identifier, depending on the resource type. (HTTP verb:
    GET)
  - UPDATE is used to modify an entity. Update may require the client to
    provide the resource identifier, depending on the resource type.
    (HTTP verb: PUT)
  - DELETE is used to remove an entity. Delete may require the client to
    provide the resource identifier, depending on the resource type.
    (HTTP verb: DELETE)
  - FIND is used to search for entities. Find allows the client to
    provide query parameters for the search. Find methods return a list
    of entities. (HTTP verb: GET)

CREATE, GET, UPDATE, DELETE and FIND also have a corresponding batch method.
For example, BATCH\_GET is used to read multiple entities and requires
the client to provide a list of resource identifiers. It returns a map
from identifiers to entities.

### Resource Types

Each Rest.li resource endpoint is one of the following types:
Collection, Simple, Association. Additionally, each resource endpoint
may be a sub-resource of any other resource. These resources can be implemented with synchronous or asynchronous templates. Asynchronous is recommended if the code implementation is non-blocking.

#### Collection

Collection is the most frequently used resource type. A collection
models a key/value map of entities. It may be helpful to think of a
database table with a primary key. Collections have a key type, used for
identifying entities in the collection, and a value type used to
represent the entity itself.

Collections can support all of the Rest.li resource methods described
above.

Synchronous collections are declared by creating a class that extends
`CollectionResourceTemplate` (or `ComplexKeyResourceTemplate` for complex
keys, see below for details).

Asynchronous collections are declared by creating a class that extends `CollectionResourceAsyncTemplate` or `CollectionResourceTaskTemplate`. 

For example:

```java  
@RestLiCollection(name=“items”, …)  
public class ItemsResource extends CollectionResourceTaskTemplate\<Long,
Item\>  
```

Defines an asynchronous resource with a URI of the form:

```  
/items/{longKey}  
```

For keys with complex hierarchical data structures, use
`ComplexKeyResourceTaskTemplate`.

For Example:

```java  
@RestLiCollection(name=“widgets”, …)  
public class WidgetResource implements extends
ComplexKeyResourceTaskTemplate\<WidgetKey, EmptyRecord, Widget\>  
```

Defines a resource with a URI of the
form:

```  
/widgets/number={number}\&thing.make={thing.make}\&thing.model={thing.model}  
```

#### Simple

A simple resource models a singleton entity in a particular scope.
Simple resources support the Rest.li resource methods GET, UPDATE and
DELETE.

Synchronous simple resources are declared by creating a class that extends
`SimpleResourceTemplate`.

Asynchronous simple resources are declared by creating a class that extends `SimpleResourceAsyncTemplate` or `SimpleResourceTaskTemplate`.

For example:

```java  
@RestLiSimple(name=“selectedItem”, …)  
public class SelectedItemResource extends SimpleResourceTaskTemplate<Item>  
```

Defines a resource with a URI of the form:

```  
/selectedItem  
```

#### Association

Associations are structured like specialized collections but are used
for a different modeling purpose. Associations model relationships
between entities. Associations are like mapping tables in a database.
Associations have a compound key consisting of multiple partial keys.
Each partial key references one of the associated entities. Like
collections, associations have a value type. However, in the case of
associations, the value type is used to model attributes on the
relationship between entities.

Associations support all of the Rest.li resource methods except for
Create. Create requires the server to assign the resource identifier,
which is incompatible with the constraint that an association’s partial
keys are “foreign” keys from the referenced entities. Instead of using
Create, new association relationships are made by using Update, which
allows the client to provide the resource identifier.

Asynchronous association resources are declared by creating a class that extends `AssociationResourceAsyncTemplate` or `AssociationResourceTaskTemplate`.

For example:

```java  
@RestLiAssociation(name="myRelations", assocKeys={`Key(name=“key1”, type=long.class), @Key(name=“key2”, type=long.class)}, …)  
public class MyRelationResource extends AssociationResourceTaskTemplate<Relation> { … }  
```

Defines a child resource with a URI of the form:

```  
/myRelations/key1={longKey1}\&key2={longKey2}  
```
#### Asynchronous resources
Asynchronous Rest.li resources should be used when the downstream implementations are non-blocking. This means that your implementation should not be blocking while waiting for any downstream service. If the downstream service has an asynchronous implementation, please use that.

There are asynchronous resource templates for Collection, Simple, Association, and ComplexKey resources.

Task templates should be used when the resource implementation will leverage ParSeq `Task`s. 

Async templates should be used when the resource implementation will leverage `Callback`s.

Please see [Async Server Implementations](/rest.li/Asynchronous-Servers-and-Clients-in-Rest_li/async-server-implementations) for more information on implementation.

#### Child Resources (Sub-resources)

Child resources are resources that are referenced through a parent
resource. In Rest.li, the resource identifier for a child resource is an
extension of the resource identifier for the parent resource. This means
that the child resource has access to all of the information used to
access the parent resource, including entity keys. A common pattern for
using child resources is when accessing an element of a collection
requires the key of another collection (the parent resource).

For example:

```java  
@RestLiCollection(parent=MyParentResource.class, name=“mySubResources”,…)  
public class MySubResource extends CollectionResourceTemplate\<String,MySub\> { … }  
```

Defines a child resource with a URI of the form:

```  
/myParentResources/{parentResourceKey}/mySubResources/{stringKey}  
```

### Resources Customizations

Rest.li resources may be customized to handle a variety of use cases.
The most common means of customization is to only implement the methods
appropriate for a particular use case. Rest.li allows you to implement
as many or as few of the resource methods as you choose. The framework
will understand which methods you have implemented, and will advertise
only those methods to clients.

Here are some examples.

#### Read-Only Collections

A read-only collection can be used to expose entities that you do not
want your clients to be able to modify. This might be useful if you need
to expose a view which is derived from other data, or if you need to
expose entities whose lifecycle is managed internally by your domain.

Read-Only collections can easily be implemented using Rest.li
Collections, by only including read methods such as GET, BATCH\_GET and
FIND. By omitting any write operations (CREATE, UPDATE, DELETE) the
collection becomes read-only.

#### Natural Keys

Another variation on collections is a “natural key” collection, where
the identifier for each entity is one of the domain attributes of the
entity itself. (The alternative is a synthetic key collection, where the
identifier is assigned arbitrarily, such as from a sequence number).

Natural key collections are implemented in Rest.li using Collections, by
omitting the implementation for CREATE, and instead using UPDATE to add
entities into the Collection. This is because CREATE is used when the
server assigns the key for the entity. Although the name may be
counter-intuitive, using UPDATE for this case is correct, because it is
the resource method with the best defined semantics (PUT this entity
representation at this location).

#### Complex Keys

By extending `ComplexKeyResourceTemplate` instead of
`CollectionResourceTemplate`, a collection may use any complex type (any
defined pegasus record type) as a key.

#### Factories

A “Factory” Resource is used when the representation of an entity is
assigned by the server using some input information provided by the
client. For example, an authentication service might create a Session
entity given a username and password, but the Session entity is created
internally by the service.

To create a Factory resource in rest.li, use a Collection which only
implements CREATE (and optionally DELETE). The value type of the
Collection should be a Pegasus schema representing the input to the
factory method. The key type should be the key for the resulting entity.

Note that a Factory would normally be paired with a Collection (perhaps
a Read-Only Collection) which provides access to the entities
themselves.

## Rest.li Modeling Tips

1.  Start with nouns (“things”) in your domain. These will be your
    resources and entity representations.
2.  Think about the keys for each entity — what is the minimal
    information you need in order to GET (read) an entity?
3.  For any entity that can be accessed with a single key, try creating
    a root-level Collection resource. Implement as many of the Rest.li
    methods as you reasonably can.
4.  For a singleton entity, try creating a root-level Simple resource
    and implement the GET, UPDATE and DELETE methods where applicable. 
5.  For entities that require multiple keys to access,
    1.  If all but one of the keys belongs to other resources, try
        modeling as a child resource of another entity.
    2.  If all of the keys belong to other resources, consider whether
        your entity is really a relationship between the other entities.
        If so, try creating an association.
    3.  If all of the keys are unique to this entity, you may have a
        collection with a complex key.

## Common Modeling Challenges

### Common Key, Different Entity Type

Rest.li collections have a single value type that is used for all of the
resource methods supported by the collection. There are cases where you
want to use different entity representations with the same key, for
example, the input to CREATE is a different type than that returned by
GET (See Factories section above). In such cases, you have two options:

1.  Use a union in the Pegasus schema for your value type.
2.  Create a different resource for each value type

### Common Entity Type, Different Keys

Sometimes the same entity can be accessed using different sets of keys.

For example, Products belong to Companies. They are normally accessed
efficiently by using both the CompanyId and ProductId, so the Product
collection is a child resource of the Company collection. But if there
is a legacy access path which uses only ProductId, this method cannot be
modeled on the Product resource because the client does not have a key
for the parent Company.

In such cases, you have a few options:

1.  Create a separate resource for each access path
2.  Use FIND methods for some or all of the access paths

## Options for Modeling Entity Relationships

A frequently encountered question is how relationships between entities
should be modeled. Suppose you have two objects, A and B - should you
model their relationship by:

  - Creating an association between A and B?
  - Making B a subresource of A?
  - Making B a field of A?

Here are some rules of thumb to help answer this question:

Associations are preferred when:

  - A and B have independent lifecycles. For example, they can both
    exist without needing to be related
  - A and B have a many-to-many / bidirectional relationship (you can
    lookup in either direction)
  - You need to store attributes on the relationship itself
  - You need to relate more than two objects: A, B, and C
  - There is more than one type of relationship between A and B

Fields are preferred (B is a field of A) when:

  - B exists only as a part (fragment) of A. A is not complete without
    B.
  - B does not have its own independent key
  - B models a value, not an entity. For example, identity is not
    important; only the values of attributes matter for equality.
  - There are a small number of B objects related to any given A object,
    such that they can all be accessed together.
  - B is usually accessed and modified as part of A

Subresources are preferred (B is a subresource of A) when:

  - B has its own key, which is unique within the scope of A.
  - A’s key is required to access B
  - B is frequently accessed or modified independent of A
  - B is queried as a collection, e.g., using filters, sorts, or
    pagination
  - B models an entity that is dependent on A, but which is not a part
    of A
  - Each B relates to exactly one A

## General Tips

  - Remember that it’s ok to have multiple separate Resources with the
    same Pegasus schema as value type, as long as each resource
    represents a distinct pattern of interaction. Likewise, it’s okay to
    have separate Resources which access the same underlying
    implementation.

<!-- end list -->

  - Resource identifiers are the key (pun intended) to parent/child
    resource relationships. A resource should be made a child resource
    if and only if it depends on the parent resource’s key. If a
    resource can be accessed without providing the parent’s key, it
    should not be a child.

<!-- end list -->

  - Use separate resources for different representations of the same
    underlying implementation. If you want to provide more than one
    representation (Pegasus schema) of the same information, you can
    create a resource for each representation. However, if one
    representation is just a subset of the other, you should use
    Rest.li’s field projection support.

## When All Else Fails: Non-Uniform Interfaces

Uniform Interfaces give us a powerful way to expose resources to the
broadest possible set of clients. However, if you have a requirement
which can’t reasonably be modeled using the uniform interface
constructs, Rest.li does provide a loophole to help you. Actions are a
special type of resource method which allow arbitrary RecordTemplate
types as input and output parameters, and which have no constraints on
semantics. It should be possible to model any operation as an Action,
allowing you to fit your special case within Rest.li. However, because
Actions do not conform to the Uniform Interface, actions cannot easily
be used by higher-level frameworks like query languages, etc., which may
be built on top of Rest.li. You should therefore avoid Actions whenever
there is another reasonable option.

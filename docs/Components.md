---
layout: guide
title: Components
permalink: /Components
---

Rest.li is the top layer of a larger software stack code named 'pegasus'.  Pegasus is comprised of the following major components.

### data/

The pegasus data layer provides an in-memory data
representation structurally equivalent to JSON, serialization to/from JSON
format, and a schema-definition language for specifying data format.

### generator/

The pegasus data template code generation tool generates
type-safe Java APIs for manipulating pegasus data objects.

### r2/

The pegasus request/response layer provides fully asynchronous
abstractions for transport protocols, along with implementations
for HTTP based on Netty and Jetty.

### d2/

The pegasus dynamic discovery layer uses Apache Zookeeper to
maintain cluster information.  D2 provides client-side software load balancing.

### restli-*/

The pegasus Rest.li framework provides a simple framework
for building and consuming RESTful resources.  Rest.li provides resource
patterns that streamline common CRUD use cases for collections of entities and
associations between entities.
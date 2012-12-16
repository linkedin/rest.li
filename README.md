What is Pegasus?
================
Pegasus is a framework for building robust, scalable service architectures
using dynamic discovery and simple asychronous type-checked REST + JSON APIs.

Components
----------
Pegasus comprises the following major components:

### data/

The pegasus data layer, which provides an in-memory data
representation structurally equivalent to JSON, serialization to/from JSON
format, and a schema-definition language for specifying data format.

### generator/

The pegasus data template code generation tool, which generates
type-safe Java APIs for manipulating pegasus data objects.

### r2/

The pegasus request/response layer, which provides fully asynchronous
abstractions for transport protocols, along with implementations
for HTTP based on Netty and Jetty.

### d2/

The pegasus dynamic discovery layer, which uses Apache Zookeeper to
maintain cluster information.  D2 provides client-side software load balancing.

### restli-*/

The pegasus Rest.li framework, which provides a simple framework
for building and consuming RESTful resources.  Rest.li provides resource
patterns which streamline common CRUD use cases for collections of entities and
associations between entities.

Getting Started
---------------
Check our [wiki](pegasus/wiki) and the [Quick Start Guide](pegasus/wiki/Quick-Start-Guide).
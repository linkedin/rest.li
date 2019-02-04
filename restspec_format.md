---
layout: api_reference
title: Rest.li restspec (IDL) format
permalink: /spec/restspec_format
index: 2
excerpt: Rest.li restspec (IDL) format
---

# Rest.li restspec format

Rest.li uses a RESTSpec JSON schema as its interface definition language
(IDL).

Each RESTSpec file contains the full definition of a root level resource
and all its sub-resources.

RESTSpec files use the `.restspec.json` file extension. The contents of
each file is JSON describing the resource according to the
[ResourceSchema](https://github.com/linkedin/rest.li/blob/master/restli-common/src/main/pegasus/com/linkedin/restli/restspec/ResourceSchema.pdsc)
data schema.

For example:

The RESTSpec for the
[GreetingsResource](https://github.com/linkedin/rest.li/blob/master/restli-int-test-server/src/main/java/com/linkedin/restli/examples/greetings/server/GreetingsResourceImpl.java)
implementation is
[com.linkedin.restli.examples.greetings.client.greetings.restspec.json](https://github.com/linkedin/rest.li/blob/master/restli-int-test-api/src/main/idl/com.linkedin.restli.examples.greetings.client.greetings.restspec.json)

---
layout: guide
title: Test Suite
permalink: /test_suite
---

# Rest.li Cross-Language Test Suite Framework

## Contents
  - [Introduction](#introduction)
  - [Motivation](#motivation)
  - [Getting Started](#getting-started)  
  - [Design and Code Structure](#design-and-code-structure)
  - [Next Steps](#next-steps)
  - [Troubleshooting](#troubleshooting)

## Introduction

The Rest.li Cross-Language Test Suite is a framework for testing and comparing Rest.li implementations.
The framework contains the Rest.li Test Suite Specification, which provides test data
and testing guidelines that specify which features and scenarios to test, and how to test them.
These test guidelines should be followed when testing a particular language implementation of Rest.li. 
Currently, the framework only supports client testing, though we hope to add server testing support in the future.

The test suite also includes Java tests that follow the [Rest.li Test Suite Specification](#restli-test-suite-specification), demonstrating how the spec
can be used to test the Java implementation of Rest.li client. 


## Motivation

The Rest.li Test Suite Specification will help us achieve consistency and quality across multiple Rest.li client
language bindings by:

* Reducing testing effort required to add a new Rest.li client implementation
* Standardizing test plans that can be shared among multiple Rest.li client implementations
* "Certifying" Rest.li client implementations with a quantifiable measure of quality
* Making it easier to develop compatible Rest.li implementations, so that a Rest.li client in one language can be used with a Rest.li server in another language. 

We have leveraged this Rest.li Test Suite Specification to implement tests for Java and Python Rest.li client bindings,
respectively. With this sharable test suite spec, we have identified their coverage and feature parity in this 
[Compatibility Matrix](testsuite_compatibility_matrix.md).

## Getting Started

### How to Download Project
Make sure you have installed [gradle](https://gradle.org/), which is used for building the test suite.
 
Download the test suite from its git repository: 
```
git clone <coming soon>
cd restli-testsuite
```

### How to Run Java Tests and Expand Test Spec
For new test suite users, take a look at [How to Run the Java TestNG Tests and Expand the Test Suite Specification](testsuite_how_to.md). This will walk you through running
the provided Java TestNG tests. It will also explain how to expand the Rest.li Test Suite Specification.

### How to Add Tests to a New Client Language Binding
If you would like to follow the spec and add tests for a new language binding, refer to
[How to Add Tests to a New Client Language Binding](testsuite_new_language.md). 
This is a more involved process that requires understanding the spec. It will be helpful to go over the Design and Code
Structure section of this document first.


## Design and Code Structure

### Background
This documentation assumes a background in [Rest.li](https://github.com/linkedin/rest.li). Specifically, test developers should be familar with Rest.li data
schema, IDL, and wire protocol format as demonstrated by the following links: 

* Data schema (```.pdl```) - https://linkedin.github.io/rest.li/pdl_syntax
* IDL (```.restspec.json```) - https://linkedin.github.io/rest.li/spec/restspec_format
* Rest.li Wire Protocol - https://linkedin.github.io/rest.li/spec/protocol

Implementers are encouraged to explore the
test data and reference documentation in tandem to learn the details of Rest.li.

### Design Principles

There are two main ways of interacting with the Rest.li Test Suite Specification: 

1. Expanding or updating the Rest.li Test Suite Specification for new Rest.li features or updated Rest.li behavior.
2. Following the spec to add tests for a Rest.li client binding implemented in a new language.

These distinct ways of using the Rest.li Test Suite Specification point to an important distinction between language-independent
and language-specific components within this test suite. The Rest.li Test Suite Specification consists of
language-independent data and test guidelines, meant to be used for standardizing cross-platform testing. 
It cannot immediately be used to test a specific language implementation of Rest.li client. As a test developer,
you follow the Rest.li Test Suite Specification to write tests in your desired language. 

Throughout the documentation, we use "Rest.li Test Suite Specification" to refer to the language-independent
components of the project. We use "Java TestNG Tests" or "Python pytest Tests" for its language-dependent components, which
follow the Test Suite Specification.

### Components 
This test suite is composed of:

* Rest.li Test Suite Specification  
    * Data files in language-neutral formats such as ```.json```, ```.pdsc```,```.restspec.json```.
    * A ```manifest.json``` file containing all the test data provided in this suite to help drive automated test execution.
    * Guidelines on how to use the spec to validate a Rest.li client implementation.
* A Java TestNG suite that uses the spec's test data to validate the Java Rest.li client implementation.
* A Rest.li Java server for wire protocol data generation and potential integration testing


The test suite is structured as follows: 
```
.
|—— client-testsuite
|   |—— manifest.json
|   |—— data/
|   |—— requests/
|   |—— requests-v2/
|   |—— responses/
|   |—— responses-v2/
|   |—— restspecs/
|   |—— schemas/
|   |—— snapshots/
|   |—— src/
|   |—— ...
|—— restli-testsuite-server
|   |—— src
|   |—— ...
|—— build.gradle
|... 
```


#### Rest.li Test Suite Specification

##### manifest.json
This manifest file provides machine readable information about all the automated tests included in this spec.
It is shared across languages, and does not need to be changed when adding a test suite for a new language. 
It should be changed when expanding the Rest.li Test Suite Specification.

This file is intended to help drive portions of the test suite execution for each language. Although it does help,
writing additional assertions by hand will still be needed in most languages to validate correctness of in-memory
representations and language bindings.

The file is broken down into a few main sections:

* jsonTestData - list of JSON data files.
* schemaTestData - list of Rest.li data schema files (```.pdsc```) as well as JSON data files matching the schemas.
* wireProtocolTestData - list of Rest.li interface definition files (```.restspec.json```) as well as test HTTP requests and responses,
  in the form of files, for operations supported by the interface definition.

##### Test data folders
These folders contain testing data used by the automated tests in the spec. Most of them were generated by Java Rest.li,
and should be used when following the spec to test your Rest.li client implementation. 

The following folders are included:
* **data**: input JSON data for testing schemas and json serialization
* **requests**: incoming HTTP requests for wire protocol tests (Rest.li protocol 1.0)
* **requests-v2**: incoming HTTP requests for wire protocol tests (Rest.li protocol 2.0)
* **responses**: expected HTTP responses for wire protocol tests (Rest.li protocol 1.0)
* **responses-v2**: expected HTTP responses for wire protocol tests (Rest.li protocol 2.0)
* **restspecs**: IDL (Interface Description Language) generated from Java resources. These should be used by Rest.li implementations
to make request builders.
* **snapshots**: snapshots for resource compatibility checker

#### Java TestNG Tests
In ```src/test/java```, you can find the Java test suite, which uses the TestNG testing framework. 
You can add test suites for new languages in ```src/test/```. The Java suite is separated into two folders:
* **test**: Java files containing the tests (```TestRestClientAgainstStandardTestSuite``` and ```TestRestClientWithManualAssertions```), and utility methods (```StandardTestSuiteBase```) for running tests. 

* **testsuite**: Java files for building and loading requests and responses.

#### Sample Rest.li server 
The ```restli-testsuite-server``` directory contains code for a Rest.li Java server. Based on java resources defined there,
Java Rest.li will generate language-independent restspecs in the ```client-testsuite/restspecs``` folder. Using these restspecs,
other Rest.li implementations can generate request builders without modifying server code.
When adding a new test, you may want to update or add a resource to ```restli-testsuite-server/src/main/java/testsuite```.
See the section on wire protocol tests in [How To Run the Java TestNG Tests and Expand the Test Suite Specification](testsuite_how_to.md).

## Rest.li Test Suite Specification Coverage

The test suite spec is intended to cover three categories of Rest.li client behavior: **JSON serialization, data template
generation, and wire protocol**. Each test category is described in more detail below. 
  
### JSON Tests
These are tests for serialization and deserialization. Tests cover basic JSON and JSON corner cases such as large
numbers, special characters and encodings.


| JSON feature | Details |
|--------------|---------|
| Basic&nbsp;Types| string, number, boolean, null, object, array|
| Empty&nbsp;Collections| empty object, empty array, object with empty arrays, object with empty objects, array of empty object, array of empty arrays| 
| Large&nbsp;Numbers |int32 and int64|
| Special&nbsp;Characters| periods in key|
| Unicode| Chinese character and e with an accent mark|


### Data Schema Tests
These are tests for data template generation from schema. Tests cover schema types (records, unions, enums, typerefs,
...), primitive types, optionals and defaults. Backward compatibility rules are also covered.
                                                            

| Schema feature | Details|
|------------------|--------|
| Primitive&nbsp;Types|int, long, float, double, bytes, string |
| Complex&nbsp;Types | array of maps, map of ints, record |
| Complex&nbsp;Type: Unions | union of complex types, union of primitives, union of same types |
| Enums | with properties and with alias|
| Fixed&nbsp;Type | |
| Typerefs | for string, array, chained typeref, array, map, field, union|
| Include | include, and include with include |
| Default&nbsp;Fixup | to see if required fields are "fixed up" with defaults|
| Optional&nbsp;Fields | |

### Wire Protocol Tests
These are tests for building requests and decoding responses. Tests cover serializing/deserializing of URLs, 
headers and bodies, escaping/encoding, batch formats, projections, and partial updates.

We test for well-formed requests by comparing the built HTTP request
with the expected HTTP request in the ```requests/``` or ```requests-v2/``` folder. We compare url, method, headers, and body.
We test that Rest.li can decode a Rest.li response from an HTTP response by checking the decoded Rest.li response for
the correct values. We compare the response's status and error message with the status and error message specified by 
```manifest.json```. The body of the response is tested through manual assertions that check for the correct values.

#### Basic Resource Method Tests for Requests/Responses

| Rest.li Method | Collection | Simple | Association | Action Set|
|--------|------|----------------|----------------|------------------|
| get | x | x | x | |
| batch-get | x |  | x | |
| finder | x | | x | |
| batch-finder | o | | o | |
| create | x | | | |
| create&nbsp;with&nbsp;returned&nbsp;entity | x | | | |
| batch-create | x | | | |
| update | x | x | x | |
| partial update| x | |o| |
| batch-update| x |  | x | |
| batch-partial-update| x| | o | |
| delete | x | x | x | | 
| batch-delete | x | | x | | 
| action | o |  o | o | x | 

"x" - test is included in Rest.li Test Suite Specification  
"o" - test is not included but method should be supported by the resource  
" " - test is not included and method should NOT be supported by resource


#### Resource Key and Parameter Tests

| Key Feature | Rest.li Method used|
|-------------|---------------|
|Key&nbsp;with&nbsp;Union&nbsp;of&nbsp;Simple&nbsp;and&nbsp;Complex&nbsp;Members | get | 
|Query&nbsp;Params (int, string, long, string array, message array, string map, primitive union, complex types union, optional string, url typeref)| get | 
|Complex&nbsp;Key (simple record) | get, create, delete, update batch-create, batch-delete, batch-get, batch-update, partial-update |
|Special&nbsp;Chars&nbsp;in&nbsp;ComplexKey&nbsp;Strings | get, batch-get|
 

#### Error tests

| Error | Resource used | Rest.li Method | Details |
|-----|-----|----|----|
|404 | Collection | get | Send empty get request|
|400 | Collection | update | Request has a missing required field | 
|500 | Collection | create | Create request with id field |
|Error&nbsp;Details | Collection | create | CreateResponse with Error Details| 
|Batch&nbsp;Results&nbsp;with&nbsp;Errors | Collection | batch_update | Batch update with one good and two bad requests| 


#### Misc. Tests

| Feature | Resource tested| Method used|
|---------|---------|------|
| Typeref | Collection, Association | get |
| Subresource | Collection, Association | get |
| Projection | Collection, Association, ComplexKeyResource | get | 

## Next Steps

### Improvements to Test Suite Specification
* Add test for record with lowercase name 
* Add test for tunneled query params
* Add test that includes unicode (non-ascii) characters on wire
* Extend spec to include cross-language server testing

### Gaps in example Java Tests
* Enrich manual assertions for wire protocol and schema tests
* Fill in the gaps listed in the Base Resource Method Tests table 

### Future Work
* Expand reference implementations of spec to different languages, such as mobile.

## Troubleshooting

For questions or troubleshooting, refer to [Test Suite Troubleshooting](testsuite_troubleshooting.md). 

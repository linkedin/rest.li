---
layout: guide
title: Test Suite - How To
permalink: /testsuite_how_to
---

How to Run the Java TestNG Tests and Expand the Test Suite Specification
========================
This page is meant to help a new user get familiarized with using the Rest.li Test Suite Specification.
It explains how to run the example Java TestNG tests, and how to expand the Test Suite Specification. 

For an overview of the test suite, refer to the [test suite overview](testsuite_overview.md).


How to Run the Java TestNG Tests
--------------------
### Test Categories
The Rest.li Test Suite Specification is language-independent, and it provides test data and guidelines to follow when
testing a Rest.li client.
To validate that the spec's tests all pass using the Rest.li Java client binding, *and* to demonstrate how
to use the Test Suite Specification, this project contains a Java TestNG test suite,
located in the `client-testsuite/src/test/java` folder.

The Java TestNG test suite follows the Rest.li Test Suite Specification, and it has two types of tests: data driven
automated tests and manual assertion tests. 
##### Data Driven Automated Tests
These tests are located in the `TestRestClientAgainstStandardTestSuite` class, and the tests run are specified by `manifest.json`.

For an example, consider the Java schema tests in the Java suite. Our testSchema() method verifies that Rest.li correctly
generates a data template from a schema, and `manifest.json` specifies a list of schema-data pairs. These pairs are passed
to testSchema() using a TestNG data provider, so all the schema cases specified by `manifest.json` are tested in the Java suite.  

##### Manual Assertion Tests
These tests are located in the `TestRestClientWithManualAssertions` class.
Manual assertions are used to verify that Rest.li responses are decoded properly from http responses. Each test verifies
the content of the Rest.li response fields. These assertions are built by hand, but they should be similar across test suite languages. 


### Run the Tests

To run all tests in the provided TestNG suite:

```
gradle test
```

This command should run successfully, since the Java implementation of the Rest.li client should pass all the tests.  

Because the build is successful, it's not immediately clear what is happening when ```gradle test```  is run. 
For better illustration, this guide will help you make tests that fail when run:

* Try adding an untrue assertion to testSchema() in `TestRestClientAgainstStandardTestSuite.java`. 
Running ```gradle test``` again will result in multiple tests failing, because testSchema() is used for all the schema
tests defined in `manifest.json`.


* Try adding an untrue manual assertion to `TestRestClientWithManualAssertions.java`. For example, the following assertion
is untrue because the response should have 1 as its id:

```java  
@Test
public void testCollectionCreateAgain() throws Exception
{
  Response<EmptyRecord> response = loadResponse("collection-create", "responses/collection-create.res");

  Assert.assertEquals(response.getId(), "2");
}
```
  
  Run `gradle test` again, and this test should fail, while the other tests pass.




Step-by-Step Guide to Expand Test Suite Specification
-------------------------------
This section explains how to add a new language-independent test to the Rest.li Test Suite Specification.
Please do not modify existing tests: multiple language implementations are using these test and changing existing tests 
may break their test suites.

Keep tests simple!  They should test a single case and test it well.

### Add a JSON test
To add a new JSON corner case, e.g. `corner-case.json`:
1. In `client-testsuite/src/data/`, add `corner-case.json`.
2. Find the "jsonTestData" list in `manifest.json`, and add a list entry of the form {"data": "data/[filename].json"}.

```js
"jsonTestData": [
  ...
  { "data": "data/corner-case.json" }
  ...
],
```

### Add a Data Schema test
To test a new schema, `NewSchema.pdsc`:
1. In `client-testsuite/src/schemas/testsuite/`, add a new .pdsc file, `NewSchema.pdsc`, with fields and
   field types supported by Rest.li. Set "namespace" to "testsuite". Rest.li client should generate a data template from 
   this .pdsc file. 
2. In `client-testsuite/src/data/`, add a corresponding .json file, `new-schema.json`, with test data to fill your new data template's fields. 
3. In `manifest.json`, find the schemaTestData list. Add an entry for your new schema, following the general format:
{"schema": "testsuite.[SchemaName]", "data": "data/[json-name].json"}

```js
"schemaTestData": [
  ...
  {"schema": "testsuite.NewSchema", "data": "data/new-schema.json"}
  ...
],
```

4. Run ```gradle build``` before running the test, so Rest.li will generate the data binding.


### Add a Wire Protocol test 
When adding a new wire protocol test, you also need to add its associated flat `.req` and `.res` files. 

The Java suite contains a convenience tool to generate `.restspec.json`, `.req`, and `.res` files for the
Rest.li Test Suite Specification. To use it:

1. Add or update the `*Resource.java` classes in the `restli-testsuite-server` project under `src/main/java/testsuite.` You can override a new method in an existing resource, or add a completely new resource class. For example, the following class is a simple collection resource that only overrides create() using the option to return the created entity.

```java
@RestLiCollection(name = "collectionReturnEntity", namespace = "testsuite")
public class CollectionReturnEntityResource extends CollectionResourceTemplate<Long, Message>
{

  @ReturnEntity
  @Override
  public CreateKVResponse create(Message entity) {

    if(entity.getMessage().equals("test message"))
    {
      return new CreateKVResponse<Long, Message>(1l, entity, HttpStatus.S_201_CREATED);
    }
    else if(entity.getMessage().equals("another message"))
    {
      return new CreateKVResponse<Long, Message>(3l, entity, HttpStatus.S_201_CREATED);
    }
    else
    {
      return new CreateKVResponse<Long, Message>(null, entity, HttpStatus.S_404_NOT_FOUND);
    }
  }
}
```

2. Re-generate the `.restspec.json` and `.snapshot.json` files:

    ```
    gradle publishRestIdl
    gradle publishRestSnapshot
    ```

3. Run the test server using:

    ```
    gradle JettyRunWar
    ```
    This will use the new restspecs to generate or update the appropriate request builders, which are used to make the
    requests in the following step.

4. Update `RequestResponseTestCases.java` in the language-specific suites of `client-testsuite` by adding a new
  request and test name to the map of Rest.li requests to be tested. 

   For the java implementation, this is done by modifying the builtRequests map in the buildRequests() function,
   in `client-testsuite/src/test/java/com/linkedin/pegasus/testsuite/RequestResponseTestCases.java`.

   ```java
    builtRequests.put("collectionReturnEntity-create", new CollectionReturnEntityRequestBuilders(_options).create().input(testMessage).build());
    ```

5. Re-generate the request and response files.  Files will be written to `requests/` and `responses/`, and to `requests-v2/` and `responses-v2/`:

    ```
    gradle generateRequestAndResponseFiles
    ```
    Note that Java's requestBuilders are generating the request files with the desired output. 
    These flat files can be used to test the other implementations for well-formed requests.

6. Update the "wireProtocolTestData" entry of `manifest.json` to include test data references to all the files you've added.
 
   If overriding a new method for an existing resource: 

    - Find name of your resource in the wireProtocolTestData list. Under "operations", add test for the new method that you added
    to `RequestResponseTestCases.java` in Step 4. The new operation test should look something like this, where test name is usually the resource and method, and "status" is the expected response status:
 
```js
{
  "name": "collection-get",
  "method": "get",
  "request": "requests/collection-get.req",
  "response": "responses/collection-get.res",
  "status": 200
},
``` 
   
   If adding a new resource:
    - Create a new entry for your resource in wireProtocolTestData list. This should follow the general format:

```js
"wireProtocolTestData": [
  { 
    "name": "collectionReturnEntity",
    "restspec": "restspecs/testsuite.collectionReturnEntity.restspec.json",
    "snapshot": "snapshots/testsuite.collectionReturnEntity.snapshot.json",
    "operations": [
      {
        "name":"collectionReturnEntity-create",
        "method": "create",
        "request": "requests/collectionReturnEntity-create.req",
        "response": "responses/collectionReturnEntity-create.res",
        "status": 201
      }
   ] 
  },
  ...
]
```
    - For the list of operation tests, follow the instructions for overriding a new method for an existing resource.
     
7. Now that the test suite spec includes new responses, you need to update the manual assertion tests in the
   language-specific test suites. You should write a manual assertion for decoding each new flat HTTP response. Ensure
   that the Rest.li client can decode an HTTP response to a Rest.li response,
   and that the Rest.li response is a correct representation of the HTTP response.
   

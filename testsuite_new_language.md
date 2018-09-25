---
layout: guide
title: How To - Add new language
permalink: /howto_add_new_language
---

How to Add Tests to a New Client Language Binding
--------------------------
This page explains how to follow the Rest.li Test Suite Specification to validate a new Rest.li client binding.
This guide is based on experience with Python, and your language implementation may diverge from this guide.

For a broader overview of code structure and test coverage, refer to [test suite overview](testsuite_overview.md).
For Java client binding example, please refer to the provided Java TestNG suite, which follows the Rest.li Test Suite
Specification to validate a Java Rest.li client implementation.

### New Language Set Up
To use the Rest.li Test Suite Specification for a new language implementation of Rest.li, you must set up code
generation within the project so you can test Rest.li client's code generation.
You must also write methods in the new language so you can use the language-independent data included in the
Rest.li Test Suite Specification.

Here is a step-by-step guide on how to set up a test suite for a new language implementation of Rest.li:
1.  In client-testsuite's ```build.gradle```, apply the plugin for your new implementation of Rest.li to enable code generation.  

    ```
    apply plugin: 'li-python-restli'
    ```
2.  Run the Rest.li code generators against all the files in the ```schemas/``` directory (for data
    bindings) and/or the ```restspecs/``` directory (for rest client bindings). 
    For Python and Java, this means running a ```gradle build```. In Java, the code is generated in ```src/```. In Python, 
    the code is generated  in ```build/```
    
    The generated code will be tested later in the testsuite.   
    
3.  Select a test framework.  You should use the same test framework you usually use for the language your Rest.li
    client is written in. For the Python test suite, we use pytest. 

4.  In your test framework, load the ```manifest.json``` file into a convenient in-memory representation before running the tests.

    In Python, a Manifest class is generated from the ```.pdsc``` provided by the test suite. 
    The ```manifest.json``` file is loaded into a Manifest representation that is used by the Python test suite.

5.  Based on your test framework, decide how you want to use your Manifest representation to drive your automated tests. 

    For example, consider json tests in the Python test suite, shown below. We get the list of manifest tests from the
    Manifest representation in json_test_data(). Then we use the pytest @parametrize decorator to pass the test names
    and data files into the function test_json(), which contains the testing logic. 
    ```python
    def json_test_data():
        """Data provider for json_test"""
        results = []
        for test in manifest.get_json_test_data():
            results.append(test)
        return results
    
    
    json_data = json_test_data()
    
    
    @pytest.mark.parametrize("json_test", json_test_data(), ids=[test.data_ for test in json_data])
    def test_json(json_test):
        """Tests for correct serialization/deserialization of provided 'data' files"""
        ...
    ```   
 
6.  Set up utility methods in your new language. These methods should provide a way to read in the files referenced by
    ```manifest.json```. You also need methods to compare and load flat HTTP requests and responses from the test suite's
    provided test data. 
 
    In the Java TestNG suite, these utility methods are part of a test suite base class that is extended by the classes
    containing actual tests. The Python pytest suite has no subclass/superclass structure because it makes pytest harder to use.
 
Now your test suite is set up to generate code and read language-independent files from the Rest.li Test Suite
Specification. 

### Writing Tests with the Provided Test Data
The next step is to implement tests using the spec's guidelines and provided test data. 
This will be a combination of manual assertions and data driven tests specified by ```manifest.json```.
#### Data Driven Tests
The tests are driven by ```manifest.json``` and cover three main categories: 
* JSON: serialization/deserialization of JSON
* schema: data template generation from schema
* wire protocol: building HTTP requests and decoding Rest.li responses from HTTP responses

##### JSON Tests 

Json tests ensure that Rest.li correctly serializes/deserializes all the provided ```data/``` files. 
These files include corner cases to check that they are supported.
 
One possible approach, which is used by the Java and Python test suites:

1. deserialize a JSON file to an in-memory representation
2. serialize the in-memory representation back to bytes (or a file)
3. deserialize the bytes again into another in-memory representation
4. comparing the two in-memory representations
(Note that comparing the serialized representations isn't always an option since JSON map entries are unordered)

```python
def test_json(json_test):
    """Tests for correct serialization/deserialization of provided 'data' files"""
    deserialized = load_json_file(os.path.join(*json_test.get_data_().split("/")))
    serialized = JSONRestliCodec.encode(deserialized)
    twice_deserialized = JSONRestliCodec.decode(serialized)
    for field in deserialized.keys():
        assert deserialized[field] == twice_deserialized[field]
    assert len(deserialized) == len(twice_deserialized) 
```  
 
##### Schema Tests

Schema tests ensure that Rest.li correctly generates language-specific data templates from language-independent data schemas.

For implementations that use data schemas to provide convenience data bindings from data schemas (either dynamically or
via code generation):

1.  Make sure you have run ```gradle build``` to construct data bindings for all the ```schema/``` files.
2.  Load instance data for each accessor according to the 'data' fields of the 'schemas' in ```manifest.json```.
3.  Verify the accessor is correctly constructed and provides access to the data correctly, writing custom asserts as needed
4.  If the schema contains default fields, validate that they are defaulted correctly

For implementations that provide validation:

1. load instance data for each 'schemas.data' file and validate it against the 'schemas.schema' files.  Note that some data
   files should fail to validate (and be marked as such in manifest.json).

##### Wire Protocol Tests 

When testing wire protocol, we want to ensure that the generated request builders can build correct HTTP responses.
We also want to ensure that the Rest.li client can correctly create a Rest.li Response representation from an 
HTTP response.  

To test requests and responses, flat files containing HTTP requests and responses are provided in
Rest.li Test Suite Specification's ```requests/``` and ```responses/``` folders, which are for Rest.li protocol 1.0.0.
These flat files have counterparts in ```requests-v2/``` and ```responses-v2/``` that use Rest.li protocol version 2.0.0.
but are otherwise identical.

A few possible approaches one might take to use these test files:

1. Organize the Rest.li client code such that a HTTP request can be produced and tested without actually sending it
   and such that a HTTP response stored in a flat file can be handled as though it was received in response to a request.
2. Create a simple mock HTTP server that uses the flat files to send the appropriate response back for each request, and
   returns a clear error message back if the client sends an incorrect request.

The Java and Python test suites use approach 1.

For Rest.li implementations that use restspecs to provide convenience rest client bindings from restspecs (either dynamically
or via code generation):

1. Construct rest client bindings for all the ```restspecs/``` files. 
2. Use the constructed rest client bindings to produce HTTP requests.  Do this for all the 'restspecs.operations.request'
   entries in ```manifest.json```.  Instead of sending the requests,  keep the request in an in-memory representation (a string,
   or basic HTTP representation) or write it to a file.
   
   In Python, the logic for this is as follows:
   1. Using the generated request builders, build all the Rest.li requests specified by Manifest. 
   2. For each Rest.li request, use a RequestsRestClient to build an HTTP PreparedRequest. In the next step, we will compare
   this with the expected HTTP request. 
   
3. Verify the HTTP request created by the rest client bindings matches the expected HTTP as found in the
   'restspecs.operations.request' files.  This may require writing a routine to compare HTTP requests that ignores header
   order, and does not fail for optional headers. For instance, Python includes an optional "User-Agent" header, which 
   the flat requests do not include. You may want to ignore such differences in wire protocol. 
   
4. For each of the 'restspecs.operations.response' files, verify the constructed rest client bindings are able to
   handle the response correctly (as if it were sent as an actual HTTP response).  Write custom assertions to verify all the
   fields in the HTTP response are correctly marshaled to the response representation provided by the constructed rest client
   bindings. See "Manual Assertions" for more details about custom test cases. 

For implementations that do not provide convenience rest client bindings:

1. At a minimum, you should provide utilities to serialize data to request URLs and deserialize data from response headers
  and batch map keys. Write tests to validate that all the ```.req``` files can be produced correctly using these utilities.

#### Manual Assertions
Some tests cannot be easily automated. For instance, if you want to check the correct value of a field of an in-memory 
representation, you do not want to validate it against another in-memory representation.
Instead, you can use manual assertions. 

Examples of test cases for manual assertions:
* Test for properly generated data template accessors: The generated templates are filled using test data in ```data/```, 
so assert that the accessors return the correct test data.
* Test for properly decoded HTTP responses: Rest.li decodes a Rest.li response from an HTTP response. Assert that the
decoded fields have the correct data. For instance, check that a returned entity has the proper fields. 
 
 

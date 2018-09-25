---
layout: guide
title: Test suite - Troubleshooting
permalink: /testsuite_troubleshooting
---

Test Suite Troubleshooting
================

### My language-specific test suite doesn't run at all:
While the tests specified by the test suite spec are helpful for finding incompatibilities between Rest.li
implementations, some incompatibilities prevent the project from building. When adding a new language or expanding the
Rest.li Test Suite Specification, it may be necessary to omit some of the spec's tests, schemas, or resources so that
the rest of the suite can run properly. The omitted features should be noted as incompatibilities.

##### DataTemplate generation fails
A .pdsc schema may not be supported by your Rest.li implementation. Ignore this schema when generating code so that the 
code generation does not fail and stop. 


##### RequestBuilder generation fails
In other languages, requestBuilders are generated from language-independent restspecs for a particular resources. 
This generation can fail (causing your build to fail) if the resource is not supported in your Rest.li implementation. 
If this is the case, you can skip requestBuilder generation for that resource, and skip wire protocol tests that use that
requestBuilder. 

##### Building requests in RequestResponseTestCases (or equivalent file) fails
Requests are built before running wire protocol tests, so an error here will prevent all the tests from running. Make 
sure the attributes of the request you are building are actually supported by your language. 

### My language-specific test suite runs but certain tests fail:

##### Consistent Behavior Incompatibilities
Failing tests can indicate that your Rest.li implementation has some incompatibilities with Java Rest.li. However, some
incompatibilities have no effect on Rest.li performance. For example, consider wire protocol requests.
When comparing the flat HTTP request with your Rest.li built request, you should not look for a carbon copy. Some differences
are acceptable. For instance, order may be different, or your implementation may use an optional header that is not included
in the flat HTTP request.
If your wire protocol tests fail due to acceptable differences, you may wish to note the incompatibility and 
make your tests more lenient. 
 
##### Inconsistent Behavior Incompatibilities
Failing tests can also indicate incompatibilities that affect behavior. 
For instance, the keywithunion wire protocol test fails because Python Rest.li does not support complex key
with params, while Java Rest.li does. This test should fail, as it indicates a gap in Python Rest.li's implementation of
the Rest.li Protocol.

### I want to test an internal Rest.li implementation:
This project is open source, which may cause integration problems if your implementation is internal.
Instead of adding a language-specific test suite to the project, you may need to import the Rest.li Test Suite Specification
folders into an internal project. Please refer to Design and Code Structure in 
[test suite overview](testsuite_overview.md) to know which language-independent files and folders are part of the test
suite spec.
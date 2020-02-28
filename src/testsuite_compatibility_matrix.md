---
layout: guide
title: Multi-language Compatibility Matrix
permalink: /multi_language_compatibility_matrix
---

# Rest.li Multi-language Compatibility Matrix

## Contents

  - [Supported Resources](#supported-resources)
  - [Resource Keys and Parameters](#resource-keys-and-parameters)
  - [Supported Data Templates in Test Suite Spec](#supported-data-templates-in-test-suite-spec)
  - [Supported HTTP Headers ](#supported-http-headers )
  - [Request Format Differences](#request-format-differences)

Following [Rest.li Test Suite Specification](/rest.li/test_suite), we've added test suites for Python and Java.
Based on these test suites, we've made the following compatibility matrices:

## Supported Resources

| Resource | Java | Python | Additional Information |
|--------|------|----------------|----------------|
| ActionSet | x | x | Python requires a top-level "namespace", which is optional according to Rest.li protocol |
| Association | x | | Python generates code using a template engine, which doesn’t support Association |
| Collection | x | x | Python requires a top-level "namespace", which is optional according to Rest.li protocol |
| Simple | x | x | Python requires a top-level "namespace", which is optional according to Rest.li protocol |


## Resource Keys and Parameters

| Key Feature | Java | Python | Additional Information |
|--------|-----|-----|----------------|
| Primitive&nbsp;Key | x | x | long |
| Complex&nbsp;Key&nbsp;(Simple Record and Union)| x |  |In Java, ComplexResourceKey is a map of complex keys and params. In Python, complex key is supported, but only as a record, not a map with params |
| Query&nbsp;Parameters | x | x | Integer, string, long, string array, message array, string map, primitive union, complex types union, optional string, url typeref |


## Supported Data Templates in Test Suite Spec

| Template | Java | Python | Additional Information |
|--------|------|------|----------------|
| Array&nbsp;of&nbsp;Maps | x | x | |
| Complex&nbsp;Types | x | x | |
| Defaults | x | x | Test spec provides a schema with default values. In Python, we cannot specify a default value for Fixed field because Python does not support Fixed in the same way as Java. |
| Enums | x | x | |
| Enum&nbsp;with&nbsp;Properties | x | x | |
| Fixed | x | | Python does not support Fixed in the same way as Java|
| Fixed5 | x | | Python does not support Fixed in the same way as Java|
| Fruits | x | x | |
| Includes | x | x | |
| Large&nbsp;Record | x | x | |
| Map&nbsp;of&nbsp;Ints| x | x | |
| MD5| x | x | |
| Message | x | x | | 
| Optionals| x | x | |
| Primitives | x | x | |
| Record&nbsp;with&nbsp;Properties | x | x | |
| Record&nbsp;with&nbsp;Typeref&nbsp;Field | x | x | |
| Time | x | x | |
| Type&nbsp;Defined&nbsp;Before&nbsp;Include | x | x | |
| Type&nbsp;Defined&nbsp;in&nbsp;Include | x | x | |
| Typeref&nbsp;Message | x | x | |
| Typerefs | x | x | Typerefs.pdsc may need a different name in Python to avoid a naming conflict. If the filename is not changed, Python testsuite will have a folder for typeref resources, such as collectionTyperef, and it will also generate typeref.py from Typerefs.pdsc. |
| Union&nbsp;of&nbsp;Complex Types | x | x | |
| Union&nbsp;of&nbsp;Primitives | x | x | |
| Union&nbsp;of&nbsp;Same&nbsp;Types | x | | Python does not support union of same types using aliases |
| Url | x | x | |


## Supported HTTP Headers 

| Header | Java | Python | Additional Information |
|--------|------|----------------|----------------|
| Content&#8209;Type:&nbsp;PSON | x | | Java's content type can be set to JSON, PSON, or any. In Python's restconstants.py, Content-Type is always set to "application/json". | 
| Accept | x | | Java can specify media accepted (e.g. JSON, or PSON). Python does not use an Accept header, so according to RFC, it is assumed all media types are accepted|
| User&#8209;Agent | | x | This header is optional as per RFC. Java does not use it, while Python uses it| 
| X&#8209;RestLi&#8209;Method | x | x | According to Rest.li protocol, X-RestLi-Method is only required for BATCH_CREATE and BATCH_PARTIAL_UPDATE. Java always includes it for all POST requests, and Python uses the header only when required.|

## Request Format Differences

| Request Feature | Java | Python |
|--------|------|----------------|
|Unfilled&nbsp;Optional&nbsp;ActionParams|If optional ActionParam is not set, it is omitted from the serialized data.|If optional ActionParam is not set, it is still explicitly set to null in the serialized body. For an example, see the ```actionset-multiple-inputs-no-optional``` test, in which Python Rest.li includes " 'optionalString': null" in the request.|
| Scheme&nbsp;and&nbsp;Host&nbsp;Components&nbsp;of&nbsp;URL | Request URL is relative, scheme and host can be configured in instantiating a RestClient | URL scheme is hard-coded to http in requesturlbuilders.py, and requests.models.py requires a host |

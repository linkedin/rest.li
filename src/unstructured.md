---
layout: get_started
title: Unstructured Data in Rest.li Quick Start
permalink: /start/unstructured
excerpt: This tutorial demonstrates how to serve unstructured binary data, such as Binary Large Object (BLOB), in a Rest.li server. It will show how to define a Rest.li resource that responds with fortune reports (in PDF format) for GET requests and how to consume the GET response from a HTTP client.
index: 1
---

#  Unstructured Data in Rest.li Quick Start

## Contents

 - [Introduction](#introduction)
 - [Serve Unstructured Data](#serve-unstructured-data)
 - [Consume Unstructured Data](#consume-unstructured-data)
 - [Recap](#recap)

## Introduction
This tutorial demonstrates how to serve unstructured binary data, such as Binary Large Object (BLOB), in a Rest.li server. It will show how to define a Rest.li resource that responds with fortune reports (in PDF format) for GET requests and how to consume the GET response from a HTTP client.

This tutorial assumes that you already have a working Rest.li server. Otherwise, follow the [Rest.li Quick Start Guide](/rest.li/get_started/quick_start) before you continue.

## Serve Unstructured Data
We start by defining a resource class on the server side by extending the provided CollectionUnstructuredDataResourceTemplate with the generic type of our resource key as String. Notice that, different from a regular Rest.li resource interface/template that also requires a value type, an unstructured data resource doesn’t require one. Next, we annotate the resource with @RestLiCollection and specify the required resource name and namespace:

```
@RestLiCollection(name = "fortuneReports", namespace = "com.example.fortune")
public class FortuneReportsResource extends CollectionUnstructuredDataResourceTemplate<String>
{
  @Override
  public void get(String key, @UnstructuredDataWriterParam UnstructuredDataWriter writer)
  {
    byte[] fortuneReportPDF = fortuneReportDB.get(key);  // Fetch the data from source
    writer.setContentType("application/pdf");            // Set the proper MIME content-type
    writer.getOutputStream().write(fortuneReportPDF);    // Output the data into response
  }
}
```

We then implement the GET method simply by overriding from the template class. We obtain the requested fortune report PDF from source (in bytes) and use the UnstructuredDataWriter instance given by the method parameter to return the response.

UnstructuredDataWriter provides a setter for the required Content-Type header and an OutputStream instance for writing the binary data that goes into the response content. The final response will then be sent to the client after the GET method is successfully returned.

## Consume Unstructured Data
The response wire format contains the Content-Type header and content payload exactly as they were specified in the GET method without any alterations from the Rest.li framework. This means the response can be consumed directly by any HTTP client with no special client-side handling required.

Example: Using the GET endpoint in a HTML anchor

```
<a src="d2://fortuneReports/1">
```

When the link is clicked, the browser receive the PDF response and render the PDF inline or as file-download depends on the resource implementation.

Example: Calling a local deployed GET endpoint with curl
```
$ curl -v http://localhost:1338/fortuneReports/1
...
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Length: 5
Content: <<< binary data of the PDF report >>>
```

Currently, the Rest.li client doesn’t have support for unstructured data resource. Request builders aren’t generated for unstructured data resources.


## Recap
As you can see, serving unstructured data in Rest.li is very easy. Defining a resource for unstructured data is similar to how you define a regular Rest.li resource for Records with one exception. Instead of returning records, you respond with writing the data to an OutputStream.

You can learn more about unstructured data support in [Rest.li User Guide](/rest.li/user_guide/restli_server).
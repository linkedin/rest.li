---
layout: guide
title: Rest.li-2.x-upgrade-instructions
permalink: /Rest_li-2_x-upgrade-instructions
---

## Contents

* [Introduction](#introduction)
* [Source Code Changes](#source-code-changes)
* [Protocol Changes](#protocol-changes)
* [Upgrade and Deployment Ordering](#upgrade-and-deployment-ordering)

## Introduction

Rest.li 2.0 introduces a new backwards incompatible URI format, as well as removes several APIs that have been marked as deprecated for quite some time. You can find details about these changes on our [user guide](user_guide/server_architecture) as well as our [protocol documentation](spec/protocol). This page documents the steps needed to upgrade from a 1.x release to a 2.x release.

Having issues with any of the steps listed below? Please [create an issue](https://github.com/linkedin/rest.li/issues) and someone from the Rest.li team will help you.

## Source Code Changes

Take these steps to make your source code compatible with Rest.li 2.x:

1. Upgrade your Rest.li dependency to the latest version of Rest.li 1.x.
2. Compile your code with deprecation warnings turned on. This is typically done by setting the `-Xlint:deprecation` flag when using `javac`. 
3. Fix all the deprecation warnings related to Rest.li APIs. Almost all APIs marked deprecated in Rest.li 1.x are removed in Rest.li 2.x. To do this, look at the Javadoc for the deprecated API to find out which non-deprecated method to use instead. For example - one of the APIs that we removing in Rest.li 2.x is `com.linkedin.restli.client.BatchRequest#getIdObjects`. If you look at the Javadoc for this method we have instructions on what to use instead.
4. Recompile your code with deprecation warnings turned on to make sure you've removed all deprecated Rest.li APIs. 
5. Upgrade your Rest.li dependency to the latest version of Rest.li 2.x.
6. Compile your code. If you removed all the deprecated Rest.li APIs in step 3, there should be no issues.

## Protocol Changes

For the most part the protocol changes are taken care off under the hood by Rest.li. As an application developer, you don't have to worry about the Rest.li 2.0 protocol changes.

However, if you are hard coding URLs, HTTP request/response bodies, or HTTP request/response headers in your code you will have to update these to the Rest.li 2.0 protocol format. Details of the protocol can be found on [this page](spec/protocol).

## Upgrade and Deployment Ordering

The basic rules for upgrading and deploying your clients and servers is as follows:

1. Upgrade your server to Rest.li 2.x using the instructions described above.
2. Deploy your server. **Since Rest.li servers running Rest.li 2.x can understand the 1.x protocol this is safe to do.**
3. Upgrade your client to Rest.li 2.x using the instructions described above.
4. Deploy your client. It should now start sending protocol 2 traffic.

If you have a complicated Rest.li call graph involving multiple Rest.li services, start the upgrade and deployment process at the leaf nodes of the graph (i.e. the Rest.li services that don't call other Rest.li services) and work your way backwards.
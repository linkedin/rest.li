---
layout: api_reference
title: Dynamic Discovery
permalink: /Dynamic-Discovery
excerpt: Dynamic Discovery (D2) is a framework for service discovery and load balancing.
---

# D2 - Dynamic Discovery

## Contents

* [What is D2](#what-is-d2)
* [Terminology](#terminology)
* [Core Architecture](#core-architecture)
* [Service Discovery](#service-discovery)
  * [ZooKeeper Stores](#zookeeper-stores)
* [Load Balancer](#load-balancer)
  * [Load Balancing Algorithms](#load-balancing-algorithms)
  * [Quarantine](#quarantine)
  * [Partitioning](#partitioning)
  * [Sticky Routing](#sticky-routing)
  * [Cluster Subsetting](#cluster-subsetting)
* [Client]
  * [Client Retry](#client-retry)
  * + more
* [Dark Cluster](#dark-cluster)

## What is D2

D2 (Dynamic Discovery) is a framework addressing service discovery and load balancing for communication between microservices.

Service discovery - D2 uses Zookeeper to store configurations and maintain a registry of available hosts for a service. Given a service name, D2 is able to discover all online hosts within that microservice group. Functionally speaking, D2 translates a URI like `d2://<my d2 service>` to another address like `http://myD2service.something.com:9520/someContextPath`.

Load balancing - D2 uses client-side, dynamic load balancing to ensure traffic is routed evenly to hosts in the downstream service group. D2 actively tracks the health of downstream hosts by looking at request metrics and uses that information to route traffic away from unhealthy hosts.

Rest.li uses D2 to help decouple a REST resource from the real address of the resource.  Note that D2 is an <b>optional</b> layer in the Rest.li framework. In fact, Rest.li client can communicate directly to Rest.li servers without D2. But D2 provides nice benefits like partitioning, load balancing, and many others.  
Likewise, D2 can also be used outside of the rest.li framework because in essence D2 is just a name service and a load balancer.

## Terminology

- <b>Service</b>: Represents a REST/Rest.li resource, an endpoint, a thing that you want to interact with. The name of a service must be unique.
- <b>Cluster</b>: Represents a set of Services. A service can belong to exactly one cluster only.
- <b>URI</b>: Represents a machine that belongs to a Cluster and is able to serve all the Services in the Cluster. A physical representation of an instance of a host within a Cluster.

## Core Architecture

Here's a very simplified, top level illustration describing the core architecture of D2. 

<center><img src="{{ 'assets/images/D2-Top-Graph.png' | relative_url }}"></center>

There are two sides to D2 framework which are used in both client and server side:

1. Server side: You have a service, and you want to make it discoverable by other clients and communicate with your service via D2. D2 uses ZooKeeper to store D2 data such as cluster/service configurations and the set of available hosts able to serve each cluster.
2. Client side: You want to send a request to a service that is available in D2. D2 will discover the set of available hosts for that service and route to a host within that set based on a load balancing algorithm.

In the following sections we will dive further in depth into each major component of D2.

## Service Discovery

Let's start with the ZooKeeper data layout. All data is stored within three directories: /clusters, /services, /uris. Here is simplified illustration of how data is organized within ZooKeeper that captures the high level concepts. 

<center><img src="{{ 'assets/images/zookeeper-layout.png' | relative_url }}"></center>

All the metadata needed for D2 to function are stored in zookeeper. These include:

- What Services (Resources) are available
- What Clusters (Collection of services) are available
- What URI (machines) belongs to which cluster
- Mappings of service to cluster to URIs
- What are the properties and configuration for services/clusters for example: how long is time out, what kind load balancer strategy should a cluster use, etc.

/clusters and /services store cluster and service configurations in zookeeper nodes. They define what services are discoverable in the D2 ecosystem and the clusters that they belong to. 

To learn more about the configurable properties refer to the property schemas: 

[Cluster Properties](https://github.com/linkedin/rest.li/blob/master/d2-schemas/src/main/pegasus/com/linkedin/d2/D2Cluster.pdl)
[Service Properties](https://github.com/linkedin/rest.li/blob/master/d2-schemas/src/main/pegasus/com/linkedin/d2/D2Service.pdl)

The /uris directory contains a directory for each cluster in /clusters. Within each cluster directory we store an ephemeral node for each active host in the cluster able to serve that traffic. The ephemeral note contains the URI of the machine. These znodes exists as long as the session that created the znode is active. When the session ends the znode is deleted. D2 maintains a heartbeat connection with ZK to keep the node alive. When servers are started, they will announce to the corresponding cluster directory their URI and create the ephemeral node. When they are shut down, they will de-announce and remove the node.

D2 clients use the information in /uris directory to discover the active hosts within a cluster. They will first read the service properties for the cluster name that the service belongs to. Then it will read the /uris/{cluster_they_are_interested_in}/ directory for the list of hosts which are able to serve their traffic. Clients have watches set on the properties they are interested in so they can receive updates.

### ZooKeeper Stores

In D2, a store is a way to get/put/delete properties.

D2 contains two main ZooKeeper implementations for service discovery. The first is the ZooKeeperPermanentStore. This store operates by attaching listeners to a file in ZooKeeper. Every time the file is updated, the listeners are notified of the property change. The second is the ZooKeeperEphemeralStore. This store operates by attaching listeners to a ZooKeeper directory, and putting sequential ephemeral nodes inside of the directory. The ZooKeeperEphemeralStore is provided with a "merger" that merges all ephemeral nodes into a single property. Whenever a node is added or removed to the directory, the ZooKeeperEphemeralStore re-merges all nodes in the directory, and sends them to all listeners.

D2 uses the permanent store for cluster and service properties and the ephemeral store for URI properties.

<h4> File System </h4>

As explained above, D2 uses zookeeper to keep track of the state of the clusters that it needs to talk to. More precisely, D2's load balancer needs to keep track of the state because it needs to know how many servers are in the cluster, what are their address so it can route requests to them. All these information is in zookeeper. But what if zookeeper is down or there's a connection problem to zookeeper?

It's important that the load balancer still works even if zookeeper is down. In order to do this, we provide a backup store using file system. So D2 LoadBalancer has a listener that gets informed when zookeeper's state changes:
  
1. If the state is SyncConnected then we use zookeeper store.
2. If the state is changed to Disconnected then we enable file system store. The file system store is also listening to zookeeper store for updates. So it knows what's the latest state before zookeeper gets disconnected.
3. If the state is changed to Expired. This means we can no longer trust the information from backup store (nor the zookeeper store). So we should discard all known information, shut-down all http transport clients that we already created and retry connecting to zookeeper. We will retry connecting to zookeeper a couple of times (See RetryZookeeper.java)
4. Once the zookeeper is restored, load balancer will receive the event that makes the state to be SyncConnected again. From there we'll reconstruct our state and re-initialize our transport clients.

The file system implementation simply uses a directory on the local filesystem to manage property updates. When a property is updated, a file is written to disk with the property's key. For instance, putting a property with name "foo" would create /file/system/path/foo, and store the serialized property data in it. The File System will then alert all listeners of the update.

<h4> Toggling </h4>

The toggling store that wraps another PropertyStore. The purpose of the toggling store is to allow a store to be "toggled off". By toggling a store off, all future put/get/removes will be ignored. The reason that this class is useful is because we want to toggle the ZooKeeper stores off if connectivity is lost with the ZooKeeperCluster (until a human being can verify the state of the cluster, and re-enable connectivity).

<center><img src="{{ 'assets/images/LoadBalancer.png' | relative_url }}"></center>

## Load Balancer

### Load Balancing Algorithms

### Quarantine

### Partitioning

### Sticky Routing

### Cluster Subsetting

## Client

### Client Retry

### Dark Cluster


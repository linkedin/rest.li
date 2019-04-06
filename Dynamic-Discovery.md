---
layout: api_reference
title: Dynamic Discovery (D2)
permalink: /Dynamic_Discovery
excerpt: Dynamic Discovery (D2) is a layer of indirection similar to DNS for the rest.li framework.
---

# Dynamic Discovery (D2)

## Contents

* [What is D2](#what-is-d2)
* [Terminology](#terminology)
* [D2 and Zookeeper](#d2-and-zookeeper)
* [D2 Load Balancer](#d2-load-balancer)
* [Stores](#stores)
* [Operations](#operations)

## What is D2

<p>
    Dynamic Discovery (D2) is a layer of indirection similar to DNS for the rest.li framework.
    Functionally speaking, D2 translates a URI like <em>d2://&lt;my d2 service&gt;</em> to another address like
    <em>http://myD2service.something.com:9520/someContextPath</em>.
</p>
<p>
    Rest.li uses D2 to help decouple a REST resource from the real address of the resource.
    D2 also acts as a client side load balancer. </p>

<p> Note that D2 is an <b>optional</b> layer in the rest.li framework.
    In fact, rest.li client can communicate directly to rest.li servers without D2.
    But D2 provides nice benefits like partitioning, load balancing, and many others.
    Vice versa, <b>D2 can also be used outside of the rest.li framework</b>.
    Because in essence D2 is just a name service and a load balancer.
</p>
<p>
    If you just need a quick working tutorial for D2 please refer to
    <a href='/rest.li/start/d2_quick_start'>Dynamic Discovery Quickstart</a>
</p>

## Terminology

<ul>
<li><b>Service</b>: Represents a REST resource, an endpoint, a thing that you want to interact with. The name of a service must be unique. </li>
<li><b>Cluster</b>: Represents a set of Services. A service must belong to exactly one cluster only.</li>
<li><b>URI</b>: Represent a machine that belongs to a cluster. A physical representation of a cluster.</li>
</ul>

<p>
    As we said above, D2 works like DNS. It translates a <em>D2 URI</em> to a real address.
    D2 works by keeping the state in Zookeeper. We chose Zookeeper because it's distributed
    and fault tolerant.
</p>
<p>
    When a client is about to send a request, the D2 client library extract the <em>service</em> name from the d2 URI.
    Then the d2 client library queries zookeeper for the <em>cluster</em> that owns that <em>service</em>.
    Once d2 client know the cluster, it will then queries zookeeper for the available <em>URIs</em> for that cluster.

    Given a list of URIs, D2 client can select which URI to send the request to.
    The D2 client will listen for any updates related to the cluster and service it previously contacted. So if there's
    any changes happening either because the server membership changes, or there are new services in a cluster,
    the d2 client can pick up the changes immediately
</p>
<p>
    Sometimes D2 client's connection to zookeeper might be interrupted. When this happens, D2 will not know
    what is the latest state so it will assume the state is the same as before. D2 keep backup of the state
    in the filesystem. If the zookeeper connection interruption happened for a long period of time (configurable), D2
    will discard the state and will fail to work.
</p>

## D2 and Zookeeper

<p>
    Running D2 requires a Zookeeper ensemble running somewhere. Please download <a href="http://zookeeper.apache.org/">Zookeeper</a>
    if you don't have one yet.
</p>
<p>

</p>


## D2 Load Balancer

As we said above, all the load balancing happened in the client side. D2 client keep tracks of the health of the cluster.

There are 2 types of mode that we can use to load balance traffic.
<ul>
    <li> Shifting traffic away from one server to the other a.k.a LOAD_BALANCING </li>
    <li> Dropping request on the floor a.k.a CALL_DROPPING. </li>
</ul>
We aim to alternate between these 2 modes but it's not always guaranteed.

So how do we choose between CALL_DROPPING and LOAD_BALANCING?

We measure 2 different things for health. One is the *cluster* health and the other one is the *client* health.

For cluster health, we only measure the average cluster latency. If the average cluster latency is higher than LoadBalancer's high water mark, we'll increment the drop rate by 20%. Drop rate means all traffic to this cluster will be dropped 20% of the time. So obviously cluster health is relevant only to CALL_DROPPING mode. If the cluster latency exceeds high water mark 5 times in a row, we'll reach 100% drop rate. We have some measure of "recovery mode" to prevent the cluster from getting stuck in perpetual "drop everything" mode. During this mode, we'll still allow traffic to pass by to calibrate our cluster latency once in a while.

On the other hand, client health is tracked per client. We tracked many things per client e.g. error rate, number of calls, latency of calls, etc. We use this measurement to compute the "computed drop rate" of the client. Healthy client is a client whose latency is lower than client's high water mark (NOTE that there's client's high water mark and there's also load balancer's high water mark). For healthy client the computed drop rate should be 0. The computed drop is inversely proportional to the number of virtual points the client gets in a hash ring.

The points are used to distribute traffic amongst many clients. For example there are 4 clients for service "widget". In perfect condition, each client would have 100 points (this is configurable in service properties). So total points in the hash ring would be 400. If one client's latency becomes higher than water mark, the computed drop rate will change then the number of points of that client maybe reduced to 80. So that client will receive less traffic and the other servers will get the remaining traffic.

We try to alternate between CALL_DROPPING and LOAD_BALANCING mode. The logic for doing this alternation happens in Load Balancer Strategy.

<h3> Implementation of the client load balancer </h3>

Here are the moving components that you should know about load balancer:
<ul>
<li>Properties (ServiceProperties, ClusterProperties and URIProperties). For load balancing purposes we only care about ServiceProperties. Because that's where all the important parameters, like percent error rate, minimum call count, average latency that we can configure to tell whether a service is in "bad state", are located.</li>
<li> Clients: rest.li client wraps a client (which we know is a d2 client). The implementation of that client is called DynamicClient. DynamicClient has a LoadBalancer and a wrapper over many simpler R2 clients to send requests.</li>
<li> LoadBalancer: LoadBalancer figures out the following question: "given a resource name, tell me who can serve the resource, then get all the clients that can send bytes to the servers, then choose one according to the load balancer strategy logic".</li>
<li> LoadBalancerStrategies: The strategy is used by LoadBalancer to pick one client among many potential clients. The interface takes a list of TrackerClients and choose one to return.</li>
</ul>
In the following section we'll elaborate more of each component:

<h3> Properties </h3>

D2 uses a hierarchy of properties to model the system:
<ul>
<li> ServiceProperties </li>
<li> ClusterProperties </li>
<li> UriProperties </li>
</ul>

<h4> ServiceProperties</h4>

Like its name, ServiceProperties defines anything related to a service. The most important one is the load balancer strategy properties. For example: we can set the highWaterMark and lowWaterMark for this service. If the average latency of all the servers that serves this resource is higher than highWaterMark we'll start dropping calls because we know the servers are in a "degraded" state.

<h4> ClusterProperties</h4>

ClusterProperties define's a cluster's name, partioning, preferred schemes, banned nodes, and connection properties.
<ul>
<li> The schemes are defined in their priority. That is, if a cluster supports both HTTP and Spring RPC, for instance, the order of the schemes defines in which order the load balancer will try to find a client. </li>
<li> The banned nodes are a list of nodes (URIs) that belong to the cluster, but should not be called. </li>
<li> The connection properties describe things like what is the maximum size of a response. What is the time out value of an http connection and so on. </li>
(Note that we are migrating connection properties to ServiceProperties. This allows users to have a more fine-grained configuration. We estimate the code change will be released in version 1.8.13 and above)
</ul>

<h4> UriProperties </h4>

UriProperties define a cluster name and asset of URIs associated with the cluster. Each URI is also given a weight, which will be passed to the load balancer strategy.

<h3> D2 Client: what are D2 Client anyway? </h3>

D2 Client is a wrapper over other simpler clients. The real implementation of D2 Client is DynamicClient.java. But underneath we use R2 client to shove bits from client to server. So DynamicClient wraps r2 clients with three classes: TrackerClient, RewriteClient, and LazyClient. The underlying R2 clients are: HttpNettyClient, FilterChainClient and FactoryClient.

<h4> TrackerClient </h4>

The TrackerClient attaches a CallTracker and Degrader to a URI. When a call is made to this client, it will use call tracker to track it, and then forward all calls to the r2 client. CallTracker keeps track of call statistics like call count, error count, latency, etc.

<h4> RewriteClient </h4>

The RewriteClient simply rewrites URIs from the URN style to a URL style. For example, it will rewrite "urn:MyService:/getWidget" to "http://hostname:port/my-service/widgets/getWidget".

<h4> LazyClient </h4>

The LazyClient is just a wrapper that does not actually create an r2 client until the first rest/rpc request is made.

<h4> Client Wrapper Diagram </h4>

<center><img src="/rest.li/images/TransportClient.png"></center>

<h3> LoadBalancer </h3>

There is currently one "true" implementation of a LoadBalancer in com.linkedin.d2.balancer. This implementation is called <b>SimpleLoadBalancer</b>. There are other implementations of LoadBalancer that <b>will wrap</b> this SimpleLoadBalancer for example: ZKFSLoadBalancer. In any case, the simple load balancer contains one important method: getClient. The getClient method is called with a URN such as "urn:MyService:/getWidget". The responsibility of the load balancer is to return a client that can handle the request, if one is available, or to throw a ServiceUnavailableException, if no client is available.

When getClient is called on the simple load balancer, it:
        <ul>
            <li>First tries to extract the service name from the URI that was provided. </li>
            <li> It then makes sure that it's listening to that service in the LoadBalancerState. </li>
            <li> It then makes sure that it's listening to the service's cluster in the LoadBalancerState </li>
            <li> If either the service or cluster is unknown, it will throw a ServiceUnavailableException. </li>
            <li> It will then iterate through the prioritized schemes (prpc, http, etc) for the cluster.   </li>
            <li> For each scheme, it will get all URIs in the service's cluster for that scheme, and ask the service's load balancer strategy to load balance them.</li>
            <li> If the load balancer strategy returns a client, it will be returned, otherwise the next scheme will be tried.</li>
            <li>If all schemes are exhausted, and no client was found, a ServiceUnavailableException will be thrown. </li>
        </ul>

<h3> Strategies </h3>

Load balancer strategies have one responsibility. Given a list of TrackerClients for a cluster, return one that can be used to make a service call. There are currently two implementations of load balancer strategies: random and degrader.

<h4> Random </h4>

The random load balancer strategy simply chooses a random tracker client from the list that it is given. If the list is empty, it returns null. This is the default behavior for dev environment. Because in development environments, one may wish to use the same machine for every service. so with this strategy, we will always return the "dev" tracker client to route the request (and prevent confusion).

<h4> Degrader </h4>

The load balancer strategy that attempts to do degradation is the DegraderLoadBalancerStrategy. Here are some facts about the degrader strategy:
        <ul>
<li> Each node in a cluster (TrackerClient) has an associated CallTracker and Degrader.</li>
<li> The CallTracker tracks things like latency, number of exceptions, number of calls, etc for a given URI endpoint in the cluster.</li>
<li> The Degrader uses the CallTracker to try and figure out whether to drop traffic, how much traffic to drop, the health of the node, etc. This is boiled down to a "drop rate" score between 0 and 1. </li>
<li> If the cluster's average latency per node is less than the max cluster latency, all calls will go through. The probability of selecting a node in the cluster will depend on its computed drop rate (nodes with lower drop rates will be weighted higher), but no messages will be dropped.</li>
<li> If the cluster's average latency per node is greater than the max cluster latency, the balancer will begin allowing the nodes to drop traffic (using the degrader's checkDrop method).</li>
        

<h4> Partitioning </h4>

D2 currently support range-based and hash-based partitioning.

<h3> Load Balancer Flow </h3>

<center><img src="/rest.li/images/LoadBalancerFlow.png"></center>

Here is an example of the code flow when a request comes in. For the sake of this example, we'll a fictional widget service. Let's also say that in order to get the data for a widget resource, we need to contact 3 different services: WidgetX, WidgetY, and WidgetZ backend.

On the server side:

<li> When a machine joins a *cluster*, let's say we add a new machine to Widget Server Cluster. Let's say that is machine number #24. Then discovery server code in machine #24 will "announce" to D2 zookeeper that there is another machine joining the widget server cluster. </li>
<li> It will tell zookeeper about the machine #24 *URI*. </li>
<li> All the "listeners" for "widget server" *service* will be notified (these are all the clients for example widget front-end) and since the load balancer client side has the load balancing strategy, the client will determine which machine gets the new request. </li>

On the client side:

<li> A request comes to @http://example.com/widget/1@  </li>
<li> The HTTP load balancer knows that /widget/ is redirected to widget service (this is not the D2 load balancer) </li>
<li> One of the machines in widget front-end gets the request and processes it. </li>
<li> Since there's D2 client code in every war and the D2 client code is connected to D2 zookeeper, the client code knows how to load balance the request and choose the machine for each service needed to construct the returned data. </li>
<li> In this case we assume that widget front-end needs a resource from WidgetX, WidgetY and WidgetZ backend. So the D2 client code in widget front-end is listening to these 3 services in zookeeper.  </li>
<li> In the example, the D2 client code in widget front-end chooses machine #14 for WidgetX backend, machine #5 for WidgetY and machine #33 for WidgetZ backend. </li>
<li> Then the requests get dispersed to each corresponding machine. </li>

## Stores

In D2, a store is a way to get/put/delete properties.

<h3> Store Type </h3>

<h4> ZooKeeper </h4>

D2 contains two ZooKeeper implementations of DynamicDiscovery. The first is the ZooKeeperPermanentStore. This store operates by attaching listeners to a file in ZooKeeper. Every time the file is updated, the listeners are notified of the property change. The second is the ZooKeeperEphemeralStore. This store operates by attaching listeners to a ZooKeeper directory, and putting sequential ephemeral nodes inside of the directory. The ZooKeeperEphemeralStore is provided with a "merger" that merges all ephemeral nodes into a single property. Whenever a node is added or removed to the directory, the ZooKeeperEphemeralStore re-merges all nodes in the directory, and sends them to all listeners.

In the software load balancing system, the permanent store is used for cluster and service properties, while the ephemeral store is used for URI properties.

<h4> File System</h4>

The file system implementation simply uses a directory on the local filesystem to manage property updates. When a property is updated, a file is written to disk with the property's key. For instance, putting a property with name "foo" would create /file/system/path/foo, and store the serialized property data in it. The File System will then alert all listeners of the update.

<h4> In-Memory </h4>

The in-memory implementation of Dynamic Discovery just uses a HashMap to store properties by key. Whenever a store is put/delete occurs, the HashMap is updated, and the listeners are notified.

<h4> Toggling </h4>

The toggling store that wraps another PropertyStore. The purpose of the toggling store is to allow a store to be "toggled off". By toggling a store off, all future put/get/removes will be ignored. The reason that this class is useful is because LinkedIn wants to toggle the ZooKeeper stores off if connectivity is lost with the ZooKeeperCluster (until a human being can verify the state of the cluster, and re-enable connectivity).

<h3> Registries </h3>

In D2, a registry is a way to listen for properties. Registries allow you to register/unregister on a given channel. Most stores also implement the registry interface. Thus, if you're interested in updates for a given channel, you would register with the store, and every time a put/delete is made, the store will update the listeners for that channel.

<h3> Messengers  </h3>

By default, none of the stores in Dynamic Discovery are thread safe. To make the stores thread safe, a PropertyStoreMessenger can be used. The messenger is basically a wrapper around a store that forces all writes to go through a single thread. Reads still happen synchronously.

<center><img src="/rest.li/images/LoadBalancer.png"></center>


## Operations

<h3>Populating Zookeeper</h3>

<h3>Deleting zookeeper nodes</h3>
<h3>JMX</h3>
<p>
We have beans in the com.linkedin.d2 JMX name space.
</p>
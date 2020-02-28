---
layout: api_reference
title: D2 Zookeeper configuration properties
permalink: /D2-Zookeeper-Properties
excerpt: D2 Zookeeper configuration properties
---

# D2 Zookeeper Configuration Properties

## Contents

* [Tiers of Configuration](#tiers-of-configuration)
* [Cluster Level Properties](#cluster-level-properties)
* [partitionProperties Level Properties](#partitionproperties-level-properties)
* [Service Level Properties](#service-level-properties)
* [transportClient Level Properties](#transportclient-level-properties)
* [degraderProperties Level Properties](#degraderproperties-level-properties)
* [loadBalancerStrategy Level Properties](#loadbalancerstrategy-level-properties)

## Tiers of Configuration

<p>
    There are tiers of configuration in D2. This is how we structure our
    configuration.
    <ul>
      <li>List of all clusters</li>
        <ul>
            <li><b>cluster A</b></li>
            <ul>
                <li><em>cluster level configuration (see below for more info)</em></li>
                <li>services (all the services that belong to cluster A)</li>
                  <ul>
                      <li><em><b>service A-1</b></em></li>
                      <ul>
                          <li><em>service level properties (see below for more info)</em></li>
                          <li>loadBalancerStrategyProperties</li>
                            <ul>
                                <li><em>"loadBalancerStrategy" level properties</em></li>
                                <li>http.loadBalancer.updateIntervalMs</li>
                                <li>http.loadBalancer.globalStepDown</li>
                                <li>other load balancer properties</li>
                            </ul>
                          <li>transportClientProperties</li>
                          <ul>
                              <li><em>"transportClient" level properties</em></li>
                              <li>http.maxResponseSize</li>
                              <li>http.shutdownTimeout</li>
                              <li>other transport client properties</li>
                          </ul>
                          <li>degraderProperties</li>
                          <ul>
                              <li><em>"degraderProperties" level properties</em></li>
                              <li>degrader.lowLatency</li>
                              <li>degrader.maxDropDuration</li>
                              <li>other degrader properties</li>
                          </ul>
                      </ul>
                      <li><em><b>service A-2</b></em></li>
                      <ul>
                          <li><em>service level properties</em></li>
                      </ul>
                      <li><em><b>other services under cluster A</b></em></li>
                  </ul>
                <li>partitionProperties for cluster A</li>
                <ul>
                    <li><em>"partitionProperties" level properties</em></li>
                    <li>partitionType</li>
                    <li>partitionKeyRegex</li>
                    <li>other partitionProperties level properties</li>
                </ul>
            </ul>
            <li><b>cluster B</b></li>
            <ul>
                <li>partitionProperties for cluster B (optional)</li>
                <ul>
                    <li>etc</li>
                </ul>
                <li>services (all the services that belong to cluster B)</li>
                <ul>
                    <li><em><b>service B-1</b></em></li>
                    <li><em><b>service B-2</b></em></li>
                    <li>etc</li>
                </ul>
            </ul>
            <li><b>cluster C</b></li>
            <li><b>cluster D</b></li>
            <li>etc</li>
        </ul>
    </ul>
</p>
<p>
As you can see, there are multiple tiers for configuration. Next we'll enumerate all the
levels and the configurations that belong to that level.
</p>

## Cluster Level Properties
<table>
<tbody>
<tr>
    <th>
        <p>Property Name</p>
    </th>
    <th>
        <p>Description</p>
    </th>
</tr>
<tr>
    <td>
        <p>partitionProperties</p>
    </td>
    <td>
        <p>A map containing all the properties to partition the cluster. (See below for more details) </p>
    </td>
</tr>
<tr>
    <td>
        <p>services</p>
    </td>
    <td>
        <p>A list of d2 services that belong to this cluster.</p>
    </td>
</tr>
</tbody>
</table>

## partitionProperties Level Properties

<table>
<tbody>
<tr>
    <th>
        <p>Property Name</p>
    </th>
    <th>
        <p>Description</p>
    </th>
</tr>
<tr>
    <td>
        <p>partitionType</p>
    </td>
    <td>
        <p>The type partitioning your cluster use. Valid values are RANGE and HASH.</p>
    </td>
</tr>
<tr>
    <td>
        <p>partitionKeyRegex</p>
    </td>
    <td>
        <p>The regex pattern used to extract key out of URI.</p>
    </td>
</tr>
<tr>
    <td>
        <p>partitionSize</p>
    </td>
    <td>
        <p>Only if you choose partitionType RANGE. The size of the partition i.e. what the is the size of the RANGE in one partition</p>
    </td>
</tr>
<tr>
    <td>
        <p>partitionCount</p>
    </td>
    <td>
        <p>How many partition in the clusters</p>
    </td>
</tr>
<tr>
    <td>
        <p>keyRangeStart</p>
    </td>
    <td>
        <p>Only if you choose partitionType RANGE. This is the number where the key starts. Normally we start at 0. </p>
    </td>
</tr>
<tr>
    <td>
        <p>hashAlgorithm</p>
    </td>
    <td>
        <p>Only if you choose partitionType HASH. You have to give the type of hash. Valid values are MODULE and MD5.</p>
    </td>
</tr>
</tbody>
</table>

## Service Level Properties

<table>
<tbody>
<tr>
    <th>
        <p>Property Name</p>
    </th>
    <th>
        <p>Description</p>
    </th>
</tr>
<tr>
    <td>
        <p>loadBalancerStrategyList</p>
    </td>
    <td>
        <p>The list of Strategies that you want to use in your LoadBalancer. Valid values are random, degraderV2, degraderV3. Only degraderV3 support partitioning.
        Random load balancer just choose any random server to send the request to. So you can't do sticky routing if you choose random load balancer. </p>
    </td>
</tr>
<tr>
    <td>
        <p>path</p>
    </td>
    <td>
        <p>The context path of your service</p>
    </td>
</tr>
<tr>
    <td>
        <p>loadBalancerStrategyProperties</p>
    </td>
    <td>
        <p>The properties of D2 LoadBalancer.</p>
    </td>
</tr>
<tr>
    <td>
        <p>transportClientProperties</p>
    </td>
    <td>
        <p>A map of all properties related on the creation transport client </p>
    </td>
</tr>
<tr>
    <td>
        <p>degraderProperties</p>
    </td>
    <td>
        <p>Properties of D2 Degrader.
          Basically it's a map of all properties related to how D2 perceives a single
            server's health so D2 can redirect traffic to healthier server. Contrast this
        to LoadBalancer properties which is used to determine the health of the entire cluster.
        The difference is, if the health of cluster deteriorate, d2 will start dropping
        requests instead of redirecting traffic.</p>
    </td>
</tr>
<tr>
    <td>
        <p>banned</p>
    </td>
    <td>
        <p>A list of all the servers that shouldn't be used. </p>
    </td>
</tr>
</tbody>
</table>

## transportClient Level Properties

Properties used to create a client to talk to a server.

<table>
    <tbody>
    <tr>
        <th>
            <p>Property Name</p>
        </th>
        <th>
            <p>Description</p>
        </th>
    </tr>
<tr>
    <td>
        <p>http.queryPostThreshold</p>
    </td>
    <td>
        <p>The max length of a URL before we convert GET into POST because the server buffer header size maybe limited. Default is Integer.MAX_VALUE (a.k.a not enabled).</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.poolSize</p>
    </td>
    <td>
        <p>Maximum size of the underlying HTTP connection pool. Default is 200.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.requestTimeout</p>
    </td>
    <td>
        <p>Timeout, in ms, to get a connection from the pool or create one, send the request, and receive a response (if applicable). Default is 10000.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.idleTimeout</p>
    </td>
    <td>
        <p>Interval, in ms, after which idle connections will be automatically closed. Default is 25000.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.shutdownTimeout</p>
    </td>
    <td>
        <p>Timeout, in ms, the client should wait after shutdown is initiated before terminating outstanding requests. Default is 10000.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.maxResponseSize</p>
    </td>
    <td>
        <p>Maximum response size, in bytes, that the client can process. Default is 2 MB. </p>
    </td>
</tr>
</tbody>
</table>

## degraderProperties Level Properties

Note that each degrader is used to represent a server among many servers in a cluster.

<table>
<tbody>
<tr>
    <th>
        <p>Property Name</p>
    </th>
    <th>
        <p>Description</p>
    </th>
</tr>
<tr>
    <td>
        <p>degrader.name</p>
    </td>
    <td>
        <p>Name that will show up in the logs (make debugging easier)</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.logEnabled</p>
    </td>
    <td>
        <p>Whether or not logging is enabled in degrader</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.latencyToUse</p>
    </td>
    <td>
        <p>What kind of latency to use for our calculation. We support AVERAGE (default), PCT50, PCT90, PCT95, PCT99</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.overrideDropDate</p>
    </td>
    <td>
        <p>What fraction of the call should be dropped. A value larger than 0 means this client will permanenty drop that fraction of the calls. Default is -1.0.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.maxDropRate</p>
    </td>
    <td>
        <p>The maximum fraction of calls that can be dropped. A value of greater or equal than 0 and less than 1
            means we cannot degrade the client to drop all calls if necessary. Default is 1.0.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.maxDropDuration</p>
    </td>
    <td>
        <p>The maximum duration, in ms, that is allowed when all requests are dropped. For example if maxDropDuration is 1 min and the last request that should not
        be dropped is older than 1 min, then the next request should not be dropped. Default is 60000.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.upStep</p>
    </td>
    <td>
        <p>The drop rate incremental step every time a degrader crosses the high water mark. Default is 0.2.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.downStep</p>
    </td>
    <td>
        <p>The drop rate decremental step every time a degrader recover below the low water mark. Default is 0.2.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.minCallCount</p>
    </td>
    <td>
        <p>The minimum number of calls needed before we use the tracker statistics to determine whether a client is healthy or not. Default is 5.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.highLatency</p>
    </td>
    <td>
        <p>If the latency of the client exceeds this value then we'll increment the computed drop rate. The higher the computed drop rate,
            the less the traffic that will go to this server. Default is 3000.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.lowLatency</p>
    </td>
    <td>
        <p>If the latency of the client is less than this value then we'll decrement the computed drop rate. The lower the computed drop rate,
        the more the traffic will go to this server. Default is 500</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.highErrorRate</p>
    </td>
    <td>
        <p>If the error rate is higher than this value then we'll increment the computed drop rate which cause less traffic to this server. </p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.lowErrorRate</p>
    </td>
    <td>
        <p>If the error rate is lower that this value then we'll decrement the computed drop rate which in turn will cause
        more traffic to this server.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.highOutstanding</p>
    </td>
    <td>
        <p>If the number of outstanding call is higher than this value then we'll increment the computed drop rate
        which causes less traffic to this server. Default is 10000.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.lowOutstanding</p>
    </td>
    <td>
        <p>If the number of outstanding call is lower than this value then we'll decrement the computed drop rate
            which causes more traffic to this server. Default is 500.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.minOutstandingCount</p>
    </td>
    <td>
        <p>The number of outstanding calls sohuld be greater or equal than this value for the degrader to use
            the average outstanding latency to determine if high and low watermark condition has been met.
            High and low water mark conditions are any of these: errorRate, latency and outstandingCount. Default is 5.</p>
    </td>
</tr>
<tr>
    <td>
        <p>degrader.overrideMinCallCount</p>
    </td>
    <td>
        <p>If overriden, we will use this value as the minimum number of calls needed before we compute drop rate. Default is -1.</p>
    </td>
</tr>
</tbody>
</table>

## loadBalancerStrategy Level Properties

Properties for load balancers. This affects all servers in a cluster.

<table>
<tbody>
    <tr>
        <th>
            <p>Property Name</p>
        </th>
        <th>
            <p>Description</p>
        </th>
    </tr>
    
<tr>
    <td>
        <p>http.loadBalancer.hashMethod</p>
    </td>
    <td>
        <p>What kind of hash method we should use (this is relevant to stickiness). Valid values are none or uriRegex </p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.hashConfig</p>
    </td>
    <td>
        <p>If you declare this, you need to define the regexes list that we need to use to parse the URL</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.updateIntervalMs</p>
    </td>
    <td>
        <p>Time interval that the load balancer will update the state (meaning should load balancer, rebalance the traffic, should it increase the drop rate, etc). Default value is 5000.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.pointsPerWeight</p>
    </td>
    <td>
        <p>The max number of points a client get in a hashring per 1.0 of weight. Default is 100. Increasing this number will increase the computation needed to create a hashring but lead to more even-ness in the hashring.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.lowWaterMark</p>
    </td>
    <td>
        <p>If the cluster average latency, in ms, is lower than this, we'll reduce the entire cluster drop rate. (This will affect all the clients in the same cluster regardless whether they are healthy or not). Default value is 500. </p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.highWaterMark</p>
    </td>
    <td>
        <p>If the cluster average latency is higher than this, in ms, we'll increase the cluster drop rate.(This will affect all the clients in the same cluster regardless whether they are healthy or not). Default value is 3000.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.initialRecoveryLevel</p>
    </td>
    <td>
        <p>Once a cluster gets totally degraded, this is the baseline that the cluster use to start recovering. Let's say a healthy client has 100 points in a hashring. At a complete degraded state, it has 0 point. Let's say the initial recovery level is 0.005, that means the client get 0.5 point not enough to be reintroduced (because a client need at least 1 point). Default value is 0.01.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.ringRampFactor</p>
    </td>
    <td>
        <p>Once a cluster is in the recovery mode, this is the multiplication factor that we use to increase the number of point for a client in the ring. For example: a healthy client has 100 points in a hashring. It's completely degraded now with 0 points. The initialRecoveryLevel is set to 0.005 and ringRampFactor is set to 2. So during the #1 turn of recovery we get 0.5 point. Not enough to be reintroduced into the ring. But at #2 turn, because ringRampFactor is 2, then we get 1 point. Turn #3 we get 2 points, etc. Default value is 1.</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.globalStepUp</p>
    </td>
    <td>
        <p>The size of step function when incrementing drop rate in the cluster. Default value is 0.2. Example if globalStepUp = 0.2 <br class="atl-forced-newline"/> drop rate is 0.0 then becomes 0.2 then becomes 0.4 etc as the cluster gets more degraded</p>
    </td>
</tr>
<tr>
    <td>
        <p>http.loadBalancer.globalStepDown</p>
    </td>
    <td>
        <p>Same as http.loadBalancer.globalStepUp except this is for decrementing drop rate</p>
    </td>
</tr>
</tbody>
</table>



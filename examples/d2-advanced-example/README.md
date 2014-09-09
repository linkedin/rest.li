This is a standalone example that demonstrates how D2 client-server works. For simple
d2 tutorial please refer to the d2-quickstart example. This example is geared toward the
more advanced features of D2.

=========================
The topology :

In this example we have 4 D2 Clusters:
1.) ProfileService Cluster
2.) EmailService Cluster
3.) BigDataService Cluster
4.) CacheService Cluster

Each cluster is used to demonstrate different D2 capabilities.
We use ProfileService to demonstrate D2 partitioning and load balancing.
We use EmailService to demonstrate assigning different weights per server.
We use BigDataService to demonstrate client override config.
We use CacheService to demonstrate D2 sticky ("soft affinity") routing.

We have 6 D2 Services:
1.) member Service
2.) contact Service
3.) connection Service
4.) inbox Service
5.) compute Service
6.) cache service

"member", "contact", "connection" belong to ProfileService. "inbox" belongs
to EmailService. "compute" belongs to BigDataService. "cache" belongs to
CacheService

The mapping of services to clusters is defined in the d2Config.json. D2's config-runner
populates zookeeper with this mapping. Then d2 client and d2 servers uses zookeeper
to coordinate their communication.

=========================
The server configuration:

To mimic a real server, we implemented a dummy http server that we call EchoServer.
This server is not a full-fledged REST server like rest.li. We want to demonstrate that
D2 can be used outside of rest.li.

Different from EchoServer in the quick-start example, the EchoServer in this example will
introduce delay when responding to request. The delay time is part of the request so
the client can modify the server's latency. This makes it easy for us to simulate examples
that mimic real world situation where the servers were under heavy load.

We will instantiate 8 EchoServers. EchoServer 1-3 are assigned to ProfileService.
EchoServer 4-5 are assigned to EmailService. EchoServer 6 are assigned to BigDataService.
EchoServer 7-8 are assigned to CacheService.

To make this example more interesting, we will create 3 partitions for ProfileService.
Each partition will contain 1 server. EchoServer 1 for partition 0. EchoServer 2 for
partition 1 and EchoServer 3 for partition 2.

On the other hand EmailService has 2 servers and also 2 partitions.
There will be 2 Echo Server for each partition but the weight of the server is different.
Here's the partition configuration for EmailService:

EchoServer 4,5 in partition 0. The weight of server 4 is 10 while server 5 is 1.
EchoServer 4,5 in partition 1. The weight of server 4 is 10 while server 5 is 1.
So weight of server 4 is 20 and weight of server 5 is 2. So this means the traffic ratio
between server 4 and 5 is 10 to 1. You will see that the traffic proportion that we send
to these server will be similar to the weight defined here.

For BigDataService, we will not have partitioning but we will set the delay
to be really high to mimic a very intensive compute cluster.

You will see how we'll modify load balancer configuration to tune D2's behavior
when a server is responding slowly.

Lastly, for CacheService, we will create 2 servers that belongs to the same partition.
We'll show how we can configure d2 to "stick" the same request to the same server to
improve cache locality.

Note that sticky routing is different from 'partitioning' because sticky routing does not
provide strict guarantee that a request will be routed in the same server/partition.
D2 will make an effort to "stick" the same request to the same server. However if the
server is in a bad state, D2 may route the request to a different server.

=========================
The client configuration:

In this example we will create 2 kinds of d2 clients. The first client will send traffic
like normal but for the second d2 client, we will inject some client overrides (will be
explained later). The amount of traffic sent to each service is configured in client's
json config file. The servers then print to stdout when they receive the traffic.

In D2 world, the service dictates how the client should communicate to the servers.
To be more concrete, the service tells how to configure the client for communicating with
the service. For example: Service dictates what is the time out, how big is the connection
pool, whether or not the client should use SSL and what are the SSL parameters.

This sounds counter-intuitive. Why does the server have the power to configure its client?
This is because we believe the service owners knows best how clients should interact with
its servers. If there are misbehaving clients, the ones who get most of the burden will be
the service owners so they have more stake than the client. But having said that, D2 also
has the flexibility to allow the client to override the server configuration.
But the service must allow the user to override the configuration. You will see how we
can set this up using BigDataService and the second d2 client.

To run this example
1.) Make sure zookeeper is running. For our example we will assume zookeeper is hosted
at localhost:2181. You can change this configuration in the config files.
2.) run ../../gradlew runConfigRunner. This will populate zookeeper with d2 configuration
defined in d2Config.json
3.) run ../../gradlew runServer. This will create echo servers as well as d2 announcers
according to the configuration in server.json. Press enter to stop the server from running
4.) Remember we have 4 different scenarios:
  1.) ProfileService Cluster
  to run this do ../../gradlew runProfileClient
  2.) EmailService Cluster
  to run this do ../../gradlew runEmailClient
  3.) BigDataService Cluster
  to run this do ../../gradlew runBigDataClient
  4.) CacheService Cluster
  to run this do ../../gradlew runCacheClient

For more details about how each client differs, please see the javadoc in the client code.

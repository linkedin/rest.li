## This is a standalone example that demonstrates how a simple D2 client-server works.

In this example we have 2 D2 Clusters:

* `RecommendationService Cluster`
* `NewsService Cluster`

We have 3 D2 Services:
* `articleRecommendation Service`
* `jobRecommendation Service`
* `newsArticle Service`

`articleRecommendation service` and `jobRecommendation service` belong to the
`RecommendationService cluster`.

`newsArticle Service` belongs to the `NewsService cluster`.

The mapping of services to clusters is defined in the `d2Config.json`. D2's config-runner
populates zookeeper with this mapping. Then d2 client and d2 servers uses zookeeper
to coordinate their communication.

To mimic a real server, we implemented a dummy http server that we call EchoServer.
This server is not a full-fledged REST server like rest.li. We want to demonstrate that
D2 can be used outside of rest.li.

We will instantiate 6 EchoServers. We will assign EchoServer 1-3 to the
`RecommendationService cluster` and EchoServer 4-6 to the `NewsService cluster`.
This configuration is defined in `server.json`.

D2 has the ability to adjust the weight of each server and also the partitions that
a server joins. D2 also have the ability to impart `stickiness` when routing. But these
are all advanced features that we won't cover in this example.

In this example we will simply create a d2 client that sends regular traffic to services.
The amount of traffic sent to each service is configured in `client.json`. The servers then
print to stdout when they receive the traffic.

### To run this example
* Make sure zookeeper is running. For our example we will assume zookeeper is hosted
at `localhost:2181`. You can change this configuration in the config files.
* run `../../gradlew runConfigRunner`. This will populate zookeeper with d2 configuration
defined in `d2Config.json`
* run `../../gradlew runServer`. This will create echo servers as well as d2 announcers
according to the configuration in `server.json`. Press enter to stop the server from running
* in a different terminal, run `../../gradlew runClient`. This will create a d2 client
that sends traffic to all three d2 services according to the traffic proportion specified
in client.json. Check the terminal in step #3 to see that all the traffic that's being
received by the servers. Press enter to stop the client from sending traffic.

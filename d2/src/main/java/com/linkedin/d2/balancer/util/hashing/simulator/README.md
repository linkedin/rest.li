`ConsistentHashRingSimulator` simulates the load distribution when multiple concurrent clients
route requests to servers in the hash ring. It is a tool for testing, debugging and tuning d2 consistent hash
ring configuration. 

The simulator is able to compare the results on the test ring against the strict
consistent hash ring, and report the request distribution (consistency) and the average latency (load balancing).
Also, it automatically generates a bar chart visualizing the requests distribution on the test ring against the strict
consistent hash ring, and line charts visualizing the CIR changes per server over time on test ring
and strict consistent hash ring.

The simulator is configurable using the `simulator.config` file under `com.linkedin.d2.balancer.util.hashing.simulator.config`. 
We provide a default config in this file. The following is an example config.

```
{
  "hashingAlgorithm": "multiProbe",
  "boundedLoadBalancingFactor": 1.25,
  "clients": [
    {
      "number": 5,
      "arrival": {
        "minInterval": 2,
        "maxInterval": 4,
        "randomStrategy": "UNIFORM"
      },
      "requests": [
        {
          "id": 15,
          "number": 20,
          "minLatency": 10,
          "maxLatency": 20,
          "randomStrategy": "GAUSSIAN"
        },
        {
          "number": 200,
          "minLatency": 10,
          "maxLatency": 20,
          "randomStrategy": "GAUSSIAN"
        }
      ]
    }
  ],
  "servers": [
    {
      "number": 1,
      "points": 5
    },
    {
      "number": 7,
      "points": 100
    }
  ],
  "shuffleRequests": true,
  "serverCapacity": 200
}
```

The “hashingAlgorithm” field specifies the hashing algorithm to use. All available hashing algorithms are
specified in DegraderRingFactory.

The “boundedLoadBalancingFactor” field specifies the balancing factor that enables the bounded-load feature,
which is a decorator of consistent hashing algorithms. No single server is allowed to have a load more than this
factor times the average load among all servers. A value of -1 disables the feature. Otherwise, it is a factor
greater than 1. Defaults to -1.

The “clients” field is a list of concurrent clients. For each client, we have to specify three fields:
“number”, “arrival” and “requests”.

  - The “number” field specifies the number of clients of this kind.
  - The “arrival” field specifies the arrival interval of the requests, which can be configured by
      “minInterval”, “maxInterval” and “randomStrategy”. The “randomStrategy” is an enum field of GAUSSIAN and UNIFORM,
      which specifies the distribution we use to pick an integer between “minInterval” and “maxInterval”.
  - The “requests” field is a list of request. For each request, we can specify its “id”, “number”, “minLatency”,
      “maxLatency” and “randomStrategy”. “id” is an optional field. When it is specified, the group of requests will
      be considered as the same kind. It is useful when we want to create a hot spot. When not specified, the group
      of requests will be assigned with random id’s. The “number” field specifies the number of requests of this kind,
      and the “randomStrategy” is an enum field of GAUSSIAN and UNIFORM.

The “servers” field is a list of servers. For each server, we have to specify two fields:
"number", which specifies the number of servers of this kind, and "points", which specifies the number of points it has
in the hash ring.

The “shuffleRequests” field is a boolean indicating whether the requests are shuffled before sending out. When
set to false, the requests will arrive in the order specified by the “requests” field.

Finally, the “serverCapacity” field specifies the capacity of a server. When a request is sent to a server whose number
of concurrent inflight request is approaching this capacity, the latency of the request will increase. When a
server’s capacity is reached, all of its requests will time out (time out is set to 1000 ms).

After the `simulator.config` file is configured, run the `main` method of `com.linkedin.d2.balancer.util.hashing.simulator.ConsistentHashSimulator`
to start the simulator.

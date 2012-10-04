#!/bin/sh

if [ $# -lt 2 ];
then
	echo 'USAGE:	./lb-echo-server.sh <zookeeper host> <zookeeper port> <echo server host> <echo server port> <scheme> <cluster> [services..]'
	echo '	./lb-echo-server.sh 127.0.0.1 2181 127.0.0.1 2345 http /d2 cluster1 service1 service2 service3'
	exit 1
fi

base_dir=$(dirname $0)

$base_dir/lb-run-class.sh com.linkedin.d2.balancer.util.LoadBalancerEchoServer "$@" &

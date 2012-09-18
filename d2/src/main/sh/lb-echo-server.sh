#!/bin/sh

if [ $# -lt 2 ];
then
	echo 'USAGE:	./scrips/lb-echo-server.sh port scheme cluster [services..]'
	echo '	./scrips/lb-echo-server.sh 2345 http cluster-1 service-1 service-2'
	exit 1
fi

base_dir=$(dirname $0)

$base_dir/lb-run-class.sh com.linkedin.d2.balancer.util.LoadBalancerEchoServer "$@" &

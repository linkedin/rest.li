#!/bin/sh

if [ $# -lt 1 ];
then
	echo 'USAGE:	./scrips/lb-echo-client.sh [services...]'
	echo '	./scrips/lb-echo-client.sh service-1 service-2'
	exit 1
fi

base_dir=$(dirname $0)

$base_dir/lb-run-class.sh com.linkedin.d2.balancer.util.LoadBalancerEchoClient "$@" &

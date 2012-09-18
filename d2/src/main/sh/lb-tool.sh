#!/bin/sh

base_dir=$(dirname $0)

$base_dir/lb-run-class.sh com.linkedin.d2.balancer.util.LoadBalancerCli "$@"

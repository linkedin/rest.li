#!/bin/sh

usage()
{
echo "Usage: lb-client.sh -z=[zookeeper] -p=[path] ...parameters..."
    echo "Examples"
    echo "========"
    echo "Example RunDiscovery (register clusters/services with zk): lb-client.sh -z zk://localhost:2121 -p /d2 -f d2_config_example.json -D"
    echo "Example RunDiscovery (register clusters/services with zk): lb-client.sh --zkserver zk://localhost:2121 --path /d2 --file d2_config_example.json --rundiscovery"
    echo "Example Print zk stores: lb-client.sh -z zk://localhost:2121 -p /d2 -c cluster-1 -s service-1_1 -S"
    echo "Example Print zk stores: lb-client.sh --zkserver zk://localhost:2121 --path /d2 --cluster cluster-1 --service service-1_1 --printstores"
    echo "Example Print single store: lb-client.sh -z=zk://localhost:2181 -p=/d2 -c='cluster-1' -s=service-1_1 -P"
    echo "Example Print single store: lb-client.sh --zkserver=zk://localhost:2181 --path=/d2 --cluster='cluster-1' --service=service-1_1 --printstore"
    echo "Example Print Service Schema: lb-client.sh -z zk://localhost:2121 -p /d2 -c 'cluster-1' -s service-1_1 -H"
    echo "Example Print Service Schema: lb-client.sh --zkserver zk://localhost:2181 --path /d2 --cluster 'cluster-1' --service service-1_1 --printschema"
    echo "Example Get Endpoints: lb-client.sh --zkserver zk://localhost:2121 --path /d2 --cluster cluster-1 --endpoints --service service-1_1"
    echo "Example Send request to service: lb-client.sh -z zk://localhost:2181 -p /d2 -c 'cluster-1' -s service-1_1 -r 'test' -R"
    echo "Example Send request to service: lb-client.sh -z zk://localhost:2181 -p /d2 -c 'cluster-1' -s service-1_1 -r 'test' -t rpc -R"
    echo "Example Send request to service: lb-client.sh --zkserver zk://localhost:2181 --path /d2 --cluster 'cluster-1' --service service-1_1 --request 'test' --sendrequest"
    echo "Example Send request to service: lb-client.sh -z zk://localhost:2181 -p /d2 -c 'history-write-1' -s HistoryService -m getCube -r 'test' -R"
    echo "Example Send request to service: lb-client.sh --zkserver zk://localhost:2181 --path /d2 --cluster 'history-write-1' --service HistoryService --method getCube --request 'test' --sendrequest"
    echo " "
}


if [ $# -lt 1 ];
then
	usage
	exit 1
fi

base_dir=$(dirname $0)
$base_dir/lb-run-class.sh com.linkedin.d2.balancer.util.LoadBalancerClientCli "$@"

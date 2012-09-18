#!/bin/sh

usage()
{
echo "Usage: lb-client.sh -z=[zookeeper] -p=[path]"
echo "Examples"
      echo "========"
      echo "Example Print zk stores: lb-client.sh -z=zk://esv4-be32.stg.linkedin.com:12913 -p=/d2 -S"
      echo "Example Print zk stores: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --printstores"
      echo "Example Print single store: lb-client.sh -z=zk://esv4-be32.stg.linkedin.com:12913 -p=/d2 -c='history-read-1' -s=HistoryService -P"
      echo "Example Print single store: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --cluster='history-write-1' --service=HistoryService --printstore"
      echo "Example Get Service Schema: lb-client.sh -z=zk://esv4-be32.stg.linkedin.com:12913 -p=/d2 -c='history-write-1' -s=HistoryService -H  "
      echo "Example Get Service Schema: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --cluster='history-write-1' --service=HistoryService --getschema"
      echo "Example Send request to service: lb-client.sh -z=zk://esv4-be32.stg.linkedin.com:12913 -p=/d2 -c='history-write-1' -s=HistoryService -m=getCube -r=$stgrequest -R"
      echo "Example Send request to service: lb-client.sh --zkserver=zk://esv4-be32.stg.linkedin.com:12913 --path=/d2 --cluster='history-write-1' --service=HistoryService --method=getCube --request=$stgrequest --sendrequest"
      echo " where stgrequest=\"{\"query\":{\"query\":[{\"limit\":12,\"transform\":\"SUM\",\"order\":[{\"column\":\"profile_views.tracking_time\",\"ascending\":false}],\"select\":[\"impression\",\"profile_views.tracking_time\"],\"group\":[\"profile_views.tracking_time\"]}],\"ids\":[\"1213\"],\"type\":\"wvmp-cube-profile-views\",\"stringCols\":[]}}\""
}


if [ $# -lt 1 ];
then
	usage
	exit 1
fi

base_dir=$(dirname $0)
$base_dir/lb-run-class.sh com.linkedin.d2.balancer.util.LoadBalancerClientCli "$@"

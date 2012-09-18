import signal
import sys
import random

from gremlins import faults, metafaults, triggers, procutils

# TODO use an environment variable for paths.. no more absolutes
# TODO make shutdown proper.. right now, bg processes are continuing ot run

procutils.START_COMMANDS = {
  'QuorumPeerMain': ["/Users/criccomi/Downloads/apache-zookeeper-ef198fc/bin/zkServer.sh", "start"],
  'LoadBalancerEchoServer': ["/Users/criccomi/svn/pegasus/trunk/d2/scripts/lb-echo-server.sh", "2345", "prpc", "cluster-1", "service-1", "service-2"],
  'LoadBalancerEchoClient': ["/Users/criccomi/svn/pegasus/trunk/d2/scripts/lb-echo-client.sh", "service-1", "service-2"],
}

# start zoo keeper
procutils.start_daemon('QuorumPeerMain');

# put cluster and service properties
procutils.run(["/Users/criccomi/svn/pegasus/trunk/d2/scripts/lb-tool.sh", "--put_cluster", "cluster-1", "--schemes", "prpc,http", "--banned", "http://www.google.com", "--store", "zk://localhost:2181/echo/lb/clusters"])
procutils.run(["/Users/criccomi/svn/pegasus/trunk/d2/scripts/lb-tool.sh", "--put_service", "service-1", "--cluster", "cluster-1", "--path", "/service-1", "--balancer", "degrader", "--store", "zk://localhost:2181/echo/lb/services"])
procutils.run(["/Users/criccomi/svn/pegasus/trunk/d2/scripts/lb-tool.sh", "--put_service", "service-2", "--cluster", "cluster-1", "--path", "/service-2", "--balancer", "degrader", "--store", "zk://localhost:2181/echo/lb/services"])

# start server and client
procutils.start_daemon('LoadBalancerEchoServer');
procutils.start_daemon('LoadBalancerEchoClient');

# declare faults
kill_short_zk = faults.kill_daemons(["QuorumPeerMain"], signal.SIGKILL, 5)
kill_short_server = faults.kill_daemons(["LoadBalancerEchoServer"], signal.SIGKILL, 5)
kill_short_client = faults.kill_daemons(["LoadBalancerEchoClient"], signal.SIGKILL, 5)

kill_long_zk = faults.kill_daemons(["QuorumPeerMain"], signal.SIGKILL, 60)
kill_long_server = faults.kill_daemons(["LoadBalancerEchoServer"], signal.SIGKILL, 60)
kill_long_client = faults.kill_daemons(["LoadBalancerEchoClient"], signal.SIGKILL, 60)

pause_zk = faults.pause_daemons(["QuorumPeerMain"], 60)
pause_server = faults.pause_daemons(["LoadBalancerEchoServer"], 60)
pause_client = faults.pause_daemons(["LoadBalancerEchoClient"], 60)

def random_fault():
  metafaults.pick_fault([
    (1, kill_short_zk),
    (1, kill_short_server),
    (1, kill_short_client),
    
    #(1, kill_long_zk),
    #(1, kill_long_server),
    #(1, kill_long_client),
    
    #(1, pause_zk),
    #(1, pause_server),
    #(1, pause_client),
  ])()

random_periodic = triggers.Periodic(10, random_fault)

profile = [
  random_periodic
]

def signal_handler(sig, frame):
  faults.kill_daemons(["QuorumPeerMain"], signal.SIGKILL, 60)
  faults.kill_daemons(["LoadBalancerEchoServer"], signal.SIGKILL, 60)
  faults.kill_daemons(["LoadBalancerEchoClient"], signal.SIGKILL, 60)
  random_periodic.stop
  random_periodic.join
  sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)

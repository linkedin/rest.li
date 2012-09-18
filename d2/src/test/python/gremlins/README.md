These tests aren't super useful at this point, because we're using a
ZooKeeperTogglingStore, which will disable itself the second anything bad
happens. The first time a gremlin strikes, the client will just fall back on its
file store.

1. Download: https://github.com/toddlipcon/gremlins
2. Install gremlins according to the instructions on the page
3. In d2 run: gradle packup
4. Install pegasus gremlins: sudo python setup.py develop
5. Run pegasus gremlins: gremlins -m pegasus.profiles.pegasus -p pegasus.profile
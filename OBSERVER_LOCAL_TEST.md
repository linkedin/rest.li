# Local end-to-end test: INDIS observer-cluster subscription (WS2)

> Throwaway branch `hliang/observer-local-test`. Do **not** merge this; it carries debug scaffolding
> (`XdsToD2SampleClient` edits + a `runSampleClient` Gradle task) on top of the real PR branch
> `hliang/indis-force-observer-cluster-subscription`. Delete after testing.

## What we're verifying
The PR change makes the xDS-based D2 client subscribe to the INDIS observer's **own** D2 cluster
(`IndisRegistryObserver`) so it receives and caches the live observer endpoint set over xDS — the first
step to removing the hard DNS-DISCO dependency for discovering the observer.

This runbook spins up a real D2 xDS client (`XdsToD2SampleClient`) pointed at a **running observer**,
with the subscription **forced on**, and prints the observer endpoints it receives. Seeing those endpoints
arrive = the change works end to end.

## What's in this branch
- **The PR change** (`com.linkedin.pegasus:d2`):
  - `XdsToD2PropertiesAdaptor.start()` → when `_subscribeToObserverCluster` is true, also calls
    `listenToCluster("IndisRegistryObserver")` + `listenToUris("IndisRegistryObserver")`.
  - Flag plumbed via `D2ClientConfig.subscribeToIndisObserverCluster` → `D2ClientBuilder` →
    `XdsLoadBalancerWithFacilitiesFactory` → `adaptor.setSubscribeToObserverCluster(...)`.
- **The harness (this branch only):**
  - `d2/src/test/java/com/linkedin/d2/xds/XdsToD2SampleClient.java` — sets the flag, calls `start()`,
    and logs received endpoints via a small `LoggingSdEventEmitter`.
  - `d2/build.gradle` — adds task `:d2:runSampleClient` (JavaExec over the test classpath).

## Prerequisites (on the VM)
1. **A running observer** reachable from the VM (this is why we test here, not on the Mac). Confirm it's up
   and announcing itself, e.g.:
   ```
   curli -k --dv-auth SELF -f <fabric> d2://d2Clusters/IndisRegistryObserver
   ```
   Note its **xDS address** (host:port, typically `<host>:32123`; `localhost:32123` if running locally).
2. **JDK 11** — Gradle 6.9.4 will NOT run on Java 17/21 ("Unsupported class file major version"). Find an 11:
   ```
   update-alternatives --list java        # or: ls /usr/lib/jvm
   export JAVA_HOME=/usr/lib/jvm/<your-jdk-11>     # e.g. java-11-openjdk
   ```
3. **mTLS certs** — only if the observer requires TLS (most fabric observers do; a local plaintext dev
   observer does not). You need a client **keystore** + **truststore** (your LinkedIn identity). If you don't
   have them handy, generate/locate via `id-tool` or your `~/.linkedin` identity.

## Steps
```bash
# 1. Get the branch (public repo — no auth needed for read-only build)
git clone https://github.com/linkedin/rest.li.git
cd rest.li
git fetch origin hliang/observer-local-test
git checkout hliang/observer-local-test

# 2. JDK 11
export JAVA_HOME=/usr/lib/jvm/<your-jdk-11>

# 3a. Run against a PLAINTEXT observer (e.g. local dev):
./gradlew :d2:runSampleClient --args="-xds localhost:32123"

# 3b. Run against an mTLS observer (most fabrics):
./gradlew :d2:runSampleClient --args="\
  -xds <observer-host>:32123 \
  -keyStoreFilePath <path/to/identity.p12> -keyStorePassword <pw> -keyStoreType PKCS12 \
  -trustStoreFilePath <path/to/truststore.jks> -trustStorePassword <pw>"
```
The client stays running (Ctrl-C to stop).

## Success criteria
In the output you should see (order may vary):
- `Subscribing to ... /d2/uris/IndisRegistryObserver` and `.../d2/clusters/IndisRegistryObserver`
  (from `XdsClientImpl`) — proves the subscription is sent.
- `[observer-test] initial request for cluster 'IndisRegistryObserver' succeeded=true`
- One or more `[observer-test] endpoint MARK_READY: cluster='IndisRegistryObserver' <host>:<port>`
  — **these are the observer's own endpoints, delivered over xDS.** That's the win.

If you flip the behavior off (set `adaptor.setSubscribeToObserverCluster(false)` in the sample client and
rerun), you should see **no** `IndisRegistryObserver` subscription — the negative case.

## Troubleshooting
- **`Unsupported class file major version`** → wrong JDK; use Java 11 (step 2).
- **Connection refused / UNAVAILABLE** → observer not reachable at that host:port, or needs TLS but you ran plaintext.
- **TLS handshake / SSL errors** → keystore/truststore wrong, or observer expects mTLS you didn't supply.
- **Subscribes but no endpoints** → confirm the observer actually announces `IndisRegistryObserver`
  (the `curli` in prerequisites should list `uris`).

## Cleanup
```bash
git push origin --delete hliang/observer-local-test   # remove the throwaway branch when done
```

---
### Context for an AI assistant picking this up fresh
You are continuing INDIS "Resilient Client Discovery Beyond DNS", Workstream 2 (force clients to subscribe to
the observer cluster). The rest.li change is implemented + unit-tested on `hliang/indis-force-observer-cluster-subscription`
(pushed to `linkedin/rest.li`). This branch adds a manual end-to-end harness. The goal here is only to **run**
`:d2:runSampleClient` against a live observer on this VM and confirm the observer's endpoints arrive over xDS
(see "Success criteria"). The observer lives on this VM; the rest.li clone can be read-only (public repo).
JDK 11 required. Do not modify the PR change; harness edits stay on this branch.

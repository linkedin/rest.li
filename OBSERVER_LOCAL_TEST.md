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
  - `d2/src/test/java/com/linkedin/d2/xds/XdsToD2SampleClient.java` — builds a real `D2Client` through
    `D2ClientBuilder` with the shipping `XdsLoadBalancerWithFacilitiesFactory` and
    `.setSubscribeToIndisObserverCluster(true)`, then `start()`s it (warm-up disabled). This drives the
    **full production plumbing** — `D2ClientConfig` → `D2ClientBuilder` → factory →
    `adaptor.setSubscribeToObserverCluster` — against a live observer, rather than hand-building the adaptor.
    SD events are logged via a small `LoggingSdEventEmitter`.
  - `d2/build.gradle` — adds task `:d2:runSampleClient` (JavaExec over the test classpath), plus an
    isolated `sampleClientLogging` configuration (SLF4J→log4j2 binding). **The test runtime has no SLF4J
    binding, so without this the run silently uses the NOP logger and you see none of the XdsClientImpl
    logs.** The task points log4j2 at `d2/src/test/resources/log4j2-sampleclient.xml` (console, DEBUG for
    `com.linkedin.d2.xds`).

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
3. **mTLS certs — REQUIRED.** Confirmed empirically: the observer's xDS gRPC server has security enabled
   (`gRPCServerSecurity.disabled = adsServer.GRPCInsecure = false`), so a plaintext connection just times
   out (`initial request ... succeeded=false (2002ms)`). You need a client **keystore** + **truststore**.
   On the VM these work out of the box — the observer's own identity is a valid LinkedIn cert the observer
   trusts, and `/etc/riddler/cacerts` is the standard truststore:
   - keystore: `<observer-app-dir>/var/identity.p12`  (PKCS12, password `work_around_jdk-6879539`)
   - truststore: `/etc/riddler/cacerts`  (JKS, password `changeit`)
   (`<observer-app-dir>` is e.g. `/export/content/lid/apps/indis-registry-observer/dev-i001`.)

## Steps
```bash
# 1. Get the branch (public repo — no auth needed for read-only build)
git clone https://github.com/linkedin/rest.li.git
cd rest.li
git fetch origin hliang/observer-local-test
git checkout hliang/observer-local-test

# 2. JDK 11  (on this VM it's already here)
export JAVA_HOME=/export/apps/jdk/JDK-11_0_10_9-msft

# 3. Run against the mTLS observer (this is the real case — plaintext just times out).
#    Use the observer's own identity as the client cert + the standard truststore:
P12=/export/content/lid/apps/indis-registry-observer/dev-i001/var/identity.p12
./gradlew :d2:runSampleClient --args="\
  -xds localhost:32123 \
  -keyStoreFilePath $P12 -keyStorePassword work_around_jdk-6879539 -keyStoreType PKCS12 \
  -trustStoreFilePath /etc/riddler/cacerts -trustStorePassword changeit"
```
The client stays running (Ctrl-C to stop). Tip: launch it detached and tee to a log, e.g.
`setsid nohup ./gradlew ... > /tmp/observer-test.log 2>&1 < /dev/null &`, then `grep` the log — the
process never exits on its own, so a foreground `gradlew` will appear to "hang."

## Success criteria
In the output you should see (order may vary):
- `[observer-test] D2 client started via D2ClientBuilder (subscribeToIndisObserverCluster=true, warmUp=false)`
  — confirms the full builder path (not a hand-built adaptor) started.
- `Subscribing to NODE resource: /d2/clusters/IndisRegistryObserver` **and**
  `Subscribing to D2_URI_MAP resource: /d2/uris/IndisRegistryObserver` (from `XdsClientImpl`) — proves the
  subscription is sent, driven by the config flag through the production plumbing.
- `[observer-test] initial request for cluster 'IndisRegistryObserver' succeeded=true`.
- **The authoritative win** — the observer's own endpoint delivered and cached over xDS:
  - `Received initial data for D2_URI_MAP /d2/uris/IndisRegistryObserver. Set state to FETCHED.`
  - just above it, the raw `D2URI` payload for the resource (`cluster_name: "IndisRegistryObserver"`, `uri:
    "https://<host>:32123"`, `partition_desc`, ...).

  FETCHED-with-real-payload **is** the proof the change works end to end. Treat it as the success signal,
  not MARK_READY (see next section).

To see the negative case, set `.setSubscribeToIndisObserverCluster(false)` (or drop the call) in the sample
client and rerun: there should be **no** `IndisRegistryObserver` subscription at all.

## Why you will NOT see a `MARK_READY` line (and why that's fine)
The `[observer-test] endpoint MARK_READY: ...` SD-status event does **not** fire for an endpoint that was
announced **before** this client subscribed — which is always the case here, since the observer announced
itself long before you start the harness. This is by design, not a failure:

- `XdsClientImpl.trackServerLatencyHelper` sets `isStaleModifiedTime = (modifiedAt < subscribedAt)` (IRV is
  off in this harness). The observer's endpoint `modifiedAt` (its announce time) precedes the client's
  `subscribedAt`, so the URI is flagged stale.
- `XdsToD2PropertiesAdaptor.emitSDStatusUpdateReceiptEvents` then skips stale URIs
  (`if (xdsUpdate.isStaleModifiedTime(name)) return;`), so no receipt event is emitted.

MARK_READY is downstream **telemetry** for *newly-changed* endpoints; it is not the subscribe-and-cache
behavior this PR adds. The FETCHED `D2_URI_MAP` with the real endpoint payload proves the endpoint is
received and cached regardless.

Bouncing the observer does **not** produce a MARK_READY either: on reconnect the subscriber's `reset()`
bumps `subscribedAt` to the reconnect time, so the re-announced endpoint stays stale. The only way to force
a MARK_READY would be to announce a *fresh* endpoint into the `IndisRegistryObserver` cluster while the
client stays continuously subscribed — but that cluster is served from **shared staging ZK**
(`zk-ltx1-d2.stg.linkedin.com:12913`), so injecting test endpoints there is risky and not worth it for a
cosmetic log line.

## Troubleshooting
- **`Unsupported class file major version`** → wrong JDK; use Java 11 (step 2).
- **No logs at all, only the two `[observer-test]` `System.out` lines** → SLF4J fell back to the NOP logger
  (`Failed to load class StaticLoggerBinder`). The `sampleClientLogging` config + `log4j.configurationFile`
  on the task fix this; make sure you didn't strip them.
- **`initial request ... succeeded=false (~2002ms)`** → you connected plaintext to the mTLS observer; pass
  the keystore/truststore args (step 3). Confirm with `openssl s_client -connect localhost:32123` — a TLS
  handshake means mTLS is required.
- **Connection refused / UNAVAILABLE** → observer not reachable at that host:port.
- **TLS handshake / SSL errors** → keystore/truststore path or password wrong.
- **Subscribes, FETCHED, but no MARK_READY** → expected; see "Why you will NOT see a `MARK_READY` line".
- **Subscribes but never reaches FETCHED / no payload** → the observer isn't actually announcing
  `IndisRegistryObserver`; check its `_json-uri-audit.log` for a `clusterName":"IndisRegistryObserver"`
  mark-up.

## Cleanup
```bash
git push origin --delete hliang/observer-local-test   # remove the throwaway branch when done
```

---
### Context for an AI assistant picking this up fresh
You are continuing INDIS "Resilient Client Discovery Beyond DNS", Workstream 2 (force clients to subscribe to
the observer cluster). The rest.li change is implemented + unit-tested on `hliang/indis-force-observer-cluster-subscription`
(pushed to `linkedin/rest.li`). This branch adds a manual end-to-end harness. The goal here is only to **run**
`:d2:runSampleClient` against a live observer on this VM and confirm the observer's endpoints arrive over xDS.
JDK 11 required. Do not modify the PR change; harness edits stay on this branch.

**Status as of last run (verified):** the change works end to end through the full production path. The
harness builds a `D2Client` via `D2ClientBuilder.setSubscribeToIndisObserverCluster(true)` and starts it; over
mTLS the client subscribes to `/d2/clusters/IndisRegistryObserver` + `/d2/uris/IndisRegistryObserver`, the
initial request succeeds, and the observer's own endpoint (`https://hliang-ld1.linkedin.biz:32123`) is received
and cached over xDS (`D2_URI_MAP ... FETCHED` with the real payload). The doc's old "MARK_READY" success bullet
was misleading: that SD-status event is intentionally suppressed for endpoints announced before the
subscription (stale-modified-time guard). The FETCHED `D2_URI_MAP` payload is the real success signal. See the
two sections above for the full explanation. Nothing further is needed to call WS2 verified.

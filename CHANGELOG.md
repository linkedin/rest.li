# Changelog
All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The process for developers to update the changelog is as follows:
- Always enter your change descriptions under the **"Unreleased"** heading.
  Do _not_ attempt to manually create your own version heading.
- When bumping the project version in your commit, run the `./scripts/update-changelog`
  script to automatically move everything under **"Unreleased"** to a new heading.

When updating the changelog, remember to be very clear about what behavior has changed
and what APIs have changed, if applicable.

## [Unreleased]

## [29.48.4] - 2023-12-06
- Rename next to nextPageToken in standardized models for cursor based pagination

## [29.48.3] - 2023-11-28
- Add standardized models for cursor based pagination

## [29.48.2] - 2023-11-27
- Remove usage of Optional from SimpleLoadBalancer

## [29.48.1] - 2023-11-27
- Update SimpleLoadBalancer to use for loop instead of Map

## [29.48.0] - 2023-11-13
- Fix dual-read potential risk that newLb may impact oldLb

## [29.47.0] - 2023-11-13
- Use Node instead of D2Node and D2URIMap instead of NodeMap for xDS flow

## [29.46.9] - 2023-11-02
- Update FieldDef so that it will lazily cache the hashCode.

## [29.46.8] - 2023-10-11
- add metrics about xds connection status and count

## [29.46.7] - 2023-10-10
- fix xDS client bugs and race conditions

## [29.46.6] - 2023-10-04
- simplify symlink subscription in xds flow

## [29.46.5] - 2023-10-02
- support d2 symlink in indis flow

## [29.46.4] - 2023-09-27
- Conduct a more thorough search and fix the remaining ByteBuffer errors to be compatible with Java 8 runtimes.

## [29.46.3] - 2023-09-26
- Fix ByteBuffer errors to be compatible with Java 8 runtimes.

## [29.46.2] - 2023-09-25
- add service/cluster-not-found count to simple load balancer jmx. And add entry-out-of-sync count to dual read monitoring.

## [29.46.1] - 2023-09-20
- Keep the old convention (using a variable java of type matrix) in publish.yml

## [29.46.0] - 2023-09-05
- Rewrite the Java Doc logic in Java 11 APIs and use multi-release jar to be backward compatible with Java 8 consumers

## [29.45.1] - 2023-09-05
- add @Nullable annotations to pegasus java getters and setters with mode

## [29.45.0] - 2023-08-25

- Downgrade major version back to 29. Technically this is not semver-compatible
  but we feel that the impact should be less severe than the impact of bumping
  the major version.

## [30.0.0] - 2023-08-15
- Remove resetTogglingStores functionality from LoadBalancerClientCli, which is incompatible with Java 17

## [29.44.0] - 2023-08-06
- dynamically switch jmx/sensor names based on dual read mode and source type

## [29.43.11] - 2023-08-01
- fix logging issues about observer host and dual read mode

## [29.43.10] - 2023-07-24
- set log level of dual read mode changes to info.

## [29.43.9] - 2023-07-18
- add `rest.idl.processEmptyIdlDir` property in `PegasusPlugin` to support IDL files auto generation
  - If this property is true, plugin will create rest client gradle tasks even if IDL dir is empty.

## [29.43.8] - 2023-07-13
- Add support for gRPC-downstream extension annotations (`@grpcExtension`, `@grpcService`).

## [29.43.7] - 2023-07-11
- Make file extension of D2 ZKFS file store fully customizable.

## [29.43.6] - 2023-07-10
- Enable passing settings to custom partition accessor

## [29.43.5] - 2023-06-27
- Remove a delegated method in LoadBalancerWithFacilitiesDelegator

## [29.43.4] - 2023-06-22
- Refactor ZookeeperServer, making functionality to generate URI properties for node accessible to subclasses

## [29.43.3] - 2023-06-22
- Bump jfrog build-info-extractor-gradle to 4.32.0

## [29.43.2] - 2023-06-21
- Add missing interface method in LoadBalancerWithFacilitiesDelegator

## [29.43.1] - 2023-06-20
- mute SD update receipt event for initial request on a new cluster

## [29.43.0] - 2023-06-16
- Implement rest.li xDS service discovery flow and DualRead loadbalancer

## [29.42.4] - 2023-06-02
- Add log message in RestClient for ScatterGatherStrategy map URIs empty case

## [29.42.3] - 2023-05-18
- Support for UDS sockets for HTTP/1
- Make ValidationExtensionSchemaTask cacheable

## [29.42.2] - 2023-05-11
- Fix synchronization on `RequestContext` to prevent `ConcurrentModificationException`.

## [29.42.1] - 2023-05-11
- Add support for returning location of schema elements from the PDL schema encoder.
  
## [29.42.0] - 2023-05-02
- Remove the overriding of content-length for HEADER requests as per HTTP Spec
  More details about this issue can be found @ https://jira01.corp.linkedin.com:8443/browse/SI-31814

## [29.41.12] - 2023-04-06
- Introduce `@extension.injectedUrnParts` ER annotation.
  - This will be used as the replacement for using `@extension.params` to specify injected URN parts.
  - `@extension.params` will now primarily be used for specifying injection query parameters.

## [29.41.11] - 2023-03-09
- Updates `Data.TraverseCallback` to have a callback for 'endKey'.

## [29.41.10] - 2023-02-23
- Use proper UTF8 encoding in AvroUtil.jsonFromGenericRecord, also deprecate AvroUtil and bump avro-util dependency

## [29.41.9] - 2023-02-15
- Handle infinity/-infinity/NaN in DataSchema -> Avro record data translation.

## [29.41.8] - 2023-02-14
- Allow annotation `@ExcludedInGraphQL` on extension schema fields.

## [29.41.7] - 2023-02-13
- Split getPotentialClients impl between subsetting and not-subsetting cases

## [29.41.6] - 2023-01-25
- Fix Async R2 Servlet deadlock condition

## [29.41.5] - 2023-01-11
- Handle Avro self-referential aliases in Avro to Proto schema translation.

## [29.41.4] - 2023-01-09
- Change the innitialize size of resolvedProperties to 0 in order to save memory pre-allocated

## [29.41.3] - 2023-01-03
- Add option to force publish idl and snapshot

## [29.41.2] - 2022-12-21
- Enable enumeration of clusters in `ZKFailoutConfigProvider`.

## [29.41.1] - 2022-12-19
- Replace the API call getArchivePath() with getArchiveFile() on Gradle 7 in the Pegasus Plugin

## [29.41.0] - 2022-12-15
- Reduce memory allocations during rich schema traversal

## [29.40.15] - 2022-12-08
- Allow disabling the ivy publication preconfiguration in the Pegasus Gradle plugin

## [29.40.14] - 2022-12-06
- Make CurrentSchemaEntryMode public so that all TraverserContext interface getters can be accessed by restli users

## [29.40.13] - 2022-12-01
- Add D2 loggings for tracking the initial received D2 Clusters and D2 Uris

## [29.40.12] - 2022-11-30
- Add channel writability to streaming timeout exception

## [29.40.11] - 2022-11-17
- Add util class to convert generic List/Map to DataList/DataMap or vice versa

## [29.40.10] - 2022-11-16
- Fix the deprecated configuration name used in the PegasusPlugin

## [29.40.9] - 2022-11-15
- Enable validation check in the build of the gradle plugin and fix some validation errors with Gradle 7

## [29.40.8] - 2022-11-14
- Upgrade Apache Commons Text to 1.10.0 as vulnerability fix (CVE-2022-42889)

## [29.40.7] - 2022-11-07
- Remove @PathSensitive from property idlDestinationDir in GenerateRestModelTask

## [29.40.6] - 2022-11-06
- Add getter of IncludesDeclaredInline in RecordDataSchema

## [29.40.5] - 2022-11-03
- Fix the Gradle 7 validation errors in GenerateRestClientTask

## [29.40.4] - 2022-10-31
- Update SchemaToPdlEncoder to fix nested schema encoding layout

## [29.40.3] - 2022-10-25
- Change logging level of D2 cluster subsetting updates to DEBUG

## [29.40.2] - 2022-10-25
- Refactor the Netty JMX handling for injection of the metrics handling into the client rather than the other way around.

## [29.40.1] - 2022-10-13
- Add service discovery event emitter to d2 client

## [29.40.0] - 2022-10-13
- Empty commit to bump pegasus minor version 

## [29.39.6] - 2022-10-06
- Add equals and hashCode methods to `CollectionResult`, `GetResult`, `UpdateResponse` and `UpdateEntityResponse`.

- Add `RestLiTraceInfo` to the `RequestContext` for both incoming and outgoing requests.
Added `Request.getResourceMethodIdentifier()`,
 `ResourceDefinition.getBaseUriTemplate()`, and `ResourceMethodDescriptor.getResourceMethodIdentifier()`.

## [29.39.5] - 2022-10-04
- Emit service discovery status related events

## [29.39.4] - 2022-09-30
- Add JMX metrics for DNS resolution and clarify DNS timeout errors.

## [29.39.3] - 2022-09-26
- Catch exceptions when zk connection state change event is received after zk connection shutdown.

## [29.39.2] - 2022-09-23
- Remove unnessary extra IDL annotations due to recent restriction of adding new method into bridged service and emit
  resourceClass for javaparser to use to update rest.li resource.

## [29.39.1] - 2022-09-20
- Expose an easy way to override validation options for ActionArgumentBuilder

## [29.39.0] - 2022-09-19
- Releasing support for UDS in HTTP/2 stack

## [29.38.6] - 2022-09-15
- Add a validation option to coerce base64 encoded fixed values

## [29.38.5] - 2022-09-15
- Update the error message to better guide users when zkRef is null.

## [29.38.4] - 2022-09-08
- Use ZooKeeper 3.6.3

## [29.38.3] - 2022-09-07
- Emit java method name in the IDL/Snapshot to enable us to generate a java stub back from the IDL.

## [29.38.2] - 2022-09-07
- Removing the release candidate version number

## [29.38.1-rc.1] - 2022-09-06
- Add Support for UDS transport protocol in R2 outbound traffic

## [29.38.0] - 2022-08-31
- Upgrade Netty to 4.1.79 and remove ZooKeeper Netty exclusions.

## [29.37.19] - 2022-08-31
- Emit some additional information in the IDL/Snapshot to enable us to generate a java stub back from the IDL

## [29.37.18] - 2022-08-29
- Support supplying D2 subsetting peer cluster name at run-time
 
## [29.37.17] - 2022-08-29
- Add "notify" to reversed word set when generating data template

## [29.37.16] - 2022-08-24
- Make `DefaultDocumentationRequestHandler` blocking again to avoid the `503` errors users were frequently seeing for `OPTIONS` calls.
  - Introduce the existing non-blocking ("fail fast") variant as optional subclass `NonBlockingDocumentationRequestHandler`.

## [29.37.15] - 2022-08-23
- Exclude transitive Netty dependency for ZooKeeper client.

## [29.37.14] - 2022-08-19
- Avoid casting classloader to `URLLoader` in `ResourceModelEncoder` and use `ClassGraph` to search for restspec file

## [29.37.13] - 2022-08-15
- Fix `d2-test-api` dependencies

## [29.37.12] - 2022-08-10
- Support removing cluster watches created due to cluster failout

## [29.37.11] - 2022-08-09
- Avoid using `SmileFactoryBuilder` to be more compatible with pre-`2.10` jackson at runtime

## [29.37.10] - 2022-08-08
- Fix `PrimitiveTemplateSpec` not having `className`

## [29.37.9] - 2022-08-07
- Add null-checks for cluster and service properties in `D2ClientJmxManager`

## [29.37.8] - 2022-08-04
- Switch to use name regex pattern to skip deprecated fields in spec generation

## [29.37.7] - 2022-08-03
- Bugfix: mark a dark request as sent if it is sent to any dark clusters

## [29.37.6] - 2022-07-28
- Bump ZooKeeper client version to [3.7.1](https://zookeeper.apache.org/releases.html#releasenotes) (latest stable version at the time).

## [29.37.5] - 2022-07-28
- Add option to skip deprecated field when recursively generate class spec

## [29.37.4] - 2022-07-25
- Serialize ZK data with non-null fields only 

## [29.37.3] - 2022-07-18
- Add connection warm up support when a failout has been initiated

## [29.37.2] - 2022-07-18
- Validate HTTP override header for query tunneling.

## [29.37.1] - 2022-06-29
- Handle method order when validating methods to ensure consistent linked batch finder validation

## [29.37.0] - 2022-06-23
- Package translated legacy PDSC models into `:restli-common` JAR

## [29.36.1] - 2022-06-22
- Fix FailoutClient delegated client's restRequest invocation 

## [29.36.0] - 2022-06-21
- Add Enum symbols order change as compatible message in checker. This will make equivalent compatibility check to fail and publish the new snapshot files.

## [29.35.0] - 2022-06-15
- Avoid using JsonFactoryBuilder to be more compatible with pre 2.10 jackson at runtime

## [29.34.3] - 2022-06-06
- Translate Data.null to Avro null if schema field is optional

## [29.34.2] - 2022-06-03
- Provide a way to link a finder with a functionally equivalent batch finder declaratively

## [29.34.1] - 2022-05-28
- fix passing in canary distribution provider from ZKFS load balancer factory.

## [29.34.0] - 2022-05-11
- update d2 partitioning logic to map unmapped URIs to default partition 0

## [29.33.9] - 2022-05-10
- Experimental optimization of action request building
- Revert "Provide a mechanism to set a routing hint for the d2 request to get request symbol table (#787)"

## [29.33.8] - 2022-05-10
- Add (currently unused) models for `D2FailoutProperties`.

## [29.33.7] - 2022-05-04
- Silence Zookeeper errors in logs on race condition between watched events and async shutdown.

## [29.33.6] - 2022-05-03
- Provide a mechanism to set a routing hint for the d2 request to get request symbol table.

## [29.33.5] - 2022-05-02
- Expose `RestLiConfig` from `RestLiServer`.

## [29.33.4] - 2022-04-26
- Support failout redirection in D2 client.

## [29.33.3] - 2022-04-25
- Add end-to-end integration tests for D2 client.

## [29.33.2] - 2022-04-21
- Add JMX-based canary monitoring for cluster properties and service properties.

## [29.33.1] - 2022-04-12
- Fix an Avro translation bug where optional fields in a partial default record are not treated properly.

## [29.33.0] - 2022-03-28
- Add Support for `ByteString[]` Query Parameters

## [29.32.5] - 2022-03-22
- Updating newInstance usage which is deprecated in Java 9+

## [29.32.4] - 2022-03-21
- Add support for custom affinity routing provider

## [29.32.3] - 2022-03-18
- Ignore null values in Schema Parser instead of throwing error.

## [29.32.2] - 2022-03-17
- Fix documentation renderer's doc string rendering failure for restspec filename that has api-name prefix

## [29.32.1] - 2022-03-15
- Support failouts in ClusterStoreProperties.

## [29.32.0] - 2022-03-08
- Add support for dark warm-up

## [29.31.0] - 2022-03-02
- Support d2 config canary

## [29.30.0] - 2022-02-28
- Re-apply avro update to 1.9.2

## [29.29.2] - 2022-02-17
- Generalize avro --> pegasus translation code to accept any CharSequence value for strings

## [29.29.1] - 2022-02-10
- Make DarkGateKeeper configurable for different dark clusters

## [29.29.0] - 2022-02-09
- Revert avro update introduced in 29.27.0

## [29.28.0] - 2022-02-04
- Fix weight double-count in D2 SimpleLoadBalancer

## [29.27.0] - 2022-01-25
- Update Avro version to 1.9.2

## [29.26.4] - 2022-01-24
- Map local variant of service ZNodes to cluster without colo suffix

## [29.26.3] - 2022-01-18
- Generate compile time constants for union members if people want to write build time switch case to check for things based on member

## [29.26.2] - 2022-01-13
- Fix for null pointer exception when registering listener on event bus before publisher is set.

## [29.26.1] - 2022-01-13
- Fail documentation requests while renderers are being lazily initialized (rather than block threads until complete).

## [29.26.0] - 2022-01-10
- Add header provider to generate required request to fetch default remote symbol table

## [29.25.0] - 2022-01-06
- Fix a race condition where TransportClient is shutdown while subset cache still holds reference to it

## [29.24.0] - 2021-12-09
- bump minor version for the new public method added in 29.23.3

## [29.23.3] - 2021-12-09
- Take default headers as input to fetch remote symbol table in `DefaultSymbolTableProvider`

## [29.23.2] - 2021-12-09
- Observability enhancements for D2 announcer.

## [29.23.1] - 2021-12-08
- Add support for a rate limiter supplier to enable multiple dark clusters with the CONSTANT_QPS strategy

## [29.23.0] - 2021-12-06
- Introduce d2ServiceName annotation on rest.li resources. This is an optional param that is meant to be populated by resources for whom the resource name is not the same as d2 service name

## [29.22.16] - 2021-12-03
- Fixed issues with potential duplicate TimingKeys being registered.

## [29.22.15] - 2021-11-30
- Add mock response generator factory for BATCH_FINDER methods.
- Deprecate `FileFormatDataSchemaParser#new(String, DataSchemaResolver, DataSchemaParserFactory)`. 
- Add file existence check before performing compatibility report check during snapshot and restmodel publishing

## [29.22.14] - 2021-11-24
- Fix bug where content type was not set for stream requests

## [29.22.13] - 2021-11-05
- Make SmoothRateLimiter setRate idempotent

## [29.22.12] - 2021-10-28
- add canaries to service and cluster properties

## [29.22.11] - 2021-10-25
- Fix an issue in D2 StateUpdater to force update PartitionState

## [29.22.10] - 2021-10-20
- SmoothRateLimiter - do not double-count execution delays on setRate

## [29.22.9] - 2021-10-12
- Make client timeout to fetch remote symbol table configurable in `RestLiSymbolTableProvider`

## [29.22.8] - 2021-10-12
- No changes; re-releasing because the previous release (`29.22.7`) was corrupted.

## [29.22.7] - 2021-10-11
- Fix bug for `generateDataTemplateTask` to consume command line option correctly.

## [29.22.6] - 2021-10-08
- Fix bug in `SmoothRateLimiter` where `executionDelay` is not honored.

## [29.22.5] - 2021-10-08
- Make `PegasusPlugin#getDataSchemaPath` public.

## [29.22.4] - 2021-09-28
- Improve support for JSR330 by allowing package protected constructors annotated with `@Inject`.
- Fix Supported mime type config for response payload.

## [29.22.3] - 2021-09-21
- Allow disabling of load balancing for a specific host.

## [29.22.2] - 2021-09-20
- Add server config support to define supported accept types

## [29.22.1] - 2021-09-13
- Mark the `extensions` directory as a resource root in the Gradle plugin.
- Add a pegasus plugin config to use case sensitive path in dataTemplate generation and rest client generation

## [29.22.0] - 2021-09-09
- Allow customizing `MethodAdapterRegistry` (now called `MethodAdapterProvider`) via `RestLiConfig`.
  - Rename `MethodAdapterRegistry` to `DefaultMethodAdapterProvider` and add interface `MethodAdapterProvider`.
  - Deprecate the constructors with `ErrorResponseBuilder` for `BaseRestLiServer` and its dependent classes
- Update the data template generator command-line app to accept a list of resolver directories
  to use for resolving schema references.
  - Also refactored the app to use a CLI library instead of passing arguments using system properties.
  - Update `GenerateDataTemplateTask` to use the refactored command-line app `DataTemplateGeneratorCmdLineApp`.
- `ConstantQpsDarkClusterStrategy` post-prod fixes.
  - Change the type of `dispatcherOutboundTargetRate` in `DarkClusterConfig.pdl` from `int` to `float`.
  - `ConstantQpsRateLimiter` - Introduce randomness while maintaining constant per-period rate.

## [29.21.5] - 2021-09-09
- Fix a bug in `DataTranslator` where accessing non-existent fields under avro 1.10+ throws an exception.

## [29.21.4] - 2021-08-30
- Expose an API to build a URI without query params. Expose a local attr for passing query params for in-process calls.

## [29.21.3] - 2021-08-25
- Fix a bug in `SmoothRateLimiter` where `getEvents` will always return `0`.

## [29.21.2] - 2021-08-18
- Remove support for disabling request validation via headers since doing so can have dangerous side effects.

## [29.21.1] - 2021-08-18
- Enable skipping request and response validation via the use of request headers.

## [29.21.0] - 2021-08-17
- Fixed relative load balancer executor schedule cancellation due to silent runtime exception.

## [29.20.1] - 2021-08-12
- Minimize computations for requests resolved via in-process Rest.li servers

## [29.20.0] - 2021-08-10
- Fixed race condition when switching d2 load balancer strategies.

## [29.19.17] - 2021-08-09
- Fix bug in `ConstantQpsDarkClusterStrategy` that would call `ConstantRateLimiter.setRate` with an invalid burst value.

## [29.19.16] - 2021-08-09
- Add support for resolving from multiple schema source directories.
  - This change also introduces the concept of "source" and "resolver" directories when
    creating a `DataSchemaParser`. "Source" directories are used to parse/load the input
    schemas, while the "resolver" directories will only be used for resolving referenced
    schemas.

## [29.19.15] - 2021-08-09
- Provide the ability to set cookies and projection params in request context's local attributes to avoid
serializing/deserializing them for requests that are executed in-process.

## [29.19.14] - 2021-07-29
- Bump netty version to use ALPN support needed for JDK8u282.

## [29.19.13] - 2021-07-26
- Add support for validating aliased union members.
  - Union members originally didn't support custom properties and thus custom validation
    was not supported for union members. With aliased unions, members now support custom
    properties and thus can specify custom validation. Validation logic is updated to
    include custom validations on union members.

## [29.19.12] - 2021-07-22
- Add a predicate based bulk remove method for checkedMap.

## [29.19.11] - 2021-07-20
- Add compatibility level config for extension schema compatibility check.
   - "pegasusPlugin.extensionSchema.compatibility" is the compatibility level config for extension schema compatibility check.
      It supports following 4 levels:
     - "off": the extension schema compatibility check will not be run.
     - "ignore": the extension schema compatibility check will run, but it allows backward incompatible changes.
     - "backwards": Changes that are considered backwards compatible will pass the check, otherwise changes will fail the check.
     - "equivalent": No changes to extension schemas will pass.
   - If this config is not provided by users, by default the extension schema compatibility check is using "backwards".
   - How to use it: users could add 'pegasusPlugin.extensionSchema.compatibility=<compatibility level>' in the gradle.properties file
     or directly add this property '-PpegasusPlugin.extensionSchema.compatibility=<compatibility level>' to the gradle build.

- Revert "Relax extension schema check to make '@extension' annotation is optional for 1-to-1 injections."

## [29.19.10] - 2021-07-16
- Add hooks for customizing documentation (OPTIONS) response.
  - Documentation renderers now get the request headers and resource models available during rendering.

## [29.19.9] - 2021-07-15
- Relax extension schema check to make '@extension' annotation is optional for 1-to-1 injections.
- Update RestliRouter to allow "bq", "action" as query parameter name for finder, "q" as action parameter name for action

## [29.19.8] - 2021-07-02
- Define new Dark Cluster configs in d2 PropertyKeys

## [29.19.7] - 2021-06-30
- Fix equals() and hashCode() in ServiceProperties to support cluster subsetting

## [29.19.6] - 2021-06-28
- Fix validation logic for non-numeric float values (i.e. `NaN`, `Infinity`, `-Infinity`).
  - This affects the underlying implementation for the coercion modes defined by `CoercionMode`
    (the Javadoc for each mode has been updated accordingly).

## [29.19.5] - 2021-06-24
- Fix request builder generator to skip unstructured data sub resources correctly.
- Use the Java 7 diamond operator everywhere.

## [29.19.4] - 2021-06-23
- Do not apply Idea and Eclipse plugins.

## [29.19.3] - 2021-06-18
- More changes for Gradle 7 compatibility.
  - Add schemas as source set resources and rely on the Java plugin to copy them
    into the artifact instead of doing so directly, to avoid copying duplicates.
  - Change getter names in `GenerateDataTemplateTask` to conform to what Gradle 7
    requires and deprecate the old ones.

## [29.19.2] - 2021-06-17
- Allow client-side `RetriableRequestException` to be retried after `ClientRetryFilter`.

## [29.19.1] - 2021-06-09
- Add support for `CONSTANT_QPS` dark canary cluster strategy.

## [29.18.15] - 2021-06-02
- Fix race conditions in D2 cluster subsetting. Refactor subsetting cache to `SubsettingState`.

## [29.18.14] - 2021-05-27
- Use `class.getClassLoader()` instead of `thread.getContextClassLoader()` to get the class loader.

## [29.18.13] - 2021-05-27
- Remove one more `"runtime"` configuration reference.

## [29.18.12] - 2021-05-26
- Use daemon threads to unregister `TimingKey` instances.

## [29.18.11] - 2021-05-24
- Add support for returning location of schema elements from the PDL schema parser.

## [29.18.10] - 2021-05-24
- Introduce a readonly attribute on the `@Action` annotation.

## [29.18.9] - 2021-05-24
- Initial support for the modern `ivy-publish` plugin when producing data-template artifacts
  - Use of `ivy-publish` plugin requires Gradle 6.1+.
  - When `pegasus` and `ivy-publish` plugins are applied in concert,
    a new [Publication](https://docs.gradle.org/5.2.1/javadoc/org/gradle/api/publish/Publication.html) called `ivy` is created.
  - This Publication name can be modified by setting the `PegasusPublicationName` project property.
  - See [Ivy Publish Plugin](https://docs.gradle.org/5.2.1/userguide/publishing_ivy.html) for more information about the modern publishing mechanism.

## [29.18.8] - 2021-05-21
- Fix a bug in `ZKDeterministicSubsettingMetadataProvider` to make host set distinct.

## [29.18.7] - 2021-05-16
- Copy the input pegasus data schema when translating to avro.

## [29.18.6] - 2021-05-13
- Expose `getResourceClass` from `ResourceDefinition` interface.

## [29.18.5] - 2021-05-13
- Add `"http.streamingTimeout"` to `AllowedClientPropertyKeys`.

## [29.18.4] - 2021-05-06
- Replace `runtime` configuration with `runtimeClasspath` configuration in plugin for compatibility with Gradle 7.

## [29.18.3] - 2021-05-03
- Strictly enforce Gradle version compatibility in the `pegasus` Gradle plugin.
  - Minimum required Gradle version is now `1.0` (effectively backward-compatible).
  - Minimum suggested Gradle version is now `5.2.1`
- Fix TimingKey Memory Leak
- Fix bottlenecks in DataSchemaParser

## [29.18.2] - 2021-04-28
- Fix bug in generated fluent client APIs when typerefs are used as association key params
- Add debug log for cluster subsetting updates

## [29.18.1] - 2021-04-22
- Add fluent client API for `FINDER` and `BATCH_FINDER` methods.
- Fix a bug when converting `enableClusterSubsetting` config to Boolean in `ServicePropertiesJsonSerializer`.

## [29.18.0] - 2021-04-20
- Use host FQDN instead of nodeUri to get D2 subsetting metadata

## [29.17.4] - 2021-04-16
- Migrate the Rest.li release process from Bintray to JFrog Artifactory.
  - As of this version, Bintray will no longer host Rest.li releases.
  - Releases can be found on [LinkedIn's JFrog Artifactory instance](https://linkedin.jfrog.io/).

## [29.17.3] - 2021-04-15
- Releasing to test new CI behavior.

## [29.17.2] - 2021-04-11
- Fix the default value resolution logic in Avro schema translator to match the PDL behavior.

## [29.17.1] - 2021-04-02
- Add fluent client api for subresources
- Update fluent client APIs to include projection mask as input parameter.
- Update projection mask builder APIs to support updating the mask objects.
- Added support for checking if a nested type supports new ProjectionMask API before generating new typesafe APIs for them.
- Fix a typo in D2ClientConfig

## [29.17.0] - 2021-03-23
- Implement D2 cluster subsetting.

## [29.16.2] - 2021-03-22
- Fix an issue where in collection response, we did not fill in the default values in the metadata and paging metadata.

## [29.16.1] - 2021-03-17
- Add fluent client api for simple resource and association resource.
- Add support for generating projection mask as the mask data map.
- Fix UnmodifiableList wrap in d2 relative load balancer.

## [29.16.0] - 2021-03-10
- Add a ParSeq based CompletionStage implementation
- Bump minor version for internal services to pick up config change

## [29.15.9] - 2021-03-06
- Add separate configuration control for retrying RestRequest and StreamRequest.

## [29.15.8] - 2021-03-05
- Exclude 3XX http status from adding error logs during build error response from restli server.

## [29.15.7] - 2021-03-05
- Include accept header params when setting the response content type.

## [29.15.6] - 2021-03-04
- Fix bug that if a schema is an enum without any symbols, doc gen should handle it instead of throwing exception.

## [29.15.5] - 2021-03-03
- Fix content type header not set in case of `RestliResponseException` from non-streaming server.

## [29.15.4] - 2021-03-02
- Fix content type header not set in case of `StreamException` from Rest.li server.

## [29.15.3] - 2021-02-24
- Add support for update, partial_update, delete and get_all methods in fluent API bindings.
- Prevent `RetriableRequestException` from cascading to the indirect caller.

## [29.15.2] - 2021-02-19
- Add `UnionTemplate.memberKeyName()` to directly return the key name for a union member.

## [29.15.1] - 2021-02-18
- Cleanup compression code to reduce duplication and minimize memcopies

## [29.15.0] - 2021-02-17
- Always enable client compression filter so that responses can be decompressed. If the request already has an accept encoding header set do not overwrite it.

## [29.14.5] - 2021-02-11
- Shortcircuit already serialized projection params

## [29.14.4] - 2021-02-10
- Deal with status code 204, when we see 204 in error path, we will not return data (from data layer only)

## [29.14.3] - 2021-02-10
- Add PathSpecSet, an immutable set of PathSpecs that is convenient to use when building logic based on Rest.li projection

## [29.14.2] - 2021-02-03
- Exclude conflicting velocity engine dependency.

## [29.14.1] - 2021-01-31
- Gracefully degrade symbol tables when server node URI is null

## [29.14.0] - 2021-01-29
- Generate fluent client APIs get and create methods of collection resources.
- Encode JSON values in PDLs deterministically:
  - Annotation maps are now sorted alphabetically (to arbitrary depth).
  - Default values of fields with record type are sorted by the field order of the record schema.

## [29.13.12] - 2021-01-29
Fix a bug of losing HTTP status code when a retriable response goes through ClientRetryFilter

## [29.13.11] - 2021-01-27
- Update 'CreateOnly' and 'ReadOnly' javadocs to be more accurate that the validation is performed by 'RestLiValidationFilter'.
- Fix memory leak in `CheckedMap` when one map is used to create multiple record templates.
  - Change listener list now clears finalized weak references when it detects any change listener was finalized or when listeners are notified.

## [29.13.10] - 2021-01-20
- Fix bug which prevented using the `@PathKeyParam` resource method parameter annotation for a non-parent path key (i.e. path key defined in the same resource).
  - Users will no longer have to rely on `@PathKeysParam` as a workaround.
- Expose resource method parameters in the `FilterRequestContext` interface.
- Fix bug in `DataComplexTable` that breaks `Data::copy` if there are hash collisions.
  - Hashcodes for `DataComplex` objects are generated using a thread local, and there can be collisions if multiple threads are used to construct a `DataComplex` object.

## [29.13.9] - 2021-01-13
- Add max batch size support on Rest.li server.
  - Introduce the `@MaxBatchSize` annotation, which can be added on batch methods.
  - Add batch size validation based on the allowed max batch size.
  - Add resource compatibility check rules for the max batch size.

## [29.13.8] - 2021-01-13
- Fix a critical bug in `RetryClient` to set retry header instead of adding a value to retry header

## [29.13.7] - 2021-01-08
- Java does not allow inner class names to be same as enclosing classes. Detect and resolve such naming conflits for unnamed inner types (array, map and union).

## [29.13.6] - 2021-01-07
- Fix for "pegasus to avro translation of UnionWithAlias RecordFields does not have field properties"

## [29.13.5] - 2021-01-06
- Improve logging when conflicts are detected during parsing. Update translate schemas task to look in the input folder first when resolving schemas.

## [29.13.4] - 2021-01-07
- Change listeners should not be added to readonly maps.

## [29.13.3] - 2021-01-06
- Add support for accessing schema statically from generated template classes and for getting symbol properties from enum schema properties.
- Fix extra whitespaces at the end of the line in the pegasus snapshot files.

## [29.13.2] - 2020-12-23
- Implement overload failure client-side retry.

## [29.13.1] - 2020-12-14
- Fix the restriction of empty union validation from wide open to only allow when there is a projection in the union

## [29.13.0] - 2020-12-12
- Change AvroUtil to use newCompatibleJsonDecoder from avro-util
- Bump `javax.mail:mail` dependency from `1.4.1` to `1.4.4` to avoid classloader issues in `javax.activation` code with Java 11.
- Bump arvo compatibility layer `avroutil` dependency from `0.1.11` to `0.2.17` for Arvo Upgrade HI.
- Setup the base infra for generating new fluent api client bindings.

## [29.12.0] - 2020-12-02
- Add a boolean flag as header for symbol table request to avoid conflict with resource requests.

## [29.11.3] - 2020-11-25
- Enable cycle check when serializing only when assertions are enabled, to avoid severe performance degradation at high QPS due to ThreadLocal slowdown.

## [29.11.2] - 2020-11-23
- Enhance request symbol table fetch.
  - Return null if uri prefix doesn't match.
  - If the fetch call 404s internally store an empty symbol table and return null. This will avoid repeated invocations to services that are not yet ready to support symbol tables

## [29.11.1] - 2020-11-20
- When we do validation on response, in the past empty unions will fail the validation and client will
  - fail. Now we do not treat empty union as a failure, and just return the empty map as is.
  - Also, if there are projection, the projection will apply to empty union if it is projected.

## [29.10.1] - 2020-11-19
- Fix bug where records wrapping the same map were not updated when setter was invoked on one record.

## [29.10.0] - 2020-11-18
- Fix relative load balancer log. Bumping the minor version so that it can be picked up by LinkedIn internal services.

## [29.9.2] - 2020-11-16
- Implemented doNotSlowStart in relative load balancer.

## [29.9.1] - 2020-11-12
- Performance improved: add lazy instantiation of Throwable objects for timeout errors

## [29.9.0] - 2020-11-10
- By default, Pegasus Plugin's generated files (for GenerateDataTemplateTask and GenerateRestClientTask Gradle Tasks) are created with lower case file system paths. (There is an optional flag at the Gradle task level to change this behavior.)

## [29.8.4] - 2020-11-09
- Adding required record field is allowed and should be considered as backward compatible change in extension schemas.

## [29.8.3] - 2020-11-09
- Support symbolTable requests with suffixes

## [29.8.2] - 2020-11-06
- Fix bug: if there is no input schema, do not run pegasusSchemaSnapshotCheck. The check statement was wrong.

## [29.8.1] - 2020-11-05
- Check whether schemas exist or not before running pegasusSchemaSnapshotCheck task

## [29.8.0] - 2020-10-29
- Empty commit to bump pegasus minor version. LinkedIn internal service needs the new minor version to prevent client version downgrade, since the LinkedIn internal services only notice on minor version discrepancy.

## [29.7.15] - 2020-10-23
Log Streaming Error or Timeout Error in Jetty SyncIOHandler

## [29.7.14] - 2020-10-22
- Improve performance of schema format translator.

## [29.7.13] - 2020-10-22
- Check if debug logging is enabled before calling debug log message in TimingContextUtil to avoid unnecessary exception instantiation.
- Improve relative load balancer logging.

## [29.7.12] - 2020-10-20
- Fix the bug of not propagating schema properties in typeref with UnionWithAlias during pegasus to avro translation

## [29.7.11] - 2020-10-19
- Clear the destination directory for generateRestClientTask before the task runs.
- Add 'ExtensionSchemaAnnotationHandler' for extension schema annotation compatibility check
- Set javac source and target compatibility of dataTemplate compile task to "1.8" as the cogen changes in 29.7.0 is using Java 8 features.

## [29.7.10] - 2020-10-15
- Minimize memory copies and object creation during encoding.
- Use String switch instead of map lookup in traverse callback for better performance
- Reset isTraversing when cloning
- Cache data objects in wrapped mapped/lists lazily on get.
- Compute dataComplexHashCode lazily for DataList and DataMap

## [29.7.9] - 2020-10-15
- Add partition validation when getting relative load balancer metrics.
- Extend checkPegasusSchemaSnapshot task to be enable to check schema annotation compatibility.
- The annotation compatibility will be triggered if SchemaANnotationHandler config is provided.
- Update SchemaAnnotationHandler interface to have a new api - annotationCompatibilityCheck, which can be used to check the custom annotation compatibility check.

## [29.7.8] - 2020-10-12
- Encoding performance improvements

## [29.7.7] - 2020-10-06
- Adding dark cluster response validation metrics

## [29.7.6] - 2020-10-05
- Fix bug referring to coercer before registration.

## [29.7.5] - 2020-10-05
- Add an option to configure ProtoWriter buffer size. Set the default to 4096 to prevent thrashing.
- Use an identity hashmap implementation that uses DataComplex#dataComplexHashCode under the hood for better performance

## [29.7.4] - 2020-10-03
- Fix bug affecting record fields named "fields".

## [29.7.3] - 2020-10-02
- Bump `parseq` dependency from `2.6.31` to `4.1.6`.
- Add `checkPegasusSchemaSnapshot` task.
   - The task will be used to check any pegasus schema compatible and incompatible changes.
   - The pegasus schema may or may not be part of a Rest.li resource.
   - The task will be triggered at build time, if user provides gradle property: `pegasusPlugin.enablePegasusSchemaCompatibilityCheck=true`.
- Fix task caching issue by using the output file instead of task properties. Task properties will not reflect the correct state when a task is loaded from cache.
- Add method in ParseResult class to get base schema
- Fix collectionMetadata missing link issue when collection count is 0

## [29.7.2] - 2020-09-25
- Move from lambdas to explicit change listeners since lambda garbage collection is unreliable in Java

## [29.7.1] - 2020-09-24
- Handle setting map change listener correctly on copy and clone

## [29.7.0] - 2020-09-23
- Generate code to avoid reflection and map access to improve generated data template runtime performance.
    - Use member variables to avoid looking in to DataMap for every read calls. ChangeListeners on Map added to invalidate these fields when underlying map changes.
    - Use optimized coercion methods for primitive fields.
    - Use generated constants for default values for faster lookup.

## [29.6.9] - 2020-09-22
- Mitigate schema parsing performance regression introduced in `29.5.1` by reusing `ParseResult` instances
  in `DataSchemaParser` to avoid unnecessary `TreeMap` sorting.
- Include `HttpStatus` code while throwing `IllegalArgumentException`.
- Add monitoring metrics for relative strategy in DegraderLoadBalancerStrategyV3Jmx

## [29.6.8] - 2020-09-22
- Optimized logger initialization in d2 degrader.

## [29.6.7] - 2020-09-18
- Added async call to Zookeeper in backup request client.

## [29.6.6] - 2020-09-17
- Loosen `ReadOnly`/`CreateOnly` validation when setting array-descendant fields in a patch request.
- Add `generatePegasusSchemaSnapshot` task.
- Remove `final` from nested generated classes, such as inline unions.

## [29.6.5] - 2020-09-09
- Update `RestLiValidationFilter` and `RestLiDataValidator` to expose creation of Rest.li validators.

## [29.6.4] - 2020-09-08
- Fix inconsistent issue in extension schema file names: from `Extension` to `Extensions`
- Fix a bug in `FileFormatDataSchemaParser` and remove `isExtensionEntry` method call to simplify the logic.
- Update `ExtensionSchemaValidationCmdLineApp` with more validations.

## [29.6.3] - 2020-09-03
- Updated HTTP/2 parent channel idle timeout logging level to info from error.

## [29.6.2] - 2020-08-31
- Updated d2 client default config values.

## [29.6.1] - 2020-08-31
- Update R2's HTTP client API to support other Netty `EventLoopGroup` in addition to `NioEventLoopGroup`.
- Fix a `RetryClient` bug where `NullPointerException` is raised when excluded hosts hint is not set at retry.
- Update `ExtensionSchemaAnnotation` schema: remove resource field, add `versionSuffix` as an optional field.

## [29.6.0] - 2020-08-28
- Refactored the existing d2 degrader load balancer.
- Implemented a new load balancer that is based on average cluster latency.

## [29.5.8] - 2020-08-27
- Make `ChangedFileReportTask` gradle task compatible with Gradle 6.0

## [29.5.7] - 2020-08-26
- Add pdsc support for `ExtensionsDataSchemaResolver` for support legacy files in pdsc.
- Add/patch default values in Rest.li responses, controlled by the `$sendDefault` flag in the URL or server configs.

## [29.5.6] - 2020-08-21
- Add a constructor for `DataSchemaParser`, which is able to pass `ExtensionsDataSchemaResolver` to
  the `DataSchemaParser` to parse schemas from both `extensions` and `pegasus` directories.

## [29.5.5] - 2020-08-21
- Updated File and class path DataSchemaResolvers to resolve extension schemas from `/extensions` directory if specified.
- Added `DarkGateKeeper` to enable users to provide custom implementation to determine if requests are to be dispatched to dark clusters.

## [29.5.4] - 2020-08-17
- Increase default timeout for symbol table fetch to 1s.

## [29.5.3] - 2020-08-17
- Treat `ReadOnly` required fields as optional in `PARTIAL_UPDATE`/`BATCH_PARTIAL_UPDATE` patches.
  This will allow such patches to set fields containing descendent `ReadOnly` required fields, which wasn't possible before.

## [29.5.2] - 2020-08-17
- Allow publishing unstable release candidate versions of Rest.li (e.g. `1.2.3-rc.1`) from non-master branches.
    - It's _strongly_ suggested to only use a release candidate version if you have a specific reason to do so.
- Put extension schemas into the `dataTemplate` jar under `/extensions` path instead of putting them into the `extensionSchema` jar.
- Remove stacktrace when convert between `RestException` and `StreamException`.

## [29.5.1] - 2020-08-14
- Provide an option in `SmoothRateLimiter` to not drop tasks if going above the max buffered. Dropping tasks might be more diruptive to workflows compared to just not ratelimit.
- Fix non-deterministic issues on generated java files to solve build performance issues.

## [29.5.0] - 2020-08-12
- Add Callback method for `ClusterInfoProvider.getDarkClusterConfigMap`.

## [29.4.14] - 2020-08-11
- Provide an option to set an overridden SSL socket factory for the default symbol table provider.

## [29.4.13] - 2020-08-11
- Undeprecate some Rest.li client methods since we do want the ability to set default content/accept types at the client level.

## [29.4.12] - 2020-08-10
- Directly fetch `DarkClusterConfigMap` during startup, before registering `ClusterListener`.

## [29.4.11] - 2020-08-06
- Relax validation of read-only fields for upsert usecase: UPDATE used for create or update. Fields marked as ReadOnly will be treated as optional for UPDATE methods.

## [29.4.10] - 2020-08-05
- Allow `RestRestliServer` and `StreamRestliServer` to throw `RestException` & `StreamException` with no stacktrace.

## [29.4.9] - 2020-08-04
- Add missing `ClusterInfoProvider` implementations in `ZKFSLoadBalancer` and `TogglingLoadBalancer`.

## [29.4.8] - 2020-08-04
- Add identical traffic multiplier strategy for dark clusters to enable identical traffic across all dark clusters.

## [29.4.7] - 2020-07-30
- Add support for configuring fields that are always projected on the server. Configs can be applied for the entire service, resource or method level.

## [29.4.6] - 2020-07-29
- Provide a default symbol table provider implementation that doesn't use symbol tables for requests/responses of its own, but is able to retrieve remote symbol tables to decode responses from other services (#357)
- Provide public method in the `AbstractRequestBuilder` for adding field projections (#353)

## [29.4.5] - 2020-07-21
- Update `ExtensionSchemaValidation` task to check extension schema annotation (#254)
- Improve performance of uri mask encoding and decoding (#350)

## [29.4.4] - 2020-07-02
- Disable string interning in Jackson JSON since it causes GC issues (#346)

## [29.4.3] - 2020-07-01
- Add an option (enabled by default) to gracefully degrade on encountering invalid surrogate pairs during protobuf string serialization (#334)

## [29.4.2] - 2020-06-25
- Update Pegasus Plugin's `CopySchema` tasks to delete stale schemas (#337)

## [29.4.1] - 2020-06-24
- Relax visibility of some methods in PDL schema parser to allow extending it.

## [29.4.0] - 2020-06-23
- Add new changelog (`CHANGELOG.md`) and changelog helper script (`./scripts/update-changelog`).
- Fix a bug in batch_finder that ignored metadata when generating IDL. This change will result in IDL change without source change, but the change will be considered backwards compatible. There will not be any change to runtime behavior of the server.

## [29.3.2] - 2020-06-19
- Fix dark cluster startup problem (#330)
- Only include the underlying exception message in BufferedReaderInputStream instead of rethrowing the original exception (#329)

## [29.3.1] - 2020-06-16
- Allow the client to specifically request the symbol table it needs the server to encode the response in. This is useful for cases like remote clients where the client wants the server to encode the response with the symbol table it has, instead of the one the server has (#327)
- Fix non-deterministic issue for annotations in the generated data template code by using right schema location for typeref class template (#323)

## [29.3.0] - 2020-06-11
- Bump minor version to release LastSeen load balancer fix (#325)
- Enabling warmup in LastSeen Loadbalancer (#313)

## [29.2.5] - 2020-06-10
- fix dark cluster strategy refresh (#321)

## [29.2.4] - 2020-06-04
- Introduce pegasus-all meta-project (#322)

## [29.2.3] - 2020-06-04
- Add option to force fully qualified names in PDSC encoder. (#319)

## [29.2.2] - 2020-06-03
- Expose an additional constructor that can accept pre-generated symbol table (#320)
- Roll back Bintray upload on failure in Travis (#318)

## [29.2.1] - 2020-06-03
- Release 29.2.1 (#317)
-  Fix BatchFinder response URI builder to take projection fields properly (#312)

## [29.2.0] - 2020-06-02
- Reclassify -all as -with-generated for restli-int-test-api (#316)

## [29.1.0] - 2020-06-02
- Release 29.1.0 (#315)

## [29.0.2] - 2020-06-01
- add protobuf stream data codec and use in PROTOBUF2 (#308)
- Handle valueClass NPE issue and give warnings (#303)
- Add protobuf stream data decoder (#306)
- fix smile data codec for data list and protobuf codec for optimizations (#309)
- Refactor restli stream data decoder (#292)

## [29.0.1] - 2020-05-20
- Implement Host Override List feature in D2. (#299)

## [29.0.0] - 2020-05-19
- Stop publishing test artifacts & internal modules (#295)
- Exclude 2 flaky tests in Travis CI (#304)
- Add schemas for relative load balancing. (#285)
- Enable tests in Travis, add some exclusions (#275)

## [28.3.11] - 2020-05-19
- Release 28.3.11 (#302)
- Reuse the expensive Jackson factories in the JsonBuilder to reduce garbage. Provide an option to encode to a custom writer to allow writing directly to files in some cases for reasons of efficiency (#300)

## [28.3.10] - 2020-05-19
- implement ClusterInfoProvider.getDarkClusterConfigMap in TogglingLoadBalancer (#301)
- Create a lightweight representation of InJarFileDataSchemaLocation (#293)

## [28.3.9] - 2020-05-18
- R2 resiency change to timeout HTTP/2 parent channel creation (#297)
- change errors in ChannelPoolLIecycle to warn (#294)

## [28.3.8] - 2020-05-14
- Fix max content-length bug  and add info log to log http protocol used per service
- Refactor stream encoder and Add data encoder for protobuf (#266)

## [28.3.7] - 2020-05-07
- change transportClientProperties to optional (#291)

## [28.3.6] - 2020-05-06
- Use "pro" secure strings in Travis config (#290)
- Dequote secure strings in Travis config (#289)
- Set up automated releases, improve helper scripts (#286)

## [28.3.5] - 2020-05-04
- Provide an option to serialize floating point values (aka floats and doubles) using fixed size integers. (#282)

## [28.3.4] - 2020-05-04
- Release 28.3.4 version (#284)

## [28.3.3] - 2020-04-30
- Bump JMH version, fix sub-project test dependency (#276)
- Fix Retry Client Bug by inspecting the inner exceptions to check if RetriableRequestException is wrapped inside another exception (#271)
- Update avroUtil to com.linkedin.avroutil1:helper-all:0.1.11 (#274)
- fix failing DarkClustersConverter tests. (#273)
- bump to 28.3.3 (#272)
- DarkCluster schema changes: adding multiplierStrategyList and transportClientProperties (#264)

## [28.3.2] - 2020-04-28
- Optimize Zookeeper Read during Announcing/DeAnnouncing (#267)
- Fix the pathSpec generated for typeRef DataElement (#270)
- Switch the order of resolving schemas. (#269)
- Fix Maven publication for extra artifacts again (#268)
- Fix Maven publication for extra artifacts (#261)
- PDL Migration. (#265)

## [28.3.1] - 2020-04-19
- Right size hashmaps accounting for load factor in ProtobufDataCodec (#263)

## [28.3.0] - 2020-04-18
- Add resource key validation while parsing (#239)

## [28.2.8] - 2020-04-17
- Switch from commons-lang3 to commons-text. (#262)

## [28.2.7] - 2020-04-16
- Protobuf parsing performance improvements. (#260)
- Fix #241: Upgradle to Gradle 5.2.1 (#242)

## [28.2.6] - 2020-04-13
- publish the test artifacts for darkcluster (#259)

## [28.2.5] - 2020-04-10
- Extend the use of argFile and pathing Jar across all tasks. (#257)
- Bump spring dependencies to 3.2.18.RELEASE (#256)
- darkcluster - basic framework for sending dark cluster requests (#213)

## [28.2.4] - 2020-04-09
- Enable arg file for resolver path to avoid arg too long errors. (#255)
- Feature/zk batch with jitter (#240)

## [28.2.3] - 2020-04-08
- Release 28.2.3 (#253)
- Move CopySchemaUtil and its test to util package and make CopySchemaUtil public (#252)
- Add support for custom properties on anonymous complex schemas. (#250)

## [28.2.2] - 2020-04-06
- Release 28.2.2 (#251)
- Fix #245: Refactor RestRequestBuilderGenerator to accept arg file with '@' syntax (#246)

## [28.2.1] - 2020-04-03
- Disable Request Waiter Timeout if not within fail fast threshold and Optimize SingleTimeout runnable to not capture callback to deal with the future.cancel behaviour in ScheduledExecutor (#249)
- Add local-release script (#234)

## [28.2.0] - 2020-03-31
- Release 28.2.0 (#244)
- Replacing Usages of DataMapBuilder in AbstractJacksonDataCodec (#167)
- Add extensionSchema validation task (#235)

## [28.1.36] - 2020-03-29
- Fix the issue introduced when trim whitespace started clobbering newlines between elements. (#237)

## [28.1.35] - 2020-03-29
- Fix build (#243)
- Release 28.1.35 (#238)
- Enforce schema properties order in JsonBuilder so Pegasus schema and Avro schema properties would have deterministic order (#233)
- Configure Bintray release task (#232)

## [28.1.34] - 2020-03-23
- Raise exception in ValidateSchemaAnnotationTask if any handler configured not found. (#223)
- Use manifest-only JAR for TranslateSchemasTask (#230)

## [28.1.33] - 2020-03-19
- Bump ZK Client from 3.4.6 to 3.4.13 (#229)

## [28.1.32] - 2020-03-19
- Revert (#228)

## [28.1.31] - 2020-03-19
- Trim PDL files only when writing the final output to ensure the validation step doesn't fail. (#227)

## [28.1.30] - 2020-03-18
- Bump version in preparation for release (#226)
- Update the default connection creation timeout to 10s from 2s (#225)

## [28.1.29] - 2020-03-17
- Improvements to the PDL translation task. (#224)
- Add resource key validation while parsing (#217)
- Change ValidateSchemaAnnotationTask to ignore exception or errors durng initializaing class by classLoader (#218)

## [28.1.28] - 2020-03-12
- Introduce an object creation timeout to completely de-couple channel pool from the behaviour of object creator (lifecycle). This will make sure that channel pool always claim its objects slot if the object creator did not return in time. Fix the SSL Completion event handling. Fix the error handling when channel creation fails so that it wont create a retry creation task in the rate limitor (#219)
- Revert "Enforce schema properties order in JsonBuilder so Pegasus schema and Avro schema properties would have deterministic order (#207)" (#220)
- pretty print json in pdl builder (#216)
- Enforce schema properties order in JsonBuilder so Pegasus schema and Avro schema properties would have deterministic order (#207)

## [28.1.27] - 2020-03-08
- Fix bug in classpath resolver when there are PDSC-PDL cross references. (#215)
- Fix Classpath resource schema resolver to handle pdl resources. (#214)
- Add Streaming Timeout to TransportClientProperties (#212)

## [28.1.26] - 2020-03-06
- Release 28.1.26 (#211)
- Refactored DataSchemaRichContextTraverser(#204)
- Fix data translator for translating Pegasus non-union field to Avro union and vise versa (#202)

## [28.1.25] - 2020-02-28
- AsyncPool Improvements and Fixes: (#185)

## [28.1.24] - 2020-02-28
- Fix a bug in PDL encoder that would cause transitively referenced types to be added to imports. (#199)

## [28.1.23] - 2020-02-28
- Use InputStream/OutputStream instead of ObjectInput/ObjectOutput for Avro Binary codec (#197)
- Use pathspec object as hash map key in ResolvedPropertiesReaderVisitor (#193)
- fix template exception while request build init for custom response types (#179)
- Fix for PDL schema cyclic referencing detection in "includes" and "typeref"  (#192)
- Add check to prepare-release script to prevent branch conflicts (#195)

## [28.1.22] - 2020-02-26
- Use PathingJarUtil to prepare manifest-only JAR for invocations of GenerateRestClientTask (#189)
- bump gradle.properties (#191)
- ClusterInfoProvider API and implementation (#181)

## [28.1.21] - 2020-02-25
- Release 28.1.21 (#190)
- Update AVRO compat layer in pegasus to use adapters from avro-util (#175)

## [28.1.20] - 2020-02-25
- Fix for DataTranslator where PegasusToAvroDefaultFieldTranslation mode throws unexpcted exception (#178)

## [28.1.19] - 2020-02-25
- Fix issues when encoding/parsing custom properties in PDL. (#187)
- Fix prepare-release script so that tag is deleted on failed push (#186)

## [28.1.18] - 2020-02-24
- Release 28.1.18 (#184)
- Fix bug in PDL encoding logic for imports when there are conflicting types in use. (#183)
- Configure Travis to only build on master and tags (#182)
- Fix the release script, release from tags (#180)
- Enhance MockCollectionResponseFactory to mock custom metadata in collection responses. (#174)
- Create .travis.yml (#172)
- Disable flaky tests (#171)

## [28.1.17] - 2020-02-13
- Release version 28.1.17 (#170)
- Fix bug that would cause pdl encoder to import types conflicting with inline types. (#169)
- Properly escape union member aliases and enum symbols in PDL (#168)
- Change PDL property encoding logic to expand maps with multiple entries (#166)

## [28.1.16] - 2020-02-11
- Enable debug logging only in DEBUG mode in ResolvedPropertiesReaderVisitor

## [28.1.15] - 2020-02-04
- Provide an optional fast path for codecs to read ByteStrings directly, and provide an implementation utilizing this for Protobuf.
Close the underlying stream sources when using the Protobuf codec
Improve string decoding performance for Protobuf codec
Add an option in symbol table provider to pass in a list of overridden symbols if clients don't want to use the RuntimeSymbolTableGenerator for some reason
- Emit method definitions in deterministic order

## [28.1.14] - 2020-01-10
- Support long for HTTP_IDLE_TIMEOUT and HTTP_SSL_IDLE_TIMEOUT
- Add support for delegated property in ClusterProperties, and new property keys for Dark Cluster multiplier

## [28.1.13] - 2020-01-31
- Fix the logic in SchemaParser to match SchemaResolver when loading pegaus schemas from jar files.

## [28.1.12] - 2020-01-28
- Fix server error response for bad input entity, to return bad request in response
- Fix bug that would cause schema translator to fail if run on MPs with multiple modules containing PDSC files.

## [28.1.11] - 2020-01-27
- Publish all schemas as-is, temporarily publish translated PDSC schemas
- Suppress logging statement in ResolvedPropertiesReaderVisitor
- Guard against NPE when RestLiServiceException is initialized with null status. Also updates server logic to handle null status cleanly.
 This change should not affect clients based on the following points:
  -> Today, if status is null, the framework code throws NPE when serializing the exception or converting to Response. This NPE is then handled as unknown error and returned as 500 error to client.
  -> With this change, clients will still get 500 error, with the correct error details serialized in the body.
  -> The Rest.Li filter chain will continue to see the actual exception (with null status), thus not affecting any custom filter behavior.
- Add "validateSchemaAnnotationTask" gradle task for validating schema annotations

## [28.1.10] - 2020-01-13
- Expose some utility methods for use elsewhere
- Expose action return type in FilterRequestContext

## [28.1.9] - 2020-01-09
- Add back TransportHealthCheck constructor and deprecate it.

## [28.1.8] - 2020-01-09
- Do not share RequestContext and wire attributes between D2 healthcheck requests.

## [28.1.7] - 2020-01-06
- Expose some methods in the protobuf codec for inheritors to customize

## [28.1.6] - 2020-01-06
- Add additional debug logging to RequestFinalizers.

## [28.1.5] - 2019-12-23
- Empty commit to trigger a new release

## [28.1.4]
- Clean up some javadocs to reduce javadoc warnings during build
- Add schemaAnnotationProcessor and other related schema annotation processing feature

## [28.1.3] - 2019-11-06
- Revert "dark cluster schema and serializer changes"
- Use PDL in examples and restli-example-api
- Dark cluster schema and serializer changes
- Use unmodifiableSortedMap instead of unModifiableMap when wrapping treemaps to ensure that bulk copy operations are faster when copied onto another TreeMap.
- Fix convertToPdl tool validation issue when there is an extra whitespace in the beginning of a line in doc.
- add DataSchemaRichContextTraverser, introduce DataSchemaRichContextTraverser.SchemaVisitor interface

## [28.1.2] - 2019-12-12
- Lower latency instrumentation logs from warn to debug

## [28.1.1] - 2019-12-11
- Minor performance optimization to use indexOf instead of split. Split internally uses indexOf but only if the regex length is 1. Since | has a special meaning in regex land, we need to escape it, meaning that we end up losing out on this internal optimization.
- Update convertToPdl tool to preserve source history

## [28.1.0] - 2019-12-10
- Add integration testing for latency instrumentation
- Add instrumentation timing markers for projection mask application
- Add support for framework latency instrumentation
- Mark data schema directories as resource roots

## [28.0.12] - 2019-12-09
- Improve debug logging in RequestFinalizers.

## [28.0.11] - 2019-11-27
- Make symbol table path handling more robust to handle D2 URIs
- Allow incompatible rest model changes in restli-int-test-server
- PDL: Verify translated schemas by writing to temporary location and comparing them with source schemas. Enable option to keep source files or remove them.

## [28.0.10] - 2019-11-12
- Support to set up custom request handler in restli config
- Optimize Jackson data decoder to create DataMaps with proper capacity.
- Fix the failing AclAwareZookeeperTest

## [28.0.9] - 2019-11-26
- Fix a bug in query tunneling affecting URIs containing keys with encoded reserved Rest.li protocol 2.0.0 characters

## [28.0.8]
- Add debug logging to client/server request finalizers.

## [28.0.7]
- Allow waiting until zookeeper connection establishes before ZKconnection#start returns
- Adds equality/hash methods for ActionResult class
- Disable setup and teardown for SymlinkAwareZooKeeperTest
- Bump TestNG to enable @Ignore annotation
- Log the mime types that failed to parse with error log level before propagating exception. Fix bugs in encoding symbol table names with special characters
- add clone method, resolvedProperties to DataSchema methods, changes to MapDataSchema PathSpec to have key fields reference, Add pathSpec format validation function
- Add configuration to disable generating deprecated protocol version 1.0.0 request builders.
- Disable SymlinkAwareZooKeeperTest temporarily

## [28.0.6] - 2019-11-21
- Remove accept header for individual mux requests
- Support for data template schema field string literals in PDL

## [28.0.5] - 2019-11-20
- Update SchemaToPdlEncoder interface

## [28.0.4] - 2019-11-19
- Add the HeaderBasedCodecProvider interface back
- Reference symbol table provider and handler implementation to enable symmetric protocol for exchanging symbol tables between rest.li services
- Fix NPE in URIElementParser and add more undecodable tests to TestURIElementParser
- defined SchemaAnnotationHandler interface for custom users to implement

## [28.0.3]
- Clean generated dirs in Pegasus sub-projects without the Pegasus plugin
- Fix incorrect PDL encoding for bytes default values

## [28.0.2] - 2019-11-11
- Expose some methods for use in pemberly-api

## [28.0.1] - 2019-11-05
- Remove RestResponse.NO_RESPONSE.

## [28.0.0] - 2019-10-25
- Wire up symbol table providers on client and server. This will enable us to inject a runtime symbol table lookup system via container Add tool to generate symbol tables at runtime

## [27.7.18] - 2019-11-05
- Introduce RequestFinalizer API and server-side RequestFinalizerTransportDispatcher.
- Add ClientRequestFinalizerFilter to finalize requests on the client-side.

## [27.7.17] - 2019-11-04
- Remove dependency on protobuf artifact, to avoid runtime errors with dependencies of pegasus that may depend on other protobuf versions. Instead extract relevant parts of protobuf code into a separate li-protobuf module.
- Support configurable encoding styles for PDL

## [27.7.16]
- Make Pegasus Java 8 and Java 11 compatible

## [27.7.15] - 2019-10-24
- Update PDL schema parser and encoder to allow properties with dots in them. Properties with dots are escaped using back-tick ` character.
- Optimizing UriBuilder & validatePathKeys.

## [27.7.14]
- LoadBalancer: when using empty d2ServicePath, use the default service path. Consolidating code between ZKFS and LastSeen code paths

## [27.7.13] - 2019-10-25
- Remove configure task for data template
- Adding JMX registration of D2 LoadBalancer components

## [27.7.12] - 2019-10-22
- Right size the DataObjectToObjectCache used for wrapped fields to avoid wasting memory.

## [27.7.11]
- Adding initial capacity to clientsToLoadBalance list and trackerClientMap to avoid expensive resize operations.
- Generate ToString, Equals and Hashcode for PathSegment

## [27.7.10] - 2019-10-21
- Introduce new protobuf codec

## [27.7.9] - 2019-10-16
- Fix codec bugs and improve deserialization performance

## [27.7.8] - 2019-10-17
- Add support to compare DataList without order, by sorting them with a comparator.

## [27.7.7]
- Refactor disruptor code path to use common method for adding disrupt context
- Update GenerateRestModelTask to have an ordered set for setWatchedInputDirs input property.

## [27.7.6] - 2019-10-09
- Move NO_RESPONSE to RestResponseFactory.

## [27.7.5]
- Fixing JSON deserialization bug  while setting partition weight via JMX using JSON format

## [27.7.4] - 2019-09-23
- Add varargs constructor to primitive arrays.

## [27.7.3] - 2019-10-08
- Fixing possible IC corruption issue in dealing with cached CompletionStage.

## [27.7.2]
- Small improvement in test performance for the ClockExecutor

## [27.7.1] - 2019-09-25
- Adding cache to store 'serviceName' in Request and 'protocolVersion' in RestClient.
- Do not allow to set weight through ZooKeeperAnnouncerJmx when D2 Server is announced to multiple partitions and Fix the bug of resetting the D2 Partition to Default Partition in Zookeeper

## [27.7.0] - 2019-09-30
- Make ApplicationProtocolConfig constructor backward compatible to previous versions of netty by using the deprecated versions present in the previous versions.

## [27.6.8] - 2019-09-30
- Update call tracking filter to handle batch finder
- LastSeenLoadBalancer: adding back again support for custom d2ServicePath on ZK. Add support for BackupStoreFilePath to LastSeen

## [27.6.7] - 2019-09-26
- Rephrase PDL parser error messages to make them more clear
- Revert "LastSeenLoadBalancer: adding support for custom d2ServicePath on ZK. Add support for BackupStoreFilePath to LastSeen"

## [27.6.6]
- LastSeenLoadBalancer: adding support for custom d2ServicePath on ZK. Add support for BackupStoreFilePath to LastSeen
- Disable SymlinkAwareZooKeeperTest temporarily

## [27.6.5] - 2019-09-16
- Add an configurable option to exclude some typeRef properties from translating into avro schema

## [27.6.4] - 2019-09-13
- Fix bug when convert .pdsc to .avsc with override namespace prefix in DENORMALIZE model

## [27.6.3] - 2019-09-10
- Streaming Timeout (Idle Timeout) Implementation - as detiled in the document @ https://docs.google.com/document/d/1s1dNjqoUkmo2TZ4mql4utHB2CZwe14JW8EPZFRmxOwA
- Add RestRequest support in Pipeline V2 and Convert FullyBuffered StreaRequest to RestRequest for better efficiency.

## [27.6.2] - 2019-09-18
- Add a varargs constructor to *Array classes.

## [27.6.1] - 2019-09-20
- Fix unreliable unit tests in TestRestClientRequestBuilder
- Fix PDL encoder logic for writing import statements
- Pegasus tmc release job is failing with out of memory error while runing r2-int-tests. Try to reduce the overall memory
footprint by having one netty eventloop group and cleaning up objects created in tests
- Add a config field in RestLiMethodConfig to validate query parameters
- Set the initial capacity of records and union templates to reduce unused memory.
- Int-tests for Alternative Key Server Implementation
- Fix PDL bug for encoding escaped package declarations
- Fix PDL bug for encoding schemas with empty namespaces
- Fix PDL bug for encoding self-referential schemas in DENORMALIZE mode
- Support aliases for named data schemas in PDL

## [27.5.3] - 2019-09-05
- Temporarily add the "java" keyword poped from TypedUrn as excluded property keyword for pdsc to avsc schema translation

## [27.5.2] - 2019-09-05
- Properly recover from corrupted uriValues in FileStore.

## [27.5.1] - 2019-08-29
- remove "validate" property that brought by TypedUrn in avsc schemas

## [27.5.0] - 2019-09-02
- Fix memory leak in test by properly shutting down HttpClientFactory in test data providers.
Revert commit 'f9d016b1b7f969c04368c0872b501e592c48c889' - that introduced a test failure
- Create constants for bounded load thresholds.

## [27.4.3]
- Upgrade to Netty 4.1.39, Fix Deprecated Http2 APIs, Fix Netty Bug
Bumping the r2 integration test port to higher number as the tmc ORCA job is failing consistently due to port conflict

## [27.4.2]
- Precompiling regex Pattern instead of calling String.replaceAll
- Fix indentation for certain PDL encoder cases
- Extend PDL syntax to allow unions with aliases
- Fix TestDisruptFilter flaky test

## [27.4.1]
- [pgasus]: Control Pipeline V2 through a constant and a config value set through builder
- Revert "[Pegasus]: Control Pipeline V2 through a constant and a config value set through builder"

## [27.4.0] - 2019-08-15
- [Pegasus]: Control Pipeline V2 through a constant and a config value set through builder
- Adding guarantee that last operation (markUp/Down) will always win
Making more tolerant if a node is not found while deleting (since the goal is to delete it)
Giving the guarantee that markUp/Down will be called the minimum set of time to make collapse multiple markUp/markDown requests which would be idempotent
- Adding MinimumDelay mode for disrupting restli response flow
- Revert "Adding MinimumDelay mode for disrupting restli response flow"
- Adding MinimumDelay mode for disrupting restli response flow
- Control Pipeline V2 through a constant and a config value set through builder

## [27.3.19] - 2019-08-06
- Refactor GenerateDataTemplateTask

## [27.3.18] - 2019-08-09
- Add consistent hash ring simulator to visualize request distribution and server load
- Update README.md

## [27.3.17] - 2019-08-06
- Fix Gradle caching regression introduced in 27.3.8.
- Fix a race condition bug in CompositeWriter.

## [27.3.16]
- Merge EntityStream Race Condition bug to New Unified Netty Pipeline
- R2 Netty Pipeline Unification

## [27.3.15]
- Fix for TypeRef field's annotation properties not propagated to the Record field containing it

## [27.3.14] - 2019-08-01
- Fix a backward-incompatible bug for setting ErrorDetails in RestliServiceException
- Fix open-source builds by removing certain Guava usages

## [27.3.13] - 2019-07-31
- Fix flaky unit test in BaseTestSmoothRateLimiter
- Add bounded-load consistent hashing algorithm implementation

## [27.3.12] - 2019-07-30
- Support special floating point values - NaN, NEGATIVE_INFINITY, POSITIVE_INFINITY in rest.li

## [27.3.11] - 2019-07-29
- (Revert pending DMRC review) Add bounded-load consistent hashing algorithm implementation
- Read Avro project properties at execution time.
- Add bounded-load consistent hashing algorithm implementation

## [27.3.10] - 2019-07-29
- Attachment streaming bug: java.lang.IllegalStateException: Attempt to write when remaining is 0

## [27.3.9] - 2019-07-23
- Adding support for RestLiServer to accept RestLiResponse callback

## [27.3.8] - 2019-07-23
- Move migrated plugin code to use the Java plugin.
- Migrate PegasusPlugin from Groovy to Java.
- Make project properties cacheable for GenerateAvroSchemaTask

## [27.3.7] - 2019-07-22
- Remove content length http header if present for ServerCompressionFilter

## [27.3.6] - 2019-07-10
- Add options to translate Pegasus Default fields to Avro Optioanl fields in SchemaTranslator, DataTranslator*
- Fix incorrect end of stream bug in MultipartMIMEInputStream

## [27.3.5] - 2019-06-26
- Fix for Avro to Pegasus data translation union member key namespace override bug

## [27.3.4] - 2019-06-28
- Expose requestcontext as well when constructing the validation schema

## [27.3.3] - 2019-06-27
- Generate documentation for service errors and success statuses
- Modify scatter gather API to allow more flexibility in custom use case

## [27.3.2] - 2019-06-20
- Guarantee the order of EntityStream callbacks in Netty Outbound Layer (downstream calls)
- Improvements to the streaming library.
  - Simplify the logic in ByteStringWriter
  - Make CompositeWriter threadsafe.

## [27.3.1] - 2019-06-24
- Provide an extension point for constructing the validating schema
- Enable string sharing when generating LICOR binary

## [27.3.0] - 2019-06-18
- Check compatibility of IDL service errors and schema field validators

## [27.2.0] - 2019-06-10
- Fix $returnEntity response validation bug
- Adding switch for destroyStaleFiles
- In ServiceError interface integer http status code has been changed to HttStatus type

## [27.1.7] - 2019-06-05
- Set TimingContext#complete log from warn to debug.
- Updated MockValidationErrorHandler to create BadRequest response

## [27.1.6] - 2019-06-03
- Add the ability to specify the TimingImportance of a TimingKey to TimingContextUtil.

## [27.1.5] - 2019-05-08
- Adding RampUp RateLimiter

## [27.1.4] - 2019-05-30
- Client integration for service error standardization
- Added Service Unavailable Http Status Code 503 to RestStatus Class
- Added ErrorResponseValidationFilter for error response validation
- Add support for method parameter service errors and success status codes
- Server integration for service error standardization
- Revert "Migrate PegasusPlugin from Groovy to Java."
- Migrate PegasusPlugin from Groovy to Java.
- Fix stale build directory for dataTemplates
- Add logging for troubleshooting channel pool

## [27.1.3] - 2019-05-08
- Add support for defining service errors, document service errors in the IDL

## [27.1.2] - 2019-05-06
- Update GenerateAvroSchemaTask to allow overriding namespaces in the generated Avro files.

## [27.1.1] - 2019-05-06
- Added ValidationErrorHandler interface.
- Migrate Pegasus utilities from Groovy to Java.

## [27.1.0] - 2019-05-03
- Fix ChangedFileReportTask property annotations.
- Migrate CheckIdlTask from Groovy to Java.
- Migrate CheckRestModel task from Groovy to Java.
- Migrate CheckSnapshotTask from Groovy to Java.
- Migrate GenerateAvroSchemaTask from Groovy to Java.
- Migrate GenerateDataTemplateTask from Groovy to Java.
- Migrate GenerateRestClientTask from Groovy to Java.
- Migrate GenerateRestModelTask from Groovy to Java.
- Migrate PublishRestModelTask from Groovy to Java.
- Migrate TranslateSchemasTask from Groovy to Java.

## [27.0.18] - 2019-04-26
- Add removeNulls utility in DataMapUtils.

## [27.0.17]
- Don't invoke startMap and startList with null values since the normalization code in pemberly-api is overriding these methods and craps out with NPE on encountering a null parameter

## [27.0.16]
- Fixing error in CertificateHandler which was sending the message even if the handshake was failing, hiding the real SSL error
Adding cipher suite information to the server side's context
- Update ErrorResponse schema for error standardization
- Migrate Pegasus tests from Groovy to Java.

## [27.0.15] - 2019-04-22
- Include TypeInfo when adding compound key parts.

## [27.0.14] - 2019-04-16
- Guard against implementations of ProjectionDataMapSerializer returning null

## [27.0.13] - 2019-04-13
- Nuke option to force wildcard, and instead add in an option to pass in a custom projection mask param to mask tree data map serializer

## [27.0.12] - 2019-04-11
- added RestLiInfo.

## [27.0.11]
- Allow ' in the line style comment in PDL.

## [27.0.10] - 2019-04-04
- Restrict API in request to only allow forcing wildcard projections

## [27.0.9] - 2019-04-04
- Make request options settable on the request. Remove the ability to set force wildcard projections on the requestOptions object since doing so may inadvertently cause shared final constant requestOptions objects to be modified, leading toside effects

## [27.0.8] - 2019-04-02
- Add request option to override projections to wildcard

## [27.0.7] - 2019-03-28
- Added remove query param support in AbstractRequestBuilder

## [27.0.6] - 2019-03-27
- Update R2 REST Client to send RFC 6265 compliant cookie headers during HTTP/1.1 requests
- Update docgen to include the symbols for Enum types.

## [27.0.5] - 2019-03-18
- Properly encoding empty datamap property value in PDL, and fix PDL nested properties parsing bug
- Check if originally declared inline in encoding include to JSON under PRESERVE mode
- Add default NoOp implementation for TraverseCallback.

## [27.0.4] - 2019-03-06
- Escape keywords in namespace of inline schema

## [27.0.3]
- Link parameter to its corresponding array item record in restli HTML documentation

## [27.0.2] - 2019-02-28
- Optimize URI parsing inefficiencies

## [27.0.1] - 2019-02-21
- Generate BatchFinder Example in HTML Doc without using ResourceModel

## [27.0.0] - 2019-02-25
- Add default null value to translated union aliased members

## [26.0.19]
- Trimming each packageName value in the comma separated packageName to make sure that spaces around the commas are handled.

## [26.0.18] - 2019-02-15
- Make RestLiStreamCallbackAdapter non-final and public for extension in dash-cache

## [26.0.17]
- (This version was used to produce custom builds. So skipping this to avoid confusion.)

## [26.0.16] - 2019-02-04
- Use ordered collection for classTemplateSpecs field

## [26.0.15] - 2019-02-08
- Add batch parameter for batchFinder in association template snapshot, to fix the incompatible issue that type cannnot be resolved
- Add HTML Documentation render for BatchFinder

## [26.0.14] - 2019-02-12
- add drain reader for unstructured data get

## [26.0.13] - 2019-02-12
- Tweak implementation a little to allow extension to support streaming normalized/deduped codecs in pemberly-api

## [26.0.12] - 2019-02-10
- Make some stuff more visible for overriding in pemberly-api

## [26.0.11]
- (This version was used to produce custom builds. So skipping this to avoid confusion.)

## [26.0.10] - 2019-02-03
- Rename KSON to LICOR aka LinkedIn Compact Object Representation

## [26.0.9] - 2019-01-29
- Added missing accessors for request URI components

## [26.0.8] - 2019-01-31
- Refactoring format of TimingKeys in R2 for better analysis

## [26.0.7]
- (This version was used to produce custom builds. So skipping this to avoid confusion.)

## [26.0.6] - 2019-01-28
- Add timing marker for 2.0.0 URI parsing

## [26.0.5] - 2019-01-29
- Add KSON support. KSON is a variant of JSON that serializes maps as lists, and supports optional compression of the payload using a shared symbol dictionary.

## [26.0.4] - 2019-01-11
- Allow configurable basePath for d2 service znode under cluster path

## [26.0.3]
- (This version was used to produce custom builds. So skipping this to avoid confusion.)

## [26.0.2] - 2018-12-20
- Allow RestLiValidation filter to accept a list of non-schema fields that should be ignored in the projection mask. Also fixes bug in the ProjectionMask applier that was not unescaping field names from masktree correctly.

## [26.0.1] - 2018-12-19
- Provide the validation filter for BatchFinder
- Add client-side support for Batch Finder

## [26.0.0] - 2018-12-03
- Delete restli-tools-scala module. Dependencies should be updated to `com.linkedin.sbt-restli:restli-tools-scala:0.3.9`.

## [25.0.21] - 2018-12-05
- Enable PDL in pegasus plugin through gradle config.

## [25.0.20] - 2018-12-06
- Fix NPE issue when d2Server announces without scheme
- Log warning instead of exception for unsupported association resource in build spec generator.

## [25.0.19] - 2018-12-05
- Use temporary directory in TestIOUtil test.
- Make some methods in DefaultScatterGatherStrategy protected for easy override in custom strategy.

## [25.0.18] - 2018-11-19
- Add comment to indicate the supplied queue implementation to SmoothRateLimiter must be non-blocking as well
- Scope restli-tools-scala dependency in a few places.

## [25.0.17] - 2018-11-15
- Simplify SmoothRateLimiter event loop logic

## [25.0.16] - 2018-11-12
- Add error logging to SmoothRateLimiter

## [25.0.15] - 2018-11-09
- Zookeeper client-side: recognize and apply "doNotSlowStart" UriProperty when a node is marked up.

## [25.0.14] - 2018-11-01
- Server-side: expose changeWeight method in JMX and add "doNotSlowStart" UriProperty

## [25.0.13] - 2018-10-25
- Keep ALLOWED_CLIENT_OVERRIDE_KEYS property when invoke #getTransportClientPropertiesWithClientOverrides

## [25.0.12] - 2018-10-22
- Have d2-benchmark compilation task depend on d2's tests compilatoin task
- Adding default value to enableSSLSessionResumption

## [25.0.11] - 2018-10-18
- Keep time unit consistent for SmoothRateLimtter
- Choose right algorithm automatically when consistentHashAlgorithm is not specified

## [25.0.10]
- GCN fix to revert data feeder change

## [25.0.9] - 2018-10-04
- Making SSLResumption feature configurable

## [25.0.8] - 2018-10-04
- Support BatchGetKVRequest and BatchPartialUpdateEntityRequest in scatter-gather

## [25.0.7] - 2018-10-03
- add R2Constants.CLINET_REQUEST_METRIC_GROUP_NAME and r2 client delegator
- Disable aggregateFailures to avoid flaky tests blocking tmc

## [25.0.6] - 2018-10-01
- Fix a bug in constructing gathered batch response in scatter-gather.

## [25.0.5] - 2018-10-02
- Support returning the entities for BATCH_PARTIAL_UPDATE resource methods
- Support ParSeq task resource method level timeout configuration.

## [25.0.4] - 2018-06-29
- Add Batch Finder support on server side

## [25.0.3] - 2018-09-26
- Add a new constructor for RestLiServiceException to disable stacktrace inclusion.
- Fix a AsyncPoolImpl bug where cancel calls did not trigger shutdown
- Making documentation request handling lazy.
- Add ForwardingRestClient implementation to ease transition to Client

## [25.0.2] - 2018-09-25
- Fix an integration test to test client side streaming correctly. Also updating the error created when stream decoding fails.
- Don't expose rest.li client config and run scatter gather tests in parallel.

## [25.0.1] - 2018-09-06
- Improve how circular references involving includes are handled in schema parser.
- Change backupRequest so that it can work when d2 host hint is given
- Relax typeref circular references, disallowing typeref-only cycles.
Provide better error message when cycles are detected.

## [25.0.0] - 2018-09-18
- Making RestLiResponseException constructor public.
- Fix ActionResult bug to return an empty response body
- Preserve order of sets added to request builders.
- Look up Javadoc by correct custom parameter name.

## [24.0.2] - 2018-09-10
- Generating fat jar for restli-int-test-api to maven.
- Add client-side support for returning the entity with PARTIAL_UPDATE
- Support starting rest.li integration test server without document handler.

## [24.0.1] - 2018-09-05
- Make bannedUris field optional

## [24.0.0] - 2018-09-05
- Disable test that fails consistently in TMC and passes consistently in local box
- Bump major version and update gradle.properties for release
- Refactor some code for ease of access in pemberly-api
- Fix test relying on string error message
- Implement blocking and non blocking smile data codec
- Support returning the entity for PARTIAL_UPDATE resource methods
- D2 merging clientOverridesProperties into the serviceProperties at deserialization time.
- D2 adding REQUEST_TIMEOUT_IGNORE_IF_HIGHER_THAN_DEFAULT parameter to request context's request

## [23.0.19] - 2018-08-08
- Allow custom users to set overridden partition ids to URIMapper

## [23.0.18] - 2018-08-25
- make aclAwareZookeeper only apply acl if the create mode is EPHEMERAL OR EPHEMERAL_SEQUENTIAL

## [23.0.17] - 2018-08-22
- Avoid double encoding typeref names in resource model encoder.
- Fix bug introduced in 22.0.0 during the server refactor which results in NPE for empty request body.

## [23.0.16] - 2018-08-22
- Add "$returnEntity" query parameter to support returning the entity on demand
- Added $reorder command for array items to Rest.li patch format.
- Log channel inception time when exception is thrown during writeAndFlush

## [23.0.15] - 2018-08-20
- Populate internal DataMap for gathered batch response.
- Updated gradle wrapper to version 4.6.
- Remove Guava dependency from compile in all modules except data-avro and restli-int-test
- Register coercers for custom typed typerefs in the generated TyperefInfo class.

## [23.0.14] - 2018-08-16
- Skip schema validation when the projection mask is empty.

## [23.0.13] - 2018-08-14
- Clean up ProjectionInfo interfaces to hide mutable API.
- Fix for reference to internal record.

## [23.0.12] - 2018-08-09
- Fixed bug in ObjectIterator when using PRE_ORDER iteration order.

## [23.0.11] - 2018-08-09
- Fix a race condition in AsyncSharedPoolImpl with createImmediately enabled

## [23.0.10] - 2018-08-08
- fix requestTimeoutClient bug for override client provided timeout value

## [23.0.9] - 2018-08-06
- Support scatter-gather in rest.li client using ScatterGatherStrategy.
- Add ProjectionInfo to prepare for emitting projection data into SCE.

## [23.0.8] - 2018-08-03
- StreamCodec integration in Restli client

## [23.0.7] - 2018-07-31
- Flipping the map order from Set<KEY> -> URI to URI -> Set<KEY> in URIMapper results
- Skip unstructured data check in RestLiValidationFilter

## [23.0.6] - 2018-07-24
- Cache SessionValidator to save validating time

## [23.0.5] - 2018-07-24
- add R2Constants.CLIENT_REQUEST_TIMEOUT_VIEW and refactor requestTimeoutClient to always pass timeout value down
- Enable ZK SymLink redirection by default

## [23.0.4] - 2018-07-13
- Improve cache ability of data template and request builder generating task

## [23.0.3] - 2018-05-16
- Remove logs when data template task is disabled.
- Introduce RestLiClientConfig
- Retry markUp/Down in event of KeeperException.SessionExpiredException
- Fix warnings in ZooKeeperAnnouncerTest resulting in failed compile.
- Support BannedUris property for cluster

## [23.0.2] - 2018-06-28
- Remove/Deprecate Promised based API support
- Unstructured Data update and delete Reactive
- Fix DataTranslator for ARRAY field types when converting a GenericRecord to DataMap

## [23.0.1]
- Safely build validating schema on request if using projections, return HTTP 400 for projections with nonexistent fields
- Add serialization of degrader.preemptiveRequestTimeoutRate

## [23.0.0] - 2018-06-22
- Add URIMapper class and changed getPartitionAccessor API (backwards incompatible)

## [22.0.5] - 2018-06-18
- Unstructured Data Post Reactive
- Allow SslSessionNotTrustedException to extend RuntimeException

## [22.0.4] - 2018-06-14
- Implements AsyncRateLimiter

## [22.0.3] - 2018-06-13
- Revert "Build validating schema on request if using projections, return HTTP 400 for projections with nonexistent fields"

## [22.0.2] - 2018-06-12
- Disable offending D2 tests failing with connection loss to unblock pegasus version release.
- Removed _declaredIncline property from equals and hashCode methods on UnionDataSchema.Member class.
- Add default value for HTTP_QUERY_POST_THRESHOLD

## [22.0.1] - 2018-06-05
- Fix pdsc to avro transform issue when record type is union with alias

## [22.0.0] - 2018-06-04
- Integrated streaming codec with RestLiServer. This change also includes substantial refactoring of RestLiServer.
- Added enums to TestQueryParamsUtil to prove it works since we have production use cases that depend on it
- Creating a startupExecutor to use in the first phases of startup to not re-use/conflict with the internal usages of other executors

## [21.0.6] - 2018-05-08
- Build validating schema on request if using projections, return HTTP 400 for projections with nonexistent fields
- use retryZookeeper in AclAwareZookeeperTest to improve test stability
- refactored zkConnectionDealer to SharedZkConnectionProvider

## [21.0.5] - 2018-05-30
- Add support for coercing Maps to DataMaps in QueryParamsUtil.convertToDataMap()

## [21.0.4] - 2018-03-20
- Adding session resumption and certificate checker for http2
- Add application Principals to cluster for server authentication

## [21.0.3] - 2018-05-24
- Make ClientRequestFilter implementation of wire attributes case-insensitive

## [21.0.2] - 2018-05-23
- Update test to use old property values
- Make R2 wire attribute key implementation case-insensitive

## [21.0.1] - 2018-05-14
- Update tests to use old property values
- Remove request query params from response location header for CREATE and BATCH_CREATE
- Bump up parseq version to non EOLed version.
- Define ZKAclProvider interface

## [21.0.0]

## [20.0.23]
- Update default values of the following properties:
	DEFAULT_RAMP_FACTOR = 2.0;
	DEFAULT_HIGH_WATER_MARK = 600;
	DEFAULT_LOW_WATER_MARK = 200;
	DEFAULT_DOWN_STEP = 0.05;
	DEFAULT_MIN_CALL_COUNT = 1;
	DEFAULT_HIGH_LATENCY = Time.milliseconds(400);
	DEFAULT_LOW_LATENCY  = Time.milliseconds(200);
	DEFAULT_REQUEST_TIMEOUT = 1000;

## [20.0.22] - 2018-04-16
- SyncIoHandler should notify its reader/writer when exception happends

## [20.0.21] - 2018-04-24
- Remove Flow API
- Emits R2 channel pool events
- SslHandshakeTimingHandler only produces a TransportCallback

## [20.0.20] - 2018-04-23
- Adding error codes to ServiceUnavailableException

## [20.0.19] - 2018-04-16
- add zookeeper connection sharing feature

## [20.0.18] - 2018-04-16
- Expose getFields() to FindRequest/GetAllRequest/CreateIdEntityRequest

## [20.0.17] - 2018-03-30
- Fix NPE in d2 caused by QD.
When QD doesn't contain required schema(https), transportClient will be null and through NPE.
This change fix the NPE and throw ServiceUnavailableException instead.
- Fix distributionBased ring creation with an empty pointsMap
- Added implementation for reactive streaming JSON encoder.

## [20.0.16] - 2018-04-03
- Temporarily downgrade timings warning to debug to satisfy EKG

## [20.0.15] - 2018-04-02
- Fix warning on "already completed timing"

## [20.0.14] - 2018-03-30
- Add timings to d2 and r2 stages
- Remove LongTracking and add minimum sampling period to AsyncPoolStatsTracker
- Updated Javadoc regarding concurrent use of PrettyPrinter in JacksonDataCodec.
- Added reactive streaming JSON parser.
- Use Instantiable.createInstance for stateful PrettyPrinter in JacksonDataCodec.
- add debug information to warn users about the distributionRing not supporting stickiness

## [20.0.13] - 2018-03-22
- Fixed thread safety issue for JacksonDataCodec due to its PrettyPrinter.

## [20.0.12] - 2018-03-21
- fix MPConsistentHashRing iterator bug when the host list is empty
- Fixing ZooKeeperConnectionAwareStore unneeded exception. Improving error logs in SimpleLoadBalancer

## [20.0.11] - 2018-03-19
- add more tests for MPConsistentHashRing iterator
- Adding ability to pass down the provider of the list of downstream services.
- Moving HttpServerBuilder to a module accessible also from other tests

## [20.0.10] - 2018-03-19
- Implement d2 degrader preemptive request timeout

## [20.0.9] - 2018-03-16
- modified MPConsistentHashRing iterator
- updated tests due to changing behavior of MPConsistentHashRing iterator

## [20.0.8] - 2018-03-13
- Adding SSL session resumption to Http1.1

## [20.0.7] - 2018-03-13
- Allow configurable number of hash ring points per host to improve traffic balance

## [20.0.6] - 2018-03-13
- Fix Client class to be backward compatible

## [20.0.5] - 2018-03-12
- Support for injecting metadata in Rest.Li response
- Refactoring internal LoadBalancer interfaces to async model
- Added XXHash as an alternative to MD5 for PartitionAccessor
- Enable setting default values for custom type parameters.
- Set proper default for HttpServerBuilder class
- Add DistrbutionNonDiscreteRing for distribution-based routing

## [20.0.4] - 2018-03-07
- Moved Entity Stream implementation to a new entity-stream module and made it generic. Added adapters between generic
Entity Stream and ByteString-specific Entity Stream so that existing R2 Streaming API didn't change.
- Add filter method in Multiplexer custom filter to filter all individual requests. This can be used to filter/check
if the combination of individual requests can be handled.

## [20.0.3] - 2018-02-28
- Improving error logging in hash functions

## [20.0.2] - 2018-02-27
- Change some degrader logging messages from 'warn' to 'info'

## [20.0.1] - 2018-02-27
- Fix NPE for addFields, addMetadataFields, addPagingFields

## [20.0.0] - 2018-02-26
- Request object should store the projection fields <PathSpec> in Set
- Tests fix for SI-5482(Request object should store the projection fields <PathSpec> in Set)

## [19.0.4]
- PDSC to AVSC translator supports namespace override based on cmd option.
- Replace dependency on antlr with antlr-runtime.
- Update Apache commons-lang to 2.6.

## [19.0.3] - 2018-02-21
- Exposing setter methods in FilterRequestContext to set the projection mask for CollectionResponse's METADATA and PAGING fields.

## [19.0.2] - 2018-02-22
- Removed logging for replaced error in RestLiFilterChainIterator

## [19.0.1]
- Add LogThreshold property to degrader

## [19.0.0] - 2017-09-28
- Refactoring changes:
 - Route resource request before adapting StreamReqeust to RestRequest.
 - Simplified callbacks that passe Rest.li attachment and ParSeq trace.

## [18.0.8] - 2018-02-02
- Cutdown d2 INFO/WARN messages when a lot of hosts are put into recoveryMap
- Add check and debug information to workaround NPE issue during #logState

## [18.0.7] - 2018-01-31
- Rename RewriteClient and TransportAdaptor

## [18.0.6]
- Add TransportAdaptor for easy request rewriting and reusing

## [18.0.5]

## [18.0.4] - 2018-01-22
- Add writableException option to RestException, StreamException, and RemoteInvocationException

## [18.0.3] - 2018-01-16
- Allow empty lists to be passed as a value for a required array query parameter.
- Log AsyncPool instance ID and not reset active streams during channel pool shutdown

## [18.0.2] - 2018-01-17
- Added javadocs and renamed field variables in PagingContext to avoid ambiguities.
- Update scala version to 2.10.6

## [18.0.1] - 2018-01-12
- Exposes PartitionDegraderLoadBalancerStateListener through constructors of D2 classes
- Add SSL support for netty server.
- Adding the ability to set the request timeout on a per-request basis

## [18.0.0]
- Updated unstructured data streaming to use Java 9 Flow APIs

## [17.0.5] - 2017-12-12
- Update quickstart service to use the latest maven release.
- Make CodeUtil.getUnionMemberName(UnionTemplate.Member) public to make it accessible for codegen in other languages.

## [17.0.4] - 2017-12-08
- Fix SchemaParser to handle jar files as sources. Bug was introduced during the refactor to support pdl schemas.

## [17.0.3] - 2017-12-04
- Update MaskTree#getOperations method to handle array range attributes.

## [17.0.2] - 2017-12-04
- Shutting down ssl channelPool and fixing race condition

## [17.0.1]
- Re-enable delay tests with seeded randomHash
- Ensure publisher is set after zk connection is established

## [17.0.0] - 2017-11-22

## [16.0.6] - 2017-11-21
- Refactor DegraderLoadBalancerStrategyV3 class

## [16.0.5] - 2017-11-09
- Integration tests for projecting array fields with specific ranges.
- Fix the DataTemplate generation issue on Gradle 4.3's modified OutputDirectory generation time.

## [16.0.4] - 2017-11-10
- Logs request path only instead of the full URI
- Allows SslSessionNotTrustedException to be created with an inner exception and message

## [16.0.3] - 2017-11-09
- Changes to specify array ranges while projecting fields.
(0) Update PathSpec to include attributes (start and count).
(1) Update Java DataTemplate codegen to generate field PathSpec methods for specifying array ranges.
(2) Update MaskComposition to take care of merging array ranges.
- Update URIMaskUtil is parse array ranges in URI fields projection and fix bugs in Filer and CopyFilter.
- Error out if zookeeper connection is not ready for markup/markdown

## [16.0.2] - 2017-11-02
- Adding 'fast recovery' mode to help host recover in low QPS situation

## [16.0.1] - 2017-10-27
- Adding a D2 LoadBalancer that reads always latest data from disk in parallel of the LoadBalancer toggling mechanism
in order to eventually deprecate it.
The user will be able to switch between the two.

## [16.0.0] - 2017-10-26
- Support Async Interface for Unstructured Data and rename Unstructured Data resource classes
- Support Reactive Streaming for Unstructured Data

## [15.1.10] - 2017-10-24
- Fix the SchemaTranslator to translate array or map of union with aliases fields in the Pegasus schema.

## [15.1.9]
- Implement FileStore support in ZooKeeperEphemeralStore to avoid re-fetching children data when start watching a node

## [15.1.8] - 2017-10-13
- Relax multiple DisruptRestControllerContainer::setInstance from throwing exceptions to logging warnings

## [15.1.7] - 2017-10-12
- Make CertificateHandler queue flush calls in addition to write calls

## [15.1.6] - 2017-10-11
- Adding ability to decorate the VanillaZooKeeper from ZKConnection

## [15.1.5]
- Use EmptyRecord for unstructured data get response

## [15.1.4] - 2017-10-10
- SSLEngine to be created with all SSLParameters

## [15.1.3] - 2017-10-06
- Revert throwing 400 for invalid query parameters due to existing services usage.

## [15.1.2] - 2017-10-05
- Ignore missing required fields in validating query parameters.

## [15.1.1]
- Adding possibility to specify a server's certificate checker implementing the SSLSessionValidator interface

## [15.1.0] - 2017-09-29
- Added ResourceDefinitionListener to provide a mechanism for RestLiServer to notify the listener of the initialized Rest.li resources.

## [15.0.5] - 2017-09-14
- D2 customized partition implementation

## [15.0.4] - 2017-09-26
- Expose remote port number in the request context in both server and client sides
- Introduced a generic RequestHandler interface. Refactored existing documentation handler, multiplexed request handler and debug request handler to use this interface.
Collapse BaseRestServer with RestLiServer as its not used.
Removed deprecated InvokeAware and related code. It's not used at LinkedIn.

## [15.0.3] - 2017-09-25
- Support Unstructured Data (Blob) as Rest.li Resource

## [15.0.2]
- Allow d2 clients to configure http.poolStatsNamePrefix from D2 zookeeper's service config
- FilterChainCallback and RestLiCallback code refactoring.
- Improve the test execution for multipart-mime tests. Server is started/shutdown once per class instead of per test. This brings the build time for pegasus down from 20+ minutes to 6.5 mins.
- Throw 400 error for invalid query parameter values.
- Adds property pegasus.generateRestModel.includedSourceTypes to let users specify source types for GenerateRestModelTask
Changes RestLiSnapshotExporter,RestLiResourceModelExporter to use MultiLanguageDocsProvider by default

## [15.0.1] - 2017-09-11
- Fixing double callback invocation bug in WarmUpLoadBalancer

## [15.0.0] - 2017-08-18
- Added type parameter for RestLiResponseEnvelope to RestLiResponseData and RestLiResponseDataImpl.
Moved status and exception from RestLiResponseData to RestLiResponseEnvelope.
Created subclasses for UpdateResponseBuilder, BatchUpdateResponseBuilder, and CollectionResponseBuilder to build responses for specific types.
Broke ErrorResponseBuilder from RestLiResponseBuilder hierarchy.
Removed type parameter for RestLiCallback and RestLiFilterResponseContextFactory.
Made IdEntityResponse to extend IdResponse.

## [14.1.0]
- Introduced early handshake and early http2 upgrade request.
Added AssertionMethods utils to test-util module

## [14.0.12] - 2017-09-07
- Simplifies channel creation failure log message
- Reset streams during a connection error

## [14.0.11] - 2017-09-05
- Resize HTTP/2 connection window to the same size as initial stream windows

## [14.0.10] - 2017-08-29
- Request timeout no longer cause an HTTP/2 connection to be destroyed

## [14.0.9] - 2017-08-23
- Check Certificate Principal Name only when necessary

## [14.0.8] - 2017-08-15
- Use "*" as default path in Http2 upgrade requests
- Adding ChannelPoolManager Sharing feature

## [14.0.7] - 2017-08-14
- Fix filter chain callback to handle errors while building partial response and response. Handle all errors to ensure callback chain is not broken.

## [14.0.6] - 2017-08-09
- Fail promise if h2c upgrade or alpn negotiation do not complete before connection closes
- TimeoutAsyncPoolHandle to use Optional#ofNullable instead Optional#of to deal with a potentially null value
- Expose configuration and fix remote address exception logging to log both exception class and cause

## [14.0.5] - 2017-08-07
- Fix StackOverflowError introduced in 13.0.1 part 2

## [14.0.4] - 2017-08-07
- Fix StackOverflowError introduced in 13.0.1

## [14.0.3] - 2017-08-04
- Fix remote address exception logging to log class instead of cause

## [14.0.2] - 2017-08-03
- Logs remote address to stdout if servlet read or write throws exception

## [14.0.1] - 2017-08-02
- Enlarge default HTTP/2 client stream flow window size and enable auto refill for connection flow control window

## [14.0.0] - 2017-08-01
- Updated BatchCreateResponseEnvelope.CollectionCreateResponseItem with HttpStatus.
Fixed the bug in setting ID for CollectionCreateResponseItem.

## [13.0.7]
- Include Content-Type on all error responses.

## [13.0.6] - 2017-08-01
- Increase HttpClient default shutdown timeout from 5s to 15s
- Bump open source plugin to Gradle 4.0

## [13.0.5] - 2017-07-31
- Provide Location for BATCH_CREATE.
- Added BackupRequestsConverter and a dedicated ScheduledThreadPool for latencies notifier in BackupRequestsClient

## [13.0.4] - 2017-07-25
- Allow createDefaultTransportClientFactories to support https scheme as well
- Disabled few flaky tests.
- Fix testShutdownRequestOutstanding flaky test
- Allowing user to specify the "expected server's certificate principal name"
to verify the identify of the server in http 1.1
- Refactoring shutdown Netty Client to AbstractNettyClient and ChannelPoolManager

## [13.0.3] - 2017-07-24
- Temporarily remove the content type check for multiplexed requests.

## [13.0.2] - 2017-07-20
- Update D2 LoadBalancerStrategyProperties PDSC file with new D2Event properties
- Fix pegasus test failure

## [13.0.1]
- Add D2Event support

## [13.0.0] - 2017-07-14
- Bump open source to Gradle 3.5. Make most tasks cacheable.

## [12.0.3]
- Fix NPE when client is using D2 and does not set R2Constants.OPERATION on request context.

## [12.0.2] - 2017-07-17
- Logs a warning if maxResponseSize greater than max int is set to the HttpNettyChannelPoolFactory
- Reintroducing UnionDataSchema#getType() to ease the migration to UnionDataSchema#getTypeByMemberKey().

## [12.0.1] - 2017-07-13
- Improves HTTP/2 error handling

## [12.0.0] - 2017-07-10
- Initial set of changes in the data schema layer to support aliasing Union members.
- Changes in the data template and Java codegen layer for supporting Union members aliases.
- Updating compatibility checks for Union member aliasing.
- Schema translator changes for translating Pegasus union with aliases to Avro records with optional fields.
- Data translator changes for supporting Pegasus union with aliases.
- Remove 'type' based methods in UnionDataSchema and cleanup pegasus codebase that uses those methods.
- Support custom content-types/DataCodecs in Rest.Li. This change also adds customizable codec support for multiplexed requests.
- Removed the deprecated methods in ResourceMethodDescriptor (follow up from commit #a70de22).
- Killing the usage of getFinderMetadataType in ResourceModelEncoder.

## [11.1.1] - 2017-06-28
- Capture collection custom metadata type for GET_ALL in IDL and expose that in FilterRequestContext.
- Adding gracefulShutdownTimeout and tcpNoDelay to D2TransportClientProperties.pdsc
- Refactoring HttpNettyClients to use AbstractNettyClient for the common parts
- Adding idleTimeoutSsl's HttpClientFactory support
- Capture the @Deprecated trait of finder and action parameters in IDL.

## [11.1.0] - 2017-05-02
- Added backup requests.

## [11.0.18] - 2017-06-19
- Added sslIdleTimeout to PropertyKeys and TransportClientPropertiesConverter
Update pegasus version to newly released 11.0.17 for opensource quickstart project.

## [11.0.17] - 2017-06-19
- Added sslIdleTimeout to the d2-schemas
- Added D2 State WarmUp feature
- Fix pegasus plugin configuration to include the jdk tools jar.

## [11.0.16] - 2017-06-14
- Expose collection metadata projection mask in FilterRequestContext.

## [11.0.15] - 2017-06-08

## [11.0.14]

## [11.0.13] - 2017-06-07
- Typo in the test file : test-util/src/main/java/com/linkedin/test/util/GaussianRandom.java
- generateRestModel doesn't need a copy of the codegen classpath
- Changes to handle Restli info chatty output

## [11.0.12] - 2017-06-02

## [11.0.11] - 2017-06-01
- HttpClientFactory.Builder, added documentation and improved to handle more easily more cases

## [11.0.10] - 2017-06-01
- Make tcpNoDelay constructor's change backward compatible in HttpClientFactory
- Make check tasks incremental
- Honor ErrorResponseFormat for exception thrown within multiplexer (or by multiplexer filter).
- Add D2Monitor to support D2Event
- Make AsyncPoolImpl consistent returning a Cancellable but cannot be cancelled instead of null

## [11.0.9] - 2017-05-24
- Moving ChannelPoolManager sensors from ServiceName-based to be composed by Custom Prefix and Hash of transport properties

## [11.0.8] - 2017-05-12
- generateRestModel should be more strict about running
- Fixed log message formatting in SimpleLoadBalancerState.
- Add field aliases, field order, and include after support to PDL.
Fix PDSC to serialize include after fields if include was after fields in original schema declaration.
- Surppress the warning message if quarantine is already enabled
- Encode HttpOnly for cookies that are set with this flag.
- Change the failure for request body parsing error to 400_BAD_REQUEST instead of 500 internal error. This is also consistent with the behavior for other argument builders.

## [11.0.7] - 2017-05-04
- Upgrade to Netty 4.1.6 and remove maxHeaderSize from Http2StreamCodecBuilder

## [11.0.6] - 2017-04-28
- Improved configurability of GenerateDataTemplateTask.
- Add restModel jar generation to make gradle plugin consistent with sbt plugin.
Revised ResourceModelEncoder to fallback to ThreadContextClassLoader if idl cannot be located in the Class level class loader (this is needed in for play-on-gradle since the way class loader is setup for play-on-gradle in play, it only guarantees class are reachable via ThreadContextClassLoader).
- Create new EntityStream when retrying StreamRequest in RetryClient
- Fixed issue with GenerateDataTemplateTask introduced in 11.0.6

## [11.0.5] - 2017-04-21
- Fix StreamExecutionCallback.EventLoopConnector race condition.
- Remove deprecated << operator in PegasusPlugin.
- Cache DataTemplateUtil.getSchema.

## [11.0.4] - 2017-04-12
- Fix FileClassNameScanner to ignore hidden dot-files.

## [11.0.3] - 2017-04-10
- Provide a utility to perform semantic equality comparison for two pegasus objects.
- Change the quarantine health checking latency to use degrader low latency

## [11.0.2] - 2017-04-03
- Move schema logic from constructor to method for BatchKVResponse for performance.
Moving the logic around creating the BatchKVResponse schema to the schema() method and pulling some variables into constants for efficiency gains since schema is inaccurate and rarely used in this class.

## [11.0.1] - 2017-03-31
- Make RestLiDataValidator aware of wilcard for projecting Union members.

## [11.0.0] - 2017-03-31
- Major version bump due to backward incompatible changes introduced in 10.1.13.
- Add note to BatchEntityResponse explaining the backward incompatible change to DataMap format returned by data() method.
10.1.15  (Note: This version is not backward compatible. See 11.0.0)
- Fix one of ResponseDataBuilderUtil helper methods to return the correct ResponseEnvelope type and re-enabled unit tests which weren't running.
10.1.14  (Note: This version is not backward compatible. See 11.0.0)
- Extract interface from RestClient and add a DisruptRestClient decorator class
10.1.13  (Note: This version is not backward compatible. See 11.0.0)
- Refactor batch_get response conversion to entity response code into BatchKVResponse to be consistent with other decoder behavior.
- Make 'optional' fields more explicit in .pdl grammar

## [10.1.12] - 2017-03-23
- Disable TranslateSchemas task in pegasus plugin. It was incorrectly being enabled for all source sets.

## [10.1.11] - 2017-03-21
- Add gradle task to translate .pdsc to .pdl
- Increase d2 quarantine pre-check retry times

## [10.1.10] - 2017-03-17
- Added new constructor for DelegatingTransportDispatcher which takes StreamRequestHandler

## [10.1.9] - 2017-03-14
- Modify .pdl grammar to support nested namespace/package scopes.
- Added ability to share data between filters and resources.

## [10.1.8] - 2017-03-06
- Make RestClient get DisruptRestController from DisruptRestControllerContainer every time

## [10.1.7] - 2017-03-03
- Support null resolverPaths in DataSchemaParser to fix regression.

## [10.1.6] - 2017-03-01
- Fix LoadBalancerStrategyPropertiesConverter to serialize quarantine properties with or without maxPercentage property present

## [10.1.5] - 2017-02-25
- Fix the issue that ZooKeeperAnnouncerJmxMBean does not return a open type
- Fix streaming timeout non-active if response entity stream can be buffered in memory

## [10.1.4] - 2017-02-23
- Ensuring cloned DataLists and DataMaps have unique __dataComplexHashCode values.
- Bump up zero-allocation-hashing library version to the latest
- Update documentation on CreateKVResponse and BatchCreateKVResult.
- Add .pdsc to .pdl conversion support
- Modify client compression filter header-setting behavior to allow null operations.

## [10.1.3] - 2017-02-01
- Fix d2 quarantine pre-healthchecking related issues

## [10.1.2] - 2017-02-10
- Support surrogate pairs for UTF-16 strings in URI encoding/decoding

## [10.1.1] - 2017-02-06
- Using Treemap to allow QueryTunnelUtil.decode to parse case insensitive http headers

## [10.1.0] - 2016-12-06
- Introduce .pdl file format for authoring Pegasus data schemas.

## [10.0.2]
- Reduce the error messages generated by quarantine
- Implement disruptor interfaces and core classes

## [10.0.1] - 2017-01-13
- Delay releasing channel until entity stream is either done or aborted

## [10.0.0] - 2017-01-25
- Add attribute pagingSupported in method schema and indicate in restspec if get_all and finder methods support paging.

## [9.0.7] - 2017-01-23
- Remove the route percent checking in unit test
- RootBuilderSpec should provide a way to indicate parent-subresource hierarchy.

## [9.0.6] - 2017-01-20
- Allow http.maxResponseSize to be Long type

## [9.0.5] - 2017-01-13
- Fixed test failures in TestMultiplexerRunMode and TestParseqTraceDebugRequestHandler
- Force to convert QuarantineLatency to String

## [9.0.4] - 2016-12-22
- Fix bug in pegasus plugin. Run publishRestliIdl task only if rest-spec is not equivalent.

## [9.0.3] - 2016-12-19
- Added null annotation to generated data models
- Add utility methods to convert rest and stream TransportCallback

## [9.0.2]
- [RecordTemplate] Do not allocate RecordTemplate's _cache until the first usage in order to save unnecessary memory allocation.
- Fix wrong error message in converting key string.

## [9.0.1] - 2016-12-09
- Upgraded ParSeq version to 2.6.3
Added setting plan class that reflects type of request for async resources implemented using ParSeq
- [pegasus] Add two APIs to DataMapUtils.java to support mapping DataMap objects to Json ByteString and Pson ByteString.
RestResponseBuilder.encodeResult needs to set the builder's entity with the incoming dataMap object. This is currently done by converting the dataMap object to a raw byte array (either in Json or Pson format) and passing the array to RestResponseBuilder.setEntity. Then inside setEntity it converts the array to a ByteString object by calling ByteString.copy, which involves allocating an extra raw byte array (because they want to make the ByteString object independant of the original array). This process adds unnecessary memory footprint because we allocate two byte arrays, and the first one becomes dead after the second one copies it. In restli-perf-pegasus test, this shows up as 8% - 9% memory usage (allocation pressure) from Java flight recorder. The extra allocation could be avoided by a few approaches. 1) in RestResponseBuilder.setEntity, use ByteString(byte[] bytes) constructor instead of ByteString.copy, but this requires to make the constructor from private to public, which is kinda a violation of the original OO design. 2) add APIs to DataMapUtils.java to support mapping DataMap objects to Json ByteString and Pson ByteString without copying (via ByteString.unsafeWrap). This patch chose the second approach because there's no need to change any existing data structure. With the patch, restli-perf-pegasus shows smaller memory footprint (roughly the same 10 min period), and less GC occurrances (25 -> 23, 2 less ParallelScavenge young gc).

## [9.0.0]
- Remove dependency on slow IdentityHashTable. Remove copyReferencedObjects from DataComplex interface.

## [8.1.10]
- Fix test failures caused by Jackson version bump
- added comments to getResult() and getErrors()

## [8.1.9] - 2016-11-22
- Make HttpClientFactory default HTTP version configurable

## [8.1.8] - 2016-11-22
- Upgrade com.fasterxml.jackson version to 2.8.3

## [8.1.7] - 2016-11-09
- Fix several bugs in pegasus backward compatibility check, report generation and publishModel tasks.
(All bugs were introduced in 8.0.0)
PegasusPlugin:
 - With Gradle 3.0 and gradle daemon enabled, the plugin will not be loaded on every run. So the plugin cannot assume the static variables will be reset for each run. Fixed the issue by initializing these static variables in runOnce block.
CheckIdl, CheckRestModel and CheckSnapshot:
 - These tasks were ignoring the compat level flag while checking for status after the compat checker was run. Fix is to look at the result of compat checker from the report it prints.
 - Add the compatibility report message to the global message to be printed out after build is finished.
 - Task was configured to fail only if both idl and models were incompatible. Fixed it to fail if either is incompatible.
 - Fixed the order of files in checkRestModel task (order was wrong earlier)
ChangedFileReport:
 - Changed the inputs to this task from generated files to source system files on which the report should be based on.
 - Now outputs list of files requiring checkin, which is printed after build is finished.

## [8.1.6] - 2016-11-14
- Bump up version due to mint snapshot bug.

## [8.1.5] - 2016-11-09
- Support package override in PDSC.

## [8.1.4] - 2016-11-08
- Fix the racing condition for healthcheckMap map

## [8.1.3] - 2016-11-07
- Fix a bug in LoadBalancerStrategyPropertiesConverter to not use double

## [8.1.2] - 2016-11-02
- Fix an NPE in Http2StreamCodec resulting in incorrect logging

## [8.1.1]
- Fix bug in CheckSnapshotTask that caused the publishRestModelTask to always skip.
- Add more info to the logfile when a client is quarantined

## [8.1.0] - 2016-10-13
- Count server error when calculating error rate in degrader

## [8.0.7] - 2016-10-18
- D2 will retry request to a different host when the first attempt meets condition
- Add serviceName to quarantine logs
- Catch and log exceptions thrown by the async pool waiters

## [8.0.6] - 2016-10-04
- Removed extra R2 filter error log

## [8.0.5] - 2016-09-29
- Add request details to the quarantine health checking logs
- Update SetMode.java
- Cache Validator classes to avoid reloading them on every request and response.

## [8.0.4] - 2016-09-27
- Fixed SI-3070 and related issues

## [8.0.3] - 2016-09-22
- Add D2 slow start support.
- Adding quarantine properties to d2 schema

## [8.0.2] - 2016-09-20
- Fixed snapshot check bug which treats all the IDL and snapshot as new.

## [8.0.1] - 2016-09-19
- Fixed bug that snapshot file is not generated.

## [8.0.0] - 2016-09-15
- Isolation of gradle's classpath from pegasus plugin's classpath.
- Add a cap to quarantine maxPercent.

## [7.0.3] - 2016-08-31
- Add d2 quarantine support

## [7.0.2] - 2016-08-25
- [pegasus] Follow up on Min's feedback from 758221 - refactored filter response
context factory name and refactored RestLiCallback to be instantiated
from within Filterchain.
- Add HTTP protocol version to request context
- Add http.protocolVersion to AllowedClientPropertyKeys

## [7.0.1] - 2016-08-18
- Make MPConsistentHashRing iterator honor the weight of each element.
- Fix logging issues in DegraderLoadBalancerStrategyV3.

## [7.0.0] - 2016-08-15
- Added response data builder to allow Rest.li users the ability to
instantiate response data and response envelope for tests.
- Making envelope interfaces more intuitive by having naming reflect resource
method and adding support for determining CREATE vs. CREATE + GET.
- Filter improvements - simpler interface, support for async error invocation,
and safeguards against user error.
- Fix SnappyFramedCompressor to follow the standard x-snappy-framed
specification.

## [6.1.2]
- Removed ServiceProperties constructor that always threw a NullPointerException.

## [6.1.1] - 2016-08-10
- Fixing comparator logic in ResourceModelEncoder.
- Fix failing tests due to restriction of removing optional fields
- Convey that the removal of optional fields may be a backward incompatible change
- Remove JVM argument for setting the max memory sizes available to the JVM
- Increase max heap size to 4g for tests
- Add http.protocolVersion to d2 property keys and expose http protocol version in D2 schemas

## [6.1.0] - 2016-08-05
- Netty HTTP/2 client implementation

## [6.0.17] - 2016-08-01
- Add H2C Jetty server option to r2-perf-test
- Making TestStreamEcho.testBackPressureEcho test more generous and add logging

## [6.0.16] - 2016-08-01
- Change AbstractR2Servlet to only read input stream with size when content length is present
- Fix test concurrency issue in TestAsyncSharedPoolImpl

## [6.0.15] - 2016-07-29
- Removes -XX:MaxPermSize JVM argument now that we are fully on JDK8
- Reduces the number of threads in the ScheduledThreadPoolExecutor used in TestAsyncSharedPoolImpl
- Add a new consistent hash ring implementation that is more balanced and uses less memory.

## [6.0.14] - 2016-06-30
- Skip decoding simple keys which were passed in the request body for BATCH UPDATE and PATCH requests.

## [6.0.13] - 2016-06-30
- Removed double quotes for cookie attribute values.

## [6.0.12] - 2016-06-28
- Adding setters for projection masks in ServerResourceContext and FilterRequestContext.
- Add a shared implementation of AsyncPool, AsyncSharedPoolImpl

## [6.0.11] - 2016-06-14
- Support projections in rest.li schema validation
- Add a custom data comparator for JSON-like comparisons, and custom
data asserts with easy-to-understand error messages.

## [6.0.10] - 2016-06-10
- Add a d2 delay test framework
- Fixed cookie attribute name Max-Age.
- Add PathKeyParam annotation to enable declaring methods with strongly
typed keys.

## [6.0.9] - 2016-06-07
- Use parameterized logging to avoid excessive string concatenation

## [6.0.8] - 2016-05-27
- Add symlink support for Zk#getChildren with Children2Callback

## [6.0.7] - 2016-05-23
- Fixing the flawed comparision of keys on batch update/patch operations.
- Added parseq_restClient into restli-int-test module.
- Add HTTP_REQUEST_CONTENT_ENCODINGS to allowedClientPropertyKeys

## [6.0.6] - 2016-05-18
- Optimize ZookeeperEphemeral to reduce to number of read request to zookeeper.

## [6.0.5] - 2016-05-12
- Fix the wiki link for resource compatibility checking in Pegasus plugin.
- Fix missing custom information for record field and union member.
- Refactor code related to custom type.
- Change the Pegasus compatibility checker to consider the addition of a
required field with a default as compatible and to consider setting a
default on an existing required field as backward incompatible.
- Modifying tag to make javadoc generation happy

## [6.0.4]
- Add missing d2 config properties into d2 schemas

## [6.0.3] - 2016-05-03
- Fix build of the example projects.
- Remove default values for zkHosts

## [6.0.2] - 2016-04-22
- Fix doc generation for action methods with custom parameter types.
- Added cookies to isEquals/hashCode/toString in Request.java
- Added addAcceptTypes and addAcceptType to RestliRequestOptionsBuilder
- Resolving 500 error issue from invalid list params to return 400 instead.

## [6.0.1] - 2016-03-28
- Bump zookeeper library from 3.3.4 to 3.4.6

## [6.0.0] - 2016-04-08
- Add MultiplexerRunMode configuration that controls whether individual requests are executed as separate ParSeq plans or all are part of one ParSeq plan, assuming resources handle those requests asynchronously using ParSeq.
- Pull parseq rest client from parseq project.
- Rest.li streaming and attachment support

## [5.0.20] - 2016-03-30
- Fix a minor d2 update bug that affects the printout values in the logfile.
- Save resolved ip address into request context keyed by REMOTE_SERVER_ADDR in HttpNettyClient. It enables container to implement IPv6CallTracker+Sensor to track outbound traffic
Disable transient TestMIMEIntegrationReaderWriter suggested by the test case owner
- Add MockRestliResponseExceptionBuilder.

## [5.0.19] - 2016-03-23
- Fix a bug in R2 query tunnel with wrong content length

## [5.0.18] - 2016-03-16
- Improve memory and performance efficiency for LoadBalancerState and fix the dirty bits in the hasing in ConsistentHashRing
- Fix a flaky r2 test where the callback executor is shutdown before entity
stream of the response is read

## [5.0.17] - 2016-03-09
- Fix compatibility impact for added/removed fields in union
- Offset method in buffer chain should return 0 if start and end are the same
- Add ability to access RequestContext local attributes from FilterRequestContext

## [5.0.16] - 2016-02-29
- Add hasBindingMethods to RequestBuilderSpec to support generating request builders based on type binding information.

## [5.0.15] - 2016-02-26
- check for writehandle not null in connector

## [5.0.14] - 2016-02-25
- Add support for "x-snappy-framed" encoding in rest compression filter

## [5.0.13] - 2016-02-25
- Fix a race condition in StreamExecutionCallback

## [5.0.12]
- Do not create https transport client if SSL context is not available

## [5.0.11] - 2016-02-21
- Fix a bug when detecting cookies name with $ sign at the beginning. Check $ sign in the trimed name.

## [5.0.10] - 2016-01-26
- Write missing d2 configurations into zookeeper.
- Relax the SSL Context/Parameter checking for d2.

## [5.0.9] - 2016-01-25
- Allow array of union be resource method parameter.

## [5.0.8] - 2016-02-03
- Validate data returned from create and get.
- Notify listeners when SimpleLoadBalancerState shutdown
- Compare ReadOnly and CreateOnly paths properly in RestLiAnnotationReader.
- Add support for alternate schema parsers by introducing PegasusSchemaParser and DataSchemaParserFactory interface.
- Fix a bug in R2 client where a valid connection close can lead to
rate-limiting logic being applied to subsequent connection attempts.

## [5.0.7] - 2016-01-15
- OPTIONS response now includes named schemas from association keys.
- Change PhotoResource in restli-example-server to allow dummy ID and URN values in CREATE and UPDATE

## [5.0.6] - 2015-12-15
- Modify multipart mime streaming test to avoid transient failures
- Add option to specify a validator class map in RestLiDataValidator

## [5.0.5] - 2016-01-07
- Enable delay markup in ZooKeeperAnnouncer
- ByteString API changes to support multipart mime streaming
- Introduce async multipart mime support

## [5.0.4] - 2016-01-05
- Fix bug in ReplaceableFilter so that it won't buffer request/response
when not active.

## [5.0.3] - 2015-12-15
- Updated DataTemplateUtil with new coercion error messages
- Extend range of permissible protocol versions

## [5.0.2] - 2015-12-10
- Add restOverStream switch to D2ClientBuilder

## [5.0.1] - 2015-12-10
- Make CapRep work in streaming when activated

## [5.0.0] - 2015-12-09
- Change rest.li client request default protocol version.

## [4.1.0] - 2015-12-04
- Add streaming code path on client side & server side
The default is to use the existing code path.
- Increase timeout values in some tests

## [4.0.0] - 2015-12-03
- Gradle changes for Java 8
Refactor ErrorResponse formatting so that it is more flexible
- Two bug fixes: rest.li batch projection in response builder, restli validation schema source.

## [3.1.4] - 2015-12-01
- Made multiplexer returns correct HTTP status code (by throwing the right type of Exception) when encounter errors.
- Prevent possible NPEs in RestLiDataValidator.
- Include custom annotations in request builder specs

## [3.1.3] - 2015-11-24
- Add HTTP status code in response body for ErrorResponse in certain formats.
- Refactoring the d2config code for serviceGroup

## [3.1.2] - 2015-11-20
- Provide decouping of class exception and stack trace for certain client use cases.

## [3.1.1] - 2015-11-16
- Improve Rest.li client error message in handling non-JSON RestException.

## [3.1.0] - 2015-11-17
- Add streaming compressor and streaming compression filter. Note that
"snappy" encoding is no longer supported in streaming scenario because
it requires full data in memory before encoding/decoding. Instead, we
will be using "snappy-framed" encoding.
- Backport timeout to AbstractR2Servlet
- Invoke user callback immediately if netty write fails

## [3.0.2] - 2015-11-19
- Refactor ByteString to avoid extra copy in certain cases
- Make root builder method spec available to request builder specs.
- Correctly serialize typerefed ByteString in URI.

## [3.0.1] - 2015-11-16
- Allow ServiceGroup to specify clusters with coloVariants.

## [3.0.0] - 2015-10-30
- Refactor R2 message hierarchy

## [2.12.7] - 2015-11-13
- Make CookieUtil more robust.

## [2.12.6] - 2015-11-12
- Dummy version bump up to work around release issue.

## [2.12.5]
- Allow non-clusterVariant in ServiceGroup in d2.src.
- Catch throwable when invoking callbacks in ZooKeeperAnnouncer#drain

## [2.12.4] - 2015-11-05
- Fix a bug in ZooKeeperConnectionManager where we didn't retry when
EphemeralStore failed to start.

## [2.12.3] - 2015-11-04
- Treat query/action parameters with default values as optional in client builders.
- Address Multiplexer security concerns.
1. Disallow cookies to be passed in the individual requests.
2. Only whitelisted headers are allowed to be specified in the individual requests.
3. Cookies set by each of individual responses will be aggregated at the envelope level.
- Fix NPE when translating from an Avro GenericRecord with a null value
for field that is non-optional in the Pegasus schema.
Previously, if a source Avro GenericRecord returned a null value
(either because the field did not exist in the writer schema, or the
field has a null value) for a field mapping to a Pegasus field that
was not explicitly marked optional, the translator would throw an
uninformative NPE (i.e., it would crash).
Now, the translator no longer crashes and instead adds no mapping to
the Pegasus DataMap for the field.
- Support enum in compound key for batch update and batch partial update cases.

## [3.0.0] - 2015-10-30
- Refactor R2 message hierarhcy

## [2.12.1] - 2015-10-28
- Only process supported methods for non action resources in RequestBuilderSpecGenerator.
- Add a check for null R2 filter

## [2.12.0] - 2015-10-21
- Refactor R2 filter chain

## [2.11.3] - 2015-10-22
- Fix for single item list query parameter in HTTP request for 2.0

## [2.11.2] - 2015-10-18
- Generate template for top-level unnamed data schema.

## [2.11.1] - 2015-10-20
- Add original typeref data schema to template spec.
- Updated RestLi filters so that they can be executed asynchronously.

## [2.11.0] - 2015-10-19
- Make the resource information available to non-root request builder specs.
- Disallow @ActionParam on non-action methods.

## [2.10.19] - 2015-10-13
- Fix a bug in ZKPersistentConnection where a ZKConnection shouldn't be reused
after session expiration.

## [2.10.18] - 2015-10-02
- Fix for expensive ComplexResourceKey creation.
- Add additional methods to filter resource model to expose additional schema information.
- Fix createResponse error handling.
- Add a load balancer strategy config to control whether only updating partition state at the end of each internal.

## [2.10.17] - 2015-09-11
- Added fix for ActionResult<Void> return types throwing NPE
- Fix for null optional parameter values in ActionsRequest throwing NPE
- Allow custom method builder suffix in RequestBuilderSpecGenerator.

## [2.10.16] - 2015-09-09
- Data generator generates unambiguous @see link.
- Enable application to do additional processing on IndividualRequest and IndividualResponse by using
MultiplexerSingletonFilter.

## [2.10.15] - 2015-08-13
- Move request builder spec code from restli-swift repo to pegasus repo so that Android Rest.li client
can use it too. Currently this has not been used by Pegasus RestRequestBuilderGenerator yet.
- Allow Resources to designate alternative keys with the @AlternativeKey and @AlternativeKeys annotation.
Only client side work has been done.  Resources can be called with alternative keys but builders have
not been modified to allow alternative key formats to be sent.

## [2.10.14] - 2015-08-27
- Corrected CHANGELOG to correctly reflect changes made in 2.10.13.

## [2.10.13] - 2015-08-26
- Change Rest.li validator API to expose validateInput() and validateOutput() instead of a single validate() method.
- Changed multiplexer payload for IndividualRequest and IndividualResponse to use a real dictionary instead of a string.
- Replaced requests in MultiplexedRequestContent and responses in MultiplexedResponseContent from array to map.

## [2.10.10] - 2015-08-13
- Avoid logging error when there are https uris but the client is not ssl enabled.

## [2.10.9] - 2015-08-06
- Fix race condition and refactory d2 announcer

## [2.10.8] - 2015-08-03
- Changed RestResponseDecoder.wrapResponse to be public to account for a small number of
external users.

## [2.10.7] - 2015-08-12
- Create d2 configuration to change Accept-Encoding header.
- Added cookie support for restli.

## [2.10.6] - 2015-08-11
- Added required vs optional validation for partial updates.
Changed validation path formats to match PathSpec.
- Added batch create with entity back.
Enabled projection for createAndGet/BatchCreateAndGet.
- Serialize query parameter with custom coercer and uses bytes as underlying type correctly.

## [2.10.5] - 2015-08-03
- Align response compression with request compression.
- Updated DataSchemaParser to include non-top-level schemas and schemas in jar file.

## [2.10.4] - 2015-08-03
- Register custom coercers in union templates.
- Fix WireAttributeHelper not treating headers as case insensitive.

## [2.10.3] - 2015-07-29
- Better template spec generator interface.
- Coerce child classes correctly for query parameters.
- Support returning entity for the create method.

## [2.10.2]
- Fix build.gradle signing process to enable mavin release.

## [2.10.1] - 2015-07-20
- Fix a bug in FileStore where IllegalArgumentException may throw if temp file
prefix contains fewer than three characters

## [2.10.0] - 2015-07-14
- Add schema based autoprojection utility methods.
- Restructured API for exposing response data to rest.li filters.
Exposed per-exception ErrorResponse formatter for RestLiServiceException.
- Fixed inconsistency with exception handling for async resources.

## [2.9.1]
- Add configuration for maximum number of R2 concurrent connection attempts

## [2.9.0] - 2015-07-08
- Upgrade commons.io to version 2.4

## [2.8.0] - 2015-07-06
- Fix AbstarctClient to NOT share RequestContext across multiple requests.
- Make AbstractR2Servlet handles requests with chunked encoding

## [2.7.0] - 2015-06-18
- Enable file resolution for SchemaTranslator and propagate validation options to all parsers spawned during schema translation.
- Fail faster for bad requests with unknown hostname in HttpNettyClient.

## [2.6.3] - 2015-06-15
- Made generation of path spec methods, copier methods and record remove method in data template
configurable.

## [2.6.2] - 2015-06-14
- Fixed a bug in typeref processing in request params.
- Prepare data template generator for extension.

## [2.6.1] - 2015-06-08
- Remove unused dependency to mina-core.
- Factor out compression filter implementations to a new module (r2-filter-compression).
- Remove unnecessary generator dependency from modules that generates data template.
Remove unnecessary data-avro-generator dependency from modules that generates avro schema.
- Create IOUtil class to remove r2-core's dependency on commons-io.
- Fix trackerclient deleting racing problem.

## [2.6.0] - 2015-05-18
- Factor out PatchRequestRecorder and related classes into a new module (restli-client-util-recorder).
- Take out ParSeqRestClient into a separate module (restli-client-parseq).

## [2.5.1] - 2015-05-14
- Fixed a bug in processing of custom types in association resource keys.
- Handle error responses and batch results correctly in output validation filter.
- Broke down r2 into r2-core and r2-netty.
Removed dependency on json and jackson core from some modules.
- Fix bug that request builder generator may generate duplicate data template class.

## [2.5.0] - 2015-05-11
Make ChannelPoolHander iterate through all connection tokens.
- Modify content-encoding and content-length headers when decompressing body.
- New finer-grain data template and request builder generator classes and structure.

## [2.4.4] - 2015-05-05
- Fix a bug in ChannelPoolHandler where it should NOT put a channel back to the pool if
there is a "connection:close" header in the response.
- Rest.li data validator, ReadOnly and CreateOnly Rest.li data annotations.

## [2.4.3] - 2015-05-04
- Remove try-catch in parseAcceptEncodingHeader().
- Removing unused title field from the Link.pdsc.

## [2.4.2] - 2015-04-30
- Add back log4j.xml in generator and data-avro-generator module.

## [2.4.1] - 2015-04-28
- Add new status code constants for redirection and added new property key for redirection hops.
- Fix a bug in HttpClientFactory where _clientsOutstanding can go negative if client#shutdown
is called multiple times on the same client and thus preventing clientFactory to be shutdown.
- bug fix in ConfigRunner of d2 quick-start example

## [2.4.0] - 2015-04-27
- Fix race condition in D2 when Zookeeper is slow.
- Migrate r2 to netty4.
- Add more r2 integration tests.
- Refactor restli-int-test to properly shutdown R2 client/clientFactory

## [2.3.0] - 2015-04-23
- Handle key coercion error for CompoundKey and BatchKey to return 400.
- Rewrite KeyMapper API

## [2.2.11]
- Formally support the Java 8 compiler to create 1.6 target source files
- using servlet container threads to do IO for response in AbstractAsyncR2Servlet
- Migrate to using log4j2 internally in pegasus.
Fixed and consolidated the r2 perf tests.
- Disable transient failing d2 tests
- Move QueryTunnel to filters

## [2.2.10]
- Fixed key value conversion for typeref key without custom Java binding.
- Provide configuration for acceptType and contentType in RestLiRequestOptions per request.

## [2.2.9] - 2015-03-12
- Fixed DefaultMessageSerializer to support cookies and non-ASCII message entities.

## [2.2.8] - 2015-03-25
- Generate indented avro schemas

## [2.2.7] - 2015-03-19
- Allow selective response decompression for ClientCompressionFilter.

## [2.2.6] - 2015-03-06
- Add feature to QueryTunnelUtil and fix a bug in encode of MimeMultiPart entity
- Handle @PathKeysParam and other parameter annotation for action methods by
invoking the common routine ArgumentBuilder.buildArgs for all RestLiArgumentBuilder classes.
- Add warning message for invocation of deprecated pegasus plugin method addIdlItem accepting two parameters.

## [2.2.5] - 2015-03-05
- LoadBalancerStrategyName is deprecated and replaced by LoadBalancerStrategyList.
This patch gets rid of the StrategyName from the source.
- Added tests for BatchCreateIdResponse
Added new mock factory and tests for BatchCreateIdResponse.
- Return Http 400 error in case of key coercion error instead of Http 500.
Enhance existing RestLiRouter error testcases to always check return status.
- Fix intermittent test failure in ZooKeeperChildrenDataPublisherTest.
- Create option to turn off ClientCompressionFilter.

## [2.2.4] - 2015-02-17
- Let Rest.li filters modify request headers.

## [2.2.3] - 2015-02-09
- Change ByteString to have slice & copySlice methods
Add new copy method to copy from a sub array
- Fix a bug in AsyncPoolImpl where it fails to cancel pending create requests when it has drained out all pool waiters.

## [2.2.2] - 2015-02-05
- Fix illegal argument exception thrown when a cookie header is added.

## [2.2.1] - 2015-02-05
- Migrate to HttpClient 4.3.

## [2.2.0] - 2015-02-03
- Create separate internal storage data structure for Cookie and Set-Cookie HTTP header. Making header storage data structure case insensitive.

## [2.1.2] - 2015-01-20
- Configuration support for Rest.li request compression.

## [2.1.1] - 2015-01-07
- Add unbatch logic while extracts single resonpes from an auto-batched batch response.
Add field name to RestliRequestUriSignature.toString() result.

## [2.1.0] - 2014-07-17
- Making rest.li requests read-only.
- Product sorted snapshot files.
- Populate update status from error status.

## [2.0.5] - 2014-12-18
- Adding a method on Request class that creates a string representation of the request without using security sensitive information.

## [2.0.4] - 2014-12-19
- Pass the actual exception thrown by the resource (or a previous filter) to the rest.li response filters.
- Fixing the wiring of parseq context for the method parameters annotated with the deprecated ParSeqContext annotation.
- Added Rest.li 2.*.* release notes.
- Fix deprecation Javadoc on annotations.
- Handle the case when certain implementations of the Map inteface can't handle null.

## [2.0.3] - 2014-12-01
- Change RestException.forError to set the given throwable as the cause.

## [2.0.2]
- Add additional unit tests for RestLiArgumentBuilders.
- Remove redundant warning suppressions.
- Fix enum encoding bug for compound keys in protocol v2.

## [2.0.1] - 2014-12-04
- Java 8 support for Pegasus
- Improve performance for BatchGet when using the new request builder.
- Add projection tests to Rest.li response builder tests.
- Add dump functionality to RestliRequestUriSignature, which produces stable toString() output.

## [2.0.0] - 2014-10-28
- Remove Rest.li 1.0 deprecated APIs.
- Deprecate Rest.li 1.0 request builders.

## [1.24.8] - 2014-11-20
- Update Javadoc of Request#getUri().

## [1.24.7] - 2014-11-06
- Decrease default R2 http.idleTimout to 25s.

## [1.24.6] - 2014-11-06
- Fix memory leak in documentation generation.
- Add unit tests for RestLiArgumentBuilders.
- Task and Promise based async templates.
- Enable Gradle parallel build and config-on-demand.
Fix the dependency order in the generated IntelliJ IDEA module of data-avro-1_6.
- Introduce ProjectionUtil that tells if specific PathSpecs are filtered by a given MaskTree.

## [1.24.5] - 2014-11-03
- Add null pointer check for load balancer.

## [1.24.4] - 2014-10-29
- Turn on Rest.li 2.0 request builders by default in Gradle plugin.
- Add protocol 2 URIs for BatchGetRequestBuilderTest.
- Add unit tests for all classes that implement RestLiResponseBuilder.
- Disallow server impl module itself to be its API project.
- Allow projections on custom metadata and paging.
- Repair logging for restli tests
- Return parameterized ComplexResourceKey from MockBatchKVResponseFactory.createWithComplexKey().

## [1.24.3] - 2014-10-08
- Revisit resource method null handling.
Deprecating some parameter annotations, replacing with new ones, adding new resource context parameter annotaions and adding unit tests for the same.
- Upgrade jackson-core and jackson-databind dependencies to 2.4.3.

## [1.24.2] - 2014-10-01
- fix the bug in handlePut of UriProperties.

## [1.24.1] - 2014-09-30
- Make Request#getId() return null for Rest.li 2.0.

## [1.24.0] - 2014-09-26
- Fail fast if resource names clash.
- Make latest version Rest.li 2.0.

## [1.23.8] - 2014-09-23
- Expose more properties through DegraderLoadBalancerStrategyV3Jmx
- Server responds with the same protocol version header name as the client requests.

## [1.23.7] - 2014-09-22
- Force use next version override.

## [1.23.6] - 2014-09-19
- reduce the number of hashes in mapKeyV3
update HashBasedPartitionAccessor
move hashLong into HashFunction interface

## [1.23.5]
- Support deprecated protocol version header (X-Restli-Protocol-Version).
- Use Semaphore to allow multiple outstanding put (when calling D2Config) simultaneously.

## [1.23.4] - 2014-09-15
- Include file name for pdsc related error messages.
- Subclassing ZooKeeperPermanentStore to only write changed and new properties to store.

## [1.23.3] - 2014-09-09
- Update RestLiAnnotationReader to check if a resource' annotation matches its template type.
Remove RestLiCollectionCompoundKey as it is not used.
- Introduce doc support for action return types.
- Allow client to change r2 min pool size

## [1.23.2] - 2014-09-02
- RestliRequestUriSignature: Handle assocKey for FindRequest and GetAllRequest.
MockActionResponseFactory: Add option to support dynamic schema, such as CollectionResponse.
- Throw exception while generating IDL when Finder or GetAll methods are annotated with non-existing assocKeys.

## [1.23.1] - 2014-08-19
- Deprecate RestliProtocolCheck.

## [1.23.0] - 2014-08-05
- change getPartitionInformation so the ordering of the server will be consistent for identical hashrings
- Add RestliRequestUriSignature, A summary object for the URI of a Rest.li Request.
Add MockActionResponseFactory for mocking ActionResponse.

## [1.22.0] - 2014-08-07
- allows client to change r2 client pool waiter size
- Adding logic to throw exception if BatchGetRequestBuilder.batch methods are called for requests with Compound or Complex Keys.

## [1.21.2] - 2014-08-08
- Unit test and the fix for the DegraderImpl rollver deadlock

## [1.21.1] - 2014-08-07
- Add new API to Ring: getIterator()
- Fixing Java 7 Build.
- Changing the protocol version to 1.0 in those places we want 1.0 instead of baseline such as deprecated code paths.

## [1.21.0] - 2014-08-05
- Add NPE check for removePartial in ZookeeperEphemeralStore
- Fixing documentation handler to handle empty path and paths containing just "restli".
- Throw an exception when getId is called on the response and the key is a complex or compound key.
- make sure we always get consistent ordering of hashes from getPartitionInformation
- Fix incorrect generated snapshot when a typerefed data schema has included schema.

## [1.20.0] - 2014-06-24
- Forbid ID header being directly accessed.

## [1.19.2] - 2014-07-15
- Add API to get multiple hosts from all partitions
- remove checkPathValue from idl/snapshot backwards compatibility checks.
Paths are now expected to be identical.
- Generated included unnamed union in the defining class.
- Update PathKeys.
- Fix bugs in ArgumentBuilder.
- Fix bug in ActionRequestUriBuilder.

## [1.19.1] - 2014-07-01
- return 400 status on input coercion failure
- remove autoboxing from ConsistentHashRing.compareTo

## [1.19.0] - 2014-06-27
- expose partitionInfoProvider to Facilities (this can break classes that implement Facilities)
- Update snapshot generator to expand included schemas in the models list instead of inside the include field.
- fix d2TransportClientProperties schema to reflect what's actually being stored
- Distinguish BatchGet with empty batch keys and GetAll by passing empty "ids=" query parameter from client and handle in server.

## [1.18.3] - 2014-06-25
- add NPE check for transport client compression

## [1.18.2] - 2014-06-23
- Use Gradle 1.12.
- Fix bug in how example requests are generated for batch update and batch partial update.
- Introduced new interface called RestLiResponseData to expose response data to filters.

## [1.18.1] - 2014-06-19
- Fix typo in protocol version header.

## [1.18.0] - 2014-06-17
- Introducing a check inside BatchGetRequestBuilder.build() to fail when the key is CompoundKey or ComplexResourceKey.

## [1.17.3] - 2014-06-16
- Fix issue with inconsistent space encoding/decoding in uri paths.
- Add cache to RecordTemplate field getter/setter whose type needs to be coerced (custom type).
Note that RecordTemplate classes need to re-generated from .pdsc files to activate this feature.
- Add wire attrs as a param.

## [1.17.2] - 2014-06-12
- Re-apply "Re-design Rest.li response API for various batch operations" with performance issue solved.
- Support BatchGetEntity and EntityResponse for ScatterGatherBuilder.

## [1.17.1]
- (We skipped this version)

## [1.17.0] - 2014-06-05
- CreateIdResponse.getId() now throws an exception if the requested Id is a Complex or Compound key.

## [1.16.2] - 2014-06-06
- Match previous changes in BatchCreateIdResponse to BatchCreateResponse
- Temproarily revert "Re-design Rest.li response API for various batch operations" due to performance issue.

## [1.16.1] - 2014-06-05
- remove smurfing ability in D2 KeyMapper
- fix bug in zookeeperAnnouncerJmx

## [1.16.0] - 2014-06-03
- Decoders for responses that require a non-null dataMap will now return null if passed a null dataMap in wrapResponse.
- Allow filters access to strongly typed Ids in batch create responses.
- Keep non-batch query parameters in ScatterGatherBuilder.
- Re-design Rest.li response API for various batch operations. These changes does not include any change in
wire protocols. Changes in the APIs are mainly reflected in the new generated *RequestBuilder classes.
For more information, please refer to https://github.com/linkedin/rest.li/wiki/Rest.li-2.0-response-API

## [1.15.24] - 2014-06-02
- add new method to set partitionData in ZKAnnouncerJMX
expose method to access zkannouncer from ZooKeeperConnectionManager

## [1.15.23] - 2014-05-30
- Allow for clients to recieve strongly-typed keys returned from batch creates.
old builders can cast CreateStatus to CreateIdStatus and then call .getKey
new builders simply return CreateIdsStatuses.

## [1.15.22] - 2014-05-23
- changed rangePartition properties to long because in the actual property, it's long not int

## [1.15.21] - 2014-05-23
- Fix toString, equals and hashCode on idResponse
- Add ability to suppress regex matching failure warning via service properties, for cases where stickiness is desired only some of the time.
- Adding a read only view of ResourceModel for filters

## [1.15.20] - 2014-05-16
- Provide methods to map keys to multiple hosts in KeyMapper

## [1.15.19] - 2014-05-14
- Fix java 7 warnings.
- Allow for clients to receive strongly-typed keys returned from creates.
old builder format:
CreateResponse<K> entity = (CreateResponse<K>)response.getEntity();
K key = entity.getId();
new builder format:
CreateIdResponse<K> entity = response.getEntity();
K key = entity.getId();
Additionally, added back in public wrapResponse function RestResponseDecoder that was removed in 1.15.14,
but it is marked as deprecated.

## [1.15.18] - 2014-05-12
- Add trace level logging of response to DynamicClient
- Make ScatterGatherResult.getRequestInfo() and .getUnmappedKeys() public. KVScatterGatherResult also.
- Clean up caprep so it can be better leveraged for language independent test suite.

## [1.15.17] - 2014-05-08
- Fixed bug where any request with the word restli in it is treated as a documentation request
- Expose hash ring information through jmx

## [1.15.16] - 2014-05-07
- Update D2ClientBuilder to honor d2ServicePath
- PegasusPlugin: Generate list of input pdsc files for generateDataTemplate task at execution time.
- Extract client cert from https request and save it in the RequestContext.
- Support AsyncCallableTasks and documentation requests in the mock http server. Clean up mock http server threadpools. Fix hashCode in ProtocolVersion.

## [1.15.15] - 2014-05-05
- Resurrecting InvokeAwares.
- Checking in support for RestLi filters.
Checking in RestLi filters integration test.

## [1.15.14] - 2014-04-29
- Changes to allow 2.0 URI format.
2.0 URI format will be publicly documented shortly.
Related refactoring of key encoding.
Added many tests to cover both 1.0 and 2.0 URI format.
- add setter for d2ServicePath in D2ClientConfig

## [1.15.13] - 2014-04-29
- Support Avro translation OptionalDefaultMode in PegasusPlugin.
- Fix avro schema translator to not translate default values (that will not be used) when avro override is present.
- Added a PegasusSchema pdsc.

## [1.15.12] - 2014-04-25
- Reapply "add LRU mode and minimum pool size to AsyncPool"
- Add more async pool metrics

## [1.15.11] - 2014-04-24
- PegasusPlugin: Deprecate compatibility level OFF and short-circuit to IGNORE.
- Changing the action parameter setting method name in new client builders to "<parameter name>Param".
- Add support for AsyncR2Servlet in RestliServlet, update examples to use Jetty 8 with async enabled.
- Adding a central place (new module r2-unittest-util) to check in test classes all across r2 and can be used in all r2 tests

## [1.15.10] - 2014-04-17
- Fix scaladoc extractor to not throw an exception on a undocumented param.
- Fixing D2 client to log only the non-sensitive parts of the request.

## [1.15.9] - 2014-04-16
- Fix bug in scaladoc provider where class and object of same were not disambiguated between correctly.
- Fix bug where when maven artifacts are not properly depended on using gradle 1.9+.  This was because
the maven pom contained test and compile scopes for the same artifact.  The fix is to not publish the
test artifact dependencies into maven poms.

## [1.15.8] - 2014-04-14
- Relax master colo check in D2Config if enableSymlink is set.
- Fix a bug where an exists watch gets incorrectly disabled when it's still valid.
- Add symlinkAware option in ZKPersistentConnection.

## [1.15.7] - 2014-04-10
- Fix bug in example generator where query params of complex types are incorrectly
rendered as stringified data maps with { and } instead of the correct URI
representation.
- Removing X-RestLi-Type and X-RestLi-Sub-Type headers.

## [1.15.6] - 2014-04-08
- Add the IP address to RequestContext.
- Use the correct markUp function for ZooKeeperAnnouncers

## [1.15.5] - 2014-04-04
- Use TestNG listener to fail skipped tests rather than ignoring them.
Upgrade quickstart example to support Gradle 1.9+.
- Update restli-int-test data provider to avoid suppressing the rawtypes warning.
- Assume that the server is using the baseline protocol version.
- Add support for URI specific properties to D2.
- Replace dependency of google-collections with guava.
Remove usage of Gradle internal API.

## [1.15.4] - 2014-03-19
- ComplexResourceKey now tries to create key/param record templates using schemas
from the key spec

## [1.15.3] - 2014-03-19
- Added .pdscs for D2 related information into newly created d2-schemas module.

## [1.15.2] - 2014-03-17
- Added new fields to the Request toString method.

## [1.15.1] - 2014-03-13
- Generate alternative version of client request builders.
Change integration tests to test the new request builders.
- Implementation of equals, hashCode, and toString in Request and derived classes.
- Add ability in d2Config to produce d2 symlink for single-master services

## [1.15.0] - 2014-03-12
- Add protocol version header to error response.
Add test for protocol version in error case.
- Fix example generator to include finder params in generated examples, add test.
- Remove hard-coding of format of association keys in IDLs and Builders.
Add tests to ensure backwards compatibility, and make sure the path changes resulting from this in IDLs
are considered backwards compatible.

## [1.14.7] - 2014-03-07
- Add support of enum array in parameter's default value.
- Added test utilities that can be used by application developers to test their Rest.li clients and servers.

## [1.14.6] - 2014-03-04
- Add dataTemplate to generateRestClient classpath for smaller Java binding.
- Deprecate R2 RPC.

## [1.14.5] - 2014-03-03
- Fix bug in Data to Avro schema translation in which assertion will be thrown if the same record schema is included
more than once, and that schema contains fields that either have a default value or is optional.

## [1.14.4] - 2014-02-26
- Making request execution report generated only for debug requests.
- Fix a bug where documentation strings would not show up in idls/snapshots when a method parameter was an array.

## [1.14.3] - 2014-02-24
- Fix a bug where RecordTemplates in Array parameters were not validated.
- Add support of reading symbolic link in Zookeeper.
- Fix bug that single element is added to query param.

## [1.14.2] - 2014-02-19
- Increment parseq version which removes unservable files from the tracevis tar ball.
- Use ProtocolVersionUtil to get protocol version in ErrorResponseBuilder.

## [1.14.1] - 2014-02-14
- Adding set method for Rest.li debug request handlers on Rest.li server config.
- Adding a temporary fix to ignore the unused folders in the parseq-tracevis artifact in maven central.
- Adding debug request handler support to Rest.Li. Introducing a new debug request handler: Parseq Trace Debug Request Handler.
- Fix header display bug in docgen resource page.

## [1.14.0] - 2014-02-13
- Create enum for Rest.li protocol versions.
- Replace hand written data templates with generated ones.
- Move AllProtocolVersions from com.linkedin.restli.common.internal to com.linkedin.restli.internal.common.
- Fail fast when a server receives a request from a client that is encoding using a Rest.li protocol that the server does not support.
- Rename X-Linkedin headers (ID and ErrorResponse) to X-RestLi headers.
- Change zookeeperAnnouncer's markdown() name and implementation so its action is easier to understand
- Shorten the logging in d2 state to be more readable + changed the interface of D2 strategy Jmx
- Make the error details optional in an ErrorResponse to be consistent with previous behavior

## [1.13.5] - 2014-02-04
- Fix for getting the uri in ScatterGatherBuilder and GetAllPartitionsRequestBuilder if the legacy constructor is used.

## [1.13.4] - 2014-01-31
- Fix memory leaks from CopyFilter.

## [1.13.3] - 2014-01-30
- Add scaladoc support to Rest.li IDL generation.
- Fixed a bug where if the deprecated constructor + D2 is used then getting the protocol version will fail in the RestClient as "d2://" is not a valid URI.

## [1.13.2] - 2014-01-29
- Refactor when projections are encoded in the URI.  Move encoding back to the normal URI encoding process.
- Include schemas referenced inline when generating OPTIONS responses.
- Disallow typeref as key type in annotation reader. This fixes the inconsistency between annotation reader and resource model.
- Add scaladoc support to Rest.li IDL generation.

## [1.13.1] - 2014-01-24
- Added add markdown and markup to ZKConnectionManager

## [1.13.0] - 2014-01-24
- Added next protocol version. Set the latest protocol version to 1. Added a FORCE_USE_NEXT ProtocolVersionOption. Updated negotiation code.

## [1.12.4]
- Fix d2 rewrite bug and fix related pathKeys incorrect encoding issue.
- Fix for handling invalid MIME types in accept header. Now, if a request has one or more invalid MIME types in the accept header of the request, the request is rejected with a 400. If the no supported MIME type is found in the specified accept header, a 406 is returned BEFORE the request is processed.
- Fixed assertion ordering in TestRestClientRequestBuilder.

## [1.12.3] - 2014-01-13
- pegasus plugin: Add "overrideGeneratedDir" property to override per-module generated directory.

## [1.12.2] - 2014-01-16
- Added null checks for ComplexResourceKey.makeReadOnly

## [1.12.1] - 2014-01-14
- Revert RB 249757

## [1.12.0] - 2014-01-14
- RestClient now fetches properties for the URI the request is going to before sending the request.
Added RequestOptions at the top level client builders as well as each generated RequestBuilder.
Added Rest.li protocol version negotiation.

## [1.11.2] - 2014-01-10
- Improve Rest.li projection performance, especially in sparse use cases.
Rename DataMapProcessor to DataComplexProcessor. The old DataMapProcessor is deprecated.

## [1.11.1] - 2014-01-10
- Fix d2 rewrite bug

## [1.11.0] - 2014-01-06
- Refactor *RequestBuilders into *RequestUriBuilders that are responsbile for constructing the request URI.
Introduced the concept of a Rest.li protocol version.

## [1.10.7] - 2013-12-03
- Providing a way to get the response payload and status without catching exceptions in case of a Rest.Li error.
- Add more tests for AbstractRequestBuilder.
Use resource stream in restli-tools tests.
- Added injectable headers to resource methods.
Use by adding a param to a resource method like @HeaderParam("Header-Name") headerValue
This allows KeyValueResources to access headers, even though they cannot call getContex.

## [1.10.6] - 2013-12-16
- Add test for DegraderLoadBalancerState
- Improve test for DegraderLoadBalancerState
- Simplify V3 DegraderLoadBalancerState
- Add support for rest.li 'OPTIONS' requests to java client bindings.

## [1.10.5] - 2013-12-13
- Simplify state update logic in degrader balancer strategy in V3
The same change for V2 is made to the new V2_1 strategy to leave
V2 untouched for the safer rollout

## [1.10.4] - 2013-12-13
- Fix bug caused by race condition in resize() of DegraderLoadBalancerStrategyV3
- Fix a bug where CallTracker doesn't honor the use of LoadBalancer interval

## [1.10.3] - 2013-12-10
- Generate error that was not previously detected when trying to set incompatible overriding default value in
outer type (e.g. record) that overrides default of an inner type (e.g. string field within record.)
- Add support for schema JSON strings greater max Java string literal length.
- Add propagation of deprecated keys used on types and fields in pdscs to generated java data templates.

## [1.10.2] - 2013-12-06
- fix a problem where threads will get locked if there is an uncaught exception being thrown during updateState in LoadBalancerStrategy
- Add javadoc to SchemaSampleDataGenerator.
Implement sample data callback for SchemaSampleDataGenerator.

## [1.10.1] - 2013-12-06
- Remove logging from data.

## [1.10.0] - 2013-12-03
- Upgrade Jackson to 2.2.2.

## [1.9.49] - 2013-12-02
- Fixed log error message in ClientServiceConfigValidator.

## [1.9.48] - 2013-12-02
- Fix bug in ClientServiceConfigValidator. We were previously casting the values directly to an int. However, this is incorrect as the values in the map are Strings.

## [1.9.47] - 2013-11-22
- Fix of getClient for scatter/gather and search
- Replacing IdentityHashMap in RecordTemplate, WrappingMapTemplate and WrappingArrayTemplate with a custom cache implementation.

## [1.9.46] - 2013-11-20
- Disable data object checking on safe and performance-critical situations.
- Added compatibility checking to annotations. Annotation changes will now be considered compatible rather than
simply skipped over and thus considered equivalent.
- Add functionality of listening to all children's data under a certain znode in ZooKeeper.

## [1.9.45] - 2013-11-14
- Add permissive option to degrade on serializing bad user data

## [1.9.44] - 2013-11-13
- Adding perf test for Record Template put performance.
- Make skipping publishRestliIdl task more precise by taking advantage to changes to CompatibilityInfoMap.
PublishRestliIdl should now be skipped if there are only model changes.
- Add support for deprecated annotation.

## [1.9.43] - 2013-11-08
- Only validate union data if map has a single entry

## [1.9.42] - 2013-11-07
- Add @TestMethod annotation to indicate which methods on a resource are intended to only be used for testing.
- Add compatibility checking between snapshot and idl.
- Fixing the onlyIf closure for Publish tasks, adding more logging to ease debugging for future.
- Fix bug that schema compatibility checking throws exception of "java.util.MissingFormatArgumentException: Format specifier 's'".
- Support per-sourceSet pegasus/snapshot/idl override properties.
- Fix missing doc field in generated snapshot/idl files, which is caused by multi-threaded generation.

## [1.9.41] - 2013-10-18
- Refactor r2 asyncpool stats to make it visible outside the package.

## [1.9.40] - 2013-10-25
- Fix a bug where SimpleLoadBalancerState didn't remove an old entry in cluster -> services
mapping when SimpleLoadBalancerState receive a service changes notifications from Zookeeper.
At the same time we are adding more JMX handle to load balancers to allow more control at runtime.
- Fix two bugs related to snapshots:
snapshot compatibility messages during checkSnapshot task should now print correctly.
snapshots of simple resources should be generated correctly.
- break up compatibility info in CompatibilityInfoMap into two maps: one for tracking info from restSpecs, the other for
tracking info from models.  Added new methods for extracting this information from the infoMap.  Old methods for getting
general data are still around.  Backwards-incompatible changes to method names for inserting info into compatibilityInfoMap.

## [1.9.39] - 2013-10-23
- Improving Pegasus build messages for network parallel builds. Making sure the access to static variables are synchronized.
- Add additional http status codes to list.

## [1.9.38] - 2013-10-22
- Make d2 test artifacts visible.

## [1.9.37] - 2013-10-21
- added logic to prevent callDroppingMode in LBStrategy to be changed when traffic is low
- Change emitted message on successful build to include a listing of all published
IDLs and snapshots that likely need to be committed.
- Fixes to checkIdl task in PegausPlugin.  Some small clean-up in compatibility tasks:
Only initialize a single checker class rather than one per pair of files, and don't
bother setting resolver paths when checking snapshots of file counts.
- Fix a bug in R2 that a pooled channel can be disposed twice.
- Add operation information to the resource context to enable logging on the server side.
- Made get data length safe in RetryZooKeeper
- Fixed the READMEs in the examples folder and converted them to Markdown
- Fixed a bug in Snapshot generation relating to entity-level Actions and Finders in
Association resources.

## [1.9.36] - 2013-10-14
- Fixes to make Rest.li build on Windows.
- Fix DynamicRecordTemplate to accept DataList argument while setting fields of type array.
- Enabling complex key based look ups on BatchKVResponse objects. Fixing a query parameter array serialization issue in BatchKVResponse for Complex Keys.
- Refactored Count checks as individual tasks out of PegasusPlugin, and reintegrated them back into
regular compatibility checks.
Changed the message emitted with changes.
New message will appear if a compatibility check is run on what appears to be a continuous integ.
environment (where -Prest.model.compatibility=equivalent).
- Revert suspicious changes in R2 AsyncPool that may cause site relibility issue.

## [1.9.35] - 2013-10-07
- Add ability to collect and export R2 AsyncPool Stats
- Add ability to config R2 AsyncPool strategy between LRU and MRU.

## [1.9.34] - 2013-10-02
- Enabling Async R2 Servlet

## [1.9.33] - 2013-10-03
- Disallow null values in setParam. Add more tests.

## [1.9.32] - 2013-10-02
- Fix the allowed client override keys.

## [1.9.31] - 2013-10-01
- Revert "Make use of async servlet api in R2 servlet. Change integration tests to start test servers as necessary."

## [1.9.30] - 2013-09-30
- Allowed access to the ResourceModels of a RestLiServer. Made the resourcePath generation function public.
- Fixing binary incompatible removal of header, param and reqParam methods on client builder base classes.

## [1.9.29] - 2013-09-27
- Rename X-Linkedin headers to X-RestLi headers.
- Fixed a bug in SimpleLoadBalancerState that prevented recovering from a bad property push during publishInitialize

## [1.9.28] - 2013-09-24
- Make use of async servlet api in R2 servlet. Change integration tests to start test servers as necessary.

## [1.9.27]
- Refactor restli-client request builder classes:
  1) deprecate header(), param and reqParam()
  2) add setHeader(), setHeaders(), addHeader(), setParam(), setReqParam(), addParam() and addReqParam()
For query parameter and action parameter that is array type, add convenient request builder method to add element one by one.
For ActionRequestBuilder, required parameter will call reqParam() instead of param() now.

## [1.9.26] - 2013-09-18
- Added the ability to inject MaskTree (@Projection) and PathKeys (@Keys) from a
request into a method.  This allows KeyValueResources to be able to use
Projections and PathKeys in their method implementations.
- Fix bug that when complex resource key contains invalid URI characters (e.g. space), batch update fails with URISyntaxException.

## [1.9.25] - 2013-09-17
Added ability for clients to specify either actual lists or string representation of lists for transport client properties.

## [1.9.24] - 2013-09-13
- Refactor IDL and Snapshot compatibility checks. Move file number checks to their
own tasks. Add in a flag -Prest.idl.skipCheck to allow all IDL checks to be
skipped. (IDL file count check is still run with -Prest.idl.compatibility=OFF)
- Add InvokeAware interface to allow user code to listen to the restli method invocation events in restli server.
- Add ProjectionMode option in ResourceContext to allow rest.li service implementers
to disable automatic projection when they are explicitly examining and applying
projections.

## [1.9.23] - 2013-09-10
- To detect, as early as possible, a mistake that is otherwise difficult to debug, add
check during data template generation that verifies filename and path match schema
name and namespace.
- Add configuration to allow the rest.li server to limit exception details in responses and to customize the default response for internal server error responses.

## [1.9.22] - 2013-09-05
- Allow routing to batch partial update with no "X-RestLi-Method" HTTP header.
- Support more HTTP header manipulation methods in restli-client request builder.

## [1.9.21] - 2013-09-05
- Add spring and guice support, enables running rest.li servlets with dependency injection, also add a logging filter.
- Fix bug in D2Config that service variant doesn't point to master colo when defaultRoutingToMaster is set.
- Fix bug that R2 Client may lose connection forever after the server being bounced when there is a very high downstream
qps and D2 is not used.

## [1.9.20] - 2013-09-03
- Removed the notion of client only supplied config keys. Fixed bug in reading set from transport client properties.

## [1.9.19] - 2013-08-30
- Fix bug when GenericArrayType is used in action return type.

## [1.9.18] - 2013-08-27
- Fixed bug in client only config key-values.
- Add support for returning error details in batch create responses.
- Implement context path for Jetty server.

## [1.9.17] - 2013-08-26
- fix isRegistered in JmxManager
- Added ability for clients to provide service level configs. Added support for clients to enable response compression.
- Add thread pool size configuration parameters to RestliServlet, NettyStandaloneLauncher and StandaloneLauncher (jetty).
- Allow an boolean expression of predicate names to be passed to FilterSchemaGenerator.
Add NOT predicate.

## [1.9.16] - 2013-08-20
- add isRegistered to JmxManager to find out whether a bean has been registered to jmx
- Changing the dev default of the compat checker to BACKWARDS.

## [1.9.15] - 2013-08-15
- Remove unneeded dependencies on r2-jetty to avoid dragging jetty dependency downstream

## [1.9.14] - 2013-08-13
- Print warning for the deprecated configuration from the pegasus plugin.
Correct variable names in the pegasus plugin.
- Relaxing the action parameter check to allow them on all method types as before.

## [1.9.13] - 2013-08-12
- Added batch operations to the async complex key template.
- Fixing the schema resolution ordering problem.
- Disallow @QueryParam in action methods, disallow @ActionParam in non-action methods.
- Added support for shutting down the ZK connection asynchronously in the d2 client and ZKFSLoadBalancer.

## [1.9.12] - 2013-08-09
- Fixing data template generator to process type refs specified as array and map items.
- Add class to filter DataSchemas in a directory by removing unwanted fields or custom properties of the schema according to given Predicate.
- Improve FileClassNameScanner to 1) require a specific extension; 2) exclude whose guessed class name contains dots.

## [1.9.11] - 2013-07-26
- Added batch operations to the async association template.
- allow specifying an empty string for coloVariants, useful in testing.

## [1.9.10] - 2013-08-07
- Fix a problem that can block Netty boss thread for a long time.
- Fixed issue with Complex Keys with fields arrays containing a single element in get requests.
- Fixing Example Generator to create correct request body for partial updates.
- Added batch methods to the async interface and template for simple (non complex key, non association) collections.
- Fixing couple of issues in annotated complex-key resources and async complex-key resource template. Adding extensive test covarage for both scenarios.
- Add Union template builder method per member.

## [1.9.9] - 2013-07-22
- fix the bug where threads that are waiting for state initialization, never stop waiting because the init step throws an exception

## [1.9.8]
- Added fix to prevent a complex key when a CollectionResource is being used.

## [1.9.7] - 2013-07-29
- Protect D2 from intermittent zookeeper problem

## [1.9.6] - 2013-07-28
- Changed Snappy dependency to pure Java dependency to avoid JNI issues on Jetty.

## [1.9.5] - 2013-07-25
- Add HttpNettyServerFactory and standalone launcher.

## [1.9.4] - 2013-07-25
- Fixed issue with snapshots generation failing when referenced pdscs were circularly dependent.
Added tests to make sure that Snapshot generation and reading would work correctly with
circularly dependent models.
- Added granular set methods for pagination start and count for getall and finder client builders.

## [1.9.3] - 2013-07-18
- fixes snapshot incompatibility message printouts.
- removes unused property keys and removes the non http-namespaced properties referenced in D2 code
- Move AvroSchemaGenerator out of data-avro due to logging dependency requirement.
- Adding support for partial update methods on simple resources.
- Bug fix with compression client filter Accept-Encoding generation
- Added string constructors to compression filters.
- Use ParSeq 1.3.3, which depends on log4j 1.6.2 and converges to the same dependent version as Rest.li uses.
Add missing log4j.xml to restli-example-client.

## [1.9.2] - 2013-07-03
- Simplify and speed up string intern tests in TestJacksonCodec. This only affects tests.
- Adding support for java array return and input parameters for actions.
- Add separate compatibility check for idl.
Add flag to turn off snapshot and idl compatibility check respectively.

## [1.9.1] - 2013-07-03
- Fix bug in pegasus plugin that publish snapshot task may not run.
- Fix up jdk7 warnings.
- Added server/client compression filters and associated test cases.
- Adjust log4j related dependencies and log4j.xml. Remove all compile-time dependency of log4j.

## [1.9.0] - 2013-07-01
- Introduce simple resources concept which serves a single entity from a particular path.
- Clean up SLF4J/Log4j mess by removing all dependencies on Log4j and
the SLF4J/Log4j adapter from production jars.
If your executable (war file, etc.) does not already depend on an SLF4J
adapter, you may need to introduce such a dependency, for example on
slf4jlog4j12.
- Incorporate snapshot into pegasus plugin. All existing projects will automatically generate and publish the snapshot files.
- add defaultRouting option to d2Config.

## [1.8.39] - 2013-06-20
- pegasus plugin and idl compatibility checker will check for missing and extra published idl files.

## [1.8.38] - 2013-06-25
- When generating idl, pass the source files of the resource classes to Javadoc.
When checking idl compatibility, doc field change is now a backwards compatible change instead of equivalent.
- Update gradle plugin to check source of all languages when deciding if idl generation should be skipped.  This fixes a bug where scala
*Resource.scala files were ignored.
- Use PegasusPlugin to build pegasus integration test modules and examples.

## [1.8.37] - 2013-06-18
- Fix a pegasus plugin regression about null pointer.

## [1.8.36] - 2013-06-18
- Fix HttpClientFactory.shutdown() with timeout so it does not tie up
the executor for the length of the timeout.
- Snapshots implemented locally in pegasus.  PegasusPlugin was not changed, so others using pegasus won't be able to use Snapshots yet.
Within the project, Snapshots are now used instead of IDLs for backwards compatibility checking.  (IDLs are still used to create builders
and are the source of truth for client-server interaction, however)  Snapshots have the advantage that they contain the models that they
reference, so backwards incompatibile changes between models can now be noticed.
- Gradle plugin: Add missing data and restli-client dependencies to javadoc task classpath. Add test and clean up source code.

## [1.8.35] - 2013-06-12
- In pegasus plugin, fix bug that avro schema generation is run unconditionally. Now avroSchemaGenerator configuration will be respected again.
  Note that there is a new preferred appraoch to do this. Please refer to plugin comments.
In pegasus plugin, if a source set does not have jar task, skip publishing idl.

## [1.8.34] - 2013-06-12
- Register listener before task execution for rest.li async methods that return promises.

## [1.8.33] - 2013-06-07
- Add functionality to generated idl files for all source files under a source directory.
- Remove dependency of system properties from build.gradle in restli-tools.
- Fix incorrect schema field for idl files.
- Update Gradle plugin to allow server module skip specifying idlItems. In such case, all source files will be scanned.
- The generators and tools the Gradle plugin depends on become runtime dependency so that user no longer needs to specify
  in the module dependency.
Allow dataTemplateCompile and restClientCompile configurations to be overridden.
- Add RestliBuilderUtils, modify RestrequestbuilderGenerator to have static ORIGINAL_RESOURCE_NAME and getter
moved the log4j.xml files in the d2 and restli-server src dirs to the test dirs.

## [1.8.32] - 2013-05-30
- Added PatchHelper class with method which allows applying projection on patch.
- Instead of getting properties from system properties, create config class for the data and Rest.li generators.
Hide the existing "run()" functions in the concrete generators to private generate() and provide static run() to pass required properties. Command-line main() will still use system properties.
Update the gradle plugin to use the new generator pattern. There is no need for synchronization block and support parallel build.
Remove dataModelJar and restModelJar artifacts from the plugin.

## [1.8.31] - 2013-06-03
- Interfacing the gradle plugin for LinkedIn specific version. 3rd party plugin could dynamically load the plugin and customize its properties.

## [1.8.30] - 2013-05-31
- Fix backward incompatible param change to RestLiResourceModelExporter.export()

## [1.8.29] - 2013-05-30
- Refactor IDL compatibility checking. Allow compatibility checking of referenced named Schemas.
Slightly alter some compatibility messages.
- Add -resourceclasses option to idl generator command line application.
- Update Gradle plugin. Use this version as source of truth in LinkedIn toolset.

## [1.8.28] - 2013-05-28
- Fix interface definition generation for typerefs in action return types and refactor RestLiAnnotationReader
to make action validation easier to understand.

## [1.8.27] - 2013-05-24
- Revert eec968ddab745286a8c9e05e35f0ddeab011a947 "Refactoring changes for testing resource compatibility."
as it breaks rum publishRestModel with this message:
"No such property: summary for class: com.linkedin.restli.tools.idlcheck.RestLiResourceModelCompatibilityChecker"

## [1.8.26] - 2013-05-24
- Add RestClient.sendRestRequest(..., Callback<RestResponse> callback) method.

## [1.8.25] - 2013-05-21
- Add support for enum value documentation for data template generator.
- Fix bug where client builders failed to coerce batch results for resource collections keyed by a typeref.
- Use com.linkedin.restli.server.NoMetadata to mark a finder's CollectionResult as no metadata.
Allow non-template return type for finders.
- IDL compatibility checks for new methods, finders, actions and subresources.
- Fix idl generation to correctly handle typerefs in action responses.

## [1.8.23] - 2013-01-29
- Change FixedTemplate to output using ByteString.toString() instead of asAvroString
Add more test cases for generated DataTemplate.
- Fix bug where @Optional on finder assocKeys was not respected.
- Fix a bug in idl compatibility checker that marks previously required and currently optional field as incompatible.
- Deprecate the "items" field for the query parameters in idl. Array parameters use standard pdsc array format.
To make it backwards compatibile, request builders can still use Iterable parameters.
Fix bug that builder methods with Iterable<? extends DataTemplate> parameter are not working.
Update build scripts.
Use Gradle 1.5.
- Add special rule of idl compatibility checker to handle the deprecated "items" field.

## [1.8.22] - 2013-05-06
- fix logging message for D2
- Use thread context classpath instead of pegasus classpath when using Class.forName on names of
coercers, validators and avro custom data translators.
- Add copy() and clone() methods to generated non-record DataTemplates.
Generated record DataTemplates have had these methods since 1.8.4.
- Adding new resource class is a backward compatible change now.
Add instruction message for idl compatibility check failure.

## [1.8.21] - 2013-05-03
- Fix UnsupportedOperationException from SimpleLoadBalancerSTate while creating transportClientProperties for https

## [1.8.20] - 2013-05-02
- made TARGET_SERVICE_KEY_NAME a public static variable
- Fix bug where shutdown of HttpClientFactory will fail if the final
event leading to shutdown occurs on a Netty IO thread.
- Support typerefs in association keys for batch responses.
- Disable interning of field names by Jackson parser.
This should reduce intended growth in perm gen.
- Add embedded schema to Avro schema translated from Pegasus schema
This allows reverse translation without loss (e.g. loss of typeref, custom translation instructions).

## [1.8.19] - 2013-04-10
- Fix bug that context path is missing in docgen "View in JSON format" link.
- Add SSL support in D2 client.

## [1.8.18] - 2013-04-25
- Fix NPE in Data Template generator when an array item or map value type is a typeref'ed union.
- Fix queryParamsDataMap not able to convert single element query to StringArray

## [1.8.17] - 2013-04-22
- fix default and master service bugs in D2ConfigCmdline

## [1.8.16] - 2013-04-12
- Allow repeat registration of a coercer *only* if the coercer is the same class as already registered.
- add ability to exclude individual services from colo variants in d2-config-cmdline

## [1.8.15] - 2013-04-10
- moved transportClient, degrader and many other cluster properties to service properties (part 2)
- make sure that a marked down server is not marked up by ZookeeperConnectionManager when the zookeeper connection is expired
- Add "View in JSON format" link to all docgen pages in the navigation header.

## [1.8.14] - 2013-04-09
- Improve client side logging of RestLiServiceException
- Fix race condition between ZKConnection.start() and DefaultWatcher.process() by waiting for initialization completion
This replaces RB 149393

## [1.8.13] - 2013-04-05
- Reapply "moved transportClient, degrader and many other cluster properties to service properties (part 1)"
Push the config producing code first then push the config consuming part later.
- minimize the amount of logging that D2 makes when there is no state changes

## [1.8.12] - 2013-04-04
- Reverted "moved transportClient, degrader and many other cluster properties to service properties (part 1)"
- Update RestLiConfig to allow RestLiServer to load specific resource classes.
- Restore binary compatibility by changing return type of ScatterGatherBuilder$RequestInfo.getRequest()
back to Request (it was changed to BatchRequest in 1.8.9; this change was source compatible
but not binary compatible).

## [1.8.11] - 2013-04-04
- moved transportClient, degrader and many other cluster properties to service properties (part 1)
Push the config producing code first then push the config consuming part later.
- bump to 1.8.11

## [1.8.10] - 2013-04-02
- Add detection of wrong assocKey in RestRequestBuilderGenerator.
Add display of assocKeys of finder in restli-docgen.
- Added RoutingAwareClient to facilitate service name lookup from a routeKey
- bump to 1.8.10

## [1.8.9] - 2013-03-29
- Added ScatterGather support for BatchUpdates and BatchDeletes.
Made a backwards incompatible change to ScatterGatherBuilder.RequestInfo constructor; it now
accepts a BatchRequest instead of Request.
- bump to 1.8.9

## [1.8.8] - 2013-03-27
- Added jmx methods to query trackerClient and number of hashpoint.
- Add dataModel build script and use in restli-common to publish EmptyRecord
and other core restli schemas so they can be referenced by other projects.
- fix for ZKConnection/DefaultWatcher race condition

## [1.8.7] - 2013-03-20
- Performance optimization for construction of query params to avoid
needlessly appending the array index as a string for each field in
a list only to remove it later.
- Deprecate AbstractValidator default (no-arg) constructor. See class
comments for context.
- Potential fix for Avro Schema Translator transient problem where
some embedded/contained schemas are not being translated.

## [1.8.6] - 2013-03-11
- Fix up RestLiServiceException.toString() and update ErrorResponse
schema to correctly reflect optional fields.
- Add ColoVariants to D2Config

## [1.8.5] - 2013-03-04
- Add pdsc file and validator for EmptyRecord.
- Workaround bug in ScheduledThreadPoolExecutor that caused
delays when calling HttpClientFactory.shutdown().
- Order subresources when restspec.json is exported. This avoids massive changes in restspec.json when
resources are added or removed. (This is due to internal use of HashMap.)
- add ClientBuilderUtil.addSuffixToBaseName.
- Fix bug in translating null value in a union with null when translating from Avro data to Pegasus.
- Performance tuning for requests with large numbers of query params.
- Modified LoadBalancerStrategy to use error rate for load balancing

## [1.8.4] - 2013-02-21
- Fix to PSON deserialization issues.
PSON responses should not deserialize correctly.
The default representation for PSON strings is now a length-encoded string.
All length encoded strings are now encoded with a two byte length by default.  This is a backwards-
incompatible change.
- Allow Content-Types to include an optional Charset.  For now it is ignored, but including it will
no longer allow either the client or the server to be unable to parse the Content-Type.

## [1.8.3] - 2013-02-12
- Fix UnsupportedOperationException from UnmodifiableMap in SimpleLoadBalancerState.

## [1.8.2] - 2013-02-07
- Add PatchTreeRecorder & PatchRequestRecorder to build patches that allow you to remove fields.
- Allow clients to send request bodies in pson format. Upgraded servers will be
able to interpet bodies in pson format.
- Remove legacy server code that uses ',' as separator for batch_get ids.  Correct format is "?ids=1&ids=2".

## [1.8.1] - 2013-01-28
- Revert RB 126830 until compatibility issues are resolved.

## [1.8.0] - 2013-01-23
- Increasing version to 1.8.0, because 126830 is wire-compatible, but compile-incompatible.
- Modified D2ClientBuilder to accept load balancer factory as a parameter.

## [1.7.12] - 2013-01-25
- Add RestliServlet to provide developers with a simple way to build a war using rest.li.
- Deprecate the "items" field for the query parameters in idl. Array parameters use standard pdsc array format.
To make it backwards compatibile, request builders can still use Iterable parameters.
Fix bug that builder methods with Iterable<? extends DataTemplate> parameter are not working.

## [1.7.11] - 2013-01-25
- Change build scripts to work with Gradle 1.3.
- Add RestliServlet to provide developers with a simple way to build a war using rest.li.

## [1.7.10] - 2013-01-24
- Add methods for common uses for ResponseFuture.getResponseEntity and RestClient.sendRequest(RequestBuilde ...)
client.sendRequest(builder.build()).getResponse().getEntity() can be simplified as follow to
client.sendRequest(builder).getResponseEntity();

## [1.7.9] - 2013-01-24
- add try/catch to PropertyEvent runnables, add UnhandledExceptionHandler to NamedThreadFactory
- fix a bug where the LoadBalancer config gets overwritten by empty map and causes D2 Strategy
to not instantiate properly
- Change to allow clients to request data in pson-encoded format (and interpet pson-encoded data),
and for servers to be able to send pson-encoded responses.
Clients can signify that a response should be in pson format by sending the request with the
header "Accept-Type : application/x-pson".  The server will then encode the result in pson and
send it back with the header "Content-Type : application/x-pson". If the client recieves a
response with this header it will decode it with the pson codec.
Some headers will now work a bit differently:
Content-Type headers will no longer be sent with responses unless there is actual body content
to encode.  This change was made primarily to simplify picking the right header.  There's no
point in trying to figure out the right content-type header to send back if there isn't
actually any content to send.
Accept-Type headers can now be sent with requests.  The default client won't send Accept-Type
headers (same as the old code), but users can use the new RestClient constructor to create a
client that will send Accept-Type headers.  Right now there are four basic options for
Accept-Type headers:
 - no header: server will send back result as application/json.  This is required for backwards
   compatibility.
 - application/json highest quality in header: server will send back result as application/json
 - application/x-pson highest quality in header: server will send back result as
   application/x-pson.  If the server code is old, result will be sent back as application/json
 - */* highest quality in header: for now, server will send back result as application/json, if
   no other accept types are found.  However, the server will prefer to send back responses in
   formats that are explicitly mentioned in the header, even when they are lower quality than */*
- ActionResponseDecoder.getEntity() will return Void.class if its fieldDef is
null, to preserve compatibility from before the Action response changes.
- Add javadoc to rest.li docgen and include restspec.json files as resource in rest.li server jars.

## [1.7.8] - 2013-01-16
- Add default value handling for query parameter in complex type, including all DataTemplate subclasses, array of simple types and complex types.
Union can be used as query parameter type.
- Fix NPE resulting from calling .getEntityClass() on an ActionResponseDecoder for a void-returning Action.

## [1.7.7] - 2012-12-21
- Add TextDataCodec to support serializing and deserializing to String, Writer and Reader.
Move getStringEncoding() from DataCodec to TextDataCodec interface. This is potentially
a backwards incompatible change.
Replace use of ByteArrayInputStream(string.getBytes(Data.UTF_8_CHARSET)) with new JacksonDataCodec
and SchemaParser APIs that take String as input.

## [1.7.6] - 2012-12-20
- If union is named because it is typeref'ed, the typeref schema was
originally not available through the generated code. This change
add a new HasTyperefInfo interface. If the union is named through
through typeref, the generated subclass of UnionTemplate will also
implement this interface. This interface provides the TyperefInfo
of the typeref that names the union.
- Fix encoding bug in QueryTunnel Util.
Make ByteString.toString() to return a summary instead of the whole
array as an Avro string.
HttpBridge for RPC requests should not log the whole entity.
Remove Entity body from Request/Response toString().
- restli-docgen displays all nested subresources and related models in the JSON format.

## [1.7.5] - 2012-12-18
- Move PsonDataCodec from test to main source dir.

## [1.7.4] - 2012-12-17
- RequestContext should not be shared across requests in ParSeqRestClient

## [1.7.3] - 2012-12-17
- Add support for Avro 1.6. To use Avro 1.6, depend on data-avro_1_6.
Also fix getBytes() to explicitly specify UTF-8. This has no impact
on platforms whose default encoding is UTF-8.
- Add DataList serialization and deserialization to JacksonDataCodec.

## [1.7.2] - 2012-12-13
- Infer order of include and fields properties of record if location information is not available.
Change generated and up-to-date log messages to info. This was useful initially for debugging. Since
it has not been a problem, changing to info will reduce build output noise from generator.
- Add requisite maven configuration and pom generation to root build.gradle to enable releasing pegasus
to maven central.
- Copy 'pegasus' gradle plugin into pegasus codebase from RUM, so 3rd party developers have access to
the build tools required for a working development flow.  Also add maven central and maven local as repos
so developers can publish pegasus artifacts to their local repo and build standalone apps based on those
artifacts (this part will not be needed after we push pegasus artifacts to the maven central repo but
helps in the short term).
- Fixed an issue where Actions that declare their return types as primitives (return int instead of
Integer, for example) no longer fail while trying to coercer the response into the correct type.

## [1.7.1] - 2012-12-13
- Bad build, not published

## [1.7.0] - 2012-12-10
- Add Schema compatibility checker. See com.linkedin.data.schema.compatibility.CompatibilityChecker and
CompatibilityOptions for details.
There is a change in MessageList class to take a type parameter. This is binary compatible but may
result in unchecked compilation warning/errors (depending on compiler setting.) Impact should be
minimum since this class is mostly for use within pegasus. However, it leaked by data-transform
package by DataProcessingException. This has been fixed to use List<Message> instead of MessageList.
- In idl compatibility checker, allow parameter optional to be upgraded to default, and allow default to be downgraded to optional.
- Add PageIncrement.FIXED to better support post-filtered search result paging.

## [1.6.14] - 2012-12-07
- Add handling of long queries via X-HTTP-Method-Override
- In idl compatibility checker, allow finder AssocKey to be upgraded to AssocKeys, but disallow the opposite direction.

## [1.6.12] - 2012-12-05
- Fix bug in Avro generator in which referenced schema is not generated even
if schema file or name is explicitly mentioned as input args to avro schema
generator.
- Fix bug in Avro schema and data translator that occurs when optional typeref
of union present. Significantly improve test coverage for typeref for avro
data and schema translation.
- Add Request.getResourcePath() to provide access to the resource path parts that uniquely identify what resource the request is for.
- Fix a bug where @AssocKeys of CustomTypes would cause IDL generation to crash.
Added test cases for @AssocKeys of CustomTypes.

## [1.6.11] - 2012-12-04
- Fix a bug in DegraderLoadBalancerStrategyV2 and DegraderLoadBalancerStrategyV3 that will not recover if we reach complete degraded state
- Changed RestSpecAnnotation.skipDefault default from false to true.
- All sub-level idl custom annotations are always included in class level.

## [1.6.10] - 2012-11-29
- Preserve PropertyEventBusImpl constructor backward compatibility

## [1.6.9] - 2012-11-28
- Split original Restli example server/client into two versions: Basic and D2. The Basic version does not contain any D2 features.
Improve the D2 version of server and client to fully utilize D2.
Add gradle tasks to start all the variants of servers and clients.
Add gradle task to write D2-related configuration to ZooKeeper.
- Restore method signatures changed in 1.6.7 to preserve backward compatibility

## [1.6.8] - 2012-11-27
- Revert "Don't log entity body in Rest{Request,Response}Impl.toString(), since it's likely to log sensitive data."

## [1.6.7] - 2012-11-27
- Fix a bug in batching multiple get requests into one, and refactor query parameters handling in
Request and RequestBuilder hierarchy.
- Custom Types will now work as keys.
Keys keep track of their own schemas.
Reference types for keys are annotated in the class level annotation, as a new parameter in
RestLiCollection as keyTyperefClass, or as part of the @Key annotation for associations.
Added docgen to restli-server-standalone config.
- Custom Types will now work with action parameters.
FieldDefs/Params now keep track of their own schemas.
Action parameter metadata is now calculated in a static block in generated builder code --
no longer generated on the fly at call-time.
Action response metadata is now also calculated in a static block or in the AnnotationReader,
rather than on the fly at call-time.
Fixed a typeref bug that would cause non-custom type typerefs to appear in builders as their
reference types rather than their underlying types.

## [1.6.6] - 2012-11-15
- Fix SI-515.  Escape '.'s in keys from QueryParamDataMap so AnyRecords can be encoded as query params.
- Fix url escaping of string when used as keys in rest.li. (SI-495)

## [1.6.5] - 2012-11-05
- Rename startServer task in restli-example-server to startExampleServer.
Rename RestLiExamplesServer in restli-int-test-server to RestLiIntTestServer.
The old startServer task is still used to start the current restli-int-test-server.
- Change idl custom annotation default value of skipDefault to false.

## [1.6.4] - 2012-10-25
- Allow custom annotations in resource classes to be passed to generated .restspec.json files.
- Add D2ClientBuilder class, which conveniently generates D2Client with basic ZooKeeper setup.

## [1.6.3] - 2012-10-25
- pass requestContext up to restli layer.

## [1.6.2] - 2012-10-01
- Move non-LI-specific part of photo server example into pegasus.

## [1.6.1] - 2012-10-19
- Integrate compatibility level into idl checker. The exit code of the main function now depends on both
the check result and the level.
- Fix incorrect handling of absent optional complex query parameters.

## [1.6.0] - 2012-10-17
- Add "validatorPriority" to enable validator execution order to be specified.
See details in DataSchemaAnnotationValidator class.
 <b>Validator Execution Order</b>
 <p>
Execution ordering of multiple validators specified within the same "validate"
 property is determined by the "validatorPriority" property of each validator.
<code>
   "validate" : {
     "higherPriorityValidator" : {
       "validatorPriority" : 1
     },
     "defaultPriorityValidator" : {
     },
     "lowerPriorityValidator" : {
       "validatorPriority" : -1
     }
   }
 </code>
 <p>
The higher the priority value, the higher the priority of the validator, i.e.
 a validator with higher prority value will be executed before the validators
 with lower priority values. The default priority value for a validator that
 does not specify a priority value is 0. Execution order of validators with
 the same priority value is not defined or specified.
 <p>
Validators may be attached to a field as well as the type of the field.
 This class will always execute the validators associated to the type of the field
 before it will execute the validators associated with the field.
 <p>
If schema of a data element is a typeref, then the validator associated with
 the typeref is executed after the validator of the referenced type.
 <p>
Beyond the above execution ordering guarantees provided by this class,
 the execution order of validators among different data elements is determined
 by the traversal order of the caller (i.e. how data elements passed to the
 {@link #validate(ValidatorContext)} method of this class. Typically, the caller will be
 {@link com.linkedin.data.schema.validation.ValidateDataAgainstSchema}
 and this caller performs a post-order traversal of data elements.
There is an incompatible semantic change. Previously the outer typeref validators
are executed before the inner typeref validators.
- Fix bug to not throw NPE when include schema is not valid.
When RuntimeException is thrown by code generator, make sure that accummulated
parser messages are emitted through a RuntimeException to help diagnose the
cause of the RuntimeException.

## [1.5.12] - 2012-10-16
- Fix StackOverflowError when generating mock data for schema that recursively references itself.
- Move SSL configuration to from HttpClientFactory down to TransportClientFactory.

## [1.5.11] - 2012-10-11
- Fix NullPointerException in testcase's shutdown method.

## [1.5.10] - 2012-10-05
- Fix bug with double-encoding spaces in query parameters.

## [1.5.9] - 2012-09-24
- retry d2-config-cmdline on connectionLossException

## [1.5.8] - 2012-09-24
- Add doc and Javadoc of source resource class name to generated idl and client builder.
- Allow http status code be specified in GET methods and Action methods. For GET, define custom GET method (by annotating
with @RestMethod.Get) with return type GetResult<V>. For Action, define the action method with return type
ActionResult<V>.

## [1.5.7] - 2012-09-19
- Fix NPE in RestRequestBuilderGenerator when processing legacy IDL format.

## [1.5.6]
- Generated rest client builders now contain Javadoc extracted from .restspec.json files.
Such document originally comes from the Javadoc of corresponding resource classes.

## [1.5.5]
- Add consistency check between SSLContext and SSLParameters arguments
of HttpNettyClient constructor.
- Deprecate RestLiConfig.setClassLoader().  RestLi now loads resource
classes using the current thread's contextClassLoader.

## [1.5.4]
- Enhance JSR330Adapter to support injection via constructor arguments,
allowing a safer coding practice of declaring final member variables
in rest.li resources.
- RestLiResourceModelExporter now returns a GeneratorResult of files modified/created so it is more consistent with the
other generator classes.

## [1.5.3]
- Detect class name conflicts that occur when a generated class name
is the same as the class name for a NamedDataSchema.
Also clean up DataTemplateGenerator code.
Array items and map values of generated classes is always the
the first schema with custom Java binding or the fully
dereferenced schema if there is no custom Java binding.

## [1.5.2]
- Add SSL support to R2 http client.

## [1.5.1]
- Remove cow.

## [1.5.0]
- Fix bug of JMX bean
- Follow on change to remove old Rpc code in data.
- Fix javadoc, imports, syntactical changes in data.
- Remove support for RpcEndpointGenerator and ExceptionTemplates - this functionality has been
deprecated and is currently unused.
- Fix bug that restli-docgen fail to initialize when a resource has 2 or more subresources.
This is because the hierarchy stack is not popped after visiting a resource.
Display the full name (namespace + resource name) of resources and subresource in HTML.
If the resource does not have namespace, only display resource name.

## [1.4.1]
- Allow directory command line arg for rest client builder generator.
The reason for this change is that network build is invoking the generator
for each file because there is no clean and safe way to pass a list of
file names in the java ant task.
After this change, network build can pass the directory as a single argument and
the generator will scan for restspec.json files in the directory.

## [1.4.0]
- Add parttioning support to d2.
Support range-based and hash-based partitioning.
Update scatter/gather API and add "send to all partitions" API in restli/extras.
- Allow directory command line arg for data template and avro schema translator.
The reason for this change is that network build is invoking the generator
for each file because there is no clean and safe way to pass a list of
file names in the java ant task.
After this change, network build can pass the directory as a single argument and
the generator will scan for pdsc files in the directory.
- Fix intermittent TestAbstractGenerator failures.

## [1.3.5]
- Fix issue with erroneously decoding query parameters, causing issues when a query parameter value contains "*".  This issue was introduced in 1.3.2

## [1.3.4]
- Revise the documentation generator for idl files in RestLiResourceModelExporter to handle overloaded methods
in resources.
- restli-docgen depends on Apache Velocity 1.5-LIN0 instead of previously 1.7. This change is necessary to
fix the trunk blocker ANE-6970.
- Add main function to RestLiResourceModelCompatibilityChecker so that it can be invoked in command line.
The usage pattern is:
RestLiResourceModelCompatibilityChecker [prevRestspecPath:currRestspecPath pairs]

## [1.3.3]
- Refactor tests and add AvroUtil class to data-avro to allow common models test
to not depend on test artifacts from pegasus.
- Add access to client factories from D2 Facilities interface.

## [1.3.2]
- Enhance validator API to enable AnyRecord validator to be implemented.
See AnyRecordValidator example and test cases in data.
- Add support for structured query parameters on CRUD methods.
- Remove c3po support
- Modify IDL generation to only emit shallow references to named schema types.

## [1.3.1]
- Allow "registration" of custom validators be more automatic (without having to explicitly
add to map and passing the map to DataSchemaAnnotationValidator.
 The value of this property must be a {@link DataMap}. Each entry in this {@link DataMap}
 declares a {@link Validator} that has to be created. The key of the entry determines
 {@link Validator} subclass to instantiate.
  <p>
The key to {@link Validator} class lookup algorithm first looks up the key-to-class
 map provided to the constructor to obtain the {@link Validator} subclass. If the key
 does not exist in the map, then look for a class whose name is equal to the
 provided key and is a subclass of {@link Validator}. If there is no match,
 then look for a class whose fully qualified name is derived from the provided by key
 by using "com.linkedin.data.schema.validator" as the Java package name and capitalizing
 the first character of the key and appending "Validator" to the key as the name
 of the class, and the class is a subclass of {@link Validator}.
- New on-line documentation generator for Rest.li server.
When passing an implementation of com.linkedin.restli.server.RestLiDocumentationRequestHandler to
RestLiServer through RestLiConfig, the server will respond to special URLs with documentation content
such as HTML page or JSON object.
The default implementation is from the new docgen project, which renders both HTML and JSON documentation.
It also provides an OPTIONS http method alias to the JSON documentation content.

## [1.3.0]
- Moved jetty dependents in r2, restli-server to new sub-projects r2-jetty, restli-server-standalone

## [1.2.5]
- To make sure custom Java class bound via typeref are initialized that their static initializers are
executed to register coercers, the code generator will generate a call Custom.initializeCustomClass
for each custom class referenced by a type.
For generality, this Custom.initializeCustomClass is called regardless of whether the coercer class
is also explicitly specified.
The way in which explicit coercer class initialization is performed has also changed to use
Class.forName(String className, boolean initializer, ClassLoader classLoader) with the initialize
flag set to true. This will cause the class to initialized without accessing the REGISTER_COERCER
static variable or trying to construct an instance of the coercer class. This allows the use of
static initializer block to initialize explicitly specified coercer class.
This change is not backwards compatible if the Coercer depends on constructing a new instance
to register the coercer.
- Add more test code for AvroOverrideFactory. Fixed a few bugs, i.e when schema/name and translator/class is not
specified, name is specified without namespace.
- Add support for custom data translator for translating from Avro to Pegasus data representation when there
is a custom Avro schema binding.
A custom Avro schema is provided via as follows:
<pre>
 {
   "type" : "record",
   "name" : "AnyRecord",
   "fields" : [ ... ],
   ...
   "avro" : {
     "schema" : {
       "type" : "record",
       "name" : "AnyRecord",
       "fields" : [
         {
           "name" : "type",
           "type" : "string"
         },
         {
           "name" : "value",
           "type" : "string"
         }
       ]
     },
     "translator" : {
       "class" : "com.foo.bar.AnyRecordTranslator"
     }
   }
 }
</pre>
If the "avro" property is present, it provides overrides that
override the default schema and data translation. The "schema"
property provides the override Avro schema. The "translator"
property provides the class for that will be used to translate
from the to and from Pegasus and Avro data representations.
Both of these properties are required if either is present.
If an override Avro schema is specified, the schema translation
inlines the value of the "schema" property into the translated
Avro schema.
If a translator class is specified, the data translator will
construct an instance of this class and invoke this instance
to translate the data between Pegasus and Avro representations.
- Allow query parameters to be custom types (SI-318)
Example customType annotation:
@QueryParam(value="o", typeref=CustomObjectRef.class) CustomObject o
where CustomObjectRef is an class generated off of a pdsc that specifies the underlying type of
CustomObject.
Users must also write and register a coercer that converts from the custom object to the
underlying type and back.

## [1.2.4]
- Add support for custom Avro schema binding to Pegasus to Avro Schema translator.
A custom Avro schema is provided via as follows:
<pre>
 {
   "type" : "record",
   "name" : "AnyRecord",
   "fields" : [ ... ],
   ...
   "avro" : {
     "schema" : {
     "type" : "record",
     "name" : "AnyRecord",
     "fields" : [
       {
         "name" : "type",
         "type" : "string"
       },
       {
         "name" : "value",
         "type" : "string"
       }
     ]
   }
 }
</pre>
If the "avro" property has a "schema" property, the value of this
property provides the translated Avro schema for this type. No further
translation or processing is performed. It simply inlines the value
of this property into the translated Avro schema.
- Support a custom ClassLoader in the RestliConfig to use when scanning/loading RestLi classes.
- Bump ParSeq to 0.4.4

## [1.2.3]
- Revert incompatible change to bytecode signatures of builder methods introduced in 1.1.7
- Fix bug of idl compatibility checker which did not check for new optional parameters and
  custom CRUD methods.
The report messages are revised and parameterized to be more readable.

## [1.2.2]
- Prototype custom class for records (not for production use yet.)
Enable auto-registration of coercer when it is not possible to use
a static initializer on the custom class to register. Here is the
comments from com.linkedin.data.template.Custom.
  /**
Initialize coercer class.
The preferred pattern is that custom class will register a coercer
through its static initializer. However, it is not always possible to
extend the custom class to add a static initializer.
In this situation, an optional coercer class can also be specified
with the custom class binding declaration in the schema.
<pre>
{
 "java" : {
   "class" : "java.net.URI",
   "coercerClass" : "com.linkedin.common.URICoercer"
 }
}
</pre>
When another type refers to this type, the generated class for referrer
class will invoke this method on the coercer class within the referrer
class's static initializer.
This method will reflect on the coercer class. It will attempt to read
the {@code REGISTER_COERCER} static field of the class if this field is declared
in the class. This static field may be private.
If such a field is not found or cannot be read, this method will attempt
to construct a new instance of the coercer class with the default constructor
of the coercer class. Either of these actions should cause the static initializer
of the coercer class to be invoked. The static initializer
is expected to register the coercer using {@link #registerCoercer}.
If both of these actions fail, then this method throws an {@link IllegalArgumentException}.
Note: Simply referencing to the coercer class using a static variable or
getting the class of the coercer class does not cause the static
initializer of the coercer class to be invoked. Hence, there is a need
actually access a field or invoke a method to cause the static initializer
to be invoked.
The preferred implementation pattern for coercer class is as follows:
<pre>
public class UriCoercer implements DirectCoercer<URI>
{
 static
 {
   Custom.registerCoercer(URI.class, new UriCoercer());
 }
 private static final Object REGISTER_COERCER = null;
 ...
}
</pre>
- Add more diagnostic details to idl compatibility report.

## [1.2.1]
- 2nd installment of imported util cleanup
Get rid of timespan dependency.
Fix indentation errors.
Remove used classes.
- 1st installment.  Remove unneeded code from imported util cases.
Fix problem with pegasus-common test directory is under src/main instead of src.
Remove LongStats. Make ImmutableLongStats the replacement.
Remove callsInLastSecond tracking (this is legacy that is not used and not needed in network.)
Remove unused methods in TrackerUtil.
- Eliminate pegasus dependency on util-core, in preparation for open sourcing.  This change
copies a number of classes from util-core related to Clock, CallTracker, and Stats.  These
classes have been placed in different packages, and are considered forked.  The only functional
change is that CallTracker no-longer ignores BusinessException when counting errors.

## [1.2.0]
- Experimental ParSeq client/server support

## [1.1.8]
- Fix bug where EnumDataSchema.index() always returned 0 when a symbol is found
- Add support for inspecting and modifying Data objects returned from DataIterators.
Data objects can be counted, accumulated, transformed and removed declaratively
based on value, schema properties or path in the Data object.  The intent is to
provide a core set of primitives that may be used to build decoration, filtering,
mapping, etc. for Data objects.
See: com.linkedin.data.it.{Builder, Remover, Counter, ValueAccumulator, Transformer}

## [1.1.7]
- Build "next" pagination link in collection result when start+count < total (iff total is provided by application code).
Moved spring integration from restli-contrib-spring to the pegasus-restli-spring-bridge sub-project in container.
General dependency injection functionality (JSR-330) has been moved to restli-server sub-project. Empty restli-contrib-spring
project is not removed from Pegasus, to preserve backwards compatibility with integration tests. All dependencies on
restli-contrib-spring should be removed.

## [1.1.6]
- Add RetryZooKeeper that handles ZooKeeper connection loss exception.

## [1.1.5]
- Added multiple tests for complex resource keys, fixed a number of bugs in client builders.
- Add getFacilities() to DynamicClient, in order to provide single point of entry for D2.

## [1.1.4]
- Usability fixes to RestLiResponseException - use GetMode.NULL for accessors, add hasXyz() methods.
- Clean up Exception handling in RestClient.  Previously, one could receive different exception
types for the same error in the Callback interface versus the two flavors of Future interface.
For example, if the server returned a valid error response, the caller of
ResponseFuture.getResponse() would receive a RestLiResponseException, but Callback.onError()
or the caller of Future.get() would receive a RestException.
Now, a RestLiResponseException is always generated when a valid error response is received from
the server.  Users of the Callback interface will receive a RestLiResponseException in
Callback.onError().  The ResponseFuture interface will throw a RestLiResponseException,
while the standard Future interface will throw a RestLiResponseException wrapped in an
ExecutionException.
- Remove dependency on ASM and Jersey package scanning logic.  Our ASM version is fairly
old, and presents a compatibility challenge, especially for open source usage.
This patch removes the relevant Jersey code and implements very simple package scanning
by loading the classes in the specified packages.  In theory this could waste more
resources by loading classes unnecessarily.  In practice, we expect the rest.li resource
packages to be fairly narrowly specified, so it should not be a significant issue.
- Improve exception message when there are Avro to Pegasus data translation errors.
This changes what DataTranslationException includes in getMessage().
- Add Data to Avro Schema translation mode called OptionalDefaultMode. This mode allows
the user to control how optional fields with default value is translated. The previous
behavior is to translate the default value. This new option allows all optional fields
to be translated to have default value of null (instead of the translated default value.)
This is appropriate for Avro because the default value is only used if it is present
in the reader schema and absent in the writer schema. By translating default value to
null, the absent field will have null as its value (which is a better indication of
absence and would translate more cleanly to Pegasus as an absent field). I think this
is more correct, then filling in with the translated default value for an absent field.
In addition, this also improves the Pegasus user experience. If the user did not specify
a default value for field, this is translated to a union with null and default value set to
null. Because of Avro limitation, it means that other uses of this record cannot initialize
this field to another default value. This should be allowed because specific use case
may indeed have valid default values for that specific use of the record.
Although the new mode has been added, the default is to be backwards compatible and
translate the default value (instead of forcing the translation to null.) We may change
this to be the default in the future. However, this may break backwards compatibility of
generated schema in the case that the Avro default value is significant (i.e. fields
absent in writer schema but present in reader schema.)

## [1.1.2]
- fix bug in degraderStrategyV2 where zookeeper updates would cause getTrackerClient to
return null for some calls because the existing state didn't have trackerclient information
and the threads weren't waiting for a good state.

## [1.1.1]
- Fix bug in which "include" and "fields" are not processed in the same order in
which they are defined.
As part of this fix, the parser needs to have knowledge of the location of
a data object within the input stream. JacksonCodec has been extended to
provide this location. Because this location is now available, various parsers
have been improved emit error messages that include the likely location
of the error.
Remove noisy TestCloudPerformance output.

## [1.1.0]
- An ability to define arbitrarily complex resource keys has been added. The resource
implementation has to extend ComplexKeyResource parameterized, in addition to the
value type, with key and params types, both extending RecordTemplate.  This feature is
currently considered experimental - future versions may be backwards incompatible.

## [1.0.5]
- Add -Dgenerator.generate.imported switch to PegasusDataTemplateGenerator to allow
the suppression of code generation for imported schemas.
- A ResourceConfigException will be thrown when an association resource has a single key.
The exception will be thrown during initialization.

## [1.0.4]
- ValidationOption and schema validation and fixup behavior has been refined.
The fixup boolean in ValidationOption has been replaced with CoercionMode.
This flag used to indicate whether primitive type coercion should occur and whether
the input Data objects can be modified.
There is minor incompatible change to RequiredMode.FIXUP_ABSENT_WITH_DEFAULT.
The old behavior is that the fixup flag must be set to allow
RequiredMode.FIXUP_ABSENT_WITH_DEFAULT to modify the input.
The new behavior is that RequiredMode.FIXUP_ABSENT_WITH_DEFAULT
alone allows validation to modify the input Data object.
RequiredMode and CoercionMode are independent of each other.
RequiredMode specifies how required fields should be handled.
CoercionMode specifies how coercion of primitive types should performed.
For backwards compatibility, setFixup(true) sets coercion mode to CoercionMode.NORMAL,
and isFixup returns true if coercion mode is not CoercionMode.OFF or required mode
is RequiredMode.FIXUP_ABSENT_WITH_DEFAULT.
- Change in Data.Traverse callbacks for startMap and startList to pass the DataMap
and DataList about to be traversed. This is a change to Data API. Code search
indicates there are no other users of Data.Traverse outside of the data module.
Add experimental PSON binary serialization format for more compact serialized
representation by remembering which map keys have already be seen and assigning
a numeric index to each new key seen. Subsequent occurrence of the same key
requires only serializing the numeric index of the key instead of the string
representation of the key.
The PsonCodec is currently in test directory because it is still experimental
for understanding data compression possible and processor overhead for looking
up keys before serialization and potential savings from binary representation.

## [1.0.3]
- Add support for filtering DataSchema to remove unwanted fields or custom properties.
- SI-297 Allow server application code to specify default count/start for PagingContext
- SI-274 Restli sends error responses via callback.onError rather than callback.onSuccess
- SI-346 Fix so that RoutingExceptions thrown prior to method invocation cause service code error 400.
- Backwards incompatible function name change in RestLiResourceModelCompatibilityChecker,
which requires rum version 0.13.51. Incompatibility information are changed to three categories:
UnableToChecks, Incompatibles and Compatibles. Use corresponding getters to access.

## [1.0.2]
- Fix JMX registering of tracker client.

## [1.0.1]
- Do not normalize underscores in user-defined names.

## [1.0.0]
- Final API cleanup:
  Move R2 callbacks into com.linkedin.common / pegasus-common
  Widen Callback.onError() signature to accept Throwable instead of Exception

## [0.22.3]
- Remove obsolete assembler code
- Initial work on complex resource keys
- Server-side support for query parameters on CRUD operations
Add support for custom query parameters on CRUD methods.

## [0.22.2]
- fix autometric/jmx support for DegraderLoadBalancer,State, and StrategyV2.
- Allow standard CRUD methods to be implemented without needing to override CollectionResource / AssociationResource (by annotating with @RestMethod.Get, @RestMethod.Create, etc.).  This is a step toward allowing custom query parameters on CRUD methods.

## [0.22.1]
- Report warning when idl file not found for compatibility check.

## [0.22.0]
- Add rest.li support for BatchUpdate, BatchPartialUpdate, BatchCreate, and BatchDelete
    Refactor builders to dispatch based on ResourceMethod, rather than response object type
    Improve type handling in response builders
    Initial version of routing, request handling, and response building for all batch methods
    Refactor projection to each response builder
    Unify handling of action responses and resource responses
    Refactored response builders & MethodInvoker switch cases to MethodAdapters
    Support for batch CUD operations in dynamic builder layer
    Code-generation for batch builders
    Adopt KV as default for new batch methods
These changes are intended to be backwards compatible, and should not require changes in application code

## [0.21.2]
- Separate jersey uri components from package scanning, and repackage jersey source under com.linkedin.jersey
- Fix D2 RewriteClient to respect percent-encoded query params.

## [0.21.1]
- No changes (accidental publish)

## [0.21.0]
- Add Java custom class binding support for primitive type.
You add a custom class binding by using a typeref with a "java" property.
The "java" property of the typeref declaration must be a map.
If this map has an "class" property, then the value of the "class"
property must be a string and this string provides the name of
the Java custom class for the typeref.
The generated code will now return and accept the Java custom class
as the return and argument type instead of the standard Java class
for the referenced type.
The custom class should meet the following requirements.
1. An instance of the custom class must be immutable.
2. A Coercer must be defined that can coerce the standard Java class
   of the type to the custom Java class of the type, in both the
   input and output directions. The coercer implements the
   DirectCoercer interface.
3. An instance of the coercer must be registered with the
   data template framework.
The following is an example illustrating Java custom class binding:
CustomPoint.pdsc:
{
  "type" : "typeref",
  "name" : "CustomPoint",
  "ref"  : "string",
  "java" : {
    "class" : "com.linkedin.data.template.TestCustom.CustomPoint"
  }
}
CustomPoint.java:
//
// The custom class
// It has to be immutable.
//
public class CustomPoint
{
  private int _x;
  private int _y;
  public CustomPoint(String s)
  {
    String parts[] = s.split(",");
    _x = Integer.parseInt(parts[0]);
    _y = Integer.parseInt(parts[1]);
  }
  public CustomPoint(int x, int y)
  {
    _x = x;
    _y = y;
  }
  public int getX()
  {
    return _x;
  }
  public int getY()
  {
    return _y;
  }
  public String toString()
  {
    return _x + "," + _y;
  }
  public boolean equals(Object o)
  {
    if (o == null)
      return false;
    if (this == o)
      return true;
    if (o.getClass() != getClass())
      return false;
    CustomPoint p = (CustomPoint) o;
    return (p._x == _x) && (p._y == _y);
  }
  //
  // The custom class's DirectCoercer.
  //
  public static class CustomPointCoercer implements DirectCoercer<CustomPoint>
  {
    @Override
    public Object coerceInput(CustomPoint object)
      throws ClassCastException
    {
      return object.toString();
    }
    @Override
    public CustomPoint coerceOutput(Object object)
      throws TemplateOutputCastException
    {
      if (object instanceof String == false)
      {
        throw new TemplateOutputCastException("Output " + object + " is not a string, and cannot be coerced to " + CustomPoint.class.getName());
      }
      return new CustomPoint((String) object);
    }
  }
  //
  // Automatically register Java custom class and its coercer.
  //
  static
  {
    Custom.registerCoercer(CustomPoint.class, new CustomPointCoercer());
  }
}

## [0.20.6]
- If a required field is missing, add Message for both the record and the missing field.
Modify test cases to test for paths reported as having failed.
- Throw NPE more consistently when attempting to add null elements to array templates or
adding null values to map templates. Previously, NPE will be thrown but not as
consistently and may not indicate that the input argument cannot be null.
Previously, attempting to add null to DataMap and DataList results in IllegalArgumentException.
Now, this will throw NPE.

## [0.20.5]
- Fix Avro schema converter such that default values for Avro unions can translated correctly.
Prior to this fix, the default value for an Avro union is encoded like similar using the JSON
serialization of the default value. The Avro specification specifies that the default value
for an union does not include the type discriminator, and the type is provided by the 1st
member of the union.
When a Data Schema is translated to an Avro Schema, if the union has a default value,
the default value's type must be the 1st member type of the union. Otherwise, an
IllegalArgumentException will be thrown. When a value of an union type is translated,
its translated value will not include the type discriminator.
When an Avro Schema is translated to a Data Schema, if the Avro union has a value,
the parser and validation function obtains the type of the value from the 1st member type
of union. The translated default value will include a type discriminator if translated type
remains a union after translation. (The translated type will not be a union if
the Avro union is the type for a field of a record and this union type has two members
and one of them is null as the field will become an optional field whose type is
the non-null member type of the union.)
Avro schema parser does not validate default values are valid, i.e. it does not validate the
default value for each field with the schema for the field. Pegasus schema parser will
perform this validation.
- Add support for BatchResponseKV in BatchGet, which provides correctly typed Map keys for getResults() and getError().
Convert clients to generate multi-valued params for batch requests, e.g., GET /resource?ids=1&ids=2.  Server-side support for this format has been in pegasus since 0.18.5, and has been deployed for all production use cases

## [0.20.4]
- Add include functionality to record, record can include fields from another record.
Include does not include or attempt to merge any other attributes from the included record,
including validate field (this is a TBD feature).
- Fix bug handling default query parameter values for enum types.
- Internal cleanup of JSON handling in RestLiResourceModelExporter/RestRequestBuilderGenerator

## [0.20.3]
- Re-enable action parameter validation, using fix-up mode to ensure that wire types are correctly coerced to schema types.
- Make fixup the default enabled for ValidationOptions.

## [0.20.2]
- Disable Action parameter validation since it fails when the schema declares a type of long but the value on the wire is less than MAX_INT.

## [0.20.1]
- Reduce unintentional memory retention by R2 timeout mechanism.

## [0.20.0]
- Same as 0.19.7. Bumped minor version due to backward incompatibility

## [0.19.7]
- Implemented correct initialization of InjectResourceFactory. This is an incompatible change for users of InjectResourceFactory and container/pegasus-restli-server-cmpt. To fix, you need to define an InjectResourceFactory bean in the your application's spring context and wire it in to rest.li server e.g.
<bean id="resourceFactory" class="com.linkedin.restli.server.spring.InjectResourceFactory" />

## [0.19.6]
- Fix server-side detection of rest.li compound key syntax to use best-match heuristic

## [0.19.5]
- Add support for boxified and unboxified setter for primitive types
- Add support for returning source files, target files, and modified files from data template
and rest client generators.

## [0.19.4]
- Cleanup build warnings
- Add pdsc definition for IDL (restspec.json) files in restli-common.
Add IDL compatibility checker and its test suites.
- SI-260 properly handle RestLiServiceException returned from action invocation
validate action parameters against schema to detect type mismatches
- fix quick deploy bug when no uris are registered in zk
- Inject dependencies into superclass fields when using @Inject/@Named

## [0.19.3]
- Make Continuation support configurable and off by default.
- Fix NullPointerException errors when referenced types in typeref, map, record fields are incomplete
or not resolvable.
- Fix bug causing Server Timeout when application code returns null object - should be 404

## [0.19.2]
- Remove deprecated "Application" object dependency injection through BaseResource.
- Remove rpc-demo-*
- Pass undecoded path segments to parsers to enable proper context-aware percent-decoding.

## [0.19.1]
- Fix bugs in code generation for @RestLiActions (actions set) resources
- Add fix bugs in Data Schema to Avro Schema translation
1. Fix exception thrown when translating default values of map types.
2. Fix exception thrown when translating typeref'ed optional union.
3. Translating data schema to avro schema should not mutate input data schema.

## [0.19.0]
- Enhanced exception support:
  Server-side application code may throw RestLiServiceException, which prompts the framework to send an ErrorResponse document to the client
  Client-side application code may catch RestLiResponseException, which provides access to the ErrorResponse contents.
Backwards-incompatible API changes:
  BusinessException has been replaced by RestLiServiceException
  ResponseFuture.getResponse() now throws RemoteInvocationException instead of RestException

## [0.18.7]
- Allow PagingContext to appear at any position in a Finder signature, or to be omitted entirely
DataTemplate: generate .fields() accessor methods for primitive branches of unions

## [0.18.6]
- Add SetMode to Record setters.
  /**
If the provided value is null, then do nothing.
<p>
If the provided value is null, then do nothing,
i.e. the value of the field is not changed.
The field may or may be present.
   */
  IGNORE_NULL,
  /**
If the provided value is null, then remove the field.
<p>
If the provided value is null, then remove the field.
This occurs regardless of whether the field is optional.
   */
  REMOVE_IF_NULL,
  /**
If the provided value is null and the field is
an optional field, then remove the field.
<p>
If the provided value is null and the field is
an optional field, then remove the field.
If the provided value is null and the field is
a mandatory field, then throw
{@link IllegalArgumentException}.
   */
  REMOVE_OPTIONAL_IF_NULL,
  /**
The provided value cannot be null.
<p>
If the provided value is null, then throw {@link NullPointerException}.
   */
  DISALLOW_NULL

## [0.18.5]
- (1) added support for array parameters in finders.
    url notation is ...?foo=foo1&foo=foo2&foo=foo3&...
(2) ids parameter notation changed from ids=1,2,3 to ids=1&ids=2&ids3 for better compatibility
    with standard on query part of urls, client libraries, and (1).
(3) string representation of compound keys changed back to foo=foo1&bar=bar1
    (from foo:foo1;bar:bar1) for better compatibility with (1) and (2).
(4) batch request builder will use legacy comma encoding
The new server will support both new and old URL formats.
Existing batch request client builders will emit old URL format.
The URLs emitted by batch request client builders generated from this release will use the old format.
The upgrade sequence will be
    - first update all servers to this version,
    - then release new batch client and update all clients.
- Fix bug due to not recursively translating default values when translating
schemas between Avro and Pegasus.
Fix bug due to different handling of member keys in union between Avro
and Pegasus when translating schemas.

## [0.18.4]
- Add rest.li server-side support for application-defined response headers

## [0.18.3]
- Bump RUM version to 0.13.18 to fix eclipse compatibility problem

## [0.18.2]
- Change default namespace for restli-server-examples to be backwards compatible.
Change check for TimeoutException in netty client shutdown test
Use 0.13.12 of rum plugin

## [0.18.1]
- Add support in D2 for direct local routing, as well as fix handling of
the root path in ZooKeeperStore.

## [0.18.0]
- Add support of bulk requests for association resources
- Change build to use pegasus v2 rum plugin
Require use of rum 0.13.11
/**
Pegasus code generation plugin.
<p>
Performs the following functions:
<p>
<b>Generate data model and data template jars for each source set.</b>
<p>
Generates data template source (.java) files from data schema (.pdsc) files,
compiles the data template source (.java) files into class (.class) files,
creates a data model jar file and a data template jar file.
The data model jar file contains the source data schema (.pdsc) files.
The data template jar file contains both the source data schema (.pdsc) files
and the generated data template class (.class) files.
<p>
In the data template generation phase, the plugin creates a new target source set
for the generated files. The new target source set's name is the input source set name's
suffixed with "GeneratedDataTemplate", e.g. "mainGeneratedDataTemplate".
The plugin invokes PegasusDataTemplateGenerator to generate data template source (.java) files
for all data schema (.pdsc) files present in the input source set's pegasus
directory, e.g. "src/main/pegasus". The generated data template source (.java) files
will be in the new target source set's java source directory, e.g.
"src/mainGeneratedDataTemplate/java". The dataTemplateGenerator configuration
specifies the classpath for loading PegasusDataTemplateGenerator. In addition to
the data schema (.pdsc) files in the pegasus directory, the dataModel configuration
specifies resolver path for the PegasusDataTemplateGenerator. The resolver path
provides the data schemas and previously generated data template classes that
may be referenced by the input source set's data schemas. In most cases, the dataModel
configuration should contain data template jars.
<p>
The next phase is the data template compilation phase, the plugin compiles the generated
data template source (.java) files into class files. The dataTemplateCompile configuration
specifies the pegasus jars needed to compile these classes. The compileClasspath of the
target source set is a composite of the dataModel configuration which includes the data template
classes that were previously generated and included in the dependent data template jars,
and the dataTemplateCompile configuration.
This configuration should specify a dependency on the Pegasus data jar.
<p>
The following phase is creating the the data model jar and the data template jar.
This plugin creates the data model jar that includes the contents of the
input source set's pegasus directory, and sets the jar file's classification to
"data-model". Hence, the resulting jar file's name should end with "-data-model.jar".
It adds the data model jar as an artifact to the dataModel configuration.
This jar file should only contain data schema (.pdsc) files.
<p>
This plugin also create the data template jar that includes the contents of the input
source set's pegasus directory and the java class output directory of the
target source set. It sets the jar file's classification to "data-template".
Hence, the resulting jar file's name should end with "-data-template.jar".
It adds the data template jar file as an artifact to the dataTemplate configuration.
This jar file contains both data schema (.pdsc) files and generated data template
class (.class) files.
<p>
This plugin will ensure that data template source files are generated before
compiling the input source set and before the idea and eclipse tasks. It
also adds the generated classes to the compileClasspath of the input source set.
<p>
The configurations that apply to generating the data model and data template jars
are as follow:
<ul>
<li>
 The dataTemplateGenerator configuration specifies the classpath for
 PegasusDataTemplateGenerator. In most cases, it should be the Pegasus generator jar.
</li>
<li>
 The dataTemplateCompile configuration specifies the classpath for compiling
 the generated data template source (.java) files. In most cases,
 it should be the Pegasus data jar.
 (The default compile configuration is not used for compiling data templates because
 it is not desirable to include non data template dependencies in the data template jar.)
 The configuration should not directly include data template jars. Data template jars
 should be included in the dataModel configuration.
</li>
<li>
 The dataModel configuration provides the value of the "generator.resolver.path"
 system property that is passed to PegasusDataTemplateGenerator. In most cases,
 this configuration should contain only data template jars. The data template jars
 contain both data schema (.pdsc) files and generated data template (.class) files.
 PegasusDataTemplateGenerator will not generate data template (.java) files for
 classes that can be found in the resolver path. This avoids redundant generation
 of the same classes, and inclusion of these classes in multiple jars.
 The dataModel configuration is also used to publish the data model jar which
 contains only data schema (.pdsc) files.
</li>
<li>
 The testDataModel configuration is similar to the dataModel configuration
 except it is used when generating data templates from test source sets.
 It extends from the dataModel configuration. It is also used to publish
 the data model jar from test source sets.
</li>
<li>
 The dataTemplate configuration is used to publish the data template
 jar which contains both data schema (.pdsc) files and the data template class
 (.class) files generated from these data schema (.pdsc) files.
</li>
<li>
 The testDataTemplate configuration is similar to the dataTemplate configuration
 except it is used when oublishing the data template jar files generated from
 test source sets.
</li>
</ul>
<p>
<b>Generate rest model and rest client jars for each source set.</b>
<p>
Generates the idl (.restspec.json) files from the input source set's
output class files, generates rest client source (.java) files from
the idl, compiles the rest client source (.java) files to
rest client class (.class) files, and creates a rest model jar file
and a rest-client jar file.
The rest model jar file contains the generated idl (.restspec.json) files.
The rest client jar file contains both the generated idl (.restspec.json)
files and the generated rest client class (.class) files.
<p>
In the idl generation phase, the plugin creates a new target source set
for the generated files. The new target source set's name is the input source set name's
suffixed with "GeneratedRest", e.g. "mainGeneratedRest".
The plugin invokes RestLiResourceModelExporter to generate idl (.restspec.json) files
for each IdlItem in the input source set's pegasus IdlOptions.
The generated idl files will be in target source set's idl directory,
e.g."src/mainGeneratedRest/idl".
<p>
For example, the following adds an IdlItem to the source set's pegasus IdlOptions.
<pre>
 pegasus.main.idlOptions.addIdlItem("groups", ['com.linkedin.restli.examples.groups.server'])
</pre>
<p>
The next phase is to generate the rest client source (.java) files from the
generated idl (.restspec.json) files using RestRequestBuilderGenerator.
The generated rest client source (.java) files will be in the new target source set's
java source directory, e.g. "src/mainGeneratedRest/java". The restClientGenerator
configuration specifies the classpath for loading RestLiResourceModelExporter
and for loading RestRequestBuilderGenerator.
<p>
RestRequestBuilderGenerator requires access to the data schemas referenced
by the idl. The dataModel configuration specifies the resolver path needed
by RestRequestBuilderGenerator to access the data schemas referenced by
the idl that is not in the source set's pegasus directory.
This plugin automatically includes the data schema (.pdsc) files in the
source set's pegasus directory in the resolver path.
In most cases, the dataModel configuration should contain data template jars.
The data template jars contains both data schema (.pdsc) files and generated
data template class (.class) files. By specifying data template jars instead
of data model jars, redundant generation of data template classes is avoided
as classes that can be found in the resolver path are not generated.
<p>
The next phase is the rest client compilation phase, the plugin compiles the generated
rest client source (.java) files into class files. The restClientCompile configuration
specifies the pegasus jars needed to compile these classes. The compile classpath
is a composite of the dataModel configuration which includes the data template
classes that were previously generated and included in the dependent data template jars,
and the restClientCompile configuration.
This configuration should specify a dependency on the Pegasus restli-client jar.
<p>
The following phase is creating the the rest model jar and the rest client jar.
This plugin creates the rest model jar that includes the
generated idl (.restspec.json) files, and sets the jar file's classification to
"rest-model". Hence, the resulting jar file's name should end with "-rest-model.jar".
It adds the rest model jar as an artifact to the restModel configuration.
This jar file should only contain idl (.restspec.json) files.
<p>
This plugin also create the rest client jar that includes the
generated idl (.restspec.json) files and the java class output directory of the
target source set. It sets the jar file's classification to "rest-client".
Hence, the resulting jar file's name should end with "-rest-client.jar".
It adds the rest client jar file as an artifact to the restClient configuration.
This jar file contains both idl (.restspec.json) files and generated rest client
class (.class) files.
<p>
This plugin will ensure that generating idl will occur after compiling the
input source set. It will also ensure that idea and eclipse tasks runs after
rest client source (.java) files are generated.
<p>
The configurations that apply to generating the rest model and rest client jars
are as follow:
<ul>
<li>
 The restClientGenerator configuration specifies the classpath for
 RestLiResourceModelExporter and RestRequestBuilderGenerator.
 In most cases, it should be the Pegasus restli-tools jar.
</li>
<li>
 The restClientCompile configuration specifies the classpath for compiling
 the generated rest client source (.java) files. In most cases,
 it should be the Pegasus restli-client jar.
 (The default compile configuration is not used for compiling rest client because
 it is not desirable to include non rest client dependencies, such as
 the rest server implementation classes, in the data template jar.)
 The configuration should not directly include data template jars. Data template jars
 should be included in the dataModel configuration.
</li>
<li>
 The dataModel configuration provides the value of the "generator.resolver.path"
 system property that is passed to RestRequestBuilderGenerator.
 This configuration should contain only data template jars. The data template jars
 contain both data schema (.pdsc) files and generated data template (.class) files.
 The RestRequestBuilderGenerator will only generate rest client classes.
 The dataModel configuration is also included in the compile classpath for the
 generated rest client source files. The dataModel configuration does not
 include generated data template classes, then the Java compiler may not able to
 find the data template classes referenced by the generated rest client.
</li>
<li>
 The testDataModel configuration is similar to the dataModel configuration
 except it is used when generating rest client source files from
 test source sets.
</li>
<li>
 The restModel configuration is used to publish the rest model jar
 which contains generated idl (.restspec.json) files.
</li>
<li>
 The testRestModel configuration is similar to the restModel configuration
 except it is used to publish rest model jar files generated from
 test source sets.
</li>
<li>
 The restClient configuration is used to publish the rest client jar
 which contains both generated idl (.restspec.json) files and
 the rest client class (.class) files generated from from these
 idl (.restspec.json) files.
</li>
<li>
 The testRestClient configuration is similar to the restClient configuration
 except it is used to publish rest client jar files generated from
 test source sets.
</li>
</ul>
<p>
This plugin considers test source sets whose names begin with 'test' to be
test source sets.
*/

## [0.17.6]
- Add option to disable record template generation from RestRequestBuilderGenerator, to support ant codegen integration in network

## [0.17.5]
- Refactor SimpleLoadBalancerState to use one TransportClient per
cluster and eliminate LazyClient.
- Add "namespace" parameter to @RestLi* resource annotations, allowing the resource author to
specify the default namespace to be used for the IDL and client builders.

## [0.17.4]
- Fix key usage and delete handling in groups example in rest.li
- Fix inconsistent parsing of paginationa parameters in rest.li.
- Add another workaround for Jackson http://jira.codehaus.org/browse/JACKSON-491
- Fix bugs in translation from Pegasus DataMap to Avro GenericRecord translations.
Add test cases for round-tripping through binary Avro serialization.
Map keys from Avro may be String or Utf8
Enum symbol is mapped to GenericData.EnumSymbol instead of String.
ByteBuffer not rewound after copy to ByteString.

## [0.17.3]
- Fix bug in DataTemplate wrapping of typeref'ed types

## [0.17.2]
- Code generator changes to avoid generating the same class multiple
times. If a class already exist in generator.resolver.path, then don't
generate the class again.

## [0.17.1]
- Generate typesafe pathkey-binding methods for actions in subresources
- Add AvroSchemaGenerator to output avsc files from pdsc files.
Avro avsc requires the type to be record. If a pdsc file or schema
is not a record, no avsc file will be emitted.
Refactor generator to move common schema resolution based on path,
testing for stale output files, ... can be reused by different
generators.
Simplify by consolidating DataSchemaContext and DataSchemaResolver,
eliminate duplicate tracking of names to schemas.

## [0.17.0]
- Revamp rest.li client library
   One Request/RequestBuilder pair per rest.li method.
   Generate builders for finder and action methods
   Generate xyzRequestBuilders "builder factory" classes for resources
   Generate builders for all resource methods, to allow type-safe specification of parent resource keys

## [0.16.5]
- Fix issue with field projections on CollectionResult (SI-198)

## [0.16.4]
- Update util to 4.0.1.
Merge DegraderImpl changes from container to Pegasus.

## [0.16.3]
- Add configurable maxResponseSize to NttpNettyClient/HttpClientFactory

## [0.16.2]
- Workaround fix for JACKSON-230 (http://jira.codehaus.org/browse/JACKSON-230)
- Add auto-detection of whether JACKSON-230 bug is present.
Upgrade Jackson library to 1.4.2.
Auto-detection added to handle Jackson library version override in consumers.

## [0.16.1]
- Merge 0.15.2 through 0.15.4
0.16
Refactor the relationship between HttpNettyClient and
HttpClientFactory.  HttpClientFactory now owns the thread pool
resources, and all clients created by the factory will share the same
underlying executors.  This is an incompatible change, because the
HttpClientFactory must now be shut down.
Add support for an overall request timeout to HttpNettyClient.  If the
request does not complete for any reason within the timeout, the
callback's onError will be invoked.
Add support for graceful shutdown to HttpNettyClient and
HttpClientFactory.  The factories and the clients can be shutdown in
any relative order, and outstanding requests will be allowed to
complete within the shutdown timeout.
- Add SchemaTranslator class to translate from Avro Schema to Pegasus Data Schema
and vice-versa.

## [0.15.4]
- Add a dependency from data.jar to the new empty cow.jar

## [0.15.3]
- Add empty cow.jar to facilitate renaming cow.jar to data.jar

## [0.15.2]
- Internal changes to replace Stack with ArrayList or ArrayDeque.

## [0.15.1]
- Main API change is removal/decoupling of validation from DataSchema.
DataSchema no longer has validate method. The replacement is
ValidateDataAgainstSchema.validate(...).
Reduce memory allocation for DataElement for each object visited.
Will reuse same DataElement for each member of a container.
As part of this change, it is no longer possible to get a
standard Iterator from a builder. The alternative is to use the
traverse method that takes a callback for each object iterated.
Add support for different pre-order and post-order traversal
to ObjectIterator. This allows ObjectIterator to be used for
data to schema validation. This unification allows single pass
data to schema validation as well as calling Validator after
fixup and schema validation.
Enhance DataSchemaAnnotationValidator to not throw exception
on construction. Allow validator to be used if only some
validators are constructed. Use common Message classes for emitting
initialization messages.
Refactor code to allow both iterative and recursive validation.
Add more test cases.
- Add support to taking DataElement as starting point for
iterating through Data objects and for validation. This
has been requested by Identity superblock where the patch
is applied to position (using position as starting point), but
the root object is a profile. The validation should start
where the patch is applied, but the validator plugin wants
access to the entire entity, i.e. the profile entity.
Add tests and bug fix unnecessary additional calls to validators
from ValidateDataAgainstSchema when typerefs are in use. The
bug was that the downstream validator will be called once per
typeref in the typeref chain. The correct and fixed behavior
is that the downstream validator will be called once per
data object (not once per schema typeref'ed).
0.15
Add pluggable validator to Data Schemas
1. Change behavior of ObjectIterator to include returning the
   input value.
2. See com.linkedin.data.validator package and TestValidator
   class for how to use validators.
3. This is still a prototype feature.
Output Avro-compliant equivalent JSON from Pegasus schema
Add translation between Pegasus DataMap and Avro GenericRecord
1. Also include refactoring of DataSchema to JSON encoding to
   move Avro specific code out of cow module into cow-avro module.
Rest.li support for full and partial updates
  Full update (overwrite) is transported as an HTTP PUT to the entity URI,
with a payload containing the JSON serialized RecordTemplate of the entity
schema.
  Partial update (patch) is transported as an HTTP POST to the entity URI,
with a payload containing a JSON serialized PatchRequest
The internal structure of a PatchRequest is documented here:
https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/Partial+Update
PatchRequests can be generated on the client side by "diff'ing" two
RecordTemplate objects using PatchGenerator in com.linkedin.restli.client.utility.
Patch Generation relies on the facilities from the data-transform pegasus
component, in the com.linkedin.data.transform package.
PatchRequests can be applied on the server side by providing a pre-image
RecordTemplate object and a PatchRequest to PatchApplier in
com.linkedin.restli.server.util.  Patch application uses the DataMapProcessor
from the pegasus data-transform component.
Full and Partial update are provided as overloaded update() methods in
CollectionResource/AssociationResource on the server-side and as overloaded
buildUpdate() methods in EntityRequestBuilder on the client-side.
PARTIAL_UPDATE is defined as a new ResourceMethod, and listed as appropriate
in the IDL "supports" clause of the resource.
Support for deep (nested) projections has been implemented:
  Server-side, the rest.li framework understands both the old "field1,field2,field3"
syntax and the new PAL-style "field1,field2:(nestedfield)" syntax.  Projections
are applied automatically by the framework, using pegasus data-transform.
ResourceContext provides access to the projections as either a Set<String> or a MaskTree.
  Client-side, the generated RecordTemplate classes have been modified to
provide fields as a nested class accessed through the .fields() static method.
Each field can be accessed as a Path object through a fieldName() method, which
provides full static typing.  Fields are provided as methods rather than member
variables to avoid initialization cycles when a RecordTemplate contains a field of
the same type.
Deep projection support is currently *disabled* on the client side, to avoid ordering
issues with deployment.  Once all services have been deployed to production with the
new pegasus version, we will enable deep projections on the client side.
More background on projection support is available here:
https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/Projections
Compatibility:
WIRE: This change is wire INCOMPATIBLE for existing users of update.  There are
no known uses of update in production.
CLIENT: This change is library INCOMPATIBLE for users of update, projection fields,
or code that relies on the types of framework ...Builder classes (a type parameter has
been removed).  Client code will need to be upgraded.
SERVER: This change is library INCOMPATIBLE for users of update.  Server code will
need to be upgraded to use PatchRequests and PatchApplier.
Validation code refactoring.
1. Move validation code out of DataSchema classes, enable single pass validation of schema and calling
   validator plugins.
2. Move schema validation code into validation package.
3. Move validator plugins into validator package.
4. Provide field specific diagnostic data from schema validator.
Rename cow and cow-avro module to data and data-avro module.
Refactor Cow classes and provide implementation that have the checker
functionality but without copy-on-write, known as CheckedMap and CheckedList
classes.
- Add support for "validate" annotation on fields.
Prefix DataElement accessor methods with "get".
- Add support for filtering calls to a Validator based on what has been
set as specified by the patch operations map. This functionality is
implemented by the PatchFilterValidator class.
Refactored ValidationMessage* classes into Message* classes so that
patch operations can re-use these classes for generating patch messages.

## [0.14.7]
- Fix a bug in D2 where the PropertyStore information was not correctly
  persisted to disk, preventing the load balancer from operating
  correctly if connectivity to ZooKeeper was interrupted.

## [0.14.6]
-  Add support for typeref to enable aliasing primitive types.
   Typeref works for any type (not just primitive).
   There is no support yet for binding different Java classes
   to the typeref'ed types.
   If a typeref is member of union, then the accessor method
   names for accessing the member are derived from the typeref
   name.
-  Serialization protocol and format does not change. The serialized
   representation of the member key is always the actual type, i.e.
   the type reached by following chain of typerefs.
-  Rest.li @Action and @ActionParam now has optional 'typeref'
   attribute that specifies the typeref that should be emitted to
   the IDL instead of the type inferred from the Java type.
   The provided typeref must be compatible with the Java type.
   KNOWN ISSUE - Unions cannot be used as return or parameter for
   actions.

## [0.14.5]
-  Provide human readable locations when Jackson parse errors occurs.

## [0.14.4]
- Data Schema Resolver
  1. RUM pegasus plugin will not have to unjar before passing data schema jars to
     pegasus.  See seperate review for changes to plugin.
  2. Remove location from in-memory representation of schemas. Location is only used
     for generating Java bindings and checking for freshness of generated files.
     It is not needed to in-memory representation. Storing them in in-memory bindings
     may cause file "leaks" as they refer to jar files and files.
  3. Simply by removing FIELDS_ONLY record type.

## [0.14.3]
- Minor changes to comments in rpc-demo-client.
- Fix pdpr and avsc references in tools/rest-doc-generator/docgen.py

## [0.14.2]
- Not released.

## [0.14.1]

[Unreleased]: https://github.com/linkedin/rest.li/compare/v29.48.4...master
[29.48.4]: https://github.com/linkedin/rest.li/compare/v29.48.3...v29.48.4
[29.48.3]: https://github.com/linkedin/rest.li/compare/v29.48.2...v29.48.3
[29.48.2]: https://github.com/linkedin/rest.li/compare/v29.48.1...v29.48.2
[29.48.1]: https://github.com/linkedin/rest.li/compare/v29.48.0...v29.48.1
[29.48.0]: https://github.com/linkedin/rest.li/compare/v29.47.0...v29.48.0
[29.47.0]: https://github.com/linkedin/rest.li/compare/v29.46.9...v29.47.0
[29.46.9]: https://github.com/linkedin/rest.li/compare/v29.46.8...v29.46.9
[29.46.8]: https://github.com/linkedin/rest.li/compare/v29.46.7...v29.46.8
[29.46.7]: https://github.com/linkedin/rest.li/compare/v29.46.6...v29.46.7
[29.46.6]: https://github.com/linkedin/rest.li/compare/v29.46.5...v29.46.6
[29.46.5]: https://github.com/linkedin/rest.li/compare/v29.45.1...v29.45.2
[29.46.4]: https://github.com/linkedin/rest.li/compare/v29.46.3...v29.46.4
[29.46.3]: https://github.com/linkedin/rest.li/compare/v29.46.2...v29.46.3
[29.46.2]: https://github.com/linkedin/rest.li/compare/v29.46.1...v29.46.2
[29.46.1]: https://github.com/linkedin/rest.li/compare/v29.46.0...v29.46.1
[29.46.0]: https://github.com/linkedin/rest.li/compare/v29.45.1...v29.46.0
[29.45.1]: https://github.com/linkedin/rest.li/compare/v29.45.0...v29.45.1
[29.45.0]: https://github.com/linkedin/rest.li/compare/v30.0.0...v29.45.0
[30.0.0]: https://github.com/linkedin/rest.li/compare/v29.44.0...v30.0.0
[29.44.0]: https://github.com/linkedin/rest.li/compare/v29.43.11...v29.44.0
[29.43.11]: https://github.com/linkedin/rest.li/compare/v29.43.10...v29.43.11
[29.43.10]: https://github.com/linkedin/rest.li/compare/v29.43.9...v29.43.10
[29.43.9]: https://github.com/linkedin/rest.li/compare/v29.43.8...v29.43.9
[29.43.8]: https://github.com/linkedin/rest.li/compare/v29.43.7...v29.43.8
[29.43.7]: https://github.com/linkedin/rest.li/compare/v29.43.6...v29.43.7
[29.43.6]: https://github.com/linkedin/rest.li/compare/v29.43.5...v29.43.6
[29.43.5]: https://github.com/linkedin/rest.li/compare/v29.43.4...v29.43.5
[29.43.4]: https://github.com/linkedin/rest.li/compare/v29.43.3...v29.43.4
[29.43.3]: https://github.com/linkedin/rest.li/compare/v29.43.2...v29.43.3
[29.43.2]: https://github.com/linkedin/rest.li/compare/v29.43.1...v29.43.2
[29.43.1]: https://github.com/linkedin/rest.li/compare/v29.43.0...v29.43.1
[29.43.0]: https://github.com/linkedin/rest.li/compare/v29.42.4...v29.43.0
[29.42.4]: https://github.com/linkedin/rest.li/compare/v29.42.3...v29.42.4
[29.42.3]: https://github.com/linkedin/rest.li/compare/v29.42.2...v29.42.3
[29.42.2]: https://github.com/linkedin/rest.li/compare/v29.42.1...v29.42.2
[29.42.1]: https://github.com/linkedin/rest.li/compare/v29.42.0...v29.42.1
[29.42.0]: https://github.com/linkedin/rest.li/compare/v29.41.12...v29.42.0
[29.41.12]: https://github.com/linkedin/rest.li/compare/v29.41.11...v29.41.12
[29.41.11]: https://github.com/linkedin/rest.li/compare/v29.41.10...v29.41.11
[29.41.10]: https://github.com/linkedin/rest.li/compare/v29.41.9...v29.41.10
[29.41.9]: https://github.com/linkedin/rest.li/compare/v29.41.8...v29.41.9
[29.41.8]: https://github.com/linkedin/rest.li/compare/v29.41.7...v29.41.8
[29.41.7]: https://github.com/linkedin/rest.li/compare/v29.41.6...v29.41.7
[29.41.6]: https://github.com/linkedin/rest.li/compare/v29.41.5...v29.41.6
[29.41.5]: https://github.com/linkedin/rest.li/compare/v29.41.4...v29.41.5
[29.41.4]: https://github.com/linkedin/rest.li/compare/v29.41.3...v29.41.4
[29.41.3]: https://github.com/linkedin/rest.li/compare/v29.41.2...v29.41.3
[29.41.2]: https://github.com/linkedin/rest.li/compare/v29.41.1...v29.41.2
[29.41.1]: https://github.com/linkedin/rest.li/compare/v29.41.0...v29.41.1
[29.41.0]: https://github.com/linkedin/rest.li/compare/v29.40.15...v29.41.0
[29.40.15]: https://github.com/linkedin/rest.li/compare/v29.40.14...v29.40.15
[29.40.14]: https://github.com/linkedin/rest.li/compare/v29.40.13...v29.40.14
[29.40.13]: https://github.com/linkedin/rest.li/compare/v29.40.12...v29.40.13
[29.40.12]: https://github.com/linkedin/rest.li/compare/v29.40.11...v29.40.12
[29.40.11]: https://github.com/linkedin/rest.li/compare/v29.40.10...v29.40.11
[29.40.10]: https://github.com/linkedin/rest.li/compare/v29.40.9...v29.40.10
[29.40.9]: https://github.com/linkedin/rest.li/compare/v29.40.8...v29.40.9
[29.40.8]: https://github.com/linkedin/rest.li/compare/v29.40.7...v29.40.8
[29.40.7]: https://github.com/linkedin/rest.li/compare/v29.40.6...v29.40.7
[29.40.6]: https://github.com/linkedin/rest.li/compare/v29.40.5...v29.40.6
[29.40.5]: https://github.com/linkedin/rest.li/compare/v29.40.4...v29.40.5
[29.40.4]: https://github.com/linkedin/rest.li/compare/v29.40.3...v29.40.4
[29.40.3]: https://github.com/linkedin/rest.li/compare/v29.40.2...v29.40.3
[29.40.2]: https://github.com/linkedin/rest.li/compare/v29.40.1...v29.40.2
[29.40.1]: https://github.com/linkedin/rest.li/compare/v29.40.0...v29.40.1
[29.40.0]: https://github.com/linkedin/rest.li/compare/v29.39.6...v29.40.0
[29.39.6]: https://github.com/linkedin/rest.li/compare/v29.39.5...v29.39.6
[29.39.5]: https://github.com/linkedin/rest.li/compare/v29.39.4...v29.39.5
[29.39.4]: https://github.com/linkedin/rest.li/compare/v29.39.3...v29.39.4
[29.39.3]: https://github.com/linkedin/rest.li/compare/v29.39.2...v29.39.3
[29.39.2]: https://github.com/linkedin/rest.li/compare/v29.39.1...v29.39.2
[29.39.1]: https://github.com/linkedin/rest.li/compare/v29.39.0...v29.39.1
[29.39.0]: https://github.com/linkedin/rest.li/compare/v29.38.6...v29.39.0
[29.38.6]: https://github.com/linkedin/rest.li/compare/v29.38.5...v29.38.6
[29.38.5]: https://github.com/linkedin/rest.li/compare/v29.38.4...v29.38.5
[29.38.4]: https://github.com/linkedin/rest.li/compare/v29.38.3...v29.38.4
[29.38.3]: https://github.com/linkedin/rest.li/compare/v29.38.2...v29.38.3
[29.38.2]: https://github.com/linkedin/rest.li/compare/v29.38.1-rc.1...v29.38.2
[29.38.1-rc.1]: https://github.com/linkedin/rest.li/compare/v29.38.0...v29.38.1-rc.1
[29.38.0]: https://github.com/linkedin/rest.li/compare/v29.37.19...v29.38.0
[29.37.19]: https://github.com/linkedin/rest.li/compare/v29.37.18...v29.37.19
[29.37.18]: https://github.com/linkedin/rest.li/compare/v29.37.17...v29.37.18
[29.37.17]: https://github.com/linkedin/rest.li/compare/v29.37.16...v29.37.17
[29.37.16]: https://github.com/linkedin/rest.li/compare/v29.37.15...v29.37.16
[29.37.15]: https://github.com/linkedin/rest.li/compare/v29.37.14...v29.37.15
[29.37.14]: https://github.com/linkedin/rest.li/compare/v29.37.13...v29.37.14
[29.37.13]: https://github.com/linkedin/rest.li/compare/v29.37.12...v29.37.13
[29.37.12]: https://github.com/linkedin/rest.li/compare/v29.37.11...v29.37.12
[29.37.11]: https://github.com/linkedin/rest.li/compare/v29.37.10...v29.37.11
[29.37.10]: https://github.com/linkedin/rest.li/compare/v29.37.9...v29.37.10
[29.37.9]: https://github.com/linkedin/rest.li/compare/v29.37.8...v29.37.9
[29.37.8]: https://github.com/linkedin/rest.li/compare/v29.37.7...v29.37.8
[29.37.7]: https://github.com/linkedin/rest.li/compare/v29.37.6...v29.37.7
[29.37.6]: https://github.com/linkedin/rest.li/compare/v29.37.5...v29.37.6
[29.37.5]: https://github.com/linkedin/rest.li/compare/v29.37.4...v29.37.5
[29.37.4]: https://github.com/linkedin/rest.li/compare/v29.37.3...v29.37.4
[29.37.3]: https://github.com/linkedin/rest.li/compare/v29.37.2...v29.37.3
[29.37.2]: https://github.com/linkedin/rest.li/compare/v29.37.1...v29.37.2
[29.37.1]: https://github.com/linkedin/rest.li/compare/v29.37.0...v29.37.1
[29.37.0]: https://github.com/linkedin/rest.li/compare/v29.36.1...v29.37.0
[29.36.1]: https://github.com/linkedin/rest.li/compare/v29.36.0...v29.36.1
[29.36.0]: https://github.com/linkedin/rest.li/compare/v29.35.0...v29.36.0
[29.35.0]: https://github.com/linkedin/rest.li/compare/v29.34.3...v29.35.0
[29.34.3]: https://github.com/linkedin/rest.li/compare/v29.34.2...v29.34.3
[29.34.2]: https://github.com/linkedin/rest.li/compare/v29.34.1...v29.34.2
[29.34.1]: https://github.com/linkedin/rest.li/compare/v29.34.0...v29.34.1
[29.34.0]: https://github.com/linkedin/rest.li/compare/v29.33.7...v29.34.0
[29.33.9]: https://github.com/linkedin/rest.li/compare/v29.33.8...v29.33.9
[29.33.8]: https://github.com/linkedin/rest.li/compare/v29.33.7...v29.33.8
[29.33.7]: https://github.com/linkedin/rest.li/compare/v29.33.6...v29.33.7
[29.33.6]: https://github.com/linkedin/rest.li/compare/v29.33.5...v29.33.6
[29.33.5]: https://github.com/linkedin/rest.li/compare/v29.33.4...v29.33.5
[29.33.4]: https://github.com/linkedin/rest.li/compare/v29.33.3...v29.33.4
[29.33.3]: https://github.com/linkedin/rest.li/compare/v29.33.2...v29.33.3
[29.33.2]: https://github.com/linkedin/rest.li/compare/v29.33.1...v29.33.2
[29.33.1]: https://github.com/linkedin/rest.li/compare/v29.33.0...v29.33.1
[29.33.0]: https://github.com/linkedin/rest.li/compare/v29.32.5...v29.33.0
[29.32.5]: https://github.com/linkedin/rest.li/compare/v29.32.4...v29.32.5
[29.32.4]: https://github.com/linkedin/rest.li/compare/v29.32.3...v29.32.4
[29.32.3]: https://github.com/linkedin/rest.li/compare/v29.32.2...v29.32.3
[29.32.2]: https://github.com/linkedin/rest.li/compare/v29.32.1...v29.32.2
[29.32.1]: https://github.com/linkedin/rest.li/compare/v29.32.0...v29.32.1
[29.32.0]: https://github.com/linkedin/rest.li/compare/v29.31.0...v29.32.0
[29.31.0]: https://github.com/linkedin/rest.li/compare/v29.30.0...v29.31.0
[29.30.0]: https://github.com/linkedin/rest.li/compare/v29.29.2...v29.30.0
[29.29.2]: https://github.com/linkedin/rest.li/compare/v29.29.1...v29.29.2
[29.29.1]: https://github.com/linkedin/rest.li/compare/v29.29.0...v29.29.1
[29.29.0]: https://github.com/linkedin/rest.li/compare/v29.28.0...v29.29.0
[29.28.0]: https://github.com/linkedin/rest.li/compare/v29.27.0...v29.28.0
[29.27.0]: https://github.com/linkedin/rest.li/compare/v29.26.4...v29.27.0
[29.26.4]: https://github.com/linkedin/rest.li/compare/v29.26.3...v29.26.4
[29.26.3]: https://github.com/linkedin/rest.li/compare/v29.26.2...v29.26.3
[29.26.2]: https://github.com/linkedin/rest.li/compare/v29.26.1...v29.26.2
[29.26.1]: https://github.com/linkedin/rest.li/compare/v29.26.0...v29.26.1
[29.26.0]: https://github.com/linkedin/rest.li/compare/v29.25.0...v29.26.0
[29.25.0]: https://github.com/linkedin/rest.li/compare/v29.24.0...v29.25.0
[29.24.0]: https://github.com/linkedin/rest.li/compare/v29.23.3...v29.24.0
[29.23.3]: https://github.com/linkedin/rest.li/compare/v29.23.2...v29.23.3
[29.23.2]: https://github.com/linkedin/rest.li/compare/v29.23.1...v29.23.2
[29.23.1]: https://github.com/linkedin/rest.li/compare/v29.23.0...v29.23.1
[29.23.0]: https://github.com/linkedin/rest.li/compare/v29.22.16...v29.23.0
[29.22.16]: https://github.com/linkedin/rest.li/compare/v29.22.15...v29.22.16
[29.22.15]: https://github.com/linkedin/rest.li/compare/v29.22.14...v29.22.15
[29.22.14]: https://github.com/linkedin/rest.li/compare/v29.22.13...v29.22.14
[29.22.13]: https://github.com/linkedin/rest.li/compare/v29.22.12...v29.22.13
[29.22.12]: https://github.com/linkedin/rest.li/compare/v29.22.11...v29.22.12
[29.22.11]: https://github.com/linkedin/rest.li/compare/v29.22.10...v29.22.11
[29.22.10]: https://github.com/linkedin/rest.li/compare/v29.22.9...v29.22.10
[29.22.9]: https://github.com/linkedin/rest.li/compare/v29.22.8...v29.22.9
[29.22.8]: https://github.com/linkedin/rest.li/compare/v29.22.7...v29.22.8
[29.22.7]: https://github.com/linkedin/rest.li/compare/v29.22.6...v29.22.7
[29.22.6]: https://github.com/linkedin/rest.li/compare/v29.22.5...v29.22.6
[29.22.5]: https://github.com/linkedin/rest.li/compare/v29.22.4...v29.22.5
[29.22.4]: https://github.com/linkedin/rest.li/compare/v29.22.3...v29.22.4
[29.22.3]: https://github.com/linkedin/rest.li/compare/v29.22.2...v29.22.3
[29.22.2]: https://github.com/linkedin/rest.li/compare/v29.22.1...v29.22.2
[29.22.1]: https://github.com/linkedin/rest.li/compare/v29.22.0...v29.22.1
[29.22.0]: https://github.com/linkedin/rest.li/compare/v29.21.5...v29.22.0
[29.21.5]: https://github.com/linkedin/rest.li/compare/v29.21.4...v29.21.5
[29.21.4]: https://github.com/linkedin/rest.li/compare/v29.21.3...v29.21.4
[29.21.3]: https://github.com/linkedin/rest.li/compare/v29.21.2...v29.21.3
[29.21.2]: https://github.com/linkedin/rest.li/compare/v29.21.1...v29.21.2
[29.21.1]: https://github.com/linkedin/rest.li/compare/v29.21.0...v29.21.1
[29.21.0]: https://github.com/linkedin/rest.li/compare/v29.20.1...v29.21.0
[29.20.1]: https://github.com/linkedin/rest.li/compare/v29.20.0...v29.20.1
[29.20.0]: https://github.com/linkedin/rest.li/compare/v29.19.17...v29.20.0
[29.19.17]: https://github.com/linkedin/rest.li/compare/v29.19.16...v29.19.17
[29.19.16]: https://github.com/linkedin/rest.li/compare/v29.19.15...v29.19.16
[29.19.15]: https://github.com/linkedin/rest.li/compare/v29.19.14...v29.19.15
[29.19.14]: https://github.com/linkedin/rest.li/compare/v29.19.13...v29.19.14
[29.19.13]: https://github.com/linkedin/rest.li/compare/v29.19.12...v29.19.13
[29.19.12]: https://github.com/linkedin/rest.li/compare/v29.19.11...v29.19.12
[29.19.11]: https://github.com/linkedin/rest.li/compare/v29.19.10...v29.19.11
[29.19.10]: https://github.com/linkedin/rest.li/compare/v29.19.9...v29.19.10
[29.19.9]: https://github.com/linkedin/rest.li/compare/v29.19.8...v29.19.9
[29.19.8]: https://github.com/linkedin/rest.li/compare/v29.19.7...v29.19.8
[29.19.7]: https://github.com/linkedin/rest.li/compare/v29.19.6...v29.19.7
[29.19.6]: https://github.com/linkedin/rest.li/compare/v29.19.5...v29.19.6
[29.19.5]: https://github.com/linkedin/rest.li/compare/v29.19.4...v29.19.5
[29.19.4]: https://github.com/linkedin/rest.li/compare/v29.19.3...v29.19.4
[29.19.3]: https://github.com/linkedin/rest.li/compare/v29.19.2...v29.19.3
[29.19.2]: https://github.com/linkedin/rest.li/compare/v29.19.1...v29.19.2
[29.19.1]: https://github.com/linkedin/rest.li/compare/v29.18.15...v29.19.1
[29.18.15]: https://github.com/linkedin/rest.li/compare/v29.18.14...v29.18.15
[29.18.14]: https://github.com/linkedin/rest.li/compare/v29.18.13...v29.18.14
[29.18.13]: https://github.com/linkedin/rest.li/compare/v29.18.12...v29.18.13
[29.18.12]: https://github.com/linkedin/rest.li/compare/v29.18.11...v29.18.12
[29.18.11]: https://github.com/linkedin/rest.li/compare/v29.18.10...v29.18.11
[29.18.10]: https://github.com/linkedin/rest.li/compare/v29.18.9...v29.18.10
[29.18.9]: https://github.com/linkedin/rest.li/compare/v29.18.8...v29.18.9
[29.18.8]: https://github.com/linkedin/rest.li/compare/v29.18.7...v29.18.8
[29.18.7]: https://github.com/linkedin/rest.li/compare/v29.18.6...v29.18.7
[29.18.6]: https://github.com/linkedin/rest.li/compare/v29.18.5...v29.18.6
[29.18.5]: https://github.com/linkedin/rest.li/compare/v29.18.4...v29.18.5
[29.18.4]: https://github.com/linkedin/rest.li/compare/v29.18.3...v29.18.4
[29.18.3]: https://github.com/linkedin/rest.li/compare/v29.18.2...v29.18.3
[29.18.2]: https://github.com/linkedin/rest.li/compare/v29.18.1...v29.18.2
[29.18.1]: https://github.com/linkedin/rest.li/compare/v29.18.0...v29.18.1
[29.18.0]: https://github.com/linkedin/rest.li/compare/v29.17.4...v29.18.0
[29.17.4]: https://github.com/linkedin/rest.li/compare/v29.17.3...v29.17.4
[29.17.3]: https://github.com/linkedin/rest.li/compare/v29.17.2...v29.17.3
[29.17.2]: https://github.com/linkedin/rest.li/compare/v29.17.1...v29.17.2
[29.17.1]: https://github.com/linkedin/rest.li/compare/v29.17.0...v29.17.1
[29.17.0]: https://github.com/linkedin/rest.li/compare/v29.16.2...v29.17.0
[29.16.2]: https://github.com/linkedin/rest.li/compare/v29.16.1...v29.16.2
[29.16.1]: https://github.com/linkedin/rest.li/compare/v29.16.0...v29.16.1
[29.16.0]: https://github.com/linkedin/rest.li/compare/v29.15.9...v29.16.0
[29.15.9]: https://github.com/linkedin/rest.li/compare/v29.15.8...v29.15.9
[29.15.8]: https://github.com/linkedin/rest.li/compare/v29.15.7...v29.15.8
[29.15.7]: https://github.com/linkedin/rest.li/compare/v29.15.6...v29.15.7
[29.15.6]: https://github.com/linkedin/rest.li/compare/v29.15.5...v29.15.6
[29.15.5]: https://github.com/linkedin/rest.li/compare/v29.15.4...v29.15.5
[29.15.4]: https://github.com/linkedin/rest.li/compare/v29.15.3...v29.15.4
[29.15.3]: https://github.com/linkedin/rest.li/compare/v29.15.2...v29.15.3
[29.15.2]: https://github.com/linkedin/rest.li/compare/v29.15.1...v29.15.2
[29.15.1]: https://github.com/linkedin/rest.li/compare/v29.15.0...v29.15.1
[29.15.0]: https://github.com/linkedin/rest.li/compare/v29.14.5...v29.15.0
[29.14.5]: https://github.com/linkedin/rest.li/compare/v29.14.4...v29.14.5
[29.14.4]: https://github.com/linkedin/rest.li/compare/v29.14.3...v29.14.4
[29.14.3]: https://github.com/linkedin/rest.li/compare/v29.14.2...v29.14.3
[29.14.2]: https://github.com/linkedin/rest.li/compare/v29.14.1...v29.14.2
[29.14.1]: https://github.com/linkedin/rest.li/compare/v29.14.0...v29.14.1
[29.14.0]: https://github.com/linkedin/rest.li/compare/v29.13.12...v29.14.0
[29.13.12]: https://github.com/linkedin/rest.li/compare/v29.13.11...v29.13.12
[29.13.11]: https://github.com/linkedin/rest.li/compare/v29.13.10...v29.13.11
[29.13.10]: https://github.com/linkedin/rest.li/compare/v29.13.9...v29.13.10
[29.13.9]: https://github.com/linkedin/rest.li/compare/v29.13.8...v29.13.9
[29.13.8]: https://github.com/linkedin/rest.li/compare/v29.13.7...v29.13.8
[29.13.7]: https://github.com/linkedin/rest.li/compare/v29.13.6...v29.13.7
[29.13.6]: https://github.com/linkedin/rest.li/compare/v29.13.5...v29.13.6
[29.13.5]: https://github.com/linkedin/rest.li/compare/v29.13.4...v29.13.5
[29.13.4]: https://github.com/linkedin/rest.li/compare/v29.13.3...v29.13.4
[29.13.3]: https://github.com/linkedin/rest.li/compare/v29.13.2...v29.13.3
[29.13.2]: https://github.com/linkedin/rest.li/compare/v29.13.1...v29.13.2
[29.13.1]: https://github.com/linkedin/rest.li/compare/v29.13.0...v29.13.1
[29.13.0]: https://github.com/linkedin/rest.li/compare/v29.12.0...v29.13.0
[29.12.0]: https://github.com/linkedin/rest.li/compare/v29.11.3...v29.12.0
[29.11.3]: https://github.com/linkedin/rest.li/compare/v29.11.2...v29.11.3
[29.11.2]: https://github.com/linkedin/rest.li/compare/v29.11.1...v29.11.2
[29.11.1]: https://github.com/linkedin/rest.li/compare/v29.10.1...v29.11.1
[29.10.1]: https://github.com/linkedin/rest.li/compare/v29.10.0...v29.10.1
[29.10.0]: https://github.com/linkedin/rest.li/compare/v29.9.2...v29.10.0
[29.9.2]: https://github.com/linkedin/rest.li/compare/v29.9.1...v29.9.2
[29.9.1]: https://github.com/linkedin/rest.li/compare/v29.9.0...v29.9.1
[29.9.0]: https://github.com/linkedin/rest.li/compare/v29.8.5...v29.9.0
[29.8.5]: https://github.com/linkedin/rest.li/compare/v29.8.4...v29.8.5
[29.8.4]: https://github.com/linkedin/rest.li/compare/v29.8.3...v29.8.4
[29.8.3]: https://github.com/linkedin/rest.li/compare/v29.8.2...v29.8.3
[29.8.2]: https://github.com/linkedin/rest.li/compare/v29.8.1...v29.8.2
[29.8.1]: https://github.com/linkedin/rest.li/compare/v29.8.0...v29.8.1
[29.8.0]: https://github.com/linkedin/rest.li/compare/v29.7.15...v29.8.0
[29.7.15]: https://github.com/linkedin/rest.li/compare/v29.7.14...v29.7.15
[29.7.14]: https://github.com/linkedin/rest.li/compare/v29.7.13...v29.7.14
[29.7.13]: https://github.com/linkedin/rest.li/compare/v29.7.12...v29.7.13
[29.7.12]: https://github.com/linkedin/rest.li/compare/v29.7.11...v29.7.12
[29.7.11]: https://github.com/linkedin/rest.li/compare/v29.7.10...v29.7.11
[29.7.10]: https://github.com/linkedin/rest.li/compare/v29.7.9...v29.7.10
[29.7.9]: https://github.com/linkedin/rest.li/compare/v29.7.8...v29.7.9
[29.7.8]: https://github.com/linkedin/rest.li/compare/v29.7.7...v29.7.8
[29.7.7]: https://github.com/linkedin/rest.li/compare/v29.7.6...v29.7.7
[29.7.6]: https://github.com/linkedin/rest.li/compare/v29.7.5...v29.7.6
[29.7.5]: https://github.com/linkedin/rest.li/compare/v29.7.4...v29.7.5
[29.7.4]: https://github.com/linkedin/rest.li/compare/v29.7.3...v29.7.4
[29.7.3]: https://github.com/linkedin/rest.li/compare/v29.7.2...v29.7.3
[29.7.2]: https://github.com/linkedin/rest.li/compare/v29.7.1...v29.7.2
[29.7.1]: https://github.com/linkedin/rest.li/compare/v29.7.0...v29.7.1
[29.7.0]: https://github.com/linkedin/rest.li/compare/v29.6.9...v29.7.0
[29.6.9]: https://github.com/linkedin/rest.li/compare/v29.6.8...v29.6.9
[29.6.8]: https://github.com/linkedin/rest.li/compare/v29.6.7...v29.6.8
[29.6.7]: https://github.com/linkedin/rest.li/compare/v29.6.6...v29.6.7
[29.6.6]: https://github.com/linkedin/rest.li/compare/v29.6.5...v29.6.6
[29.6.5]: https://github.com/linkedin/rest.li/compare/v29.6.5...master
[29.6.4]: https://github.com/linkedin/rest.li/compare/v29.6.3...v29.6.4
[29.6.3]: https://github.com/linkedin/rest.li/compare/v29.6.2...v29.6.3
[29.6.2]: https://github.com/linkedin/rest.li/compare/v29.6.1...v29.6.2
[29.6.1]: https://github.com/linkedin/rest.li/compare/v29.6.0...v29.6.1
[29.6.0]: https://github.com/linkedin/rest.li/compare/v29.5.8...v29.6.0
[29.5.8]: https://github.com/linkedin/rest.li/compare/v29.5.7...v29.5.8
[29.5.7]: https://github.com/linkedin/rest.li/compare/v29.5.6...v29.5.7
[29.5.6]: https://github.com/linkedin/rest.li/compare/v29.5.5...v29.5.6
[29.5.5]: https://github.com/linkedin/rest.li/compare/v29.5.4...v29.5.5
[29.5.4]: https://github.com/linkedin/rest.li/compare/v29.5.3...v29.5.4
[29.5.3]: https://github.com/linkedin/rest.li/compare/v29.5.2...v29.5.3
[29.5.2]: https://github.com/linkedin/rest.li/compare/v29.5.1...v29.5.2
[29.5.1]: https://github.com/linkedin/rest.li/compare/v29.5.0...v29.5.1
[29.5.0]: https://github.com/linkedin/rest.li/compare/v29.4.14...v29.5.0
[29.4.14]: https://github.com/linkedin/rest.li/compare/v29.4.13...v29.4.14
[29.4.13]: https://github.com/linkedin/rest.li/compare/v29.4.12...v29.4.13
[29.4.12]: https://github.com/linkedin/rest.li/compare/v29.4.11...v29.4.12
[29.4.11]: https://github.com/linkedin/rest.li/compare/v29.4.10...v29.4.11
[29.4.10]: https://github.com/linkedin/rest.li/compare/v29.4.9...v29.4.10
[29.4.9]: https://github.com/linkedin/rest.li/compare/v29.4.8...v29.4.9
[29.4.8]: https://github.com/linkedin/rest.li/compare/v29.4.7...v29.4.8
[29.4.7]: https://github.com/linkedin/rest.li/compare/v29.4.6...v29.4.7
[29.4.6]: https://github.com/linkedin/rest.li/compare/v29.4.5...v29.4.6
[29.4.5]: https://github.com/linkedin/rest.li/compare/v29.4.4...v29.4.5
[29.4.4]: https://github.com/linkedin/rest.li/compare/v29.4.3...v29.4.4
[29.4.3]: https://github.com/linkedin/rest.li/compare/v29.4.2...v29.4.3
[29.4.2]: https://github.com/linkedin/rest.li/compare/v29.4.1...v29.4.2
[29.4.1]: https://github.com/linkedin/rest.li/compare/v29.4.0...v29.4.1
[29.4.0]: https://github.com/linkedin/rest.li/compare/v29.3.2...v29.4.0
[29.3.2]: https://github.com/linkedin/rest.li/compare/v29.3.1...v29.3.2
[29.3.1]: https://github.com/linkedin/rest.li/compare/v29.3.0...v29.3.1
[29.3.0]: https://github.com/linkedin/rest.li/compare/v29.2.5...v29.3.0
[29.2.5]: https://github.com/linkedin/rest.li/compare/v29.2.4...v29.2.5
[29.2.4]: https://github.com/linkedin/rest.li/compare/v29.2.3...v29.2.4
[29.2.3]: https://github.com/linkedin/rest.li/compare/v29.2.2...v29.2.3
[29.2.2]: https://github.com/linkedin/rest.li/compare/v29.2.1...v29.2.2
[29.2.1]: https://github.com/linkedin/rest.li/compare/v29.2.0...v29.2.1
[29.2.0]: https://github.com/linkedin/rest.li/compare/v29.1.0...v29.2.0
[29.1.0]: https://github.com/linkedin/rest.li/compare/v29.0.2...v29.1.0
[29.0.2]: https://github.com/linkedin/rest.li/compare/v29.0.1...v29.0.2
[29.0.1]: https://github.com/linkedin/rest.li/compare/v29.0.0...v29.0.1
[29.0.0]: https://github.com/linkedin/rest.li/compare/v28.3.11...v29.0.0
[28.3.11]: https://github.com/linkedin/rest.li/compare/v28.3.10...v28.3.11
[28.3.10]: https://github.com/linkedin/rest.li/compare/v28.3.9...v28.3.10
[28.3.9]: https://github.com/linkedin/rest.li/compare/v28.3.8...v28.3.9
[28.3.8]: https://github.com/linkedin/rest.li/compare/v28.3.7...v28.3.8
[28.3.7]: https://github.com/linkedin/rest.li/compare/v28.3.6...v28.3.7
[28.3.6]: https://github.com/linkedin/rest.li/compare/v28.3.5...v28.3.6
[28.3.5]: https://github.com/linkedin/rest.li/compare/v28.3.4...v28.3.5
[28.3.4]: https://github.com/linkedin/rest.li/compare/v28.3.3...v28.3.4
[28.3.3]: https://github.com/linkedin/rest.li/compare/v28.3.2...v28.3.3
[28.3.2]: https://github.com/linkedin/rest.li/compare/v28.3.1...v28.3.2
[28.3.1]: https://github.com/linkedin/rest.li/compare/v28.3.0...v28.3.1
[28.3.0]: https://github.com/linkedin/rest.li/compare/v28.2.8...v28.3.0
[28.2.8]: https://github.com/linkedin/rest.li/compare/v28.2.7...v28.2.8
[28.2.7]: https://github.com/linkedin/rest.li/compare/v28.2.6...v28.2.7
[28.2.6]: https://github.com/linkedin/rest.li/compare/v28.2.5...v28.2.6
[28.2.5]: https://github.com/linkedin/rest.li/compare/v28.2.4...v28.2.5
[28.2.4]: https://github.com/linkedin/rest.li/compare/v28.2.3...v28.2.4
[28.2.3]: https://github.com/linkedin/rest.li/compare/v28.2.2...v28.2.3
[28.2.2]: https://github.com/linkedin/rest.li/compare/v28.2.1...v28.2.2
[28.2.1]: https://github.com/linkedin/rest.li/compare/v28.2.0...v28.2.1
[28.2.0]: https://github.com/linkedin/rest.li/compare/v28.1.36...v28.2.0
[28.1.36]: https://github.com/linkedin/rest.li/compare/v28.1.35...v28.1.36
[28.1.35]: https://github.com/linkedin/rest.li/compare/v28.1.34...v28.1.35
[28.1.34]: https://github.com/linkedin/rest.li/compare/v28.1.33...v28.1.34
[28.1.33]: https://github.com/linkedin/rest.li/compare/v28.1.32...v28.1.33
[28.1.32]: https://github.com/linkedin/rest.li/compare/v28.1.31...v28.1.32
[28.1.31]: https://github.com/linkedin/rest.li/compare/v28.1.30...v28.1.31
[28.1.30]: https://github.com/linkedin/rest.li/compare/v28.1.29...v28.1.30
[28.1.29]: https://github.com/linkedin/rest.li/compare/v28.1.28...v28.1.29
[28.1.28]: https://github.com/linkedin/rest.li/compare/v28.1.27...v28.1.28
[28.1.27]: https://github.com/linkedin/rest.li/compare/v28.1.26...v28.1.27
[28.1.26]: https://github.com/linkedin/rest.li/compare/v28.1.25...v28.1.26
[28.1.25]: https://github.com/linkedin/rest.li/compare/v28.1.24...v28.1.25
[28.1.24]: https://github.com/linkedin/rest.li/compare/v28.1.23...v28.1.24
[28.1.23]: https://github.com/linkedin/rest.li/compare/v28.1.22...v28.1.23
[28.1.22]: https://github.com/linkedin/rest.li/compare/v28.1.21...v28.1.22
[28.1.21]: https://github.com/linkedin/rest.li/compare/v28.1.20...v28.1.21
[28.1.20]: https://github.com/linkedin/rest.li/compare/v28.1.19...v28.1.20
[28.1.19]: https://github.com/linkedin/rest.li/compare/v28.1.18...v28.1.19
[28.1.18]: https://github.com/linkedin/rest.li/compare/v28.1.17...v28.1.18
[28.1.17]: https://github.com/linkedin/rest.li/compare/v28.1.16...v28.1.17
[28.1.16]: https://github.com/linkedin/rest.li/compare/v28.1.15...v28.1.16
[28.1.15]: https://github.com/linkedin/rest.li/compare/v28.1.14...v28.1.15
[28.1.14]: https://github.com/linkedin/rest.li/compare/v28.1.13...v28.1.14
[28.1.13]: https://github.com/linkedin/rest.li/compare/v28.1.12...v28.1.13
[28.1.12]: https://github.com/linkedin/rest.li/compare/v28.1.11...v28.1.12
[28.1.11]: https://github.com/linkedin/rest.li/compare/v28.1.10...v28.1.11
[28.1.10]: https://github.com/linkedin/rest.li/compare/v28.1.9...v28.1.10
[28.1.9]: https://github.com/linkedin/rest.li/compare/v28.1.8...v28.1.9
[28.1.8]: https://github.com/linkedin/rest.li/compare/v28.1.7...v28.1.8
[28.1.7]: https://github.com/linkedin/rest.li/compare/v28.1.6...v28.1.7
[28.1.6]: https://github.com/linkedin/rest.li/compare/v28.1.5...v28.1.6
[28.1.5]: https://github.com/linkedin/rest.li/compare/v28.1.4...v28.1.5
[28.1.4]: https://github.com/linkedin/rest.li/compare/v28.1.3...v28.1.4
[28.1.3]: https://github.com/linkedin/rest.li/compare/v28.1.2...v28.1.3
[28.1.2]: https://github.com/linkedin/rest.li/compare/v28.1.1...v28.1.2
[28.1.1]: https://github.com/linkedin/rest.li/compare/v28.1.0...v28.1.1
[28.1.0]: https://github.com/linkedin/rest.li/compare/v28.0.12...v28.1.0
[28.0.12]: https://github.com/linkedin/rest.li/compare/v28.0.11...v28.0.12
[28.0.11]: https://github.com/linkedin/rest.li/compare/v28.0.10...v28.0.11
[28.0.10]: https://github.com/linkedin/rest.li/compare/v28.0.9...v28.0.10
[28.0.9]: https://github.com/linkedin/rest.li/compare/v28.0.8...v28.0.9
[28.0.8]: https://github.com/linkedin/rest.li/compare/v28.0.7...v28.0.8
[28.0.7]: https://github.com/linkedin/rest.li/compare/v28.0.6...v28.0.7
[28.0.6]: https://github.com/linkedin/rest.li/compare/v28.0.5...v28.0.6
[28.0.5]: https://github.com/linkedin/rest.li/compare/v28.0.4...v28.0.5
[28.0.4]: https://github.com/linkedin/rest.li/compare/v28.0.3...v28.0.4
[28.0.3]: https://github.com/linkedin/rest.li/compare/v28.0.2...v28.0.3
[28.0.2]: https://github.com/linkedin/rest.li/compare/v28.0.1...v28.0.2
[28.0.1]: https://github.com/linkedin/rest.li/compare/v28.0.0...v28.0.1
[28.0.0]: https://github.com/linkedin/rest.li/compare/v27.7.18...v28.0.0
[27.7.18]: https://github.com/linkedin/rest.li/compare/v27.7.17...v27.7.18
[27.7.17]: https://github.com/linkedin/rest.li/compare/v27.7.16...v27.7.17
[27.7.16]: https://github.com/linkedin/rest.li/compare/v27.7.15...v27.7.16
[27.7.15]: https://github.com/linkedin/rest.li/compare/v27.7.14...v27.7.15
[27.7.14]: https://github.com/linkedin/rest.li/compare/v27.7.13...v27.7.14
[27.7.13]: https://github.com/linkedin/rest.li/compare/v27.7.12...v27.7.13
[27.7.12]: https://github.com/linkedin/rest.li/compare/v27.7.11...v27.7.12
[27.7.11]: https://github.com/linkedin/rest.li/compare/v27.7.10...v27.7.11
[27.7.10]: https://github.com/linkedin/rest.li/compare/v27.7.9...v27.7.10
[27.7.9]: https://github.com/linkedin/rest.li/compare/v27.7.8...v27.7.9
[27.7.8]: https://github.com/linkedin/rest.li/compare/v27.7.7...v27.7.8
[27.7.7]: https://github.com/linkedin/rest.li/compare/v27.7.6...v27.7.7
[27.7.6]: https://github.com/linkedin/rest.li/compare/v27.7.5...v27.7.6
[27.7.5]: https://github.com/linkedin/rest.li/compare/v27.7.4...v27.7.5
[27.7.4]: https://github.com/linkedin/rest.li/compare/v27.7.3...v27.7.4
[27.7.3]: https://github.com/linkedin/rest.li/compare/v27.7.2...v27.7.3
[27.7.2]: https://github.com/linkedin/rest.li/compare/v27.7.1...v27.7.2
[27.7.1]: https://github.com/linkedin/rest.li/compare/v27.7.0...v27.7.1
[27.7.0]: https://github.com/linkedin/rest.li/compare/v27.6.8...v27.7.0
[27.6.8]: https://github.com/linkedin/rest.li/compare/v27.6.7...v27.6.8
[27.6.7]: https://github.com/linkedin/rest.li/compare/v27.6.6...v27.6.7
[27.6.6]: https://github.com/linkedin/rest.li/compare/v27.6.5...v27.6.6
[27.6.5]: https://github.com/linkedin/rest.li/compare/v27.6.4...v27.6.5
[27.6.4]: https://github.com/linkedin/rest.li/compare/v27.6.3...v27.6.4
[27.6.3]: https://github.com/linkedin/rest.li/compare/v27.6.2...v27.6.3
[27.6.2]: https://github.com/linkedin/rest.li/compare/v27.6.1...v27.6.2
[27.6.1]: https://github.com/linkedin/rest.li/compare/v27.5.3...v27.6.1
[27.5.3]: https://github.com/linkedin/rest.li/compare/v27.5.2...v27.5.3
[27.5.2]: https://github.com/linkedin/rest.li/compare/v27.5.1...v27.5.2
[27.5.1]: https://github.com/linkedin/rest.li/compare/v27.5.0...v27.5.1
[27.5.0]: https://github.com/linkedin/rest.li/compare/v27.4.3...v27.5.0
[27.4.3]: https://github.com/linkedin/rest.li/compare/v27.4.2...v27.4.3
[27.4.2]: https://github.com/linkedin/rest.li/compare/v27.4.1...v27.4.2
[27.4.1]: https://github.com/linkedin/rest.li/compare/v27.4.0...v27.4.1
[27.4.0]: https://github.com/linkedin/rest.li/compare/v27.3.19...v27.4.0
[27.3.19]: https://github.com/linkedin/rest.li/compare/v27.3.18...v27.3.19
[27.3.18]: https://github.com/linkedin/rest.li/compare/v27.3.17...v27.3.18
[27.3.17]: https://github.com/linkedin/rest.li/compare/v27.3.16...v27.3.17
[27.3.16]: https://github.com/linkedin/rest.li/compare/v27.3.15...v27.3.16
[27.3.15]: https://github.com/linkedin/rest.li/compare/v27.3.14...v27.3.15
[27.3.14]: https://github.com/linkedin/rest.li/compare/v27.3.13...v27.3.14
[27.3.13]: https://github.com/linkedin/rest.li/compare/v27.3.12...v27.3.13
[27.3.12]: https://github.com/linkedin/rest.li/compare/v27.3.11...v27.3.12
[27.3.11]: https://github.com/linkedin/rest.li/compare/v27.3.10...v27.3.11
[27.3.10]: https://github.com/linkedin/rest.li/compare/v27.3.9...v27.3.10
[27.3.9]: https://github.com/linkedin/rest.li/compare/v27.3.8...v27.3.9
[27.3.8]: https://github.com/linkedin/rest.li/compare/v27.3.7...v27.3.8
[27.3.7]: https://github.com/linkedin/rest.li/compare/v27.3.6...v27.3.7
[27.3.6]: https://github.com/linkedin/rest.li/compare/v27.3.5...v27.3.6
[27.3.5]: https://github.com/linkedin/rest.li/compare/v27.3.4...v27.3.5
[27.3.4]: https://github.com/linkedin/rest.li/compare/v27.3.3...v27.3.4
[27.3.3]: https://github.com/linkedin/rest.li/compare/v27.3.2...v27.3.3
[27.3.2]: https://github.com/linkedin/rest.li/compare/v27.3.1...v27.3.2
[27.3.1]: https://github.com/linkedin/rest.li/compare/v27.3.0...v27.3.1
[27.3.0]: https://github.com/linkedin/rest.li/compare/v27.2.0...v27.3.0
[27.2.0]: https://github.com/linkedin/rest.li/compare/v27.1.7...v27.2.0
[27.1.7]: https://github.com/linkedin/rest.li/compare/v27.1.6...v27.1.7
[27.1.6]: https://github.com/linkedin/rest.li/compare/v27.1.5...v27.1.6
[27.1.5]: https://github.com/linkedin/rest.li/compare/v27.1.4...v27.1.5
[27.1.4]: https://github.com/linkedin/rest.li/compare/v27.1.3...v27.1.4
[27.1.3]: https://github.com/linkedin/rest.li/compare/v27.1.2...v27.1.3
[27.1.2]: https://github.com/linkedin/rest.li/compare/v27.1.1...v27.1.2
[27.1.1]: https://github.com/linkedin/rest.li/compare/v27.1.0...v27.1.1
[27.1.0]: https://github.com/linkedin/rest.li/compare/v27.0.18...v27.1.0
[27.0.18]: https://github.com/linkedin/rest.li/compare/v27.0.17...v27.0.18
[27.0.17]: https://github.com/linkedin/rest.li/compare/v27.0.16...v27.0.17
[27.0.16]: https://github.com/linkedin/rest.li/compare/v27.0.15...v27.0.16
[27.0.15]: https://github.com/linkedin/rest.li/compare/v27.0.14...v27.0.15
[27.0.14]: https://github.com/linkedin/rest.li/compare/v27.0.13...v27.0.14
[27.0.13]: https://github.com/linkedin/rest.li/compare/v27.0.12...v27.0.13
[27.0.12]: https://github.com/linkedin/rest.li/compare/v27.0.11...v27.0.12
[27.0.11]: https://github.com/linkedin/rest.li/compare/v27.0.10...v27.0.11
[27.0.10]: https://github.com/linkedin/rest.li/compare/v27.0.9...v27.0.10
[27.0.9]: https://github.com/linkedin/rest.li/compare/v27.0.8...v27.0.9
[27.0.8]: https://github.com/linkedin/rest.li/compare/v27.0.7...v27.0.8
[27.0.7]: https://github.com/linkedin/rest.li/compare/v27.0.6...v27.0.7
[27.0.6]: https://github.com/linkedin/rest.li/compare/v27.0.5...v27.0.6
[27.0.5]: https://github.com/linkedin/rest.li/compare/v27.0.4...v27.0.5
[27.0.4]: https://github.com/linkedin/rest.li/compare/v27.0.3...v27.0.4
[27.0.3]: https://github.com/linkedin/rest.li/compare/v27.0.2...v27.0.3
[27.0.2]: https://github.com/linkedin/rest.li/compare/v27.0.1...v27.0.2
[27.0.1]: https://github.com/linkedin/rest.li/compare/v27.0.0...v27.0.1
[27.0.0]: https://github.com/linkedin/rest.li/compare/v26.0.19...v27.0.0
[26.0.19]: https://github.com/linkedin/rest.li/compare/v26.0.18...v26.0.19
[26.0.18]: https://github.com/linkedin/rest.li/compare/v26.0.17...v26.0.18
[26.0.17]: https://github.com/linkedin/rest.li/compare/v26.0.16...v26.0.17
[26.0.16]: https://github.com/linkedin/rest.li/compare/v26.0.15...v26.0.16
[26.0.15]: https://github.com/linkedin/rest.li/compare/v26.0.14...v26.0.15
[26.0.14]: https://github.com/linkedin/rest.li/compare/v26.0.13...v26.0.14
[26.0.13]: https://github.com/linkedin/rest.li/compare/v26.0.12...v26.0.13
[26.0.12]: https://github.com/linkedin/rest.li/compare/v26.0.11...v26.0.12
[26.0.11]: https://github.com/linkedin/rest.li/compare/v26.0.10...v26.0.11
[26.0.10]: https://github.com/linkedin/rest.li/compare/v26.0.9...v26.0.10
[26.0.9]: https://github.com/linkedin/rest.li/compare/v26.0.8...v26.0.9
[26.0.8]: https://github.com/linkedin/rest.li/compare/v26.0.7...v26.0.8
[26.0.7]: https://github.com/linkedin/rest.li/compare/v26.0.6...v26.0.7
[26.0.6]: https://github.com/linkedin/rest.li/compare/v26.0.5...v26.0.6
[26.0.5]: https://github.com/linkedin/rest.li/compare/v26.0.4...v26.0.5
[26.0.4]: https://github.com/linkedin/rest.li/compare/v26.0.3...v26.0.4
[26.0.3]: https://github.com/linkedin/rest.li/compare/v26.0.2...v26.0.3
[26.0.2]: https://github.com/linkedin/rest.li/compare/v26.0.1...v26.0.2
[26.0.1]: https://github.com/linkedin/rest.li/compare/v26.0.0...v26.0.1
[26.0.0]: https://github.com/linkedin/rest.li/compare/v25.0.21...v26.0.0
[25.0.21]: https://github.com/linkedin/rest.li/compare/v25.0.20...v25.0.21
[25.0.20]: https://github.com/linkedin/rest.li/compare/v25.0.19...v25.0.20
[25.0.19]: https://github.com/linkedin/rest.li/compare/v25.0.18...v25.0.19
[25.0.18]: https://github.com/linkedin/rest.li/compare/v25.0.17...v25.0.18
[25.0.17]: https://github.com/linkedin/rest.li/compare/v25.0.16...v25.0.17
[25.0.16]: https://github.com/linkedin/rest.li/compare/v25.0.15...v25.0.16
[25.0.15]: https://github.com/linkedin/rest.li/compare/v25.0.14...v25.0.15
[25.0.14]: https://github.com/linkedin/rest.li/compare/v25.0.13...v25.0.14
[25.0.13]: https://github.com/linkedin/rest.li/compare/v25.0.12...v25.0.13
[25.0.12]: https://github.com/linkedin/rest.li/compare/v25.0.11...v25.0.12
[25.0.11]: https://github.com/linkedin/rest.li/compare/v25.0.10...v25.0.11
[25.0.10]: https://github.com/linkedin/rest.li/compare/v25.0.9...v25.0.10
[25.0.9]: https://github.com/linkedin/rest.li/compare/v25.0.8...v25.0.9
[25.0.8]: https://github.com/linkedin/rest.li/compare/v25.0.7...v25.0.8
[25.0.7]: https://github.com/linkedin/rest.li/compare/v25.0.6...v25.0.7
[25.0.6]: https://github.com/linkedin/rest.li/compare/v25.0.5...v25.0.6
[25.0.5]: https://github.com/linkedin/rest.li/compare/v25.0.4...v25.0.5
[25.0.4]: https://github.com/linkedin/rest.li/compare/v25.0.3...v25.0.4
[25.0.3]: https://github.com/linkedin/rest.li/compare/v25.0.2...v25.0.3
[25.0.2]: https://github.com/linkedin/rest.li/compare/v25.0.1...v25.0.2
[25.0.1]: https://github.com/linkedin/rest.li/compare/v25.0.0...v25.0.1
[25.0.0]: https://github.com/linkedin/rest.li/compare/v24.0.2...v25.0.0
[24.0.2]: https://github.com/linkedin/rest.li/compare/v24.0.1...v24.0.2
[24.0.1]: https://github.com/linkedin/rest.li/compare/v24.0.0...v24.0.1
[24.0.0]: https://github.com/linkedin/rest.li/compare/v23.0.19...v24.0.0
[23.0.19]: https://github.com/linkedin/rest.li/compare/v23.0.18...v23.0.19
[23.0.18]: https://github.com/linkedin/rest.li/compare/v23.0.17...v23.0.18
[23.0.17]: https://github.com/linkedin/rest.li/compare/v23.0.16...v23.0.17
[23.0.16]: https://github.com/linkedin/rest.li/compare/v23.0.15...v23.0.16
[23.0.15]: https://github.com/linkedin/rest.li/compare/v23.0.14...v23.0.15
[23.0.14]: https://github.com/linkedin/rest.li/compare/v23.0.13...v23.0.14
[23.0.13]: https://github.com/linkedin/rest.li/compare/v23.0.12...v23.0.13
[23.0.12]: https://github.com/linkedin/rest.li/compare/v23.0.11...v23.0.12
[23.0.11]: https://github.com/linkedin/rest.li/compare/v23.0.10...v23.0.11
[23.0.10]: https://github.com/linkedin/rest.li/compare/v23.0.9...v23.0.10
[23.0.9]: https://github.com/linkedin/rest.li/compare/v23.0.8...v23.0.9
[23.0.8]: https://github.com/linkedin/rest.li/compare/v23.0.7...v23.0.8
[23.0.7]: https://github.com/linkedin/rest.li/compare/v23.0.6...v23.0.7
[23.0.6]: https://github.com/linkedin/rest.li/compare/v23.0.5...v23.0.6
[23.0.5]: https://github.com/linkedin/rest.li/compare/v23.0.4...v23.0.5
[23.0.4]: https://github.com/linkedin/rest.li/compare/v23.0.3...v23.0.4
[23.0.3]: https://github.com/linkedin/rest.li/compare/v23.0.2...v23.0.3
[23.0.2]: https://github.com/linkedin/rest.li/compare/v23.0.1...v23.0.2
[23.0.1]: https://github.com/linkedin/rest.li/compare/v23.0.0...v23.0.1
[23.0.0]: https://github.com/linkedin/rest.li/compare/v22.0.5...v23.0.0
[22.0.5]: https://github.com/linkedin/rest.li/compare/v22.0.4...v22.0.5
[22.0.4]: https://github.com/linkedin/rest.li/compare/v22.0.3...v22.0.4
[22.0.3]: https://github.com/linkedin/rest.li/compare/v22.0.2...v22.0.3
[22.0.2]: https://github.com/linkedin/rest.li/compare/v22.0.1...v22.0.2
[22.0.1]: https://github.com/linkedin/rest.li/compare/v22.0.0...v22.0.1
[22.0.0]: https://github.com/linkedin/rest.li/compare/v21.0.6...v22.0.0
[21.0.6]: https://github.com/linkedin/rest.li/compare/v21.0.5...v21.0.6
[21.0.5]: https://github.com/linkedin/rest.li/compare/v21.0.4...v21.0.5
[21.0.4]: https://github.com/linkedin/rest.li/compare/v21.0.3...v21.0.4
[21.0.3]: https://github.com/linkedin/rest.li/compare/v21.0.2...v21.0.3
[21.0.2]: https://github.com/linkedin/rest.li/compare/v21.0.1...v21.0.2
[21.0.1]: https://github.com/linkedin/rest.li/compare/v21.0.0...v21.0.1
[21.0.0]: https://github.com/linkedin/rest.li/compare/v20.0.23...v21.0.0
[20.0.23]: https://github.com/linkedin/rest.li/compare/v20.0.22...v20.0.23
[20.0.22]: https://github.com/linkedin/rest.li/compare/v20.0.21...v20.0.22
[20.0.21]: https://github.com/linkedin/rest.li/compare/v20.0.20...v20.0.21
[20.0.20]: https://github.com/linkedin/rest.li/compare/v20.0.19...v20.0.20
[20.0.19]: https://github.com/linkedin/rest.li/compare/v20.0.18...v20.0.19
[20.0.18]: https://github.com/linkedin/rest.li/compare/v20.0.17...v20.0.18
[20.0.17]: https://github.com/linkedin/rest.li/compare/v20.0.16...v20.0.17
[20.0.16]: https://github.com/linkedin/rest.li/compare/v20.0.15...v20.0.16
[20.0.15]: https://github.com/linkedin/rest.li/compare/v20.0.14...v20.0.15
[20.0.14]: https://github.com/linkedin/rest.li/compare/v20.0.13...v20.0.14
[20.0.13]: https://github.com/linkedin/rest.li/compare/v20.0.12...v20.0.13
[20.0.12]: https://github.com/linkedin/rest.li/compare/v20.0.11...v20.0.12
[20.0.11]: https://github.com/linkedin/rest.li/compare/v20.0.10...v20.0.11
[20.0.10]: https://github.com/linkedin/rest.li/compare/v20.0.9...v20.0.10
[20.0.9]: https://github.com/linkedin/rest.li/compare/v20.0.8...v20.0.9
[20.0.8]: https://github.com/linkedin/rest.li/compare/v20.0.7...v20.0.8
[20.0.7]: https://github.com/linkedin/rest.li/compare/v20.0.6...v20.0.7
[20.0.6]: https://github.com/linkedin/rest.li/compare/v20.0.5...v20.0.6
[20.0.5]: https://github.com/linkedin/rest.li/compare/v20.0.4...v20.0.5
[20.0.4]: https://github.com/linkedin/rest.li/compare/v20.0.3...v20.0.4
[20.0.3]: https://github.com/linkedin/rest.li/compare/v20.0.2...v20.0.3
[20.0.2]: https://github.com/linkedin/rest.li/compare/v20.0.1...v20.0.2
[20.0.1]: https://github.com/linkedin/rest.li/compare/v20.0.0...v20.0.1
[20.0.0]: https://github.com/linkedin/rest.li/compare/v19.0.4...v20.0.0
[19.0.4]: https://github.com/linkedin/rest.li/compare/v19.0.3...v19.0.4
[19.0.3]: https://github.com/linkedin/rest.li/compare/v19.0.2...v19.0.3
[19.0.2]: https://github.com/linkedin/rest.li/compare/v19.0.1...v19.0.2
[19.0.1]: https://github.com/linkedin/rest.li/compare/v19.0.0...v19.0.1
[19.0.0]: https://github.com/linkedin/rest.li/compare/v18.0.8...v19.0.0
[18.0.8]: https://github.com/linkedin/rest.li/compare/v18.0.7...v18.0.8
[18.0.7]: https://github.com/linkedin/rest.li/compare/v18.0.6...v18.0.7
[18.0.6]: https://github.com/linkedin/rest.li/compare/v18.0.5...v18.0.6
[18.0.5]: https://github.com/linkedin/rest.li/compare/v18.0.4...v18.0.5
[18.0.4]: https://github.com/linkedin/rest.li/compare/v18.0.3...v18.0.4
[18.0.3]: https://github.com/linkedin/rest.li/compare/v18.0.2...v18.0.3
[18.0.2]: https://github.com/linkedin/rest.li/compare/v18.0.1...v18.0.2
[18.0.1]: https://github.com/linkedin/rest.li/compare/v18.0.0...v18.0.1
[18.0.0]: https://github.com/linkedin/rest.li/compare/v17.0.5...v18.0.0
[17.0.5]: https://github.com/linkedin/rest.li/compare/v17.0.4...v17.0.5
[17.0.4]: https://github.com/linkedin/rest.li/compare/v17.0.3...v17.0.4
[17.0.3]: https://github.com/linkedin/rest.li/compare/v17.0.2...v17.0.3
[17.0.2]: https://github.com/linkedin/rest.li/compare/v17.0.1...v17.0.2
[17.0.1]: https://github.com/linkedin/rest.li/compare/v17.0.0...v17.0.1
[17.0.0]: https://github.com/linkedin/rest.li/compare/v16.0.6...v17.0.0
[16.0.6]: https://github.com/linkedin/rest.li/compare/v16.0.5...v16.0.6
[16.0.5]: https://github.com/linkedin/rest.li/compare/v16.0.4...v16.0.5
[16.0.4]: https://github.com/linkedin/rest.li/compare/v16.0.3...v16.0.4
[16.0.3]: https://github.com/linkedin/rest.li/compare/v16.0.2...v16.0.3
[16.0.2]: https://github.com/linkedin/rest.li/compare/v16.0.1...v16.0.2
[16.0.1]: https://github.com/linkedin/rest.li/compare/v16.0.0...v16.0.1
[16.0.0]: https://github.com/linkedin/rest.li/compare/v15.1.10...v16.0.0
[15.1.10]: https://github.com/linkedin/rest.li/compare/v15.1.9...v15.1.10
[15.1.9]: https://github.com/linkedin/rest.li/compare/v15.1.8...v15.1.9
[15.1.8]: https://github.com/linkedin/rest.li/compare/v15.1.7...v15.1.8
[15.1.7]: https://github.com/linkedin/rest.li/compare/v15.1.6...v15.1.7
[15.1.6]: https://github.com/linkedin/rest.li/compare/v15.1.5...v15.1.6
[15.1.5]: https://github.com/linkedin/rest.li/compare/v15.1.4...v15.1.5
[15.1.4]: https://github.com/linkedin/rest.li/compare/v15.1.3...v15.1.4
[15.1.3]: https://github.com/linkedin/rest.li/compare/v15.1.2...v15.1.3
[15.1.2]: https://github.com/linkedin/rest.li/compare/v15.1.1...v15.1.2
[15.1.1]: https://github.com/linkedin/rest.li/compare/v15.1.0...v15.1.1
[15.1.0]: https://github.com/linkedin/rest.li/compare/v15.0.5...v15.1.0
[15.0.5]: https://github.com/linkedin/rest.li/compare/v15.0.4...v15.0.5
[15.0.4]: https://github.com/linkedin/rest.li/compare/v15.0.3...v15.0.4
[15.0.3]: https://github.com/linkedin/rest.li/compare/v15.0.2...v15.0.3
[15.0.2]: https://github.com/linkedin/rest.li/compare/v15.0.1...v15.0.2
[15.0.1]: https://github.com/linkedin/rest.li/compare/v15.0.0...v15.0.1
[15.0.0]: https://github.com/linkedin/rest.li/compare/v14.1.0...v15.0.0
[14.1.0]: https://github.com/linkedin/rest.li/compare/v14.0.12...v14.1.0
[14.0.12]: https://github.com/linkedin/rest.li/compare/v14.0.11...v14.0.12
[14.0.11]: https://github.com/linkedin/rest.li/compare/v14.0.10...v14.0.11
[14.0.10]: https://github.com/linkedin/rest.li/compare/v14.0.9...v14.0.10
[14.0.9]: https://github.com/linkedin/rest.li/compare/v14.0.8...v14.0.9
[14.0.8]: https://github.com/linkedin/rest.li/compare/v14.0.7...v14.0.8
[14.0.7]: https://github.com/linkedin/rest.li/compare/v14.0.6...v14.0.7
[14.0.6]: https://github.com/linkedin/rest.li/compare/v14.0.5...v14.0.6
[14.0.5]: https://github.com/linkedin/rest.li/compare/v14.0.4...v14.0.5
[14.0.4]: https://github.com/linkedin/rest.li/compare/v14.0.3...v14.0.4
[14.0.3]: https://github.com/linkedin/rest.li/compare/v14.0.2...v14.0.3
[14.0.2]: https://github.com/linkedin/rest.li/compare/v14.0.1...v14.0.2
[14.0.1]: https://github.com/linkedin/rest.li/compare/v14.0.0...v14.0.1
[14.0.0]: https://github.com/linkedin/rest.li/compare/v13.0.7...v14.0.0
[13.0.7]: https://github.com/linkedin/rest.li/compare/v13.0.6...v13.0.7
[13.0.6]: https://github.com/linkedin/rest.li/compare/v13.0.5...v13.0.6
[13.0.5]: https://github.com/linkedin/rest.li/compare/v13.0.4...v13.0.5
[13.0.4]: https://github.com/linkedin/rest.li/compare/v13.0.3...v13.0.4
[13.0.3]: https://github.com/linkedin/rest.li/compare/v13.0.2...v13.0.3
[13.0.2]: https://github.com/linkedin/rest.li/compare/v13.0.1...v13.0.2
[13.0.1]: https://github.com/linkedin/rest.li/compare/v13.0.0...v13.0.1
[13.0.0]: https://github.com/linkedin/rest.li/compare/v12.0.3...v13.0.0
[12.0.3]: https://github.com/linkedin/rest.li/compare/v12.0.2...v12.0.3
[12.0.2]: https://github.com/linkedin/rest.li/compare/v12.0.1...v12.0.2
[12.0.1]: https://github.com/linkedin/rest.li/compare/v12.0.0...v12.0.1
[12.0.0]: https://github.com/linkedin/rest.li/compare/v11.1.1...v12.0.0
[11.1.1]: https://github.com/linkedin/rest.li/compare/v11.1.0...v11.1.1
[11.1.0]: https://github.com/linkedin/rest.li/compare/v11.0.18...v11.1.0
[11.0.18]: https://github.com/linkedin/rest.li/compare/v11.0.17...v11.0.18
[11.0.17]: https://github.com/linkedin/rest.li/compare/v11.0.16...v11.0.17
[11.0.16]: https://github.com/linkedin/rest.li/compare/v11.0.15...v11.0.16
[11.0.15]: https://github.com/linkedin/rest.li/compare/v11.0.14...v11.0.15
[11.0.14]: https://github.com/linkedin/rest.li/compare/v11.0.13...v11.0.14
[11.0.13]: https://github.com/linkedin/rest.li/compare/v11.0.12...v11.0.13
[11.0.12]: https://github.com/linkedin/rest.li/compare/v11.0.11...v11.0.12
[11.0.11]: https://github.com/linkedin/rest.li/compare/v11.0.10...v11.0.11
[11.0.10]: https://github.com/linkedin/rest.li/compare/v11.0.9...v11.0.10
[11.0.9]: https://github.com/linkedin/rest.li/compare/v11.0.8...v11.0.9
[11.0.8]: https://github.com/linkedin/rest.li/compare/v11.0.7...v11.0.8
[11.0.7]: https://github.com/linkedin/rest.li/compare/v11.0.6...v11.0.7
[11.0.6]: https://github.com/linkedin/rest.li/compare/v11.0.5...v11.0.6
[11.0.5]: https://github.com/linkedin/rest.li/compare/v11.0.4...v11.0.5
[11.0.4]: https://github.com/linkedin/rest.li/compare/v11.0.3...v11.0.4
[11.0.3]: https://github.com/linkedin/rest.li/compare/v11.0.2...v11.0.3
[11.0.2]: https://github.com/linkedin/rest.li/compare/v11.0.1...v11.0.2
[11.0.1]: https://github.com/linkedin/rest.li/compare/v11.0.0...v11.0.1
[11.0.0]: https://github.com/linkedin/rest.li/compare/v10.1.12...v11.0.0
[10.1.12]: https://github.com/linkedin/rest.li/compare/v10.1.11...v10.1.12
[10.1.11]: https://github.com/linkedin/rest.li/compare/v10.1.10...v10.1.11
[10.1.10]: https://github.com/linkedin/rest.li/compare/v10.1.9...v10.1.10
[10.1.9]: https://github.com/linkedin/rest.li/compare/v10.1.8...v10.1.9
[10.1.8]: https://github.com/linkedin/rest.li/compare/v10.1.7...v10.1.8
[10.1.7]: https://github.com/linkedin/rest.li/compare/v10.1.6...v10.1.7
[10.1.6]: https://github.com/linkedin/rest.li/compare/v10.1.5...v10.1.6
[10.1.5]: https://github.com/linkedin/rest.li/compare/v10.1.4...v10.1.5
[10.1.4]: https://github.com/linkedin/rest.li/compare/v10.1.3...v10.1.4
[10.1.3]: https://github.com/linkedin/rest.li/compare/v10.1.2...v10.1.3
[10.1.2]: https://github.com/linkedin/rest.li/compare/v10.1.1...v10.1.2
[10.1.1]: https://github.com/linkedin/rest.li/compare/v10.1.0...v10.1.1
[10.1.0]: https://github.com/linkedin/rest.li/compare/v10.0.2...v10.1.0
[10.0.2]: https://github.com/linkedin/rest.li/compare/v10.0.1...v10.0.2
[10.0.1]: https://github.com/linkedin/rest.li/compare/v10.0.0...v10.0.1
[10.0.0]: https://github.com/linkedin/rest.li/compare/v9.0.7...v10.0.0
[9.0.7]: https://github.com/linkedin/rest.li/compare/v9.0.6...v9.0.7
[9.0.6]: https://github.com/linkedin/rest.li/compare/v9.0.5...v9.0.6
[9.0.5]: https://github.com/linkedin/rest.li/compare/v9.0.4...v9.0.5
[9.0.4]: https://github.com/linkedin/rest.li/compare/v9.0.3...v9.0.4
[9.0.3]: https://github.com/linkedin/rest.li/compare/v9.0.2...v9.0.3
[9.0.2]: https://github.com/linkedin/rest.li/compare/v9.0.1...v9.0.2
[9.0.1]: https://github.com/linkedin/rest.li/compare/v9.0.0...v9.0.1
[9.0.0]: https://github.com/linkedin/rest.li/compare/v8.1.10...v9.0.0
[8.1.10]: https://github.com/linkedin/rest.li/compare/v8.1.9...v8.1.10
[8.1.9]: https://github.com/linkedin/rest.li/compare/v8.1.8...v8.1.9
[8.1.8]: https://github.com/linkedin/rest.li/compare/v8.1.7...v8.1.8
[8.1.7]: https://github.com/linkedin/rest.li/compare/v8.1.6...v8.1.7
[8.1.6]: https://github.com/linkedin/rest.li/compare/v8.1.5...v8.1.6
[8.1.5]: https://github.com/linkedin/rest.li/compare/v8.1.4...v8.1.5
[8.1.4]: https://github.com/linkedin/rest.li/compare/v8.1.3...v8.1.4
[8.1.3]: https://github.com/linkedin/rest.li/compare/v8.1.2...v8.1.3
[8.1.2]: https://github.com/linkedin/rest.li/compare/v8.1.1...v8.1.2
[8.1.1]: https://github.com/linkedin/rest.li/compare/v8.1.0...v8.1.1
[8.1.0]: https://github.com/linkedin/rest.li/compare/v8.0.7...v8.1.0
[8.0.7]: https://github.com/linkedin/rest.li/compare/v8.0.6...v8.0.7
[8.0.6]: https://github.com/linkedin/rest.li/compare/v8.0.5...v8.0.6
[8.0.5]: https://github.com/linkedin/rest.li/compare/v8.0.4...v8.0.5
[8.0.4]: https://github.com/linkedin/rest.li/compare/v8.0.3...v8.0.4
[8.0.3]: https://github.com/linkedin/rest.li/compare/v8.0.2...v8.0.3
[8.0.2]: https://github.com/linkedin/rest.li/compare/v8.0.1...v8.0.2
[8.0.1]: https://github.com/linkedin/rest.li/compare/v8.0.0...v8.0.1
[8.0.0]: https://github.com/linkedin/rest.li/compare/v7.0.3...v8.0.0
[7.0.3]: https://github.com/linkedin/rest.li/compare/v7.0.2...v7.0.3
[7.0.2]: https://github.com/linkedin/rest.li/compare/v7.0.1...v7.0.2
[7.0.1]: https://github.com/linkedin/rest.li/compare/v7.0.0...v7.0.1
[7.0.0]: https://github.com/linkedin/rest.li/compare/v6.1.2...v7.0.0
[6.1.2]: https://github.com/linkedin/rest.li/compare/v6.1.1...v6.1.2
[6.1.1]: https://github.com/linkedin/rest.li/compare/v6.1.0...v6.1.1
[6.1.0]: https://github.com/linkedin/rest.li/compare/v6.0.17...v6.1.0
[6.0.17]: https://github.com/linkedin/rest.li/compare/v6.0.16...v6.0.17
[6.0.16]: https://github.com/linkedin/rest.li/compare/v6.0.15...v6.0.16
[6.0.15]: https://github.com/linkedin/rest.li/compare/v6.0.14...v6.0.15
[6.0.14]: https://github.com/linkedin/rest.li/compare/v6.0.13...v6.0.14
[6.0.13]: https://github.com/linkedin/rest.li/compare/v6.0.12...v6.0.13
[6.0.12]: https://github.com/linkedin/rest.li/compare/v6.0.11...v6.0.12
[6.0.11]: https://github.com/linkedin/rest.li/compare/v6.0.10...v6.0.11
[6.0.10]: https://github.com/linkedin/rest.li/compare/v6.0.9...v6.0.10
[6.0.9]: https://github.com/linkedin/rest.li/compare/v6.0.8...v6.0.9
[6.0.8]: https://github.com/linkedin/rest.li/compare/v6.0.7...v6.0.8
[6.0.7]: https://github.com/linkedin/rest.li/compare/v6.0.6...v6.0.7
[6.0.6]: https://github.com/linkedin/rest.li/compare/v6.0.5...v6.0.6
[6.0.5]: https://github.com/linkedin/rest.li/compare/v6.0.4...v6.0.5
[6.0.4]: https://github.com/linkedin/rest.li/compare/v6.0.3...v6.0.4
[6.0.3]: https://github.com/linkedin/rest.li/compare/v6.0.2...v6.0.3
[6.0.2]: https://github.com/linkedin/rest.li/compare/v6.0.1...v6.0.2
[6.0.1]: https://github.com/linkedin/rest.li/compare/v6.0.0...v6.0.1
[6.0.0]: https://github.com/linkedin/rest.li/compare/v5.0.20...v6.0.0
[5.0.20]: https://github.com/linkedin/rest.li/compare/v5.0.19...v5.0.20
[5.0.19]: https://github.com/linkedin/rest.li/compare/v5.0.18...v5.0.19
[5.0.18]: https://github.com/linkedin/rest.li/compare/v5.0.17...v5.0.18
[5.0.17]: https://github.com/linkedin/rest.li/compare/v5.0.16...v5.0.17
[5.0.16]: https://github.com/linkedin/rest.li/compare/v5.0.15...v5.0.16
[5.0.15]: https://github.com/linkedin/rest.li/compare/v5.0.14...v5.0.15
[5.0.14]: https://github.com/linkedin/rest.li/compare/v5.0.13...v5.0.14
[5.0.13]: https://github.com/linkedin/rest.li/compare/v5.0.12...v5.0.13
[5.0.12]: https://github.com/linkedin/rest.li/compare/v5.0.11...v5.0.12
[5.0.11]: https://github.com/linkedin/rest.li/compare/v5.0.10...v5.0.11
[5.0.10]: https://github.com/linkedin/rest.li/compare/v5.0.9...v5.0.10
[5.0.9]: https://github.com/linkedin/rest.li/compare/v5.0.8...v5.0.9
[5.0.8]: https://github.com/linkedin/rest.li/compare/v5.0.7...v5.0.8
[5.0.7]: https://github.com/linkedin/rest.li/compare/v5.0.6...v5.0.7
[5.0.6]: https://github.com/linkedin/rest.li/compare/v5.0.5...v5.0.6
[5.0.5]: https://github.com/linkedin/rest.li/compare/v5.0.4...v5.0.5
[5.0.4]: https://github.com/linkedin/rest.li/compare/v5.0.3...v5.0.4
[5.0.3]: https://github.com/linkedin/rest.li/compare/v5.0.2...v5.0.3
[5.0.2]: https://github.com/linkedin/rest.li/compare/v5.0.1...v5.0.2
[5.0.1]: https://github.com/linkedin/rest.li/compare/v5.0.0...v5.0.1
[5.0.0]: https://github.com/linkedin/rest.li/compare/v4.1.0...v5.0.0
[4.1.0]: https://github.com/linkedin/rest.li/compare/v4.0.0...v4.1.0
[4.0.0]: https://github.com/linkedin/rest.li/compare/v3.1.4...v4.0.0
[3.1.4]: https://github.com/linkedin/rest.li/compare/v3.1.3...v3.1.4
[3.1.3]: https://github.com/linkedin/rest.li/compare/v3.1.2...v3.1.3
[3.1.2]: https://github.com/linkedin/rest.li/compare/v3.1.1...v3.1.2
[3.1.1]: https://github.com/linkedin/rest.li/compare/v3.1.0...v3.1.1
[3.1.0]: https://github.com/linkedin/rest.li/compare/v3.0.2...v3.1.0
[3.0.2]: https://github.com/linkedin/rest.li/compare/v3.0.1...v3.0.2
[3.0.1]: https://github.com/linkedin/rest.li/compare/v3.0.0...v3.0.1
[3.0.0]: https://github.com/linkedin/rest.li/compare/v2.12.7...v3.0.0
[2.12.7]: https://github.com/linkedin/rest.li/compare/v2.12.6...v2.12.7
[2.12.6]: https://github.com/linkedin/rest.li/compare/v2.12.5...v2.12.6
[2.12.5]: https://github.com/linkedin/rest.li/compare/v2.12.4...v2.12.5
[2.12.4]: https://github.com/linkedin/rest.li/compare/v2.12.3...v2.12.4
[2.12.3]: https://github.com/linkedin/rest.li/compare/v3.0.0...v2.12.3
[3.0.0]: https://github.com/linkedin/rest.li/compare/v2.12.1...v3.0.0
[2.12.1]: https://github.com/linkedin/rest.li/compare/v2.12.0...v2.12.1
[2.12.0]: https://github.com/linkedin/rest.li/compare/v2.11.3...v2.12.0
[2.11.3]: https://github.com/linkedin/rest.li/compare/v2.11.2...v2.11.3
[2.11.2]: https://github.com/linkedin/rest.li/compare/v2.11.1...v2.11.2
[2.11.1]: https://github.com/linkedin/rest.li/compare/v2.11.0...v2.11.1
[2.11.0]: https://github.com/linkedin/rest.li/compare/v2.10.19...v2.11.0
[2.10.19]: https://github.com/linkedin/rest.li/compare/v2.10.18...v2.10.19
[2.10.18]: https://github.com/linkedin/rest.li/compare/v2.10.17...v2.10.18
[2.10.17]: https://github.com/linkedin/rest.li/compare/v2.10.16...v2.10.17
[2.10.16]: https://github.com/linkedin/rest.li/compare/v2.10.15...v2.10.16
[2.10.15]: https://github.com/linkedin/rest.li/compare/v2.10.14...v2.10.15
[2.10.14]: https://github.com/linkedin/rest.li/compare/v2.10.13...v2.10.14
[2.10.13]: https://github.com/linkedin/rest.li/compare/v2.10.10...v2.10.13
[2.10.10]: https://github.com/linkedin/rest.li/compare/v2.10.9...v2.10.10
[2.10.9]: https://github.com/linkedin/rest.li/compare/v2.10.8...v2.10.9
[2.10.8]: https://github.com/linkedin/rest.li/compare/v2.10.7...v2.10.8
[2.10.7]: https://github.com/linkedin/rest.li/compare/v2.10.6...v2.10.7
[2.10.6]: https://github.com/linkedin/rest.li/compare/v2.10.5...v2.10.6
[2.10.5]: https://github.com/linkedin/rest.li/compare/v2.10.4...v2.10.5
[2.10.4]: https://github.com/linkedin/rest.li/compare/v2.10.3...v2.10.4
[2.10.3]: https://github.com/linkedin/rest.li/compare/v2.10.2...v2.10.3
[2.10.2]: https://github.com/linkedin/rest.li/compare/v2.10.1...v2.10.2
[2.10.1]: https://github.com/linkedin/rest.li/compare/v2.10.0...v2.10.1
[2.10.0]: https://github.com/linkedin/rest.li/compare/v2.9.1...v2.10.0
[2.9.1]: https://github.com/linkedin/rest.li/compare/v2.9.0...v2.9.1
[2.9.0]: https://github.com/linkedin/rest.li/compare/v2.8.0...v2.9.0
[2.8.0]: https://github.com/linkedin/rest.li/compare/v2.7.0...v2.8.0
[2.7.0]: https://github.com/linkedin/rest.li/compare/v2.6.3...v2.7.0
[2.6.3]: https://github.com/linkedin/rest.li/compare/v2.6.2...v2.6.3
[2.6.2]: https://github.com/linkedin/rest.li/compare/v2.6.1...v2.6.2
[2.6.1]: https://github.com/linkedin/rest.li/compare/v2.6.0...v2.6.1
[2.6.0]: https://github.com/linkedin/rest.li/compare/v2.5.1...v2.6.0
[2.5.1]: https://github.com/linkedin/rest.li/compare/v2.5.0...v2.5.1
[2.5.0]: https://github.com/linkedin/rest.li/compare/v2.4.4...v2.5.0
[2.4.4]: https://github.com/linkedin/rest.li/compare/v2.4.3...v2.4.4
[2.4.3]: https://github.com/linkedin/rest.li/compare/v2.4.2...v2.4.3
[2.4.2]: https://github.com/linkedin/rest.li/compare/v2.4.1...v2.4.2
[2.4.1]: https://github.com/linkedin/rest.li/compare/v2.4.0...v2.4.1
[2.4.0]: https://github.com/linkedin/rest.li/compare/v2.3.0...v2.4.0
[2.3.0]: https://github.com/linkedin/rest.li/compare/v2.2.11...v2.3.0
[2.2.11]: https://github.com/linkedin/rest.li/compare/v2.2.10...v2.2.11
[2.2.10]: https://github.com/linkedin/rest.li/compare/v2.2.9...v2.2.10
[2.2.9]: https://github.com/linkedin/rest.li/compare/v2.2.8...v2.2.9
[2.2.8]: https://github.com/linkedin/rest.li/compare/v2.2.7...v2.2.8
[2.2.7]: https://github.com/linkedin/rest.li/compare/v2.2.6...v2.2.7
[2.2.6]: https://github.com/linkedin/rest.li/compare/v2.2.5...v2.2.6
[2.2.5]: https://github.com/linkedin/rest.li/compare/v2.2.4...v2.2.5
[2.2.4]: https://github.com/linkedin/rest.li/compare/v2.2.3...v2.2.4
[2.2.3]: https://github.com/linkedin/rest.li/compare/v2.2.2...v2.2.3
[2.2.2]: https://github.com/linkedin/rest.li/compare/v2.2.1...v2.2.2
[2.2.1]: https://github.com/linkedin/rest.li/compare/v2.2.0...v2.2.1
[2.2.0]: https://github.com/linkedin/rest.li/compare/v2.1.2...v2.2.0
[2.1.2]: https://github.com/linkedin/rest.li/compare/v2.1.1...v2.1.2
[2.1.1]: https://github.com/linkedin/rest.li/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/linkedin/rest.li/compare/v2.0.5...v2.1.0
[2.0.5]: https://github.com/linkedin/rest.li/compare/v2.0.4...v2.0.5
[2.0.4]: https://github.com/linkedin/rest.li/compare/v2.0.3...v2.0.4
[2.0.3]: https://github.com/linkedin/rest.li/compare/v2.0.2...v2.0.3
[2.0.2]: https://github.com/linkedin/rest.li/compare/v2.0.1...v2.0.2
[2.0.1]: https://github.com/linkedin/rest.li/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/linkedin/rest.li/compare/v1.24.8...v2.0.0
[1.24.8]: https://github.com/linkedin/rest.li/compare/v1.24.7...v1.24.8
[1.24.7]: https://github.com/linkedin/rest.li/compare/v1.24.6...v1.24.7
[1.24.6]: https://github.com/linkedin/rest.li/compare/v1.24.5...v1.24.6
[1.24.5]: https://github.com/linkedin/rest.li/compare/v1.24.4...v1.24.5
[1.24.4]: https://github.com/linkedin/rest.li/compare/v1.24.3...v1.24.4
[1.24.3]: https://github.com/linkedin/rest.li/compare/v1.24.2...v1.24.3
[1.24.2]: https://github.com/linkedin/rest.li/compare/v1.24.1...v1.24.2
[1.24.1]: https://github.com/linkedin/rest.li/compare/v1.24.0...v1.24.1
[1.24.0]: https://github.com/linkedin/rest.li/compare/v1.23.8...v1.24.0
[1.23.8]: https://github.com/linkedin/rest.li/compare/v1.23.7...v1.23.8
[1.23.7]: https://github.com/linkedin/rest.li/compare/v1.23.6...v1.23.7
[1.23.6]: https://github.com/linkedin/rest.li/compare/v1.23.5...v1.23.6
[1.23.5]: https://github.com/linkedin/rest.li/compare/v1.23.4...v1.23.5
[1.23.4]: https://github.com/linkedin/rest.li/compare/v1.23.3...v1.23.4
[1.23.3]: https://github.com/linkedin/rest.li/compare/v1.23.2...v1.23.3
[1.23.2]: https://github.com/linkedin/rest.li/compare/v1.23.1...v1.23.2
[1.23.1]: https://github.com/linkedin/rest.li/compare/v1.23.0...v1.23.1
[1.23.0]: https://github.com/linkedin/rest.li/compare/v1.22.0...v1.23.0
[1.22.0]: https://github.com/linkedin/rest.li/compare/v1.21.2...v1.22.0
[1.21.2]: https://github.com/linkedin/rest.li/compare/v1.21.1...v1.21.2
[1.21.1]: https://github.com/linkedin/rest.li/compare/v1.21.0...v1.21.1
[1.21.0]: https://github.com/linkedin/rest.li/compare/v1.20.0...v1.21.0
[1.20.0]: https://github.com/linkedin/rest.li/compare/v1.19.2...v1.20.0
[1.19.2]: https://github.com/linkedin/rest.li/compare/v1.19.1...v1.19.2
[1.19.1]: https://github.com/linkedin/rest.li/compare/v1.19.0...v1.19.1
[1.19.0]: https://github.com/linkedin/rest.li/compare/v1.18.3...v1.19.0
[1.18.3]: https://github.com/linkedin/rest.li/compare/v1.18.2...v1.18.3
[1.18.2]: https://github.com/linkedin/rest.li/compare/v1.18.1...v1.18.2
[1.18.1]: https://github.com/linkedin/rest.li/compare/v1.18.0...v1.18.1
[1.18.0]: https://github.com/linkedin/rest.li/compare/v1.17.3...v1.18.0
[1.17.3]: https://github.com/linkedin/rest.li/compare/v1.17.2...v1.17.3
[1.17.2]: https://github.com/linkedin/rest.li/compare/v1.17.1...v1.17.2
[1.17.1]: https://github.com/linkedin/rest.li/compare/v1.17.0...v1.17.1
[1.17.0]: https://github.com/linkedin/rest.li/compare/v1.16.2...v1.17.0
[1.16.2]: https://github.com/linkedin/rest.li/compare/v1.16.1...v1.16.2
[1.16.1]: https://github.com/linkedin/rest.li/compare/v1.16.0...v1.16.1
[1.16.0]: https://github.com/linkedin/rest.li/compare/v1.15.24...v1.16.0
[1.15.24]: https://github.com/linkedin/rest.li/compare/v1.15.23...v1.15.24
[1.15.23]: https://github.com/linkedin/rest.li/compare/v1.15.22...v1.15.23
[1.15.22]: https://github.com/linkedin/rest.li/compare/v1.15.21...v1.15.22
[1.15.21]: https://github.com/linkedin/rest.li/compare/v1.15.20...v1.15.21
[1.15.20]: https://github.com/linkedin/rest.li/compare/v1.15.19...v1.15.20
[1.15.19]: https://github.com/linkedin/rest.li/compare/v1.15.18...v1.15.19
[1.15.18]: https://github.com/linkedin/rest.li/compare/v1.15.17...v1.15.18
[1.15.17]: https://github.com/linkedin/rest.li/compare/v1.15.16...v1.15.17
[1.15.16]: https://github.com/linkedin/rest.li/compare/v1.15.15...v1.15.16
[1.15.15]: https://github.com/linkedin/rest.li/compare/v1.15.14...v1.15.15
[1.15.14]: https://github.com/linkedin/rest.li/compare/v1.15.13...v1.15.14
[1.15.13]: https://github.com/linkedin/rest.li/compare/v1.15.12...v1.15.13
[1.15.12]: https://github.com/linkedin/rest.li/compare/v1.15.11...v1.15.12
[1.15.11]: https://github.com/linkedin/rest.li/compare/v1.15.10...v1.15.11
[1.15.10]: https://github.com/linkedin/rest.li/compare/v1.15.9...v1.15.10
[1.15.9]: https://github.com/linkedin/rest.li/compare/v1.15.8...v1.15.9
[1.15.8]: https://github.com/linkedin/rest.li/compare/v1.15.7...v1.15.8
[1.15.7]: https://github.com/linkedin/rest.li/compare/v1.15.6...v1.15.7
[1.15.6]: https://github.com/linkedin/rest.li/compare/v1.15.5...v1.15.6
[1.15.5]: https://github.com/linkedin/rest.li/compare/v1.15.4...v1.15.5
[1.15.4]: https://github.com/linkedin/rest.li/compare/v1.15.3...v1.15.4
[1.15.3]: https://github.com/linkedin/rest.li/compare/v1.15.2...v1.15.3
[1.15.2]: https://github.com/linkedin/rest.li/compare/v1.15.1...v1.15.2
[1.15.1]: https://github.com/linkedin/rest.li/compare/v1.15.0...v1.15.1
[1.15.0]: https://github.com/linkedin/rest.li/compare/v1.14.7...v1.15.0
[1.14.7]: https://github.com/linkedin/rest.li/compare/v1.14.6...v1.14.7
[1.14.6]: https://github.com/linkedin/rest.li/compare/v1.14.5...v1.14.6
[1.14.5]: https://github.com/linkedin/rest.li/compare/v1.14.4...v1.14.5
[1.14.4]: https://github.com/linkedin/rest.li/compare/v1.14.3...v1.14.4
[1.14.3]: https://github.com/linkedin/rest.li/compare/v1.14.2...v1.14.3
[1.14.2]: https://github.com/linkedin/rest.li/compare/v1.14.1...v1.14.2
[1.14.1]: https://github.com/linkedin/rest.li/compare/v1.14.0...v1.14.1
[1.14.0]: https://github.com/linkedin/rest.li/compare/v1.13.5...v1.14.0
[1.13.5]: https://github.com/linkedin/rest.li/compare/v1.13.4...v1.13.5
[1.13.4]: https://github.com/linkedin/rest.li/compare/v1.13.3...v1.13.4
[1.13.3]: https://github.com/linkedin/rest.li/compare/v1.13.2...v1.13.3
[1.13.2]: https://github.com/linkedin/rest.li/compare/v1.13.1...v1.13.2
[1.13.1]: https://github.com/linkedin/rest.li/compare/v1.13.0...v1.13.1
[1.13.0]: https://github.com/linkedin/rest.li/compare/v1.12.4...v1.13.0
[1.12.4]: https://github.com/linkedin/rest.li/compare/v1.12.3...v1.12.4
[1.12.3]: https://github.com/linkedin/rest.li/compare/v1.12.2...v1.12.3
[1.12.2]: https://github.com/linkedin/rest.li/compare/v1.12.1...v1.12.2
[1.12.1]: https://github.com/linkedin/rest.li/compare/v1.12.0...v1.12.1
[1.12.0]: https://github.com/linkedin/rest.li/compare/v1.11.2...v1.12.0
[1.11.2]: https://github.com/linkedin/rest.li/compare/v1.11.1...v1.11.2
[1.11.1]: https://github.com/linkedin/rest.li/compare/v1.11.0...v1.11.1
[1.11.0]: https://github.com/linkedin/rest.li/compare/v1.10.7...v1.11.0
[1.10.7]: https://github.com/linkedin/rest.li/compare/v1.10.6...v1.10.7
[1.10.6]: https://github.com/linkedin/rest.li/compare/v1.10.5...v1.10.6
[1.10.5]: https://github.com/linkedin/rest.li/compare/v1.10.4...v1.10.5
[1.10.4]: https://github.com/linkedin/rest.li/compare/v1.10.3...v1.10.4
[1.10.3]: https://github.com/linkedin/rest.li/compare/v1.10.2...v1.10.3
[1.10.2]: https://github.com/linkedin/rest.li/compare/v1.10.1...v1.10.2
[1.10.1]: https://github.com/linkedin/rest.li/compare/v1.10.0...v1.10.1
[1.10.0]: https://github.com/linkedin/rest.li/compare/v1.9.49...v1.10.0
[1.9.49]: https://github.com/linkedin/rest.li/compare/v1.9.48...v1.9.49
[1.9.48]: https://github.com/linkedin/rest.li/compare/v1.9.47...v1.9.48
[1.9.47]: https://github.com/linkedin/rest.li/compare/v1.9.46...v1.9.47
[1.9.46]: https://github.com/linkedin/rest.li/compare/v1.9.45...v1.9.46
[1.9.45]: https://github.com/linkedin/rest.li/compare/v1.9.44...v1.9.45
[1.9.44]: https://github.com/linkedin/rest.li/compare/v1.9.43...v1.9.44
[1.9.43]: https://github.com/linkedin/rest.li/compare/v1.9.42...v1.9.43
[1.9.42]: https://github.com/linkedin/rest.li/compare/v1.9.41...v1.9.42
[1.9.41]: https://github.com/linkedin/rest.li/compare/v1.9.40...v1.9.41
[1.9.40]: https://github.com/linkedin/rest.li/compare/v1.9.39...v1.9.40
[1.9.39]: https://github.com/linkedin/rest.li/compare/v1.9.38...v1.9.39
[1.9.38]: https://github.com/linkedin/rest.li/compare/v1.9.37...v1.9.38
[1.9.37]: https://github.com/linkedin/rest.li/compare/v1.9.36...v1.9.37
[1.9.36]: https://github.com/linkedin/rest.li/compare/v1.9.35...v1.9.36
[1.9.35]: https://github.com/linkedin/rest.li/compare/v1.9.34...v1.9.35
[1.9.34]: https://github.com/linkedin/rest.li/compare/v1.9.33...v1.9.34
[1.9.33]: https://github.com/linkedin/rest.li/compare/v1.9.32...v1.9.33
[1.9.32]: https://github.com/linkedin/rest.li/compare/v1.9.31...v1.9.32
[1.9.31]: https://github.com/linkedin/rest.li/compare/v1.9.30...v1.9.31
[1.9.30]: https://github.com/linkedin/rest.li/compare/v1.9.29...v1.9.30
[1.9.29]: https://github.com/linkedin/rest.li/compare/v1.9.28...v1.9.29
[1.9.28]: https://github.com/linkedin/rest.li/compare/v1.9.27...v1.9.28
[1.9.27]: https://github.com/linkedin/rest.li/compare/v1.9.26...v1.9.27
[1.9.26]: https://github.com/linkedin/rest.li/compare/v1.9.25...v1.9.26
[1.9.25]: https://github.com/linkedin/rest.li/compare/v1.9.24...v1.9.25
[1.9.24]: https://github.com/linkedin/rest.li/compare/v1.9.23...v1.9.24
[1.9.23]: https://github.com/linkedin/rest.li/compare/v1.9.22...v1.9.23
[1.9.22]: https://github.com/linkedin/rest.li/compare/v1.9.21...v1.9.22
[1.9.21]: https://github.com/linkedin/rest.li/compare/v1.9.20...v1.9.21
[1.9.20]: https://github.com/linkedin/rest.li/compare/v1.9.19...v1.9.20
[1.9.19]: https://github.com/linkedin/rest.li/compare/v1.9.18...v1.9.19
[1.9.18]: https://github.com/linkedin/rest.li/compare/v1.9.17...v1.9.18
[1.9.17]: https://github.com/linkedin/rest.li/compare/v1.9.16...v1.9.17
[1.9.16]: https://github.com/linkedin/rest.li/compare/v1.9.15...v1.9.16
[1.9.15]: https://github.com/linkedin/rest.li/compare/v1.9.14...v1.9.15
[1.9.14]: https://github.com/linkedin/rest.li/compare/v1.9.13...v1.9.14
[1.9.13]: https://github.com/linkedin/rest.li/compare/v1.9.12...v1.9.13
[1.9.12]: https://github.com/linkedin/rest.li/compare/v1.9.11...v1.9.12
[1.9.11]: https://github.com/linkedin/rest.li/compare/v1.9.10...v1.9.11
[1.9.10]: https://github.com/linkedin/rest.li/compare/v1.9.9...v1.9.10
[1.9.9]: https://github.com/linkedin/rest.li/compare/v1.9.8...v1.9.9
[1.9.8]: https://github.com/linkedin/rest.li/compare/v1.9.7...v1.9.8
[1.9.7]: https://github.com/linkedin/rest.li/compare/v1.9.6...v1.9.7
[1.9.6]: https://github.com/linkedin/rest.li/compare/v1.9.5...v1.9.6
[1.9.5]: https://github.com/linkedin/rest.li/compare/v1.9.4...v1.9.5
[1.9.4]: https://github.com/linkedin/rest.li/compare/v1.9.3...v1.9.4
[1.9.3]: https://github.com/linkedin/rest.li/compare/v1.9.2...v1.9.3
[1.9.2]: https://github.com/linkedin/rest.li/compare/v1.9.1...v1.9.2
[1.9.1]: https://github.com/linkedin/rest.li/compare/v1.9.0...v1.9.1
[1.9.0]: https://github.com/linkedin/rest.li/compare/v1.8.39...v1.9.0
[1.8.39]: https://github.com/linkedin/rest.li/compare/v1.8.38...v1.8.39
[1.8.38]: https://github.com/linkedin/rest.li/compare/v1.8.37...v1.8.38
[1.8.37]: https://github.com/linkedin/rest.li/compare/v1.8.36...v1.8.37
[1.8.36]: https://github.com/linkedin/rest.li/compare/v1.8.35...v1.8.36
[1.8.35]: https://github.com/linkedin/rest.li/compare/v1.8.34...v1.8.35
[1.8.34]: https://github.com/linkedin/rest.li/compare/v1.8.33...v1.8.34
[1.8.33]: https://github.com/linkedin/rest.li/compare/v1.8.32...v1.8.33
[1.8.32]: https://github.com/linkedin/rest.li/compare/v1.8.31...v1.8.32
[1.8.31]: https://github.com/linkedin/rest.li/compare/v1.8.30...v1.8.31
[1.8.30]: https://github.com/linkedin/rest.li/compare/v1.8.29...v1.8.30
[1.8.29]: https://github.com/linkedin/rest.li/compare/v1.8.28...v1.8.29
[1.8.28]: https://github.com/linkedin/rest.li/compare/v1.8.27...v1.8.28
[1.8.27]: https://github.com/linkedin/rest.li/compare/v1.8.26...v1.8.27
[1.8.26]: https://github.com/linkedin/rest.li/compare/v1.8.25...v1.8.26
[1.8.25]: https://github.com/linkedin/rest.li/compare/v1.8.23...v1.8.25
[1.8.23]: https://github.com/linkedin/rest.li/compare/v1.8.22...v1.8.23
[1.8.22]: https://github.com/linkedin/rest.li/compare/v1.8.21...v1.8.22
[1.8.21]: https://github.com/linkedin/rest.li/compare/v1.8.20...v1.8.21
[1.8.20]: https://github.com/linkedin/rest.li/compare/v1.8.19...v1.8.20
[1.8.19]: https://github.com/linkedin/rest.li/compare/v1.8.18...v1.8.19
[1.8.18]: https://github.com/linkedin/rest.li/compare/v1.8.17...v1.8.18
[1.8.17]: https://github.com/linkedin/rest.li/compare/v1.8.16...v1.8.17
[1.8.16]: https://github.com/linkedin/rest.li/compare/v1.8.15...v1.8.16
[1.8.15]: https://github.com/linkedin/rest.li/compare/v1.8.14...v1.8.15
[1.8.14]: https://github.com/linkedin/rest.li/compare/v1.8.13...v1.8.14
[1.8.13]: https://github.com/linkedin/rest.li/compare/v1.8.12...v1.8.13
[1.8.12]: https://github.com/linkedin/rest.li/compare/v1.8.11...v1.8.12
[1.8.11]: https://github.com/linkedin/rest.li/compare/v1.8.10...v1.8.11
[1.8.10]: https://github.com/linkedin/rest.li/compare/v1.8.9...v1.8.10
[1.8.9]: https://github.com/linkedin/rest.li/compare/v1.8.8...v1.8.9
[1.8.8]: https://github.com/linkedin/rest.li/compare/v1.8.7...v1.8.8
[1.8.7]: https://github.com/linkedin/rest.li/compare/v1.8.6...v1.8.7
[1.8.6]: https://github.com/linkedin/rest.li/compare/v1.8.5...v1.8.6
[1.8.5]: https://github.com/linkedin/rest.li/compare/v1.8.4...v1.8.5
[1.8.4]: https://github.com/linkedin/rest.li/compare/v1.8.3...v1.8.4
[1.8.3]: https://github.com/linkedin/rest.li/compare/v1.8.2...v1.8.3
[1.8.2]: https://github.com/linkedin/rest.li/compare/v1.8.1...v1.8.2
[1.8.1]: https://github.com/linkedin/rest.li/compare/v1.8.0...v1.8.1
[1.8.0]: https://github.com/linkedin/rest.li/compare/v1.7.12...v1.8.0
[1.7.12]: https://github.com/linkedin/rest.li/compare/v1.7.11...v1.7.12
[1.7.11]: https://github.com/linkedin/rest.li/compare/v1.7.10...v1.7.11
[1.7.10]: https://github.com/linkedin/rest.li/compare/v1.7.9...v1.7.10
[1.7.9]: https://github.com/linkedin/rest.li/compare/v1.7.8...v1.7.9
[1.7.8]: https://github.com/linkedin/rest.li/compare/v1.7.7...v1.7.8
[1.7.7]: https://github.com/linkedin/rest.li/compare/v1.7.6...v1.7.7
[1.7.6]: https://github.com/linkedin/rest.li/compare/v1.7.5...v1.7.6
[1.7.5]: https://github.com/linkedin/rest.li/compare/v1.7.4...v1.7.5
[1.7.4]: https://github.com/linkedin/rest.li/compare/v1.7.3...v1.7.4
[1.7.3]: https://github.com/linkedin/rest.li/compare/v1.7.2...v1.7.3
[1.7.2]: https://github.com/linkedin/rest.li/compare/v1.7.1...v1.7.2
[1.7.1]: https://github.com/linkedin/rest.li/compare/v1.7.0...v1.7.1
[1.7.0]: https://github.com/linkedin/rest.li/compare/v1.6.14...v1.7.0
[1.6.14]: https://github.com/linkedin/rest.li/compare/v1.6.12...v1.6.14
[1.6.12]: https://github.com/linkedin/rest.li/compare/v1.6.11...v1.6.12
[1.6.11]: https://github.com/linkedin/rest.li/compare/v1.6.10...v1.6.11
[1.6.10]: https://github.com/linkedin/rest.li/compare/v1.6.9...v1.6.10
[1.6.9]: https://github.com/linkedin/rest.li/compare/v1.6.8...v1.6.9
[1.6.8]: https://github.com/linkedin/rest.li/compare/v1.6.7...v1.6.8
[1.6.7]: https://github.com/linkedin/rest.li/compare/v1.6.6...v1.6.7
[1.6.6]: https://github.com/linkedin/rest.li/compare/v1.6.5...v1.6.6
[1.6.5]: https://github.com/linkedin/rest.li/compare/v1.6.4...v1.6.5
[1.6.4]: https://github.com/linkedin/rest.li/compare/v1.6.3...v1.6.4
[1.6.3]: https://github.com/linkedin/rest.li/compare/v1.6.2...v1.6.3
[1.6.2]: https://github.com/linkedin/rest.li/compare/v1.6.1...v1.6.2
[1.6.1]: https://github.com/linkedin/rest.li/compare/v1.6.0...v1.6.1
[1.6.0]: https://github.com/linkedin/rest.li/compare/v1.5.12...v1.6.0
[1.5.12]: https://github.com/linkedin/rest.li/compare/v1.5.11...v1.5.12
[1.5.11]: https://github.com/linkedin/rest.li/compare/v1.5.10...v1.5.11
[1.5.10]: https://github.com/linkedin/rest.li/compare/v1.5.9...v1.5.10
[1.5.9]: https://github.com/linkedin/rest.li/compare/v1.5.8...v1.5.9
[1.5.8]: https://github.com/linkedin/rest.li/compare/v1.5.7...v1.5.8
[1.5.7]: https://github.com/linkedin/rest.li/compare/v1.5.6...v1.5.7
[1.5.6]: https://github.com/linkedin/rest.li/compare/v1.5.5...v1.5.6
[1.5.5]: https://github.com/linkedin/rest.li/compare/v1.5.4...v1.5.5
[1.5.4]: https://github.com/linkedin/rest.li/compare/v1.5.3...v1.5.4
[1.5.3]: https://github.com/linkedin/rest.li/compare/v1.5.2...v1.5.3
[1.5.2]: https://github.com/linkedin/rest.li/compare/v1.5.1...v1.5.2
[1.5.1]: https://github.com/linkedin/rest.li/compare/v1.5.0...v1.5.1
[1.5.0]: https://github.com/linkedin/rest.li/compare/v1.4.1...v1.5.0
[1.4.1]: https://github.com/linkedin/rest.li/compare/v1.4.0...v1.4.1
[1.4.0]: https://github.com/linkedin/rest.li/compare/v1.3.5...v1.4.0
[1.3.5]: https://github.com/linkedin/rest.li/compare/v1.3.4...v1.3.5
[1.3.4]: https://github.com/linkedin/rest.li/compare/v1.3.3...v1.3.4
[1.3.3]: https://github.com/linkedin/rest.li/compare/v1.3.2...v1.3.3
[1.3.2]: https://github.com/linkedin/rest.li/compare/v1.3.1...v1.3.2
[1.3.1]: https://github.com/linkedin/rest.li/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/linkedin/rest.li/compare/v1.2.5...v1.3.0
[1.2.5]: https://github.com/linkedin/rest.li/compare/v1.2.4...v1.2.5
[1.2.4]: https://github.com/linkedin/rest.li/compare/v1.2.3...v1.2.4
[1.2.3]: https://github.com/linkedin/rest.li/compare/v1.2.2...v1.2.3
[1.2.2]: https://github.com/linkedin/rest.li/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/linkedin/rest.li/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/linkedin/rest.li/compare/v1.1.8...v1.2.0
[1.1.8]: https://github.com/linkedin/rest.li/compare/v1.1.7...v1.1.8
[1.1.7]: https://github.com/linkedin/rest.li/compare/v1.1.6...v1.1.7
[1.1.6]: https://github.com/linkedin/rest.li/compare/v1.1.5...v1.1.6
[1.1.5]: https://github.com/linkedin/rest.li/compare/v1.1.4...v1.1.5
[1.1.4]: https://github.com/linkedin/rest.li/compare/v1.1.2...v1.1.4
[1.1.2]: https://github.com/linkedin/rest.li/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/linkedin/rest.li/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/linkedin/rest.li/compare/v1.0.5...v1.1.0
[1.0.5]: https://github.com/linkedin/rest.li/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/linkedin/rest.li/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/linkedin/rest.li/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/linkedin/rest.li/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/linkedin/rest.li/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/linkedin/rest.li/compare/v0.22.3...v1.0.0
[0.22.3]: https://github.com/linkedin/rest.li/compare/v0.22.2...v0.22.3
[0.22.2]: https://github.com/linkedin/rest.li/compare/v0.22.1...v0.22.2
[0.22.1]: https://github.com/linkedin/rest.li/compare/v0.22.0...v0.22.1
[0.22.0]: https://github.com/linkedin/rest.li/compare/v0.21.2...v0.22.0
[0.21.2]: https://github.com/linkedin/rest.li/compare/v0.21.1...v0.21.2
[0.21.1]: https://github.com/linkedin/rest.li/compare/v0.21.0...v0.21.1
[0.21.0]: https://github.com/linkedin/rest.li/compare/v0.20.6...v0.21.0
[0.20.6]: https://github.com/linkedin/rest.li/compare/v0.20.5...v0.20.6
[0.20.5]: https://github.com/linkedin/rest.li/compare/v0.20.4...v0.20.5
[0.20.4]: https://github.com/linkedin/rest.li/compare/v0.20.3...v0.20.4
[0.20.3]: https://github.com/linkedin/rest.li/compare/v0.20.2...v0.20.3
[0.20.2]: https://github.com/linkedin/rest.li/compare/v0.20.1...v0.20.2
[0.20.1]: https://github.com/linkedin/rest.li/compare/v0.20.0...v0.20.1
[0.20.0]: https://github.com/linkedin/rest.li/compare/v0.19.7...v0.20.0
[0.19.7]: https://github.com/linkedin/rest.li/compare/v0.19.6...v0.19.7
[0.19.6]: https://github.com/linkedin/rest.li/compare/v0.19.5...v0.19.6
[0.19.5]: https://github.com/linkedin/rest.li/compare/v0.19.4...v0.19.5
[0.19.4]: https://github.com/linkedin/rest.li/compare/v0.19.3...v0.19.4
[0.19.3]: https://github.com/linkedin/rest.li/compare/v0.19.2...v0.19.3
[0.19.2]: https://github.com/linkedin/rest.li/compare/v0.19.1...v0.19.2
[0.19.1]: https://github.com/linkedin/rest.li/compare/v0.19.0...v0.19.1
[0.19.0]: https://github.com/linkedin/rest.li/compare/v0.18.7...v0.19.0
[0.18.7]: https://github.com/linkedin/rest.li/compare/v0.18.6...v0.18.7
[0.18.6]: https://github.com/linkedin/rest.li/compare/v0.18.5...v0.18.6
[0.18.5]: https://github.com/linkedin/rest.li/compare/v0.18.4...v0.18.5
[0.18.4]: https://github.com/linkedin/rest.li/compare/v0.18.3...v0.18.4
[0.18.3]: https://github.com/linkedin/rest.li/compare/v0.18.2...v0.18.3
[0.18.2]: https://github.com/linkedin/rest.li/compare/v0.18.1...v0.18.2
[0.18.1]: https://github.com/linkedin/rest.li/compare/v0.18.0...v0.18.1
[0.18.0]: https://github.com/linkedin/rest.li/compare/v0.17.6...v0.18.0
[0.17.6]: https://github.com/linkedin/rest.li/compare/v0.17.5...v0.17.6
[0.17.5]: https://github.com/linkedin/rest.li/compare/v0.17.4...v0.17.5
[0.17.4]: https://github.com/linkedin/rest.li/compare/v0.17.3...v0.17.4
[0.17.3]: https://github.com/linkedin/rest.li/compare/v0.17.2...v0.17.3
[0.17.2]: https://github.com/linkedin/rest.li/compare/v0.17.1...v0.17.2
[0.17.1]: https://github.com/linkedin/rest.li/compare/v0.17.0...v0.17.1
[0.17.0]: https://github.com/linkedin/rest.li/compare/v0.16.5...v0.17.0
[0.16.5]: https://github.com/linkedin/rest.li/compare/v0.16.4...v0.16.5
[0.16.4]: https://github.com/linkedin/rest.li/compare/v0.16.3...v0.16.4
[0.16.3]: https://github.com/linkedin/rest.li/compare/v0.16.2...v0.16.3
[0.16.2]: https://github.com/linkedin/rest.li/compare/v0.16.1...v0.16.2
[0.16.1]: https://github.com/linkedin/rest.li/compare/v0.15.4...v0.16.1
[0.15.4]: https://github.com/linkedin/rest.li/compare/v0.15.3...v0.15.4
[0.15.3]: https://github.com/linkedin/rest.li/compare/v0.15.2...v0.15.3
[0.15.2]: https://github.com/linkedin/rest.li/compare/v0.15.1...v0.15.2
[0.15.1]: https://github.com/linkedin/rest.li/compare/v0.14.7...v0.15.1
[0.14.7]: https://github.com/linkedin/rest.li/compare/v0.14.6...v0.14.7
[0.14.6]: https://github.com/linkedin/rest.li/compare/v0.14.5...v0.14.6
[0.14.5]: https://github.com/linkedin/rest.li/compare/v0.14.4...v0.14.5
[0.14.4]: https://github.com/linkedin/rest.li/compare/v0.14.3...v0.14.4
[0.14.3]: https://github.com/linkedin/rest.li/compare/v0.14.2...v0.14.3
[0.14.2]: https://github.com/linkedin/rest.li/compare/v0.14.1...v0.14.2
[0.14.1]: https://github.com/linkedin/rest.li/tree/v0.14.1

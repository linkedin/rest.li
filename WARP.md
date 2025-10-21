# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Rest.li is LinkedIn's open-source REST framework for building robust, scalable RESTful architectures using type-safe bindings and asynchronous, non-blocking IO. **Note**: LinkedIn is migrating from Rest.li to gRPC and this repository is being deprecated.

## Build & Development Commands

### Essential Commands
```bash
# Build the entire project
./gradlew build

# Clean build
./gradlew clean build

# Run all unit tests
./gradlew test

# Run async tests  
./gradlew asyncTests

# Run integration tests
./gradlew integrationTests

# Run D2 integration tests
./gradlew d2integrationTests

# Run all tests (including integration)
./gradlew allTests

# Build specific module
./gradlew :restli-server:build
./gradlew :d2:test

# Generate code documentation
./gradlew javadoc
```

### Testing Commands by Category
- **Unit tests**: `./gradlew test` (excludes integration, known issues)
- **Integration tests**: `./gradlew integrationTests` 
- **All functional tests**: `./gradlew allTests`
- **Known failing tests**: `./gradlew knownIssuesTests`
- **Not implemented features**: `./gradlew notImplementedFeaturesTests`

### Specialized Tasks
```bash
# Test specific modules
./gradlew :restli-client:test
./gradlew :d2:integrationTests

# Skip tests for specific subprojects
./gradlew build -Ppegasus.skipTestsForSubprojects=module1,module2

# Create directories (utility task)
./gradlew makedir -Ddirpath=path/to/directory
```

## Architecture Overview

Rest.li follows a multi-module architecture with distinct layers:

### Core Framework Components

**Server-Side**:
- `restli-server` - Core REST server implementation and resource handling
- `restli-common` - Shared components between client and server
- `restli-tools` - Code generation and development tools

**Client-Side**:
- `restli-client` - Type-safe client generation and request building
- `restli-client-parseq` - ParSeq integration for asynchronous clients

**Data Layer**:
- `data` - Pegasus data binding framework (JSON/Avro serialization)
- `data-avro*` - Avro integration and schema evolution
- `generator` - Code generation from PDL schemas

### D2 (Dynamic Discovery) Service Discovery
- `d2` - Core dynamic discovery and load balancing
- `d2-schemas` - Service and cluster configuration schemas  
- `d2-contrib` - Additional D2 utilities and integrations

### R2 Transport Layer
- `r2-core` - Core transport abstraction
- `r2-netty` - Netty-based transport implementation
- `r2-jetty` - Jetty-based transport implementation

### Key Architectural Patterns

1. **Pegasus Schema-First Development**: PDL files define data schemas, generating type-safe Java classes
2. **Async-First Design**: Uses ParSeq for non-blocking operations throughout
3. **Transport Abstraction**: R2 layer provides pluggable transport implementations
4. **Service Discovery**: D2 provides client-side load balancing and service discovery
5. **Code Generation**: Extensive use of generated builders, resources, and data templates

## Development Workflow

### Working with Schemas
- PDL files in `src/main/pegasus/` define data schemas
- Running build generates corresponding Java classes in `src/mainGeneratedDataTemplate/`
- Schema evolution requires backward compatibility validation

### Resource Development
- Resources extend generated base classes with CRUD operations
- JAX-RS-style annotations define REST endpoints
- ParSeq integration enables async resource methods

### Client Development  
- Generated request builders provide type-safe client APIs
- Builders handle URI construction, parameter validation, and serialization
- RestClient provides the execution layer for requests

## Module Dependencies & Integration Points

**Critical Integration Points**:
- All modules depend on `data` for Pegasus data binding
- REST modules require `restli-common` for shared utilities
- D2-enabled applications need both `d2` and transport modules (`r2-netty`/`r2-jetty`)
- Generated code depends on `generator` and related tools during build

**Testing Infrastructure**:
- `*-testutils` modules provide testing utilities for each layer
- `restli-int-test*` modules contain comprehensive integration tests
- TestNG is the primary testing framework with custom test groups

## Common Development Patterns

### Adding New REST Resources
1. Define PDL schema in `src/main/pegasus/`
2. Create resource class extending appropriate base (CollectionResource, etc.)
3. Implement CRUD methods with proper annotations
4. Build generates client bindings automatically

### Integrating with D2
1. Configure service properties (cluster, load balancing strategy)
2. Use D2-enabled transport clients
3. Handle service discovery events for dynamic scaling

### Schema Evolution
1. Modify PDL files following backward compatibility rules
2. Run compatibility checks during build
3. Version schemas appropriately for breaking changes

## Build Configuration Notes

- **Java 8+ Required**: Minimum Java 8, targets Java 8 bytecode for compatibility
- **Gradle 6.9.4**: Uses older Gradle version with some deprecated features
- **Dependency Management**: Strict transitive dependency control with exclusions
- **Multi-Module Build**: 65+ subprojects with complex interdependencies
- **Code Generation**: Heavy use of annotation processors and custom Gradle plugins

## Testing & Quality Assurance

**Test Categories** (using TestNG groups):
- `integration` - Full workflow integration tests
- `d2integration` - D2-specific integration tests  
- `async` - Asynchronous operation tests
- `known_issue` - Known failing tests (excluded by default)
- `ci-flaky` - Tests excluded in CI due to flakiness

**Coverage Areas**:
- End-to-end request/response handling
- Schema serialization/deserialization  
- Service discovery and load balancing
- Client request building and execution
- Error handling and edge cases
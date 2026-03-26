# ShadowProxy
ShadowProxy is a Java 17 desktop web security testing proxy platform inspired by professional interception and testing suites.
## Current status
This repository currently contains Phase 1 foundation work:
- Maven-based Java 17 project with dependency management
- Swing desktop shell with Burp-style module tabs
- Netty-based proxy server with upstream HTTP forwarding, CONNECT tunneling, and initial MITM TLS interception
- Interception workflow (queue, edit, forward/drop) wired to live proxy traffic
- Real-time history table with send-to-tool actions (Repeater/Intruder/Scanner/Comparer)
- H2-backed durable request/response history store with automatic in-memory fallback
- Passive scanner engine with initial checks and issue dashboard in Scanner tab
- Core contracts for routing, persistence, certificate bootstrap, and project state
- JUnit 5 baseline tests
## Build and run
```bash
mvn clean test
mvn clean package
java -jar target/shadowproxy-0.1.0-SNAPSHOT.jar
```
## Architecture overview
- Presentation layer: Swing UI (`com.shadowproxy.ui`)
- Business logic layer: proxy/routing/certificate services (`com.shadowproxy.core`)
- Data layer: repositories and future JDBC persistence (`com.shadowproxy.persistence`)
- Domain layer: immutable records for HTTP/project data (`com.shadowproxy.domain`)
## Roadmap (mapped to `project.md`)
1. Harden MITM trust workflow (certificate install UX, passthrough controls, certificate lifecycle and imports).
2. Build interception editor with match/replace, breakpoints, and advanced tool dispatch.
3. Add mature modules (Repeater, Intruder, Scanner, Spider, Sequencer, Decoder, Comparer, Extender).
4. Add project persistence (H2/SQLite), reporting, macros/session handling, and extension runtime.
5. Harden performance, test coverage, and packaging for cross-platform distribution.
## Legal and ethical use
Use ShadowProxy only for authorized security testing on systems you own or have explicit permission to assess.

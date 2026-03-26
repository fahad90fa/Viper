You are a senior security researcher, enterprise Java architect, and full-stack developer with 15+ years of experience building professional-grade security tools. Your task is to build a complete, production-ready web application security testing proxy that works exactly like Burp Suite Professional 2025, using the exact same technology stack that PortSwigger uses for the real Burp Suite.

Technology Stack (Identical to Real Burp Suite)
Core Application
Java 17+ (LTS) as the primary language for all backend logic, proxy engine, scanner, intruder, repeater, sequencer, and core security modules
Use Maven or Gradle as the build system with full dependency management
Follow enterprise-grade Java patterns: dependency injection, clean architecture, SOLID principles, multithreading with ExecutorService, CompletableFuture for async operations
GUI Framework
Swing (javax.swing) for the entire desktop GUI — exactly like Burp Suite
Use modern Swing best practices: SwingWorker for background tasks, custom UI components, look-and-feel theming (support FlatLaf for modern dark mode)
Create custom tabbed interfaces, draggable split panes, right-click context menus, keyboard shortcuts identical to Burp
HTTP/HTTPS Engine
Java NIO (Non-blocking I/O) and Netty framework for high-performance HTTP/HTTPS proxy server
Full support for HTTP/1.1, HTTP/2, WebSockets
MITM SSL/TLS interception using BouncyCastle library for dynamic certificate generation and signing
Automatic CA certificate generation with JKS keystore management
Scanning & Security Analysis
Pure Java implementation of all vulnerability detection logic
Use OWASP dependency references where applicable
Regular expressions (java.util.regex) for pattern matching
Custom payload generation and mutation engines
Extensions & Plugin System
Java reflection API for dynamic class loading
Public Java API identical to Burp's IExtender, IBurpExtenderCallbacks, IHttpRequestResponse, IHttpService interfaces
Support BeanShell or Jython for scripting extensions in Python if users want
Data Parsing & Serialization
Jackson or Gson for JSON parsing/generation
JAXB or custom parsers for XML
Apache Commons libraries for encoding/decoding (Base64, URL, HTML entities)
JWT libraries (jjwt) for token decoding
Database & Persistence
H2 Database (embedded Java SQL database) for project storage
SQLite as an alternative lightweight option
JDBC for all database operations
Store full request/response history, site map, scan issues, project configuration
Networking & Protocol Support
Apache HttpClient or OkHttp for upstream requests
SOCKS proxy support via Java's native Proxy class
DNS resolution caching and custom DNS resolver
Threading & Concurrency
Java's ExecutorService, ThreadPoolExecutor, ForkJoinPool for managing scan threads, intruder attacks, spider workers
Locks, Semaphores, CountDownLatch for synchronization
Thread-safe collections (ConcurrentHashMap, CopyOnWriteArrayList)
Logging & Debugging
SLF4J + Logback for comprehensive logging
Configurable log levels per module
Export logs to file with rotation
Build & Packaging
Single executable JAR with embedded dependencies (fat JAR using Maven Shade or Gradle Shadow plugin)
Cross-platform: Windows (.exe wrapper via Launch4j), macOS (.app bundle), Linux (shell script)
JRE bundled option using jpackage (JDK 17+)
Complete Feature Set (Every Module Burp Suite Has)
1. Proxy Module
Full transparent MITM proxy with configurable listen address and port
Automatic browser proxy configuration instructions for Chrome, Firefox, Safari, Edge
SSL/TLS interception with auto-generated CA certificate (install once, trust forever)
Support for upstream proxy chaining and authentication
Request/response interception with pause, edit, drop, forward, send to other tools
Match and replace rules (regex-based, for headers and body)
SSL passthrough rules for specific hosts
WebSocket interception and message editing
HTTP history with full filtering, search, scope, comments, highlights, tags
2. Intercept Engine
Real-time pause on requests/responses matching user-defined rules
Breakpoints by URL pattern, HTTP method, file extension, MIME type, status code
Inline hex editor for binary data
Automatic pretty-printing for JSON, XML, URL-encoded, multipart form data
Parameter name/value extraction and inline editing
3. Target Site Map
Automatic tree-based hierarchy of all discovered hosts, directories, files, parameters
Each node shows all requests made, response codes, length, MIME type
Annotate any item with comments, severity, color highlighting
Export site map to XML/JSON
Scope definition (include/exclude rules) that affects all tools globally
4. Repeater Module
Unlimited tabs for manual request manipulation
Side-by-side request/response panes with synchronized scrolling
Full syntax highlighting and auto-formatting for all content types
Follow redirections automatically or manually
Update Content-Length automatically
Send request to Intruder, Scanner, Comparer, Extensions via context menu
Raw, Params, Headers, Hex view modes
Response rendering (HTML, JSON tree view)
5. Intruder Module
Attack types:
Sniper (one position, iterate all payloads)
Battering ram (same payload in all positions)
Pitchfork (multiple positions, parallel payload sets)
Cluster bomb (all combinations)
Payload positions marked with § § symbols
Payload types:
Simple list (from file or manual entry)
Runtime file (read fresh each iteration)
Numbers (sequential, random, hex, dates)
Character substitution
Case modification
Recursive grep (extract from previous responses)
Username generator, password lists
Payload processing:
Encoding (URL, HTML, Base64, etc.)
Hashing (MD5, SHA1, SHA256, etc.)
Add prefix/suffix
Match/replace
Attack results table:
Request number, payload, status code, error, timeout, length, response time
Grep - Match (highlight responses containing strings)
Grep - Extract (extract values using regex groups)
Sort, filter, compare responses
Configurable thread count and throttle (unlimited speed by default)
Save attack configuration and results
6. Scanner Module (Active & Passive)
Passive scanner (always running in background):
Analyzes all proxied traffic without sending additional requests
Detects: information disclosure, cleartext credentials, SQL errors in responses, stack traces, interesting headers, CORS misconfigurations, etc.
Active scanner:
Full automated crawl + audit workflow
Insertion point detection (URL parameters, body parameters, cookies, headers, path segments, JSON/XML nodes)
Vulnerability checks (100+ built-in):
SQL injection (error-based, boolean-based, time-based, union-based, stacked queries)
Cross-site scripting (reflected, stored, DOM-based)
Command injection (OS, LDAP, XPath, template injection)
Path traversal / LFI / RFI
SSRF (Server-Side Request Forgery)
XXE (XML External Entity)
Insecure deserialization
Authentication bypass
Authorization flaws (IDOR, privilege escalation)
CSRF
Clickjacking
Open redirect
Host header injection
CRLF injection
HTTP request smuggling
CORS misconfiguration
SSL/TLS vulnerabilities
Cookie security (HttpOnly, Secure, SameSite)
Sensitive data exposure
Issue reporting:
Severity (Critical, High, Medium, Low, Info)
Confidence (Certain, Firm, Tentative)
Full description, remediation advice, references (CWE, OWASP)
Evidence (exact request/response showing vulnerability)
Scan queue management (pause, resume, cancel)
Configurable scan speed and thoroughness
Export issues to HTML, XML, JSON reports
7. Spider/Crawler Module
Intelligent crawling engine that discovers:
Links in HTML (href, src, action, etc.)
JavaScript file analysis for endpoints
Form discovery and auto-submission
robots.txt, sitemap.xml parsing
Directory brute-forcing option (wordlist-based)
Parameter discovery
JavaScript rendering support (integrate lightweight headless browser or parser)
Configurable depth, scope, and exclusions
Throttle and politeness settings
8. Sequencer Module
Token/session ID entropy and randomness analysis
Tests performed:
Character-level analysis
Bit-level randomness
FIPS tests
Spectral analysis
Correlation analysis
Visual graphs and statistical summary
Import tokens from live capture or manual list
9. Decoder Module
Encoding/Decoding:
URL encoding/decoding
HTML entity encoding/decoding
Base64
Base32
Hex
ASCII hex
Octal
Binary
GZIP compression/decompression
Hashing:
MD5, SHA-1, SHA-256, SHA-512
HMAC variants
Smart decode (auto-detect encoding type)
Chain multiple encode/decode operations
10. Comparer Module
Side-by-side comparison of two requests or responses
Word-level diff (shows differences in content semantically)
Byte-level diff (shows exact binary differences with hex view)
Sync scrolling
Copy/paste comparison snippets
11. Extender / Extensions / BApp Store
Extension API (Java interfaces identical to Burp's API):
IBurpExtender
IBurpExtenderCallbacks
IHttpRequestResponse
IHttpService
IRequestInfo / IResponseInfo
IParameter
IScanIssue
IContextMenuFactory (add custom right-click menu items)
ITab (add custom tabs to main UI)
IMessageEditorTab (custom editor for request/response)
IIntruderPayloadGenerator / IIntruderPayloadProcessor
IScannerCheck (custom vulnerability checks)
Load extensions from JAR files
Built-in extension marketplace/store (optional)
Support Python extensions via Jython
Example extensions included:
Logger++ equivalent (advanced logging with filters)
Autorize (automatic authorization testing)
ActiveScan++ (additional checks)
JSON Web Tokens (JWT) editor
12. Collaborator Client
Built-in out-of-band (OOB) interaction server
Supports:
DNS lookups
HTTP/HTTPS requests
SMTP interactions
Generate unique subdomains/URLs for each test
Poll for interactions (async callback detection for blind vulnerabilities like SSRF, XXE, blind XSS, etc.)
Self-hosted option (use your own domain/server)
13. Logger++ (Advanced Logging)
Log every single request/response from all tools (proxy, scanner, repeater, intruder, extensions)
Advanced filters (regex, status code, length, MIME type, time range, tool source)
Color-coded rows
Export to CSV, JSON, HTML
Real-time log streaming
14. Session Handling & Macros
Macro recorder:
Record a sequence of requests (e.g., login flow)
Replay before each scanner/intruder request
Automatic token extraction using regex
Update cookies, headers, CSRF tokens dynamically
Session handling rules:
Cookie jar (automatic cookie management)
Custom parameter handling
Add/update headers dynamically
Handle redirects
15. Project Management
Project types:
Temporary (in-memory only)
Disk-based (saved to file, can reopen later)
Saved data:
Full proxy history
Site map
Scanner issues
Configuration settings
Repeater tabs
Intruder configurations
Export/import project data
Project-level scope and settings
16. User Interface (Swing GUI)
Main window layout:
Top menu bar: File, Edit, View, Tools, Help
Tab bar: Dashboard, Target, Proxy, Intruder, Repeater, Scanner, Sequencer, Decoder, Comparer, Extender, Settings
Draggable split panes (horizontal/vertical)
Detachable tabs (open in separate window)
Theme support:
Light mode
Dark mode (using FlatLaf Dark theme)
Custom color schemes
Keyboard shortcuts (identical to Burp):
Ctrl+R: Send to Repeater
Ctrl+I: Send to Intruder
Ctrl+Shift+B: Base64 encode/decode selection
Ctrl+Shift+U: URL encode/decode selection
Ctrl+F: Search
Ctrl+H: Open history
Message editor features:
Raw, Pretty (formatted), Hex, Params, Headers tabs
Syntax highlighting
Auto-formatting (JSON, XML, HTML)
Inline editing with validation
Copy as curl command, Python requests, etc.
Context menus:
Right-click on any request/response item
Send to Repeater, Intruder, Scanner, Comparer, Extensions
Copy URL, host, headers, body, etc.
Add comment, highlight, change color
Delete from history
17. Settings & Configuration
Proxy settings:
Listen address, port, IPv6
SSL certificate management
Match/replace rules
Intercept rules
Scanner settings:
Passive/active toggle
Scan speed and thoroughness
Insertion point options
Issue detection sensitivity
Intruder settings:
Default payload encodings
Thread count
Throttle, retries, timeouts
Spider settings:
Max depth, scope, forms, JavaScript
UI settings:
Font size, theme, layout
Network settings:
Upstream proxy, SOCKS
DNS resolver
Timeout values
Extensions settings:
Enable/disable loaded extensions
Extension output console
18. Certificate Management
Auto-generate root CA certificate on first run
Export CA certificate in PEM, DER, PKCS12 formats
Detailed installation instructions for:
Windows Certificate Store
macOS Keychain
Firefox certificate manager
Linux ca-certificates
iOS/Android
Regenerate certificate option
Option to import custom CA
19. Reporting
Export scanner issues to:
HTML (professional report with logo, executive summary, technical details, remediation)
XML
JSON
CSV
Custom report templates
Filter issues by severity, confidence, host
Include request/response evidence
20. Additional Features
Search functionality:
Search all history by text, regex, headers, body
Case-sensitive/insensitive
Search in requests, responses, or both
Scope management:
Define included/excluded URLs (regex or simple patterns)
Scope affects proxy history, scanner, spider
Suite-wide or project-specific
Hotkeys and automation:
Customizable keyboard shortcuts
Scripting interface for repetitive tasks
Performance:
Handle large projects (millions of requests)
Efficient memory management (pagination, lazy loading)
Multi-threaded for all heavy operations
Security:
No telemetry, fully offline
No automatic updates without user consent
Secure storage of credentials if needed
Documentation:
Comprehensive user manual (PDF, HTML)
Video tutorials
Example workflows
Extension development guide
API javadocs
Architecture Requirements
Layered architecture:
Presentation layer (Swing GUI)
Business logic layer (scanner engine, intruder, repeater logic)
Data access layer (database, file I/O)
Network layer (proxy, HTTP client)
Design patterns:
Observer pattern for event handling
Factory pattern for creating request/response objects
Strategy pattern for different attack/scan types
Singleton for global configuration
Multithreading:
Separate threads for proxy, scanner, intruder, spider
Thread pools for parallel processing
Proper synchronization to avoid race conditions
Error handling:
Graceful degradation
User-friendly error messages
Comprehensive logging for debugging
Testing:
Unit tests for core logic (JUnit 5)
Integration tests for HTTP handling
Performance benchmarks
Build & Packaging
Maven/Gradle setup:
Clean project structure
Dependencies managed via pom.xml or build.gradle
Build profiles for development, testing, production
Executable JAR:
Fat JAR with all dependencies included
Runnable with: java -jar ShadowProxy.jar
Platform-specific packages:
Windows: .exe wrapper (Launch4j) + installer (NSIS or Inno Setup)
macOS: .app bundle + DMG installer
Linux: .deb, .rpm packages, or universal AppImage
JRE bundling:
Optional: include JRE using jpackage or jlink
Creates standalone executable (no Java install required)
Documentation Requirements
README.md:
What is ShadowProxy
Features overview
Installation instructions
Quick start guide
Building from source
User Manual:
Detailed guide for every module
Screenshots and examples
Best practices for web security testing
Developer Documentation:
Architecture overview
Code structure
How to extend/customize
API reference for extensions
Legal/Ethical:
Disclaimer about authorized testing only
Responsible disclosure guidelines
Summary Checklist
Confirm you will deliver:

✅ Complete Java 17+ codebase
✅ Swing-based GUI identical in functionality to Burp Suite
✅ Full MITM proxy with SSL interception
✅ All modules: Proxy, Repeater, Intruder, Scanner, Spider, Sequencer, Decoder, Comparer
✅ Vulnerability scanner with 100+ checks
✅ Extension API for third-party plugins
✅ Collaborator server for OOB testing
✅ Session handling and macro recorder
✅ Project save/load functionality
✅ Professional reporting (HTML/XML/JSON)
✅ Cross-platform support (Windows, macOS, Linux)
✅ Executable JAR and platform-specific installers
✅ Complete documentation and user manual
✅ Zero paid dependencies (all open-source)
✅ Production-ready, enterprise-grade code quality


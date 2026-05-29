# mitmproxy-grid-java-client

Standalone Java SDK for the [mitmproxy-grid](https://github.com/ygrip/mitmproxy-grid) REST API v2.

No Selenium, Playwright, Appium, Cucumber, Spring, or Testara dependencies.

---

## Prerequisites

This SDK is a client for **mitmproxy-grid** — a custom REST management layer on top of
[mitmproxy](https://mitmproxy.org/) that runs multiple isolated proxy instances, each on its
own port, via a single HTTP API. It is **not** compatible with the standard mitmproxy CLI or
any other proxy server.

### What you need

| Requirement | Details |
|---|---|
| [mitmproxy](https://docs.mitmproxy.org/stable/overview-installation/) | `pip install mitmproxy` — tested with mitmproxy 10+ |
| [mitmproxy-grid](https://github.com/ygrip/mitmproxy-grid) | The custom grid addon server — see setup below |
| Python 3.11+ | Required by mitmproxy-grid |
| Java 21+ | Required by this SDK |

### Setting up mitmproxy-grid

Clone and start the grid server:

```bash
git clone https://github.com/ygrip/mitmproxy-grid.git
cd mitmproxy-grid
pip install -r requirements.txt
python grid.py
```

By default the grid API listens on **port 8090** and allocates proxy instances in the
**port range 8100–8199**. These are configurable via environment variables:

```bash
MITMPROXY_GRID_PORT=8090           # REST API port
MITMPROXY_GRID_PROXY_PORT_START=8100
MITMPROXY_GRID_PROXY_PORT_END=8199
MITMPROXY_GRID_DEFAULT_TTL=300     # seconds
```

Verify the server is healthy:

```bash
curl http://localhost:8090/health
# {"status":"up","instances":0,"availableSlots":100,...}
```

### API version

This SDK targets **mitmproxy-grid REST API v2**. The required endpoints are:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/health` | Grid health and capacity |
| `POST` | `/instances` | Create a proxy instance |
| `GET` | `/instances` | List all instances |
| `GET` | `/instances/{id}` | Get instance detail |
| `DELETE` | `/instances/{id}` | Destroy an instance |
| `POST` | `/instances/{id}/renew` | Renew instance TTL |
| `POST` | `/instances/{id}/rules` | Add an interception rule |
| `GET` | `/instances/{id}/rules` | List rules |
| `DELETE` | `/instances/{id}/rules/{index}` | Delete a rule by index |
| `PATCH` | `/instances/{id}/rules/{index}/toggle` | Toggle a rule on/off |
| `GET` | `/instances/{id}/cert` | Download CA certificate PEM |

### How interception works

```
Browser / App
    │
    └──► mitmproxy instance (port 8101, 8102, …)   ← SDK configures rules here
              │
              └──► Target server
```

Each test thread creates its own proxy instance (with its own port and isolated rule set)
through the grid API. Rules are applied to intercepted traffic in real time without
restarting the proxy.

---

## Installation

```xml
<dependency>
  <groupId>io.github.ygrip</groupId>
  <artifactId>mitmproxy-grid-java-client</artifactId>
  <version>0.1.0</version>
</dependency>
```

---

## Quick Start

```java
MitmProxyGridClient client = MitmProxyGridClient.builder()
    .baseUrl("http://localhost:8090")
    .connectTimeout(Duration.ofSeconds(10))
    .requestTimeout(Duration.ofSeconds(10))
    .maxRetries(3)
    .build();

// Wait for the grid to be ready (useful in CI where the server may still be starting)
client.waitUntilReady(Duration.ofSeconds(30), Duration.ofSeconds(1));

// Create an isolated proxy instance with a 5-minute TTL
MitmProxyCreateInstanceResponse instance = client.createInstance(300);
System.out.println("Proxy port: " + instance.getPort()); // e.g. 8101

// Add an interception rule
client.createRule(instance.getInstanceId(), MitmProxyRule.mockResponse(
    "api.example.com/users",
    200,
    Map.of("id", 1, "name", "Test User")
));

// Point your browser/driver at http://<host>:8101 — rules fire automatically

// After the test: clear rules and destroy the instance
client.clearAllRules(instance.getInstanceId());
client.destroyInstance(instance.getInstanceId(), true);
```

---

## Rule Factory Methods

```java
// Mock a response with a fixed status and JSON body
MitmProxyRule.mockResponse("/api/users", 200, Map.of("id", 1))

// Block requests (responds with 403)
MitmProxyRule.block("/analytics")

// Substring find-and-replace in response body
MitmProxyRule.replaceResponseBody("/feature-flag", "\"enabled\":false", "\"enabled\":true")

// Substring find-and-replace in request body
MitmProxyRule.replaceRequestBody("/submit", "staging-id", "prod-id")

// Inject or overwrite request headers
MitmProxyRule.setRequestHeaders("/api", Map.of("Authorization", "Bearer test-token"))

// Inject or overwrite response headers
MitmProxyRule.setResponseHeaders("/api", Map.of("X-Feature", "enabled"))

// Append or overwrite URL query parameters
MitmProxyRule.setQueryParams("/search", Map.of("lang", "en", "page", "1"))

// Strip cache-negotiation headers — apply BEFORE navigating
MitmProxyRule.disableCaching()                       // all traffic
MitmProxyRule.disableCaching("static.example.com")  // scoped to URL substring

// Replace a binary asset (image, font, …) with a local file
MitmProxyRule.replaceImage("/avatar.png", new File("src/test/resources/stub.png"), "image/png")
MitmProxyRule.replaceImageBase64("/avatar.png", base64EncodedContent, "image/png")
```

---

## Client Configuration

```java
MitmProxyGridClient client = MitmProxyGridClient.builder()
    .baseUrl("http://localhost:8090")     // mitmproxy-grid API base URL
    .connectTimeout(Duration.ofSeconds(10))
    .requestTimeout(Duration.ofSeconds(15))
    .maxRetries(3)                        // retries on 5xx; 4xx are not retried
    .retryBaseDelay(Duration.ofMillis(500)) // exponential backoff: 500ms, 1s, 2s, …
    .objectMapper(customObjectMapper)     // optional — bring your own Jackson mapper
    .build();
```

### Configuration defaults

| Parameter | Default |
|---|---|
| `baseUrl` | `http://localhost:8090` |
| `connectTimeout` | 10 s |
| `requestTimeout` | 10 s |
| `maxRetries` | 3 |
| `retryBaseDelay` | 500 ms |

---

## File-Based Rules

Use `MitmProxyRuleFileSpec` to load rule definitions from JSON files in your test
resources. Binary files (images, fonts, archives) are automatically base64-encoded.

```java
// src/test/resources/proxy-rules/mock-avatar.json
// {
//   "match": { "urlContains": "avatars.example.com", "responseContentType": "image/" },
//   "action": { "modifyResponse": { "statusCode": 200 } },
//   "responseBodyFile": "images/stub-avatar.png"
// }

MitmProxyRuleFileSpec spec = MitmProxyRuleFileSpec.builder()
    .match(MitmProxyRuleMatch.builder()
        .urlContains("avatars.example.com")
        .responseContentType("image/")
        .build())
    .action(MitmProxyRuleAction.builder()
        .modifyResponse(MitmProxyResponseModification.builder().statusCode(200).build())
        .build())
    .responseBodyFile("images/stub-avatar.png")
    .build();

MitmProxyRule rule = spec.toMitmProxyRule(Path.of("src/test/resources"));
client.createRule(instanceId, rule);
```

---

## Error Handling

All SDK exceptions extend `MitmProxyGridException` (unchecked).

```java
try {
    client.createInstance(300);
} catch (MitmProxyGridHttpException e) {
    // HTTP-level error — inspect e.statusCode(), e.method(), e.path(), e.responseBody()
} catch (MitmProxyGridTimeoutException e) {
    // Request exceeded the configured requestTimeout
} catch (MitmProxyGridSerializationException e) {
    // JSON serialization or deserialization failure
} catch (MitmProxyGridException e) {
    // Any other SDK-level error (I/O, interrupted, etc.)
}
```

---

## Browser / Driver Integration

Browser-specific proxy wiring is intentionally outside this SDK. Use the instance port
returned by `createInstance()` to configure your driver:

**Selenium:**
```java
int port = instance.getPort();
Proxy proxy = new Proxy();
proxy.setHttpProxy("localhost:" + port);
proxy.setSslProxy("localhost:" + port);
ChromeOptions options = new ChromeOptions();
options.setProxy(proxy);
```

**Playwright:**
```java
int port = instance.getPort();
Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
    .setProxy(new Proxy("http://localhost:" + port)));
```

**Appium:**
```java
int port = instance.getPort();
DesiredCapabilities caps = new DesiredCapabilities();
caps.setCapability("proxyUrl", "http://localhost:" + port);
```

If you use [Testara](https://github.com/ygrip/testara), `MitmProxySeleniumUtility`,
`MitmProxyPlaywrightUtility`, and `MitmProxyAppiumUtility` handle this wiring automatically.

---

## CA Certificate

Some browsers and mobile devices require trusting the mitmproxy CA to intercept HTTPS.
Retrieve the PEM for a running instance:

```java
String pem = client.getCaCertificate(instance.getInstanceId());
// Install pem into your browser profile or device trust store
```

---

## Requirements

| Dependency | Version |
|---|---|
| Java | 21+ |
| mitmproxy-grid server | v2 API (see [prerequisites](#prerequisites)) |
| Jackson Databind | 2.x (transitive) |

---

## License

Apache License 2.0

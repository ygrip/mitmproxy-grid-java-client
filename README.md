# mitmproxy-grid-java-client

Standalone Java SDK for the [mitmproxy-grid](https://github.com/ygrip/mitmproxy-grid) REST API.

The SDK has no Selenium, Playwright, Appium, Cucumber, Spring, Testcontainers, or Testara dependency.

## Compatibility

Client `0.2.0` supports the exact grid release:

```text
mitmproxy-grid 2.1.0
API v2
ghcr.io/ygrip/mitmproxy-grid:2.1.0
```

The same values are available in code:

```java
MitmProxyGridCompatibility.SUPPORTED_GRID_VERSION; // 2.1.0
MitmProxyGridCompatibility.SUPPORTED_API_VERSION;  // 2
MitmProxyGridCompatibility.SUPPORTED_IMAGE;        // ghcr.io/ygrip/mitmproxy-grid:2.1.0
```

This lets an automation module pin the exact image without duplicating the version string:

```java
DockerImageName image = DockerImageName.parse(
    MitmProxyGridCompatibility.SUPPORTED_IMAGE
);
```

Compatibility checking is explicit rather than forced on every request:

```java
MitmProxyGridClient client = new MitmProxyGridClient("http://localhost:8090");
client.requireCompatibleGrid();
```

`requireCompatibleGrid()` verifies both grid `2.1.0` and API `2`. Existing applications that do not call it keep the previous tolerant behavior.

## Installation

```xml
<dependency>
  <groupId>io.github.ygrip</groupId>
  <artifactId>mitmproxy-grid-java-client</artifactId>
  <version>0.2.0</version>
</dependency>
```

## Start the supported grid

```bash
docker run -d \
  --name mitmproxy-grid \
  -p 8090:8090 \
  -p 10000-10100:10000-10100 \
  ghcr.io/ygrip/mitmproxy-grid:2.1.0
```

For horizontally scalable Selenium Grid deployments, use the coordinator/worker deployment documented by `mitmproxy-grid` and connect browser nodes to the returned worker proxy endpoint.

## Quick start

```java
MitmProxyGridClient client = MitmProxyGridClient.builder()
    .baseUrl("http://localhost:8090")
    .connectTimeout(Duration.ofSeconds(10))
    .requestTimeout(Duration.ofSeconds(10))
    .maxRetries(3)
    .build();

client.waitUntilReady(Duration.ofSeconds(30), Duration.ofSeconds(1));
client.requireCompatibleGrid();

MitmProxyCreateInstanceResponse instance = client.createInstance(300);
MitmProxyEndpoint endpoint = client.resolveProxyEndpoint(instance);

System.out.println(endpoint.url());

client.createRule(instance.getInstanceId(), MitmProxyRule.mockResponse(
    "api.example.com/users",
    200,
    Map.of("id", 1, "name", "Test User")
));

client.clearAllRules(instance.getInstanceId());
client.destroyInstance(instance.getInstanceId(), true);
```

## Distributed proxy endpoints

Grid `2.1.0` can run as a coordinator with multiple proxy workers. In that mode the REST API host and the proxy host are intentionally different.

A create response can look like:

```json
{
  "instanceId": "abc-123",
  "port": 10003,
  "proxyHost": "mitm-worker-3",
  "proxyPort": 10003,
  "proxyUrl": "http://mitm-worker-3:10003",
  "workerId": "worker-3",
  "status": "running",
  "ttl": 1800,
  "expiresAt": "2026-08-11T10:00:00Z"
}
```

Do not construct a distributed proxy address from the coordinator host plus `port`. Resolve it through the SDK:

```java
MitmProxyCreateInstanceResponse instance = client.createInstance();
MitmProxyEndpoint endpoint = client.resolveProxyEndpoint(instance);

String proxyHost = endpoint.host();
int proxyPort = endpoint.port();
String proxyUrl = endpoint.url();
```

For older standalone responses that only contain `port`, `resolveProxyEndpoint()` automatically falls back to the grid API host, preserving the old behavior.

## Testcontainers integration

Testcontainers remains outside this SDK, but an automation project can use the SDK's pinned image constant:

```java
GenericContainer<?> grid = new GenericContainer<>(
    DockerImageName.parse(MitmProxyGridCompatibility.SUPPORTED_IMAGE)
)
    .withExposedPorts(8090)
    .waitingFor(Wait.forHttp("/health").forPort(8090));
```

For a local standalone Testcontainer, expose or bind the proxy range as required by your browser topology. For Selenium Grid at scale, prefer the grid's shared coordinator/worker deployment rather than one grid Testcontainer per Jenkins job.

## Health and compatibility

```java
MitmProxyHealthResponse health = client.health();

health.getGridVersion(); // 2.1.0
health.getApiVersion();  // 2
health.getMode();        // standalone, coordinator, worker
health.getWorkers();     // coordinator worker list

boolean supported = client.isCompatibleGrid();
```

## Rule factory methods

```java
MitmProxyRule.mockResponse("/api/users", 200, Map.of("id", 1));
MitmProxyRule.block("/analytics");
MitmProxyRule.replaceResponseBody("/feature-flag", "\"enabled\":false", "\"enabled\":true");
MitmProxyRule.replaceRequestBody("/submit", "staging-id", "prod-id");
MitmProxyRule.setRequestHeaders("/api", Map.of("Authorization", "Bearer test-token"));
MitmProxyRule.setResponseHeaders("/api", Map.of("X-Feature", "enabled"));
MitmProxyRule.setQueryParams("/search", Map.of("lang", "en", "page", "1"));
MitmProxyRule.disableCaching();
MitmProxyRule.disableCaching("static.example.com");
MitmProxyRule.replaceImage("/avatar.png", new File("src/test/resources/stub.png"), "image/png");
```

## Client configuration

```java
MitmProxyGridClient client = MitmProxyGridClient.builder()
    .baseUrl("http://localhost:8090")
    .connectTimeout(Duration.ofSeconds(10))
    .requestTimeout(Duration.ofSeconds(15))
    .maxRetries(3)
    .retryBaseDelay(Duration.ofMillis(500))
    .objectMapper(customObjectMapper)
    .build();
```

Defaults:

| Parameter | Default |
|---|---|
| `baseUrl` | `http://localhost:8090` |
| `connectTimeout` | 10 s |
| `requestTimeout` | 10 s |
| `maxRetries` | 3 |
| `retryBaseDelay` | 500 ms |

## Browser wiring

Use the resolved endpoint rather than the raw legacy port.

### Selenium

```java
MitmProxyEndpoint endpoint = client.resolveProxyEndpoint(instance);
Proxy proxy = new Proxy();
proxy.setHttpProxy(endpoint.host() + ":" + endpoint.port());
proxy.setSslProxy(endpoint.host() + ":" + endpoint.port());
```

### Playwright

```java
MitmProxyEndpoint endpoint = client.resolveProxyEndpoint(instance);
Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
    .setProxy(new Proxy(endpoint.url())));
```

## CA certificate

```java
String pem = client.getCaCertificate(instance.getInstanceId());
```

## Error handling

All SDK exceptions extend `MitmProxyGridException`.

```java
try {
    client.requireCompatibleGrid();
    client.createInstance(300);
} catch (MitmProxyGridCompatibilityException e) {
    // Grid/client version mismatch.
} catch (MitmProxyGridHttpException e) {
    // HTTP error.
} catch (MitmProxyGridTimeoutException e) {
    // Request timeout.
} catch (MitmProxyGridException e) {
    // Other SDK transport/serialization error.
}
```

## Requirements

| Dependency | Version |
|---|---|
| Java | 21+ |
| mitmproxy-grid | 2.1.0 |
| mitmproxy-grid API | v2 |
| Jackson Databind | 2.x transitive |

## License

Apache License 2.0

# mitmproxy-grid-java-client

Standalone Java SDK for the [MitmProxy Grid](https://github.com/ygrip/mitmproxy-grid) REST API v2.

No Selenium, Playwright, Appium, Cucumber, Spring, or Testara dependencies.

## Installation

```xml
<dependency>
  <groupId>io.github.ygrip</groupId>
  <artifactId>mitmproxy-grid-java-client</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quick Start

```java
MitmProxyGridClient client = MitmProxyGridClient.builder()
    .baseUrl("http://localhost:8090")
    .connectTimeout(Duration.ofSeconds(10))
    .requestTimeout(Duration.ofSeconds(10))
    .maxRetries(3)
    .build();

client.waitUntilReady(Duration.ofSeconds(30), Duration.ofSeconds(1));
MitmProxyCreateInstanceResponse instance = client.createInstance(300);

client.createRule(instance.getInstanceId(), MitmProxyRule.mockResponse(
    "api.example.com/users",
    200,
    Map.of("mocked", true)
));

// ... run your tests ...

client.clearAllRules(instance.getInstanceId());
client.destroyInstance(instance.getInstanceId(), true);
```

## Rule Factory Methods

```java
// Mock a response
MitmProxyRule.mockResponse("/api/endpoint", 200, Map.of("key", "value"))

// Block requests
MitmProxyRule.block("/analytics")

// Replace response body text
MitmProxyRule.replaceResponseBody("/feature-flag", "false", "true")

// Replace request body text
MitmProxyRule.replaceRequestBody("/submit", "old", "new")

// Set request headers
MitmProxyRule.setRequestHeaders("/api", Map.of("Authorization", "Bearer token"))

// Set response headers
MitmProxyRule.setResponseHeaders("/api", Map.of("X-Custom", "value"))

// Add query params
MitmProxyRule.setQueryParams("/search", Map.of("lang", "en"))

// Disable caching (apply before navigation)
MitmProxyRule.disableCaching()
MitmProxyRule.disableCaching("static.example.com")

// Replace image/binary asset
MitmProxyRule.replaceImage("/avatar", new File("replacement.png"), "image/png")
MitmProxyRule.replaceImageBase64("/avatar", base64String, "image/png")
```

## Client Configuration

```java
MitmProxyGridClient client = MitmProxyGridClient.builder()
    .baseUrl("http://localhost:8090")
    .connectTimeout(Duration.ofSeconds(10))
    .requestTimeout(Duration.ofSeconds(15))
    .maxRetries(3)
    .retryBaseDelay(Duration.ofMillis(500))
    .objectMapper(customObjectMapper)   // optional
    .build();
```

## File-Based Rules

```java
MitmProxyRuleFileSpec spec = MitmProxyRuleFileSpec.builder()
    .match(MitmProxyRuleMatch.builder().urlContains("avatars.example.com").build())
    .action(MitmProxyRuleAction.builder()
        .modifyResponse(MitmProxyResponseModification.builder().statusCode(200).build())
        .build())
    .responseBodyFile("images/replacement-avatar.png")
    .build();

MitmProxyRule rule = spec.toMitmProxyRule(Path.of("src/test/resources"));
client.createRule(instanceId, rule);
```

## Error Handling

```java
try {
    client.createInstance(300);
} catch (MitmProxyGridHttpException e) {
    // e.statusCode(), e.method(), e.path(), e.responseBody()
} catch (MitmProxyGridTimeoutException e) {
    // request timed out
} catch (MitmProxyGridSerializationException e) {
    // JSON parse/write failure
} catch (MitmProxyGridException e) {
    // base exception for all SDK errors
}
```

## Selenium / Testara Integration

Browser-specific proxy conversion is intentionally outside this SDK.
In Testara, `MitmProxySeleniumUtility` converts the created instance port into a Selenium `Proxy`.

## Requirements

- Java 21+
- Jackson Databind 2.x

## License

Apache License 2.0

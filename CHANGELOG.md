# Changelog

## [0.1.0-SNAPSHOT] - Unreleased

### Added
- Initial extraction from `ygrip/testara` (`testara-ui` module)
- `MitmProxyGridClient`: full MitmProxy Grid REST API v2 client
- Typed exception hierarchy: `MitmProxyGridException`, `MitmProxyGridHttpException`, `MitmProxyGridTimeoutException`, `MitmProxyGridSerializationException`
- `MitmProxyGridClientConfig` record with builder
- `MitmProxyGridClient.builder()` static factory
- `waitUntilReady(Duration, Duration)` overload
- `MitmProxyRuleFileSpec`: file-based rule spec with JDK-only file I/O (replaces Testara's `ProxyRuleCreation`)
- All DTO models repackaged to `io.github.ygrip.mitmproxy.grid.client.model`
- Zero Testara dependency

### Migration from Testara

Old imports:
```java
import io.github.ygrip.testara.ui.proxy.MitmProxyClient;
import io.github.ygrip.testara.ui.model.MitmProxyRule;
```

New imports:
```java
import io.github.ygrip.mitmproxy.grid.client.MitmProxyGridClient;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyRule;
```

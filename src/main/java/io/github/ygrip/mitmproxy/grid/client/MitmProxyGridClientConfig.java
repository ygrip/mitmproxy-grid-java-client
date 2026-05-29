package io.github.ygrip.mitmproxy.grid.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Immutable configuration for {@link MitmProxyGridClient}.
 * Construct via {@link MitmProxyGridClient#builder()} or directly.
 */
public record MitmProxyGridClientConfig(
    URI baseUri,
    Duration connectTimeout,
    Duration requestTimeout,
    int maxRetries,
    Duration retryBaseDelay,
    ObjectMapper objectMapper
) {

  public static final URI DEFAULT_BASE_URI = URI.create("http://localhost:8090/");
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);
  public static final int DEFAULT_MAX_RETRIES = 3;
  public static final Duration DEFAULT_RETRY_BASE_DELAY = Duration.ofMillis(500);

  public MitmProxyGridClientConfig {
    if (baseUri == null) baseUri = DEFAULT_BASE_URI;
    String uriStr = baseUri.toString();
    if (!uriStr.endsWith("/")) baseUri = URI.create(uriStr + "/");

    connectTimeout = Objects.requireNonNullElse(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
    requestTimeout = Objects.requireNonNullElse(requestTimeout, DEFAULT_REQUEST_TIMEOUT);
    retryBaseDelay = Objects.requireNonNullElse(retryBaseDelay, DEFAULT_RETRY_BASE_DELAY);

    if (connectTimeout.isNegative() || connectTimeout.isZero())
      throw new IllegalArgumentException("connectTimeout must be positive");
    if (requestTimeout.isNegative() || requestTimeout.isZero())
      throw new IllegalArgumentException("requestTimeout must be positive");
    if (maxRetries < 1)
      throw new IllegalArgumentException("maxRetries must be >= 1");
    if (retryBaseDelay.isNegative() || retryBaseDelay.isZero())
      throw new IllegalArgumentException("retryBaseDelay must be positive");

    if (objectMapper == null) objectMapper = defaultObjectMapper();
  }

  public static MitmProxyGridClientConfig defaults(String baseUrl) {
    return new MitmProxyGridClientConfig(
        URI.create(baseUrl),
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_REQUEST_TIMEOUT,
        DEFAULT_MAX_RETRIES,
        DEFAULT_RETRY_BASE_DELAY,
        null);
  }

  static ObjectMapper defaultObjectMapper() {
    return new ObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
}

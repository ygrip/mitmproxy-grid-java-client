package io.github.ygrip.mitmproxy.grid.client;

import java.net.URI;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MitmProxyGridClientBuilder {

  private URI baseUri = MitmProxyGridClientConfig.DEFAULT_BASE_URI;
  private Duration connectTimeout = MitmProxyGridClientConfig.DEFAULT_CONNECT_TIMEOUT;
  private Duration requestTimeout = MitmProxyGridClientConfig.DEFAULT_REQUEST_TIMEOUT;
  private int maxRetries = MitmProxyGridClientConfig.DEFAULT_MAX_RETRIES;
  private Duration retryBaseDelay = MitmProxyGridClientConfig.DEFAULT_RETRY_BASE_DELAY;
  private ObjectMapper objectMapper;

  public MitmProxyGridClientBuilder baseUrl(String url) {
    this.baseUri = URI.create(url);
    return this;
  }

  public MitmProxyGridClientBuilder baseUri(URI uri) {
    this.baseUri = uri;
    return this;
  }

  public MitmProxyGridClientBuilder connectTimeout(Duration timeout) {
    this.connectTimeout = timeout;
    return this;
  }

  public MitmProxyGridClientBuilder requestTimeout(Duration timeout) {
    this.requestTimeout = timeout;
    return this;
  }

  public MitmProxyGridClientBuilder maxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
    return this;
  }

  public MitmProxyGridClientBuilder retryBaseDelay(Duration delay) {
    this.retryBaseDelay = delay;
    return this;
  }

  public MitmProxyGridClientBuilder objectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    return this;
  }

  public MitmProxyGridClient build() {
    MitmProxyGridClientConfig config = new MitmProxyGridClientConfig(
        baseUri, connectTimeout, requestTimeout, maxRetries, retryBaseDelay, objectMapper);
    return new MitmProxyGridClient(config);
  }
}

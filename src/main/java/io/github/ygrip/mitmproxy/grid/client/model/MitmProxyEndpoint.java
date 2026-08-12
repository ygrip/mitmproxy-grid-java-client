package io.github.ygrip.mitmproxy.grid.client.model;

import java.net.URI;

/**
 * Routable proxy endpoint returned by mitmproxy-grid.
 */
public record MitmProxyEndpoint(String host, int port) {

  public MitmProxyEndpoint {
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("proxy host must not be blank");
    }
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("proxy port must be between 1 and 65535");
    }
  }

  public String url() {
    return "http://" + host + ":" + port;
  }

  public URI uri() {
    return URI.create(url());
  }

  static MitmProxyEndpoint resolve(
      String proxyUrl,
      String proxyHost,
      Integer proxyPort,
      int legacyPort,
      String fallbackHost) {
    if (proxyUrl != null && !proxyUrl.isBlank()) {
      URI uri = URI.create(proxyUrl);
      if (uri.getHost() != null && uri.getPort() > 0) {
        return new MitmProxyEndpoint(uri.getHost(), uri.getPort());
      }
    }

    String host = proxyHost != null && !proxyHost.isBlank() ? proxyHost : fallbackHost;
    int port = proxyPort != null && proxyPort > 0 ? proxyPort : legacyPort;
    return new MitmProxyEndpoint(host, port);
  }
}

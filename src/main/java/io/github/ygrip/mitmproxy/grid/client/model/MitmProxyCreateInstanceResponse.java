package io.github.ygrip.mitmproxy.grid.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyCreateInstanceResponse {
  private String instanceId;
  private int port;
  private String proxyHost;
  private Integer proxyPort;
  private String proxyUrl;
  private String workerId;
  private String status;
  private int ttl;
  private String expiresAt;

  /**
   * Resolve the actual proxy endpoint. Distributed grid responses use the advertised
   * worker endpoint; older standalone responses fall back to the grid host plus legacy port.
   */
  public MitmProxyEndpoint resolveProxyEndpoint(String fallbackGridHost) {
    return MitmProxyEndpoint.resolve(proxyUrl, proxyHost, proxyPort, port, fallbackGridHost);
  }
}

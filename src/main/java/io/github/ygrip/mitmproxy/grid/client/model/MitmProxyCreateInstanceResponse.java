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
  private String status;
  private int ttl;
  private String expiresAt;
}

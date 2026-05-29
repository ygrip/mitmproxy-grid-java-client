package io.github.ygrip.mitmproxy.grid.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyRenewResponse {
  private String status;
  private String message;
  private int ttl;
  private String expiresAt;
  private double remainingSeconds;
}

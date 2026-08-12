package io.github.ygrip.mitmproxy.grid.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyWorkerInfo {
  private String workerId;
  private String apiUrl;
  private String proxyHost;
  private int availableSlots;
  private int instances;
  private String gridVersion;
  private String apiVersion;
  private double lastSeenSeconds;
}

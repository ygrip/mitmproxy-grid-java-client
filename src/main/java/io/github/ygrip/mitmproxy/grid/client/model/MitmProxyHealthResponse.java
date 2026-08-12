package io.github.ygrip.mitmproxy.grid.client.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyHealthResponse {
  private String status;
  private int instances;
  private List<Integer> usedPorts;
  private int availableSlots;
  private String portRange;
  private int defaultTtl;
  private String gridVersion;
  private String apiVersion;
  private String mode;
  private String workerId;
  private String proxyHost;
  private List<MitmProxyWorkerInfo> workers;

  public boolean isHealthy() {
    return "up".equalsIgnoreCase(status);
  }
}

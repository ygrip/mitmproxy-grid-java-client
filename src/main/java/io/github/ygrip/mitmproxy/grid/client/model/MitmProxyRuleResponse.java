package io.github.ygrip.mitmproxy.grid.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Rule as returned by the API — includes the positional index. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyRuleResponse {
  private int index;
  private boolean enabled;
  private int priority;
  private MitmProxyRuleMatch match;
  private MitmProxyRuleAction action;
}

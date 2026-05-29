package io.github.ygrip.mitmproxy.grid.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** What to do when a rule matches. Both fields are optional and can be combined. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyRuleAction {
  private MitmProxyRequestModification modifyRequest;
  private MitmProxyResponseModification modifyResponse;
}

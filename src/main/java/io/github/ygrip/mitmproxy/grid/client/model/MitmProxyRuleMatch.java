package io.github.ygrip.mitmproxy.grid.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Criteria a flow must satisfy for the rule to fire. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyRuleMatch {
  private String urlContains;
  private String urlPattern;
  private String method;
  private String contentType;
  private String responseContentType;
}

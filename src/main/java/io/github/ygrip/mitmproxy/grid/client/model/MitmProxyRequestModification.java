package io.github.ygrip.mitmproxy.grid.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Modifications applied to the outgoing request. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyRequestModification {
  private MitmProxyHeaderModification headers;
  private MitmProxyParamModification params;
  private Object body;
  private MitmProxyBodyReplace bodyReplace;
}

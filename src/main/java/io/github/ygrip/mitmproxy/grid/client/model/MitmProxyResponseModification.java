package io.github.ygrip.mitmproxy.grid.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Modifications applied to the incoming response. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyResponseModification {
  private Integer statusCode;
  private MitmProxyHeaderModification headers;
  private Object body;
  /** Base64-encoded body for binary responses (images, fonts, etc.). */
  private String bodyBase64;
  private MitmProxyBodyReplace bodyReplace;
}

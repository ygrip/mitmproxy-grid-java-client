package io.github.ygrip.mitmproxy.grid.client.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create a new interception rule (input model).
 * Maps to {@code RuleCreate} in the MitmProxy Grid API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyRule {

  @Builder.Default
  private boolean enabled = true;
  @Builder.Default
  private int priority = 0;
  private MitmProxyRuleMatch match;
  private MitmProxyRuleAction action;

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static String toJson(Object body) {
    try {
      return MAPPER.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot serialize body to JSON: " + e.getMessage(), e);
    }
  }

  // ── Convenience factory methods ────────────────────────────────────

  public static MitmProxyRule mockResponse(String urlContains, int statusCode, Object body) {
    String bodyStr = body instanceof String s ? s : toJson(body);
    return MitmProxyRule.builder()
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyResponse(MitmProxyResponseModification.builder()
                .statusCode(statusCode)
                .body(bodyStr)
                .build())
            .build())
        .build();
  }

  public static MitmProxyRule replaceResponseBody(String urlContains, String from, String to) {
    return MitmProxyRule.builder()
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyResponse(MitmProxyResponseModification.builder()
                .bodyReplace(MitmProxyBodyReplace.builder().from(from).to(to).build())
                .build())
            .build())
        .build();
  }

  public static MitmProxyRule replaceRequestBody(String urlContains, String from, String to) {
    return MitmProxyRule.builder()
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyRequest(MitmProxyRequestModification.builder()
                .bodyReplace(MitmProxyBodyReplace.builder().from(from).to(to).build())
                .build())
            .build())
        .build();
  }

  public static MitmProxyRule setRequestHeaders(String urlContains, Map<String, String> headers) {
    return MitmProxyRule.builder()
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyRequest(MitmProxyRequestModification.builder()
                .headers(MitmProxyHeaderModification.builder().set(headers).build())
                .build())
            .build())
        .build();
  }

  public static MitmProxyRule setResponseHeaders(String urlContains, Map<String, String> headers) {
    return MitmProxyRule.builder()
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyResponse(MitmProxyResponseModification.builder()
                .headers(MitmProxyHeaderModification.builder().set(headers).build())
                .build())
            .build())
        .build();
  }

  public static MitmProxyRule block(String urlContains) {
    return MitmProxyRule.builder()
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyResponse(MitmProxyResponseModification.builder()
                .statusCode(403)
                .body("")
                .build())
            .build())
        .build();
  }

  public static MitmProxyRule setQueryParams(String urlContains, Map<String, String> params) {
    return MitmProxyRule.builder()
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyRequest(MitmProxyRequestModification.builder()
                .params(MitmProxyParamModification.builder().set(params).build())
                .build())
            .build())
        .build();
  }

  /**
   * Strip cache-negotiation headers from requests and force no-store on responses.
   * Apply before navigating so assets are never served from cache.
   */
  public static MitmProxyRule disableCaching(String urlContains) {
    return MitmProxyRule.builder()
        .priority(Integer.MAX_VALUE)
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyRequest(MitmProxyRequestModification.builder()
                .headers(MitmProxyHeaderModification.builder()
                    .remove(List.of("If-None-Match", "If-Modified-Since", "If-Range"))
                    .build())
                .build())
            .modifyResponse(MitmProxyResponseModification.builder()
                .headers(MitmProxyHeaderModification.builder()
                    .set(Map.of(
                        "Cache-Control", "no-store, no-cache, must-revalidate",
                        "Pragma", "no-cache"))
                    .remove(List.of("ETag", "Last-Modified"))
                    .build())
                .build())
            .build())
        .build();
  }

  public static MitmProxyRule disableCaching() {
    return disableCaching("");
  }

  public static MitmProxyRule replaceImage(String urlContains, File imageFile, String contentType) throws IOException {
    byte[] bytes = Files.readAllBytes(imageFile.toPath());
    return replaceImageBase64(urlContains, Base64.getEncoder().encodeToString(bytes), contentType);
  }

  public static MitmProxyRule replaceImageBase64(String urlContains, String base64Content, String contentType) {
    return MitmProxyRule.builder()
        .match(MitmProxyRuleMatch.builder().urlContains(urlContains).build())
        .action(MitmProxyRuleAction.builder()
            .modifyResponse(MitmProxyResponseModification.builder()
                .statusCode(200)
                .headers(MitmProxyHeaderModification.builder()
                    .set(Map.of(
                        "Content-Type", contentType,
                        "Cache-Control", "no-cache, no-store, must-revalidate",
                        "Pragma", "no-cache"))
                    .build())
                .bodyBase64(base64Content)
                .build())
            .build())
        .build();
  }
}

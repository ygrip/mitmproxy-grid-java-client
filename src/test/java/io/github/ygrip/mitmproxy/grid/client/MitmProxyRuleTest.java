package io.github.ygrip.mitmproxy.grid.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyRule;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyRuleFileSpec;

class MitmProxyRuleTest {

  private final ObjectMapper om = new ObjectMapper();

  @Test
  void mockResponse_stringBody_usedDirectly() {
    MitmProxyRule rule = MitmProxyRule.mockResponse("api.example.com", 200, "{\"ok\":true}");
    assertEquals("{\"ok\":true}", rule.getAction().getModifyResponse().getBody());
  }

  @Test
  void mockResponse_objectBody_serializedToJson() {
    MitmProxyRule rule = MitmProxyRule.mockResponse("api.example.com", 200, Map.of("mocked", true));
    Object body = rule.getAction().getModifyResponse().getBody();
    assertNotNull(body);
    assertTrue(body.toString().contains("mocked"));
    assertTrue(body.toString().contains("true"));
  }

  @Test
  void mockResponse_setsStatusCode() {
    MitmProxyRule rule = MitmProxyRule.mockResponse("example.com", 404, "not found");
    assertEquals(404, rule.getAction().getModifyResponse().getStatusCode());
  }

  @Test
  void block_sets403AndEmptyBody() {
    MitmProxyRule rule = MitmProxyRule.block("/analytics");
    assertEquals(403, rule.getAction().getModifyResponse().getStatusCode());
    assertEquals("", rule.getAction().getModifyResponse().getBody());
    assertEquals("/analytics", rule.getMatch().getUrlContains());
  }

  @Test
  void disableCaching_setsMaxPriority() {
    MitmProxyRule rule = MitmProxyRule.disableCaching();
    assertEquals(Integer.MAX_VALUE, rule.getPriority());
    assertEquals("", rule.getMatch().getUrlContains());
  }

  @Test
  void disableCaching_withUrl_setsMatchUrl() {
    MitmProxyRule rule = MitmProxyRule.disableCaching("static.example.com");
    assertEquals("static.example.com", rule.getMatch().getUrlContains());
  }

  @Test
  void replaceResponseBody_setsBodyReplaceWithFrom_() throws Exception {
    MitmProxyRule rule = MitmProxyRule.replaceResponseBody("api.example.com", "old", "new");
    String json = om.writeValueAsString(rule);
    // from_ is the JSON field name due to @JsonProperty("from_")
    assertTrue(json.contains("\"from_\""), "Expected 'from_' field in JSON: " + json);
    assertTrue(json.contains("\"old\""));
    assertTrue(json.contains("\"new\""));
  }

  @Test
  void setRequestHeaders_setsHeaders() {
    MitmProxyRule rule = MitmProxyRule.setRequestHeaders("api.example.com", Map.of("X-Token", "abc"));
    assertEquals(Map.of("X-Token", "abc"), rule.getAction().getModifyRequest().getHeaders().getSet());
  }

  @Test
  void setQueryParams_setsParams() {
    MitmProxyRule rule = MitmProxyRule.setQueryParams("/search", Map.of("lang", "en"));
    assertEquals(Map.of("lang", "en"), rule.getAction().getModifyRequest().getParams().getSet());
  }

  @Test
  void replaceImageBase64_setsBase64Body() {
    MitmProxyRule rule = MitmProxyRule.replaceImageBase64("avatars.example.com", "AAAA", "image/png");
    assertEquals("AAAA", rule.getAction().getModifyResponse().getBodyBase64());
    assertEquals(200, rule.getAction().getModifyResponse().getStatusCode());
    assertEquals("image/png", rule.getAction().getModifyResponse().getHeaders().getSet().get("Content-Type"));
  }

  @Test
  void rule_enabledByDefault() {
    MitmProxyRule rule = MitmProxyRule.block("/ads");
    assertTrue(rule.isEnabled());
  }

  @Test
  void rule_priorityZeroByDefault() {
    MitmProxyRule rule = MitmProxyRule.block("/ads");
    assertEquals(0, rule.getPriority());
  }

  @Test
  void ruleFileSpec_isBinary_detectsExtensions() {
    assertTrue(MitmProxyRuleFileSpec.isBinary("image.png"));
    assertTrue(MitmProxyRuleFileSpec.isBinary("font.woff2"));
    assertTrue(MitmProxyRuleFileSpec.isBinary("archive.zip"));
    assertFalse(MitmProxyRuleFileSpec.isBinary("data.json"));
    assertFalse(MitmProxyRuleFileSpec.isBinary("body.txt"));
  }
}

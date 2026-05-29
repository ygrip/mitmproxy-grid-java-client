package io.github.ygrip.mitmproxy.grid.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import io.github.ygrip.mitmproxy.grid.client.exception.MitmProxyGridHttpException;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyCreateInstanceResponse;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyHealthResponse;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyInstanceSummary;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyMessageResponse;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyRule;

@WireMockTest
class MitmProxyGridClientTest {

  private MitmProxyGridClient client(WireMockRuntimeInfo wm) {
    return new MitmProxyGridClient("http://localhost:" + wm.getHttpPort());
  }

  // ── Health ─────────────────────────────────────────────────────────

  @Test
  void health_returnsDeserializedResponse(WireMockRuntimeInfo wm) {
    stubFor(get("/health").willReturn(okJson("""
        {"status":"up","instances":2,"usedPorts":[8101,8102],"availableSlots":8,"portRange":"8100-8199","defaultTtl":300}
        """)));

    MitmProxyHealthResponse h = client(wm).health();
    assertTrue(h.isHealthy());
    assertEquals("up", h.getStatus());
    assertEquals(8, h.getAvailableSlots());
  }

  @Test
  void healthRaw_returnsRawString(WireMockRuntimeInfo wm) {
    stubFor(get("/health").willReturn(okJson("{\"status\":\"up\"}")));
    assertTrue(client(wm).healthRaw().contains("up"));
  }

  @Test
  void isReady_trueWhenStatusUp(WireMockRuntimeInfo wm) {
    stubFor(get("/health").willReturn(okJson("{\"status\":\"up\"}")));
    assertTrue(client(wm).isReady());
  }

  @Test
  void isReady_trueWhenHealthReachableButStatusUnknown(WireMockRuntimeInfo wm) {
    // Server is reachable but returns non-"up" status — should still be treated as ready
    stubFor(get("/health").willReturn(okJson("{\"status\":\"degraded\"}")));
    assertTrue(client(wm).isReady());
  }

  @Test
  void isReady_falseWhenServerUnreachable() {
    // No WireMock stub — point at a port with nothing listening
    MitmProxyGridClient c = MitmProxyGridClient.builder()
        .baseUrl("http://localhost:19998/")
        .connectTimeout(Duration.ofMillis(200))
        .requestTimeout(Duration.ofMillis(200))
        .maxRetries(1)
        .build();
    assertFalse(c.isReady());
  }

  // ── URL normalization ──────────────────────────────────────────────

  @Test
  void urlNormalization_noTrailingSlash(WireMockRuntimeInfo wm) {
    // Client built WITHOUT trailing slash should still work
    MitmProxyGridClient c = new MitmProxyGridClient("http://localhost:" + wm.getHttpPort());
    stubFor(get("/health").willReturn(okJson("{\"status\":\"up\"}")));
    assertDoesNotThrow(c::healthRaw);
  }

  @Test
  void urlNormalization_nullUrl_usesDefault(WireMockRuntimeInfo wm) {
    // Config built with null baseUri falls back to default
    MitmProxyGridClientConfig config = new MitmProxyGridClientConfig(
        null,
        Duration.ofSeconds(5),
        Duration.ofSeconds(5),
        1,
        Duration.ofMillis(50),
        null);
    assertEquals("http://localhost:8090/", config.baseUri().toString());
  }

  // ── Instances ──────────────────────────────────────────────────────

  @Test
  void createInstance_withTtl(WireMockRuntimeInfo wm) {
    stubFor(post("/instances?ttl=300").willReturn(okJson("""
        {"instanceId":"abc-123","port":8101,"status":"running","ttl":300,"expiresAt":"2026-01-01T00:00:00Z"}
        """)));

    MitmProxyCreateInstanceResponse resp = client(wm).createInstance(300);
    assertEquals("abc-123", resp.getInstanceId());
    assertEquals(8101, resp.getPort());
  }

  @Test
  void createInstance_withoutTtl(WireMockRuntimeInfo wm) {
    stubFor(post("/instances").willReturn(okJson("""
        {"instanceId":"no-ttl-id","port":8102,"status":"running","ttl":0}
        """)));

    MitmProxyCreateInstanceResponse resp = client(wm).createInstance();
    assertEquals("no-ttl-id", resp.getInstanceId());
  }

  @Test
  void listInstances_returnsEmptyListOnNullBody(WireMockRuntimeInfo wm) {
    stubFor(get("/instances").willReturn(okJson("null")));
    List<MitmProxyInstanceSummary> list = client(wm).listInstances();
    assertNotNull(list);
    assertTrue(list.isEmpty());
  }

  @Test
  void destroyInstance_withCleanup(WireMockRuntimeInfo wm) {
    stubFor(delete("/instances/abc-123?cleanup=true")
        .willReturn(okJson("{\"status\":\"ok\",\"message\":\"destroyed\"}")));

    MitmProxyMessageResponse resp = client(wm).destroyInstance("abc-123", true);
    assertEquals("ok", resp.getStatus());
  }

  @Test
  void destroyInstance_withoutCleanup(WireMockRuntimeInfo wm) {
    stubFor(delete("/instances/abc-123")
        .willReturn(okJson("{\"status\":\"ok\",\"message\":\"destroyed\"}")));

    assertDoesNotThrow(() -> client(wm).destroyInstance("abc-123"));
  }

  @Test
  void renewInstance_withTtl(WireMockRuntimeInfo wm) {
    stubFor(post("/instances/abc-123/renew?ttl=600")
        .willReturn(okJson("{\"status\":\"ok\",\"ttl\":600,\"remainingSeconds\":600.0}")));

    var resp = client(wm).renewInstance("abc-123", 600);
    assertEquals("ok", resp.getStatus());
  }

  // ── Rules ──────────────────────────────────────────────────────────

  @Test
  void createRule_callsPostAndDeserializes(WireMockRuntimeInfo wm) {
    stubFor(post("/instances/abc-123/rules")
        .willReturn(okJson("{\"status\":\"ok\",\"message\":\"rule created\"}")));

    MitmProxyMessageResponse resp = client(wm).createRule("abc-123", MitmProxyRule.block("/ads"));
    assertEquals("ok", resp.getStatus());
    verify(postRequestedFor(urlEqualTo("/instances/abc-123/rules")));
  }

  @Test
  void clearAllRules_deletesInReverseIndexOrder(WireMockRuntimeInfo wm) {
    stubFor(get("/instances/abc-123/rules").willReturn(okJson("""
        [
          {"index":0,"enabled":true,"priority":0,"match":{"urlContains":"foo"}},
          {"index":1,"enabled":true,"priority":0,"match":{"urlContains":"bar"}}
        ]
        """)));
    stubFor(delete(urlMatching("/instances/abc-123/rules/.*"))
        .willReturn(okJson("{\"status\":\"ok\"}")));

    client(wm).clearAllRules("abc-123");

    verify(deleteRequestedFor(urlEqualTo("/instances/abc-123/rules/1")));
    verify(deleteRequestedFor(urlEqualTo("/instances/abc-123/rules/0")));
  }

  @Test
  void getCaCertificate_returnsRawPem(WireMockRuntimeInfo wm) {
    stubFor(get("/instances/abc-123/cert")
        .willReturn(ok("-----BEGIN CERTIFICATE-----\nABC\n-----END CERTIFICATE-----")));

    String pem = client(wm).getCaCertificate("abc-123");
    assertTrue(pem.contains("BEGIN CERTIFICATE"));
  }

  // ── Error handling ─────────────────────────────────────────────────

  @Test
  void http404_throwsMitmProxyGridHttpException(WireMockRuntimeInfo wm) {
    stubFor(get("/health").willReturn(aResponse().withStatus(404).withBody("not found")));

    MitmProxyGridHttpException ex = assertThrows(
        MitmProxyGridHttpException.class,
        () -> client(wm).health());

    assertEquals(404, ex.statusCode());
    assertEquals("GET", ex.method());
    assertEquals("health", ex.path());
    assertTrue(ex.responseBody().contains("not found"));
  }

  @Test
  void http4xx_isNotRetried(WireMockRuntimeInfo wm) {
    stubFor(get("/health").willReturn(aResponse().withStatus(401).withBody("unauthorized")));

    // Should fail immediately, not retry
    assertThrows(MitmProxyGridHttpException.class, () -> client(wm).health());
    verify(1, getRequestedFor(urlEqualTo("/health")));
  }

  // ── Retry ──────────────────────────────────────────────────────────

  @Test
  void retry_succeeds_afterTransient500(WireMockRuntimeInfo wm) {
    stubFor(get("/health")
        .inScenario("retry-test")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("ok")
        .willReturn(aResponse().withStatus(500)));
    stubFor(get("/health")
        .inScenario("retry-test")
        .whenScenarioStateIs("ok")
        .willReturn(okJson("{\"status\":\"up\"}")));

    MitmProxyGridClient c = MitmProxyGridClient.builder()
        .baseUrl("http://localhost:" + wm.getHttpPort() + "/")
        .maxRetries(3)
        .retryBaseDelay(Duration.ofMillis(5))
        .build();

    assertDoesNotThrow(c::health);
    verify(2, getRequestedFor(urlEqualTo("/health")));
  }

  @Test
  void retry_exhausted_throws(WireMockRuntimeInfo wm) {
    stubFor(get("/health").willReturn(aResponse().withStatus(503)));

    MitmProxyGridClient c = MitmProxyGridClient.builder()
        .baseUrl("http://localhost:" + wm.getHttpPort() + "/")
        .maxRetries(2)
        .retryBaseDelay(Duration.ofMillis(5))
        .build();

    MitmProxyGridHttpException ex = assertThrows(
        MitmProxyGridHttpException.class,
        c::health);

    assertEquals(503, ex.statusCode());
    verify(2, getRequestedFor(urlEqualTo("/health")));
  }
}

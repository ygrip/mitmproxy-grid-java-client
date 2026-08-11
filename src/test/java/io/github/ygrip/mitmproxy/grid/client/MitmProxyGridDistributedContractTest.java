package io.github.ygrip.mitmproxy.grid.client;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import io.github.ygrip.mitmproxy.grid.client.exception.MitmProxyGridCompatibilityException;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyCreateInstanceResponse;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyEndpoint;

@WireMockTest
class MitmProxyGridDistributedContractTest {

  private MitmProxyGridClient client(WireMockRuntimeInfo wm) {
    return new MitmProxyGridClient("http://localhost:" + wm.getHttpPort());
  }

  @Test
  void compatibilityDeclaresExactSupportedGridImage() {
    assertEquals("2.1.0", MitmProxyGridCompatibility.SUPPORTED_GRID_VERSION);
    assertEquals("2", MitmProxyGridCompatibility.SUPPORTED_API_VERSION);
    assertEquals(
        "ghcr.io/ygrip/mitmproxy-grid:2.1.0",
        MitmProxyGridCompatibility.SUPPORTED_IMAGE);
  }

  @Test
  void requireCompatibleGridAcceptsSupportedRelease(WireMockRuntimeInfo wm) {
    stubFor(get("/health").willReturn(okJson("""
        {
          "status":"UP",
          "instances":0,
          "availableSlots":40,
          "portRange":"distributed",
          "defaultTtl":1800,
          "gridVersion":"2.1.0",
          "apiVersion":"2",
          "mode":"coordinator"
        }
        """)));

    assertEquals("2.1.0", client(wm).requireCompatibleGrid().getGridVersion());
  }

  @Test
  void requireCompatibleGridRejectsDifferentGridRelease(WireMockRuntimeInfo wm) {
    stubFor(get("/health").willReturn(okJson("""
        {
          "status":"UP",
          "instances":0,
          "availableSlots":40,
          "portRange":"distributed",
          "defaultTtl":1800,
          "gridVersion":"2.2.0",
          "apiVersion":"2",
          "mode":"coordinator"
        }
        """)));

    assertThrows(MitmProxyGridCompatibilityException.class, () -> client(wm).requireCompatibleGrid());
  }

  @Test
  void createInstanceUsesAdvertisedWorkerProxyEndpoint(WireMockRuntimeInfo wm) {
    stubFor(post("/instances").willReturn(okJson("""
        {
          "instanceId":"abc-123",
          "port":10003,
          "proxyHost":"mitm-worker-3",
          "proxyPort":10003,
          "proxyUrl":"http://mitm-worker-3:10003",
          "workerId":"worker-3",
          "status":"running",
          "ttl":1800,
          "expiresAt":"2026-08-11T10:00:00Z"
        }
        """)));

    MitmProxyCreateInstanceResponse instance = client(wm).createInstance();
    MitmProxyEndpoint endpoint = client(wm).resolveProxyEndpoint(instance);

    assertEquals("worker-3", instance.getWorkerId());
    assertEquals("mitm-worker-3", endpoint.host());
    assertEquals(10003, endpoint.port());
    assertEquals("http://mitm-worker-3:10003", endpoint.url());
  }

  @Test
  void legacyCreateResponseFallsBackToGridHost(WireMockRuntimeInfo wm) {
    stubFor(post("/instances").willReturn(okJson("""
        {
          "instanceId":"legacy",
          "port":10001,
          "status":"running",
          "ttl":1800,
          "expiresAt":"2026-08-11T10:00:00Z"
        }
        """)));

    MitmProxyCreateInstanceResponse instance = client(wm).createInstance();
    MitmProxyEndpoint endpoint = client(wm).resolveProxyEndpoint(instance);

    assertEquals("localhost", endpoint.host());
    assertEquals(10001, endpoint.port());
    assertTrue(endpoint.url().endsWith(":10001"));
  }
}

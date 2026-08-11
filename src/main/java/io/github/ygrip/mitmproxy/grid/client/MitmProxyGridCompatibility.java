package io.github.ygrip.mitmproxy.grid.client;

import io.github.ygrip.mitmproxy.grid.client.exception.MitmProxyGridCompatibilityException;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyHealthResponse;

/**
 * Compatibility contract between this SDK release and mitmproxy-grid.
 */
public final class MitmProxyGridCompatibility {

  public static final String SUPPORTED_GRID_VERSION = "2.1.0";
  public static final String SUPPORTED_API_VERSION = "2";
  public static final String SUPPORTED_IMAGE =
      "ghcr.io/ygrip/mitmproxy-grid:" + SUPPORTED_GRID_VERSION;

  private MitmProxyGridCompatibility() {
  }

  public static boolean isCompatible(MitmProxyHealthResponse health) {
    return health != null
        && SUPPORTED_API_VERSION.equals(health.getApiVersion())
        && SUPPORTED_GRID_VERSION.equals(health.getGridVersion());
  }

  public static void requireCompatible(MitmProxyHealthResponse health) {
    if (isCompatible(health)) {
      return;
    }

    String actualGrid = health == null ? null : health.getGridVersion();
    String actualApi = health == null ? null : health.getApiVersion();
    throw new MitmProxyGridCompatibilityException(
        "Unsupported mitmproxy-grid version. Expected grid=" + SUPPORTED_GRID_VERSION
            + " api=" + SUPPORTED_API_VERSION
            + ", actual grid=" + actualGrid
            + " api=" + actualApi);
  }
}

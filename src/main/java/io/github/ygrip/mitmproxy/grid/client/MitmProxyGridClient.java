package io.github.ygrip.mitmproxy.grid.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.ygrip.mitmproxy.grid.client.exception.MitmProxyGridException;
import io.github.ygrip.mitmproxy.grid.client.exception.MitmProxyGridHttpException;
import io.github.ygrip.mitmproxy.grid.client.exception.MitmProxyGridTimeoutException;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyCreateInstanceResponse;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyHealthResponse;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyInstanceDetail;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyInstanceSummary;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyMessageResponse;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyRenewResponse;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyRule;
import io.github.ygrip.mitmproxy.grid.client.model.MitmProxyRuleResponse;

/**
 * HTTP client for the MitmProxy Grid REST API v2.
 * Thread-safe and stateless — safe to share across tests.
 *
 * <pre>{@code
 * MitmProxyGridClient client = MitmProxyGridClient.builder()
 *     .baseUrl("http://localhost:8090")
 *     .build();
 *
 * client.waitUntilReady(Duration.ofSeconds(30), Duration.ofSeconds(1));
 * MitmProxyCreateInstanceResponse instance = client.createInstance(300);
 * client.createRule(instance.getInstanceId(), MitmProxyRule.disableCaching());
 * }</pre>
 */
public class MitmProxyGridClient {

  private static final Logger LOG = Logger.getLogger(MitmProxyGridClient.class.getName());

  private final MitmProxyGridClientConfig config;
  private final HttpClient httpClient;
  private final JsonCodec codec;

  public MitmProxyGridClient(String apiBaseUrl) {
    this(MitmProxyGridClientConfig.defaults(apiBaseUrl));
  }

  public MitmProxyGridClient(MitmProxyGridClientConfig config) {
    this(config, HttpClient.newBuilder()
        .connectTimeout(config.connectTimeout())
        .build());
  }

  public MitmProxyGridClient(MitmProxyGridClientConfig config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
    this.codec = new JsonCodec(config.objectMapper());
  }

  public static MitmProxyGridClientBuilder builder() {
    return new MitmProxyGridClientBuilder();
  }

  // ── Health ─────────────────────────────────────────────────────────

  public MitmProxyHealthResponse health() {
    String response = sendWithRetry("GET", "health", null);
    LOG.fine(() -> "health response: " + response);
    return codec.read(response, MitmProxyHealthResponse.class);
  }

  public String healthRaw() {
    return sendWithRetry("GET", "health", null);
  }

  public boolean isReady() {
    try {
      MitmProxyHealthResponse h = health();
      if (h == null) return false;
      if (h.isHealthy()) return true;
      LOG.fine(() -> "health reachable but status='" + h.getStatus() + "'; treating as ready");
      return true;
    } catch (Exception e) {
      LOG.finest(() -> "health check failed: " + e.getMessage());
      return false;
    }
  }

  public boolean waitUntilReady(Duration timeout, Duration poll) {
    return waitUntilReady(timeout.toMillis(), poll.toMillis());
  }

  public boolean waitUntilReady(long timeoutMs, long pollMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (isReady()) return true;
      try {
        Thread.sleep(pollMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return isReady();
  }

  // ── Instances ──────────────────────────────────────────────────────

  public MitmProxyCreateInstanceResponse createInstance(Integer ttl) {
    String path = ttl != null ? "instances?ttl=" + ttl : "instances";
    return codec.read(sendWithRetry("POST", path, null), MitmProxyCreateInstanceResponse.class);
  }

  public MitmProxyCreateInstanceResponse createInstance() {
    return createInstance(null);
  }

  public List<MitmProxyInstanceSummary> listInstances() {
    String response = sendWithRetry("GET", "instances", null);
    List<MitmProxyInstanceSummary> list = codec.read(response, new TypeReference<>() {});
    return list != null ? list : Collections.emptyList();
  }

  public MitmProxyInstanceDetail getInstance(String instanceId) {
    return codec.read(sendWithRetry("GET", "instances/" + instanceId, null), MitmProxyInstanceDetail.class);
  }

  public MitmProxyMessageResponse destroyInstance(String instanceId, boolean cleanup) {
    String path = cleanup
        ? "instances/" + instanceId + "?cleanup=true"
        : "instances/" + instanceId;
    return codec.read(sendWithRetry("DELETE", path, null), MitmProxyMessageResponse.class);
  }

  public MitmProxyMessageResponse destroyInstance(String instanceId) {
    return destroyInstance(instanceId, false);
  }

  public MitmProxyRenewResponse renewInstance(String instanceId, Integer ttl) {
    String path = ttl != null
        ? "instances/" + instanceId + "/renew?ttl=" + ttl
        : "instances/" + instanceId + "/renew";
    return codec.read(sendWithRetry("POST", path, null), MitmProxyRenewResponse.class);
  }

  public MitmProxyRenewResponse renewInstance(String instanceId) {
    return renewInstance(instanceId, null);
  }

  public int destroyAllInstances(boolean cleanup) {
    int destroyed = 0;
    try {
      List<MitmProxyInstanceSummary> instances = listInstances();
      LOG.info("Destroying all MitmProxy instances (" + instances.size() + " found, cleanup=" + cleanup + ")");
      for (MitmProxyInstanceSummary instance : instances) {
        try {
          destroyInstance(instance.getInstanceId(), cleanup);
          destroyed++;
        } catch (Exception e) {
          LOG.warning("Failed to destroy instance " + instance.getInstanceId() + ": " + e.getMessage());
        }
      }
    } catch (Exception e) {
      LOG.warning("Failed to list instances during destroyAll: " + e.getMessage());
    }
    return destroyed;
  }

  public int destroyAllInstances() {
    return destroyAllInstances(true);
  }

  // ── Rules ──────────────────────────────────────────────────────────

  public MitmProxyMessageResponse createRule(String instanceId, MitmProxyRule rule) {
    String body = codec.write(rule);
    LOG.fine(() -> "createRule on " + instanceId + ": " + body);
    String response = sendWithRetry("POST", "instances/" + instanceId + "/rules", body);
    LOG.fine(() -> "createRule response: " + response);
    return codec.read(response, MitmProxyMessageResponse.class);
  }

  public List<MitmProxyRuleResponse> listRules(String instanceId) {
    String response = sendWithRetry("GET", "instances/" + instanceId + "/rules", null);
    List<MitmProxyRuleResponse> rules = codec.read(response, new TypeReference<>() {});
    return rules != null ? rules : Collections.emptyList();
  }

  public MitmProxyMessageResponse deleteRule(String instanceId, int ruleIndex) {
    return codec.read(
        sendWithRetry("DELETE", "instances/" + instanceId + "/rules/" + ruleIndex, null),
        MitmProxyMessageResponse.class);
  }

  public MitmProxyMessageResponse toggleRule(String instanceId, int ruleIndex) {
    return codec.read(
        sendWithRetry("PATCH", "instances/" + instanceId + "/rules/" + ruleIndex + "/toggle", null),
        MitmProxyMessageResponse.class);
  }

  /** Remove all rules from an instance in reverse index order. */
  public void clearAllRules(String instanceId) {
    List<MitmProxyRuleResponse> rules = listRules(instanceId);
    for (int i = rules.size() - 1; i >= 0; i--) {
      deleteRule(instanceId, rules.get(i).getIndex());
    }
  }

  // ── Certificate ────────────────────────────────────────────────────

  public String getCaCertificate(String instanceId) {
    return sendWithRetry("GET", "instances/" + instanceId + "/cert", null);
  }

  // ── HTTP transport ─────────────────────────────────────────────────

  private String sendWithRetry(String method, String path, String body) {
    MitmProxyGridException lastException = null;
    for (int attempt = 0; attempt < config.maxRetries(); attempt++) {
      try {
        return send(method, path, body);
      } catch (MitmProxyGridHttpException e) {
        if (e.statusCode() < 500) throw e;  // don't retry 4xx
        lastException = e;
      } catch (MitmProxyGridException e) {
        lastException = e;
      }
      if (attempt < config.maxRetries() - 1) {
        long backoff = config.retryBaseDelay().toMillis() * (1L << attempt);
        LOG.fine("API call failed (attempt " + (attempt + 1) + "), retrying in " + backoff + "ms");
        try {
          Thread.sleep(backoff);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw lastException;
        }
      }
    }
    throw lastException;
  }

  private String send(String method, String path, String body) {
    URI uri = config.baseUri().resolve(path);
    HttpRequest.BodyPublisher publisher = body == null
        ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(uri)
        .timeout(config.requestTimeout())
        .header("Content-Type", "application/json")
        .header("Accept", "application/json");

    switch (method.toUpperCase()) {
      case "GET"    -> builder.GET();
      case "DELETE" -> builder.DELETE();
      case "POST"   -> builder.POST(publisher);
      case "PUT"    -> builder.PUT(publisher);
      case "PATCH"  -> builder.method("PATCH", publisher);
      default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
    }

    try {
      HttpResponse<String> response = httpClient.send(
          builder.build(),
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

      if (response.statusCode() >= 400) {
        throw new MitmProxyGridHttpException(response.statusCode(), method, path, response.body());
      }
      return response.body();

    } catch (MitmProxyGridHttpException e) {
      throw e;
    } catch (HttpTimeoutException e) {
      throw new MitmProxyGridTimeoutException(
          "Timeout on MitmProxy Grid API " + method + " " + path, e);
    } catch (IOException e) {
      throw new MitmProxyGridException(
          "I/O error on MitmProxy Grid API " + method + " " + path + ": " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MitmProxyGridException(
          "Interrupted while calling MitmProxy Grid API " + method + " " + path, e);
    }
  }
}

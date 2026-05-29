package io.github.ygrip.mitmproxy.grid.client.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.logging.Logger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rule specification that supports file-based body content.
 * SDK-native replacement for Testara's {@code ProxyRuleCreation} that depends only on JDK APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitmProxyRuleFileSpec {

  private static final Logger LOG = Logger.getLogger(MitmProxyRuleFileSpec.class.getName());

  private static final String[] BINARY_EXTENSIONS = {
      ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".bmp", ".tiff", ".avif",
      ".woff", ".woff2", ".ttf", ".eot", ".pdf", ".zip", ".gz", ".br"
  };

  @Builder.Default
  private boolean enabled = true;
  @Builder.Default
  private int priority = 0;
  private MitmProxyRuleMatch match;
  private MitmProxyRuleAction action;

  /** Path to a file whose content replaces the response body. Binary files are base64-encoded. */
  private String responseBodyFile;

  /** Path to a file whose content replaces the request body. */
  private String requestBodyFile;

  public MitmProxyRule toMitmProxyRule(Path baseFolder) {
    return MitmProxyRule.builder()
        .enabled(enabled)
        .priority(priority)
        .match(match)
        .action(resolveAction(baseFolder))
        .build();
  }

  public MitmProxyRule toMitmProxyRule() {
    return toMitmProxyRule(null);
  }

  private MitmProxyRuleAction resolveAction(Path baseFolder) {
    if (action == null) {
      return MitmProxyRuleAction.builder().build();
    }

    MitmProxyResponseModification responseModification = action.getModifyResponse() != null
        ? copyResponse(action.getModifyResponse()) : null;

    MitmProxyRequestModification requestModification = action.getModifyRequest() != null
        ? copyRequest(action.getModifyRequest()) : null;

    if (responseBodyFile != null && !responseBodyFile.isBlank()) {
      if (responseModification == null) {
        responseModification = MitmProxyResponseModification.builder().build();
      }
      Path file = resolve(responseBodyFile, baseFolder);
      if (file != null && Files.exists(file)) {
        if (isBinary(responseBodyFile)) {
          responseModification.setBodyBase64(readBase64(file));
        } else {
          responseModification.setBody(readText(file));
        }
      } else {
        LOG.warning(() -> "Response body file not found: " + responseBodyFile);
      }
    }

    if (requestBodyFile != null && !requestBodyFile.isBlank()) {
      if (requestModification == null) {
        requestModification = MitmProxyRequestModification.builder().build();
      }
      Path file = resolve(requestBodyFile, baseFolder);
      if (file != null && Files.exists(file)) {
        requestModification.setBody(readText(file));
      } else {
        LOG.warning(() -> "Request body file not found: " + requestBodyFile);
      }
    }

    return MitmProxyRuleAction.builder()
        .modifyRequest(requestModification)
        .modifyResponse(responseModification)
        .build();
  }

  private static MitmProxyResponseModification copyResponse(MitmProxyResponseModification src) {
    return MitmProxyResponseModification.builder()
        .statusCode(src.getStatusCode())
        .headers(src.getHeaders())
        .body(src.getBody())
        .bodyBase64(src.getBodyBase64())
        .bodyReplace(src.getBodyReplace())
        .build();
  }

  private static MitmProxyRequestModification copyRequest(MitmProxyRequestModification src) {
    return MitmProxyRequestModification.builder()
        .headers(src.getHeaders())
        .params(src.getParams())
        .body(src.getBody())
        .bodyReplace(src.getBodyReplace())
        .build();
  }

  private static Path resolve(String filePath, Path baseFolder) {
    Path p = Path.of(filePath);
    if (p.isAbsolute() || baseFolder == null) return p;
    return baseFolder.resolve(filePath);
  }

  private static String readText(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.severe(() -> "Failed to read file: " + file + " — " + e.getMessage());
      return null;
    }
  }

  private static String readBase64(Path file) {
    try {
      return Base64.getEncoder().encodeToString(Files.readAllBytes(file));
    } catch (IOException e) {
      LOG.severe(() -> "Failed to read binary file: " + file + " — " + e.getMessage());
      return null;
    }
  }

  public static boolean isBinary(String filePath) {
    String lower = filePath.toLowerCase();
    for (String ext : BINARY_EXTENSIONS) {
      if (lower.endsWith(ext)) return true;
    }
    return false;
  }
}

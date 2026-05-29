package io.github.ygrip.mitmproxy.grid.client.exception;

public class MitmProxyGridException extends RuntimeException {

  public MitmProxyGridException(String message) {
    super(message);
  }

  public MitmProxyGridException(String message, Throwable cause) {
    super(message, cause);
  }
}

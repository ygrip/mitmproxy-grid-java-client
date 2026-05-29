package io.github.ygrip.mitmproxy.grid.client.exception;

public class MitmProxyGridHttpException extends MitmProxyGridException {

  private final int statusCode;
  private final String method;
  private final String path;
  private final String responseBody;

  public MitmProxyGridHttpException(int statusCode, String method, String path, String responseBody) {
    super(String.format("MitmProxy Grid API %s %s returned %d: %s", method, path, statusCode, responseBody));
    this.statusCode = statusCode;
    this.method = method;
    this.path = path;
    this.responseBody = responseBody;
  }

  public int statusCode() { return statusCode; }
  public String method() { return method; }
  public String path() { return path; }
  public String responseBody() { return responseBody; }
}

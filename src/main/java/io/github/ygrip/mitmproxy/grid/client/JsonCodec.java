package io.github.ygrip.mitmproxy.grid.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ygrip.mitmproxy.grid.client.exception.MitmProxyGridSerializationException;

final class JsonCodec {

  private final ObjectMapper objectMapper;

  JsonCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  <T> T read(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new MitmProxyGridSerializationException("Failed to deserialize response as " + type.getSimpleName(), e);
    }
  }

  <T> T read(String json, TypeReference<T> typeRef) {
    try {
      return objectMapper.readValue(json, typeRef);
    } catch (JsonProcessingException e) {
      throw new MitmProxyGridSerializationException("Failed to deserialize response", e);
    }
  }

  String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new MitmProxyGridSerializationException("Failed to serialize request", e);
    }
  }
}

package com.healthline.backend.identity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stores an embedding as a comma-separated list of {@code Double.toString} values. Java guarantees
 * {@code Double.parseDouble(Double.toString(d)) == d} for every double, so this round-trips a
 * 512-dimension embedding without precision loss.
 */
@Converter
class EmbeddingConverter implements AttributeConverter<List<Double>, String> {

  @Override
  public String convertToDatabaseColumn(List<Double> embedding) {
    return embedding.stream().map(String::valueOf).collect(Collectors.joining(","));
  }

  @Override
  public List<Double> convertToEntityAttribute(String csv) {
    return Stream.of(csv.split(",")).map(Double::parseDouble).collect(Collectors.toList());
  }
}

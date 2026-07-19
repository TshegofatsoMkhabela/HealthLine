package com.healthline.backend.identity;

import java.util.List;

public interface IdentityAiServiceClient {

  List<Double> embed(String selfieBlob);

  CompareResult compare(String selfieBlob, List<Double> storedEmbedding);
}

package com.healthline.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

  @Test
  void leavesAnAlreadyValidJdbcUrlUnchanged() {
    var result =
        DatabaseUrlEnvironmentPostProcessor.normalize(
            "jdbc:postgresql://localhost:5432/healthline");

    assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/healthline");
    assertThat(result.username()).isNull();
    assertThat(result.password()).isNull();
  }

  @Test
  void convertsSupabaseStylePostgresUrlToJdbcForm() {
    var result =
        DatabaseUrlEnvironmentPostProcessor.normalize(
            "postgresql://healthline_user:s3cret@db.supabase.co:5432/postgres");

    assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://db.supabase.co:5432/postgres");
    assertThat(result.username()).isEqualTo("healthline_user");
    assertThat(result.password()).isEqualTo("s3cret");
  }

  @Test
  void convertsShortPostgresSchemeVariant() {
    var result =
        DatabaseUrlEnvironmentPostProcessor.normalize("postgres://user:pass@example.com:6543/mydb");

    assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://example.com:6543/mydb");
  }

  @Test
  void handlesUrlWithNoEmbeddedCredentials() {
    var result =
        DatabaseUrlEnvironmentPostProcessor.normalize("postgresql://example.com:5432/mydb");

    assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://example.com:5432/mydb");
    assertThat(result.username()).isNull();
    assertThat(result.password()).isNull();
  }

  @Test
  void preservesSslmodeQueryParameter() {
    var result =
        DatabaseUrlEnvironmentPostProcessor.normalize(
            "postgresql://user:pass@db.supabase.co:5432/postgres?sslmode=require");

    assertThat(result.jdbcUrl())
        .isEqualTo("jdbc:postgresql://db.supabase.co:5432/postgres?sslmode=require");
  }

  @Test
  void preservesPgbouncerAndMultipleQueryParameters() {
    var result =
        DatabaseUrlEnvironmentPostProcessor.normalize(
            "postgresql://user:pass@db.supabase.co:6543/postgres?pgbouncer=true&sslmode=require");

    assertThat(result.jdbcUrl())
        .isEqualTo("jdbc:postgresql://db.supabase.co:6543/postgres?pgbouncer=true&sslmode=require");
  }

  @Test
  void rejectsAnUnrecognizedScheme() {
    assertThatThrownBy(
            () -> DatabaseUrlEnvironmentPostProcessor.normalize("mysql://user:pass@host/db"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mysql");
  }
}

package com.healthline.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Managed Postgres providers (Supabase, Render, Heroku) hand out connection strings as
 * postgres://user:pass@host:port/db, but Spring/Hikari require a jdbc:postgresql:// URL to resolve
 * the driver. Binding spring.datasource.url straight to DATABASE_URL therefore only works if
 * whoever pastes it into Render's dashboard also manually reformats it — a step with no automated
 * check, and the exact way this broke before this class existed.
 *
 * <p>This runs before Spring binds any datasource properties, so the datasource always sees a
 * normalized jdbc: URL regardless of which format the raw env var actually contains.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String DATABASE_URL_ENV = "DATABASE_URL";
  private static final String DATABASE_USER_ENV = "DATABASE_USER";
  private static final String DATABASE_PASSWORD_ENV = "DATABASE_PASSWORD";
  private static final String PROPERTY_SOURCE_NAME = "databaseUrlNormalized";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String rawUrl = environment.getProperty(DATABASE_URL_ENV);
    if (rawUrl == null || rawUrl.isBlank()) {
      return;
    }

    NormalizedDatabaseUrl normalized = normalize(rawUrl);

    Map<String, Object> properties = new HashMap<>();
    properties.put("spring.datasource.url", normalized.jdbcUrl());

    // render.yaml also exposes DATABASE_USER/DATABASE_PASSWORD as separately configurable
    // secrets. Those are the more discoverable, intentionally-set surface in Render's
    // dashboard, so they win; credentials embedded in the URL are only a fallback for when
    // they're left unset.
    putIfNotExplicitlySet(
        properties,
        environment,
        "spring.datasource.username",
        DATABASE_USER_ENV,
        normalized.username());
    putIfNotExplicitlySet(
        properties,
        environment,
        "spring.datasource.password",
        DATABASE_PASSWORD_ENV,
        normalized.password());

    environment
        .getPropertySources()
        .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
  }

  private static void putIfNotExplicitlySet(
      Map<String, Object> properties,
      ConfigurableEnvironment environment,
      String propertyKey,
      String envVarName,
      String urlValue) {
    if (urlValue != null && environment.getProperty(envVarName) == null) {
      properties.put(propertyKey, urlValue);
    }
  }

  /**
   * Pure, dependency-free so it can be unit tested without a Spring context. Accepts either an
   * already-valid jdbc: URL (returned unchanged, no embedded credentials extracted, since a jdbc:
   * URL doesn't carry userinfo the same way) or a postgres(ql):// URI with optional embedded
   * user:pass.
   */
  static NormalizedDatabaseUrl normalize(String rawUrl) {
    if (rawUrl.startsWith("jdbc:")) {
      return new NormalizedDatabaseUrl(rawUrl, null, null);
    }

    URI uri;
    try {
      uri = new URI(rawUrl);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(
          "DATABASE_URL is neither a valid jdbc: URL nor a parseable URI: " + rawUrl, e);
    }

    String scheme = uri.getScheme();
    if (scheme == null || !(scheme.equals("postgres") || scheme.equals("postgresql"))) {
      throw new IllegalArgumentException(
          "DATABASE_URL must start with jdbc:, postgres://, or postgresql:// — got scheme '"
              + scheme
              + "'");
    }

    // The query string carries connection flags managed Postgres providers rely on — e.g.
    // ?sslmode=require (encrypted connection) or ?pgbouncer=true (Supabase's connection-pooler
    // mode). Dropping it silently would downgrade to an unencrypted connection or break pooler
    // compatibility, so it's preserved as-is onto the reconstructed jdbc: URL.
    String jdbcUrl =
        "jdbc:postgresql://"
            + uri.getHost()
            + (uri.getPort() != -1 ? ":" + uri.getPort() : "")
            + uri.getPath()
            + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");

    String username = null;
    String password = null;
    String userInfo = uri.getUserInfo();
    if (userInfo != null && userInfo.contains(":")) {
      String[] parts = userInfo.split(":", 2);
      username = parts[0];
      password = parts[1];
    }

    return new NormalizedDatabaseUrl(jdbcUrl, username, password);
  }

  record NormalizedDatabaseUrl(String jdbcUrl, String username, String password) {}
}

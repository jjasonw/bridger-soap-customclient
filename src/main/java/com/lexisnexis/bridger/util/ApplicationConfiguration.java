package com.lexisnexis.bridger.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class ApplicationConfiguration {

    private static final Map<String, String> ENVIRONMENT_VARIABLES = Map.of(
        "bridger.api.clientId", "BRIDGER_API_CLIENT_ID",
        "bridger.api.userId", "BRIDGER_API_USER_ID",
        "bridger.api.password", "BRIDGER_API_PASSWORD"
    );

    private static final Properties PROPERTIES = load();

    public ApplicationConfiguration() {
    }

    public static String getRequiredProperty(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration property: " + key);
        }
        return value.trim();
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream bundledConfiguration =
                 ApplicationConfiguration.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (bundledConfiguration != null) {
                properties.load(bundledConfiguration);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load bundled application.properties", e);
        }

        loadFile(properties, Path.of("src", "main", "resources", "application.properties"));
        loadFile(properties, Path.of("application.properties"));
        ENVIRONMENT_VARIABLES.forEach((property, variable) -> {
            String value = System.getenv(variable);
            if (value != null) {
                properties.setProperty(property, value);
            }
        });
        return properties;
    }

    private static void loadFile(Properties properties, Path configurationFile) {
        if (!Files.exists(configurationFile)) {
            return;
        }
        if (!Files.isRegularFile(configurationFile)) {
            throw new IllegalStateException("application.properties is not a regular file: "
                + configurationFile.toAbsolutePath());
        }
        try (InputStream input = Files.newInputStream(configurationFile)) {
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + configurationFile.toAbsolutePath(), e);
        }
    }
}

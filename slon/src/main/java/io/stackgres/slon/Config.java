package io.stackgres.slon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public final class Config {

    private static final Properties properties = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null)
                throw new IllegalStateException("application.properties not found");
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String getValue(String envVarKey, String defaultValue) {
        String envValue = System.getenv(envVarKey);
        return envValue != null ? envValue : defaultValue;
    }

    public static String getAppProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

}
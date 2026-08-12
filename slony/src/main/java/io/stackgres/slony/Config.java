package io.stackgres.slony;

public final class Config {

    public static String getValue(String envVarKey, String defaultValue) {
        String envValue = System.getenv(envVarKey);
        return envValue != null ? envValue : defaultValue;
    }

}
package io.stackgres;

public final class Strings {

    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    public static String strip(String value) {
        return value != null ? value.strip() : null;
    }

}

package io.stackgres.postgres;

import java.util.regex.Pattern;

public class PostgresClusterValidator {

    public static final Pattern NAME_REGEX = Pattern.compile("^[a-z][a-z0-9-_]+[a-z0-9]$");

    public static boolean isNameValid(String name) {
        return NAME_REGEX.matcher(name).matches();
    }

}
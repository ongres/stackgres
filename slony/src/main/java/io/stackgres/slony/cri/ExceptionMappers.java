package io.stackgres.slony.cri;

public final class ExceptionMappers {

    public static Throwable getRootCause(Throwable throwable) {
        Throwable root = throwable;

        while (root.getCause() != null || root == root.getCause()) {
            root = root.getCause();
        }
        return root;
    }

    public static String getRootCauseMessage(Throwable throwable) {
        String message = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
        Throwable cause = ExceptionMappers.getRootCause(throwable);
        if (cause != throwable) {
            message = cause.getMessage() != null ? cause.getMessage() : message;
        }
        return message;
    }

}

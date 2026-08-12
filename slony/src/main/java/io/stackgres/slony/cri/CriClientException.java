package io.stackgres.slony.cri;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;

public class CriClientException extends RuntimeException {

    private Cause knownCause;
    private String errorDetails;
    private CriContext context;
    private Throwable root;

    public CriClientException(Throwable cause, CriContext context) {
        super(cause);
        this.context = context;
        this.root = ExceptionMappers.getRootCause(cause);
        handleKnownCause();
        handleErrorDetails(cause);
    }

    private void handleKnownCause() {
        if (root instanceof FileNotFoundException)
            knownCause = Cause.SOCKET_NOT_FOUND;

        else if (root instanceof ConnectException)
            knownCause = Cause.SOCKET_CONNECT;

        else if (root instanceof AccessDeniedException)
            knownCause = Cause.ACCESS_DENIED;
    }

    private void handleErrorDetails(Throwable cause) {
        if (cause.getMessage() != null)
            errorDetails = cause.getMessage();

        if (root.getMessage() != null)
            errorDetails = root.getMessage();
    }

    public Cause getKnownCause() {
        return knownCause;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public CriContext getContext() {
        return context;
    }

    public Throwable getRoot() {
        return root;
    }

    public enum Cause {
        SOCKET_NOT_FOUND, SOCKET_CONNECT, ACCESS_DENIED
    }

}

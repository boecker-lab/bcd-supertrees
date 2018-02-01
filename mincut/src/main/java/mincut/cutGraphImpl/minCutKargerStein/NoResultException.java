package mincut.cutGraphImpl.minCutKargerStein;

public class NoResultException extends RuntimeException {
    public NoResultException() {
    }

    public NoResultException(String message) {
        super(message);
    }

    public NoResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoResultException(Throwable cause) {
        super(cause);
    }

    public NoResultException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

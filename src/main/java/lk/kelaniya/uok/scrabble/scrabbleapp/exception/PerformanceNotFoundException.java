package lk.kelaniya.uok.scrabble.scrabbleapp.exception;

public class PerformanceNotFoundException extends RuntimeException {
    public PerformanceNotFoundException() {
        super();
    }

    public PerformanceNotFoundException(String message) {
        super(message);
    }

    public PerformanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

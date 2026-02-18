package lk.kelaniya.uok.scrabble.scrabbleapp.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException() {
        super();
    }

    public PlayerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlayerNotFoundException(String message) {
        super(message);
    }
}

package FrancescoAlves.capstone.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resource, Object id) {
        super(resource + " con id " + id + " non trovato!");
    }

}
package FrancescoAlves.capstone.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// gestione centralizzata delle eccezioni dei controller
@RestControllerAdvice
public class ExceptionsHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(NotFoundException ex) {
        return createPayload(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(ValidationException ex) {
        return createPayload(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(UnauthorizedException ex) {
        return createPayload(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // messaggio generico per non rivelare se l'email esiste
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleAuthentication(AuthenticationException ex) {
        return createPayload("Email o password non corretti", HttpStatus.UNAUTHORIZED);
    }

    // fallback per i vincoli DB non intercettati prima: meglio un 409 che un 500
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        return createPayload("Operazione in conflitto con un dato gia' esistente", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneric(Exception ex) {
        ex.printStackTrace();
        return createPayload("Errore interno del server", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> createPayload(String message, HttpStatus status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("status", status.value());
        payload.put("timestamp", LocalDateTime.now());
        payload.put("error", status.getReasonPhrase());
        return payload;
    }
}
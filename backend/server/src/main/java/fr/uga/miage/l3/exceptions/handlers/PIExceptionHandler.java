package fr.uga.miage.l3.exceptions.handlers;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.exceptions.ConflictException;
import fr.uga.miage.l3.exceptions.InvalidRequestException;
import fr.uga.miage.l3.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.stream.Collectors;

/**
 * Traduit les exceptions métier en réponses HTTP, pour que les services ne dépendent pas du web.
 */
@Slf4j
@RestControllerAdvice
public class PIExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorDTO notFound(HttpServletRequest request, ResourceNotFoundException ex) {
        return error(request, ex.getMessage());
    }

    @ExceptionHandler({
        InvalidRequestException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDTO badRequest(HttpServletRequest request, Exception ex) {
        return error(request, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDTO fileTooLarge(HttpServletRequest request) {
        return error(request, "La photo dépasse 3 Mo");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDTO missingFile(HttpServletRequest request) {
        return error(request, "Aucune photo reçue");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDTO invalidBody(HttpServletRequest request, MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + " : " + fieldError.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return error(request, "Requête invalide (" + details + ")");
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorDTO conflict(HttpServletRequest request, ConflictException ex) {
        return error(request, ex.getMessage());
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorDTO externalResourceNotFound(HttpServletRequest request, HttpClientErrorException.NotFound ex) {
        return error(request, ex.getMessage());
    }

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ErrorDTO externalServiceFailure(HttpServletRequest request, RestClientException ex) {
        log.error("Appel à OpenRouteService en échec sur {}", request.getRequestURI(), ex);
        return error(request, "Le service de cartographie est indisponible, réessayez plus tard.");
    }

    private ErrorDTO error(HttpServletRequest request, String message) {
        return new ErrorDTO(request.getRequestURI(), message);
    }
}

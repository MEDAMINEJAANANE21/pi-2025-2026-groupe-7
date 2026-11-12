package fr.uga.miage.l3.exceptions.handlers;

import fr.uga.miage.l3.errors.ErrorDTO;
import fr.uga.miage.l3.exceptions.rest.BadRequestRestException;
import fr.uga.miage.l3.exceptions.rest.NotFoundElementRestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class PIExceptionHandler {

    @ExceptionHandler({
        NotFoundElementRestException.class,
        HttpClientErrorException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorDTO notFoundException(HttpServletRequest httpServletRequest, Exception ex){
        return new ErrorDTO(
                httpServletRequest.getRequestURI(),
                ex.getMessage()
        );
    }

    @ExceptionHandler({
        BadRequestRestException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDTO badRequest(HttpServletRequest httpServletRequest, Exception ex){
        return new ErrorDTO(
                httpServletRequest.getRequestURI(),
                ex.getMessage()
        );
    }
}

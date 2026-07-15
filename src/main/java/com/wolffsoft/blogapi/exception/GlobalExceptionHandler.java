package com.wolffsoft.blogapi.exception;

import com.wolffsoft.blogapi.auth.exception.EmailAlreadyExistsException;
import com.wolffsoft.blogapi.auth.exception.InvalidCredentialsException;
import com.wolffsoft.blogapi.auth.exception.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExistsException(final EmailAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(value = InvalidTokenException.class)
    public ProblemDetail handleInvalidTokenException(final InvalidTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(final DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email Already Exists");
    }

    @ExceptionHandler(value = InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentialsException(final InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    public ProblemDetail handleException(final Exception ex) {
        log.error("Unhandled Exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}

package org.example.flowershop.exception;

import org.example.flowershop.dto.ErrorResponseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(value = {NotFoundException.class,
            ImageNotFoundException.class,
            CartItemNotFoundException.class,
            OrderNotFoundException.class
    })
    public ResponseEntity<ErrorResponseDto> handleNotFound(Exception e) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message(e.getMessage())
                .status(HttpStatus.NOT_FOUND.name())
                .statusCode(HttpStatus.NOT_FOUND.value())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParams(
            org.springframework.web.bind.MissingServletRequestParameterException ex) {

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message("Missing parameter: " + ex.getParameterName())
                .status(HttpStatus.BAD_REQUEST.name())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> handleValidationErrors(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    private Map<String, List<String>> getErrorsMap(List<String> errors) {
        Map<String, List<String>> errorsMap = new HashMap<>();
        errorsMap.put("errors", errors);
        return errorsMap;
    }

    @ExceptionHandler({
            CategoryAlreadyExistsException.class,
            EmailAlreadyExistsException.class,
            UsernameAlreadyExistsException.class,
            ProductAlreadyExistsException.class,
            UserHasRelationsException.class,
            CategoryHasProductsException.class


    })
    public ResponseEntity<ErrorResponseDto> handleConflict(Exception ex) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.name())
                .statusCode(HttpStatus.CONFLICT.value())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message(ex.getMessage())
                .status(HttpStatus.FORBIDDEN.name())
                .statusCode(HttpStatus.FORBIDDEN.value())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ImageReadException.class)
    public ResponseEntity<ErrorResponseDto> handleImageReadException(ImageReadException ex) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message(ex.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UserHasOrdersException.class)
    public ResponseEntity<ErrorResponseDto> handleUserHasOrders(UserHasOrdersException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.name())
                        .statusCode(HttpStatus.CONFLICT.value())
                        .build());
    }

    @ExceptionHandler(ProductHasRelationsException.class)
    public ResponseEntity<ErrorResponseDto> handleProductHasRelations(ProductHasRelationsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.name())
                        .statusCode(HttpStatus.CONFLICT.value())
                        .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        return ResponseEntity
                .status(status)
                .body(ErrorResponseDto.builder()
                        .message(message)
                        .status(status.name())
                        .statusCode(status.value())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneralException(Exception ex) {
        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message("Internal server error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(
            org.springframework.security.authentication.BadCredentialsException ex) {

        ErrorResponseDto errorResponse = ErrorResponseDto.builder()
                .message(ex.getMessage())
                .status(HttpStatus.UNAUTHORIZED.name())
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
}
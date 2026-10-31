package busKaroo.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("Access denied", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        LOG.error("MethodArgumentNotValidException caught: ", ex);
        Map<String, Object> errors = new HashMap<>();
        errors.put("error", "Validation failed");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        errors.put("fieldErrors", fieldErrors);
        errors.put("message", "Request validation failed. Check fieldErrors for details.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String detail = mostSpecificMessage(ex).toLowerCase(java.util.Locale.ROOT);
        String path = request.getRequestURI();
        String message = "The request conflicts with existing data. Check for duplicate or related records.";
        if (detail.contains("user_phone") || detail.contains("phone_key") || detail.contains("phone)=")) {
            message = "Phone number is already registered. Use a different number or leave it blank.";
        } else if (detail.contains("user_email") || detail.contains("email_key") || detail.contains("email)=")) {
            message = "Email address is already registered. Sign in or use another email.";
        } else if (detail.contains("registration_number") || detail.contains("registrationnumber")) {
            message = "That bus number is already in use.";
        } else if (detail.contains("bus_category") && detail.contains("name")) {
            message = "A bus category with that name already exists.";
        } else if ("DELETE".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/buses/")) {
            message = "Cannot delete this bus while it is assigned to trips.";
        } else if ("DELETE".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/bus-categories/")) {
            message = "Cannot delete this category while buses are assigned to it.";
        } else if ("DELETE".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/routes/")) {
            message = "Cannot delete this route while trips or other records use it.";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("Data integrity violation", message));
    }

    private String mostSpecificMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("Entity not found", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ReservationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("Reservation not found", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("Resource not found", ex.getMessage()));
    }

    @ExceptionHandler(CancellationNotAllowedException.class)
    public ResponseEntity<ApiError> handleCancellationNotAllowed(
            CancellationNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Cancellation not allowed", ex.getMessage()));
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ApiError> handleSeatUnavailable(SeatUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("Seat unavailable", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        LOG.error("AuthenticationException caught: ", ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("Authentication failed", "Invalid email or password"));
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        LOG.error("IllegalStateException caught: ", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("Illegal state", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        LOG.error("IllegalArgumentException caught: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Bad request", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        LOG.error("HttpMessageNotReadableException caught: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Bad request", "Malformed JSON request body: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex) {
        LOG.error("Unhandled exception caught: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Internal server error", ex.getMessage()));
    }
}

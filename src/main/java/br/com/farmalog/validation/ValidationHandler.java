package br.com.farmalog.validation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class ValidationHandler {

	public record ValidationErrorResponse(String field, String message) {
		public ValidationErrorResponse(FieldError error) {
			this(error.getField(), error.getDefaultMessage());
		}
	}

	public record ErrorResponse(int status, String message) {
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public List<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		return exception.getFieldErrors().stream()
				.map(ValidationErrorResponse::new)
				.toList();
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
		ErrorResponse body = new ErrorResponse(
				exception.getStatusCode().value(),
				exception.getReason());
		return ResponseEntity.status(exception.getStatusCode()).body(body);
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleConflitoDeConcorrencia(ObjectOptimisticLockingFailureException exception) {
		return new ErrorResponse(HttpStatus.CONFLICT.value(),
				"O registro foi alterado por outra operação simultânea. Tente novamente.");
	}
}

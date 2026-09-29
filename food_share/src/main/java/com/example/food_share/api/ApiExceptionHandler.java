package com.example.food_share.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.example.food_share.api.ApiModels.ErrorResponse;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleStatusException(ResponseStatusException exception) {
		return ResponseEntity.status(exception.getStatusCode())
				.body(new ErrorResponse(exception.getReason()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.orElse("Request validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
	}
}
package br.com.farmalog.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ProblemDetail handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request);
	}

	@ExceptionHandler(RecursoDuplicadoException.class)
	ProblemDetail handleRecursoDuplicado(RecursoDuplicadoException ex, HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "Conflito com recurso existente", ex.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleGenerico(Exception ex, HttpServletRequest request) {
		log.error("Erro não tratado em {} {}", request.getMethod(), request.getRequestURI(), ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
				"Ocorreu um erro inesperado. Tente novamente mais tarde.", request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new CampoInvalido(fe.getField(), fe.getDefaultMessage()))
				.toList();
		ProblemDetail body = ex.getBody();
		body.setTitle("Requisição inválida");
		body.setDetail("A requisição contém campos inválidos");
		body.setProperty("campos", campos);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(title);
		pd.setInstance(URI.create(request.getRequestURI()));
		return pd;
	}

	record CampoInvalido(String campo, String mensagem) {
	}
}

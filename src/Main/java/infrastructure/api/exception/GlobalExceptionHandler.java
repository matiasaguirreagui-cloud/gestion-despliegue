package infrastructure.api.exception;

import domain.exeptions.ConflictoNegocioException;
import domain.exeptions.DatosInvalidosException;
import domain.exeptions.RecursoDuplicadoException;
import domain.exeptions.RecursoNoEncontradoException;
import domain.exeptions.ServicioCorreoException;
import domain.exeptions.SobrecargaLaboralException;
import domain.exeptions.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({DatosInvalidosException.class, SolicitudInvalidaException.class,
            IllegalArgumentException.class, DateTimeParseException.class,
            HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleDatosInvalidos(Exception ex) {
        String mensaje = ex instanceof HttpMessageNotReadableException
                ? "El cuerpo de la solicitud no es JSON válido o contiene datos con formato incorrecto."
                : mensajeSeguro(ex, "Revise los datos enviados.");
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", mensaje);
    }

    @ExceptionHandler({SobrecargaLaboralException.class, ConflictoNegocioException.class,
            RecursoDuplicadoException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, String>> handleConflicto(Exception ex) {
        String mensaje = ex instanceof DataIntegrityViolationException
                ? "La operación viola una restricción de datos o existe un registro con el mismo identificador."
                : mensajeSeguro(ex, "La operación entra en conflicto con el estado actual.");
        return respuesta(HttpStatus.CONFLICT, "Conflicto", mensaje);
    }

    @ExceptionHandler({RecursoNoEncontradoException.class, java.util.NoSuchElementException.class})
    public ResponseEntity<Map<String, String>> handleNoEncontrado(Exception ex) {
        return respuesta(HttpStatus.NOT_FOUND, "No encontrado", mensajeSeguro(ex, "No se encontró el recurso solicitado."));
    }

    @ExceptionHandler(ServicioCorreoException.class)
    public ResponseEntity<Map<String, String>> handleServicioCorreo(ServicioCorreoException ex) {
        return respuesta(HttpStatus.BAD_GATEWAY, "Error de correo",
                mensajeSeguro(ex, "No se pudo enviar el correo mediante Brevo."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMetodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "El método HTTP solicitado no está disponible para este recurso.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleErrorInesperado(Exception ex) {
        logger.error("Error interno de API: {}", ex.getClass().getSimpleName());
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado al procesar la solicitud.");
    }

    private String mensajeSeguro(Exception ex, String alternativa) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? alternativa : ex.getMessage();
    }

    private ResponseEntity<Map<String, String>> respuesta(HttpStatus status, String error, String mensaje) {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", error);
        cuerpo.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}

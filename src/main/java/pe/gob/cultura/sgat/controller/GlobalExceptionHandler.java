package pe.gob.cultura.sgat.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Datos inválidos");
        cuerpo.put("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    /** Restricciones de la BD (unicidad, checks). No se expone el detalle interno. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "La operación no cumple las restricciones de la base de datos");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }
}

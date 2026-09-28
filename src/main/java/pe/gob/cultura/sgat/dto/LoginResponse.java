package pe.gob.cultura.sgat.dto;

import java.util.List;

public record LoginResponse(String token, String tipo, String correo, List<String> roles) {
}

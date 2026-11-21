package infrastructure.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import domain.model.Tecnico;
import service.TecnicoService;
import service.dto.TecnicoCargaDTO;

import java.util.List;

@RestController
@RequestMapping("/api/tecnicos")
public class TecnicoAPI {

    private final TecnicoService tecnicoService;

    public TecnicoAPI(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    // Endpoint para ver la carga de todos los técnicos
    @GetMapping("/carga")
    public ResponseEntity<List<TecnicoCargaDTO>> obtenerCargaTrabajo() {
        return ResponseEntity.ok(tecnicoService.consultarCargaTecnicos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tecnico> obtenerTecnico(@PathVariable String id) {
        return ResponseEntity.ok(tecnicoService.obtenerPorId(id));
    }

    // Endpoint para asignar un caso manualmente a un técnico
    @PostMapping("/{id}/asignar")
    public ResponseEntity<String> asignarCaso(@PathVariable String id) {
        tecnicoService.asignarCasoManual(id);
        return ResponseEntity.ok("Caso asignado correctamente al técnico con ID: " + id);
    }

    // Endpoint para registrar un nuevo técnico
    @PostMapping
    public ResponseEntity<Tecnico> registrarTecnico(@RequestBody service.dto.TecnicoCreacionDTO nuevoTecnicoDTO) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(tecnicoService.registrarNuevoTecnico(nuevoTecnicoDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tecnico> actualizarTecnico(
            @PathVariable String id,
            @RequestBody service.dto.TecnicoCreacionDTO tecnicoDTO) {
        return ResponseEntity.ok(tecnicoService.actualizarTecnico(id, tecnicoDTO));
    }
}

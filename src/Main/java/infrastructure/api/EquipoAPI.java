package infrastructure.api;

import domain.model.Equipo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.EquipoService;

import java.util.List;

@RestController
@RequestMapping("/api/equipos")
public class EquipoAPI {
    private final EquipoService equipoService;

    public EquipoAPI(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    public ResponseEntity<List<Equipo>> listarEquipos() {
        return ResponseEntity.ok(equipoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Equipo> obtenerEquipo(@PathVariable String id) {
        return ResponseEntity.ok(equipoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Equipo> crearEquipo(@RequestBody CrearEquipoRequest request) {
        if (request == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe enviar los datos del equipo.");
        }
        Equipo equipo = equipoService.registrarEquipo(
                request.idEquipo(), request.marca(), request.modelo(), request.ubicacion());
        return ResponseEntity.status(HttpStatus.CREATED).body(equipo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Equipo> actualizarEquipo(@PathVariable String id,
                                                    @RequestBody ActualizarEquipoRequest request) {
        if (request == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe enviar los datos para actualizar el equipo.");
        }
        return ResponseEntity.ok(equipoService.actualizarEquipo(
                id, request.marca(), request.modelo(), request.ubicacion()));
    }

    @PostMapping("/{id}/fuera-de-servicio")
    public ResponseEntity<Equipo> marcarFueraDeServicio(@PathVariable String id) {
        return ResponseEntity.ok(equipoService.marcarFueraDeServicio(id));
    }

    public record CrearEquipoRequest(String idEquipo, String marca, String modelo, String ubicacion) {}
    public record ActualizarEquipoRequest(String marca, String modelo, String ubicacion) {}
}

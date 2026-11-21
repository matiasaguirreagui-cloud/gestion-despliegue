package infrastructure.api;

import domain.model.Equipo;
import domain.model.SolicitudMantenimiento;
import domain.model.UsuarioSolicitante;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.SolicitudService;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudAPI {
    private final SolicitudService solicitudService;

    public SolicitudAPI(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public ResponseEntity<List<SolicitudMantenimiento>> listarSolicitudes() {
        return ResponseEntity.ok(solicitudService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudMantenimiento> obtenerSolicitud(@PathVariable int id) {
        return ResponseEntity.ok(solicitudService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<SolicitudMantenimiento> registrarSolicitud(
            @RequestBody CrearSolicitudRequest request) {
        if (request == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe enviar los datos de la solicitud.");
        }
        Equipo equipo = new Equipo(request.equipoId(), request.equipoNombre(), request.equipoUbicacion());

        UsuarioSolicitante solicitante = new UsuarioSolicitante(
                request.solicitanteId(), request.solicitanteNombre(),
                request.departamento(), request.email());

        SolicitudMantenimiento solicitud = solicitudService.crearSolicitud(
                equipo, solicitante, request.descripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitud);
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<SolicitudMantenimiento> cerrarSolicitud(
            @PathVariable int id, @RequestBody CerrarSolicitudRequest request) {
        if (request == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe enviar el diagnóstico y la solución.");
        }
        return ResponseEntity.ok(solicitudService.cerrarSolicitud(
                id, request.diagnostico(), request.solucion()));
    }

    public record CrearSolicitudRequest(
            String equipoId,
            String equipoNombre,
            String equipoUbicacion,
            String solicitanteId,
            String solicitanteNombre,
            String departamento,
            String email,
            String descripcion
    ) {}

    public record CerrarSolicitudRequest(String diagnostico, String solucion) {}
}

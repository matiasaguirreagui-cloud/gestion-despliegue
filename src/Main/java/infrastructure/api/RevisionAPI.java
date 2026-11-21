package infrastructure.api;

import domain.model.RevisionPreventiva;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.RevisionService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/revisiones")
public class RevisionAPI {
    private final RevisionService revisionService;

    public RevisionAPI(RevisionService revisionService) {
        this.revisionService = revisionService;
    }

    @GetMapping
    public ResponseEntity<List<RevisionPreventiva>> listarRevisiones() {
        return ResponseEntity.ok(revisionService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RevisionPreventiva> obtenerRevision(@PathVariable String id) {
        return ResponseEntity.ok(revisionService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<RevisionPreventiva> registrarRevision(@RequestBody CrearRevisionRequest request) {
        if (request == null || request.diasAvisoPrevio() == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe indicar los días de aviso previo.");
        }
        RevisionPreventiva revision = new RevisionPreventiva(
                request.idRevision(),
                new domain.model.Equipo(request.equipoId(), "", ""),
                revisionService.parsearFecha(request.fechaProgramada()),
                request.diasAvisoPrevio(),
                request.emailSupervisor());
        return ResponseEntity.status(HttpStatus.CREATED).body(revisionService.registrarRevision(revision));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RevisionPreventiva> actualizarRevision(
            @PathVariable String id, @RequestBody ActualizarRevisionRequest request) {
        if (request == null || request.diasAvisoPrevio() == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe indicar los días de aviso previo.");
        }
        return ResponseEntity.ok(revisionService.actualizarRevision(
                id, request.equipoId(), revisionService.parsearFecha(request.fechaProgramada()),
                request.diasAvisoPrevio(), request.emailSupervisor()));
    }

    @PutMapping("/{id}/reprogramar")
    public ResponseEntity<RevisionPreventiva> reprogramarRevision(
            @PathVariable String id, @RequestBody ReprogramarRequest request) {
        LocalDate nuevaFecha = revisionService.parsearFecha(request == null ? null : request.nuevaFecha());
        revisionService.reprogramarRevision(id, nuevaFecha);
        return ResponseEntity.ok(revisionService.obtenerPorId(id));
    }

    public record CrearRevisionRequest(
            String idRevision,
            String equipoId,
            String fechaProgramada,
            Integer diasAvisoPrevio,
            String emailSupervisor) {}

    public record ActualizarRevisionRequest(
            String equipoId,
            String fechaProgramada,
            Integer diasAvisoPrevio,
            String emailSupervisor) {}

    public record ReprogramarRequest(String nuevaFecha) {}
}

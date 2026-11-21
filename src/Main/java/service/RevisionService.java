package service;

import domain.exeptions.RecursoDuplicadoException;
import domain.exeptions.RecursoNoEncontradoException;
import domain.model.Equipo;
import domain.model.RevisionPreventiva;
import domain.model.UsuarioSolicitante;
import domain.repository.EquipoRepository;
import domain.repository.RevisionPreventivaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class RevisionService {
    private static final Logger logger = LoggerFactory.getLogger(RevisionService.class);
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

    private final SolicitudService solicitudService;
    private final EmailService emailService;
    private final EquipoRepository equipoRepository;
    private final RevisionPreventivaRepository revisionRepository;

    public RevisionService(
            SolicitudService solicitudService,
            EmailService emailService,
            EquipoRepository equipoRepository,
            RevisionPreventivaRepository revisionRepository) {
        this.solicitudService = solicitudService;
        this.emailService = emailService;
        this.equipoRepository = equipoRepository;
        this.revisionRepository = revisionRepository;
    }

    @Scheduled(fixedRateString = "${app.revisiones.intervalo-ms:60000}")
    public void procesarAlertasPreventivas() {
        LocalDate hoy = LocalDate.now(ZONA_NEGOCIO);
        for (RevisionPreventiva revision : revisionRepository.obtenerTodas()) {
            try {
                procesarAlerta(revision, hoy);
            } catch (Exception ex) {
                logger.error("Falló el procesamiento de la revisión {} ({}). Se continuará con las demás.",
                        revision.getIdRevision(), ex.getClass().getSimpleName(), ex);
            }
        }
    }

    private void procesarAlerta(RevisionPreventiva revision, LocalDate hoy) {
        if (revision.isAlertaGenerada()) {
            return;
        }
        LocalDate fechaAlerta = revision.getFechaProgramada().minusDays(revision.getDiasAvisoPrevio());
        if (hoy.isBefore(fechaAlerta)) {
            return;
        }

        String asunto = "ALERTA: Mantenimiento Preventivo Próximo - " + revision.getEquipo().getNombre();
        String cuerpo = "El equipo requiere revisión el " + revision.getFechaProgramada()
                + ". Se ha generado la orden previa.";
        emailService.enviarCorreo(revision.getEmailSupervisor(), asunto, cuerpo);

        UsuarioSolicitante sistema = new UsuarioSolicitante(
                "SYS", "Sistema Motor Alertas", "Mantenimiento", revision.getEmailSupervisor());
        solicitudService.crearSolicitud(revision.getEquipo(), sistema, "Revisión preventiva programada");

        revision.setAlertaGenerada(true);
        revisionRepository.guardar(revision);
    }

    public void reprogramarRevision(String idRevision, LocalDate nuevaFecha) {
        RevisionPreventiva revision = obtenerPorId(idRevision);
        if (nuevaFecha == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe indicar la nueva fecha de la revisión.");
        }
        revision.setFechaProgramada(nuevaFecha);
        revision.setAlertaGenerada(false);
        revisionRepository.guardar(revision);
    }

    public List<RevisionPreventiva> obtenerTodas() {
        return List.copyOf(revisionRepository.obtenerTodas());
    }

    public RevisionPreventiva obtenerPorId(String idRevision) {
        String id = ValidacionesEntrada.requerido(idRevision, "ID de revisión", 30);
        return revisionRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la revisión " + id + "."));
    }

    public RevisionPreventiva registrarRevision(RevisionPreventiva revision) {
        if (revision == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe enviar los datos de la revisión.");
        }
        String id = ValidacionesEntrada.requerido(revision.getIdRevision(), "ID de revisión", 30);
        if (revisionRepository.buscarPorId(id).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe una revisión con el código " + id + ".");
        }
        if (revision.getFechaProgramada() == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe indicar la fecha programada.");
        }
        validarDiasAviso(revision.getDiasAvisoPrevio());
        if (revision.getEquipo() == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe asociar la revisión a un equipo.");
        }
        String idEquipo = ValidacionesEntrada.requerido(revision.getEquipo().getIdEquipo(), "ID del equipo", 30);
        Equipo equipo = equipoRepository.buscarPorId(idEquipo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + idEquipo + "."));
        String email = ValidacionesEntrada.email(revision.getEmailSupervisor(), "correo del supervisor");
        RevisionPreventiva nueva = new RevisionPreventiva(id, equipo, revision.getFechaProgramada(),
                revision.getDiasAvisoPrevio(), email);
        return revisionRepository.guardar(nueva);
    }

    public RevisionPreventiva actualizarRevision(
            String idRevision, String idEquipo, LocalDate fecha, int diasAvisoPrevio, String emailSupervisor) {
        RevisionPreventiva revision = obtenerPorId(idRevision);
        String codigoEquipo = ValidacionesEntrada.requerido(idEquipo, "ID del equipo", 30);
        Equipo equipo = equipoRepository.buscarPorId(codigoEquipo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + codigoEquipo + "."));
        if (fecha == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe indicar la fecha programada.");
        }
        validarDiasAviso(diasAvisoPrevio);
        String email = ValidacionesEntrada.email(emailSupervisor, "correo del supervisor");
        revision.actualizarDatos(equipo, fecha, diasAvisoPrevio, email);
        return revisionRepository.guardar(revision);
    }

    public LocalDate parsearFecha(String fecha) {
        String valor = ValidacionesEntrada.requerido(fecha, "fecha", 10);
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new domain.exeptions.DatosInvalidosException("La fecha debe usar el formato AAAA-MM-DD.");
        }
    }

    private void validarDiasAviso(int dias) {
        if (dias < 0) {
            throw new domain.exeptions.DatosInvalidosException("Los días de aviso previo no pueden ser negativos.");
        }
    }
}

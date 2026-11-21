package service;

import domain.exeptions.ConflictoNegocioException;
import domain.exeptions.RecursoNoEncontradoException;
import domain.exeptions.SolicitudInvalidaException;
import domain.model.Equipo;
import domain.model.SolicitudMantenimiento;
import domain.model.Tecnico;
import domain.model.UsuarioSolicitante;
import domain.repository.EquipoRepository;
import domain.repository.SolicitanteRepository;
import domain.repository.SolicitudRepository;
import domain.repository.TecnicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SolicitudService {
    private final EmailService emailService;
    private final TecnicoRepository tecnicoRepository;
    private final EquipoRepository equipoRepository;
    private final SolicitanteRepository solicitanteRepository;
    private final SolicitudRepository solicitudRepository;

    public SolicitudService(
            EmailService emailService,
            TecnicoRepository tecnicoRepository,
            EquipoRepository equipoRepository,
            SolicitanteRepository solicitanteRepository,
            SolicitudRepository solicitudRepository) {
        this.emailService = emailService;
        this.tecnicoRepository = tecnicoRepository;
        this.equipoRepository = equipoRepository;
        this.solicitanteRepository = solicitanteRepository;
        this.solicitudRepository = solicitudRepository;
    }

    public List<SolicitudMantenimiento> obtenerTodas() {
        return List.copyOf(solicitudRepository.obtenerTodas());
    }

    public SolicitudMantenimiento obtenerPorId(int idSolicitud) {
        if (idSolicitud <= 0) {
            throw new domain.exeptions.DatosInvalidosException("El ID de la solicitud debe ser mayor que cero.");
        }
        return solicitudRepository.buscarPorId(idSolicitud)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud " + idSolicitud + "."));
    }

    @Transactional
    public SolicitudMantenimiento crearSolicitud(Equipo equipo, UsuarioSolicitante solicitante, String descripcion) {
        if (equipo == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un equipo válido para formalizar la solicitud.");
        }
        String idEquipo = ValidacionesEntrada.requerido(equipo.getIdEquipo(), "ID del equipo", 30);
        Equipo equipoGestionado = equipoRepository.buscarPorId(idEquipo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + idEquipo + "."));
        if (equipoGestionado.isFueraDeServicio()) {
            throw new SolicitudInvalidaException("No se puede crear la solicitud. El equipo "
                    + equipoGestionado.getNombre() + " está Fuera de servicio.");
        }
        String descripcionValidada = ValidacionesEntrada.requerido(descripcion, "descripción de la falla", 10000);
        if (solicitante == null) {
            throw new SolicitudInvalidaException("Debe registrar los datos del solicitante.");
        }

        String idSolicitante = ValidacionesEntrada.requerido(solicitante.getIdUsuario(), "ID del solicitante", 30);
        String nombreSolicitante = ValidacionesEntrada.requerido(solicitante.getNombre(), "nombre del solicitante", 120);
        String departamento = ValidacionesEntrada.requerido(solicitante.getDepartamento(), "departamento", 120);
        String emailSolicitante = ValidacionesEntrada.email(solicitante.getEmail(), "correo del solicitante");

        Tecnico tecnico = tecnicoRepository.obtenerTodos().stream()
                .filter(t -> t.getCasosActivos() < 5)
                .min(Comparator.comparingInt(Tecnico::getCasosActivos))
                .orElseThrow(() -> new ConflictoNegocioException(
                        "No hay técnicos disponibles: todos alcanzaron el máximo de 5 casos activos."));

        UsuarioSolicitante solicitanteGestionado = solicitanteRepository.buscarPorId(idSolicitante)
                .map(existente -> {
                    existente.actualizarDatos(nombreSolicitante, departamento, emailSolicitante);
                    return solicitanteRepository.guardar(existente);
                })
                .orElseGet(() -> solicitanteRepository.guardar(new UsuarioSolicitante(
                        idSolicitante, nombreSolicitante, departamento, emailSolicitante)));

        SolicitudMantenimiento solicitud = solicitudRepository.guardar(
                new SolicitudMantenimiento(equipoGestionado, solicitanteGestionado, descripcionValidada));

        String cuerpo = "La solicitud #" + solicitud.getIdSolicitud()
                + " cambió al estado ASIGNADO. Técnico: " + tecnico.getNombre()
                + " (" + tecnico.getIdUsuario() + "). Equipo: " + equipoGestionado.getNombre()
                + ". Falla reportada: " + descripcionValidada;
        emailService.enviarCorreo(emailSolicitante,
                "Solicitud #" + solicitud.getIdSolicitud() + " asignada", cuerpo);

        tecnico.asignarNuevoCaso();
        solicitud.asignarTecnico(tecnico);
        tecnicoRepository.guardar(tecnico);
        return solicitudRepository.guardar(solicitud);
    }

    @Transactional
    public SolicitudMantenimiento cerrarSolicitud(int id, String diagnostico, String solucion) {
        SolicitudMantenimiento solicitud = obtenerPorId(id);
        String diagnosticoValidado = ValidacionesEntrada.requerido(diagnostico, "diagnóstico", 10000);
        String solucionValidada = ValidacionesEntrada.requerido(solucion, "solución", 10000);
        if ("Cerrado".equals(solicitud.getEstado())) {
            throw new ConflictoNegocioException("La solicitud ya está cerrada.");
        }

        emailService.enviarCorreo(
                solicitud.getSolicitante().getEmail(),
                "Solicitud #" + id + " cerrada",
                "La solicitud #" + id + " cambió al estado Cerrado.\n"
                        + "Equipo: " + solicitud.getEquipo().getNombre() + "\n"
                        + "Diagnóstico: " + diagnosticoValidado + "\n"
                        + "Solución: " + solucionValidada);

        solicitud.cerrar(diagnosticoValidado, solucionValidada);
        if (solicitud.getTecnicoAsignado() != null) {
            tecnicoRepository.guardar(solicitud.getTecnicoAsignado());
        }
        equipoRepository.guardar(solicitud.getEquipo());
        return solicitudRepository.guardar(solicitud);
    }
}

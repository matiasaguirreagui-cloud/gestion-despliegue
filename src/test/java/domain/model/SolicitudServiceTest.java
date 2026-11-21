package domain.model;

import domain.exeptions.ConflictoNegocioException;
import domain.exeptions.SolicitudInvalidaException;
import domain.repository.EquipoRepository;
import domain.repository.SolicitanteRepository;
import domain.repository.SolicitudRepository;
import domain.repository.TecnicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.EmailService;
import service.SolicitudService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {
    @Mock private EmailService emailService;
    @Mock private TecnicoRepository tecnicoRepository;
    @Mock private EquipoRepository equipoRepository;
    @Mock private SolicitanteRepository solicitanteRepository;
    @Mock private SolicitudRepository solicitudRepository;

    @InjectMocks private SolicitudService solicitudService;

    @BeforeEach
    void configurarRepositorios() {
        lenient().when(solicitanteRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(solicitudRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Tecnico tecnicoConCarga(String id, String nombre, int casosActivos) {
        Tecnico tecnico = new Tecnico(id, nombre, "Mantenimiento", "Mecánica");
        for (int i = 0; i < casosActivos; i++) tecnico.asignarNuevoCaso();
        return tecnico;
    }

    private UsuarioSolicitante solicitante(String id, String nombre, String departamento, String correo) {
        return new UsuarioSolicitante(id, nombre, departamento, correo);
    }

    @Test
    @DisplayName("Asigna automáticamente al técnico disponible con menor carga")
    void asignaTecnicoConMenorCarga() {
        Equipo equipo = new Equipo("EQ-100", "Impresora Industrial", "Piso 2");
        UsuarioSolicitante solicitante = solicitante("USR-001", "Carlos", "Operaciones", "carlos@example.com");
        Tecnico cargado = tecnicoConCarga("T01", "Juan Pérez", 3);
        Tecnico libre = tecnicoConCarga("T02", "María Gómez", 1);
        when(equipoRepository.buscarPorId("EQ-100")).thenReturn(Optional.of(equipo));
        when(solicitanteRepository.buscarPorId("USR-001")).thenReturn(Optional.empty());
        when(tecnicoRepository.obtenerTodos()).thenReturn(List.of(cargado, libre));

        SolicitudMantenimiento resultado = solicitudService.crearSolicitud(equipo, solicitante, "Fallo en rodillo");

        assertEquals("ASIGNADO", resultado.getEstado());
        assertSame(libre, resultado.getTecnicoAsignado());
        assertEquals(2, libre.getCasosActivos());
        verify(emailService).enviarCorreo(eq("carlos@example.com"), contains("asignada"), contains("María Gómez"));
        verify(tecnicoRepository).guardar(libre);
    }

    @Test
    @DisplayName("Rechaza crear una solicitud cuando todos los técnicos tienen cinco casos activos")
    void rechazaCuandoNoHayTecnicosDisponibles() {
        Equipo equipo = new Equipo("EQ-200", "Servidor Principal", "Data Center");
        when(equipoRepository.buscarPorId("EQ-200")).thenReturn(Optional.of(equipo));
        when(tecnicoRepository.obtenerTodos()).thenReturn(List.of(
                tecnicoConCarga("T01", "Juan Pérez", 5), tecnicoConCarga("T02", "María Gómez", 5)));

        assertThrows(ConflictoNegocioException.class,
                () -> solicitudService.crearSolicitud(equipo,
                        solicitante("USR-002", "Mariana", "Operaciones", "mariana@example.com"), "Pantalla azul"));
        verifyNoInteractions(emailService);
        verify(solicitudRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Actualiza nombre y correo del solicitante existente antes de notificar")
    void actualizaSolicitanteExistente() {
        Equipo equipo = new Equipo("EQ-100", "Impresora", "Piso 2");
        UsuarioSolicitante existente = solicitante("USR-003", "Nombre anterior", "Operaciones", "anterior@example.com");
        when(equipoRepository.buscarPorId("EQ-100")).thenReturn(Optional.of(equipo));
        when(solicitanteRepository.buscarPorId("USR-003")).thenReturn(Optional.of(existente));
        when(tecnicoRepository.obtenerTodos()).thenReturn(List.of(tecnicoConCarga("T01", "Técnico", 0)));

        SolicitudMantenimiento resultado = solicitudService.crearSolicitud(equipo,
                solicitante("USR-003", "Nombre nuevo", "Soporte", "nuevo@example.com"), "Falla nueva");

        assertEquals("Nombre nuevo", resultado.getSolicitante().getNombre());
        assertEquals("nuevo@example.com", resultado.getSolicitante().getEmail());
        verify(solicitanteRepository).guardar(existente);
        verify(emailService).enviarCorreo(eq("nuevo@example.com"), any(), any());
    }

    @Test
    @DisplayName("Cierra una solicitud, registra diagnóstico y libera la carga del técnico")
    void cierraSolicitudYLiberarTecnico() {
        Equipo equipo = new Equipo("EQ-200", "Servidor Principal", "Data Center");
        UsuarioSolicitante solicitante = solicitante("USR-004", "Luis", "Operaciones", "luis@example.com");
        Tecnico tecnico = tecnicoConCarga("T01", "Juan Pérez", 2);
        SolicitudMantenimiento solicitud = new SolicitudMantenimiento(42, equipo, solicitante, "Sin disco");
        solicitud.asignarTecnico(tecnico);
        when(solicitudRepository.buscarPorId(42)).thenReturn(Optional.of(solicitud));

        SolicitudMantenimiento cerrada = solicitudService.cerrarSolicitud(42,
                "Disco duro desconectado", "Reconexión del cable");

        assertEquals("Cerrado", cerrada.getEstado());
        assertEquals("Disco duro desconectado", cerrada.getDiagnostico());
        assertEquals("Reconexión del cable", cerrada.getSolucion());
        assertNotNull(cerrada.getFechaHoraCierre());
        assertEquals(1, tecnico.getCasosActivos());
        verify(emailService).enviarCorreo(eq("luis@example.com"), contains("cerrada"), contains("Reconexión"));
        verify(tecnicoRepository).guardar(tecnico);
        verify(equipoRepository).guardar(equipo);
    }

    @Test
    @DisplayName("No crea solicitudes para equipos fuera de servicio")
    void rechazaEquipoFueraDeServicio() {
        Equipo equipo = new Equipo("EQ-999", "Motor Principal", "Planta 1");
        equipo.marcarFueraDeServicio();
        when(equipoRepository.buscarPorId("EQ-999")).thenReturn(Optional.of(equipo));

        assertThrows(SolicitudInvalidaException.class,
                () -> solicitudService.crearSolicitud(equipo,
                        solicitante("USR-005", "Ana", "Operaciones", "ana@example.com"), "Motor detenido"));
        verifyNoInteractions(emailService);
        verifyNoInteractions(tecnicoRepository);
    }
}

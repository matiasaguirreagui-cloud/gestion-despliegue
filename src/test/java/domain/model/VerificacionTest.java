package domain.model;

import domain.exeptions.SolicitudInvalidaException;
import domain.repository.EquipoRepository;
import domain.repository.SolicitanteRepository;
import domain.repository.SolicitudRepository;
import domain.repository.TecnicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.EmailService;
import service.SolicitudService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificacionTest {
    @Mock private EmailService emailService;
    @Mock private TecnicoRepository tecnicoRepository;
    @Mock private EquipoRepository equipoRepository;
    @Mock private SolicitanteRepository solicitanteRepository;
    @Mock private SolicitudRepository solicitudRepository;

    private SolicitudService solicitudService;

    @BeforeEach
    void configurarServicio() {
        solicitudService = new SolicitudService(emailService, tecnicoRepository, equipoRepository,
                solicitanteRepository, solicitudRepository);
        lenient().when(solicitanteRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(solicitudRepository.guardar(any())).thenAnswer(invocacion -> {
            SolicitudMantenimiento solicitud = invocacion.getArgument(0);
            return solicitud.getIdSolicitud() == null
                    ? new SolicitudMantenimiento(1001, solicitud.getEquipo(), solicitud.getSolicitante(),
                            solicitud.getDescripcionFalla())
                    : solicitud;
        });
    }

    @Test
    void deberiaCrearSolicitudExitosamente() {
        Equipo equipo = new Equipo("EQ-101", "Laptop Lenovo", "Oficina 302");
        UsuarioSolicitante usuario = new UsuarioSolicitante("USR-001", "Carlos", "Sistemas", "carlos@empresa.com");
        Tecnico tecnico = new Tecnico("TEC-01", "Pedro", "Sistemas", "Soporte");
        when(equipoRepository.buscarPorId("EQ-101")).thenReturn(Optional.of(equipo));
        when(solicitanteRepository.buscarPorId("USR-001")).thenReturn(Optional.empty());
        when(tecnicoRepository.obtenerTodos()).thenReturn(List.of(tecnico));

        SolicitudMantenimiento resultado = solicitudService.crearSolicitud(equipo, usuario, "Pantalla azul");

        assertNotNull(resultado);
        assertEquals(1001, resultado.getIdSolicitud());
        assertEquals("ASIGNADO", resultado.getEstado());
        verify(emailService).enviarCorreo(anyString(), anyString(), anyString());
        verify(solicitudRepository, times(2)).guardar(any(SolicitudMantenimiento.class));
    }

    @Test
    void deberiaLanzarExcepcionSiFaltaEquipo() {
        UsuarioSolicitante usuario = new UsuarioSolicitante("USR-001", "Carlos", "Sistemas", "carlos@empresa.com");

        assertThrows(SolicitudInvalidaException.class,
                () -> solicitudService.crearSolicitud(null, usuario, "Pantalla azul"));
    }

    @Test
    void deberiaLanzarExcepcionSiDescripcionEstaVacia() {
        Equipo equipo = new Equipo("EQ-101", "Laptop Lenovo", "Oficina 302");
        UsuarioSolicitante usuario = new UsuarioSolicitante("USR-001", "Carlos", "Sistemas", "carlos@empresa.com");
        when(equipoRepository.buscarPorId("EQ-101")).thenReturn(Optional.of(equipo));

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.crearSolicitud(equipo, usuario, ""));
    }
}

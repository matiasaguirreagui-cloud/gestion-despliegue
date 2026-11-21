package domain.model;

import domain.repository.EquipoRepository;
import domain.repository.RevisionPreventivaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.EmailService;
import service.RevisionService;
import service.SolicitudService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import domain.exeptions.RecursoDuplicadoException;
import domain.exeptions.ServicioCorreoException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.clearInvocations;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {
    @Mock private SolicitudService solicitudService;
    @Mock private EmailService emailService;
    @Mock private EquipoRepository equipoRepository;
    @Mock private RevisionPreventivaRepository revisionRepository;

    private RevisionService revisionService;
    private final Equipo equipo = new Equipo("EQ-100", "Impresora", "Piso 1");

    @BeforeEach
    void configurarServicio() {
        revisionService = new RevisionService(solicitudService, emailService, equipoRepository, revisionRepository);
        lenient().when(revisionRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(equipoRepository.buscarPorId("EQ-100")).thenReturn(Optional.of(equipo));
        lenient().when(revisionRepository.buscarPorId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void procesaAlertaYGeneraSolicitudCuandoLlegaLaFecha() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        RevisionPreventiva revision = revisionService.registrarRevision(
                new RevisionPreventiva("REV-001", equipo, hoy.plusDays(3), 3, "supervisor@example.com"));
        when(revisionRepository.obtenerTodas()).thenReturn(List.of(revision));

        revisionService.procesarAlertasPreventivas();

        verify(emailService, times(1)).enviarCorreo(anyString(), anyString(), anyString());
        verify(solicitudService, times(1)).crearSolicitud(any(), any(), anyString());
        assertTrue(revision.isAlertaGenerada());
    }

    @Test
    void noProcesaAlertaAntesDeLaFechaDeAviso() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        RevisionPreventiva revision = revisionService.registrarRevision(
                new RevisionPreventiva("REV-002", equipo, hoy.plusDays(10), 3, "supervisor@example.com"));
        when(revisionRepository.obtenerTodas()).thenReturn(List.of(revision));

        revisionService.procesarAlertasPreventivas();

        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
        verify(solicitudService, never()).crearSolicitud(any(), any(), anyString());
        assertFalse(revision.isAlertaGenerada());
    }

    @Test
    void continuaConLasDemasRevisionesSiUnaFalla() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        RevisionPreventiva fallida = revisionService.registrarRevision(
                new RevisionPreventiva("REV-010", equipo, hoy.plusDays(1), 1, "fallo@example.com"));
        RevisionPreventiva correcta = revisionService.registrarRevision(
                new RevisionPreventiva("REV-011", equipo, hoy.plusDays(1), 1, "correcto@example.com"));
        when(revisionRepository.obtenerTodas()).thenReturn(List.of(fallida, correcta));
        doThrow(new ServicioCorreoException("No se pudo enviar el correo mediante Brevo."))
                .when(emailService).enviarCorreo("fallo@example.com", "ALERTA: Mantenimiento Preventivo Próximo - Impresora",
                        "El equipo requiere revisión el " + hoy.plusDays(1) + ". Se ha generado la orden previa.");

        revisionService.procesarAlertasPreventivas();

        assertFalse(fallida.isAlertaGenerada());
        assertTrue(correcta.isAlertaGenerada());
        verify(solicitudService, times(1)).crearSolicitud(any(), any(), anyString());
    }

    @Test
    void reprogramarActualizaFechaYReiniciaAlerta() {
        LocalDate inicial = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(1);
        RevisionPreventiva revision = revisionService.registrarRevision(
                new RevisionPreventiva("REV-003", equipo, inicial, 2, "supervisor@example.com"));
        revision.setAlertaGenerada(true);
        when(revisionRepository.buscarPorId("REV-003")).thenReturn(Optional.of(revision));
        LocalDate nueva = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(20);
        clearInvocations(revisionRepository);

        revisionService.reprogramarRevision("REV-003", nueva);

        assertEquals(nueva, revision.getFechaProgramada());
        assertFalse(revision.isAlertaGenerada());
        verify(revisionRepository).guardar(revision);
    }

    @Test
    void rechazaRegistrarUnCodigoDeRevisionDuplicado() {
        when(revisionRepository.buscarPorId("REV-DUP")).thenReturn(Optional.of(
                new RevisionPreventiva("REV-DUP", equipo, LocalDate.now(), 2, "supervisor@example.com")));

        org.junit.jupiter.api.Assertions.assertThrows(RecursoDuplicadoException.class,
                () -> revisionService.registrarRevision(new RevisionPreventiva(
                        "REV-DUP", equipo, LocalDate.now().plusDays(1), 2, "supervisor@example.com")));
    }
}

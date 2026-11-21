package infrastructure.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.ReporteSemanalService;
import service.dto.ReporteSemanalDTO;

@RestController
@RequestMapping("/api/reportes")
public class ReporteAPI {
    private final ReporteSemanalService reporteSemanalService;

    public ReporteAPI(ReporteSemanalService reporteSemanalService) {
        this.reporteSemanalService = reporteSemanalService;
    }

    @PostMapping("/tecnicos/semanal/enviar")
    public ResponseEntity<ReporteSemanalDTO> enviarReporteSemanal(
            @RequestBody EnviarReporteRequest request) {
        if (request == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe indicar el correo del coordinador.");
        }
        return ResponseEntity.ok(reporteSemanalService.enviarReporteSemanal(request.emailCoordinador()));
    }

    public record EnviarReporteRequest(String emailCoordinador) {}
}

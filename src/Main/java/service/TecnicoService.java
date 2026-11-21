package service;

import domain.exeptions.RecursoDuplicadoException;
import domain.exeptions.RecursoNoEncontradoException;
import domain.model.Tecnico;
import domain.repository.TecnicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.dto.TecnicoCargaDTO;
import service.dto.TecnicoCreacionDTO;

import java.util.List;

@Service
public class TecnicoService {
    private final TecnicoRepository tecnicoRepository;

    public TecnicoService(TecnicoRepository tecnicoRepository) {
        this.tecnicoRepository = tecnicoRepository;
    }

    public List<TecnicoCargaDTO> consultarCargaTecnicos() {
        return tecnicoRepository.obtenerTodos().stream()
                .map(t -> new TecnicoCargaDTO(
                        t.getIdUsuario(), t.getNombre(), t.getDepartamento(), t.getEspecialidad(),
                        t.getCasosActivos(), t.getDisponibilidad()))
                .toList();
    }

    public Tecnico obtenerPorId(String idTecnico) {
        String id = ValidacionesEntrada.requerido(idTecnico, "ID del técnico", 30);
        return tecnicoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el técnico " + id + "."));
    }

    @Transactional
    public Tecnico actualizarTecnico(String idTecnico, TecnicoCreacionDTO dto) {
        Tecnico tecnico = obtenerPorId(idTecnico);
        if (dto == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe enviar los datos del técnico.");
        }
        String nombre = ValidacionesEntrada.requerido(dto.getNombre(), "nombre del técnico", 120);
        String departamento = ValidacionesEntrada.requerido(dto.getDepartamento(), "departamento", 120);
        String especialidad = ValidacionesEntrada.requerido(dto.getEspecialidad(), "especialidad", 120);
        tecnico.actualizarDatos(nombre, departamento, especialidad);
        tecnicoRepository.guardar(tecnico);
        return tecnico;
    }

    @Transactional
    public void asignarCasoManual(String idTecnico) {
        Tecnico tecnico = obtenerPorId(idTecnico);
        tecnico.asignarNuevoCaso();
        tecnicoRepository.guardar(tecnico);
    }

    @Transactional
    public Tecnico registrarNuevoTecnico(TecnicoCreacionDTO dto) {
        DatosTecnico datos = validar(dto);
        if (tecnicoRepository.buscarPorId(datos.id()).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe un técnico con el ID " + datos.id() + ".");
        }
        Tecnico tecnico = new Tecnico(datos.id(), datos.nombre(), datos.departamento(), datos.especialidad());
        tecnicoRepository.guardar(tecnico);
        return tecnico;
    }

    private DatosTecnico validar(TecnicoCreacionDTO dto) {
        if (dto == null) {
            throw new domain.exeptions.DatosInvalidosException("Debe enviar los datos del técnico.");
        }
        return new DatosTecnico(
                ValidacionesEntrada.requerido(dto.getIdUsuario(), "ID del técnico", 30),
                ValidacionesEntrada.requerido(dto.getNombre(), "nombre del técnico", 120),
                ValidacionesEntrada.requerido(dto.getDepartamento(), "departamento", 120),
                ValidacionesEntrada.requerido(dto.getEspecialidad(), "especialidad", 120));
    }

    private record DatosTecnico(String id, String nombre, String departamento, String especialidad) {}
}

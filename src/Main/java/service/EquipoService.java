package service;

import domain.exeptions.RecursoDuplicadoException;
import domain.exeptions.RecursoNoEncontradoException;
import domain.model.Equipo;
import domain.repository.EquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class EquipoService {
    private final EquipoRepository equipoRepository;

    public EquipoService(EquipoRepository equipoRepository) {
        this.equipoRepository = equipoRepository;
    }

    public List<Equipo> obtenerTodos() {
        List<Equipo> ordenados = new ArrayList<>(equipoRepository.obtenerTodos());
        ordenados.sort(Comparator.comparing(Equipo::isCritico).reversed());
        return ordenados;
    }

    public Optional<Equipo> buscarPorId(String idEquipo) {
        if (idEquipo == null || idEquipo.isBlank()) {
            return Optional.empty();
        }
        return equipoRepository.buscarPorId(idEquipo.trim());
    }

    public Equipo obtenerPorId(String idEquipo) {
        String id = ValidacionesEntrada.requerido(idEquipo, "código del equipo", 30);
        return buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo " + id + "."));
    }

    public Equipo registrarEquipo(String id, String nombre, String ubicacion) {
        String codigo = ValidacionesEntrada.requerido(id, "código del equipo", 30);
        String nombreValidado = ValidacionesEntrada.requerido(nombre, "nombre del equipo", 120);
        String ubicacionValidada = ValidacionesEntrada.requerido(ubicacion, "ubicación", 160);
        validarNoDuplicado(codigo);
        return equipoRepository.guardar(new Equipo(codigo, nombreValidado, ubicacionValidada));
    }

    public Equipo registrarEquipo(String id, String marca, String modelo, String ubicacion) {
        DatosEquipo datos = validarDatos(id, marca, modelo, ubicacion);
        validarNoDuplicado(datos.id());
        return equipoRepository.guardar(new Equipo(datos.id(), datos.marca(), datos.modelo(), datos.ubicacion()));
    }

    @Transactional
    public Equipo actualizarEquipo(String id, String marca, String modelo, String ubicacion) {
        Equipo equipo = obtenerPorId(id);
        DatosEquipo datos = validarDatos(id, marca, modelo, ubicacion);
        equipo.actualizarDatos(datos.marca(), datos.modelo(), datos.ubicacion());
        return equipoRepository.guardar(equipo);
    }

    @Transactional
    public Equipo marcarFueraDeServicio(String idEquipo) {
        Equipo equipo = obtenerPorId(idEquipo);
        equipo.marcarFueraDeServicio();
        return equipoRepository.guardar(equipo);
    }

    public Equipo obtenerORegistrarEquipo(String id, String nombre, String ubicacion) {
        return buscarPorId(id).orElseGet(() -> registrarEquipo(id, nombre, ubicacion));
    }

    private DatosEquipo validarDatos(String id, String marca, String modelo, String ubicacion) {
        String codigo = ValidacionesEntrada.requerido(id, "código del equipo", 30);
        String marcaValidada = ValidacionesEntrada.requerido(marca, "marca", 120);
        String modeloValidado = ValidacionesEntrada.requerido(modelo, "modelo", 120);
        if ((marcaValidada + " " + modeloValidado).length() > 120) {
            throw new domain.exeptions.DatosInvalidosException("La marca y el modelo juntos no pueden superar 120 caracteres.");
        }
        return new DatosEquipo(codigo, marcaValidada, modeloValidado,
                ValidacionesEntrada.requerido(ubicacion, "ubicación", 160));
    }

    private void validarNoDuplicado(String id) {
        if (buscarPorId(id).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe un equipo registrado con el código " + id + ".");
        }
    }

    private record DatosEquipo(String id, String marca, String modelo, String ubicacion) {}
}

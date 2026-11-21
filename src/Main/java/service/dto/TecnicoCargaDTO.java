package service.dto;

import domain.model.EstadoDisponibilidad;

public class TecnicoCargaDTO {
    private String idUsuario;
    private String nombre;
    private String departamento;
    private String especialidad;
    private int casosActivos;
    private EstadoDisponibilidad estado;
    private String porcentajeOcupacion;

    public TecnicoCargaDTO(String idUsuario, String nombre, String departamento, String especialidad,
                           int casosActivos, EstadoDisponibilidad estado) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.departamento = departamento;
        this.especialidad = especialidad;
        this.casosActivos = casosActivos;
        this.estado = estado;
        this.porcentajeOcupacion = (casosActivos * 20) + "%"; // 1 caso = 20% de 5
    }

    // Getters
    public String getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getDepartamento() { return departamento; }
    public String getEspecialidad() { return especialidad; }
    public int getCasosActivos() { return casosActivos; }
    public EstadoDisponibilidad getEstado() { return estado; }
    public String getPorcentajeOcupacion() { return porcentajeOcupacion; }
}

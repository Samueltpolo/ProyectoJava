package edu.puj.negocio;

import edu.puj.excepciones.FechaInvalidaException;
import edu.puj.excepciones.PlacaInvalidaException;
import edu.puj.excepciones.VehiculoNoEncontradoException;
import edu.puj.excepciones.VehiculoYaAdentroException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import edu.puj.modelo.Convenio;
import edu.puj.modelo.Mensualidad;
import edu.puj.modelo.Tiquete;
import edu.puj.modelo.Vehiculo;

public interface IParqueadero {

    void agregarMensualidad(Mensualidad mensualidad)
            throws FechaInvalidaException;

    Tiquete registrarEntrada(Vehiculo  vehiculo, LocalDateTime entrada)
        throws PlacaInvalidaException, VehiculoYaAdentroException;

    Tiquete registrarSalida(String placa, LocalDateTime salida, Convenio convenio)
        throws VehiculoNoEncontradoException, FechaInvalidaException;

    List<String> reporteVehiculosAdentro();

    List<String> reporteCierre(LocalDate fecha);

    List<String> reportePlacasFrecuentes();

    List<Tiquete> getTiquetes();
}

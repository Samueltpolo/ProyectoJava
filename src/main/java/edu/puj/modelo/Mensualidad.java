package edu.puj.modelo;

import edu.puj.excepciones.FechaInvalidaException;
import java.io.Serializable;
import java.time.LocalDate;

public class Mensualidad implements Serializable {

    private static final long serialVersionUID = 1L;
    private String placa;
    private LocalDate inicio;
    private LocalDate fin;

    public Mensualidad(String placa, LocalDate inicio, LocalDate fin)
        throws FechaInvalidaException {
        if (placa == null || placa.trim().isEmpty() || inicio == null || fin == null) {
            throw new IllegalArgumentException("La mensualidad debe tener placa y fechas validas.");
        }

        validarFechas(placa, inicio,fin);
        this.placa = placa;
        this.inicio = inicio;
        this.fin = fin;
    }

    private static void validarFechas(String placa, LocalDate inicio, LocalDate fin)
        throws FechaInvalidaException {
        if(fin.isBefore(inicio)) {
            throw new FechaInvalidaException("La mensualidad de " + placa + " termina el " + fin + ", antes de su inicio el " + inicio);
        }
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        if (placa == null || placa.trim().isEmpty()) {
            throw new IllegalArgumentException("La placa de la mensualidad no puede estar vacia.");
        }

        this.placa = placa;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio)
        throws FechaInvalidaException {
        if (inicio == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede estar vacia.");
        }

        validarFechas(placa, inicio, fin);
        this.inicio = inicio;
    }

    public LocalDate getFin() {
        return fin;
    }

    public void setFin(LocalDate fin)
        throws FechaInvalidaException {
        if (fin == null) {
            throw new IllegalArgumentException("La fecha final no puede estar vacia.");
        }

        validarFechas(placa, inicio, fin);
        this.fin = fin;
    }

    public boolean cubre(LocalDate fecha) {
        return fecha != null && !fecha .isBefore(inicio) && !fecha.isAfter(fin);
    }

    @Override
    public String toString() {
        return placa + " " + inicio + " a " + fin;
    }
}

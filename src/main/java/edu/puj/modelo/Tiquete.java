package edu.puj.modelo;

import edu.puj.excepciones.FechaInvalidaException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

public class Tiquete implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    private final int numero;
    private final Vehiculo vehiculo;
    private final LocalDateTime entrada;
    private LocalDateTime salida;
    private long valorParqueo;
    private long valorRecarga;
    private Convenio convenio;

    public Tiquete(int numero, Vehiculo vehiculo, LocalDateTime entrada) {
        if(numero <= 0 || vehiculo == null || entrada == null) {
            throw new IllegalArgumentException("Los datos del tiquete son invalidos.");
        }

        this.numero = numero;
        this.vehiculo = vehiculo;
        this.entrada = entrada;
    }

    public int getNumero() {
        return numero;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public LocalDateTime getSalida() {
        return salida;
    }

    public long getValorParqueo() {
        return valorParqueo;
    }

    public long getValorRecarga() {
        return valorRecarga;
    }

    public Convenio getConvenio() {
        return convenio;
    }

    public boolean estaAbierto() {
        return salida == null;
    }

    public long getMinutos() {
        if (estaAbierto()) {
            return 0;
        }

        return ChronoUnit.MINUTES.between(entrada, salida);
    }

    public void cerrar(LocalDateTime salida, Convenio convenio, boolean mensualidad)
        throws FechaInvalidaException {
        if (!estaAbierto()) {
            throw new IllegalStateException("El tiquete " + numero + "ya esta cerrado.");
        }

        if (salida == null || convenio == null) {
            throw new IllegalArgumentException("La salida y el convenio son obligatorios.");
        }

        if (salida.isBefore(entrada)) {
            throw new FechaInvalidaException("La salida de " + vehiculo.getPlaca() + " (" + salida.format(FORMATO) + ") es anterior a su entrada (" + entrada.format(FORMATO) + ")");
        }

        long minutos = ChronoUnit.MINUTES.between(entrada, salida);
        long minutosCobrables= Math.max(0, minutos - convenio.getMinutosGratis());
        long parqueoCalculado = mensualidad ? 0 : vehiculo.calcularTarifa(minutosCobrables);
        long recargaCalculada = vehiculo.cargoAdicional(minutos);

        this.valorParqueo = parqueoCalculado;
        this.valorRecarga = recargaCalculada;
        this.convenio = convenio;
        this.salida = salida;
    }

    public long getTotal() {
        return valorParqueo + valorRecarga;
    }

    @Override
    public String toString() {
        String base = "Tiquete " + numero + "-" +vehiculo.getPlaca() + "-" + vehiculo.getTipo() + "- Entrada: " + entrada.format(FORMATO);
        if (estaAbierto()) {
            return base + "- Abierto";
        }

        return base + "- Salida: " + salida.format(FORMATO) + "- Minutos: " + getMinutos() + "- Convenio: " + convenio + "- Parqueo: " + valorParqueo + "- Recarga: " + valorRecarga + "- Total: " + getTotal();
    }
}

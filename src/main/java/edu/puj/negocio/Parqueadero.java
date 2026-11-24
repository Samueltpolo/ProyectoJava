package edu.puj.negocio;

import edu.puj.excepciones.FechaInvalidaException;
import edu.puj.excepciones.PlacaInvalidaException;
import edu.puj.excepciones.VehiculoNoEncontradoException;
import edu.puj.excepciones.VehiculoYaAdentroException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import edu.puj.modelo.Convenio;
import edu.puj.modelo.Mensualidad;
import edu.puj.modelo.Tiquete;
import edu.puj.modelo.Vehiculo;

public class Parqueadero implements IParqueadero, Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    private final ArrayList<Mensualidad> mensualidades;
    private final ArrayList<Tiquete> tiquetes;
    private int siguientNumero;

    public Parqueadero() {
        mensualidades = new ArrayList<>();
        tiquetes = new ArrayList<>();
        siguientNumero = 1;
    }

    @Override
    public void agregarMensualidad(Mensualidad mensualidad)
        throws FechaInvalidaException {
        if (mensualidad == null) {
            throw new IllegalArgumentException("La mensualidad es obligatoria.");
        }

        if (mensualidad.getFin().isBefore(mensualidad.getInicio())) {
            throw new FechaInvalidaException("La mensualidad de " + mensualidad.getPlaca() + "termina el " + mensualidad.getFin() + "antes de si inicio el " + mensualidad.getInicio());
        }

        mensualidades.add(mensualidad);
    }

    @Override
    public Tiquete registrarEntrada(Vehiculo vehiculo, LocalDateTime entrada)
        throws PlacaInvalidaException, VehiculoYaAdentroException {
        if (vehiculo == null) {
            throw new IllegalArgumentException("Se debe especificar un vehiculo.");
        }

        if (!vehiculo.placaValida()) {
            throw new PlacaInvalidaException("La placa " + vehiculo.getPlaca() + "no es valida para un vehiculo " + vehiculo.getTipo());
        }

        if (buscarAbierto(vehiculo.getPlaca()) != null) {
            throw new VehiculoYaAdentroException("El vehiculo " + vehiculo.getPlaca() + "ya esta dentro del parqueadero.");
        }

        if (entrada == null) {
            throw new IllegalArgumentException("La fecha de entrada es obligatoria.");
        }

        Tiquete tiquete = new Tiquete(siguientNumero, vehiculo, entrada);
        tiquetes.add(tiquete);
        siguientNumero++;
        return tiquete;
    }

    @Override
    public Tiquete registrarSalida(String placa, LocalDateTime salida, Convenio convenio)
        throws VehiculoNoEncontradoException, FechaInvalidaException {
        Tiquete tiquete = buscarAbierto(placa);
        if (tiquete == null) {
            throw new VehiculoNoEncontradoException("No hay un tiquete abierto para la placa " + placa);
        }

        if (salida == null) {
            throw new IllegalArgumentException("La fecha de salida es obligatoria.");
        }

        if (salida.isBefore(tiquete.getEntrada())) {
            throw new FechaInvalidaException("La salida de " + placa + "(" + salida.format(FORMATO) + ") es anterior a su entrada (" + tiquete.getEntrada().format(FORMATO) + ")");
        }

        if (convenio == null) {
            throw new IllegalArgumentException("El convenio es obligatoria.");
        }

        boolean mensualidad = tieneMensualidad(placa, tiquete.getEntrada().toLocalDate());
        tiquete.cerrar(salida, convenio, mensualidad);
        return tiquete;
    }

    private Tiquete buscarAbierto(String placa) {
        for (Tiquete tiquete : tiquetes) {
            if (tiquete.estaAbierto() && tiquete.getVehiculo().getPlaca().equals(placa)) {
                return tiquete;
            }
        }

        return null;
    }

    private boolean tieneMensualidad(String placa, LocalDate fecha) {
        for (Mensualidad mensualidad : mensualidades) {
            if (mensualidad.getPlaca().equals(placa) && mensualidad.cubre(fecha)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> reporteVehiculosAdentro() {
        ArrayList<Tiquete> abiertos = new ArrayList<>();
        for (Tiquete tiquete : tiquetes) {
            if (tiquete.estaAbierto()) {
                abiertos.add(tiquete);
            }
        }

        Collections.sort(abiertos, Comparator.comparing(Tiquete::getEntrada).thenComparing(t -> t.getVehiculo().getPlaca()));
        ArrayList<String> lineas = new ArrayList<>();
        for (Tiquete tiquete : abiertos) {
            lineas.add(tiquete.getNumero() + " " + tiquete.getVehiculo().getPlaca() + " " + tiquete.getVehiculo().getTipo() + " " + tiquete.getEntrada().format(FORMATO));
        }

        return lineas;
    }

    @Override
    public List<String> reporteCierre(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de cierre es obligatoria.");
        }

        long totalParqueo = 0;
        long totalRecarga = 0;
        int totalTiquetes = 0;
        int[] cantidadPorTipo = new int[3];
        long[] valorPorTipo = new long[3];

        for (Tiquete tiquete : tiquetes) {
            if (!tiquete.estaAbierto() && tiquete.getSalida().toLocalDate().equals(fecha)) {
                totalTiquetes++;
                totalParqueo += tiquete.getValorParqueo();
                totalRecarga += tiquete.getValorRecarga();
                int posicion = posicionTipo(tiquete.getVehiculo().getTipo());
                cantidadPorTipo[posicion]++;
                valorPorTipo[posicion] += tiquete.getTotal();
            }
        }

        String[] tipos = {"CARRO", "MOTO", "ELECTRICO"};
        ArrayList<String> lineas = new ArrayList<>();
        lineas.add("CIERRE " + fecha);
        lineas.add("tiquetes=" + totalTiquetes);
        lineas.add("parqueo=" + totalParqueo);
        lineas.add("recarga=" + totalRecarga);
        lineas.add("total=" + (totalParqueo + totalRecarga));
        for (int i = 0; i < tipos.length; i++) {
            lineas.add(tipos[i] + " tiquetes=" + cantidadPorTipo[i] + " total=" + valorPorTipo[i]);
        }

        return lineas;
    }

    private int posicionTipo(String tipo) {
        switch (tipo) {
            case "CARRO":
                return 0;
            case "MOTO":
                return 1;
            case "ELECTRICO":
                return 2;
            default:
                throw  new IllegalArgumentException("Tipo de vehhiculo desconocido: " + tipo);
        }
    }

    private static class ResumenPlaca {
        private final String placa;
        private int visitas;
        private long pagado;
        private ResumenPlaca(String placa) {
            this.placa = placa;
        }
    }

    @Override
    public List<String> reportePlacasFrecuentes() {
        ArrayList<ResumenPlaca> resumenes = new ArrayList<>();
        for (Tiquete tiquete : tiquetes) {
            if (tiquete.estaAbierto()) {
                continue;
            }

            String placa = tiquete.getVehiculo().getPlaca();
            ResumenPlaca encontrado = null;
            for (ResumenPlaca resumen : resumenes) {
                if (resumen.placa.equals(placa)) {
                    encontrado = resumen;
                    break;
                }
            }

            if (encontrado == null) {
                encontrado = new ResumenPlaca(placa);
                resumenes.add(encontrado);
            }

            encontrado.visitas++;
            encontrado.pagado += tiquete.getTotal();
        }

        Collections.sort(resumenes, (a, b) -> {
            int comparacion = Integer.compare(b.visitas, a.visitas);
            if (comparacion != 0) {
                return comparacion;
            }

            comparacion = Long.compare(b.pagado, a.pagado);
            if (comparacion != 0) {
                return comparacion;
            }

            return a.placa.compareTo(b.placa);
        });

        ArrayList<String> lineas = new ArrayList<>();
        for (int i = 0; i < Math.min(3, resumenes.size()); i++) {
            ResumenPlaca resumen = resumenes.get(i);
            lineas.add((i + 1) + ". " + resumen.placa + " visitas=" + resumen.visitas + " pagado=" + resumen.pagado);
        }

        return lineas;
    }

    @Override
    public List<Tiquete> getTiquetes() {
        return new ArrayList<>(tiquetes);
    }
}

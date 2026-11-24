package edu.puj.persistencia;

import edu.puj.excepciones.ParqueaderoException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import edu.puj.modelo.Carro;
import edu.puj.modelo.CarroElectrico;
import edu.puj.modelo.Convenio;
import edu.puj.modelo.Mensualidad;
import edu.puj.modelo.Moto;
import edu.puj.modelo.Tiquete;
import edu.puj.modelo.Vehiculo;
import edu.puj.negocio.IParqueadero;
import edu.puj.negocio.Parqueadero;

public class ManejoArchivos {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public List<String> cargarMensualidades(String ruta, IParqueadero sistema)
        throws IOException {
        validarSistema(sistema);
        Path archivo = Paths.get(ruta, "Mensualidades.txt");
        ArrayList<String> resultados = new ArrayList<>();
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (ignorarLinea(linea)) {
                    continue;
                }

                try {
                    String[] partes = linea.split("\\*", -1);
                    if (partes.length != 3) {
                        throw new IllegalArgumentException("La mensualidad debe tener tres campos.");
                    }

                    String placa = partes[0].trim();
                    LocalDate inicio = LocalDate.parse(partes[1].trim(), FORMATO_FECHA);
                    LocalDate fin = LocalDate.parse(partes[2].trim(), FORMATO_FECHA);
                    Mensualidad mensualidad = new Mensualidad(placa, inicio, fin);
                    sistema.agregarMensualidad(mensualidad);
                    resultados.add("Linea " + numeroLinea + ": mensualidad guardada - " + mensualidad);
                } catch (ParqueaderoException e) {

                    resultados.add("Linea " + numeroLinea +  ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } catch (DateTimeParseException e) {

                    resultados.add("Linea " + numeroLinea + ": fecha invalida (use AAAA-MM-DD)");
                }catch (IllegalArgumentException e) {
                    resultados.add("Linea " + numeroLinea + ": " + e.getMessage());
                }
            }
        }

        return resultados;
    }

    public List<String> procesarMovimientos(String ruta, IParqueadero sistema)
        throws IOException {
        validarSistema(sistema);
        Path archivo = Paths.get(ruta, "Movimientos.txt");
        ArrayList<String> resultados = new ArrayList<>();
        try (BufferedReader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) !=null) {
                numeroLinea++;
                if (ignorarLinea(linea)) {
                    continue;
                }

                try {
                    String[] partes = linea.split("\\*", -1);
                    if (partes.length != 4) {
                        throw  new IllegalArgumentException("El movimiento debe tener 4 campos.");
                    }

                    String operacion = partes[0].trim();
                    String placa = partes[1].trim();
                    if ("E".equals(operacion)) {
                        Vehiculo vehiculo = crearVehiculo(partes[2].trim(), placa);
                        LocalDateTime entrada = LocalDateTime.parse(partes[3].trim(), FORMATO_FECHA_HORA);
                        Tiquete tiquete = sistema.registrarEntrada(vehiculo, entrada);
                        resultados.add("Linea " + numeroLinea + ": entrada - " + tiquete);
                    } else if ("S".equals(operacion)) {
                        LocalDateTime salida = LocalDateTime.parse(partes[2].trim(), FORMATO_FECHA_HORA);
                        Convenio convenio;
                        try {
                            convenio = Convenio.valueOf(partes[3].trim());
                        } catch (IllegalArgumentException e) {

                            throw new IllegalArgumentException("Convenio desconocido: " + partes [3].trim());
                        }

                        Tiquete tiquete = sistema.registrarSalida(placa, salida, convenio);
                        resultados.add("Linea " + numeroLinea + ": salida - " + tiquete);
                    } else {

                        throw new IllegalArgumentException("Movimiento desconocido: " + "(Solo E y S");
                    }

                } catch (ParqueaderoException e) {

                    resultados.add("Linea " + numeroLinea + ": " + e.getClass().getSimpleName() + " _ " + e.getMessage());
                } catch (DateTimeParseException e) {

                    resultados.add("Linea " + numeroLinea + ": fecha hora invalida (use AAAA-MM-DD HH:mm)");
                } catch (IllegalArgumentException e) {
                    resultados.add("Linea " + numeroLinea + ": " + e.getMessage());
                }
            }
        }

        return resultados;
    }

    private boolean ignorarLinea(String linea) {
        String texto = linea.trim();
        return texto.isEmpty() || texto.startsWith("#");
    }

    private void validarSistema(IParqueadero sistema) {
        if (sistema == null) {
            throw new IllegalArgumentException("El sistema de parqueadero no existe.");
        }
    }

    private Vehiculo crearVehiculo(String tipo, String placa) {
        switch (tipo) {
            case "CARRO":
                return new Carro(placa);
            case "MOTO":
                return new Moto(placa);
            case "ELECTRICO":
                return new CarroElectrico(placa);
            default:
                throw new IllegalArgumentException("Tipo de vehiculo desconocido: " + tipo);
        }
    }

    public void exportarReporte(List<String> lineas, String ruta) throws IOException {
        if (lineas == null) {
            throw new IllegalArgumentException("El reporte no puede ser nulo");
        }

        Files.write(Paths.get(ruta), lineas, StandardCharsets.UTF_8);
    }

    public void guardarSistema(IParqueadero sistema, String ruta) throws IOException {
        validarSistema(sistema);
        try (ObjectOutputStream salida = new ObjectOutputStream(Files.newOutputStream(Paths.get(ruta)))) {
            salida.writeObject(sistema);
        }
    }

    public IParqueadero cargarSistema(String ruta) throws IOException,ClassNotFoundException {
        try (ObjectInputStream entrada = new ObjectInputStream(Files.newInputStream(Paths.get(ruta)))) {
            try {
                return (IParqueadero) entrada.readObject();
            } catch (ClassCastException e) {

                throw new IOException("El archivo no contiene un sistema de parqueadero.", e);
            }
        }
    }
}

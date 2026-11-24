package edu.puj.presentacion;

import edu.puj.excepciones.ParqueaderoException;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import edu.puj.modelo.Carro;
import edu.puj.modelo.CarroElectrico;
import edu.puj.modelo.Convenio;
import edu.puj.modelo.Moto;
import edu.puj.modelo.Tiquete;
import edu.puj.modelo.Vehiculo;
import edu.puj.negocio.IParqueadero;
import edu.puj.negocio.Parqueadero;
import edu.puj.persistencia.ManejoArchivos;

public class Consola {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final String ARCHIVO_SISTEMA = "sistema.dat";

    private  IParqueadero sistema;
    private final Scanner lector;
    private final ManejoArchivos archivos;

    public Consola() {
        sistema = new Parqueadero();
        lector = new Scanner(System.in);
        archivos = new ManejoArchivos();
    }

    public static void main(String[] args) {
        Consola consola = new Consola();
        consola.iniciar();
    }

    private void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            String texto = pedir("Selecciona una opcion");
            try {
                opcion = Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Debe ingresar un numero de opcion valido.");
                opcion = -1;
            }

            try {
                switch (opcion) {
                    case 1:
                        cargarMensualidades();
                        break;
                    case 2:
                        procesarMovimientos();
                        break;
                    case 3:
                        registrarEntrada();
                        break;
                    case 4:
                        registrarSalida();
                        break;
                    case 5:
                        mostrarLineas(sistema.reporteVehiculosAdentro(), "No hay vehiculos dentro del parqueadero.");
                        break;
                    case 6:
                        cierreCaja();
                        break;
                    case 7:
                        mostrarLineas(sistema.reportePlacasFrecuentes(), "No hay tiquetes cerrados para mostrar.");
                        break;
                    case 8:
                        archivos.guardarSistema(sistema, ARCHIVO_SISTEMA);
                        System.out.println("Sistema guardado en " + Paths.get(ARCHIVO_SISTEMA).toAbsolutePath());
                        break;
                    case 9:
                        IParqueadero recuperado = archivos.cargarSistema(ARCHIVO_SISTEMA);
                        sistema = recuperado;
                        System.out.println("Sistema recuperado correctamente.");
                        break;
                    case 0:
                        System.out.println("Salir.");
                        break;
                    default:
                        System.out.println("Opcion no valida.");
                }

            } catch (ParqueaderoException e) {

                System.out.println(e.getClass().getSimpleName() +": " + e.getMessage());
            } catch (DateTimeParseException e) {

                System.out.println("Fecha o hora invalida.");
            } catch (NumberFormatException e) {

                System.out.println("Numero invalido: " + e.getMessage());
            } catch (IOException e) {

                System.out.println("Error de archivo: " + e.getMessage());
            } catch (ClassNotFoundException e) {

                System.out.println("No se pudo reconocer el sistema guardado: " + e.getMessage());
            } catch (IllegalArgumentException e) {

                System.out.println("Dato invalido: " + e.getMessage());
            }

        } while (opcion !=0);
    }

    private void mostrarMenu() {
        System.out.println("\nGestion del parqueadero");
        System.out.println("1. Cargar mensualidades.");
        System.out.println("2. Procesar movimientos.");
        System.out.println("3. Registrar una entrada.");
        System.out.println("4. Registrar una salida.");
        System.out.println("5. Ver los vehiculos adentro.");
        System.out.println("6. Cierre de caja de un dia.");
        System.out.println("7. Placas frecuentes.");
        System.out.println("8. Guardar el sistema.");
        System.out.println("9. Cargar el sistema.");
        System.out.println("0. Salir.");
    }

    private String pedir(String mensaje) {
        System.out.print(mensaje);
        return lector.nextLine().trim();
    }

    private void cargarMensualidades() throws IOException {
        String carpeta = pedir("Carpeta que contiene Mensualidades.txt: ");
        mostrarLineas(archivos.cargarMensualidades(carpeta, sistema), "No se encontraron registros.");
    }

    private void procesarMovimientos() throws IOException {
        String carpeta = pedir("Carpeta que contiene Movimientos.txt: ");
        mostrarLineas(archivos.procesarMovimientos(carpeta, sistema), "No se encontraron movimientos.");
    }

    private void registrarEntrada() throws ParqueaderoException {
        String placa = pedir("Placa: ");
        String tipo = pedir("Tipo: ").toUpperCase(Locale.ROOT);
        LocalDateTime fecha = leerFechaHora();
        Vehiculo vehiculo = crearVehiculo(tipo, placa);
        Tiquete tiquete = sistema.registrarEntrada(vehiculo, fecha);
        System.out.println("Entrada registrada: " + tiquete);
    }

    private void registrarSalida() throws ParqueaderoException {
        String placa = pedir("Placa: ");
        LocalDateTime fecha = leerFechaHora();
        String textoConvenio = pedir("Convenio (NINGUNO, ALMACEN, RESTAURANTE, CINE): ").toUpperCase(Locale.ROOT);
        Convenio convenio;
        try {
            convenio = Convenio.valueOf(textoConvenio);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("Convenio desconocido: " + textoConvenio);
        }
        
        Tiquete tiquete = sistema.registrarSalida(placa, fecha, convenio);
        System.out.println("Salida registrada: " + tiquete);
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

    private LocalDateTime leerFechaHora() {
        String texto = pedir("Fecha y hora (AAAA-MM-DD HH:mm): ");
        return LocalDateTime.parse(texto, FORMATO_FECHA_HORA);
    }

    private void cierreCaja() throws IOException {
        String texto = pedir("Fecha del cierre (AAAA-MM-DD): ");
        LocalDate fecha = LocalDate.parse(texto, FORMATO_FECHA);
        List<String> reporte = sistema.reporteCierre(fecha);
        mostrarLineas(reporte, "Sin datos. ");
        String nombre = "Cierre_" + fecha + ".txt";
        archivos.exportarReporte(reporte, nombre);
        System.out.println("Reporte exportado en " + Paths.get(nombre).toAbsolutePath());
    }

    private void mostrarLineas(List<String> lineas, String mensajeVacio) {
        if (lineas.isEmpty()) {
            System.out.println(mensajeVacio);
            return;
        }
        for (String linea : lineas) {
            System.out.println(linea);
        }
    }
}

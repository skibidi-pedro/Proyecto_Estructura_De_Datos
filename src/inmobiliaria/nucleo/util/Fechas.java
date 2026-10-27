package inmobiliaria.nucleo.util;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class Fechas {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String convertirAString(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return fecha.format(FORMATO);
    }

    public static LocalDate convertirALocalDate(String fechaStr) {
        if (fechaStr == null || fechaStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(fechaStr.trim(), FORMATO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static boolean esPosterior(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return false;
        }
        return fechaFin.isAfter(fechaInicio);
    }

    public static long calcularDiasDiferencia(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin);
    }
}

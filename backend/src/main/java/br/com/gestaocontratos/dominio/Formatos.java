package br.com.gestaocontratos.dominio;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação no padrão brasileiro para mensagens ao usuário. */
public final class Formatos {

    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatos() {
    }

    /** 50000 -> "50.000,00". */
    public static String moeda(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        return formato.format(valor);
    }

    /** 259200 segundos -> "3d 0h 0min". */
    public static String duracao(long segundos) {
        long dias = segundos / 86_400;
        long horas = segundos % 86_400 / 3_600;
        long minutos = segundos % 3_600 / 60;
        return dias + "d " + horas + "h " + minutos + "min";
    }
}

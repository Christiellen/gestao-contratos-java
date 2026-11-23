package br.com.gestaocontratos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CnpjTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = ';', value = {
        "11.222.333/0001-81; true",
        "11222333000181; true",
        "45.997.418/0001-53; true",
        "11.222.333/0001-82; false",
        "11111111111111; false",
        "1122233300018; false",
        "''; false",
    })
    void validaPelosDigitosVerificadores(String cnpj, boolean esperado) {
        assertThat(Cnpj.valido(cnpj)).isEqualTo(esperado);
    }

    @Test
    void removeAMascara() {
        assertThat(Cnpj.somenteDigitos("11.222.333/0001-81")).isEqualTo("11222333000181");
    }
}

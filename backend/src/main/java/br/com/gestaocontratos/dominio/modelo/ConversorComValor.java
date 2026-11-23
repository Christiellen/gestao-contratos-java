package br.com.gestaocontratos.dominio.modelo;

import br.com.gestaocontratos.dominio.ComValor;
import br.com.gestaocontratos.dominio.FaseOrdemServico;
import br.com.gestaocontratos.dominio.Papel;
import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.TipoAditivo;
import br.com.gestaocontratos.dominio.TipoFiscal;
import br.com.gestaocontratos.dominio.TipoOrdemServico;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Grava os enums pelo valor em texto ("em_preparacao"), não pelo nome nem pela posição. */
public abstract class ConversorComValor<E extends Enum<E> & ComValor> implements AttributeConverter<E, String> {

    private final Class<E> tipo;

    protected ConversorComValor(Class<E> tipo) {
        this.tipo = tipo;
    }

    @Override
    public String convertToDatabaseColumn(E atributo) {
        return atributo == null ? null : atributo.valor();
    }

    @Override
    public E convertToEntityAttribute(String coluna) {
        return coluna == null ? null : ComValor.deValorObrigatorio(tipo, coluna);
    }

    @Converter
    public static class DePerfil extends ConversorComValor<Perfil> {
        public DePerfil() {
            super(Perfil.class);
        }
    }

    @Converter
    public static class DeTipoAditivo extends ConversorComValor<TipoAditivo> {
        public DeTipoAditivo() {
            super(TipoAditivo.class);
        }
    }

    @Converter
    public static class DeTipoFiscal extends ConversorComValor<TipoFiscal> {
        public DeTipoFiscal() {
            super(TipoFiscal.class);
        }
    }

    @Converter
    public static class DePapel extends ConversorComValor<Papel> {
        public DePapel() {
            super(Papel.class);
        }
    }

    @Converter
    public static class DeTipoOrdemServico extends ConversorComValor<TipoOrdemServico> {
        public DeTipoOrdemServico() {
            super(TipoOrdemServico.class);
        }
    }

    @Converter
    public static class DeFase extends ConversorComValor<FaseOrdemServico> {
        public DeFase() {
            super(FaseOrdemServico.class);
        }
    }
}

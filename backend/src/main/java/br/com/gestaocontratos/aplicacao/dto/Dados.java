package br.com.gestaocontratos.aplicacao.dto;

/** Envelope {"data": ...} usado pela API (mesmo formato dos Resources do Laravel). */
public record Dados<T>(T data) {
}

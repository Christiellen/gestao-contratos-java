package br.com.gestaocontratos.web;

import br.com.gestaocontratos.aplicacao.AcessoNegadoException;
import br.com.gestaocontratos.aplicacao.RecursoNaoEncontradoException;
import br.com.gestaocontratos.aplicacao.ValidacaoException;
import br.com.gestaocontratos.dominio.RegraNegocioException;
import br.com.gestaocontratos.seguranca.MuitasTentativasException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Erros no mesmo formato da API em PHP, que o front-end já sabe exibir:
 * 422 {"message", "errors": {"campo": ["..."]}} para validação e {"message"} para o resto.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ValidacaoException.class)
    ResponseEntity<Map<String, Object>> validacao(ValidacaoException e) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("message", e.getMessage());
        corpo.put("errors", e.getErros());
        return ResponseEntity.unprocessableEntity().body(corpo);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<Map<String, Object>> regra(RegraNegocioException e) {
        return mensagem(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ResponseEntity<Map<String, Object>> acessoNegado(AcessoNegadoException e) {
        return mensagem(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoResourceFoundException.class})
    ResponseEntity<Map<String, Object>> naoEncontrado(Exception e) {
        String texto = e instanceof RecursoNaoEncontradoException ? e.getMessage() : "Endereço não encontrado.";
        return mensagem(HttpStatus.NOT_FOUND, texto);
    }

    @ExceptionHandler(MuitasTentativasException.class)
    ResponseEntity<Map<String, Object>> muitasTentativas(MuitasTentativasException e) {
        return mensagem(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    /** JSON malformado ou valor com tipo errado (ex.: data inválida): aponta o campo quando possível. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> corpoInvalido(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof JsonMappingException mapeamento && !mapeamento.getPath().isEmpty()) {
            String campo = mapeamento.getPath().stream()
                    .map(r -> r.getFieldName() != null ? r.getFieldName() : String.valueOf(r.getIndex()))
                    .collect(Collectors.joining("."));
            return validacao(ValidacaoException.doCampo(campo, "O valor informado para " + campo + " não é válido."));
        }
        return mensagem(HttpStatus.UNPROCESSABLE_ENTITY, "O corpo da requisição não é um JSON válido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        String campo = PropertyNamingStrategies.SnakeCaseStrategy.INSTANCE.translate(e.getName());
        return validacao(ValidacaoException.doCampo(campo, "O valor informado para " + campo + " não é válido."));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<Map<String, Object>> cabecalhoAusente(MissingRequestHeaderException e) {
        return mensagem(HttpStatus.BAD_REQUEST, "Cabeçalho obrigatório ausente: " + e.getHeaderName() + ".");
    }

    /** Última barreira: a restrição única do banco pegou um conflito que a validação não viu (ex.: corrida). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> conflito(DataIntegrityViolationException e) {
        return mensagem(HttpStatus.CONFLICT, "O registro conflita com outro já existente. Atualize a página e tente de novo.");
    }

    private static ResponseEntity<Map<String, Object>> mensagem(HttpStatus status, String texto) {
        return ResponseEntity.status(status).body(Map.of("message", texto));
    }
}

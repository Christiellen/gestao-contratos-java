package br.com.gestaocontratos.suporte;

import br.com.gestaocontratos.aplicacao.Relogio;
import br.com.gestaocontratos.dominio.Papel;
import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.TipoFiscal;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.DesignacaoFiscal;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.ContratoRepositorio;
import br.com.gestaocontratos.repositorio.DesignacaoFiscalRepositorio;
import br.com.gestaocontratos.repositorio.UsuarioRepositorio;
import br.com.gestaocontratos.seguranca.LimitadorLogin;
import br.com.gestaocontratos.seguranca.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de API: PostgreSQL real (Testcontainers, um contêiner para toda a suíte),
 * Flyway aplicando as migrations, MockMvc e um relógio controlável.
 */
@SpringBootTest(properties = "gestao-contratos.avisos.agendamento-habilitado=false")
@AutoConfigureMockMvc
@Import(TesteDeIntegracao.Configuracao.class)
public abstract class TesteDeIntegracao {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class Configuracao {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected RelogioDeTeste relogio;
    @Autowired
    protected Relogio relogioDaAplicacao;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected UsuarioRepositorio usuarios;
    @Autowired
    protected ContratoRepositorio contratos;
    @Autowired
    protected DesignacaoFiscalRepositorio designacoes;
    @Autowired
    private TokenService tokens;
    @Autowired
    private PasswordEncoder senhas;
    @Autowired
    private LimitadorLogin limitador;

    @BeforeEach
    void limparBase() {
        jdbc.execute("TRUNCATE eventos_ordem_servico, medicoes_indicadores, ordens_servico, indicadores, empenhos, prepostos,"
                + " designacoes_fiscais, alertas_vencimento, aditivos, contratos, tokens_acesso, users RESTART IDENTITY CASCADE");
        limitador.limpar();
        relogio.irPara("2026-05-04T09:00:00");
    }

    // ---- dados de teste (o papel das factories do Laravel)

    protected Usuario usuario(Perfil perfil) {
        int n = SEQUENCIA.incrementAndGet();
        return usuario(perfil, "pessoa" + n + "@exemplo.gov.br");
    }

    protected Usuario usuario(Perfil perfil, String email) {
        return usuarios.save(new Usuario("Pessoa " + email, email, senhas.encode("password"), perfil));
    }

    protected Contrato contrato(Usuario gestor, int venceEmDias) {
        return contrato(gestor, null, venceEmDias, "Empresa " + SEQUENCIA.incrementAndGet() + " Ltda", "Serviços diversos");
    }

    protected Contrato contrato(Usuario gestor, Usuario fiscal, int venceEmDias, String contratada, String objeto) {
        LocalDate hoje = relogioDaAplicacao.hoje();
        return contratos.save(new Contrato("%04d/2026".formatted(SEQUENCIA.incrementAndGet()), objeto, contratada,
                "11222333000181", new BigDecimal("1000000.00"), hoje.minusYears(1), hoje.plusDays(venceEmDias), gestor, fiscal));
    }

    protected DesignacaoFiscal designar(Contrato contrato, Usuario fiscal, TipoFiscal tipo, LocalDate fim) {
        return designacoes.save(new DesignacaoFiscal(contrato, fiscal, "Portaria nº 1/2026", tipo, Papel.TITULAR,
                relogioDaAplicacao.hoje().minusMonths(6), fim));
    }

    // ---- requisições autenticadas

    protected String token(Usuario usuario) {
        return tokens.emitir(usuario);
    }

    protected ResultActions get(String url, Usuario como) throws Exception {
        return mvc.perform(autenticado(MockMvcRequestBuilders.get(url), como));
    }

    protected ResultActions post(String url, Usuario como, Object corpo) throws Exception {
        return mvc.perform(comCorpo(autenticado(MockMvcRequestBuilders.post(url), como), corpo));
    }

    protected ResultActions put(String url, Usuario como, Object corpo) throws Exception {
        return mvc.perform(comCorpo(autenticado(MockMvcRequestBuilders.put(url), como), corpo));
    }

    protected ResultActions delete(String url, Usuario como) throws Exception {
        return mvc.perform(autenticado(MockMvcRequestBuilders.delete(url), como));
    }

    protected ResultActions login(String email, String senha) throws Exception {
        return mvc.perform(comCorpo(MockMvcRequestBuilders.post("/api/login"), Map.of("email", email, "password", senha)));
    }

    /** Requisição com um token específico (ou sem token, se null). */
    protected ResultActions getComToken(String url, String token) throws Exception {
        MockHttpServletRequestBuilder requisicao = MockMvcRequestBuilders.get(url).accept(MediaType.APPLICATION_JSON);
        return mvc.perform(token == null ? requisicao : requisicao.header("Authorization", "Bearer " + token));
    }

    protected ResultActions postComToken(String url, String token) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).header("Authorization", "Bearer " + token));
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder requisicao, Usuario como) {
        return requisicao.header("Authorization", "Bearer " + token(como)).accept(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder comCorpo(MockHttpServletRequestBuilder requisicao, Object corpo) throws Exception {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
    }

    /** Lê um campo da resposta JSON (ex.: "$.data.id"). */
    protected <T> T ler(ResultActions resposta, String caminho) throws Exception {
        return JsonPath.read(resposta.andReturn().getResponse().getContentAsString(), caminho);
    }

    /** Evita o conflito entre os métodos get/post/put/delete desta classe e os do MockMvcRequestBuilders. */
    private static final class MockMvcRequestBuilders {
        static MockHttpServletRequestBuilder get(String url) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url);
        }

        static MockHttpServletRequestBuilder post(String url) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url);
        }

        static MockHttpServletRequestBuilder put(String url) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url);
        }

        static MockHttpServletRequestBuilder delete(String url) {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url);
        }
    }
}

package br.com.gestaocontratos.infraestrutura;

import br.com.gestaocontratos.aplicacao.Relogio;
import br.com.gestaocontratos.dominio.CalculadoraGlosa;
import br.com.gestaocontratos.dominio.Faixa;
import br.com.gestaocontratos.dominio.FaseOrdemServico;
import br.com.gestaocontratos.dominio.Papel;
import br.com.gestaocontratos.dominio.Perfil;
import br.com.gestaocontratos.dominio.TipoFiscal;
import br.com.gestaocontratos.dominio.TipoOrdemServico;
import br.com.gestaocontratos.dominio.modelo.Contrato;
import br.com.gestaocontratos.dominio.modelo.DesignacaoFiscal;
import br.com.gestaocontratos.dominio.modelo.Empenho;
import br.com.gestaocontratos.dominio.modelo.Indicador;
import br.com.gestaocontratos.dominio.modelo.MedicaoIndicador;
import br.com.gestaocontratos.dominio.modelo.OrdemServico;
import br.com.gestaocontratos.dominio.modelo.Preposto;
import br.com.gestaocontratos.dominio.modelo.Usuario;
import br.com.gestaocontratos.repositorio.ContratoRepositorio;
import br.com.gestaocontratos.repositorio.DesignacaoFiscalRepositorio;
import br.com.gestaocontratos.repositorio.EmpenhoRepositorio;
import br.com.gestaocontratos.repositorio.IndicadorRepositorio;
import br.com.gestaocontratos.repositorio.OrdemServicoRepositorio;
import br.com.gestaocontratos.repositorio.PrepostoRepositorio;
import br.com.gestaocontratos.repositorio.UsuarioRepositorio;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dados fictícios de demonstração (SEED_DEMO=true). Senha de todos os usuários: "password".
 * Só roda com a base vazia.
 */
@Component
@ConditionalOnProperty(name = "gestao-contratos.dados-demonstracao", havingValue = "true")
public class DadosDemonstracao implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DadosDemonstracao.class);

    private static final List<String> OBJETOS = List.of(
            "Prestação de serviços de desenvolvimento e sustentação de sistemas",
            "Fornecimento de licenças de software de banco de dados",
            "Serviços de suporte técnico em infraestrutura de TI",
            "Serviços de computação em nuvem",
            "Serviços de impressão corporativa",
            "Serviços de fábrica de dados e business intelligence");

    private static final List<String> EMPRESAS = List.of(
            "Alfa Sistemas Ltda", "Beta Nuvem S.A.", "Gama Dados Ltda", "Delta Redes Ltda", "Épsilon Software Ltda",
            "Zeta Infraestrutura S.A.", "Ômega Tecnologia Ltda", "Sigma Consultoria Ltda", "Kappa Serviços Digitais Ltda",
            "Lambda Integração Ltda");

    private final UsuarioRepositorio usuarios;
    private final ContratoRepositorio contratos;
    private final DesignacaoFiscalRepositorio designacoes;
    private final PrepostoRepositorio prepostos;
    private final EmpenhoRepositorio empenhos;
    private final IndicadorRepositorio indicadores;
    private final OrdemServicoRepositorio ordens;
    private final PasswordEncoder senhas;
    private final TransactionTemplate transacao;
    private final Relogio relogio;
    private final Random sorteio = new Random(42);
    private final CalculadoraGlosa glosas = new CalculadoraGlosa();

    public DadosDemonstracao(UsuarioRepositorio usuarios, ContratoRepositorio contratos, DesignacaoFiscalRepositorio designacoes,
                             PrepostoRepositorio prepostos, EmpenhoRepositorio empenhos, IndicadorRepositorio indicadores,
                             OrdemServicoRepositorio ordens, PasswordEncoder senhas, TransactionTemplate transacao, Relogio relogio) {
        this.usuarios = usuarios;
        this.contratos = contratos;
        this.designacoes = designacoes;
        this.prepostos = prepostos;
        this.empenhos = empenhos;
        this.indicadores = indicadores;
        this.ordens = ordens;
        this.senhas = senhas;
        this.transacao = transacao;
        this.relogio = relogio;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            LOG.info("Base já possui usuários; dados de demonstração não foram inseridos.");
            return;
        }
        transacao.executeWithoutResult(status -> popular());
        LOG.info("Dados de demonstração inseridos. Entre com admin@exemplo.gov.br / password.");
    }

    private void popular() {
        String senha = senhas.encode("password");
        Usuario admin = usuarios.save(new Usuario("Administração", "admin@exemplo.gov.br", senha, Perfil.ADMINISTRADOR));
        Usuario gestora = usuarios.save(new Usuario("Ana Gestora", "gestora@exemplo.gov.br", senha, Perfil.GESTOR));
        Usuario gestor = usuarios.save(new Usuario("Bruno Gestor", "gestor@exemplo.gov.br", senha, Perfil.GESTOR));
        Usuario fiscal = usuarios.save(new Usuario("Carla Fiscal", "fiscal@exemplo.gov.br", senha, Perfil.GESTOR));
        Usuario fiscalAdm = usuarios.save(new Usuario("Diego Fiscal Adm", "fiscal.adm@exemplo.gov.br", senha, Perfil.GESTOR));
        usuarios.save(new Usuario("Consulta", "consulta@exemplo.gov.br", senha, Perfil.CONSULTA));

        LocalDate hoje = relogio.hoje();
        // Um contrato em cada faixa de aviso, para demonstrar o painel e os alertas.
        int[] vencimentos = {-10, 15, 28, 45, 59, 75, 88, 150, 400, 700};
        for (int i = 0; i < vencimentos.length; i++) {
            contratos.save(contrato(i + 1, hoje.minusYears(1), hoje.plusDays(vencimentos[i]), i % 2 == 0 ? gestora : gestor, fiscal));
        }
        for (int i = 0; i < 10; i++) {
            contratos.save(contrato(i + 11, hoje.minusYears(2), hoje.plusDays(sorteio.nextInt(-60, 720)), admin, null));
        }
        fabricaDeSoftware(gestora, fiscal, fiscalAdm, hoje);
    }

    private Contrato contrato(int sequencial, LocalDate inicio, LocalDate fim, Usuario gestor, Usuario fiscal) {
        return new Contrato("%03d/%d".formatted(sequencial, inicio.getYear()), OBJETOS.get(sorteio.nextInt(OBJETOS.size())),
                EMPRESAS.get(sequencial % EMPRESAS.size()), cnpj(sequencial),
                BigDecimal.valueOf(sorteio.nextLong(5_000_000L, 1_500_000_000L), 2), inicio, fim, gestor, fiscal);
    }

    /** Contrato de fábrica de software com fiscais, preposto, empenho, indicadores e OS em várias fases. */
    private void fabricaDeSoftware(Usuario gestora, Usuario fiscalTecnica, Usuario fiscalAdm, LocalDate hoje) {
        LocalDate inicio = LocalDate.of(2025, 6, 1);
        Contrato contrato = contratos.save(new Contrato("17/2025",
                "Desenvolvimento e sustentação de sistemas com métodos ágeis, por alocação de perfis profissionais "
                        + "com pagamento vinculado a resultados",
                "Fábrica Exemplo Tecnologia Ltda", "11222333000181", new BigDecimal("12000000.00"), inicio,
                hoje.plusMonths(7), gestora, null));

        designacoes.save(new DesignacaoFiscal(contrato, fiscalTecnica, "Portaria nº 210/2025", TipoFiscal.TECNICO, Papel.TITULAR, inicio, null));
        designacoes.save(new DesignacaoFiscal(contrato, fiscalAdm, "Portaria nº 210/2025", TipoFiscal.ADMINISTRATIVO, Papel.TITULAR, inicio, null));
        prepostos.save(new Preposto(contrato, "Rafael Preposto", "preposto@fabricaexemplo.com.br", "(61) 3000-0000",
                Papel.TITULAR, inicio, null));
        Empenho empenho = empenhos.save(new Empenho(contrato, hoje.getYear() + "NE000101", hoje.getYear(),
                new BigDecimal("900000.00"), "Empenho estimativo " + hoje.getYear()));

        List<Faixa> faixas = List.of(faixa(95, 0), faixa(90, 2), faixa(80, 5), faixa(0, 10));
        Indicador iap = indicadores.save(new Indicador(contrato, "IAP", "Atendimento de prazo", null, new BigDecimal("95"), faixas));
        Indicador iqc = indicadores.save(new Indicador(contrato, "IQC", "Qualidade de código", null, new BigDecimal("95"), faixas));

        record Plano(String descricao, int valor, FaseOrdemServico alvo, Integer iap, Integer iqc, int diasAtras) {
        }
        List<Plano> planos = List.of(
                new Plano("Sprint 1 - módulo de cadastro", 120_000, FaseOrdemServico.ENCERRADA, 100, 96, 120),
                new Plano("Sprint 2 - relatórios gerenciais", 135_000, FaseOrdemServico.ENCERRADA, 92, 88, 95),
                new Plano("Sustentação - março", 48_000, FaseOrdemServico.PAGAMENTO_AUTORIZADO, 97, 99, 70),
                new Plano("Sprint 3 - integração com o SEI", 150_000, FaseOrdemServico.NOTA_FISCAL, 85, 95, 50),
                new Plano("Sprint 4 - painel de indicadores", 110_000, FaseOrdemServico.FISCALIZACAO_ADMINISTRATIVA, 100, 91, 35),
                new Plano("Sprint 5 - aplicativo móvel", 160_000, FaseOrdemServico.FISCALIZACAO_TECNICA, null, null, 20),
                new Plano("Sustentação - maio", 48_000, FaseOrdemServico.EM_EXECUCAO, null, null, 10),
                new Plano("Sprint 6 - API pública", 140_000, FaseOrdemServico.EM_PREPARACAO, null, null, 2));

        Map<Integer, Integer> sequencialPorAno = new HashMap<>();
        for (Plano p : planos) {
            LocalDate dia = hoje.minusDays(p.diasAtras());
            Instant momento = dia.atTime(LocalTime.of(9, 0)).atZone(relogio.fuso()).toInstant();
            int seq = sequencialPorAno.merge(dia.getYear(), 1, Integer::sum);
            TipoOrdemServico tipo = p.descricao().startsWith("Sustentação") ? TipoOrdemServico.SUSTENTACAO : TipoOrdemServico.PROJETO;

            OrdemServico os = new OrdemServico(contrato, empenho, "%04d/%d".formatted(seq, dia.getYear()), tipo, p.descricao(),
                    BigDecimal.valueOf(p.valor()).setScale(2), dia.plusDays(30), gestora);
            os.registrarCriacao(gestora, momento);

            while (os.getFase() != p.alvo()) {
                // Intervalos variados entre as fases, para o relatório de tempo médio.
                momento = momento.plus(Duration.ofHours(sorteio.nextInt(6, 97)));
                FaseOrdemServico atual = os.getFase();
                if (atual == FaseOrdemServico.FISCALIZACAO_TECNICA) {
                    medir(os, iap, p.iap(), fiscalTecnica);
                    medir(os, iqc, p.iqc(), fiscalTecnica);
                    BigDecimal percentual = glosas.percentualTotal(
                            os.getMedicoes().stream().map(MedicaoIndicador::getPercentualGlosa).toList());
                    os.aplicarGlosa(percentual, glosas.aplicar(os.getValorEstimado(), percentual));
                }
                Usuario quem = switch (atual) {
                    case FISCALIZACAO_TECNICA -> fiscalTecnica;
                    case FISCALIZACAO_ADMINISTRATIVA -> fiscalAdm;
                    default -> gestora;
                };
                os.mudarFase(atual.proxima().orElseThrow(), "avanco", null, quem, momento);
            }
            ordens.save(os);
        }
    }

    private void medir(OrdemServico os, Indicador indicador, int realizado, Usuario fiscal) {
        BigDecimal r = BigDecimal.valueOf(realizado);
        BigDecimal resultado = glosas.resultado(r, BigDecimal.valueOf(100));
        os.registrarMedicao(indicador, r, BigDecimal.valueOf(100), resultado,
                glosas.glosaDaFaixa(indicador.getFaixas(), resultado), null, fiscal);
    }

    private static Faixa faixa(int minimo, int glosa) {
        return new Faixa(BigDecimal.valueOf(minimo), BigDecimal.valueOf(glosa));
    }

    /** CNPJ fictício com dígitos verificadores válidos. */
    static String cnpj(int semente) {
        StringBuilder base = new StringBuilder("%08d0001".formatted(10_000_000 + semente * 7_919));
        for (int posicao : new int[] {12, 13}) {
            int soma = 0;
            int peso = posicao - 7;
            for (int i = 0; i < posicao; i++) {
                soma += (base.charAt(i) - '0') * peso--;
                if (peso < 2) {
                    peso = 9;
                }
            }
            base.append(soma % 11 < 2 ? 0 : 11 - soma % 11);
        }
        return base.toString();
    }
}

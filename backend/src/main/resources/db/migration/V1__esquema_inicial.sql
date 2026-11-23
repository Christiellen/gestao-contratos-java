-- Esquema da gestão de contratos (PostgreSQL). Mesmas tabelas e colunas da versão em PHP.

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL DEFAULT 'consulta' CHECK (perfil IN ('administrador', 'gestor', 'consulta')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Tokens de acesso da API. Só o hash SHA-256 é gravado: um vazamento do banco não expõe tokens válidos.
CREATE TABLE tokens_acesso (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    hash        VARCHAR(64) NOT NULL UNIQUE,
    expira_em   TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE contratos (
    id            BIGSERIAL PRIMARY KEY,
    numero        VARCHAR(30)    NOT NULL UNIQUE,
    objeto        VARCHAR(500)   NOT NULL,
    contratada    VARCHAR(200)   NOT NULL,
    cnpj          VARCHAR(14)     NOT NULL,
    valor_global  NUMERIC(15, 2) NOT NULL CHECK (valor_global > 0),
    data_inicio   DATE           NOT NULL,
    data_fim      DATE           NOT NULL,
    gestor_id     BIGINT         NOT NULL REFERENCES users (id),
    fiscal_id     BIGINT         REFERENCES users (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT contratos_vigencia CHECK (data_fim > data_inicio)
);
CREATE INDEX contratos_data_fim_idx ON contratos (data_fim);

CREATE TABLE aditivos (
    id                 BIGSERIAL PRIMARY KEY,
    contrato_id        BIGINT         NOT NULL REFERENCES contratos (id) ON DELETE CASCADE,
    numero             INTEGER        NOT NULL,
    tipo               VARCHAR(20)    NOT NULL,
    data_assinatura    DATE           NOT NULL,
    data_fim_anterior  DATE,
    nova_data_fim      DATE,
    valor_acrescimo    NUMERIC(15, 2),
    descricao          VARCHAR(500),
    registrado_por     BIGINT         NOT NULL REFERENCES users (id),
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (contrato_id, numero)
);

CREATE TABLE alertas_vencimento (
    id                   BIGSERIAL PRIMARY KEY,
    contrato_id          BIGINT      NOT NULL REFERENCES contratos (id) ON DELETE CASCADE,
    marco                INTEGER     NOT NULL,
    data_fim_referencia  DATE        NOT NULL,
    destinatarios        INTEGER     NOT NULL,
    enviado_em           TIMESTAMPTZ NOT NULL,
    -- Garante que o mesmo aviso não seja enviado duas vezes para a mesma vigência.
    CONSTRAINT alerta_unico_por_vigencia UNIQUE (contrato_id, marco, data_fim_referencia)
);

-- Designação de fiscais por portaria.
CREATE TABLE designacoes_fiscais (
    id           BIGSERIAL PRIMARY KEY,
    contrato_id  BIGINT      NOT NULL REFERENCES contratos (id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    portaria     VARCHAR(60) NOT NULL,
    tipo         VARCHAR(20) NOT NULL,
    papel        VARCHAR(20) NOT NULL,
    data_inicio  DATE        NOT NULL,
    data_fim     DATE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX designacoes_contrato_tipo_idx ON designacoes_fiscais (contrato_id, tipo);

-- Representantes da empresa contratada.
CREATE TABLE prepostos (
    id           BIGSERIAL PRIMARY KEY,
    contrato_id  BIGINT       NOT NULL REFERENCES contratos (id) ON DELETE CASCADE,
    nome         VARCHAR(150) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    telefone     VARCHAR(20),
    papel        VARCHAR(20)  NOT NULL,
    data_inicio  DATE         NOT NULL,
    data_fim     DATE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE empenhos (
    id           BIGSERIAL PRIMARY KEY,
    contrato_id  BIGINT         NOT NULL REFERENCES contratos (id) ON DELETE CASCADE,
    numero       VARCHAR(20)    NOT NULL,
    ano          INTEGER        NOT NULL,
    valor        NUMERIC(15, 2) NOT NULL,
    descricao    VARCHAR(300),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (contrato_id, numero)
);

-- Indicadores do instrumento de medição de resultado (IMR) do contrato.
CREATE TABLE indicadores (
    id           BIGSERIAL PRIMARY KEY,
    contrato_id  BIGINT        NOT NULL REFERENCES contratos (id) ON DELETE CASCADE,
    sigla        VARCHAR(10)   NOT NULL,
    nome         VARCHAR(150)  NOT NULL,
    descricao    VARCHAR(500),
    meta         NUMERIC(5, 2) NOT NULL,
    faixas       JSONB         NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (contrato_id, sigla)
);

CREATE TABLE ordens_servico (
    id                BIGSERIAL PRIMARY KEY,
    contrato_id       BIGINT         NOT NULL REFERENCES contratos (id),
    empenho_id        BIGINT         REFERENCES empenhos (id),
    numero            VARCHAR(20)    NOT NULL,
    tipo              VARCHAR(20)    NOT NULL,
    descricao         VARCHAR(500)   NOT NULL,
    valor_estimado    NUMERIC(15, 2) NOT NULL,
    percentual_glosa  NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    valor_glosa       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    valor_final       NUMERIC(15, 2),
    fase              VARCHAR(40)    NOT NULL,
    prazo_entrega     DATE           NOT NULL,
    criado_por        BIGINT         NOT NULL REFERENCES users (id),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (contrato_id, numero)
);
CREATE INDEX ordens_servico_fase_idx ON ordens_servico (fase);

CREATE TABLE medicoes_indicadores (
    id                BIGSERIAL PRIMARY KEY,
    ordem_servico_id  BIGINT         NOT NULL REFERENCES ordens_servico (id) ON DELETE CASCADE,
    indicador_id      BIGINT         NOT NULL REFERENCES indicadores (id),
    realizado         NUMERIC(12, 2) NOT NULL,
    previsto          NUMERIC(12, 2) NOT NULL,
    resultado         NUMERIC(6, 2)  NOT NULL,
    percentual_glosa  NUMERIC(5, 2)  NOT NULL,
    justificativa     VARCHAR(500),
    registrado_por    BIGINT         NOT NULL REFERENCES users (id),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    UNIQUE (ordem_servico_id, indicador_id)
);

-- Diário de eventos: cada mudança de fase fica registrada.
CREATE TABLE eventos_ordem_servico (
    id                BIGSERIAL PRIMARY KEY,
    ordem_servico_id  BIGINT       NOT NULL REFERENCES ordens_servico (id) ON DELETE CASCADE,
    fase_anterior     VARCHAR(40),
    fase_nova         VARCHAR(40)  NOT NULL,
    acao              VARCHAR(20)  NOT NULL,
    observacao        VARCHAR(500),
    user_id           BIGINT       NOT NULL REFERENCES users (id),
    ocorrido_em       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX eventos_os_ocorrido_idx ON eventos_ordem_servico (ordem_servico_id, ocorrido_em);

export type Perfil = 'administrador' | 'gestor' | 'consulta';
export type Situacao = 'vigente' | 'a_vencer' | 'vencido';
export type TipoAditivo = 'prazo' | 'valor' | 'prazo_valor';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  perfil_rotulo: string;
}

export interface Aditivo {
  id: number;
  numero: number;
  tipo: TipoAditivo;
  data_assinatura: string;
  data_fim_anterior: string | null;
  nova_data_fim: string | null;
  valor_acrescimo: number | null;
  descricao: string | null;
  registrado_em: string;
}

export interface Contrato {
  id: number;
  numero: string;
  objeto: string;
  contratada: string;
  cnpj: string;
  valor_global: number;
  data_inicio: string;
  data_fim: string;
  dias_para_vencer: number;
  situacao: Situacao;
  situacao_rotulo: string;
  gestor?: Usuario;
  fiscal?: Usuario | null;
  aditivos?: Aditivo[];
  empenhos?: EmpenhoContrato[];
  indicadores?: IndicadorContrato[];
  fiscais?: FiscalContrato[];
  prepostos?: PrepostoContrato[];
  pode_editar: boolean;
  pode_excluir: boolean;
}

export interface ContratoPayload {
  numero: string;
  objeto: string;
  contratada: string;
  cnpj: string;
  valor_global: number;
  data_inicio: string;
  data_fim: string;
  gestor_id: number;
  fiscal_id: number | null;
}

export interface AditivoPayload {
  tipo: TipoAditivo;
  data_assinatura: string;
  nova_data_fim?: string | null;
  valor_acrescimo?: number | null;
  descricao?: string | null;
}

export interface Paginado<T> {
  data: T[];
  meta: { current_page: number; last_page: number; per_page: number; total: number };
}

export interface Painel {
  totais: Record<Situacao, number>;
  valor_total_vigente: number;
  proximos_vencimentos: Contrato[];
}

export interface FiltrosContrato {
  situacao?: Situacao | '';
  busca?: string;
  page?: number;
  por_pagina?: number;
}

/** Resposta de erro de validação da API (HTTP 422): {"message", "errors": {"campo": ["..."]}}. */
export interface ErroValidacao {
  message: string;
  errors: Record<string, string[]>;
}

export type FaseOS =
  | 'em_preparacao'
  | 'em_execucao'
  | 'fiscalizacao_tecnica'
  | 'fiscalizacao_administrativa'
  | 'recebimento_definitivo'
  | 'nota_fiscal'
  | 'analise_pagamento'
  | 'pagamento_autorizado'
  | 'encerrada'
  | 'cancelada';

export const FASES_OS: { valor: FaseOS; rotulo: string }[] = [
  { valor: 'em_preparacao', rotulo: 'Em preparação' },
  { valor: 'em_execucao', rotulo: 'Em execução' },
  { valor: 'fiscalizacao_tecnica', rotulo: 'Fiscalização técnica' },
  { valor: 'fiscalizacao_administrativa', rotulo: 'Fiscalização administrativa' },
  { valor: 'recebimento_definitivo', rotulo: 'Recebimento definitivo' },
  { valor: 'nota_fiscal', rotulo: 'Nota fiscal' },
  { valor: 'analise_pagamento', rotulo: 'Análise de pagamento' },
  { valor: 'pagamento_autorizado', rotulo: 'Pagamento autorizado' },
  { valor: 'encerrada', rotulo: 'Encerrada' },
];

export interface Faixa {
  minimo: number;
  glosa: number;
}

export interface IndicadorContrato {
  id: number;
  sigla: string;
  nome: string;
  meta: number;
  faixas: Faixa[];
}

export interface EmpenhoContrato {
  id: number;
  numero: string;
  ano: number;
  valor: number;
  utilizado: number;
  saldo: number;
  descricao: string | null;
}

export interface FiscalContrato {
  id: number;
  nome: string;
  portaria: string;
  tipo: 'tecnico' | 'requisitante' | 'administrativo';
  tipo_rotulo: string;
  papel: 'titular' | 'substituto';
  data_inicio: string;
  data_fim: string | null;
}

export interface PrepostoContrato {
  id: number;
  nome: string;
  email: string;
  telefone: string | null;
  papel: 'titular' | 'substituto';
  ativo: boolean;
  data_inicio: string;
  data_fim: string | null;
}

export interface Medicao {
  indicador_id: number;
  sigla: string;
  realizado: number;
  previsto: number;
  resultado: number;
  percentual_glosa: number;
  justificativa: string | null;
}

export interface EventoOS {
  acao: 'criacao' | 'avanco' | 'devolucao' | 'cancelamento';
  de: string | null;
  para: string;
  observacao: string | null;
  usuario: string;
  ocorrido_em: string;
}

export interface OrdemServico {
  id: number;
  numero: string;
  tipo: 'projeto' | 'sustentacao' | 'dados';
  tipo_rotulo: string;
  descricao: string;
  valor_estimado: number;
  percentual_glosa: number;
  valor_glosa: number;
  valor_final: number | null;
  prazo_entrega: string;
  fase: FaseOS;
  fase_rotulo: string;
  proxima_fase: FaseOS | null;
  proxima_fase_rotulo: string | null;
  responsavel: string | null;
  contrato: { id: number; numero: string; contratada: string; indicadores: IndicadorContrato[] | null };
  empenho?: { id: number; numero: string; saldo: number } | null;
  medicoes?: Medicao[];
  eventos?: EventoOS[];
  permissoes: { avancar: boolean; medir: boolean; devolver: boolean; cancelar: boolean };
}

export interface TempoMedio {
  inicio: string;
  fim: string;
  transicoes: { de: FaseOS; para: FaseOS; rotulo: string; quantidade: number; media_segundos: number; media_texto: string }[];
}

export interface ValoresPendentes {
  fases: { fase: FaseOS; rotulo: string; quantidade: number; valor: number }[];
  total: number;
}

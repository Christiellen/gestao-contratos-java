import { HttpErrorResponse } from '@angular/common/http';
import { ErroValidacao } from './models';

/** Converte o erro da API em uma mensagem curta e nos erros por campo (422). */
export function interpretarErro(erro: unknown): { mensagem: string; campos: Record<string, string> } {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 422) {
      const corpo = erro.error as ErroValidacao;
      const campos: Record<string, string> = {};
      for (const [campo, mensagens] of Object.entries(corpo?.errors ?? {})) {
        campos[campo] = mensagens[0];
      }
      // Regra de negócio (sem erros por campo): mostra a mensagem da API.
      const mensagem = Object.keys(campos).length ? 'Revise os campos destacados.' : (corpo?.message ?? 'Operação não permitida.');
      return { mensagem, campos };
    }
    if (erro.status === 403) {
      return { mensagem: 'Você não tem permissão para esta ação.', campos: {} };
    }
    if (erro.status === 429) {
      return { mensagem: 'Muitas tentativas. Aguarde um minuto e tente de novo.', campos: {} };
    }
    if (erro.status === 0) {
      return { mensagem: 'Não foi possível falar com o servidor.', campos: {} };
    }
  }
  return { mensagem: 'Ocorreu um erro inesperado.', campos: {} };
}

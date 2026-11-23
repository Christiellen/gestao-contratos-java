import { HttpErrorResponse } from '@angular/common/http';
import { interpretarErro } from './erros';

describe('interpretarErro', () => {
  it('separa os erros de validação por campo', () => {
    const erro = new HttpErrorResponse({
      status: 422,
      error: { message: 'Dados inválidos', errors: { cnpj: ['CNPJ inválido.'] } },
    });
    expect(interpretarErro(erro)).toEqual({ mensagem: 'Revise os campos destacados.', campos: { cnpj: 'CNPJ inválido.' } });
  });

  it('mostra a mensagem da regra de negócio quando não há campos', () => {
    const erro = new HttpErrorResponse({ status: 422, error: { message: 'Esta etapa cabe a: Fiscal técnico.' } });
    expect(interpretarErro(erro).mensagem).toBe('Esta etapa cabe a: Fiscal técnico.');
  });

  it('traduz falta de permissão e servidor fora do ar', () => {
    expect(interpretarErro(new HttpErrorResponse({ status: 403 })).mensagem).toContain('permissão');
    expect(interpretarErro(new HttpErrorResponse({ status: 0 })).mensagem).toContain('servidor');
  });
});

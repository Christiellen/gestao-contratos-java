import { CnpjPipe } from './cnpj.pipe';
import { PrazoPipe } from './prazo.pipe';
import { cnpjValido } from './cnpj.validator';

describe('CnpjPipe', () => {
  const pipe = new CnpjPipe();

  it('formata 14 dígitos com máscara', () => {
    expect(pipe.transform('11222333000181')).toBe('11.222.333/0001-81');
  });

  it('devolve o valor original quando não tem 14 dígitos', () => {
    expect(pipe.transform('123')).toBe('123');
    expect(pipe.transform(null)).toBe('');
  });
});

describe('PrazoPipe', () => {
  const pipe = new PrazoPipe();

  it('descreve prazos futuros, o dia de hoje e vencidos', () => {
    expect(pipe.transform(30)).toBe('vence em 30 dias');
    expect(pipe.transform(1)).toBe('vence amanhã');
    expect(pipe.transform(0)).toBe('vence hoje');
    expect(pipe.transform(-1)).toBe('venceu há 1 dia');
    expect(pipe.transform(-12)).toBe('venceu há 12 dias');
  });
});

describe('cnpjValido', () => {
  it('aceita CNPJ válido com ou sem máscara', () => {
    expect(cnpjValido('11.222.333/0001-81')).toBeTrue();
    expect(cnpjValido('11222333000181')).toBeTrue();
  });

  it('rejeita dígito verificador errado e sequências repetidas', () => {
    expect(cnpjValido('11.222.333/0001-82')).toBeFalse();
    expect(cnpjValido('00000000000000')).toBeFalse();
    expect(cnpjValido('')).toBeFalse();
  });
});

import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Mesmo cálculo de dígitos verificadores usado na API (App\Rules\Cnpj). */
export function cnpjValido(valor: string): boolean {
  const cnpj = (valor ?? '').replace(/\D/g, '');
  if (cnpj.length !== 14 || /^(\d)\1{13}$/.test(cnpj)) {
    return false;
  }
  for (const posicao of [12, 13]) {
    let soma = 0;
    let peso = posicao - 7;
    for (let i = 0; i < posicao; i++) {
      soma += Number(cnpj[i]) * peso--;
      if (peso < 2) {
        peso = 9;
      }
    }
    const digito = soma % 11 < 2 ? 0 : 11 - (soma % 11);
    if (Number(cnpj[posicao]) !== digito) {
      return false;
    }
  }
  return true;
}

export function cnpjValidator(controle: AbstractControl): ValidationErrors | null {
  const valor = controle.value as string;
  if (!valor) {
    return null;
  }
  return cnpjValido(valor) ? null : { cnpj: true };
}

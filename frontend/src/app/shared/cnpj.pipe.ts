import { Pipe, PipeTransform } from '@angular/core';

/** Formata 14 dígitos como 00.000.000/0000-00. */
@Pipe({ name: 'cnpj' })
export class CnpjPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    const digitos = (valor ?? '').replace(/\D/g, '');
    if (digitos.length !== 14) {
      return valor ?? '';
    }
    return digitos.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
  }
}

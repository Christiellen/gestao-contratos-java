import { Pipe, PipeTransform } from '@angular/core';

/** Texto amigável para os dias que faltam até o vencimento. */
@Pipe({ name: 'prazo' })
export class PrazoPipe implements PipeTransform {
  transform(dias: number): string {
    if (dias < 0) {
      const passados = Math.abs(dias);
      return passados === 1 ? 'venceu há 1 dia' : `venceu há ${passados} dias`;
    }
    if (dias === 0) {
      return 'vence hoje';
    }
    return dias === 1 ? 'vence amanhã' : `vence em ${dias} dias`;
  }
}

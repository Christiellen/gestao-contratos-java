import { Component, computed, input } from '@angular/core';
import { Situacao } from '../core/models';

@Component({
  selector: 'app-situacao-badge',
  template: `<span class="badge" [class]="'badge badge--' + situacao()">{{ rotulo() }}</span>`,
})
export class SituacaoBadgeComponent {
  readonly situacao = input.required<Situacao>();
  readonly dias = input<number | null>(null);

  /** Contratos com 30 dias ou menos ganham destaque de urgência. */
  readonly rotulo = computed(() => {
    const dias = this.dias();
    if (this.situacao() === 'a_vencer' && dias !== null && dias <= 30) {
      return 'Urgente';
    }
    return { vigente: 'Vigente', a_vencer: 'A vencer', vencido: 'Vencido' }[this.situacao()];
  });
}

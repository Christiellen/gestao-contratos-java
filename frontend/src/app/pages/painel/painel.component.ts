import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContratosService } from '../../core/contratos.service';
import { Painel } from '../../core/models';
import { PrazoPipe } from '../../shared/prazo.pipe';
import { SituacaoBadgeComponent } from '../../shared/situacao-badge.component';

@Component({
  selector: 'app-painel',
  imports: [RouterLink, CurrencyPipe, DatePipe, PrazoPipe, SituacaoBadgeComponent],
  template: `
    <h1>Painel</h1>

    @if (painel(); as p) {
      <section class="indicadores" aria-label="Resumo">
        <a class="cartao indicador indicador--a_vencer" routerLink="/contratos" [queryParams]="{ situacao: 'a_vencer' }">
          <span class="indicador__valor">{{ p.totais.a_vencer }}</span>
          <span>a vencer em até 90 dias</span>
        </a>
        <a class="cartao indicador indicador--vencido" routerLink="/contratos" [queryParams]="{ situacao: 'vencido' }">
          <span class="indicador__valor">{{ p.totais.vencido }}</span>
          <span>vencidos</span>
        </a>
        <a class="cartao indicador" routerLink="/contratos" [queryParams]="{ situacao: 'vigente' }">
          <span class="indicador__valor">{{ p.totais.vigente }}</span>
          <span>vigentes com folga</span>
        </a>
        <div class="cartao indicador">
          <span class="indicador__valor indicador__valor--menor">{{ p.valor_total_vigente | currency: 'BRL' }}</span>
          <span>em contratos em vigor</span>
        </div>
      </section>

      <h2>Próximos vencimentos</h2>
      @if (p.proximos_vencimentos.length) {
        <table class="tabela cartao">
          <thead>
            <tr><th>Contrato</th><th>Contratada</th><th>Fim da vigência</th><th>Prazo</th><th>Gestor</th></tr>
          </thead>
          <tbody>
            @for (c of p.proximos_vencimentos; track c.id) {
              <tr>
                <td class="numero"><a [routerLink]="['/contratos', c.id]">{{ c.numero }}</a></td>
                <td>{{ c.contratada }}</td>
                <td>{{ c.data_fim | date: 'dd/MM/yyyy' }}</td>
                <td><app-situacao-badge [situacao]="c.situacao" [dias]="c.dias_para_vencer" /> {{ c.dias_para_vencer | prazo }}</td>
                <td>{{ c.gestor?.nome }}</td>
              </tr>
            }
          </tbody>
        </table>
      } @else {
        <div class="cartao vazio">Nenhum contrato vence nos próximos 90 dias.</div>
      }
    } @else if (erro()) {
      <div class="alerta alerta--erro">{{ erro() }}</div>
    } @else {
      <p class="suave">Carregando...</p>
    }
  `,
  styles: `
    .indicadores { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }
    .indicador { display: flex; flex-direction: column; gap: 0.25rem; text-decoration: none; color: var(--texto-suave); }
    .indicador__valor { font-size: 2rem; font-weight: 700; color: var(--azul); }
    .indicador__valor--menor { font-size: 1.3rem; padding-top: 0.6rem; }
    .indicador--a_vencer .indicador__valor { color: var(--amarelo); }
    .indicador--vencido .indicador__valor { color: var(--vermelho); }
  `,
})
export class PainelComponent {
  private readonly servico = inject(ContratosService);
  readonly painel = signal<Painel | null>(null);
  readonly erro = signal('');

  constructor() {
    this.servico.painel().subscribe({
      next: (p) => this.painel.set(p),
      error: () => this.erro.set('Não foi possível carregar o painel.'),
    });
  }
}

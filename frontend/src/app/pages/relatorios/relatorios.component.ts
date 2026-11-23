import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TempoMedio, ValoresPendentes } from '../../core/models';
import { OrdensService } from '../../core/ordens.service';

@Component({
  selector: 'app-relatorios',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  template: `
    <h1>Relatórios</h1>

    <h2>Valores pendentes de pagamento por fase</h2>
    @if (pendentes(); as p) {
      <table class="tabela cartao">
        <thead><tr><th>Fase</th><th class="direita">OS</th><th class="direita">Valor</th></tr></thead>
        <tbody>
          @for (l of p.fases; track l.fase) {
            <tr>
              <td><a routerLink="/ordens-servico" [queryParams]="{ fase: l.fase }">{{ l.rotulo }}</a></td>
              <td class="direita">{{ l.quantidade }}</td>
              <td class="direita">{{ l.valor | currency: 'BRL' }}</td>
            </tr>
          }
          <tr class="total"><td>Total</td><td></td><td class="direita">{{ p.total | currency: 'BRL' }}</td></tr>
        </tbody>
      </table>
    }

    <h2>Tempo médio por transição de fase</h2>
    <form class="filtros" (ngSubmit)="carregarTempos()">
      <label>De <input type="date" name="inicio" [(ngModel)]="inicio" /></label>
      <label>Até <input type="date" name="fim" [(ngModel)]="fim" /></label>
      <button class="botao botao--secundario" type="submit">Atualizar</button>
    </form>
    @if (tempos(); as t) {
      @if (t.transicoes.length) {
        <table class="tabela cartao">
          <thead><tr><th>Transição</th><th class="direita">Ocorrências</th><th class="direita">Tempo médio</th><th>Proporção</th></tr></thead>
          <tbody>
            @for (l of t.transicoes; track l.rotulo) {
              <tr>
                <td>{{ l.rotulo }}</td>
                <td class="direita">{{ l.quantidade }}</td>
                <td class="direita">{{ l.media_texto }}</td>
                <td class="barra-celula"><span class="barra" [style.width.%]="(l.media_segundos / maiorTempo()) * 100"></span></td>
              </tr>
            }
          </tbody>
        </table>
        <p class="suave">A barra mais longa mostra onde o processo mais demora.</p>
      } @else {
        <div class="cartao vazio">Nenhuma transição no período.</div>
      }
    }
  `,
  styles: `
    .filtros { display: flex; gap: 1rem; align-items: center; margin-bottom: 1rem; }
    .filtros input { font: inherit; padding: 0.4rem 0.6rem; border: 1px solid var(--borda); border-radius: 6px; margin-left: 0.4rem; }
    .total td { font-weight: 700; background: #fafbfd; }
    .barra-celula { width: 30%; }
    .barra { display: block; height: 10px; background: var(--azul); border-radius: 5px; min-width: 2px; }
  `,
})
export class RelatoriosComponent {
  private readonly servico = inject(OrdensService);

  readonly pendentes = signal<ValoresPendentes | null>(null);
  readonly tempos = signal<TempoMedio | null>(null);
  readonly maiorTempo = signal(1);
  inicio = new Date(Date.now() - 365 * 86400000).toISOString().slice(0, 10);
  fim = new Date().toISOString().slice(0, 10);

  constructor() {
    this.servico.valoresPendentes().subscribe((p) => this.pendentes.set(p));
    this.carregarTempos();
  }

  carregarTempos(): void {
    this.servico.tempoMedio(this.inicio, this.fim).subscribe((t) => {
      this.tempos.set(t);
      this.maiorTempo.set(Math.max(1, ...t.transicoes.map((l) => l.media_segundos)));
    });
  }
}

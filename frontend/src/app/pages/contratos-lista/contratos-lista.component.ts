import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ContratosService } from '../../core/contratos.service';
import { Contrato, Paginado, Situacao } from '../../core/models';
import { PrazoPipe } from '../../shared/prazo.pipe';
import { SituacaoBadgeComponent } from '../../shared/situacao-badge.component';

@Component({
  selector: 'app-contratos-lista',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, PrazoPipe, SituacaoBadgeComponent],
  template: `
    <div class="cabecalho">
      <h1>Contratos</h1>
      @if (auth.podeCadastrar()) {
        <a class="botao" routerLink="/contratos/novo">Novo contrato</a>
      }
    </div>

    <form class="filtros" (ngSubmit)="aplicar(1)">
      <input type="search" name="busca" placeholder="Buscar por número, objeto ou contratada"
             [(ngModel)]="buscaTexto" aria-label="Buscar" />
      <select name="situacao" [(ngModel)]="situacaoFiltro" (ngModelChange)="aplicar(1)" aria-label="Situação">
        <option value="">Todas as situações</option>
        <option value="a_vencer">A vencer (até 90 dias)</option>
        <option value="vencido">Vencidos</option>
        <option value="vigente">Vigentes</option>
      </select>
      <button class="botao botao--secundario" type="submit">Buscar</button>
    </form>

    @if (resultado(); as r) {
      @if (r.data.length) {
        <table class="tabela cartao">
          <thead>
            <tr>
              <th>Número</th><th>Objeto</th><th>Contratada</th>
              <th class="direita">Valor global</th><th>Fim da vigência</th><th>Situação</th>
            </tr>
          </thead>
          <tbody>
            @for (c of r.data; track c.id) {
              <tr>
                <td class="numero"><a [routerLink]="['/contratos', c.id]">{{ c.numero }}</a></td>
                <td>{{ c.objeto }}</td>
                <td>{{ c.contratada }}</td>
                <td class="direita">{{ c.valor_global | currency: 'BRL' }}</td>
                <td>{{ c.data_fim | date: 'dd/MM/yyyy' }}<br /><small class="suave">{{ c.dias_para_vencer | prazo }}</small></td>
                <td><app-situacao-badge [situacao]="c.situacao" [dias]="c.dias_para_vencer" /></td>
              </tr>
            }
          </tbody>
        </table>

        <nav class="paginacao" aria-label="Paginação">
          <button class="botao botao--secundario" type="button" [disabled]="r.meta.current_page <= 1"
                  (click)="aplicar(r.meta.current_page - 1)">Anterior</button>
          <span class="suave">Página {{ r.meta.current_page }} de {{ r.meta.last_page }} - {{ r.meta.total }} contratos</span>
          <button class="botao botao--secundario" type="button" [disabled]="r.meta.current_page >= r.meta.last_page"
                  (click)="aplicar(r.meta.current_page + 1)">Próxima</button>
        </nav>
      } @else {
        <div class="cartao vazio">Nenhum contrato encontrado com esses filtros.</div>
      }
    } @else {
      <p class="suave">Carregando...</p>
    }
  `,
  styles: `
    .filtros { display: flex; gap: 0.75rem; margin-bottom: 1rem; }
    .filtros input { flex: 1; }
    .filtros input, .filtros select { font: inherit; padding: 0.5rem 0.65rem; border: 1px solid var(--borda); border-radius: 6px; }
    .paginacao { display: flex; align-items: center; justify-content: center; gap: 1rem; margin-top: 1rem; }
  `,
})
export class ContratosListaComponent {
  readonly auth = inject(AuthService);
  private readonly servico = inject(ContratosService);
  private readonly router = inject(Router);

  /** Parâmetros da URL (?situacao=&busca=&page=), ligados pelo roteador. */
  readonly situacao = input<Situacao | ''>('');
  readonly busca = input<string>('');
  readonly page = input<string>('1');

  readonly resultado = signal<Paginado<Contrato> | null>(null);
  situacaoFiltro: Situacao | '' = '';
  buscaTexto = '';

  constructor() {
    effect(() => {
      this.situacaoFiltro = this.situacao() ?? '';
      this.buscaTexto = this.busca() ?? '';
      this.resultado.set(null);
      this.servico
        .listar({ situacao: this.situacao() ?? '', busca: this.busca() ?? '', page: Number(this.page() ?? 1) })
        .subscribe((r) => this.resultado.set(r));
    });
  }

  aplicar(pagina: number): void {
    this.router.navigate([], {
      queryParams: {
        situacao: this.situacaoFiltro || null,
        busca: this.buscaTexto.trim() || null,
        page: pagina > 1 ? pagina : null,
      },
    });
  }
}

import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { FASES_OS, FaseOS, OrdemServico, Paginado } from '../../core/models';
import { OrdensService } from '../../core/ordens.service';

@Component({
  selector: 'app-ordens-lista',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe],
  template: `
    <div class="cabecalho">
      <h1>Ordens de serviço</h1>
      @if (auth.podeCadastrar()) {
        <a class="botao" routerLink="/ordens-servico/nova">Nova OS</a>
      }
    </div>

    <form class="filtros" (ngSubmit)="aplicar()">
      <input type="search" name="busca" placeholder="Buscar por número ou descrição" [(ngModel)]="buscaTexto" aria-label="Buscar" />
      <select name="fase" [(ngModel)]="faseFiltro" (ngModelChange)="aplicar()" aria-label="Fase">
        <option value="">Todas as fases</option>
        @for (f of fases; track f.valor) { <option [value]="f.valor">{{ f.rotulo }}</option> }
        <option value="cancelada">Cancelada</option>
      </select>
      <button class="botao botao--secundario" type="submit">Buscar</button>
    </form>

    @if (resultado(); as r) {
      @if (r.data.length) {
        <table class="tabela cartao">
          <thead>
            <tr>
              <th>OS</th><th>Contrato</th><th>Descrição</th><th>Fase</th><th>Aguardando</th>
              <th class="direita">Valor</th><th>Entrega</th>
            </tr>
          </thead>
          <tbody>
            @for (os of r.data; track os.id) {
              <tr>
                <td class="numero"><a [routerLink]="['/ordens-servico', os.id]">{{ os.numero }}</a></td>
                <td class="numero">{{ os.contrato.numero }}</td>
                <td>{{ os.descricao }}<br /><small class="suave">{{ os.tipo_rotulo }}</small></td>
                <td><span class="fase" [class]="'fase fase--' + os.fase">{{ os.fase_rotulo }}</span></td>
                <td class="suave">{{ os.responsavel ?? '-' }}</td>
                <td class="direita">
                  {{ (os.valor_final ?? os.valor_estimado) | currency: 'BRL' }}
                  @if (os.percentual_glosa > 0) { <br /><small class="glosa">glosa {{ os.percentual_glosa }}%</small> }
                </td>
                <td>{{ os.prazo_entrega | date: 'dd/MM/yyyy' }}</td>
              </tr>
            }
          </tbody>
        </table>
        <nav class="paginacao" aria-label="Paginação">
          <button class="botao botao--secundario" type="button" [disabled]="r.meta.current_page <= 1" (click)="pagina(r.meta.current_page - 1)">Anterior</button>
          <span class="suave">Página {{ r.meta.current_page }} de {{ r.meta.last_page }} - {{ r.meta.total }} OS</span>
          <button class="botao botao--secundario" type="button" [disabled]="r.meta.current_page >= r.meta.last_page" (click)="pagina(r.meta.current_page + 1)">Próxima</button>
        </nav>
      } @else {
        <div class="cartao vazio">Nenhuma ordem de serviço encontrada.</div>
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
    .glosa { color: var(--vermelho); }
  `,
})
export class OrdensListaComponent {
  readonly auth = inject(AuthService);
  private readonly servico = inject(OrdensService);
  private readonly router = inject(Router);

  readonly fase = input<FaseOS | ''>('');
  readonly busca = input<string>('');
  readonly contrato_id = input<string>('');
  readonly page = input<string>('1');

  readonly fases = FASES_OS;
  readonly resultado = signal<Paginado<OrdemServico> | null>(null);
  faseFiltro: FaseOS | '' = '';
  buscaTexto = '';

  constructor() {
    effect(() => {
      this.faseFiltro = this.fase() ?? '';
      this.buscaTexto = this.busca() ?? '';
      this.resultado.set(null);
      this.servico
        .listar({
          fase: this.fase() ?? '',
          busca: this.busca() ?? '',
          contrato_id: this.contrato_id() ? Number(this.contrato_id()) : null,
          page: Number(this.page() ?? 1),
        })
        .subscribe((r) => this.resultado.set(r));
    });
  }

  aplicar(): void {
    this.router.navigate([], {
      queryParams: { fase: this.faseFiltro || null, busca: this.buscaTexto.trim() || null, page: null },
      queryParamsHandling: 'merge',
    });
  }

  pagina(n: number): void {
    this.router.navigate([], { queryParams: { page: n > 1 ? n : null }, queryParamsHandling: 'merge' });
  }
}

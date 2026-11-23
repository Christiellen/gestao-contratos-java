import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ContratosService } from '../../core/contratos.service';
import { interpretarErro } from '../../core/erros';
import { Contrato, Usuario } from '../../core/models';

type Secao = 'empenhos' | 'indicadores' | 'fiscais' | 'prepostos';

/** Empenhos, indicadores, fiscais e prepostos do contrato, com cadastro rápido. */
@Component({
  selector: 'app-contrato-cadastros',
  imports: [FormsModule, CurrencyPipe, DatePipe],
  template: `
    <nav class="abas" aria-label="Cadastros do contrato">
      @for (a of abas; track a.id) {
        <button type="button" [class.ativa]="aba() === a.id" (click)="trocar(a.id)">{{ a.rotulo }}</button>
      }
    </nav>

    @if (mensagem(); as m) {
      <div class="alerta" [class.alerta--erro]="m.tipo === 'erro'" [class.alerta--sucesso]="m.tipo === 'ok'">{{ m.texto }}</div>
    }

    @switch (aba()) {
      @case ('empenhos') {
        <table class="tabela cartao">
          <thead><tr><th>Empenho</th><th>Ano</th><th class="direita">Valor</th><th class="direita">Utilizado</th><th class="direita">Saldo</th></tr></thead>
          <tbody>
            @for (e of contrato().empenhos ?? []; track e.id) {
              <tr><td>{{ e.numero }}</td><td>{{ e.ano }}</td><td class="direita">{{ e.valor | currency: 'BRL' }}</td>
                <td class="direita">{{ e.utilizado | currency: 'BRL' }}</td><td class="direita"><strong>{{ e.saldo | currency: 'BRL' }}</strong></td></tr>
            } @empty { <tr><td colspan="5" class="vazio">Nenhum empenho.</td></tr> }
          </tbody>
        </table>
        @if (contrato().pode_editar) {
          <form class="linha" (ngSubmit)="salvar('empenhos', novoEmpenho)">
            <input name="numero" placeholder="2026NE000123" [(ngModel)]="novoEmpenho.numero" required aria-label="Número" />
            <input name="ano" type="number" placeholder="Ano" [(ngModel)]="novoEmpenho.ano" required aria-label="Ano" />
            <input name="valor" type="number" step="0.01" placeholder="Valor" [(ngModel)]="novoEmpenho.valor" required aria-label="Valor" />
            <button class="botao" type="submit">Adicionar empenho</button>
          </form>
        }
      }
      @case ('indicadores') {
        <table class="tabela cartao">
          <thead><tr><th>Sigla</th><th>Indicador</th><th>Meta</th><th>Faixas de glosa</th></tr></thead>
          <tbody>
            @for (i of contrato().indicadores ?? []; track i.id) {
              <tr><td><strong>{{ i.sigla }}</strong></td><td>{{ i.nome }}</td><td>{{ i.meta }}%</td>
                <td>@for (f of i.faixas; track f.minimo) { <span class="faixa">≥ {{ f.minimo }}%: {{ f.glosa }}%</span> }</td></tr>
            } @empty { <tr><td colspan="4" class="vazio">Nenhum indicador. Sem indicadores, a OS é paga pelo valor estimado.</td></tr> }
          </tbody>
        </table>
        @if (contrato().pode_editar) {
          <form class="linha" (ngSubmit)="salvarIndicador()">
            <input name="sigla" placeholder="Sigla" [(ngModel)]="novoIndicador.sigla" required aria-label="Sigla" class="curto" />
            <input name="nome" placeholder="Nome do indicador" [(ngModel)]="novoIndicador.nome" required aria-label="Nome" />
            <input name="meta" type="number" placeholder="Meta %" [(ngModel)]="novoIndicador.meta" required aria-label="Meta" class="curto" />
            <button class="botao" type="submit">Adicionar</button>
          </form>
          <p class="suave dica">Faixas padrão: ≥ meta sem glosa, ≥ 90% 2%, ≥ 80% 5% e abaixo disso 10%.</p>
        }
      }
      @case ('fiscais') {
        <table class="tabela cartao">
          <thead><tr><th>Servidor</th><th>Tipo</th><th>Papel</th><th>Portaria</th><th>Vigência</th></tr></thead>
          <tbody>
            @for (f of contrato().fiscais ?? []; track f.id) {
              <tr><td>{{ f.nome }}</td><td>{{ f.tipo_rotulo }}</td><td>{{ f.papel }}</td><td>{{ f.portaria }}</td>
                <td>{{ f.data_inicio | date: 'dd/MM/yyyy' }} a {{ f.data_fim ? (f.data_fim | date: 'dd/MM/yyyy') : 'atual' }}</td></tr>
            } @empty { <tr><td colspan="5" class="vazio">Nenhum fiscal designado.</td></tr> }
          </tbody>
        </table>
        @if (contrato().pode_editar) {
          <form class="linha" (ngSubmit)="salvar('fiscais', novoFiscal)">
            <select name="user" [(ngModel)]="novoFiscal.user_id" required aria-label="Servidor">
              <option [ngValue]="null" disabled>Servidor</option>
              @for (u of pessoas(); track u.id) { <option [ngValue]="u.id">{{ u.nome }}</option> }
            </select>
            <select name="tipo" [(ngModel)]="novoFiscal.tipo" aria-label="Tipo">
              <option value="tecnico">Técnico</option><option value="requisitante">Requisitante</option><option value="administrativo">Administrativo</option>
            </select>
            <select name="papel" [(ngModel)]="novoFiscal.papel" aria-label="Papel">
              <option value="titular">Titular</option><option value="substituto">Substituto</option>
            </select>
            <input name="portaria" placeholder="Portaria nº" [(ngModel)]="novoFiscal.portaria" required aria-label="Portaria" />
            <input name="inicio" type="date" [(ngModel)]="novoFiscal.data_inicio" required aria-label="Início" />
            <button class="botao" type="submit">Designar</button>
          </form>
        }
      }
      @case ('prepostos') {
        <table class="tabela cartao">
          <thead><tr><th>Nome</th><th>E-mail</th><th>Papel</th><th>Situação</th></tr></thead>
          <tbody>
            @for (p of contrato().prepostos ?? []; track p.id) {
              <tr><td>{{ p.nome }}</td><td>{{ p.email }}</td><td>{{ p.papel }}</td><td>{{ p.ativo ? 'Ativo' : 'Inativo' }}</td></tr>
            } @empty { <tr><td colspan="4" class="vazio">Nenhum preposto.</td></tr> }
          </tbody>
        </table>
        @if (contrato().pode_editar) {
          <form class="linha" (ngSubmit)="salvar('prepostos', novoPreposto)">
            <input name="nome" placeholder="Nome" [(ngModel)]="novoPreposto.nome" required aria-label="Nome" />
            <input name="email" type="email" placeholder="E-mail" [(ngModel)]="novoPreposto.email" required aria-label="E-mail" />
            <select name="papel" [(ngModel)]="novoPreposto.papel" aria-label="Papel">
              <option value="titular">Titular</option><option value="substituto">Substituto</option>
            </select>
            <input name="inicio" type="date" [(ngModel)]="novoPreposto.data_inicio" required aria-label="Início" />
            <button class="botao" type="submit">Adicionar</button>
          </form>
        }
      }
    }
  `,
  styles: `
    .abas { display: flex; gap: 0.25rem; border-bottom: 1px solid var(--borda); margin-bottom: 1rem; }
    .abas button { font: inherit; background: none; border: 0; padding: 0.5rem 1rem; cursor: pointer; color: var(--texto-suave); border-bottom: 3px solid transparent; }
    .abas button.ativa { color: var(--azul); border-bottom-color: var(--azul); font-weight: 600; }
    .linha { display: flex; gap: 0.5rem; margin-top: 0.75rem; flex-wrap: wrap; }
    .linha input, .linha select { font: inherit; padding: 0.45rem 0.6rem; border: 1px solid var(--borda); border-radius: 6px; }
    .linha .curto { width: 100px; }
    .faixa { display: inline-block; margin-right: 0.5rem; font-size: 0.8rem; background: #f1f4f8; padding: 0.1rem 0.4rem; border-radius: 4px; }
    .dica { font-size: 0.8rem; margin: 0.4rem 0 0; }
  `,
})
export class ContratoCadastrosComponent {
  private readonly servico = inject(ContratosService);

  readonly contrato = input.required<Contrato>();
  readonly alterado = output<void>();

  readonly abas: { id: Secao; rotulo: string }[] = [
    { id: 'empenhos', rotulo: 'Empenhos' },
    { id: 'indicadores', rotulo: 'Indicadores (IMR)' },
    { id: 'fiscais', rotulo: 'Fiscais' },
    { id: 'prepostos', rotulo: 'Prepostos' },
  ];
  readonly aba = signal<Secao>('empenhos');
  readonly mensagem = signal<{ tipo: 'ok' | 'erro'; texto: string } | null>(null);
  readonly pessoas = signal<Usuario[]>([]);

  novoEmpenho = { numero: '', ano: new Date().getFullYear(), valor: null as number | null };
  novoIndicador = { sigla: '', nome: '', meta: 95 };
  novoFiscal = { user_id: null as number | null, tipo: 'tecnico', papel: 'titular', portaria: '', data_inicio: '' };
  novoPreposto = { nome: '', email: '', papel: 'titular', data_inicio: '' };

  trocar(aba: Secao): void {
    this.aba.set(aba);
    this.mensagem.set(null);
    if (aba === 'fiscais' && !this.pessoas().length) {
      this.servico.gestores().subscribe((p) => this.pessoas.set(p));
    }
  }

  salvarIndicador(): void {
    const meta = Number(this.novoIndicador.meta);
    const faixas = [
      { minimo: meta, glosa: 0 },
      { minimo: 90, glosa: 2 },
      { minimo: 80, glosa: 5 },
      { minimo: 0, glosa: 10 },
    ].filter((f, i) => i === 0 || f.minimo < meta);
    this.salvar('indicadores', { ...this.novoIndicador, faixas });
  }

  salvar(recurso: Secao, dados: object): void {
    this.mensagem.set(null);
    this.enviar(this.servico.cadastrar(this.contrato().id, recurso, dados));
  }

  private enviar(req: Observable<unknown>): void {
    req.subscribe({
      next: () => {
        this.mensagem.set({ tipo: 'ok', texto: 'Cadastro salvo.' });
        this.novoEmpenho = { numero: '', ano: new Date().getFullYear(), valor: null };
        this.novoIndicador = { sigla: '', nome: '', meta: 95 };
        this.novoFiscal = { user_id: null, tipo: 'tecnico', papel: 'titular', portaria: '', data_inicio: '' };
        this.novoPreposto = { nome: '', email: '', papel: 'titular', data_inicio: '' };
        this.alterado.emit();
      },
      error: (e) => {
        const { mensagem, campos } = interpretarErro(e);
        this.mensagem.set({ tipo: 'erro', texto: Object.values(campos)[0] ?? mensagem });
      },
    });
  }
}

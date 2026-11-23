import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { interpretarErro } from '../../core/erros';
import { FASES_OS, OrdemServico } from '../../core/models';
import { OrdensService } from '../../core/ordens.service';

interface LinhaMedicao {
  indicador_id: number;
  sigla: string;
  nome: string;
  meta: number;
  realizado: number | null;
  previsto: number | null;
}

@Component({
  selector: 'app-ordem-detalhe',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe],
  template: `
    @if (os(); as os) {
      <div class="cabecalho">
        <h1>OS {{ os.numero }} <span class="fase" [class]="'fase fase--' + os.fase">{{ os.fase_rotulo }}</span></h1>
        <a class="botao botao--secundario" routerLink="/ordens-servico">Voltar</a>
      </div>

      <ol class="etapas" aria-label="Fases da OS">
        @for (f of fases; track f.valor; let i = $index) {
          <li [class.feita]="i < indiceAtual()" [class.atual]="i === indiceAtual()">{{ f.rotulo }}</li>
        }
      </ol>

      @if (mensagem(); as m) {
        <div class="alerta" [class.alerta--erro]="m.tipo === 'erro'" [class.alerta--sucesso]="m.tipo === 'ok'" role="status">{{ m.texto }}</div>
      }

      <section class="cartao dados">
        <div class="largo"><span class="suave">Descrição</span><p>{{ os.descricao }} <small class="suave">({{ os.tipo_rotulo }})</small></p></div>
        <div><span class="suave">Contrato</span><p><a [routerLink]="['/contratos', os.contrato.id]">{{ os.contrato.numero }}</a><br /><small>{{ os.contrato.contratada }}</small></p></div>
        <div><span class="suave">Valor estimado</span><p>{{ os.valor_estimado | currency: 'BRL' }}</p></div>
        <div>
          <span class="suave">Valor final</span>
          <p>
            @if (os.valor_final !== null) {
              {{ os.valor_final | currency: 'BRL' }}<br /><small class="glosa">glosa {{ os.percentual_glosa }}% ({{ os.valor_glosa | currency: 'BRL' }})</small>
            } @else { <span class="suave">após a medição</span> }
          </p>
        </div>
        <div><span class="suave">Empenho</span><p>{{ os.empenho?.numero ?? 'Não vinculado' }}@if (os.empenho) {<br /><small>saldo {{ os.empenho.saldo | currency: 'BRL' }}</small>}</p></div>
        <div><span class="suave">Prazo de entrega</span><p>{{ os.prazo_entrega | date: 'dd/MM/yyyy' }}</p></div>
        <div><span class="suave">Próxima etapa</span><p>{{ os.proxima_fase_rotulo ?? '-' }}<br /><small>{{ os.responsavel ?? '' }}</small></p></div>
      </section>

      @if (os.contrato.indicadores?.length) {
        <h2>Indicadores e glosa</h2>
        @if (os.permissoes.medir) {
          <form class="cartao" (ngSubmit)="salvarMedicoes(os)">
            <table class="tabela">
              <thead><tr><th>Indicador</th><th>Meta</th><th>Realizado</th><th>Previsto</th></tr></thead>
              <tbody>
                @for (l of linhas(); track l.indicador_id) {
                  <tr>
                    <td><strong>{{ l.sigla }}</strong> - {{ l.nome }}</td>
                    <td>{{ l.meta }}%</td>
                    <td><input type="number" step="0.01" min="0" [(ngModel)]="l.realizado" [name]="'r' + l.indicador_id" required [attr.aria-label]="'Realizado ' + l.sigla" /></td>
                    <td><input type="number" step="0.01" min="0.01" [(ngModel)]="l.previsto" [name]="'p' + l.indicador_id" required [attr.aria-label]="'Previsto ' + l.sigla" /></td>
                  </tr>
                }
              </tbody>
            </table>
            <div class="acoes"><button class="botao" type="submit" [disabled]="enviando()">Calcular glosa</button></div>
          </form>
        }
        @if (os.medicoes?.length) {
          <table class="tabela cartao medicoes">
            <thead><tr><th>Indicador</th><th class="direita">Realizado / previsto</th><th class="direita">Resultado</th><th class="direita">Glosa</th></tr></thead>
            <tbody>
              @for (m of os.medicoes; track m.indicador_id) {
                <tr>
                  <td>{{ m.sigla }}</td>
                  <td class="direita">{{ m.realizado | number: '1.0-2' }} / {{ m.previsto | number: '1.0-2' }}</td>
                  <td class="direita">{{ m.resultado | number: '1.2-2' }}%</td>
                  <td class="direita" [class.glosa]="m.percentual_glosa > 0">{{ m.percentual_glosa }}%</td>
                </tr>
              }
            </tbody>
          </table>
        } @else if (!os.permissoes.medir) {
          <div class="cartao vazio">As medições são registradas pelo fiscal técnico na fiscalização técnica.</div>
        }
      }

      @if (os.permissoes.avancar || os.permissoes.devolver || os.permissoes.cancelar) {
        <h2>Ações</h2>
        <div class="cartao">
          <div class="campo">
            <label for="obs">Observação ou motivo</label>
            <textarea id="obs" rows="2" [(ngModel)]="observacao" name="obs"></textarea>
          </div>
          <div class="acoes">
            @if (os.permissoes.avancar) {
              <button class="botao" type="button" [disabled]="enviando()" (click)="executar(ordens.avancar(os.id, observacao || null))">
                Avançar para {{ os.proxima_fase_rotulo }}
              </button>
            }
            @if (os.permissoes.devolver) {
              <button class="botao botao--secundario" type="button" [disabled]="enviando() || !observacao" (click)="executar(ordens.devolver(os.id, observacao))">
                Devolver para correção
              </button>
            }
            @if (os.permissoes.cancelar) {
              <button class="botao botao--perigo" type="button" [disabled]="enviando() || !observacao" (click)="executar(ordens.cancelar(os.id, observacao))">
                Cancelar OS
              </button>
            }
          </div>
          <p class="suave dica">Devolver e cancelar exigem o motivo.</p>
        </div>
      }

      <h2>Diário de eventos</h2>
      <ul class="cartao diario">
        @for (e of eventosRecentes(); track $index) {
          <li>
            <time>{{ e.ocorrido_em | date: 'dd/MM/yyyy HH:mm' }}</time>
            <div>
              <strong>{{ rotuloAcao[e.acao] }}</strong>
              @if (e.de) { {{ e.de }} → } {{ e.para }}
              <span class="suave">por {{ e.usuario }}</span>
              @if (e.observacao) { <p>{{ e.observacao }}</p> }
            </div>
          </li>
        }
      </ul>
    } @else if (erro()) {
      <div class="alerta alerta--erro">{{ erro() }}</div>
    } @else {
      <p class="suave">Carregando...</p>
    }
  `,
  styles: `
    h1 { display: flex; align-items: center; gap: 0.75rem; }
    .etapas { list-style: none; display: flex; gap: 0.25rem; padding: 0; margin: 0 0 1rem; flex-wrap: wrap; }
    .etapas li { flex: 1; min-width: 100px; font-size: 0.75rem; padding: 0.4rem; text-align: center; background: #e9edf2; color: var(--texto-suave); border-radius: 4px; }
    .etapas li.feita { background: var(--azul-claro); color: var(--azul); }
    .etapas li.atual { background: var(--azul); color: #fff; font-weight: 600; }
    .dados { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
    .dados .largo { grid-column: 1 / -1; }
    .dados p { margin: 0.2rem 0 0; }
    .glosa { color: var(--vermelho); }
    .medicoes { margin-top: 1rem; }
    td input { width: 110px; font: inherit; padding: 0.3rem 0.5rem; border: 1px solid var(--borda); border-radius: 4px; }
    .diario { list-style: none; margin: 0; }
    .diario li { display: flex; gap: 1rem; padding: 0.6rem 0; border-bottom: 1px solid var(--borda); }
    .diario li:last-child { border-bottom: 0; }
    .diario time { color: var(--texto-suave); white-space: nowrap; font-size: 0.85rem; padding-top: 0.1rem; }
    .diario p { margin: 0.2rem 0 0; }
    .dica { font-size: 0.8rem; margin: 0.5rem 0 0; }
  `,
})
export class OrdemDetalheComponent implements OnInit {
  readonly ordens = inject(OrdensService);

  readonly id = input.required<string>();
  readonly os = signal<OrdemServico | null>(null);
  readonly erro = signal('');
  readonly enviando = signal(false);
  readonly mensagem = signal<{ tipo: 'ok' | 'erro'; texto: string } | null>(null);
  readonly linhas = signal<LinhaMedicao[]>([]);
  observacao = '';

  readonly fases = FASES_OS;
  readonly rotuloAcao: Record<string, string> = {
    criacao: 'OS emitida',
    avanco: 'Avanço',
    devolucao: 'Devolvida',
    cancelamento: 'Cancelada',
  };

  readonly indiceAtual = computed(() => {
    const fase = this.os()?.fase;
    return fase === 'encerrada' ? this.fases.length : this.fases.findIndex((f) => f.valor === fase);
  });

  readonly eventosRecentes = computed(() => [...(this.os()?.eventos ?? [])].reverse());

  ngOnInit(): void {
    this.ordens.obter(Number(this.id())).subscribe({
      next: (os) => this.atualizar(os),
      error: () => this.erro.set('Ordem de serviço não encontrada.'),
    });
  }

  salvarMedicoes(os: OrdemServico): void {
    const medicoes = this.linhas().map((l) => ({
      indicador_id: l.indicador_id,
      realizado: Number(l.realizado),
      previsto: Number(l.previsto),
    }));
    this.executar(this.ordens.registrarMedicoes(os.id, medicoes), 'Medições registradas e glosa calculada.');
  }

  executar(acao: Observable<OrdemServico>, sucesso?: string): void {
    this.enviando.set(true);
    this.mensagem.set(null);
    acao.subscribe({
      next: (os) => {
        this.atualizar(os);
        this.observacao = '';
        this.mensagem.set({ tipo: 'ok', texto: sucesso ?? `OS agora em: ${os.fase_rotulo}.` });
        this.enviando.set(false);
      },
      error: (e) => {
        this.mensagem.set({ tipo: 'erro', texto: interpretarErro(e).mensagem });
        this.enviando.set(false);
      },
    });
  }

  private atualizar(os: OrdemServico): void {
    this.os.set(os);
    this.linhas.set(
      (os.contrato.indicadores ?? []).map((i) => {
        const m = os.medicoes?.find((x) => x.indicador_id === i.id);
        return { indicador_id: i.id, sigla: i.sigla, nome: i.nome, meta: i.meta, realizado: m?.realizado ?? null, previsto: m?.previsto ?? null };
      }),
    );
  }
}

import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ContratosService } from '../../core/contratos.service';
import { interpretarErro } from '../../core/erros';
import { AditivoPayload, Contrato, TipoAditivo } from '../../core/models';
import { CnpjPipe } from '../../shared/cnpj.pipe';
import { PrazoPipe } from '../../shared/prazo.pipe';
import { SituacaoBadgeComponent } from '../../shared/situacao-badge.component';
import { ContratoCadastrosComponent } from './contrato-cadastros.component';

@Component({
  selector: 'app-contrato-detalhe',
  imports: [RouterLink, ReactiveFormsModule, CurrencyPipe, DatePipe, CnpjPipe, PrazoPipe, SituacaoBadgeComponent, ContratoCadastrosComponent],
  template: `
    @if (contrato(); as c) {
      <div class="cabecalho">
        <h1>Contrato {{ c.numero }} <app-situacao-badge [situacao]="c.situacao" [dias]="c.dias_para_vencer" /></h1>
        <div class="acoes-topo">
          @if (c.pode_editar) {
            <a class="botao botao--secundario" [routerLink]="['/contratos', c.id, 'editar']">Editar</a>
          }
          @if (c.pode_excluir) {
            <button class="botao botao--perigo" type="button" (click)="excluir(c)">Excluir</button>
          }
        </div>
      </div>

      <section class="cartao dados">
        <div class="largo"><span class="suave">Objeto</span><p>{{ c.objeto }}</p></div>
        <div><span class="suave">Contratada</span><p>{{ c.contratada }}<br /><small>{{ c.cnpj | cnpj }}</small></p></div>
        <div><span class="suave">Valor global</span><p>{{ c.valor_global | currency: 'BRL' }}</p></div>
        <div><span class="suave">Vigência</span><p>{{ c.data_inicio | date: 'dd/MM/yyyy' }} a {{ c.data_fim | date: 'dd/MM/yyyy' }}<br /><small>{{ c.dias_para_vencer | prazo }}</small></p></div>
        <div><span class="suave">Gestor / fiscal</span><p>{{ c.gestor?.nome }}<br /><small>{{ nomeFiscal(c) }}</small></p></div>
      </section>

      <h2>Execução e fiscalização</h2>
      <app-contrato-cadastros [contrato]="c" (alterado)="recarregar()" />
      <p><a [routerLink]="['/ordens-servico']" [queryParams]="{ contrato_id: c.id }">Ver ordens de serviço deste contrato</a></p>

      <h2>Termos aditivos</h2>
      @if (c.aditivos?.length) {
        <table class="tabela cartao">
          <thead><tr><th>Nº</th><th>Tipo</th><th>Assinatura</th><th>Efeito</th><th>Descrição</th></tr></thead>
          <tbody>
            @for (a of c.aditivos; track a.id) {
              <tr>
                <td>{{ a.numero }}º</td>
                <td>{{ rotuloTipo[a.tipo] }}</td>
                <td>{{ a.data_assinatura | date: 'dd/MM/yyyy' }}</td>
                <td>
                  @if (a.nova_data_fim) { Vigência: {{ a.data_fim_anterior | date: 'dd/MM/yyyy' }} → {{ a.nova_data_fim | date: 'dd/MM/yyyy' }}<br /> }
                  @if (a.valor_acrescimo !== null) { Valor: {{ a.valor_acrescimo | currency: 'BRL' }} }
                </td>
                <td>{{ a.descricao }}</td>
              </tr>
            }
          </tbody>
        </table>
      } @else {
        <div class="cartao vazio">Nenhum aditivo registrado.</div>
      }

      @if (c.pode_editar) {
        <h2>Registrar aditivo</h2>
        @if (mensagem(); as m) {
          <div class="alerta" [class.alerta--erro]="m.tipo === 'erro'" [class.alerta--sucesso]="m.tipo === 'ok'">{{ m.texto }}</div>
        }
        <form class="cartao formulario" [formGroup]="aditivo" (ngSubmit)="registrarAditivo(c)">
          <div class="campo">
            <label for="tipo">Tipo</label>
            <select id="tipo" formControlName="tipo">
              <option value="prazo">Prorrogação de prazo</option>
              <option value="valor">Acréscimo ou supressão de valor</option>
              <option value="prazo_valor">Prazo e valor</option>
            </select>
          </div>
          <div class="campo" [class.invalido]="errosAditivo()['data_assinatura']">
            <label for="assinatura">Data de assinatura</label>
            <input id="assinatura" type="date" formControlName="data_assinatura" />
            @if (errosAditivo()['data_assinatura']; as e) { <span class="erro">{{ e }}</span> }
          </div>
          @if (alteraPrazo()) {
            <div class="campo" [class.invalido]="errosAditivo()['nova_data_fim']">
              <label for="novaFim">Nova data de fim</label>
              <input id="novaFim" type="date" formControlName="nova_data_fim" />
              @if (errosAditivo()['nova_data_fim']; as e) { <span class="erro">{{ e }}</span> }
            </div>
          }
          @if (alteraValor()) {
            <div class="campo" [class.invalido]="errosAditivo()['valor_acrescimo']">
              <label for="acrescimo">Valor (use negativo para supressão)</label>
              <input id="acrescimo" type="number" step="0.01" formControlName="valor_acrescimo" />
              @if (errosAditivo()['valor_acrescimo']; as e) { <span class="erro">{{ e }}</span> }
            </div>
          }
          <div class="campo largo">
            <label for="descricao">Descrição (opcional)</label>
            <input id="descricao" formControlName="descricao" />
          </div>
          <div class="acoes largo">
            <button class="botao" type="submit" [disabled]="aditivo.invalid || enviando()">Registrar aditivo</button>
          </div>
        </form>
      }
    } @else if (erro()) {
      <div class="alerta alerta--erro">{{ erro() }}</div>
    } @else {
      <p class="suave">Carregando...</p>
    }
  `,
  styles: `
    h1 { display: flex; align-items: center; gap: 0.75rem; }
    .acoes-topo { display: flex; gap: 0.75rem; }
    .dados { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; }
    .dados .largo { grid-column: 1 / -1; }
    .dados p { margin: 0.2rem 0 0; }
  `,
})
export class ContratoDetalheComponent implements OnInit {
  private readonly servico = inject(ContratosService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();
  readonly contrato = signal<Contrato | null>(null);
  readonly erro = signal('');
  readonly enviando = signal(false);
  readonly mensagem = signal<{ tipo: 'ok' | 'erro'; texto: string } | null>(null);
  readonly errosAditivo = signal<Record<string, string>>({});

  readonly rotuloTipo: Record<TipoAditivo, string> = {
    prazo: 'Prazo',
    valor: 'Valor',
    prazo_valor: 'Prazo e valor',
  };

  readonly aditivo = inject(FormBuilder).group({
    tipo: ['prazo' as TipoAditivo, Validators.required],
    data_assinatura: ['', Validators.required],
    nova_data_fim: [''],
    valor_acrescimo: [null as number | null],
    descricao: [''],
  });

  ngOnInit(): void {
    this.carregar();
  }

  alteraPrazo(): boolean {
    return this.aditivo.controls.tipo.value !== 'valor';
  }

  alteraValor(): boolean {
    return this.aditivo.controls.tipo.value !== 'prazo';
  }

  registrarAditivo(c: Contrato): void {
    this.enviando.set(true);
    this.mensagem.set(null);
    this.errosAditivo.set({});

    const v = this.aditivo.getRawValue();
    const dados: AditivoPayload = {
      tipo: v.tipo ?? 'prazo',
      data_assinatura: v.data_assinatura ?? '',
      nova_data_fim: this.alteraPrazo() ? v.nova_data_fim : null,
      valor_acrescimo: this.alteraValor() ? v.valor_acrescimo : null,
      descricao: v.descricao || null,
    };

    this.servico.registrarAditivo(c.id, dados).subscribe({
      next: (a) => {
        this.mensagem.set({ tipo: 'ok', texto: `${a.numero}º termo aditivo registrado.` });
        this.aditivo.reset({ tipo: 'prazo', data_assinatura: '', nova_data_fim: '', valor_acrescimo: null, descricao: '' });
        this.enviando.set(false);
        this.carregar();
      },
      error: (e) => {
        const { mensagem, campos } = interpretarErro(e);
        this.mensagem.set({ tipo: 'erro', texto: mensagem });
        this.errosAditivo.set(campos);
        this.enviando.set(false);
      },
    });
  }

  excluir(c: Contrato): void {
    if (!confirm(`Excluir o contrato ${c.numero}? Esta ação não pode ser desfeita pela tela.`)) {
      return;
    }
    this.servico.excluir(c.id).subscribe(() => this.router.navigate(['/contratos']));
  }

  /** Fiscal principal do contrato ou, se não houver, o fiscal técnico designado. */
  nomeFiscal(c: Contrato): string {
    const tecnico = c.fiscais?.find((f) => f.tipo === 'tecnico');
    return c.fiscal?.nome ?? tecnico?.nome ?? 'Sem fiscal designado';
  }

  recarregar(): void {
    this.carregar();
  }

  private carregar(): void {
    this.servico.obter(Number(this.id())).subscribe({
      next: (c) => this.contrato.set(c),
      error: () => this.erro.set('Contrato não encontrado.'),
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ContratosService } from '../../core/contratos.service';
import { interpretarErro } from '../../core/erros';
import { Contrato, EmpenhoContrato } from '../../core/models';
import { NovaOS, OrdensService } from '../../core/ordens.service';

@Component({
  selector: 'app-ordem-form',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Nova ordem de serviço</h1>

    @if (erroGeral()) {
      <div class="alerta alerta--erro" role="alert">{{ erroGeral() }}</div>
    }

    <form class="cartao formulario" [formGroup]="form" (ngSubmit)="salvar()">
      <div class="campo" [class.invalido]="erros()['contrato_id']">
        <label for="contrato">Contrato</label>
        <select id="contrato" formControlName="contrato_id" (change)="carregarEmpenhos()">
          <option [ngValue]="null" disabled>Selecione</option>
          @for (c of contratos(); track c.id) { <option [ngValue]="c.id">{{ c.numero }} - {{ c.contratada }}</option> }
        </select>
        @if (erros()['contrato_id']; as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo">
        <label for="tipo">Tipo</label>
        <select id="tipo" formControlName="tipo">
          <option value="projeto">Projeto</option>
          <option value="sustentacao">Sustentação</option>
          <option value="dados">Dados</option>
        </select>
      </div>
      <div class="campo largo" [class.invalido]="erros()['descricao']">
        <label for="descricao">Descrição</label>
        <input id="descricao" formControlName="descricao" placeholder="Sprint 7 - módulo de relatórios" />
        @if (erros()['descricao']; as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erros()['valor_estimado']">
        <label for="valor">Valor estimado (R$)</label>
        <input id="valor" type="number" step="0.01" formControlName="valor_estimado" />
        @if (erros()['valor_estimado']; as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erros()['prazo_entrega']">
        <label for="prazo">Prazo de entrega</label>
        <input id="prazo" type="date" formControlName="prazo_entrega" />
        @if (erros()['prazo_entrega']; as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erros()['empenho_id']">
        <label for="empenho">Nota de empenho</label>
        <select id="empenho" formControlName="empenho_id">
          <option [ngValue]="null">Vincular depois</option>
          @for (e of empenhos(); track e.id) { <option [ngValue]="e.id">{{ e.numero }} (saldo R$ {{ e.saldo.toLocaleString('pt-BR') }})</option> }
        </select>
        @if (erros()['empenho_id']; as e) { <span class="erro">{{ e }}</span> }
      </div>

      <div class="acoes largo">
        <button class="botao" type="submit" [disabled]="form.invalid || salvando()">Emitir OS</button>
        <a class="botao botao--secundario" routerLink="/ordens-servico">Cancelar</a>
      </div>
    </form>
  `,
})
export class OrdemFormComponent implements OnInit {
  private readonly contratosServico = inject(ContratosService);
  private readonly ordens = inject(OrdensService);
  private readonly router = inject(Router);

  readonly contratos = signal<Contrato[]>([]);
  readonly empenhos = signal<EmpenhoContrato[]>([]);
  readonly salvando = signal(false);
  readonly erroGeral = signal('');
  readonly erros = signal<Record<string, string>>({});

  readonly form = inject(FormBuilder).group({
    contrato_id: [null as number | null, Validators.required],
    tipo: ['projeto', Validators.required],
    descricao: ['', [Validators.required, Validators.maxLength(500)]],
    valor_estimado: [null as number | null, [Validators.required, Validators.min(0.01)]],
    prazo_entrega: ['', Validators.required],
    empenho_id: [null as number | null],
  });

  ngOnInit(): void {
    this.contratosServico.listar({ situacao: '', por_pagina: 100 }).subscribe((r) =>
      this.contratos.set(r.data.filter((c) => c.pode_editar && c.situacao !== 'vencido')),
    );
  }

  carregarEmpenhos(): void {
    const id = this.form.controls.contrato_id.value;
    this.form.controls.empenho_id.setValue(null);
    this.empenhos.set([]);
    if (id) {
      this.contratosServico.obter(id).subscribe((c) => this.empenhos.set(c.empenhos ?? []));
    }
  }

  salvar(): void {
    this.salvando.set(true);
    this.erroGeral.set('');
    this.erros.set({});
    this.ordens.criar(this.form.getRawValue() as NovaOS).subscribe({
      next: (os) => this.router.navigate(['/ordens-servico', os.id]),
      error: (e) => {
        const { mensagem, campos } = interpretarErro(e);
        this.erroGeral.set(mensagem);
        this.erros.set(campos);
        this.salvando.set(false);
      },
    });
  }
}

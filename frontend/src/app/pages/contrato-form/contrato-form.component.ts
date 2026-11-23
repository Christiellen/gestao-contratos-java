import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ContratosService } from '../../core/contratos.service';
import { interpretarErro } from '../../core/erros';
import { ContratoPayload, Usuario } from '../../core/models';
import { cnpjValidator } from '../../shared/cnpj.validator';

@Component({
  selector: 'app-contrato-form',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>{{ id() ? 'Editar contrato' : 'Novo contrato' }}</h1>

    @if (erroGeral()) {
      <div class="alerta alerta--erro" role="alert">{{ erroGeral() }}</div>
    }

    <form class="cartao formulario" [formGroup]="form" (ngSubmit)="salvar()">
      <div class="campo" [class.invalido]="erro('numero')">
        <label for="numero">Número do contrato</label>
        <input id="numero" formControlName="numero" placeholder="12/2026" />
        @if (erro('numero'); as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erro('valor_global')">
        <label for="valor">Valor global (R$)</label>
        <input id="valor" type="number" step="0.01" min="0.01" formControlName="valor_global" />
        @if (erro('valor_global'); as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo largo" [class.invalido]="erro('objeto')">
        <label for="objeto">Objeto</label>
        <textarea id="objeto" rows="2" formControlName="objeto"></textarea>
        @if (erro('objeto'); as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erro('contratada')">
        <label for="contratada">Empresa contratada</label>
        <input id="contratada" formControlName="contratada" />
        @if (erro('contratada'); as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erro('cnpj')">
        <label for="cnpj">CNPJ</label>
        <input id="cnpj" formControlName="cnpj" placeholder="00.000.000/0000-00" />
        @if (erro('cnpj'); as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erro('data_inicio')">
        <label for="inicio">Início da vigência</label>
        <input id="inicio" type="date" formControlName="data_inicio" />
        @if (erro('data_inicio'); as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erro('data_fim')">
        <label for="fim">Fim da vigência</label>
        <input id="fim" type="date" formControlName="data_fim" />
        @if (erro('data_fim'); as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erro('gestor_id')">
        <label for="gestor">Gestor</label>
        <select id="gestor" formControlName="gestor_id">
          <option [ngValue]="null" disabled>Selecione</option>
          @for (u of pessoas(); track u.id) { <option [ngValue]="u.id">{{ u.nome }}</option> }
        </select>
        @if (erro('gestor_id'); as e) { <span class="erro">{{ e }}</span> }
      </div>
      <div class="campo" [class.invalido]="erro('fiscal_id')">
        <label for="fiscal">Fiscal (opcional)</label>
        <select id="fiscal" formControlName="fiscal_id">
          <option [ngValue]="null">Sem fiscal</option>
          @for (u of pessoas(); track u.id) { <option [ngValue]="u.id">{{ u.nome }}</option> }
        </select>
        @if (erro('fiscal_id'); as e) { <span class="erro">{{ e }}</span> }
      </div>

      <div class="acoes largo">
        <button class="botao" type="submit" [disabled]="salvando()">{{ salvando() ? 'Salvando...' : 'Salvar' }}</button>
        <a class="botao botao--secundario" [routerLink]="id() ? ['/contratos', id()] : ['/contratos']">Cancelar</a>
      </div>
    </form>
  `,
})
export class ContratoFormComponent implements OnInit {
  private readonly servico = inject(ContratosService);
  private readonly router = inject(Router);

  /** Preenchido pela rota contratos/:id/editar. */
  readonly id = input<string>();

  readonly pessoas = signal<Usuario[]>([]);
  readonly salvando = signal(false);
  readonly erroGeral = signal('');
  private readonly errosServidor = signal<Record<string, string>>({});

  readonly form = inject(FormBuilder).group({
    numero: ['', [Validators.required, Validators.maxLength(30)]],
    objeto: ['', [Validators.required, Validators.maxLength(500)]],
    contratada: ['', [Validators.required, Validators.maxLength(200)]],
    cnpj: ['', [Validators.required, cnpjValidator]],
    valor_global: [null as number | null, [Validators.required, Validators.min(0.01)]],
    data_inicio: ['', Validators.required],
    data_fim: ['', Validators.required],
    gestor_id: [null as number | null, Validators.required],
    fiscal_id: [null as number | null],
  });

  ngOnInit(): void {
    this.servico.gestores().subscribe((p) => this.pessoas.set(p));

    const id = this.id();
    if (id) {
      this.servico.obter(Number(id)).subscribe((c) =>
        this.form.patchValue({
          ...c,
          gestor_id: c.gestor?.id ?? null,
          fiscal_id: c.fiscal?.id ?? null,
        }),
      );
    }
  }

  /** Erro do servidor (422) tem prioridade; senão, erro local do campo já tocado. */
  erro(campo: string): string | null {
    const servidor = this.errosServidor()[campo];
    if (servidor) {
      return servidor;
    }
    const controle = this.form.get(campo);
    if (!controle || !controle.invalid || !(controle.touched || controle.dirty)) {
      return null;
    }
    if (controle.hasError('cnpj')) {
      return 'CNPJ inválido.';
    }
    return 'Preencha este campo corretamente.';
  }

  salvar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.erroGeral.set('Revise os campos destacados.');
      return;
    }

    this.salvando.set(true);
    this.erroGeral.set('');
    this.errosServidor.set({});

    const dados = this.form.getRawValue() as ContratoPayload;
    const id = this.id();
    const requisicao = id ? this.servico.atualizar(Number(id), dados) : this.servico.criar(dados);

    requisicao.subscribe({
      next: (c) => this.router.navigate(['/contratos', c.id]),
      error: (e) => {
        const { mensagem, campos } = interpretarErro(e);
        this.erroGeral.set(mensagem);
        this.errosServidor.set(campos);
        this.salvando.set(false);
      },
    });
  }
}

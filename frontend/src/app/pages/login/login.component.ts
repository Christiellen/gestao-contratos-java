import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { interpretarErro } from '../../core/erros';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  template: `
    <div class="login">
      <form class="cartao" [formGroup]="form" (ngSubmit)="entrar()">
        <h1>Gestão de Contratos</h1>
        <p class="suave">Acompanhe vigências e receba avisos de vencimento.</p>

        @if (erro()) {
          <div class="alerta alerta--erro" role="alert">{{ erro() }}</div>
        }

        <div class="campo">
          <label for="email">E-mail</label>
          <input id="email" type="email" formControlName="email" autocomplete="username" />
        </div>
        <div class="campo">
          <label for="senha">Senha</label>
          <input id="senha" type="password" formControlName="password" autocomplete="current-password" />
        </div>

        <button class="botao" type="submit" [disabled]="form.invalid || enviando()">
          {{ enviando() ? 'Entrando...' : 'Entrar' }}
        </button>
        <p class="suave dica">Demonstração: admin&#64;exemplo.gov.br / password</p>
      </form>
    </div>
  `,
  styles: `
    .login { display: flex; justify-content: center; padding-top: 8vh; }
    form { width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 1rem; }
    .dica { font-size: 0.8rem; margin: 0; }
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  readonly enviando = signal(false);
  readonly erro = signal('');

  entrar(): void {
    if (this.form.invalid) {
      return;
    }
    this.enviando.set(true);
    this.erro.set('');
    const { email, password } = this.form.getRawValue();

    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/painel']),
      error: (e) => {
        const { mensagem, campos } = interpretarErro(e);
        this.erro.set(campos['email'] ?? mensagem);
        this.enviando.set(false);
      },
    });
  }
}

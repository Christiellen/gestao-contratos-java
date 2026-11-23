import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (auth.autenticado()) {
      <header class="topo">
        <div class="topo__marca">Gestão de Contratos</div>
        <nav class="topo__nav" aria-label="Principal">
          <a routerLink="/painel" routerLinkActive="ativo">Painel</a>
          <a routerLink="/contratos" routerLinkActive="ativo">Contratos</a>
          <a routerLink="/ordens-servico" routerLinkActive="ativo">Ordens de serviço</a>
          <a routerLink="/relatorios" routerLinkActive="ativo">Relatórios</a>
        </nav>
        <div class="topo__usuario">
          <span>{{ auth.usuario()?.nome }} - {{ auth.usuario()?.perfil_rotulo }}</span>
          <button type="button" class="botao botao--claro" (click)="auth.logout()">Sair</button>
        </div>
      </header>
    }
    <main class="conteudo">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {
  readonly auth = inject(AuthService);
}

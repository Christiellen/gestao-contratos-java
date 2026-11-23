import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { Usuario } from './models';

const CHAVE_TOKEN = 'gc_token';
const CHAVE_USUARIO = 'gc_usuario';

interface RespostaLogin {
  token: string;
  usuario: Usuario;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _usuario = signal<Usuario | null>(this.lerUsuario());

  readonly usuario = this._usuario.asReadonly();
  readonly autenticado = computed(() => this._usuario() !== null && this.token !== null);
  readonly podeCadastrar = computed(() => {
    const perfil = this._usuario()?.perfil;
    return perfil === 'administrador' || perfil === 'gestor';
  });

  get token(): string | null {
    return localStorage.getItem(CHAVE_TOKEN);
  }

  login(email: string, password: string): Observable<RespostaLogin> {
    return this.http.post<RespostaLogin>('/api/login', { email, password }).pipe(
      tap(({ token, usuario }) => {
        localStorage.setItem(CHAVE_TOKEN, token);
        localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
        this._usuario.set(usuario);
      }),
    );
  }

  logout(): void {
    if (this.token) {
      this.http.post('/api/logout', {}).subscribe({ error: () => undefined });
    }
    this.limparSessao();
  }

  /** Limpa a sessão local (usado também quando a API responde 401). */
  limparSessao(): void {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
    this._usuario.set(null);
    this.router.navigate(['/login']);
  }

  private lerUsuario(): Usuario | null {
    try {
      const bruto = localStorage.getItem(CHAVE_USUARIO);
      return bruto ? (JSON.parse(bruto) as Usuario) : null;
    } catch {
      return null;
    }
  }
}

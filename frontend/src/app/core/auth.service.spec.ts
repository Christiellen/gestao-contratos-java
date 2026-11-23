import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('AuthService e authInterceptor', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let router: Router;

  const usuario = { id: 1, nome: 'Ana', email: 'ana@exemplo.gov.br', perfil: 'gestor' as const, perfil_rotulo: 'Gestor de contratos' };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('guarda o token e o usuário após o login', () => {
    auth.login('ana@exemplo.gov.br', 'password').subscribe();
    http.expectOne('/api/login').flush({ token: 'abc', usuario });

    expect(auth.token).toBe('abc');
    expect(auth.usuario()?.nome).toBe('Ana');
    expect(auth.autenticado()).toBeTrue();
    expect(auth.podeCadastrar()).toBeTrue();
  });

  it('envia o token Bearer nas requisições', () => {
    localStorage.setItem('gc_token', 'abc');
    TestBed.inject(HttpClient).get('/api/contratos').subscribe();

    const req = http.expectOne('/api/contratos');
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc');
    req.flush({ data: [] });
  });

  it('encerra a sessão quando a API responde 401', () => {
    localStorage.setItem('gc_token', 'expirado');
    TestBed.inject(HttpClient).get('/api/painel').subscribe({ error: () => undefined });
    http.expectOne('/api/painel').flush({ message: 'Não autenticado.' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.token).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});

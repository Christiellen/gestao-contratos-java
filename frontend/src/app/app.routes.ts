import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar - Gestão de Contratos',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'painel' },
      {
        path: 'painel',
        title: 'Painel - Gestão de Contratos',
        loadComponent: () => import('./pages/painel/painel.component').then((m) => m.PainelComponent),
      },
      {
        path: 'contratos',
        title: 'Contratos - Gestão de Contratos',
        loadComponent: () =>
          import('./pages/contratos-lista/contratos-lista.component').then((m) => m.ContratosListaComponent),
      },
      {
        path: 'contratos/novo',
        title: 'Novo contrato - Gestão de Contratos',
        loadComponent: () => import('./pages/contrato-form/contrato-form.component').then((m) => m.ContratoFormComponent),
      },
      {
        path: 'contratos/:id',
        title: 'Contrato - Gestão de Contratos',
        loadComponent: () =>
          import('./pages/contrato-detalhe/contrato-detalhe.component').then((m) => m.ContratoDetalheComponent),
      },
      {
        path: 'ordens-servico',
        title: 'Ordens de serviço - Gestão de Contratos',
        loadComponent: () => import('./pages/ordens-lista/ordens-lista.component').then((m) => m.OrdensListaComponent),
      },
      {
        path: 'ordens-servico/nova',
        title: 'Nova OS - Gestão de Contratos',
        loadComponent: () => import('./pages/ordem-form/ordem-form.component').then((m) => m.OrdemFormComponent),
      },
      {
        path: 'ordens-servico/:id',
        title: 'Ordem de serviço - Gestão de Contratos',
        loadComponent: () => import('./pages/ordem-detalhe/ordem-detalhe.component').then((m) => m.OrdemDetalheComponent),
      },
      {
        path: 'relatorios',
        title: 'Relatórios - Gestão de Contratos',
        loadComponent: () => import('./pages/relatorios/relatorios.component').then((m) => m.RelatoriosComponent),
      },
      {
        path: 'contratos/:id/editar',
        title: 'Editar contrato - Gestão de Contratos',
        loadComponent: () => import('./pages/contrato-form/contrato-form.component').then((m) => m.ContratoFormComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

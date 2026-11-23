import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  Aditivo,
  AditivoPayload,
  Contrato,
  ContratoPayload,
  FiltrosContrato,
  Paginado,
  Painel,
  Usuario,
} from './models';

@Injectable({ providedIn: 'root' })
export class ContratosService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/contratos';

  listar(filtros: FiltrosContrato = {}): Observable<Paginado<Contrato>> {
    let params = new HttpParams();
    for (const [chave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(chave, String(valor));
      }
    }
    return this.http.get<Paginado<Contrato>>(this.base, { params });
  }

  obter(id: number): Observable<Contrato> {
    return this.http.get<{ data: Contrato }>(`${this.base}/${id}`).pipe(map((r) => r.data));
  }

  criar(dados: ContratoPayload): Observable<Contrato> {
    return this.http.post<{ data: Contrato }>(this.base, dados).pipe(map((r) => r.data));
  }

  atualizar(id: number, dados: ContratoPayload): Observable<Contrato> {
    return this.http.put<{ data: Contrato }>(`${this.base}/${id}`, dados).pipe(map((r) => r.data));
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  registrarAditivo(contratoId: number, dados: AditivoPayload): Observable<Aditivo> {
    return this.http
      .post<{ data: Aditivo }>(`${this.base}/${contratoId}/aditivos`, dados)
      .pipe(map((r) => r.data));
  }

  /** Cadastros ligados ao contrato: empenhos, indicadores, fiscais e prepostos. */
  cadastrar(contratoId: number, recurso: 'empenhos' | 'indicadores' | 'fiscais' | 'prepostos', dados: object): Observable<unknown> {
    return this.http.post(`${this.base}/${contratoId}/${recurso}`, dados);
  }

  painel(): Observable<Painel> {
    return this.http.get<Painel>('/api/painel');
  }

  gestores(): Observable<Usuario[]> {
    return this.http.get<{ data: Usuario[] }>('/api/usuarios').pipe(map((r) => r.data));
  }
}

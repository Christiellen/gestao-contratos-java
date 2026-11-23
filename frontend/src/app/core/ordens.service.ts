import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { FaseOS, OrdemServico, Paginado, TempoMedio, ValoresPendentes } from './models';

export interface NovaOS {
  contrato_id: number;
  tipo: string;
  descricao: string;
  valor_estimado: number;
  prazo_entrega: string;
  empenho_id: number | null;
}

@Injectable({ providedIn: 'root' })
export class OrdensService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/ordens-servico';

  listar(filtros: { fase?: FaseOS | ''; contrato_id?: number | null; busca?: string; page?: number } = {}): Observable<Paginado<OrdemServico>> {
    let params = new HttpParams();
    for (const [chave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(chave, String(valor));
      }
    }
    return this.http.get<Paginado<OrdemServico>>(this.base, { params });
  }

  obter(id: number): Observable<OrdemServico> {
    return this.http.get<{ data: OrdemServico }>(`${this.base}/${id}`).pipe(map((r) => r.data));
  }

  criar(dados: NovaOS): Observable<OrdemServico> {
    return this.http.post<{ data: OrdemServico }>(this.base, dados).pipe(map((r) => r.data));
  }

  registrarMedicoes(id: number, medicoes: { indicador_id: number; realizado: number; previsto: number }[]): Observable<OrdemServico> {
    return this.http.put<{ data: OrdemServico }>(`${this.base}/${id}/medicoes`, { medicoes }).pipe(map((r) => r.data));
  }

  avancar(id: number, observacao: string | null): Observable<OrdemServico> {
    return this.http.post<{ data: OrdemServico }>(`${this.base}/${id}/avancar`, { observacao }).pipe(map((r) => r.data));
  }

  devolver(id: number, motivo: string): Observable<OrdemServico> {
    return this.http.post<{ data: OrdemServico }>(`${this.base}/${id}/devolver`, { motivo }).pipe(map((r) => r.data));
  }

  cancelar(id: number, motivo: string): Observable<OrdemServico> {
    return this.http.post<{ data: OrdemServico }>(`${this.base}/${id}/cancelar`, { motivo }).pipe(map((r) => r.data));
  }

  tempoMedio(inicio: string, fim: string): Observable<TempoMedio> {
    return this.http.get<TempoMedio>('/api/relatorios/tempo-medio', { params: { inicio, fim } });
  }

  valoresPendentes(): Observable<ValoresPendentes> {
    return this.http.get<ValoresPendentes>('/api/relatorios/valores-pendentes');
  }
}

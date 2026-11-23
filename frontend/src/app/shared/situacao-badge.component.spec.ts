import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SituacaoBadgeComponent } from './situacao-badge.component';

describe('SituacaoBadgeComponent', () => {
  let fixture: ComponentFixture<SituacaoBadgeComponent>;

  function renderizar(situacao: string, dias: number | null): HTMLElement {
    fixture = TestBed.createComponent(SituacaoBadgeComponent);
    fixture.componentRef.setInput('situacao', situacao);
    fixture.componentRef.setInput('dias', dias);
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('.badge');
  }

  it('mostra "Urgente" para contratos com 30 dias ou menos', () => {
    const badge = renderizar('a_vencer', 12);
    expect(badge.textContent?.trim()).toBe('Urgente');
    expect(badge.classList).toContain('badge--a_vencer');
  });

  it('mostra o rótulo da situação nos demais casos', () => {
    expect(renderizar('a_vencer', 70).textContent?.trim()).toBe('A vencer');
    expect(renderizar('vencido', -3).textContent?.trim()).toBe('Vencido');
    expect(renderizar('vigente', 400).textContent?.trim()).toBe('Vigente');
  });
});

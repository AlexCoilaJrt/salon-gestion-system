import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReporteVentasDetalladoComponent } from './reporte-ventas-detallado.component';

describe('ReporteVentasDetalladoComponent', () => {
  let component: ReporteVentasDetalladoComponent;
  let fixture: ComponentFixture<ReporteVentasDetalladoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReporteVentasDetalladoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReporteVentasDetalladoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

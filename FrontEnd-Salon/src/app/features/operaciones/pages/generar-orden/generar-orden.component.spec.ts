import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GenerarOrdenComponent } from './generar-orden.component';

describe('GenerarOrdenComponent', () => {
  let component: GenerarOrdenComponent;
  let fixture: ComponentFixture<GenerarOrdenComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GenerarOrdenComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GenerarOrdenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

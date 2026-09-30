import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestorCartillasComponent } from './gestor-cartillas.component';

describe('GestorCartillasComponent', () => {
  let component: GestorCartillasComponent;
  let fixture: ComponentFixture<GestorCartillasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestorCartillasComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GestorCartillasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { TestBed } from '@angular/core/testing';

import { FidelizacionService } from './fidelizacion.service';

describe('FidelizacionService', () => {
  let service: FidelizacionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FidelizacionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

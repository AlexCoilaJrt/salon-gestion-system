import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { CheckboxModule } from 'primeng/checkbox';
import { TagModule } from 'primeng/tag';

import { IncentivoService, Incentivo } from '../services/incentivo.service';

@Component({
  selector: 'app-incentivos',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, ToastModule, TableModule, 
    ButtonModule, DialogModule, DropdownModule, InputNumberModule, InputTextModule, CalendarModule, CheckboxModule, TagModule
  ],
  providers: [MessageService],
  templateUrl: './incentivos.component.html',
  styleUrls: ['./incentivos.component.scss']
})
export class IncentivosComponent implements OnInit {
  incentivos: Incentivo[] = [];
  tiposIncentivo = [
    { label: 'Porcentaje (%)', value: 'PORCENTAJE' },
    { label: 'Monto Fijo ($)', value: 'MONTO_FIJO' }
  ];
  
  displayDialog = false;
  isEditMode = false;
  loading = false;
  incentivoForm: FormGroup;
  currentIncentivoId?: number;

  constructor(
    private fb: FormBuilder,
    private incentivoService: IncentivoService,
    private messageService: MessageService
  ) {
    this.incentivoForm = this.fb.group({
      nombre: ['', Validators.required],
      tipoIncentivo: ['PORCENTAJE', Validators.required],
      valor: [0, [Validators.required, Validators.min(0.1)]],
      fechaInicio: [null, Validators.required],
      fechaFin: [null, Validators.required],
      estado: [true]
    });
  }

  ngOnInit(): void {
    this.loadIncentivos();
  }

  loadIncentivos(): void {
    this.loading = true;
    this.incentivoService.getIncentivos().subscribe({
      next: (data: Incentivo[]) => {
        this.incentivos = data;
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los incentivos' });
        this.loading = false;
      }
    });
  }

  openNew(): void {
    this.isEditMode = false;
    this.currentIncentivoId = undefined;
    this.incentivoForm.reset({
      tipoIncentivo: 'PORCENTAJE',
      valor: 0,
      estado: true
    });
    this.displayDialog = true;
  }

  openEdit(incentivo: Incentivo): void {
    this.isEditMode = true;
    this.currentIncentivoId = incentivo.id;
    this.incentivoForm.patchValue({
      nombre: incentivo.nombre,
      tipoIncentivo: incentivo.tipoIncentivo,
      valor: incentivo.valor,
      fechaInicio: new Date(incentivo.fechaInicio),
      fechaFin: new Date(incentivo.fechaFin),
      estado: incentivo.estado
    });
    this.displayDialog = true;
  }

  saveIncentivo(): void {
    if (this.incentivoForm.invalid) return;

    const data = { ...this.incentivoForm.value };
    // Formatear las fechas a ISO string para evitar problemas de parsing en java
    data.fechaInicio = data.fechaInicio.toISOString();
    data.fechaFin = data.fechaFin.toISOString();

    if (this.isEditMode && this.currentIncentivoId) {
      this.incentivoService.updateIncentivo(this.currentIncentivoId, data).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Incentivo actualizado correctamente' });
          this.loadIncentivos();
          this.displayDialog = false;
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo actualizar' })
      });
    } else {
      this.incentivoService.createIncentivo(data).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Incentivo creado correctamente' });
          this.loadIncentivos();
          this.displayDialog = false;
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo crear' })
      });
    }
  }

  deleteIncentivo(incentivo: Incentivo): void {
    if (confirm(`¿Estás seguro de inhabilitar el incentivo ${incentivo.nombre}?`)) {
      this.incentivoService.deleteIncentivo(incentivo.id!).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Incentivo eliminado' });
          this.loadIncentivos();
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar' })
      });
    }
  }
}

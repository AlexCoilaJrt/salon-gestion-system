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
import { CheckboxModule } from 'primeng/checkbox';
import { TagModule } from 'primeng/tag';

import { ComisionService, Comision } from '../services/comision.service';
import { RrhhService } from '../services/rrhh.service';

@Component({
  selector: 'app-comisiones',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, ToastModule, TableModule, 
    ButtonModule, DialogModule, DropdownModule, InputNumberModule, CheckboxModule, TagModule
  ],
  providers: [MessageService],
  templateUrl: './comisiones.component.html',
  styleUrls: ['./comisiones.component.scss']
})
export class ComisionesComponent implements OnInit {
  comisiones: Comision[] = [];
  empleados: any[] = [];
  tiposComision = [
    { label: 'Porcentaje (%)', value: 'PORCENTAJE' },
    { label: 'Monto Fijo ($)', value: 'MONTO_FIJO' }
  ];
  
  displayDialog = false;
  isEditMode = false;
  loading = false;
  comisionForm: FormGroup;
  currentComisionId?: number;

  constructor(
    private fb: FormBuilder,
    private comisionService: ComisionService,
    private rrhhService: RrhhService,
    private messageService: MessageService
  ) {
    this.comisionForm = this.fb.group({
      empleadoId: [null, Validators.required],
      tipoComision: ['PORCENTAJE', Validators.required],
      valor: [0, [Validators.required, Validators.min(0.1)]],
      estado: [true]
    });
  }

  ngOnInit(): void {
    this.loadComisiones();
    this.loadEmpleados();
  }

  loadComisiones(): void {
    this.loading = true;
    this.comisionService.getComisiones().subscribe({
      next: (data: Comision[]) => {
        this.comisiones = data;
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar las comisiones' });
        this.loading = false;
      }
    });
  }

  loadEmpleados(): void {
    this.rrhhService.getEmpleados().subscribe({
      next: (data: any[]) => {
        this.empleados = data.map((e: any) => ({
          id: e.id,
          nombreCompleto: `${e.nombres} ${e.apellidos}`
        }));
      }
    });
  }

  openNew(): void {
    this.isEditMode = false;
    this.currentComisionId = undefined;
    this.comisionForm.reset({
      tipoComision: 'PORCENTAJE',
      valor: 0,
      estado: true
    });
    this.displayDialog = true;
  }

  openEdit(comision: Comision): void {
    this.isEditMode = true;
    this.currentComisionId = comision.id;
    this.comisionForm.patchValue({
      empleadoId: comision.empleadoId,
      tipoComision: comision.tipoComision,
      valor: comision.valor,
      estado: comision.estado
    });
    this.displayDialog = true;
  }

  saveComision(): void {
    if (this.comisionForm.invalid) return;

    const data: Comision = this.comisionForm.value;

    if (this.isEditMode && this.currentComisionId) {
      this.comisionService.updateComision(this.currentComisionId, data).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Comisión actualizada correctamente' });
          this.loadComisiones();
          this.displayDialog = false;
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo actualizar' })
      });
    } else {
      this.comisionService.createComision(data).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Comisión creada correctamente' });
          this.loadComisiones();
          this.displayDialog = false;
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo crear' })
      });
    }
  }

  deleteComision(comision: Comision): void {
    if (confirm(`¿Estás seguro de inhabilitar la comisión de ${comision.empleadoNombre}?`)) {
      this.comisionService.deleteComision(comision.id!).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Comisión eliminada' });
          this.loadComisiones();
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar' })
      });
    }
  }
}

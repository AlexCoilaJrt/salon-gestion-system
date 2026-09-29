import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { MultiSelectModule } from 'primeng/multiselect';
import { CheckboxModule } from 'primeng/checkbox';
import { KeyFilterModule } from 'primeng/keyfilter';
import { MessageService } from 'primeng/api';
import { RrhhService, Empleado, Especialidad } from '../services/rrhh.service';
import { TurnoService, Turno } from '../services/turno.service';
import { DropdownModule } from 'primeng/dropdown';

@Component({
  selector: 'app-empleados',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    TableModule, ButtonModule, InputTextModule, TagModule,
    DialogModule, ToastModule, MultiSelectModule, CheckboxModule,
    KeyFilterModule, DropdownModule
  ],
  providers: [MessageService],
  templateUrl: './empleados.component.html'
})
export class EmpleadosComponent implements OnInit {
  private rrhhService = inject(RrhhService);
  private messageService = inject(MessageService);
  private fb = inject(FormBuilder);

  empleados: Empleado[] = [];
  especialidades: Especialidad[] = [];
  turnos: Turno[] = [];
  private turnoService = inject(TurnoService);
  loading = false;
  today = new Date().toISOString().split('T')[0];

  // Dialog State
  displayDialog = false;
  empleadoForm: FormGroup;
  isEditMode = false;
  selectedEmpleadoId: number | null = null;

  constructor() {
    this.empleadoForm = this.fb.group({
      nombres: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
      apellidos: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
      dni: ['', [Validators.required, Validators.pattern('^[0-9]{8,15}$')]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      telefono: ['', [Validators.required, Validators.pattern('^[0-9]{9,15}$')]],
      fechaNacimiento: ['', Validators.required],
      especialidadIds: [[], Validators.required],
      turnoId: [null, Validators.required],
      estado: [true]
    });
  }

  ngOnInit(): void {
    this.loadEspecialidades();
    this.loadTurnos();
    this.loadEmpleados();
  }

  loadEmpleados() {
    this.loading = true;
    this.rrhhService.getEmpleados().subscribe({
      next: (data) => {
        this.empleados = data;
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los empleados.' });
        this.loading = false;
      }
    });
  }

  loadEspecialidades() {
    this.rrhhService.getEspecialidades().subscribe({
      next: (data) => {
        this.especialidades = data.filter(e => e.estado); // Solo activas
      }
    });
  }

  loadTurnos() {
    this.turnoService.getActivos().subscribe({
      next: (data) => {
        this.turnos = data;
      }
    });
  }

  openNew() {
    this.isEditMode = false;
    this.selectedEmpleadoId = null;
    this.empleadoForm.reset({ estado: true, especialidadIds: [], turnoId: null });
    this.displayDialog = true;
  }

  openEdit(empleado: Empleado) {
    this.isEditMode = true;
    this.selectedEmpleadoId = empleado.id!;
    this.empleadoForm.patchValue({
      nombres: empleado.nombres,
      apellidos: empleado.apellidos,
      dni: empleado.dni,
      email: empleado.email,
      telefono: empleado.telefono,
      fechaNacimiento: empleado.fechaNacimiento,
      especialidadIds: empleado.especialidadIds || [],
      turnoId: empleado.turnoId || null,
      estado: empleado.estado
    });
    this.displayDialog = true;
  }

  saveEmpleado() {
    if (this.empleadoForm.invalid) {
      this.empleadoForm.markAllAsTouched();
      return;
    }

    const formValues = this.empleadoForm.getRawValue();
    const empleadoData: Empleado = { ...formValues };
    
    // Clean empty string for date so Jackson doesn't fail
    if (!empleadoData.fechaNacimiento) {
      empleadoData.fechaNacimiento = null as any;
    }
    // Clean empty phone
    if (!empleadoData.telefono) {
      empleadoData.telefono = null as any;
    }

    if (this.isEditMode && this.selectedEmpleadoId) {
      this.rrhhService.updateEmpleado(this.selectedEmpleadoId, empleadoData).subscribe({
        next: () => this.finishSave('Empleado actualizado correctamente.'),
        error: (err) => this.handleBackendError(err)
      });
    } else {
      this.rrhhService.createEmpleado(empleadoData).subscribe({
        next: () => this.finishSave('Empleado creado correctamente.'),
        error: (err) => this.handleBackendError(err)
      });
    }
  }

  deleteEmpleado(empleado: Empleado) {
    if (confirm(`¿Estás seguro de eliminar a ${empleado.nombres} ${empleado.apellidos}?`)) {
      this.rrhhService.deleteEmpleado(empleado.id!).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Empleado eliminado.' });
          this.loadEmpleados();
        },
        error: (err) => this.handleBackendError(err)
      });
    }
  }

  private finishSave(msg: string) {
    this.displayDialog = false;
    this.loadEmpleados();
    this.messageService.add({ severity: 'success', summary: 'Éxito', detail: msg });
  }

  private handleBackendError(err: any) {
    if (err.error?.errores) {
      const errorMap = err.error.errores;
      const firstKey = Object.keys(errorMap)[0];
      this.showError(`${firstKey.toUpperCase()}: ${errorMap[firstKey]}`);
    } else {
      this.showError(err.error?.message || 'Error en el servidor al guardar el empleado.');
    }
  }

  private showError(msg: string) {
    this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
  }
}

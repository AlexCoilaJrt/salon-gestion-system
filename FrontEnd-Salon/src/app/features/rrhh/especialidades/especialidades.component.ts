import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';
import { RrhhService, Especialidad } from '../services/rrhh.service';

@Component({
  selector: 'app-especialidades',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    TableModule, ButtonModule, InputTextModule, TextareaModule, TagModule,
    DialogModule, ToastModule, CheckboxModule
  ],
  providers: [MessageService],
  templateUrl: './especialidades.component.html'
})
export class EspecialidadesComponent implements OnInit {
  private rrhhService = inject(RrhhService);
  private messageService = inject(MessageService);
  private fb = inject(FormBuilder);

  especialidades: Especialidad[] = [];
  loading = false;

  // Dialog State
  displayDialog = false;
  especialidadForm: FormGroup;
  isEditMode = false;
  selectedId: number | null = null;

  constructor() {
    this.especialidadForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      descripcion: [''],
      estado: [true]
    });
  }

  ngOnInit(): void {
    this.loadEspecialidades();
  }

  loadEspecialidades() {
    this.loading = true;
    this.rrhhService.getEspecialidades().subscribe({
      next: (data) => {
        data.sort((a, b) => {
          if (a.estado === b.estado) return (a.id || 0) - (b.id || 0);
          return a.estado ? -1 : 1;
        });
        this.especialidades = data;
        this.loading = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar.' });
        this.loading = false;
      }
    });
  }

  openNew() {
    this.isEditMode = false;
    this.selectedId = null;
    this.especialidadForm.reset({ estado: true });
    this.displayDialog = true;
  }

  openEdit(esp: Especialidad) {
    this.isEditMode = true;
    this.selectedId = esp.id!;
    this.especialidadForm.patchValue(esp);
    this.displayDialog = true;
  }

  saveEspecialidad() {
    if (this.especialidadForm.invalid) {
      this.especialidadForm.markAllAsTouched();
      return;
    }

    const data: Partial<Especialidad> = this.especialidadForm.getRawValue();

    if (this.isEditMode && this.selectedId) {
      this.rrhhService.updateEspecialidad(this.selectedId, data).subscribe({
        next: () => this.finishSave('Actualizado correctamente.'),
        error: (err) => this.showError(err.error?.message || 'Error al actualizar.')
      });
    } else {
      this.rrhhService.createEspecialidad(data).subscribe({
        next: () => this.finishSave('Creado correctamente.'),
        error: (err) => this.showError(err.error?.message || 'Error al crear.')
      });
    }
  }

  deleteEspecialidad(esp: Especialidad) {
    if (confirm(`¿Eliminar la especialidad ${esp.nombre}?`)) {
      this.rrhhService.deleteEspecialidad(esp.id!).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Eliminado.' });
          this.loadEspecialidades();
        },
        error: (err) => this.showError(err.error?.message || 'No se pudo eliminar.')
      });
    }
  }

  private finishSave(msg: string) {
    this.displayDialog = false;
    this.loadEspecialidades();
    this.messageService.add({ severity: 'success', summary: 'Éxito', detail: msg });
  }

  private showError(msg: string) {
    this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
  }
}

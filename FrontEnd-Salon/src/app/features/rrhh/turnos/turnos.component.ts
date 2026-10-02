import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TurnoService, Turno } from '../services/turno.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TagModule } from 'primeng/tag';
import { CalendarModule } from 'primeng/calendar';

@Component({
  selector: 'app-turnos',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TableModule, ButtonModule, DialogModule, InputTextModule, ToastModule, TagModule, CalendarModule],
  providers: [MessageService],
  templateUrl: './turnos.component.html'
})
export class TurnosComponent implements OnInit {
  private turnoService = inject(TurnoService);
  private fb = inject(FormBuilder);
  private messageService = inject(MessageService);

  turnos: Turno[] = [];
  displayDialog: boolean = false;
  turnoForm: FormGroup;
  isEditMode: boolean = false;
  currentTurnoId: number | null = null;

  constructor() {
    this.turnoForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      horaEntrada: ['', Validators.required],
      horaSalida: ['', Validators.required],
      toleranciaMinutos: [0, [Validators.required, Validators.min(0)]]
    });
  }

  ngOnInit() {
    this.loadTurnos();
  }

  loadTurnos() {
    this.turnoService.getAll().subscribe({
      next: (data) => {
        data.sort((a, b) => {
          if (a.estado === b.estado) return (a.id || 0) - (b.id || 0);
          return a.estado ? -1 : 1;
        });
        this.turnos = data;
      },
      error: (err) => console.error(err)
    });
  }

  showDialogToAdd() {
    this.isEditMode = false;
    this.currentTurnoId = null;
    this.turnoForm.reset({ toleranciaMinutos: 0 });
    this.displayDialog = true;
  }

  showDialogToEdit(turno: Turno) {
    this.isEditMode = true;
    this.currentTurnoId = turno.id!;

    // Parse string to Date for p-calendar
    const parseTime = (timeStr: string) => {
        const [hours, minutes] = timeStr.split(':').map(Number);
        const d = new Date();
        d.setHours(hours, minutes, 0, 0);
        return d;
    };

    this.turnoForm.patchValue({
      nombre: turno.nombre,
      horaEntrada: parseTime(turno.horaEntrada),
      horaSalida: parseTime(turno.horaSalida),
      toleranciaMinutos: turno.toleranciaMinutos
    });
    this.displayDialog = true;
  }

  save() {
    if (this.turnoForm.invalid) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Complete los campos requeridos' });
      return;
    }

    const formValue = this.turnoForm.value;
    const formatTime = (d: Date) => {
      const pad = (n: number) => n.toString().padStart(2, '0');
      return `${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
    };

    const turnoData: Turno = {
      nombre: formValue.nombre,
      horaEntrada: formatTime(formValue.horaEntrada),
      horaSalida: formatTime(formValue.horaSalida),
      toleranciaMinutos: formValue.toleranciaMinutos,
      estado: true
    };

    if (this.isEditMode && this.currentTurnoId) {
      this.turnoService.update(this.currentTurnoId, turnoData).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Turno actualizado' });
          this.loadTurnos();
          this.displayDialog = false;
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error al actualizar' })
      });
    } else {
      this.turnoService.create(turnoData).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Turno creado' });
          this.loadTurnos();
          this.displayDialog = false;
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error al crear' })
      });
    }
  }

  delete(turno: Turno) {
    if (confirm(`¿Estás seguro de inhabilitar el turno ${turno.nombre}?`)) {
      this.turnoService.delete(turno.id!).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Turno eliminado' });
          this.loadTurnos();
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar' })
      });
    }
  }
}

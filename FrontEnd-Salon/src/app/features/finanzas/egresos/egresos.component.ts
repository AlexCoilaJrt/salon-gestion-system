import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EgresoService, EgresoDTO } from '../services/egreso.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';

@Component({
  selector: 'app-egresos',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastModule, ButtonModule, DialogModule, TableModule, InputNumberModule, DropdownModule],
  providers: [MessageService],
  templateUrl: './egresos.component.html'
})
export class EgresosComponent implements OnInit {
  private egresoService = inject(EgresoService);
  private messageService = inject(MessageService);

  egresos: EgresoDTO[] = [];
  loading = false;
  displayDialog = false;

  categorias = [
    { label: 'Insumos', value: 'Insumos' },
    { label: 'Servicios Básicos', value: 'Servicios Básicos' },
    { label: 'Caja Chica', value: 'Caja Chica' },
    { label: 'Adelantos a Personal', value: 'Adelantos a Personal' },
    { label: 'Otros', value: 'Otros' }
  ];

  nuevoEgreso = {
    monto: null as number | null,
    categoria: null as string | null,
    motivo: ''
  };

  ngOnInit(): void {
    this.cargarEgresos();
  }

  cargarEgresos() {
    this.loading = true;
    this.egresoService.obtenerEgresosCajaActual().subscribe({
      next: (data) => {
        this.egresos = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los egresos.' });
        this.loading = false;
      }
    });
  }

  abrirDialogo() {
    this.nuevoEgreso = { monto: null, categoria: null, motivo: '' };
    this.displayDialog = true;
  }

  guardarEgreso() {
    if (!this.nuevoEgreso.monto || !this.nuevoEgreso.categoria || !this.nuevoEgreso.motivo) {
      this.messageService.add({ severity: 'warn', summary: 'Advertencia', detail: 'Debe completar todos los campos.' });
      return;
    }

    this.egresoService.registrarEgreso({
      monto: this.nuevoEgreso.monto,
      categoria: this.nuevoEgreso.categoria,
      motivo: this.nuevoEgreso.motivo
    }).subscribe({
      next: (egreso) => {
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Egreso registrado correctamente.' });
        this.egresos.unshift(egreso);
        this.displayDialog = false;
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al registrar el egreso.' });
      }
    });
  }
}

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-asistencia',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './asistencia.component.html',
  styles: [`
    .kiosk-btn {
      transition: transform 0.1s, background-color 0.2s;
    }
    .kiosk-btn:active {
      transform: scale(0.95);
    }
  `]
})
export class AsistenciaComponent {
  private http = inject(HttpClient);

  currentTime: string = '';
  currentDate: string = '';
  dniInput: string = '';
  loading: boolean = false;
  
  private clockInterval: any;

  ngOnInit() {
    this.updateClock();
    this.clockInterval = setInterval(() => this.updateClock(), 1000);
  }

  ngOnDestroy() {
    if (this.clockInterval) {
      clearInterval(this.clockInterval);
    }
  }

  updateClock() {
    const now = new Date();
    this.currentTime = now.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    this.currentDate = now.toLocaleDateString('es-PE', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  }

  appendNumber(num: string) {
    if (this.dniInput.length < 15) {
      this.dniInput += num;
    }
  }

  clear() {
    this.dniInput = '';
  }

  backspace() {
    if (this.dniInput.length > 0) {
      this.dniInput = this.dniInput.slice(0, -1);
    }
  }

  marcarAsistencia(accion: string) {
    if (!this.dniInput || this.dniInput.length < 8) {
      Swal.fire({
        title: 'DNI Inválido',
        text: 'Por favor, ingresa un DNI válido de al menos 8 dígitos.',
        icon: 'warning',
        confirmButtonColor: '#984b5d'
      });
      return;
    }

    this.loading = true;
    const body = { dni: this.dniInput, accion: accion };

    this.http.post<any>('http://localhost:8080/api/rrhh/asistencias/kiosko', body).subscribe({
      next: (res) => {
        const isEntrada = res.tipoRegistro === 'ENTRADA' || res.tipoRegistro === 'FIN_DESCANSO';
        Swal.fire({
          title: isEntrada ? `¡Bienvenido, ${res.empleadoNombre}!` : `¡Éxito, ${res.empleadoNombre}!`,
          text: res.mensaje,
          icon: 'success',
          timer: 3000,
          showConfirmButton: false
        });
        this.dniInput = '';
        this.loading = false;
      },
      error: (err) => {
        Swal.fire({
          title: 'Error',
          text: err.error?.message || 'Error al registrar asistencia. Verifica tu DNI.',
          icon: 'error',
          confirmButtonColor: '#984b5d'
        });
        this.dniInput = '';
        this.loading = false;
      }
    });
  }
}

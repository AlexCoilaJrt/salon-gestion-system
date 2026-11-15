import { Component, OnInit, OnChanges, SimpleChanges, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';
import { BiometricoService, BiometricoConfig, BiometricoTestResult, BiometricoSincronizacion } from '../../../core/services/biometrico.service';

@Component({
  selector: 'app-biometrico-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DialogModule,
    InputTextModule,
    ButtonModule,
    CheckboxModule
  ],
  templateUrl: './biometrico-modal.component.html'
})
export class BiometricoModalComponent implements OnInit, OnChanges {
  @Input() visible: boolean = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  private biometricoService = inject(BiometricoService);
  private messageService = inject(MessageService);

  probandoConexion: boolean = false;
  guardandoConfig: boolean = false;
  sincronizando: boolean = false;

  resultadoTest: BiometricoTestResult | null = null;
  resultadoSync: BiometricoSincronizacion | null = null;

  configForm: BiometricoConfig = {
    ip: '192.168.1.201',
    puerto: 4370,
    commKey: '0',
    nombreDispositivo: 'ZKTeco SenseFace 3A',
    autoSincronizar: true,
    intervaloMinutos: 15,
    activo: true
  };

  ngOnInit(): void {
    this.cargarConfig();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && changes['visible'].currentValue === true) {
      this.cargarConfig();
      this.resultadoTest = null;
      this.resultadoSync = null;
    }
  }

  cargarConfig(): void {
    this.biometricoService.getConfig().subscribe({
      next: (cfg) => {
        if (cfg) {
          this.configForm = {
            ip: cfg.ip || '192.168.1.201',
            puerto: cfg.puerto || 4370,
            commKey: cfg.commKey || '0',
            nombreDispositivo: cfg.nombreDispositivo || 'ZKTeco SenseFace 3A',
            autoSincronizar: cfg.autoSincronizar ?? true,
            intervaloMinutos: cfg.intervaloMinutos || 15,
            activo: cfg.activo ?? true
          };
        }
      },
      error: () => {}
    });
  }

  probarConexion(): void {
    this.probandoConexion = true;
    this.resultadoTest = null;
    this.resultadoSync = null;

    this.biometricoService.testConexion({
      ip: this.configForm.ip,
      puerto: this.configForm.puerto
    }).subscribe({
      next: (res) => {
        this.resultadoTest = res;
        this.probandoConexion = false;
        if (res.conectado) {
          this.messageService.add({
            severity: 'success',
            summary: 'Biométrico Conectado',
            detail: res.mensaje
          });
        } else {
          this.messageService.add({
            severity: 'warn',
            summary: 'Sin Conexión con Terminal',
            detail: res.mensaje
          });
        }
      },
      error: (err) => {
        this.probandoConexion = false;
        this.resultadoTest = {
          conectado: false,
          ip: this.configForm.ip,
          puerto: this.configForm.puerto,
          mensaje: 'Error al contactar con el backend del sistema: ' + (err.error?.message || err.message)
        };
        this.messageService.add({
          severity: 'error',
          summary: 'Fallo de Red',
          detail: 'No se pudo emitir la prueba de conexión.'
        });
      }
    });
  }

  sincronizar(): void {
    this.sincronizando = true;
    this.resultadoSync = null;

    this.biometricoService.sincronizar().subscribe({
      next: (res) => {
        this.sincronizando = false;
        this.resultadoSync = res;
        this.messageService.add({
          severity: res.exito ? 'success' : 'warn',
          summary: res.exito ? 'Sincronización Exitosa' : 'Aviso',
          detail: res.mensaje
        });
      },
      error: (err) => {
        this.sincronizando = false;
        this.resultadoSync = {
          exito: false,
          totalMarcacionesLeidas: 0,
          nuevasAsistenciasRegistradas: 0,
          fechaSincronizacion: new Date().toISOString(),
          mensaje: 'Error de comunicación: ' + (err.error?.message || err.message)
        };
      }
    });
  }

  guardarConfig(): void {
    this.guardandoConfig = true;
    this.biometricoService.saveConfig(this.configForm).subscribe({
      next: () => {
        this.guardandoConfig = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Configuración Guardada',
          detail: 'Parámetros del terminal biométrico ZKTeco SenseFace 3A actualizados.'
        });
        this.cerrar();
      },
      error: () => {
        this.guardandoConfig = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo guardar la configuración del biométrico.'
        });
      }
    });
  }

  cerrar(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }

  onHide(): void {
    this.visible = false;
    this.visibleChange.emit(false);
  }
}

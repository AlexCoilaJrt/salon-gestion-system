import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule, FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { MenuModule } from 'primeng/menu';
import { MenuItem, MessageService } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { ToastModule } from 'primeng/toast';
import { AuthService } from '../../auth/services/auth.service';
import { EmpresaService, EmpresaResponse } from '../../services/empresa.service';
import { ConfiguracionService } from '../../services/configuracion.service';
import { CatalogoService, Producto } from '../../../features/catalogo/services/catalogo.service';
import { AgendaService, ClienteResponse } from '../../../features/operaciones/pages/agenda/agenda.service';
import { RrhhService, Empleado } from '../../../features/rrhh/services/rrhh.service';

import { LayoutService } from '../../services/layout.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    AvatarModule,
    BadgeModule,
    MenuModule,
    DialogModule,
    ButtonModule,
    OverlayPanelModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './header.component.html'
})
export class HeaderComponent implements OnInit {
  public layoutService = inject(LayoutService);
  private authService = inject(AuthService);
  private router = inject(Router);

  toggleSidebar(): void {
    if (typeof window !== 'undefined' && window.innerWidth < 768) {
      this.layoutService.toggleMobileSidebar();
    } else {
      this.layoutService.toggleSidebar();
    }
  }
  private fb = inject(FormBuilder);
  private empresaService = inject(EmpresaService);
  private configuracionService = inject(ConfiguracionService);
  private messageService = inject(MessageService);
  private catalogoService = inject(CatalogoService);
  private agendaService = inject(AgendaService);
  private rrhhService = inject(RrhhService);
  
  currentUser = signal<any>(null);
  isDarkMode = signal<boolean>(false);
  profileMenuItems: MenuItem[] = [];
  alertasStock = signal<Producto[]>([]);
  empresaData = signal<EmpresaResponse | null>(null);

  // Cumpleaños
  cumpleanerosEmpleados = signal<any[]>([]);
  
  get totalCumpleanos() {
    return this.cumpleanerosEmpleados().length;
  }

  // Modals state
  displayPerfilModal = signal<boolean>(false);
  displayEmpresaModal = signal<boolean>(false);

  // Modal Tiempo de Sesión
  displaySesionModal = signal<boolean>(false);
  sessionTimeoutMinutes = signal<number>(1440); // 24 horas por defecto
  selectedPresetMinutes = signal<number>(1440);
  isCustomTimeout = signal<boolean>(false);
  customHoursInput = signal<number>(24);
  isSavingSesion = signal<boolean>(false);

  presetOptions = [
    { label: '8 Horas', minutes: 480, desc: 'Turno estándar de trabajo', icon: 'pi pi-briefcase' },
    { label: '12 Horas', minutes: 720, desc: 'Jornada extendida de atención', icon: 'pi pi-sun' },
    { label: '24 Horas', minutes: 1440, desc: '1 día ininterrumpido sin cortes', icon: 'pi pi-check-circle', badge: 'Recomendado' },
    { label: '48 Horas', minutes: 2880, desc: '2 días continuos sin pedir clave', icon: 'pi pi-calendar' },
    { label: '7 Días', minutes: 10080, desc: '1 semana completa de sesión activa', icon: 'pi pi-shield' },
    { label: 'Personalizado', minutes: -1, desc: 'Define la cantidad exacta en horas', icon: 'pi pi-sliders-h' }
  ];

  // Form
  empresaForm!: FormGroup;
  currentEmpresaId: number | null = null;
  isSaving = signal<boolean>(false);

  ngOnInit() {
    this.currentUser.set(this.authService.getCurrentUser());

    this.empresaService.empresa$.subscribe(emp => {
      if (emp) this.empresaData.set(emp);
    });
    this.empresaService.getEmpresaActiva().subscribe({
      error: () => {}
    });

    // Restaurar la preferencia de modo oscuro guardada
    const savedDarkMode = localStorage.getItem('darkMode') === 'true';
    if (savedDarkMode) {
      document.documentElement.classList.add('app-dark');
    }
    this.isDarkMode.set(document.documentElement.classList.contains('app-dark'));

    this.initForm();

    // Empresa y tiempo de sesión son configuración global: el backend sólo la permite al ADMIN
    const roles: string[] = this.currentUser()?.roles || [];
    const isAdmin = roles.some(r => ['ADMIN', 'ROLE_ADMIN', 'ADMINISTRADOR'].includes(String(r).toUpperCase()));

    this.profileMenuItems = [
      {
        label: 'Mi Perfil',
        icon: 'pi pi-user',
        command: () => {
          this.displayPerfilModal.set(true);
        }
      },
      {
        label: 'Configurar Empresa',
        icon: 'pi pi-cog',
        visible: isAdmin,
        command: () => {
          this.abrirModalEmpresa();
        }
      },
      {
        label: 'Tiempo de Sesión',
        icon: 'pi pi-clock',
        visible: isAdmin,
        command: () => {
          this.abrirModalSesion();
        }
      },
      { separator: true },
      {
        label: 'Cerrar Sesión',
        icon: 'pi pi-sign-out',
        command: () => this.logout()
      }
    ];

    this.cargarAlertasStock();
    this.cargarCumpleanos();
  }

  cargarCumpleanos() {
    const currentMonth = new Date().getMonth() + 1; // 1-12

    this.rrhhService.getEmpleados().subscribe(empleados => {
      const cumpleaneros = empleados.filter(e => {
        if (!e.fechaNacimiento) return false;
        // The date comes as YYYY-MM-DD from backend
        const [year, month, day] = e.fechaNacimiento.toString().split('-');
        return parseInt(month, 10) === currentMonth;
      });
      // Sort by day of month
      cumpleaneros.sort((a, b) => {
        const dayA = parseInt(a.fechaNacimiento!.toString().split('-')[2], 10);
        const dayB = parseInt(b.fechaNacimiento!.toString().split('-')[2], 10);
        return dayA - dayB;
      });
      this.cumpleanerosEmpleados.set(cumpleaneros);
    });
  }

  cargarAlertasStock() {
    this.catalogoService.getProductosAlertasStock().subscribe({
      next: (res: Producto[]) => this.alertasStock.set(res),
      error: (err: any) => console.error('Error cargando alertas de stock', err)
    });
  }

  irAInventario() {
    this.router.navigate(['/dashboard/catalogo/inventario']);
  }

  initForm() {
    this.empresaForm = this.fb.group({
      nombreComercial: ['', [Validators.required, Validators.maxLength(150)]],
      razonSocial: ['', [Validators.required, Validators.maxLength(150)]],
      ruc: ['', Validators.maxLength(20)],
      direccion: ['', Validators.maxLength(255)],
      telefono: ['', Validators.maxLength(50)],
      email: ['', [Validators.email, Validators.maxLength(100)]],
      logoUrl: ['']
    });
  }

  abrirModalEmpresa() {
    this.empresaService.getEmpresaActiva().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.currentEmpresaId = res.data.id;
          this.empresaForm.patchValue(res.data);
          this.logoPreview.set(res.data.logoUrl || null);
          this.displayEmpresaModal.set(true);
        }
      },
      error: (err) => console.error('Error obteniendo datos de empresa', err)
    });
  }

  guardarEmpresa() {
    if (this.empresaForm.invalid || !this.currentEmpresaId) return;
    
    this.isSaving.set(true);
    this.empresaService.actualizarEmpresa(this.currentEmpresaId, this.empresaForm.value).subscribe({
      next: (res) => {
        this.isSaving.set(false);
        this.displayEmpresaModal.set(false);
        // La actualización ahora es reactiva a través del BehaviorSubject en EmpresaService
      },
      error: (err) => {
        this.isSaving.set(false);
        console.error('Error guardando empresa', err);
      }
    });
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: () => {
        this.authService.clearLocalSession();
        this.router.navigate(['/login']);
      }
    });
  }

  logoPreview = signal<string | null>(null);

  toggleDarkMode() {
    const htmlElement = document.documentElement;
    htmlElement.classList.toggle('app-dark');
    const isDark = htmlElement.classList.contains('app-dark');
    this.isDarkMode.set(isDark);
    // Guardar preferencia en localStorage para que persista al recargar
    localStorage.setItem('darkMode', isDark.toString());
  }

  isUploadingAvatar = signal<boolean>(false);

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        const base64String = reader.result as string;
        this.logoPreview.set(base64String);
        this.empresaForm.patchValue({ logoUrl: base64String });
      };
      reader.readAsDataURL(file);
    }
  }

  onUserAvatarSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        const base64String = reader.result as string;
        
        this.isUploadingAvatar.set(true);
        this.authService.updateAvatar(base64String).subscribe({
          next: () => {
            // Actualizar localmente la vista
            this.currentUser.set(this.authService.getCurrentUser());
            this.isUploadingAvatar.set(false);
          },
          error: (err) => {
            console.error('Error al subir el avatar del usuario', err);
            this.isUploadingAvatar.set(false);
          }
        });
      };
      reader.readAsDataURL(file);
    }
  }

  getDisplayName(): string {
    const user = this.currentUser();
    if (!user) return 'Invitado';
    
    // Si tiene nombres y apellidos definidos y no están vacíos
    if (user.firstName && user.firstName !== user.username) {
      const fullName = `${user.firstName} ${user.lastName || ''}`.trim();
      if (fullName) return fullName;
    }
    
    // Fallback al correo
    return user.email ? user.email.split('@')[0] : 'Usuario';
  }

  abrirModalSesion() {
    this.configuracionService.obtenerPorClave('SESSION_TIMEOUT_MINUTES').subscribe({
      next: (res) => {
        const minutes = res && res.valor ? parseInt(res.valor, 10) : 1440;
        const validMinutes = isNaN(minutes) || minutes <= 0 ? 1440 : minutes;
        this.sessionTimeoutMinutes.set(validMinutes);

        const match = this.presetOptions.find(p => p.minutes === validMinutes);
        if (match) {
          this.selectedPresetMinutes.set(validMinutes);
          this.isCustomTimeout.set(false);
          this.customHoursInput.set(Math.round(validMinutes / 60));
        } else {
          this.selectedPresetMinutes.set(-1);
          this.isCustomTimeout.set(true);
          this.customHoursInput.set(Math.round(validMinutes / 60));
        }
        this.displaySesionModal.set(true);
      },
      error: () => {
        this.sessionTimeoutMinutes.set(1440);
        this.selectedPresetMinutes.set(1440);
        this.isCustomTimeout.set(false);
        this.customHoursInput.set(24);
        this.displaySesionModal.set(true);
      }
    });
  }

  seleccionarPresetSesion(minutes: number) {
    if (minutes === -1) {
      this.isCustomTimeout.set(true);
      this.selectedPresetMinutes.set(-1);
    } else {
      this.isCustomTimeout.set(false);
      this.selectedPresetMinutes.set(minutes);
      this.customHoursInput.set(Math.round(minutes / 60));
    }
  }

  onCustomHoursChange(hours: any) {
    const val = typeof hours === 'number' ? hours : parseInt(hours, 10);
    if (!isNaN(val) && val > 0) {
      this.customHoursInput.set(val);
    }
  }

  guardarSesionTimeout() {
    let minutesToSave = this.selectedPresetMinutes();
    if (this.isCustomTimeout()) {
      const hours = this.customHoursInput();
      if (!hours || hours <= 0) {
        this.messageService.add({
          severity: 'warn',
          summary: 'Atención',
          detail: 'Por favor ingresa un número de horas válido mayor a 0.'
        });
        return;
      }
      minutesToSave = hours * 60;
    }

    this.isSavingSesion.set(true);
    this.configuracionService.actualizar('SESSION_TIMEOUT_MINUTES', minutesToSave.toString()).subscribe({
      next: () => {
        this.sessionTimeoutMinutes.set(minutesToSave);
        // Renovar token de inmediato para aplicar el nuevo tiempo
        this.authService.refreshToken().subscribe({
          next: () => {},
          error: () => {}
        });

        const horasTexto = (minutesToSave / 60).toFixed(minutesToSave % 60 === 0 ? 0 : 1);
        this.isSavingSesion.set(false);
        this.displaySesionModal.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Tiempo de Sesión Actualizado',
          detail: `La sesión se mantendrá abierta por ${horasTexto} horas sin cerrarse.`
        });
      },
      error: (err) => {
        this.isSavingSesion.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo guardar la configuración de sesión.'
        });
        console.error('Error guardando tiempo de sesión', err);
      }
    });
  }

  getTextoDuracionActual(): string {
    const min = this.sessionTimeoutMinutes();
    if (min >= 1440 && min % 1440 === 0) {
      const dias = min / 1440;
      return `${dias} ${dias === 1 ? 'día (24h)' : 'días'}`;
    }
    const horas = (min / 60).toFixed(min % 60 === 0 ? 0 : 1);
    return `${horas} horas`;
  }
}

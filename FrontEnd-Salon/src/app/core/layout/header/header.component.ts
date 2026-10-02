import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { MenuModule } from 'primeng/menu';
import { MenuItem } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { AuthService } from '../../auth/services/auth.service';
import { EmpresaService, EmpresaResponse } from '../../services/empresa.service';
import { CatalogoService, Producto } from '../../../features/catalogo/services/catalogo.service';
import { AgendaService, ClienteResponse } from '../../../features/operaciones/pages/agenda/agenda.service';
import { RrhhService, Empleado } from '../../../features/rrhh/services/rrhh.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputTextModule, AvatarModule, BadgeModule, MenuModule, DialogModule, ButtonModule, OverlayPanelModule],
  templateUrl: './header.component.html'
})
export class HeaderComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private empresaService = inject(EmpresaService);
  private catalogoService = inject(CatalogoService);
  private agendaService = inject(AgendaService);
  private rrhhService = inject(RrhhService);
  
  currentUser = signal<any>(null);
  isDarkMode = signal<boolean>(false);
  profileMenuItems: MenuItem[] = [];
  alertasStock = signal<Producto[]>([]);

  // Cumpleaños
  cumpleanerosEmpleados = signal<any[]>([]);
  
  get totalCumpleanos() {
    return this.cumpleanerosEmpleados().length;
  }

  // Modals state
  displayPerfilModal = signal<boolean>(false);
  displayEmpresaModal = signal<boolean>(false);

  // Form
  empresaForm!: FormGroup;
  currentEmpresaId: number | null = null;
  isSaving = signal<boolean>(false);

  ngOnInit() {
    this.currentUser.set(this.authService.getCurrentUser());

    // Restaurar la preferencia de modo oscuro guardada
    const savedDarkMode = localStorage.getItem('darkMode') === 'true';
    if (savedDarkMode) {
      document.documentElement.classList.add('app-dark');
    }
    this.isDarkMode.set(document.documentElement.classList.contains('app-dark'));

    this.initForm();

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
        command: () => {
          this.abrirModalEmpresa();
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
}

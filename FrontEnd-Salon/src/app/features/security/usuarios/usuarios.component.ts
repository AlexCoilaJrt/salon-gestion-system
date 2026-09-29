import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { DropdownModule } from 'primeng/dropdown';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';
import { UsuarioService, UsuarioResponseDTO } from '../../../core/services/usuario.service';
import { SecurityService, Role } from '../services/security.service';

@Component({
  selector: 'app-usuarios',
  imports: [
    CommonModule, 
    FormsModule, 
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    TagModule,
    DialogModule,
    ToastModule,
    DropdownModule,
    CheckboxModule
  ],
  providers: [MessageService, DatePipe],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.scss'
})
export class UsuariosComponent implements OnInit {
  usuarios: UsuarioResponseDTO[] = [];
  roles: Role[] = [];
  loading: boolean = false;
  totalRecords: number = 0;
  
  // Dialog State
  displayDialog: boolean = false;
  usuarioForm: FormGroup;
  isEditMode: boolean = false;
  selectedUsuarioId: number | null = null;
  
  private usuarioService = inject(UsuarioService);
  private securityService = inject(SecurityService);
  private messageService = inject(MessageService);
  private datePipe = inject(DatePipe);
  private fb = inject(FormBuilder);

  constructor() {
    this.usuarioForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(4)]],
      password: [''], // solo requerido si es nuevo
      email: ['', [Validators.required, Validators.email]],
      roleId: [null, Validators.required],
      estado: [true]
    });
  }

  ngOnInit() {
    this.loadUsuarios();
    this.loadRoles();
  }

  loadUsuarios(page: number = 0, size: number = 10) {
    this.loading = true;
    this.usuarioService.getUsuarios(page, size).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.usuarios = response.data.content.map(u => ({
            ...u,
            ultimoAcceso: u.ultimoAcceso ? this.datePipe.transform(u.ultimoAcceso, 'yyyy-MM-dd HH:mm')! : 'Nunca'
          }));
          this.totalRecords = response.data.totalElements;
        }
        this.loading = false;
      },
      error: (error) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los usuarios' });
        this.loading = false;
      }
    });
  }

  loadRoles() {
    this.securityService.getRoles(0, 100).subscribe({
      next: (res) => {
        if(res.success && res.data) {
          this.roles = res.data.content.filter(r => r.active); // Solo roles activos
        }
      }
    });
  }

  onPageChange(event: any) {
    const page = event.first / event.rows;
    const size = event.rows;
    this.loadUsuarios(page, size);
  }

  openNew() {
    this.isEditMode = false;
    this.selectedUsuarioId = null;
    this.usuarioForm.reset({ estado: true });
    
    // El password es requerido en creación
    this.usuarioForm.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);
    this.usuarioForm.get('password')?.updateValueAndValidity();
    this.usuarioForm.get('username')?.enable();

    this.displayDialog = true;
  }

  openEdit(usuario: UsuarioResponseDTO) {
    this.isEditMode = true;
    this.selectedUsuarioId = usuario.id;
    
    // Buscar el ID del rol en base a su nombre
    const roleId = this.roles.find(r => r.name === usuario.rol)?.id;

    this.usuarioForm.patchValue({
      username: usuario.username,
      email: usuario.email,
      roleId: roleId,
      estado: usuario.estado,
      password: ''
    });

    // Password no es requerido en edición (se deja en blanco si no se cambia)
    this.usuarioForm.get('password')?.clearValidators();
    this.usuarioForm.get('password')?.updateValueAndValidity();
    
    // Si es ADMIN, no dejar cambiar su username
    if(usuario.username === 'admin@salon.com') {
       this.usuarioForm.get('username')?.disable();
       this.usuarioForm.get('estado')?.disable(); // No se puede inactivar
    } else {
       this.usuarioForm.get('username')?.enable();
       this.usuarioForm.get('estado')?.enable();
    }

    this.displayDialog = true;
  }

  saveUsuario() {
    if (this.usuarioForm.invalid) {
      this.usuarioForm.markAllAsTouched();
      return;
    }

    const formValues = this.usuarioForm.getRawValue();
    const requestData = {
      username: formValues.username,
      email: formValues.email,
      password: formValues.password,
      estado: formValues.estado,
      roleIds: [formValues.roleId]
    };

    if (this.isEditMode && this.selectedUsuarioId) {
      this.usuarioService.updateUsuario(this.selectedUsuarioId, requestData).subscribe({
        next: () => this.finishSave('Usuario actualizado con éxito'),
        error: (err) => this.showError(err.error?.message || 'Error al actualizar usuario')
      });
    } else {
      this.usuarioService.createUsuario(requestData).subscribe({
        next: () => this.finishSave('Usuario creado con éxito'),
        error: (err) => this.showError(err.error?.message || 'Error al crear usuario')
      });
    }
  }

  private finishSave(message: string) {
    this.displayDialog = false;
    this.loadUsuarios();
    this.messageService.add({ severity: 'success', summary: 'Éxito', detail: message });
  }

  private showError(msg: string) {
    this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
  }

  toggleStatus(usuario: UsuarioResponseDTO) {
    if(usuario.username === 'admin@salon.com') {
       this.showError('No se puede bloquear al administrador principal');
       return;
    }
    
    const action = usuario.estado ? 'bloquear' : 'desbloquear';
    if(confirm(`¿Estás seguro que deseas ${action} al usuario ${usuario.username}?`)) {
      this.usuarioService.toggleStatus(usuario.id).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Usuario ${action}do` });
          this.loadUsuarios();
        },
        error: (err) => this.showError(err.error?.message || 'No se pudo cambiar el estado')
      });
    }
  }
}

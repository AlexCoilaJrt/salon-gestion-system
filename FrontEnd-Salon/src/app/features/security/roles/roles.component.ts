import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SecurityService, Role, Permission } from '../services/security.service';
import { MessageService } from 'primeng/api';

// PrimeNG UI
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { CheckboxModule } from 'primeng/checkbox';
import { ToastModule } from 'primeng/toast';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    TableModule, ButtonModule, DialogModule, InputTextModule, 
    TextareaModule, CheckboxModule, ToastModule, TagModule
  ],
  providers: [MessageService],
  templateUrl: './roles.component.html'
})
export class RolesComponent implements OnInit {
  
  private securityService = inject(SecurityService);
  private messageService = inject(MessageService);
  private fb = inject(FormBuilder);

  roles: Role[] = [];
  permissions: Permission[] = [];
  groupedPermissions: { module: string, items: Permission[] }[] = [];
  
  // Table state
  loading: boolean = false;
  totalRecords: number = 0;
  
  // Dialog state
  displayDialog: boolean = false;
  roleForm: FormGroup;
  isEditMode: boolean = false;
  selectedRole: Role | null = null;
  
  // Selected permissions (IDs)
  selectedPermissionIds: number[] = [];

  constructor() {
    this.roleForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: [''],
      active: [true]
    });
  }

  ngOnInit() {
    this.loadRoles();
    this.loadPermissions();
  }

  loadRoles(page: number = 0, size: number = 10) {
    this.loading = true;
    this.securityService.getRoles(page, size).subscribe({
      next: (response: any) => {
        this.roles = response.data.content;
        this.totalRecords = response.data.totalElements;
        this.loading = false;
      },
      error: (err: any) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudieron cargar los roles.' });
        this.loading = false;
      }
    });
  }

  loadPermissions() {
    this.securityService.getAllPermissionsList().subscribe({
      next: (response: any) => {
        this.permissions = response.data || [];
        this.groupPermissionsByModule();
      },
      error: (err: any) => {
        console.error('Error al cargar permisos:', err);
        this.permissions = [];
        this.groupPermissionsByModule();
      }
    });
  }

  groupPermissionsByModule() {
    const map = new Map<string, Permission[]>();
    this.permissions.forEach(p => {
      const mod = p.module || 'Otros';
      if (!map.has(mod)) map.set(mod, []);
      map.get(mod)!.push(p);
    });
    this.groupedPermissions = Array.from(map.entries()).map(([module, items]) => ({ module, items }));
  }

  onPageChange(event: any) {
    this.loadRoles(event.first / event.rows, event.rows);
  }

  openNew() {
    this.isEditMode = false;
    this.selectedRole = null;
    this.selectedPermissionIds = [];
    this.roleForm.reset({ active: true });
    this.roleForm.get('name')?.enable();
    this.displayDialog = true;
  }

  openEdit(role: Role) {
    this.isEditMode = true;
    this.selectedRole = role;
    this.roleForm.patchValue({
      name: role.name,
      description: role.description,
      active: role.active
    });
    
    // Si es ADMIN, no permitimos editar el nombre por seguridad
    if (role.name === 'ADMIN') {
      this.roleForm.get('name')?.disable();
    } else {
      this.roleForm.get('name')?.enable();
    }

    this.selectedPermissionIds = role.permissions?.map((p: any) => p.id) || [];
    this.displayDialog = true;
  }

  saveRole() {
    if (this.roleForm.invalid) return;
    
    const roleData = this.roleForm.getRawValue();
    roleData.name = roleData.name.toUpperCase();
    roleData.permissionIds = this.selectedPermissionIds;

    if (this.isEditMode && this.selectedRole?.id) {
      this.securityService.updateRole(this.selectedRole.id, roleData).subscribe({
        next: (res: any) => this.finishSave(),
        error: (err: any) => this.showError(err.error?.message || 'No se pudo actualizar el rol')
      });
    } else {
      this.securityService.createRole(roleData).subscribe({
        next: (res: any) => this.finishSave(),
        error: (err: any) => this.showError(err.error?.message || 'No se pudo crear el rol')
      });
    }
  }

  private finishSave() {
    this.displayDialog = false;
    this.loadRoles();
    this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Rol guardado correctamente.' });
  }

  deleteRole(role: Role) {
    if (role.name === 'ADMIN') {
      this.showError('No se puede eliminar el rol de Administrador.');
      return;
    }
    
    if (confirm(`¿Estás seguro de eliminar el rol ${role.name}?`)) {
      this.securityService.deleteRole(role.id!).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Rol eliminado.' });
          this.loadRoles();
        },
        error: () => this.showError('No se pudo eliminar el rol.')
      });
    }
  }

  onPermissionCheckboxChange(event: any, permissionId: number) {
    if (event.checked) {
      if (!this.selectedPermissionIds.includes(permissionId)) {
        this.selectedPermissionIds.push(permissionId);
      }
    } else {
      this.selectedPermissionIds = this.selectedPermissionIds.filter(id => id !== permissionId);
    }
  }

  private showError(msg: string) {
    this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
  }
}

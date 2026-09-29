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
import { InputSwitchModule } from 'primeng/inputswitch';
import { TooltipModule } from 'primeng/tooltip';
import { DropdownModule } from 'primeng/dropdown';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    TableModule, ButtonModule, DialogModule, InputTextModule, 
    TextareaModule, CheckboxModule, ToastModule, TagModule,
    InputSwitchModule, TooltipModule, DropdownModule
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
  
  loading: boolean = false;
  
  // Dialog state for Role Info
  displayDialog: boolean = false;
  roleForm: FormGroup;
  isEditMode: boolean = false;
  
  // Master-Detail State
  selectedRole: Role | null = null;
  selectedPermissionIds: number[] = [];
  originalPermissionIds: number[] = []; // To check for unsaved changes

  constructor() {
    this.roleForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: [''],
      active: [true],
      copyFromRoleId: [null]
    });
  }

  ngOnInit() {
    this.loadRoles();
    this.loadPermissions();
  }

  // Load all roles without pagination to show in the list
  loadRoles() {
    this.loading = true;
    this.securityService.getRoles(0, 100).subscribe({
      next: (response: any) => {
        this.roles = response.data.content;
        this.loading = false;
        
        // Update selected role if it was already selected
        if (this.selectedRole) {
          const updatedRole = this.roles.find(r => r.id === this.selectedRole?.id);
          if (updatedRole) {
            this.selectedRole = updatedRole;
            this.syncPermissionsFromRole(updatedRole);
          } else {
            this.selectedRole = null;
          }
        }
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
    this.groupedPermissions = Array.from(map.entries())
      .map(([module, items]) => ({ module, items }))
      .sort((a, b) => a.module.localeCompare(b.module));
  }

  // --- Master Detail Logic ---

  selectRole(role: Role) {
    if (this.hasUnsavedChanges) {
      if (!confirm('Tienes cambios sin guardar. ¿Deseas descartarlos y cambiar de rol?')) {
        return;
      }
    }
    this.selectedRole = role;
    this.syncPermissionsFromRole(role);
  }

  syncPermissionsFromRole(role: Role) {
    this.selectedPermissionIds = role.permissions?.map((p: any) => p.id) || [];
    this.originalPermissionIds = [...this.selectedPermissionIds];
  }

  isPermissionActive(permId: number): boolean {
    return this.selectedPermissionIds.includes(permId);
  }

  togglePermission(permId: number, checked: boolean) {
    if (checked) {
      if (!this.selectedPermissionIds.includes(permId)) {
        this.selectedPermissionIds.push(permId);
      }
    } else {
      this.selectedPermissionIds = this.selectedPermissionIds.filter(id => id !== permId);
    }
  }

  toggleEntireModule(module: string, checked: boolean) {
    const group = this.groupedPermissions.find(g => g.module === module);
    if (!group) return;
    
    group.items.forEach(perm => {
      if (checked) {
        if (!this.selectedPermissionIds.includes(perm.id!)) {
          this.selectedPermissionIds.push(perm.id!);
        }
      } else {
        this.selectedPermissionIds = this.selectedPermissionIds.filter(id => id !== perm.id);
      }
    });
  }

  getActiveCount(module: string): number {
    const group = this.groupedPermissions.find(g => g.module === module);
    if (!group) return 0;
    return group.items.filter(p => this.selectedPermissionIds.includes(p.id!)).length;
  }

  get hasUnsavedChanges(): boolean {
    if (!this.selectedRole) return false;
    if (this.selectedPermissionIds.length !== this.originalPermissionIds.length) return true;
    
    // Sort arrays to compare content
    const current = [...this.selectedPermissionIds].sort();
    const original = [...this.originalPermissionIds].sort();
    
    for (let i = 0; i < current.length; i++) {
      if (current[i] !== original[i]) return true;
    }
    return false;
  }

  discardChanges() {
    this.selectedPermissionIds = [...this.originalPermissionIds];
  }

  savePermissions() {
    if (!this.selectedRole?.id) return;

    const payload = {
      ...this.selectedRole,
      permissionIds: this.selectedPermissionIds
    };

    // Assuming the API expects the full role or at least permissionIds
    this.securityService.updateRole(this.selectedRole.id, payload).subscribe({
      next: (res: any) => {
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Matriz de permisos actualizada.' });
        this.loadRoles(); // Reload to update UI stats
      },
      error: (err: any) => this.showError(err.error?.message || 'Error al actualizar permisos')
    });
  }

  // --- Role CRUD (Info) ---

  openNew() {
    this.isEditMode = false;
    this.roleForm.reset({ active: true, copyFromRoleId: null });
    this.roleForm.get('name')?.enable();
    this.displayDialog = true;
  }

  openEdit(role: Role) {
    this.isEditMode = true;
    this.roleForm.patchValue({
      name: role.name,
      description: role.description,
      active: role.active
    });
    
    if (role.name === 'ADMIN') {
      this.roleForm.get('name')?.disable();
    } else {
      this.roleForm.get('name')?.enable();
    }

    this.displayDialog = true;
  }

  saveRoleInfo() {
    if (this.roleForm.invalid) return;
    
    const roleData = this.roleForm.getRawValue();
    roleData.name = roleData.name.toUpperCase();
    
    // Si estamos editando, preservamos los permisos actuales
    if (this.isEditMode && this.selectedRole) {
      roleData.permissionIds = this.selectedRole.permissions?.map((p:any) => p.id) || [];
      this.securityService.updateRole(this.selectedRole.id!, roleData).subscribe({
        next: (res: any) => this.finishSaveInfo(res.data || this.selectedRole),
        error: (err: any) => this.showError(err.error?.message || 'No se pudo actualizar el rol')
      });
    } else {
      // Nuevo rol
      if (roleData.copyFromRoleId) {
        const sourceRole = this.roles.find(r => r.id === roleData.copyFromRoleId);
        roleData.permissionIds = sourceRole?.permissions?.map((p:any) => p.id) || [];
      } else {
        roleData.permissionIds = []; // Nace vacío
      }
      
      delete roleData.copyFromRoleId; // No enviar al backend
      
      this.securityService.createRole(roleData).subscribe({
        next: (res: any) => this.finishSaveInfo(res.data),
        error: (err: any) => this.showError(err.error?.message || 'No se pudo crear el rol')
      });
    }
  }

  private finishSaveInfo(savedRole?: Role) {
    this.displayDialog = false;
    this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Información del rol guardada.' });
    
    // Recargar roles y auto-seleccionar el rol recién guardado
    this.securityService.getRoles(0, 100).subscribe(res => {
      this.roles = res.data.content;
      if (savedRole && savedRole.id) {
        const found = this.roles.find(r => r.id === savedRole.id);
        if (found) {
          this.selectRole(found);
        }
      }
    });
  }

  private showError(msg: string) {
    this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
  }
}

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { CatalogoService, Categoria } from '../services/catalogo.service';

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, DialogModule, InputTextModule, TagModule, ToastModule, ConfirmDialogModule],
  providers: [MessageService, ConfirmationService],
  templateUrl: './categorias.component.html'
})
export class CategoriasComponent implements OnInit {
  private catalogoService = inject(CatalogoService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  categorias: Categoria[] = [];
  categoriaDialog: boolean = false;
  categoria: Categoria = { nombre: '' };
  isEdit: boolean = false;
  loading: boolean = true;

  ngOnInit() {
    this.loadCategorias();
  }

  loadCategorias() {
    this.loading = true;
    this.catalogoService.getCategorias().subscribe({
      next: (data) => {
        this.categorias = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  openNew() {
    this.categoria = { nombre: '' };
    this.isEdit = false;
    this.categoriaDialog = true;
  }

  editCategoria(cat: Categoria) {
    this.categoria = { ...cat };
    this.isEdit = true;
    this.categoriaDialog = true;
  }

  hideDialog() {
    this.categoriaDialog = false;
  }

  saveCategoria() {
    if (this.categoria.nombre.trim()) {
      if (this.isEdit && this.categoria.id) {
        this.catalogoService.updateCategoria(this.categoria.id, this.categoria).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Categoría Actualizada' });
            this.loadCategorias();
            this.hideDialog();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al actualizar' })
        });
      } else {
        this.catalogoService.createCategoria(this.categoria).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Categoría Creada' });
            this.loadCategorias();
            this.hideDialog();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al crear' })
        });
      }
    }
  }

  deleteCategoria(cat: Categoria) {
    this.confirmationService.confirm({
      message: '¿Estás seguro de que quieres eliminar la categoría ' + cat.nombre + '?',
      header: 'Confirmar Eliminación',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        if(cat.id) {
          this.catalogoService.deleteCategoria(cat.id).subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Categoría Eliminada' });
              this.loadCategorias();
            },
            error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar' })
          });
        }
      }
    });
  }
}

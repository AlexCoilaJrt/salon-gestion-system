import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { CheckboxModule } from 'primeng/checkbox';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { FileUploadModule } from 'primeng/fileupload';
import { CatalogoService, Producto, Categoria } from '../services/catalogo.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [
    CommonModule, FormsModule, TableModule, ButtonModule, DialogModule, 
    InputTextModule, InputNumberModule, DropdownModule, CheckboxModule,
    TagModule, ToastModule, ConfirmDialogModule, FileUploadModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './productos.component.html'
})
export class ProductosComponent implements OnInit {
  private catalogoService = inject(CatalogoService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private route = inject(ActivatedRoute);

  productos: Producto[] = [];
  categorias: Categoria[] = [];
  
  productoDialog: boolean = false;
  producto: Producto = this.getEmptyProducto();
  isEdit: boolean = false;
  loading: boolean = true;
  
  modoInsumos: boolean = false;

  ngOnInit() {
    this.route.url.subscribe(segments => {
      this.modoInsumos = segments.length > 0 && segments[0].path === 'insumos';
      this.loadData();
    });
  }

  loadData() {
    this.loading = true;
    this.catalogoService.getCategoriasActivas().subscribe(cats => this.categorias = cats);
    
    this.catalogoService.getProductos().subscribe({
      next: (data) => {
        if (this.modoInsumos) {
          this.productos = data.filter(p => p.usoInterno === true);
        } else {
          this.productos = data.filter(p => p.ventaDirecta === true);
        }
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  getEmptyProducto(): Producto {
    return {
      nombre: '',
      marca: '',
      sku: '',
      proveedor: '',
      imageUrl: '',
      costo: 0,
      precioVenta: 0,
      stockActual: 0,
      stockMinimo: 5,
      usoInterno: false,
      ventaDirecta: true,
      categoriaId: 0
    };
  }

  openNew() {
    this.producto = this.getEmptyProducto();
    this.isEdit = false;
    this.productoDialog = true;
  }

  editProducto(prod: Producto) {
    this.producto = { ...prod };
    this.isEdit = true;
    this.productoDialog = true;
  }

  hideDialog() {
    this.productoDialog = false;
  }

  saveProducto() {
    if (this.producto.nombre.trim() && this.producto.categoriaId) {
      if (this.isEdit && this.producto.id) {
        this.catalogoService.updateProducto(this.producto.id, this.producto).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto Actualizado' });
            this.loadData();
            this.hideDialog();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al actualizar' })
        });
      } else {
        this.catalogoService.createProducto(this.producto).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto Creado' });
            this.loadData();
            this.hideDialog();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al crear' })
        });
      }
    }
  }

  deleteProducto(prod: Producto) {
    this.confirmationService.confirm({
      message: '¿Estás seguro de que quieres eliminar el producto ' + prod.nombre + '?',
      header: 'Confirmar Eliminación',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        if(prod.id) {
          this.catalogoService.deleteProducto(prod.id).subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Producto Eliminado' });
              this.loadData();
            },
            error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar' })
          });
        }
      }
    });
  }

  onImageSelect(event: any) {
    const file = event.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.producto.imageUrl = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }
}

import { Component, OnInit, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule } from '@angular/forms';
import { ToastModule } from 'primeng/toast';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { MessageService } from 'primeng/api';
import { CatalogoService, Producto, Categoria } from '../services/catalogo.service';

import { DropdownModule } from 'primeng/dropdown';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, TableModule, TagModule, ButtonModule, InputNumberModule, InputTextModule, FormsModule, ToastModule, OverlayPanelModule, DropdownModule],
  providers: [MessageService],
  templateUrl: './inventario.component.html'
})
export class InventarioComponent implements OnInit {
  private catalogoService = inject(CatalogoService);
  private messageService = inject(MessageService);

  productos = signal<Producto[]>([]);
  categorias = signal<Categoria[]>([]);
  loading = signal<boolean>(true);
  
  // Filters
  searchQuery = signal<string>('');
  selectedCategory = signal<number | null | undefined>(null);
  isStockCriticoFilter = signal<boolean>(false);

  productosConStockBajo = computed(() => {
    return this.productos().filter(p => p.stockActual <= p.stockMinimo);
  });

  // Computed filtered list
  filteredProductos = computed(() => {
    let result = this.productos();
    
    if (this.isStockCriticoFilter()) {
      result = result.filter(p => p.stockActual <= p.stockMinimo);
    }
    
    if (this.selectedCategory() !== null) {
      result = result.filter(p => p.categoriaId === this.selectedCategory());
    }
    
    if (this.searchQuery().trim()) {
      const q = this.searchQuery().toLowerCase();
      result = result.filter(p => 
        p.nombre.toLowerCase().includes(q) || 
        (p.sku && p.sku.toLowerCase().includes(q))
      );
    }
    
    return result;
  });

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading.set(true);
    
    this.catalogoService.getCategoriasActivas().subscribe({
      next: (cats) => this.categorias.set(cats)
    });

    this.catalogoService.getProductos().subscribe({
      next: (data) => {
        this.productos.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  filterByCategory(catId: number | null | undefined) {
    this.selectedCategory.set(catId);
  }

  getCategoryCount(catId: number | null | undefined): number {
    if (catId === null) return this.productos().length;
    return this.productos().filter(p => p.categoriaId === catId).length;
  }

  filterStockCritico() {
    this.isStockCriticoFilter.set(true);
    this.selectedCategory.set(null);
    this.searchQuery.set('');
  }

  clearFilters() {
    this.isStockCriticoFilter.set(false);
    this.selectedCategory.set(null);
    this.searchQuery.set('');
  }

  actualizarStock(prod: Producto, nuevoStock: number, op?: any) {
    if (nuevoStock < 0) return;
    
    const updatedProd = { ...prod, stockActual: nuevoStock };
    
    if (updatedProd.id) {
      this.catalogoService.updateProducto(updatedProd.id, updatedProd).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Inventario Actualizado', detail: `${prod.nombre} ahora tiene ${nuevoStock} u.` });
          this.loadData(); // Reload to refresh everything smoothly
          if (op) op.hide();
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo actualizar el stock' })
      });
    }
  }

  hacerPedido(prod: Producto) {
    this.messageService.add({ severity: 'info', summary: 'Pedido Generado', detail: `Se ha notificado al proveedor ${prod.proveedor || 'General'} para ${prod.nombre}` });
  }
}

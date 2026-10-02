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
import { TreeTableModule } from 'primeng/treetable';
import { TreeSelectModule } from 'primeng/treeselect';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SidebarModule } from 'primeng/sidebar';
import { MessageService, ConfirmationService, TreeNode } from 'primeng/api';
import { CatalogoService, Categoria, Servicio } from '../services/catalogo.service';

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, DialogModule, InputTextModule, TagModule, ToastModule, ConfirmDialogModule, TreeSelectModule, TreeTableModule, RadioButtonModule, SidebarModule],
  providers: [MessageService, ConfirmationService],
  templateUrl: './categorias.component.html'
})
export class CategoriasComponent implements OnInit {
  private catalogoService = inject(CatalogoService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  categorias: Categoria[] = [];
  categoriasTree: TreeNode[] = [];
  categoriasTreeOpciones: TreeNode[] = [];
  categoriaPadreSeleccionada: TreeNode | null = null;
  categoriaDialog: boolean = false;
  categoria: Categoria = { nombre: '' };
  isEdit: boolean = false;
  isSubcategoria: boolean = false;
  subcategoriasNuevas: string[] = [];
  nuevaSubcategoriaTemp: string = '';
  loading: boolean = true;

  // Panel de servicios
  serviciosPanel: boolean = false;
  categoriaPanelSeleccionada: Categoria | null = null;
  serviciosDeCategoria: Servicio[] = [];
  loadingServicios: boolean = false;

  ngOnInit() {
    this.loadCategorias();
  }

  loadCategorias() {
    this.loading = true;
    this.catalogoService.getCategorias().subscribe({
      next: (data) => {
        data.sort((a, b) => {
          if (a.estado === b.estado) {
            return (a.id || 0) - (b.id || 0);
          }
          return a.estado ? -1 : 1;
        });
        this.categorias = data;
        this.buildTree();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  buildTree() {
    const map = new Map<number, TreeNode>();
    const roots: TreeNode[] = [];

    this.categorias.forEach(cat => {
      if(cat.id) {
        map.set(cat.id, { 
          key: cat.id.toString(),
          label: cat.nombre,
          data: cat, 
          children: [], 
          expanded: true 
        });
      }
    });

    this.categorias.forEach(cat => {
      if(cat.id) {
        const node = map.get(cat.id)!;
        if (cat.padreId && map.has(cat.padreId)) {
          map.get(cat.padreId)!.children!.push(node);
        } else {
          roots.push(node);
        }
      }
    });

    this.categoriasTree = roots;
  }

  openNew() {
    this.categoria = { nombre: '' };
    this.isEdit = false;
    this.isSubcategoria = false;
    this.subcategoriasNuevas = [];
    this.nuevaSubcategoriaTemp = '';
    this.categoriaPadreSeleccionada = null;
    this.categoriasTreeOpciones = this.cloneTreeAndDisable(this.categoriasTree, null);
    this.categoriaDialog = true;
  }

  editCategoria(cat: Categoria) {
    this.categoria = { ...cat };
    this.isEdit = true;
    this.isSubcategoria = !!cat.padreId;
    this.categoriasTreeOpciones = this.cloneTreeAndDisable(this.categoriasTree, cat.id!);
    this.categoriaPadreSeleccionada = cat.padreId ? this.findNodeByKey(this.categoriasTreeOpciones, cat.padreId.toString()) : null;
    this.categoriaDialog = true;
  }

  cloneTreeAndDisable(nodes: TreeNode[], disableId: number | null, disableAll: boolean = false): TreeNode[] {
    return nodes.map(node => {
      const isTarget = disableAll || node.data.id === disableId;
      return {
        ...node,
        selectable: !isTarget,
        children: node.children ? this.cloneTreeAndDisable(node.children, disableId, isTarget) : []
      };
    });
  }

  findNodeByKey(nodes: TreeNode[], key: string): TreeNode | null {
    for (let node of nodes) {
      if (node.key === key) return node;
      if (node.children) {
        let child = this.findNodeByKey(node.children, key);
        if (child) return child;
      }
    }
    return null;
  }

  addSubcategoria() {
    if (this.nuevaSubcategoriaTemp.trim()) {
      this.subcategoriasNuevas.push(this.nuevaSubcategoriaTemp.trim());
      this.nuevaSubcategoriaTemp = '';
    }
  }

  removeSubcategoria(index: number) {
    this.subcategoriasNuevas.splice(index, 1);
  }

  hideDialog() {
    this.categoriaDialog = false;
  }

  verServicios(cat: Categoria) {
    this.categoriaPanelSeleccionada = cat;
    this.serviciosDeCategoria = [];
    this.loadingServicios = true;
    this.serviciosPanel = true;

    // Filtramos desde todos los servicios ya cargados en el frontend
    this.catalogoService.getServicios().subscribe({
      next: (data) => {
        // Incluir servicios de esta categoría y de sus subcategorías
        const ids = this.getIdsFromCategory(cat);
        this.serviciosDeCategoria = data.filter(s => ids.includes(s.categoriaId));
        this.loadingServicios = false;
      },
      error: () => this.loadingServicios = false
    });
  }

  private getIdsFromCategory(cat: Categoria): number[] {
    const ids: number[] = cat.id ? [cat.id] : [];
    // Buscar subcategorías hijas
    this.categorias.filter(c => c.padreId === cat.id).forEach(child => {
      ids.push(...this.getIdsFromCategory(child));
    });
    return ids;
  }

  saveCategoria() {
    if (this.categoria.nombre.trim()) {
      if (this.isSubcategoria && !this.categoriaPadreSeleccionada) {
        this.messageService.add({ severity: 'warn', summary: 'Atención', detail: 'Debe seleccionar una categoría padre.' });
        return;
      }
      
      if (!this.isSubcategoria) {
        this.categoriaPadreSeleccionada = null;
      }
      
      this.categoria.padreId = this.categoriaPadreSeleccionada ? this.categoriaPadreSeleccionada.data.id : null;
      this.categoria.subcategorias = this.isSubcategoria ? [] : this.subcategoriasNuevas;

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

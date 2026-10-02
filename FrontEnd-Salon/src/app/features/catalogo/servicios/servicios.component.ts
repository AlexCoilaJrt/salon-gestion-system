import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MultiSelectModule } from 'primeng/multiselect';
import { TreeSelectModule } from 'primeng/treeselect';
import { MessageService, ConfirmationService, TreeNode } from 'primeng/api';
import { CatalogoService, Servicio, Categoria, Producto } from '../services/catalogo.service';
import { RrhhService, Especialidad } from '../../rrhh/services/rrhh.service';

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [
    CommonModule, FormsModule, TableModule, ButtonModule, DialogModule, 
    InputTextModule, InputNumberModule, DropdownModule, TagModule, 
    ToastModule, ConfirmDialogModule, MultiSelectModule, TreeSelectModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './servicios.component.html'
})
export class ServiciosComponent implements OnInit {
  private catalogoService = inject(CatalogoService);
  private rrhhService = inject(RrhhService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  servicios: Servicio[] = [];
  categorias: Categoria[] = [];
  categoriasTree: TreeNode[] = [];
  categoriaSeleccionada: TreeNode | null = null;
  especialidades: Especialidad[] = [];
  insumosDisponibles: Producto[] = [];
  
  servicioDialog: boolean = false;
  servicio: Servicio = this.getEmptyServicio();
  isEdit: boolean = false;
  submitted: boolean = false;
  loading: boolean = true;

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading = true;
    this.catalogoService.getCategoriasActivas().subscribe(cats => {
      this.categorias = cats;
      this.buildCategoriasTree();
    });
    this.rrhhService.getEspecialidades().subscribe(esps => this.especialidades = esps.filter(e => e.estado));
    this.catalogoService.getProductos().subscribe(prods => this.insumosDisponibles = prods.filter(p => p.estado && p.usoInterno));
    
    this.catalogoService.getServicios().subscribe({
      next: (data) => {
        data.sort((a, b) => {
          if (a.estado === b.estado) return (a.id || 0) - (b.id || 0);
          return a.estado ? -1 : 1;
        });
        this.servicios = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  buildCategoriasTree() {
    const map = new Map<number, TreeNode>();
    const roots: TreeNode[] = [];

    this.categorias.forEach(cat => {
      if (cat.id) {
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
      if (cat.id) {
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

  getEmptyServicio(): Servicio {
    return {
      nombre: '',
      descripcion: '',
      precioBase: 0,
      duracionMinutos: 30,
      comisionPorcentaje: 0,
      costoMaterial: 0,
      categoriaId: 0,
      especialidadRequeridaId: 0,
      insumosIds: []
    };
  }

  openNew() {
    this.servicio = this.getEmptyServicio();
    this.isEdit = false;
    this.submitted = false;
    this.categoriaSeleccionada = null;
    this.servicioDialog = true;
  }

  editServicio(serv: Servicio) {
    this.servicio = { 
      ...serv,
      insumosIds: serv.insumos?.map(i => i.id!) || []
    };
    this.isEdit = true;
    this.submitted = false;
    this.categoriaSeleccionada = serv.categoriaId ? this.findNodeByKey(this.categoriasTree, serv.categoriaId.toString()) : null;
    this.servicioDialog = true;
  }

  calcularCostoMaterial() {
    if (!this.servicio.insumosIds || this.servicio.insumosIds.length === 0) {
      this.servicio.costoMaterial = 0;
      return;
    }
    
    let totalCosto = 0;
    this.servicio.insumosIds.forEach(id => {
      const insumo = this.insumosDisponibles.find(i => i.id === id);
      if (insumo && insumo.costo) {
        // Asumimos que 'costo' viene como número.
        totalCosto += Number(insumo.costo);
      }
    });
    
    this.servicio.costoMaterial = totalCosto;
  }

  hideDialog() {
    this.servicioDialog = false;
  }

  onImageSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'La imagen debe ser menor a 2MB' });
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        this.servicio.imageUrl = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  saveServicio() {
    this.submitted = true;
    this.servicio.categoriaId = this.categoriaSeleccionada ? this.categoriaSeleccionada.data.id : 0;
    
    if (this.servicio.nombre.trim() && this.servicio.precioBase >= 0 && this.servicio.categoriaId && this.servicio.especialidadRequeridaId) {
      if (this.isEdit && this.servicio.id) {
        this.catalogoService.updateServicio(this.servicio.id, this.servicio).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Servicio Actualizado' });
            this.loadData();
            this.hideDialog();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al actualizar' })
        });
      } else {
        this.catalogoService.createServicio(this.servicio).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Servicio Creado' });
            this.loadData();
            this.hideDialog();
          },
          error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'Error al crear' })
        });
      }
    }
  }

  deleteServicio(serv: Servicio) {
    this.confirmationService.confirm({
      message: '¿Estás seguro de que quieres eliminar el servicio ' + serv.nombre + '?',
      header: 'Confirmar Eliminación',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        if(serv.id) {
          this.catalogoService.deleteServicio(serv.id).subscribe({
            next: () => {
              this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Servicio Eliminado' });
              this.loadData();
            },
            error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar' })
          });
        }
      }
    });
  }
}

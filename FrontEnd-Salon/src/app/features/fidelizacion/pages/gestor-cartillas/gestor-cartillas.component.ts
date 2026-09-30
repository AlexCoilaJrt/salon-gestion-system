import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FidelizacionService, CartillaDTO } from '../../services/fidelizacion.service';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TagModule } from 'primeng/tag';

// We need to fetch services from catalogo
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-gestor-cartillas',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, DialogModule, DropdownModule, InputNumberModule, InputTextModule, ToastModule, TagModule],
  providers: [MessageService],
  templateUrl: './gestor-cartillas.component.html'
})
export class GestorCartillasComponent implements OnInit {
  private fidelizacionService = inject(FidelizacionService);
  private http = inject(HttpClient);
  private messageService = inject(MessageService);

  cartillas: CartillaDTO[] = [];
  servicios: any[] = [];
  loading: boolean = false;
  
  displayModal: boolean = false;
  nuevaCartilla: any = {
    nombre: '',
    servicioId: null,
    metaSellos: 5,
    descuentoPremio: 100
  };

  ngOnInit() {
    this.loadCartillas();
    this.loadServicios();
  }

  loadCartillas() {
    this.loading = true;
    this.fidelizacionService.listarCartillas().subscribe({
      next: (data) => {
        this.cartillas = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  loadServicios() {
    this.http.get<any[]>('http://localhost:8080/api/catalogo/servicios').subscribe(res => {
      this.servicios = res.filter(s => s.estado);
    });
  }

  openNew() {
    this.nuevaCartilla = { nombre: '', servicioId: null, metaSellos: 5, descuentoPremio: 100 };
    this.displayModal = true;
  }

  guardarCartilla() {
    if (!this.nuevaCartilla.nombre || !this.nuevaCartilla.servicioId || !this.nuevaCartilla.metaSellos) {
      this.messageService.add({severity:'error', summary: 'Error', detail: 'Complete los campos obligatorios'});
      return;
    }

    this.fidelizacionService.crearCartilla(this.nuevaCartilla).subscribe({
      next: () => {
        this.messageService.add({severity:'success', summary: 'Éxito', detail: 'Cartilla creada correctamente'});
        this.displayModal = false;
        this.loadCartillas();
      },
      error: (err) => {
        this.messageService.add({severity:'error', summary: 'Error', detail: 'No se pudo crear la cartilla'});
      }
    });
  }
}

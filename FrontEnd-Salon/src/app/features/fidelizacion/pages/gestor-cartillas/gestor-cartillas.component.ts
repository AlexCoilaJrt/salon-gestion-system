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

import { EmpresaService, EmpresaResponse } from '../../../../core/services/empresa.service';

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
  private empresaService = inject(EmpresaService);

  cartillas: CartillaDTO[] = [];
  progresoClientes: any[] = [];
  servicios: any[] = [];
  loading: boolean = false;
  empresaData: EmpresaResponse | null = null;
  activeTab: 'plantillas' | 'progreso' = 'plantillas';
  
  displayModal: boolean = false;
  nuevaCartilla: any = {
    nombre: '',
    servicioId: null,
    metaSellos: 5,
    descuentoPremio: 100
  };

  aplicarATodos: boolean = false;

  get sellosArray() {
    return Array.from({ length: this.nuevaCartilla.metaSellos || 5 });
  }

  get servicioSeleccionadoNombre() {
    if (this.aplicarATodos) return 'Cualquier Servicio';
    const s = this.servicios.find(x => x.id === this.nuevaCartilla.servicioId);
    return s ? s.nombre : 'Servicio no seleccionado';
  }

  ngOnInit() {
    this.loadCartillas();
    this.loadProgreso();
    this.loadServicios();
    this.empresaService.empresa$.subscribe(e => this.empresaData = e);
    this.empresaService.getEmpresaActiva().subscribe();
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

  loadProgreso() {
    this.fidelizacionService.listarProgresoTodosLosClientes().subscribe(res => {
      this.progresoClientes = res;
    });
  }

  loadServicios() {
    this.http.get<any[]>('http://localhost:8080/api/v1/servicios').subscribe(res => {
      this.servicios = res.filter(s => s.estado);
    });
  }

  openNew() {
    this.nuevaCartilla = { nombre: '', servicioId: null, metaSellos: 5, descuentoPremio: 100 };
    this.aplicarATodos = true; // Default to true as the user wants
    this.displayModal = true;
  }

  guardarCartilla() {
    if (!this.nuevaCartilla.metaSellos || !this.nuevaCartilla.descuentoPremio) {
      this.messageService.add({severity:'error', summary: 'Error', detail: 'Complete la meta y el descuento'});
      return;
    }

    if (!this.aplicarATodos && !this.nuevaCartilla.servicioId) {
      this.messageService.add({severity:'error', summary: 'Error', detail: 'Seleccione un servicio'});
      return;
    }

    this.loading = true;
    this.displayModal = false;
    
    if (this.aplicarATodos) {
      // Loop through all services that don't already have a cartilla
      const existingIds = this.cartillas.map(c => c.servicioId);
      const toCreate = this.servicios.filter(s => !existingIds.includes(s.id));
      
      if (toCreate.length === 0) {
        this.messageService.add({severity:'info', summary: 'Info', detail: 'Todos los servicios ya tienen cartilla'});
        this.loading = false;
        return;
      }

      let count = 0;
      this.messageService.add({severity:'info', summary: 'Procesando', detail: `Creando ${toCreate.length} cartillas...`});
      
      toCreate.forEach(s => {
        const payload = {
          nombre: `Cartilla de ${s.nombre}`,
          servicioId: s.id,
          metaSellos: this.nuevaCartilla.metaSellos,
          descuentoPremio: this.nuevaCartilla.descuentoPremio
        };
        this.fidelizacionService.crearCartilla(payload).subscribe(() => {
          count++;
          if (count === toCreate.length) {
            this.messageService.add({severity:'success', summary: 'Éxito', detail: `Se crearon ${count} cartillas`});
            this.loadCartillas();
          }
        });
      });
    } else {
      const payload = {
        nombre: this.nuevaCartilla.nombre || 'Cartilla de Fidelización',
        servicioId: this.nuevaCartilla.servicioId,
        metaSellos: this.nuevaCartilla.metaSellos,
        descuentoPremio: this.nuevaCartilla.descuentoPremio
      };
      this.fidelizacionService.crearCartilla(payload).subscribe({
        next: () => {
          this.messageService.add({severity:'success', summary: 'Éxito', detail: 'Cartilla creada correctamente'});
          this.loadCartillas();
        },
        error: (err) => {
          this.messageService.add({severity:'error', summary: 'Error', detail: 'No se pudo crear la cartilla'});
          this.loading = false;
        }
      });
    }
  }

  imprimirPlantilla() {
    const printContent = document.getElementById('cartilla-preview');
    if (!printContent) return;
    const windowPrint = window.open('', '', 'left=0,top=0,width=800,height=600,toolbar=0,scrollbars=0,status=0');
    if (!windowPrint) return;

    windowPrint.document.write(`
      <html>
        <head>
          <title>Plantilla de Cartilla</title>
          <link href="https://cdn.jsdelivr.net/npm/primeicons@6.0.1/primeicons.css" rel="stylesheet">
          <style>
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
            body { 
              font-family: 'Inter', sans-serif; 
              margin: 0; 
              padding: 20px;
              display: flex;
              justify-content: center;
              align-items: center;
              height: 100vh;
              background-color: #fff;
            }
            .cartilla-container {
              width: 340px; /* Tamaño aproximado de tarjeta física/billetera */
              min-height: 200px;
              border-radius: 16px;
              border: 1px solid #e5e7eb;
              padding: 20px;
              box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
              background-color: #fff;
              position: relative;
              overflow: hidden;
            }
            .bg-deco {
              position: absolute;
              top: -30px;
              right: -30px;
              width: 100px;
              height: 100px;
              background-color: rgba(188, 160, 40, 0.1);
              border-radius: 50%;
              filter: blur(10px);
            }
            .header {
              display: flex;
              align-items: center;
              gap: 12px;
              border-bottom: 1px solid #f3f4f6;
              padding-bottom: 12px;
              margin-bottom: 16px;
            }
            .logo {
              width: 48px;
              height: 48px;
              border-radius: 12px;
              background-color: #f9fafb;
              display: flex;
              align-items: center;
              justify-content: center;
              border: 1px solid #e5e7eb;
              overflow: hidden;
            }
            .logo img {
              width: 100%;
              height: 100%;
              object-fit: cover;
            }
            .logo i {
              color: #bca028;
              font-size: 20px;
            }
            .title h5 {
              margin: 0;
              font-size: 14px;
              color: #1f2937;
            }
            .title span {
              display: block;
              font-size: 10px;
              color: #9ca3af;
              text-transform: uppercase;
              letter-spacing: 1px;
              margin-top: 2px;
            }
            .desc {
              font-size: 12px;
              color: #4b5563;
              text-align: center;
              margin-bottom: 20px;
              line-height: 1.5;
            }
            .desc strong {
              color: #1f2937;
            }
            .desc .highlight {
              color: #bca028;
              font-weight: 700;
            }
            .grid {
              display: flex;
              flex-wrap: wrap;
              gap: 8px;
              justify-content: center;
            }
            .sello {
              width: 40px;
              height: 40px;
              border-radius: 50%;
              border: 2px dashed #e5e7eb;
              display: flex;
              align-items: center;
              justify-content: center;
              background-color: #f9fafb;
              position: relative;
            }
            .sello span {
              font-size: 10px;
              font-weight: bold;
              color: #d1d5db;
            }
            .sello.premio {
              background-color: rgba(188, 160, 40, 0.1);
              border-style: solid;
              border-color: #bca028;
            }
            .sello.premio i {
              color: #bca028;
              font-size: 18px;
            }
            @media print {
              @page {
                size: A4;
                margin: 10mm;
              }
              body { 
                padding: 0; 
                margin: 0; 
                background-color: transparent; 
                height: auto;
                display: block;
                -webkit-print-color-adjust: exact; 
                print-color-adjust: exact; 
              }
              .page-grid {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 15px;
                justify-items: center;
              }
              .cartilla-container { 
                box-shadow: none; 
                border: 1px dashed #9ca3af;
                margin: 0;
                page-break-inside: avoid;
              }
            }
          </style>
        </head>
        <body>
          <div class="page-grid">
            ${Array(8).fill(0).map(() => `
            <div class="cartilla-container">
              <div class="bg-deco"></div>
              <div class="header">
                <div class="logo">
                  ${this.empresaData?.logoUrl ? '<img src="'+this.empresaData.logoUrl+'">' : '<i class="pi pi-sparkles"></i>'}
                </div>
                <div class="title">
                  <h5>${this.empresaData?.nombreComercial || 'Mi Salón VIP'}</h5>
                  <span>${this.nuevaCartilla.nombre || 'Nombre del Programa'}</span>
                </div>
              </div>
              <p class="desc">
                Completa <span class="highlight">${this.nuevaCartilla.metaSellos || 5} sellos</span> de 
                <strong>${this.servicioSeleccionadoNombre}</strong> y obtén 
                <span class="highlight">${this.nuevaCartilla.descuentoPremio || 100}%</span> de dscto.
              </p>
              <div class="grid">
                ${this.sellosArray.map((_, i) => 
                  i === this.sellosArray.length - 1 
                    ? '<div class="sello premio"><i class="pi pi-gift"></i></div>'
                    : '<div class="sello"><span>'+(i+1)+'</span></div>'
                ).join('')}
              </div>
            </div>
            `).join('')}
          </div>
        </body>
      </html>
    `);
    windowPrint.document.close();
    windowPrint.focus();
    // Use timeout to allow fonts/icons to load before printing
    setTimeout(() => {
      windowPrint.print();
      windowPrint.close();
    }, 500);
  }
}

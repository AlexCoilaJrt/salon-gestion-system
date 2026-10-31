import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { DialogModule } from 'primeng/dialog';
import { CheckboxModule } from 'primeng/checkbox';
import { InputNumberModule } from 'primeng/inputnumber';
import { MultiSelectModule } from 'primeng/multiselect';
import { CatalogoService, Servicio } from '../../../catalogo/services/catalogo.service';
import { TicketService } from '../../../finanzas/services/ticket.service';
import { AgendaService, ClienteResponse } from '../agenda/agenda.service';
import { EmpresaService, EmpresaResponse } from '../../../../core/services/empresa.service';

export interface CartillaItem {
  servicioId: number;
  nombre: string;
  precio: number;
  precioOriginal?: number;
  seleccionado: boolean;
  cantidad: number;
  personalizado?: boolean;
  orden?: number;
}

export interface SubCategoria {
  id: number;
  nombre: string;
  items: CartillaItem[];
}

export interface Cartilla {
  categoriaId: number;
  nombre: string;
  items: CartillaItem[];
  subCategorias: SubCategoria[];
  tieneSubCategorias: boolean;
}

@Component({
  selector: 'app-generar-orden',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastModule, ButtonModule,
    InputTextModule, DropdownModule, DialogModule, CheckboxModule, InputNumberModule, MultiSelectModule],
  providers: [MessageService],
  templateUrl: './generar-orden.component.html',
  styleUrl: './generar-orden.component.scss'
})
export class GenerarOrdenComponent implements OnInit {
  private catalogoService = inject(CatalogoService);
  private ticketService   = inject(TicketService);
  private agendaService   = inject(AgendaService);
  private messageService  = inject(MessageService);
  private empresaService  = inject(EmpresaService);

  // --- Data ---
  clientes  = signal<ClienteResponse[]>([]);
  cartillas = signal<Cartilla[]>([]);
  empresa   = signal<EmpresaResponse | null>(null);

  // --- Config Panel ---
  cartillaSeleccionada = signal<Cartilla | null>(null);
  selectedCliente = signal<ClienteResponse | null>(null);
  nombreClienteManual = signal('');
  mesaSillon = signal('');
  numCopias = signal(1);
  mostrarPrecios = signal(true);
  incluirFilasExtra = signal(true);
  numFilasExtra = signal(2);
  mostrarFirmas = signal(false);
  mostrarCodigoBarras = signal(false);

  isProcessing = signal(false);
  ticketGenerado = signal<any>(null);

  // --- Dynamic Code ---
  codigoCartilla = signal(this.generarCodigoAleatorio());
  zoomLevel = signal<number>(1.08);

  cambiarZoom(delta: number): void {
    const nuevo = Math.min(Math.max(+(this.zoomLevel() + delta).toFixed(2), 0.8), 1.5);
    this.zoomLevel.set(nuevo);
  }

  resetZoom(): void {
    this.zoomLevel.set(1.08);
  }

  // --- Computed ---
  readonly Math = Math;

  readonly fechaPeru = (() => {
    const now = new Date();
    const peru = new Date(now.getTime() + (-5) * 3600000);
    return {
      dia:  String(peru.getUTCDate()).padStart(2, '0'),
      mes:  String(peru.getUTCMonth() + 1).padStart(2, '0'),
      anio: peru.getUTCFullYear()
    };
  })();

  cartillasOptions = computed(() =>
    this.cartillas().map(c => ({ label: c.nombre, value: c }))
  );

  todosLosItems = computed(() => {
    const c = this.cartillaSeleccionada();
    if (!c) return [];
    return c.tieneSubCategorias
      ? c.subCategorias.flatMap(s => s.items)
      : c.items;
  });

  seleccionados = signal<CartillaItem[]>([]);

  subtotal = computed(() => this.seleccionados().reduce((acc, item) => acc + (item.precio * item.cantidad), 0));

  clienteDisplay = computed(() => {
    const c = this.selectedCliente();
    return c ? c.nombreCompleto : (this.nombreClienteManual() || '');
  });

  itemsAtendidos = computed(() => {
    const c = this.cartillaSeleccionada();
    if (!c) return [];
    const seleccionados = c.items.filter(i => i.seleccionado);
    return seleccionados.length > 0 ? seleccionados : c.items;
  });

  subCategoriasAtendidas = computed(() => {
    const c = this.cartillaSeleccionada();
    if (!c || !c.tieneSubCategorias) return [];
    const subsConAtendidos = c.subCategorias
      .map(sub => ({
        ...sub,
        items: sub.items.filter(i => i.seleccionado)
      }))
      .filter(sub => sub.items.length > 0);
    return subsConAtendidos.length > 0 ? subsConAtendidos : c.subCategorias;
  });

  ngOnInit(): void {
    this.agendaService.obtenerClientes().subscribe(res => this.clientes.set(res));
    this.cargarCartillas();

    // Cargar datos de empresa
    this.empresaService.getEmpresaActiva().subscribe({
      next: (res: any) => {
        // El servicio puede devolver el objeto directo o envuelto en {data: ...}
        const data = res?.data ?? res;
        this.empresa.set(data);
        // Actualizar el título de la página para que al imprimir no salga "SalonFrontend"
        document.title = data?.nombreComercial ?? 'Cartilla';
      },
      error: () => {} // Silencioso: si falla, se usan valores por defecto del HTML
    });
  }

  cargarCartillas(): void {
    forkJoin({
      categorias: this.catalogoService.getCategorias(),
      servicios: this.catalogoService.getServicios()
    }).subscribe(({ categorias, servicios }) => {
      const activos = servicios.filter(s => s.estado !== false);
      activos.sort((a, b) => {
        const ordenA = a.orden ?? 0;
        const ordenB = b.orden ?? 0;
        if (ordenA !== ordenB) return ordenA - ordenB;
        return (a.id ?? 0) - (b.id ?? 0);
      });
      const padres = categorias.filter(c => !c.padreId && c.estado !== false).sort((a, b) => (a.id ?? 0) - (b.id ?? 0));
      const hijos  = categorias.filter(c => !!c.padreId && c.estado !== false).sort((a, b) => (a.id ?? 0) - (b.id ?? 0));

      const lista: Cartilla[] = padres.map(padre => {
        const misHijos = hijos.filter(h => h.padreId === padre.id);
        const subCats: SubCategoria[] = misHijos
          .map(h => ({
            id: h.id!, nombre: h.nombre,
            items: activos.filter(s => s.categoriaId === h.id).map(s => this.toItem(s))
          }))
          .filter(s => s.items.length > 0);

        return {
          categoriaId: padre.id!,
          nombre: padre.nombre,
          items: activos.filter(s => s.categoriaId === padre.id).map(s => this.toItem(s)),
          subCategorias: subCats,
          tieneSubCategorias: subCats.length > 0
        };
      }).filter(c => c.items.length > 0 || c.subCategorias.length > 0);

      this.cartillas.set(lista);
      if (lista.length > 0) {
        this.cartillaSeleccionada.set(lista[0]);
        this.codigoCartilla.set(this.generarCodigoAleatorio());
      }
    });
  }

  generarCodigoAleatorio(): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code = '';
    for (let i = 0; i < 6; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
  }

  private toItem(s: Servicio): CartillaItem {
    return {
      servicioId: s.id!,
      nombre: s.nombre,
      precio: s.precioBase,
      precioOriginal: s.precioBase,
      seleccionado: false,
      cantidad: 1,
      personalizado: false,
      orden: s.orden ?? 0
    };
  }

  toggleItem(item: CartillaItem): void {
    item.seleccionado = !item.seleccionado;
    const allSelected = this.todosLosItems().filter(i => i.seleccionado);
    this.seleccionados.set(allSelected);
  }

  onCartillaChange(c: Cartilla): void {
    this.cartillaSeleccionada.set(c);
    this.codigoCartilla.set(this.generarCodigoAleatorio());
    this.seleccionados.set([]);
    
    // Reset all items to false
    const all = c.tieneSubCategorias ? c.subCategorias.flatMap(s => s.items) : c.items;
    all.forEach(i => {
      i.seleccionado = false;
      i.cantidad = 1;
      i.personalizado = false;
      if (i.precioOriginal !== undefined) i.precio = i.precioOriginal;
    });
  }

  onServiciosChange(selected: CartillaItem[]): void {
    const all = this.todosLosItems();
    all.forEach(item => item.seleccionado = false);
    if (selected) {
      selected.forEach(item => {
        const match = all.find(x => x.servicioId === item.servicioId);
        if (match) {
          match.seleccionado = true;
          if (!match.cantidad) match.cantidad = 1;
          if (match.precioOriginal === undefined) match.precioOriginal = match.precio;
        }
      });
    }
    this.seleccionados.set(selected || []);
  }

  togglePersonalizar(item: CartillaItem): void {
    item.personalizado = !item.personalizado;
    if (!item.personalizado) {
      item.cantidad = 1;
      if (item.precioOriginal !== undefined) {
        item.precio = item.precioOriginal;
      }
    }
    this.seleccionados.set([...this.seleccionados()]);
  }

  incrementarCantidad(item: CartillaItem): void {
    item.cantidad = (item.cantidad || 1) + 1;
    this.seleccionados.set([...this.seleccionados()]);
  }

  decrementarCantidad(item: CartillaItem): void {
    if ((item.cantidad || 1) > 1) {
      item.cantidad = item.cantidad - 1;
      this.seleccionados.set([...this.seleccionados()]);
    }
  }

  actualizarPrecio(item: CartillaItem, nuevoPrecio: number): void {
    item.precio = nuevoPrecio !== null && nuevoPrecio !== undefined ? nuevoPrecio : 0;
    this.seleccionados.set([...this.seleccionados()]);
  }

  // --- Imprimir cartilla individual en ventana nueva ---
  imprimirCartilla(): void {
    const cartilla = this.cartillaSeleccionada();
    if (!cartilla) return;
    const fecha = this.fechaPeru;
    const cliente = this.clienteDisplay();
    const mesa = this.mesaSillon();
    const mostrarP = this.mostrarPrecios();
    const filasExtra = this.incluirFilasExtra() ? this.numFilasExtra() : 0;

    const buildRows = (items: CartillaItem[]) => {
      const atendidos = items.filter(i => i.seleccionado);
      const itemsAMostrar = atendidos.length > 0 ? atendidos : items;
      return itemsAMostrar.map((item, i) => {
        const qtyLabel = item.cantidad > 1 ? ` <span style="font-weight:700; color:#555">(x${item.cantidad})</span>` : '';
        const priceVal = mostrarP ? 'S/ ' + (item.precio * item.cantidad).toFixed(2) : '';
        return `<tr>
          <td class="tc">${i + 1}</td>
          <td>${item.nombre}${qtyLabel}</td>
          <td class="tc">${priceVal}</td>
          <td class="firma-col"></td>
        </tr>`;
      }).join('') +
      Array.from({ length: Math.max(0, filasExtra) }).map(() =>
        `<tr><td class="tc"></td><td class="italic text-gray">Adicional...</td><td class="tc"></td><td class="firma-col"></td></tr>`
      ).join('');
    };

    const buildSubTables = () => {
      const subsConAtendidos = cartilla.subCategorias
        .map(sub => ({
          ...sub,
          items: sub.items.filter(i => i.seleccionado)
        }))
        .filter(sub => sub.items.length > 0);
      const subsAMostrar = subsConAtendidos.length > 0 ? subsConAtendidos : cartilla.subCategorias;

      return subsAMostrar.map(sub =>
        `<p class="sub-lbl">${sub.nombre}</p>
         <table>
           <thead><tr>
             <th class="tc" style="width:28px">N°</th>
             <th>Servicios</th>
             <th class="tc" style="width:52px">P.U</th>
             <th style="width:110px">${cartilla.nombre.toUpperCase()}</th>
           </tr></thead>
           <tbody>${buildRows(sub.items)}</tbody>
         </table>`
      ).join('');
    };

    const html = `<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
<title>${cartilla.nombre}</title>
<style>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;900&display=swap');
@page { size: A5; margin: 10mm 10mm 10mm 10mm; }
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'Plus Jakarta Sans', Arial, sans-serif; color: #111; background: #fff; font-size: 8.5pt; }

.header { display: flex; justify-content: space-between; align-items: flex-start; padding-bottom: 4mm; }
.header-left {}
.header-right { display: flex; flex-direction: column; align-items: flex-end; gap: 3mm; }
.salon-name { font-size: 22pt; font-weight: 900; letter-spacing: 2px; text-transform: uppercase; line-height: 1; }
.salon-sub  { font-size: 6.5pt; color: #555; margin-top: 1mm; font-weight: 600; letter-spacing: 0.5px; }
.salon-addr { font-size: 6.5pt; color: #333; margin-top: 1mm; }

.header-rule { border: none; border-top: 1.5px solid #111; margin: 0 0 3mm 0; }

.orden-block { text-align: center; border: 1.5px solid #111; padding: 1.5mm 3mm; min-width: 24mm; }
.orden-num   { font-size: 14pt; font-weight: 900; line-height: 1.2; }

.fecha-wrapper { text-align: center; }
.fecha-lbl { font-size: 5.5pt; font-weight: 800; letter-spacing: 1px; margin-bottom: 1mm; }
.fecha-boxes { display: flex; gap: 1.5mm; padding: 1.5mm; background: #111; }
.fbox { width: 11mm; height: 10mm; background: #fff; border: 1.5px solid #111;
        display: flex; flex-direction: column; align-items: center; justify-content: center; }
.fbox-lbl { font-size: 4pt; font-weight: 900; line-height: 1; }
.fbox-val { font-size: 9pt; font-weight: 900; line-height: 1.2; }

.badge { background: #111; color: #fff; font-size: 6pt; font-weight: 800; letter-spacing: 1.5px;
         padding: 1mm 3mm; display: inline-block; margin-bottom: 2mm; }
.fields { display: flex; gap: 3mm; margin-bottom: 2mm; font-size: 7.5pt; }
.field  { flex: 1; border-bottom: 1px solid #555; padding-bottom: 1mm; }
.field-lbl { font-size: 6pt; font-weight: 800; letter-spacing: 0.5px; color: #555; margin-bottom: 0.5mm; }

table { width: 100%; border-collapse: collapse; margin-bottom: 2mm; }
thead tr { background: #111; color: #fff; }
th, td { border: 0.8px solid #aaa; padding: 1.5mm 2mm; font-size: 7.5pt; }
thead th { font-size: 7pt; font-weight: 800; letter-spacing: 0.5px; }
th:first-child, .tc { text-align: center; }
.firma-col { width: 110px; }
tr:nth-child(even) td { background: #f9f9f9; }
.italic { font-style: italic; }
.text-gray { color: #888; }

.sub-lbl { font-size: 7pt; font-weight: 800; background: #e5e5e5; padding: 1mm 2mm;
           text-transform: uppercase; letter-spacing: 0.8px; margin: 2mm 0 0; }

.footer-grid { display: flex; gap: 3mm; margin-top: 3mm; }
.obs-box { flex: 2; border: 0.8px solid #aaa; padding: 2mm; min-height: 14mm; font-size: 6.5pt; }
.obs-lbl { font-size: 6pt; font-weight: 800; color: #555; margin-bottom: 1mm; }
.totals-box { flex: 1; border: 1.5px solid #111; }
.total-row { display: flex; justify-content: space-between; padding: 1mm 2mm;
             border-bottom: 0.5px solid #ccc; font-size: 7pt; }
.total-row.main { font-weight: 900; font-size: 8pt; background: #111; color: #fff; }
.total-row .val { min-width: 22mm; border-bottom: 1px solid #555; text-align: right; padding-right: 1mm; }

.sigs { display: flex; gap: 5mm; margin-top: 5mm; padding-top: 3mm; border-top: 1px dashed #aaa; }
.sig  { flex: 1; text-align: center; }
.sig-line { border-top: 1px solid #333; margin-bottom: 1mm; }
.sig-lbl  { font-size: 5.5pt; font-weight: 700; color: #555; letter-spacing: 0.5px; }
.sig-sub  { font-size: 5pt; color: #888; font-style: italic; }

.barcode-area { margin-top: 4mm; display: flex; justify-content: space-between; align-items: flex-end; }
.barcode-lines { letter-spacing: -1px; font-size: 18pt; font-family: 'Libre Barcode 128', monospace; }
.gracias { text-align: right; font-size: 7pt; font-style: italic; font-weight: 700; color: #333; }
.disclaimer { font-size: 5.5pt; color: #777; margin-top: 1mm; font-style: italic; }
</style>
</head><body>

<!-- CABECERA -->
<div class="header">
  <div class="header-left">
    <div class="salon-name">SPA DIANA</div>
    <div class="salon-sub">EST\u00c9TICA &amp; NAIL ATELIER PROFESIONAL</div>
    <div class="salon-addr">\u2b55 Jr. Moquegua 656 &nbsp;|&nbsp; Cel: 987 654 321</div>
  </div>
  <div class="header-right">
    ${this.ticketGenerado() ? `
    <div class="orden-block">
      <div class="fecha-lbl">N\u00b0 ORDEN</div>
      <div class="orden-num">#${this.ticketGenerado()?.id}</div>
    </div>` : ''}
    <div class="fecha-wrapper">
      <div class="fecha-lbl">FECHA</div>
      <div class="fecha-boxes">
        <div class="fbox"><span class="fbox-lbl">D\u00cdA</span><span class="fbox-val">${fecha.dia}</span></div>
        <div class="fbox"><span class="fbox-lbl">MES</span><span class="fbox-val">${fecha.mes}</span></div>
        <div class="fbox"><span class="fbox-lbl">A\u00d1O</span><span class="fbox-val">${fecha.anio}</span></div>
      </div>
    </div>
  </div>
</div>
<hr class="header-rule">

<div class="badge">HOJA DE RUTA &amp; CONTROL DE SERVICIOS · ${cartilla.nombre.toUpperCase()}</div>

<!-- CAMPOS CLIENTA / CATEGORÍA Y MESA -->
<div class="fields" style="align-items: flex-end;">
  <div class="field" style="flex: 1.2;"><div class="field-lbl">CLIENTA:</div>${cliente}</div>
  <div style="flex: 1; display: flex; flex-direction: column; gap: 2mm;">
    <div class="field" style="border-bottom: 1px solid #555;"><div class="field-lbl">CATEGORÍA:</div><strong style="text-transform: uppercase;">${cartilla.nombre}</strong></div>
    <div class="field" style="border-bottom: 1px solid #555;"><div class="field-lbl">MESA / SILLÓN:</div>${mesa}</div>
  </div>
</div>

<!-- TABLA SERVICIOS -->
${cartilla.tieneSubCategorias ? buildSubTables() :
  `<table>
    <thead><tr>
      <th class="tc" style="width:28px">N°</th>
      <th>SERVICIOS</th>
      <th class="tc" style="width:52px">P.U</th>
      <th style="width:110px">${cartilla.nombre.toUpperCase()}</th>
    </tr></thead>
    <tbody>${buildRows(cartilla.items)}</tbody>
  </table>`}

<!-- FOOTER OBSERVACIONES + TOTALES -->
<div class="footer-grid">
  <div class="obs-box">
    <div class="obs-lbl">OBSERVACIONES / DETALLES DE CORTE:</div>
    <br><br>
    <div style="font-size:5.5pt;color:#999;font-style:italic;margin-top:4mm">
      * Válido únicamente con sello de caja y firma de conformidad de la clienta.
    </div>
  </div>
  <div class="totals-box">
    <div class="total-row"><span>SUBTOTAL:</span><span class="val">S/ ${this.subtotal().toFixed(2)}</span></div>
    <div class="total-row"><span>DSCTO / CUPÓN:</span><span class="val">S/ 0.00</span></div>
    <div class="total-row main"><span>TOTAL A PAGAR:</span><span class="val">S/ ${this.subtotal().toFixed(2)}</span></div>
  </div>
</div>`;

    const mostrarFirm = this.mostrarFirmas();
    const mostrarCod  = this.mostrarCodigoBarras();

    const sigsHtml = mostrarFirm ? `
<!-- FIRMAS -->
<div class="sigs">
  <div class="sig"><div class="sig-line"></div><div class="sig-lbl">FIRMA DE LA CLIENTA</div><div class="sig-sub">(Conformidad de Trabajo Realizado)</div></div>
  <div class="sig"><div class="sig-line"></div><div class="sig-lbl">SELLO Y FIRMA DE RECEPCIÓN</div><div class="sig-sub">(Caja / Cierre Turno Tarde)</div></div>
</div>` : '';

    const barcodeHtml = mostrarCod ? `
<!-- CÓDIGO DE BARRAS -->
<div class="barcode-area">
  <div>
    <div class="barcode-lines">▐▌█▐▌▐█▌▐▐█▌▐▌█▐</div>
    <div style="font-size:6pt;letter-spacing:2px;font-weight:700">*CART-${this.codigoCartilla()}*</div>
  </div>
  <div>
    <div class="gracias">¡Gracias por su preferencia!</div>
    <div style="font-size:5.5pt;color:#888;text-align:right">Diana Morales · Spa &amp; Salón</div>
  </div>
</div>` : '';

    const copies = this.numCopias();

    // Ensamblar el HTML completo con secciones condicionales
    const fullHtml = html + sigsHtml + barcodeHtml + '\n</body></html>';

    // Si son más de 1 copia, agregar salto de página entre copias
    const body = fullHtml.split('<body>')[1]?.split('</body>')[0] ?? '';
    const finalHtml = copies > 1
      ? fullHtml.replace('</body>', `<div style="page-break-before:always"></div>${body}</body>`)
      : fullHtml;

    // Usar window.print(): el @media print del SCSS oculta todo excepto .paper-a5
    // para imprimir exactamente el mismo preview que se ve en pantalla.
    window.print();
  }

  // --- Generar Orden (enviar a caja) ---
  generarOrden(): void {
    const sel = this.seleccionados();
    if (sel.length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Sin servicios', detail: 'Selecciona al menos un servicio.' });
      return;
    }
    this.isProcessing.set(true);
    const request = {
      clienteId: this.selectedCliente()?.id ?? null,
      nombreClienteNoRegistrado: this.selectedCliente() ? null : (this.nombreClienteManual() || 'Público General'),
      detalles: sel.map(s => ({
        servicioId: s.servicioId,
        cantidad: s.cantidad || 1,
        precioUnitario: s.precio
      }))
    };
    this.ticketService.generarOrden(request).subscribe({
      next: res => {
        this.messageService.add({ severity: 'success', summary: '¡Orden Generada!', detail: `Ticket #${res.id} listo para caja.`, life: 5000 });
        this.ticketGenerado.set(res);
        this.isProcessing.set(false);
      },
      error: err => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.message || 'No se pudo generar la orden.' });
        this.isProcessing.set(false);
      }
    });
  }

  cerrarModal(): void { this.ticketGenerado.set(null); }
  imprimirTicket(): void { this.imprimirCartilla(); }

}

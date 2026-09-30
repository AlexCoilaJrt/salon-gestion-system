import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { IncentivoService, LiquidacionResponse } from '../services/incentivo.service';

@Component({
  selector: 'app-liquidaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, CalendarModule],
  templateUrl: './liquidaciones.component.html'
})
export class LiquidacionesComponent implements OnInit {
  liquidaciones: LiquidacionResponse[] = [];
  loading = false;
  rangoFechas: Date[] | undefined;

  constructor(private incentivoService: IncentivoService) {}

  ngOnInit(): void {
    // Default to current month
    const now = new Date();
    this.rangoFechas = [new Date(now.getFullYear(), now.getMonth(), 1), new Date(now.getFullYear(), now.getMonth() + 1, 0)];
    this.generarReporte();
  }

  generarReporte(): void {
    if (!this.rangoFechas || !this.rangoFechas[0] || !this.rangoFechas[1]) {
      return;
    }

    this.loading = true;
    const inicioStr = this.formatDate(this.rangoFechas[0]);
    const finStr = this.formatDate(this.rangoFechas[1]);

    this.incentivoService.getLiquidaciones(inicioStr, finStr).subscribe({
      next: (data) => {
        this.liquidaciones = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  exportarPDF(): void {
    window.print();
  }

  private formatDate(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}

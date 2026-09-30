import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChartModule } from 'primeng/chart';
import { DropdownModule } from 'primeng/dropdown';
import Chart from 'chart.js/auto';
import { DashboardService, MargenNetoResponse, RankingEspecialistaDTO, ServicioDemandaDTO } from '../../services/dashboard.service';

@Component({
  selector: 'app-reportes-rendimiento',
  standalone: true,
  imports: [CommonModule, FormsModule, ChartModule, DropdownModule],
  templateUrl: './reportes-rendimiento.component.html',
  styleUrls: ['./reportes-rendimiento.component.scss']
})
export class ReportesRendimientoComponent implements OnInit {

  // Data para gráficos
  revenueChartData: any;
  revenueChartOptions: any;
  
  servicesChartData: any;
  servicesChartOptions: any;

  // Filtros
  periodos = [
    { label: 'Este Mes', value: 'month' }
  ];
  selectedPeriodo = 'month';

  // Ranking data
  rankingProfesionales: any[] = [];
  
  // KPIs
  kpiIngresos: number = 0;
  kpiIngresosServicios: number = 0;
  kpiIngresosProductos: number = 0;
  kpiMargenNeto: number = 0;
  kpiCitas: number = 0;
  kpiTicketPromedio: number = 0;
  kpiComisiones: number = 0;
  kpiComisionesPorcentaje: number = 0;

  private dashboardService = inject(DashboardService);

  ngOnInit() {
    this.initCharts();
    this.loadData();
  }

  loadData() {
    // 1. Obtener Margen Neto (KPIs)
    this.dashboardService.getMargenNeto().subscribe(res => {
      this.kpiIngresos = res.ingresosTotales;
      this.kpiIngresosServicios = res.ingresosServicios;
      this.kpiIngresosProductos = res.ingresosProductos;
      this.kpiMargenNeto = res.gananciaNeta;
      this.kpiComisiones = res.pagoComisiones;
      
      // 2. Obtener Ranking para sumar citas (o si tenemos otro endpoint)
      this.dashboardService.getRankingEspecialistas().subscribe(ranking => {
        this.rankingProfesionales = ranking.map(r => ({
          nombre: r.nombreCompleto,
          citas: r.cantidadServicios,
          comisiones: r.totalVendido, // Temporal, mapeando a total vendido o comisiones
          avatar: r.nombreCompleto.charAt(0).toUpperCase()
        }));
        
        // Sumar citas de todos los profesionales
        this.kpiCitas = ranking.reduce((acc, curr) => acc + curr.cantidadServicios, 0);
        
        // Calcular Ticket Promedio
        this.kpiTicketPromedio = this.kpiCitas > 0 ? (this.kpiIngresos / this.kpiCitas) : 0;
        
        // Calcular Porcentaje de Comisiones
        this.kpiComisionesPorcentaje = this.kpiIngresos > 0 ? (this.kpiComisiones / this.kpiIngresos) * 100 : 0;
      });
    });

    // 3. Obtener Servicios Más Demandados (Doughnut Chart)
    this.dashboardService.getServiciosDemandados().subscribe(servicios => {
      this.updateServicesChart(servicios);
    });
  }

  initCharts() {
    const isDark = document.documentElement.classList.contains('app-dark') || document.documentElement.classList.contains('dark');
    const textColor = isDark ? '#ffffff' : '#333333';
    const textColorSecondary = isDark ? '#cccccc' : '#666666';
    const surfaceBorder = isDark ? '#3f3f46' : '#e5e7eb';

    // --- LINE CHART (Ingresos y Citas) ---
    this.revenueChartData = {
      labels: ['Semana 1', 'Semana 2', 'Semana 3', 'Semana 4'],
      datasets: [
        {
          label: 'Ingresos Totales (S/)',
          data: [3500, 4200, 3800, 5100],
          fill: false,
          borderColor: '#bca028',
          tension: 0.4
        },
        {
          label: 'Citas Atendidas',
          data: [120, 150, 140, 180],
          fill: false,
          borderColor: '#3b82f6',
          tension: 0.4
        }
      ]
    };

    this.revenueChartOptions = {
      plugins: {
        legend: { labels: { color: textColor } }
      },
      scales: {
        x: {
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder, drawBorder: false }
        },
        y: {
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder, drawBorder: false }
        }
      }
    };

    // --- DOUGHNUT CHART (Servicios Más Vendidos) ---
    this.servicesChartData = {
      labels: [],
      datasets: []
    };

    this.servicesChartOptions = {
      plugins: {
        legend: {
          labels: { color: textColor }
        }
      }
    };
  }

  updateServicesChart(servicios: ServicioDemandaDTO[]) {
    const isDark = document.documentElement.classList.contains('app-dark') || document.documentElement.classList.contains('dark');
    const textColor = isDark ? '#ffffff' : '#333333';
    
    this.servicesChartOptions = {
      plugins: {
        legend: { labels: { color: textColor } }
      }
    };

    this.servicesChartData = {
      labels: servicios.map(s => s.servicioNombre),
      datasets: [
        {
          data: servicios.map(s => s.cantidadVendida),
          backgroundColor: [
            '#bca028',
            '#e5c158',
            '#fceabb',
            '#3b82f6',
            '#8b5cf6',
            '#ec4899'
          ]
        }
      ]
    };
  }

  onPeriodChange() {
    this.loadData();
  }
}

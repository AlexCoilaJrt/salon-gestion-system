import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { IncentivoService, MonitorComision } from '../services/incentivo.service';

@Component({
  selector: 'app-monitor-comisiones',
  standalone: true,
  imports: [CommonModule, TableModule, TagModule, ButtonModule],
  templateUrl: './monitor-comisiones.component.html'
})
export class MonitorComisionesComponent implements OnInit {
  monitores: MonitorComision[] = [];
  loading = false;

  constructor(private incentivoService: IncentivoService) {}

  ngOnInit(): void {
    this.loadMonitor();
  }

  loadMonitor(): void {
    this.loading = true;
    this.incentivoService.getMonitorComisiones().subscribe({
      next: (data) => {
        this.monitores = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
}

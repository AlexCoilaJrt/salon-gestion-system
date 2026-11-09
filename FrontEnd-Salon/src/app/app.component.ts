import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { PrimeNG } from 'primeng/config';
import { esTranslation } from './app.config';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ButtonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  title = 'salon-frontend';

  constructor(private primeng: PrimeNG) {}

  ngOnInit(): void {
    this.primeng.setTranslation(esTranslation);
  }
}

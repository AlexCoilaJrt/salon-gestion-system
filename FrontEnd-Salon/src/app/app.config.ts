import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/interceptors/auth.interceptor';

import { definePreset } from '@primeng/themes';

const MyPreset = definePreset(Aura, {
    semantic: {
        primary: {
            50: '#fcf8f9',
            100: '#f7edef',
            200: '#edd8dd',
            300: '#dfbcc5',
            400: '#cc97a6',
            500: '#984b5d',
            600: '#7a3c4a',
            700: '#632e3a',
            800: '#522831',
            900: '#46242b',
            950: '#251015'
        }
    }
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }), 
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimations(),
    providePrimeNG({
        theme: {
            preset: MyPreset,
            options: {
                darkModeSelector: '.app-dark'
            }
        }
    })
  ]
};

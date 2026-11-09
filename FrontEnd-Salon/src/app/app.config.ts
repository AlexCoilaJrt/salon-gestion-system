import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import { Translation } from 'primeng/api';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/interceptors/auth.interceptor';

import { definePreset } from '@primeng/themes';

const MyPreset = definePreset(Aura, {
    semantic: {
        primary: {
            50: '#fdfbf6',
            100: '#f9f6ea',
            200: '#f0e6c7',
            300: '#e6d3a0',
            400: '#d9bd72',
            500: '#bca028',
            600: '#9d841f',
            700: '#7c6717',
            800: '#5c4c11',
            900: '#3c320a',
            950: '#2b2306'
        }
    }
});

export const esTranslation: Translation = {
    dayNames: ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'],
    dayNamesShort: ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'],
    dayNamesMin: ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá'],
    monthNames: [
        'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ],
    monthNamesShort: [
        'Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
        'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'
    ],
    today: 'Hoy',
    clear: 'Limpiar',
    dateFormat: 'dd/mm/yy',
    firstDayOfWeek: 1,
    chooseYear: 'Elegir Año',
    chooseMonth: 'Elegir Mes',
    chooseDate: 'Elegir Fecha',
    prevDecade: 'Década anterior',
    nextDecade: 'Década siguiente',
    prevYear: 'Año anterior',
    nextYear: 'Año siguiente',
    prevMonth: 'Mes anterior',
    nextMonth: 'Mes siguiente',
    prevHour: 'Hora anterior',
    nextHour: 'Hora siguiente',
    prevMinute: 'Minuto anterior',
    nextMinute: 'Minuto siguiente',
    prevSecond: 'Segundo anterior',
    nextSecond: 'Segundo siguiente',
    emptyMessage: 'No se encontraron resultados',
    emptyFilterMessage: 'No se encontraron resultados'
};

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
        },
        translation: esTranslation
    })
  ]
};

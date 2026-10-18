export const environment = {
  production: true,
  // En desarrollo con 'ng serve' en localhost:4200 apunta directo al backend 8080.
  // En Docker, Nginx, tablets por Wi-Fi o servidores en la nube usa '/api' relativo.
  apiUrl: (typeof window !== 'undefined' && window.location.port === '4200' && window.location.hostname === 'localhost')
    ? 'http://localhost:8080/api'
    : '/api'
};

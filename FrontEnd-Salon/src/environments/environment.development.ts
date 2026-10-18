export const environment = {
  production: false,
  apiUrl: (typeof window !== 'undefined' && window.location.port === '4200' && window.location.hostname === 'localhost')
    ? 'http://localhost:8080/api'
    : '/api'
};

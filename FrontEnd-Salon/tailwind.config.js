/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ['selector', '.app-dark'],
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
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
          DEFAULT: '#bca028'
        },
        gold: {
          50: '#fdfbf6',
          100: '#f9f6ea',
          200: '#f0e6c7',
          300: '#e6d3a0',
          400: '#d9bd72',
          500: '#bca028',
          600: '#9d841f',
          700: '#7c6717',
          DEFAULT: '#bca028'
        }
      },
      fontFamily: {
        body: ["League Spartan", "sans-serif"]
      }
    },
  },
  plugins: [],
}

/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ['selector', '.app-dark'],
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      fontFamily: {
        body: ["League Spartan", "sans-serif"]
      }
    },
  },
  plugins: [],
}

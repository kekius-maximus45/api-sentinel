/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#172026",
        graphite: "#334155",
        panel: "#f7f9fb",
        line: "#d8dee6",
        cobalt: "#2563eb",
        teal: "#0f766e",
        amber: "#b7791f",
        danger: "#b42318"
      },
      boxShadow: {
        panel: "0 12px 30px rgba(15, 23, 42, 0.08)"
      }
    }
  },
  plugins: []
};

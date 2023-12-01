import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const path = require('path');

export default defineConfig({
  plugins: [react()],
  esbuild: {
    loader: "jsx",
    include: [
      // Business as usual for .jsx
      "src/**/*.jsx",
      "node_modules/**/*.jsx",

      // Allow .js files to contain JSX
      "src/**/*.js",
    ],
    exclude: [],
  },
  build: {
    lib: {
      entry: path.resolve(__dirname, 'src/index.js'),
      name: 'NetworkMapViewer',
      formats: ['umd'],
      fileName: 'network-map-viewer',
    },
    rollupOptions: {
      external: ['react', 'react-dom', 'react-intl', 'react/jsx-runtime', /^@mui\/.+$/],
    },
  },
})



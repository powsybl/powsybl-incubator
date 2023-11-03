import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const path = require('path');

export default defineConfig({
  plugins: [react()],
  build: {
    lib: {
      entry: path.resolve(__dirname, 'src/index.js'),
      name: 'NetworkMapViewer',
      formats: ['umd'],
      fileName: 'network-map-viewer',
    },
    rollupOptions: {
      external: ['react', 'react-dom', 'react-intl', /^@mui\/.+$/],
    },
  },
})



import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

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
      entry: resolve(__dirname, 'src/index.js'),
      name: 'NetworkMapViewer',
      fileName: 'network-map-viewer',
    },
    rollupOptions: {
      external: ['react', 'react-dom', 'react-intl', 'react/jsx-runtime', '@emotion/react'],
    },
  },
})



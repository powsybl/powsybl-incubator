import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// package.json declares "type": "module", so the config is loaded as ESM and
// __dirname is not defined here.
const rootDir = path.dirname(fileURLToPath(import.meta.url));

// The component lives in the same clone, on this branch. Aliasing to its
// sources rather than its dist/ gives HMR on component edits and removes the
// need to build it at all — the alias applies to `vite build` too.
const componentSrc = path.resolve(rootDir, '../../network-editor/src');

export default defineConfig({
  plugins: [react()],
  resolve: {
    // Order matters: the "/react" subpath must be matched before the bare
    // specifier, otherwise the shorter prefix swallows both.
    alias: [
      {
        find: '@powsybl/network-editor/react',
        replacement: path.join(componentSrc, 'react/index.ts'),
      },
      {
        find: '@powsybl/network-editor',
        replacement: path.join(componentSrc, 'index.ts'),
      },
    ],
    // The component has its own node_modules with react in it; without dedupe
    // two copies can load and break hooks.
    dedupe: ['react', 'react-dom'],
  },
  server: {
    // The aliased sources sit outside this project root.
    fs: {
      allow: [path.resolve(rootDir, '../..')],
    },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8000',
        changeOrigin: true,
        rewrite: (reqPath) => reqPath.replace(/^\/api/, ''),
      },
    },
  },
});

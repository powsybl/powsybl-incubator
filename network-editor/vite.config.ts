import { defineConfig } from 'vitest/config';
import path from 'path';
import dts from 'vite-plugin-dts';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [
        react({ include: ['demo/**/*.tsx', 'src/**/*.tsx'] }),
        // entryRoot pins the declarations to src/, so the emitted entry really is
        // dist/index.d.ts — without it they land under dist/src/ and "types" dangles.
        // tsconfig.build.json narrows the program to src/, so the declarations are
        // rooted there and the emitted entry really is dist/index.d.ts. With the
        // demo in the program they land under dist/src/ and "types" dangles.
        // Declarations land in dist/src/, mirroring the source tree — that is where
        // package.json "types" points. insertTypesEntry/rollupTypes both emit an
        // empty dist/index.d.ts with this entry, so neither is used.
        dts({ tsconfigPath: './tsconfig.build.json' }),
    ],
    build: {
        lib: {
            entry: {
                'network-editor': path.resolve(__dirname, 'src/index.ts'),
                react: path.resolve(__dirname, 'src/react/index.ts'),
            },
            fileName: (format, entryName) => `${entryName}.${format === 'es' ? 'js' : 'cjs'}`,
            formats: ['es', 'cjs']
        },
        rollupOptions: {
            external: ['@powsybl/network-viewer-core', 'react', 'react-dom', 'react/jsx-runtime'],
        },
    },
    test: {
        environment: 'jsdom',
    },
});

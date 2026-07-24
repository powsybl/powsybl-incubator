import { defineConfig } from 'vitest/config';
import path from 'path';
import dts from 'vite-plugin-dts';

export default defineConfig({
    plugins: [
        dts({
            insertTypesEntry: true,
            exclude: ['**/*.test.ts', 'src/test-utils.ts', 'demo/**'],
        }),
    ],
    build: {
        lib: {
            entry: path.resolve(__dirname, 'src/index.ts'),
            name: 'NetworkEditor',
            fileName: 'network-editor',
            formats: ['es', 'cjs']
        },
        rollupOptions: {
            external: [
                'react',
                'react-dom',
                'react/jsx-runtime',
                '@powsybl/network-viewer-core'
            ],
            output: {
                globals: {
                    react: 'React',
                    'react-dom': 'ReactDOM',
                    'react/jsx-runtime': 'jsxRuntime',
                    '@powsybl/network-viewer-core': 'PowsyblNetworkViewerCore'
                }
            }
        },
    },
    test: {
        environment: 'jsdom',
    },
});

# Network Editor

React/TypeScript component extending the [powsybl-network-viewer](https://github.com/powsybl/powsybl-network-viewer) SLD/NAD viewers with network editing capabilities (element selection, deletion, undo/redo command stack).

## Build

```bash
npm install
npm run build
```

The library is bundled with [Vite](https://vitejs.dev/) into the `dist` folder (ESM + CJS, with type declarations).

## Development

```bash
npm run dev    # start the demo app (Vite dev server)
npm run check  # type-check with tsc
npm run test   # run unit tests with Vitest
```

The `demo` folder contains a small application showcasing the editor on top of a single-line diagram.

## Usage

```ts
import { NetworkEditor } from '@powsybl/network-editor';
```





# Network Editor

TypeScript component extending the [powsybl-network-viewer](https://github.com/powsybl/powsybl-network-viewer) single line diagrams (SLD) with network editing capabilities.

The editor never mutates the network itself: every user edit is recorded as a command, previewed in the diagram, and exposed as a normalized list of **network modifications** that the host application sends to its backend.

## Table of contents

- [Features](#features)
- [Installation](#installation)
- [Quick start](#quick-start)
- [API](#api)
  - [Options](#options)
  - [Methods](#methods)
  - [Events](#events)
  - [Actions and menus](#actions-and-menus)
- [Development](#development)
- [Project structure](#project-structure)


## Features

- **Selection** of equipments (click, Shift + click for multi-selection).
- **Creation** of injections (load, generator, battery, shunt, SVC, converter stations, boundary line…), switches, switched injections and feeder bays.
- **Deletion** of equipments and whole bays.
- **Replacement, renaming and property update** of existing equipments.
- **Bay layout**: move a bay along a busbar, flip its direction, update its position.
- **Undo / redo** command stack.
- **Change set** export: the pending edits are normalized into `NetworkModification` objects.
- **Host-driven UI**: menus and forms are rendered by the host application; the editor only emits events.

## Installation

```bash
npm install @powsybl/network-editor @powsybl/network-viewer-core
```

`@powsybl/network-viewer-core` (`^3.8.0-dev.0`) is a peer dependency.

## Quick start

```ts
import { NetworkEditor, type SLDMetadata } from '@powsybl/network-editor';

const editor = new NetworkEditor({
    container: document.getElementById('diagram')!,
    svgContent,                         // SLD SVG produced by powsybl-diagram
    metadata: metadata as SLDMetadata,  // matching SLD metadata
    initialProperties,                  // optional: equipment properties by id
    onEvent: (event) => {
        switch (event.name) {
            case 'menu:requested':
                showMenu(event.items, event.position);
                break;
            case 'gesture:changed':
                showForm(event.action);
                break;
            case 'modifications:changed':
                console.log(event.modifications);
                break;
        }
    },
});

// Later
const changes = editor.getModifications(); // send them to the backend
editor.destroy();
```

A complete React integration is available in [`demo/App.tsx`](demo/App.tsx).

## API

### Options

| Option               | Type                                 | Default           | Description                                                        |
|----------------------|--------------------------------------|-------------------|--------------------------------------------------------------------|
| `container`          | `HTMLElement`                        | —                 | Element hosting the diagram (required).                            |
| `svgContent`         | `string`                             | —                 | SLD SVG content (required).                                        |
| `metadata`           | `SLDMetadata`                        | —                 | SLD metadata (required).                                           |
| `svgType`            | `string`                             | `'voltage-level'` | Diagram type passed to the viewer.                                 |
| `minWidth`           | `number`                             | `200`             | Minimum diagram width.                                             |
| `minHeight`          | `number`                             | `200`             | Minimum diagram height.                                            |
| `maxWidth`           | `number`                             | `2000`            | Maximum diagram width.                                             |
| `maxHeight`          | `number`                             | `2000`            | Maximum diagram height.                                            |
| `selectionBackColor` | `string`                             | `'white'`         | Background color used by the viewer for selection.                 |
| `callbacks`          | `ViewerCallbacks`                    | —                 | Viewer callbacks (`onNextVoltage`, `onBreaker`, `onFeeder`, `onBus`, `onToggleHover`). |
| `onEvent`            | `(event: EditorEvent) => void`       | —                 | Listener receiving every editor event.                             |
| `initialProperties`  | `Record<string, EquipmentProperties>`| —                 | Initial equipment properties, by equipment id.                     |
| `isSupported`        | `(action: EditorAction) => boolean`  | —                 | Filters the actions offered to the user.                           |

### Methods

| Method                          | Description                                                         |
|---------------------------------|---------------------------------------------------------------------|
| `undo()` / `redo()`             | Undo or redo the last command.                                      |
| `getModifications()`            | Returns the pending change set as `NetworkModification[]`.          |
| `clearModifications()`          | Reverts every pending command and clears the history.               |
| `cancelGesture()`               | Cancels the gesture in progress (also triggered by `Esc`).          |
| `showIidmNodes(enabled)`        | Toggles the IIDM node debug overlay.                                |
| `getViewBox()` / `setViewBox()` | Reads or sets the diagram view box.                                 |
| `getWidth()` / `getHeight()`    | Diagram dimensions.                                                 |
| `destroy()`                     | Removes listeners and clears the container.                         |

Creating a new editor on a container that already hosts one automatically destroys the previous instance.

### Events

Events are delivered to `onEvent` as `{ name, ...payload }`:

| Event                   | Payload                                       | When                                                  |
|-------------------------|-----------------------------------------------|-------------------------------------------------------|
| `element:selected`      | `{ elements }`                                | The selection changed.                                |
| `history:changed`       | `{ canUndo, canRedo }`                        | The undo/redo stack changed.                          |
| `modifications:changed` | `{ modifications }`                           | The pending change set changed.                       |
| `gesture:changed`       | `{ gesture, action }`                         | A gesture started, progressed or ended.               |
| `menu:requested`        | `{ items, targets, trigger, position }`       | The user clicked/right-clicked an editable target.    |

### Actions and menus

`menu:requested` provides `ActionMenuItem`s. The host renders them, then:

```ts
function onItemClicked(item: ActionMenuItem) {
    if (item.needsForm) openForm(item.action); // host-owned form, see actionLabel(item.action)
    else item.action.run();
}
```

Supported operations (`EditOperation`): `CREATE_INJECTION`, `CREATE_SWITCH`, `CREATE_SWITCHED_INJECTION`, `CREATE_FEEDER_BAY`, `DELETE`, `DELETE_BAY`, `REPLACE`, `UPDATE_PROPERTIES`, `UPDATE_BAY_POSITION`, `FLIP_BAY_DIRECTION`, `MOVE_BAY`, `RENAME`.

## Development

```bash
npm install
npm run dev    # start the demo app (Vite dev server)
npm run check  # type-check with tsc
npm run test   # run unit tests with Vitest
npm run build  # build the library into dist/
```

## Project structure

```
src/
├── actions/        # editor actions, context menus, property descriptors
├── commands/       # undoable commands (create, delete, rename, move bay…)
├── dom/            # SVG DOM manipulation and editor styles
├── editor/         # NetworkEditor (public facade) and EditorCore
├── layout/         # bay placement and diagram geometry
├── model/          # editor model built from SLD metadata
├── modifications/  # NetworkModification types and normalization
├── types.ts        # shared types and constants
└── index.ts        # public API
demo/               # React demo application
```
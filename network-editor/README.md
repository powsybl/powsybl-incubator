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

## Backend integration

The editor reports what the user pointed at, in IIDM terms. It never computes
network modifications itself — that belongs to the backend, typically through
[pypowsybl](https://powsybl.readthedocs.io/projects/pypowsybl/).

### Attaching a new feeder to a busbar

Clicking on (or near) a busbar emits `connection-point:picked` with a
`BusbarConnectionTarget`. Its fields map one to one onto the `create_*_bay`
helpers:

| Event field | pypowsybl parameter |
| --- | --- |
| `busbarSectionId` | `bus_or_busbar_section_id` (`…_1` / `…_2` on branches) |
| `voltageLevelId` | `voltage_level_id` |
| `direction` | `direction` (`TOP` / `BOTTOM`) |
| `previousEquipmentId`, `nextEquipmentId` | used to derive `position_order` |

`position_order` is deliberately absent: ConnectablePosition orders live in the
IIDM network, not in the SLD metadata. The backend resolves them from the
neighbours the editor reports:

```python
orders = pp.network.get_connectables_order_positions(network, voltage_level_id)
# pick an order between previous_equipment_id and next_equipment_id,
# or pp.network.get_unused_order_positions_after(network, busbar_section_id)
# when the click landed past the last feeder.
pp.network.create_line_bays(
    network, id='NEW_LINE', r=0.1, x=10, b1=0, g1=0, b2=0, g2=0,
    bus_or_busbar_section_id_1=busbar_section_id, direction_1=direction,
    position_order_1=order, ...)
```

### Other modifications

- **Deleting** — `deleteElement` / `deleteFeederBay` put the equipment id in the
  change set; feed it to `remove_feeder_bays(connectable_ids=[...])`.
- **Splitting an existing line** — `create_line_on_line` and
  `connect_voltage_level_on_line` want a `line_id`, which `element:selected`
  already reports as `id`. No dedicated mechanism is needed.
- **Property edits** — the `update` change set entries carry IIDM attribute
  names (`targetP`, `voltageRegulatorOn`, …) as their payload.





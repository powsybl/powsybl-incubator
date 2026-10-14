import {
    BAY_SLOT_CLASS,
    NODE_TARGET_CLASS,
    PENDING_CREATE_CLASS,
    SELECTED_CLASS,
    SWITCH_END_CLASS,
    SWITCH_START_CLASS,
} from '../core/types';

export const IIDM_LINKED_CLASS = 'ne-iidm-linked';
export const IIDM_UNLINKED_CLASS = 'ne-iidm-unlinked';
export const IIDM_LABEL_CLASS = 'ne-iidm-label';
export const RENAMED_LABEL_CLASS = 'ne-renamed-label';
export const MOVING_BAY_CLASS = 'ne-moving-bay';
export const BAY_SLOT_LAYER_CLASS = 'ne-bay-slot-layer';
export const PENDING_LAYER_CLASS = 'ne-pending-layer';
export const PENDING_BADGE_CLASS = 'ne-pending-badge';
export const PENDING_SYMBOL_CLASS = 'ne-symbol';

const PENDING = `:is(.${PENDING_CREATE_CLASS}, .${PENDING_BADGE_CLASS})`;

const NODE_MARK = `:is(.${NODE_TARGET_CLASS}, .${SWITCH_END_CLASS}, .${SWITCH_START_CLASS})`;

export const EDITOR_STYLE = `

g${PENDING} {
    visibility: visible;
    pointer-events: none;
}
g${PENDING} > rect {
    fill: var(--ne-accent-soft, #e3f2fd);
    stroke: var(--ne-accent, #1e88e5);
    stroke-width: 0.8;
    stroke-dasharray: 2 1.5;
    rx: 1.5;
}
g${PENDING}[id] {
    pointer-events: auto;
    cursor: pointer;
}
g${PENDING}[id]:hover > rect {
    fill: var(--ne-accent-hover, #bbdefb);
}
g${PENDING} .${PENDING_SYMBOL_CLASS} > g {
    stroke-dasharray: 2 1.5;
    opacity: 0.75;
}
g${PENDING} > g > line {
    stroke: var(--ne-accent, #1e88e5);
    stroke-width: 1.2;
    stroke-dasharray: 2 1.5;
    pointer-events: none;
}
g${PENDING} > text {
    fill: var(--ne-accent-strong, #0d47a1);
    font-size: 6px;
    font-weight: bold;
    text-anchor: middle;
}

.sld-node:is(.${IIDM_LINKED_CLASS}, .${IIDM_UNLINKED_CLASS}) {
    visibility: visible;
}
.sld-node.${IIDM_LINKED_CLASS} {
    fill: #2e7d32;
}
.sld-node.${IIDM_UNLINKED_CLASS} {
    fill: #c62828;
}
text.${IIDM_LABEL_CLASS} {
    fill: #1a237e;
    stroke: #ffffff;
    stroke-width: 0.6;
    paint-order: stroke;
    font-size: 6px;
    font-weight: bold;
    pointer-events: none;
}

.sld-node${NODE_MARK} {
    visibility: visible;
    cursor: pointer;
}
.sld-node${NODE_MARK} circle {
    stroke: var(--ne-halo, #ffffff);
    stroke-width: 1.5;
    vector-effect: non-scaling-stroke;
}
.sld-node.${NODE_TARGET_CLASS} {
    fill: var(--ne-accent, #1e88e5);
}
.sld-node.${NODE_TARGET_CLASS}:hover {
    fill: var(--ne-accent-strong, #0d47a1);
}
.sld-node.${SWITCH_END_CLASS} {
    fill: var(--ne-candidate, #f9a825);
}
.sld-node.${SWITCH_END_CLASS}:hover {
    fill: var(--ne-candidate-strong, #ef6c00);
}
.sld-node.${SWITCH_START_CLASS} {
    fill: var(--ne-candidate-strong, #ef6c00);
    cursor: default;
}
.${SWITCH_END_CLASS} .sld-busbar-section {
    stroke: var(--ne-candidate, #f9a825);
    stroke-width: 3;
    cursor: pointer;
}

g.${BAY_SLOT_CLASS} {
    pointer-events: none;
}
.sld-busbar-section > line {
    cursor: pointer;
}
g.${BAY_SLOT_CLASS} circle {
    fill: var(--ne-candidate, #f9a825);
    stroke: var(--ne-halo, #ffffff);
    stroke-width: 1.5;
    vector-effect: non-scaling-stroke;
}

:is(.${SELECTED_CLASS}, .${MOVING_BAY_CLASS}) {
    outline: 2px dashed var(--ne-selection, #1976d2);
    outline-offset: 2px;
}
:is(.${SELECTED_CLASS}, .${MOVING_BAY_CLASS}) .sld-label {
    fill: var(--ne-selection, #1976d2);
}

text.${RENAMED_LABEL_CLASS} {
    fill: var(--ne-accent, #1e88e5);
    font-style: italic;
}
`;

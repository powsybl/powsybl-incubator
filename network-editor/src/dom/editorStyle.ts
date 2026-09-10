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
export const BAY_SLOT_LAYER_CLASS = 'ne-bay-slot-layer';
export const PENDING_LAYER_CLASS = 'ne-pending-layer';
export const PENDING_BADGE_CLASS = 'ne-pending-badge';

const PENDING = `:is(.${PENDING_CREATE_CLASS}, .${PENDING_BADGE_CLASS})`;

const NODE_MARK = `:is(.${NODE_TARGET_CLASS}, .${SWITCH_END_CLASS}, .${SWITCH_START_CLASS})`;

export const EDITOR_STYLE = `

g${PENDING} {
    visibility: visible;
    pointer-events: none;
}
g${PENDING} rect {
    fill: #e3f2fd;
    stroke: #1e88e5;
    stroke-width: 0.8;
    stroke-dasharray: 2 1.5;
    rx: 1.5;
}
g${PENDING}[id] {
    pointer-events: auto;
    cursor: pointer;
}
g${PENDING}[id]:hover rect {
    fill: #bbdefb;
}
g${PENDING} line {
    stroke: #1e88e5;
    stroke-width: 1.2;
    stroke-dasharray: 2 1.5;
    pointer-events: none;
}
g${PENDING} text {
    fill: #0d47a1;
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
    stroke: #ffffff;
    stroke-width: 1.5;
    vector-effect: non-scaling-stroke;
}
.sld-node.${NODE_TARGET_CLASS} {
    fill: #1e88e5;
}
.sld-node.${NODE_TARGET_CLASS}:hover {
    fill: #0d47a1;
}
.sld-node.${SWITCH_END_CLASS} {
    fill: #f9a825;
}
.sld-node.${SWITCH_END_CLASS}:hover {
    fill: #ef6c00;
}
.sld-node.${SWITCH_START_CLASS} {
    fill: #ef6c00;
    cursor: default;
}
.${SWITCH_END_CLASS} .sld-busbar-section {
    stroke: #f9a825;
    stroke-width: 3;
    cursor: pointer;
}

g.${BAY_SLOT_CLASS} {
    cursor: pointer;
}
g.${BAY_SLOT_CLASS} circle {
    fill: #f9a825;
    stroke: #ffffff;
    stroke-width: 1.5;
    vector-effect: non-scaling-stroke;
}
g.${BAY_SLOT_CLASS}:hover circle {
    fill: #ef6c00;
}

.${SELECTED_CLASS} {
    outline: 2px dashed #1976d2;
    outline-offset: 2px;
}
.${SELECTED_CLASS} .sld-label {
    fill: #1976d2;
}
`;

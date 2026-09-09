import {
    BAY_SLOT_CLASS,
    LINK_END_CLASS,
    LINK_START_CLASS,
    NODE_TARGET_CLASS,
    PENDING_CREATE_CLASS,
    SELECTED_CLASS,
    type BaySlotCandidate,
    type NodeDiagnostic,
} from '../core/types';

const SVG_NS = 'http://www.w3.org/2000/svg';

const IIDM_LINKED_CLASS = 'ne-iidm-linked';
const IIDM_UNLINKED_CLASS = 'ne-iidm-unlinked';
const IIDM_LABEL_CLASS = 'ne-iidm-label';
const BAY_SLOT_LAYER_CLASS = 'ne-bay-slot-layer';
const PENDING_LAYER_CLASS = 'ne-pending-layer';

const EDITOR_STYLE = `

g.${PENDING_CREATE_CLASS} {
    visibility: visible;
    pointer-events: none;
}
g.${PENDING_CREATE_CLASS} rect {
    fill: #e3f2fd;
    stroke: #1e88e5;
    stroke-width: 0.8;
    stroke-dasharray: 2 1.5;
    rx: 1.5;
}
g.${PENDING_CREATE_CLASS}[id] {
    pointer-events: auto;
    cursor: pointer;
}
g.${PENDING_CREATE_CLASS}[id]:hover rect {
    fill: #bbdefb;
}
g.${PENDING_CREATE_CLASS} line {
    stroke: #1e88e5;
    stroke-width: 1.2;
    stroke-dasharray: 2 1.5;
    pointer-events: none;
}
g.${PENDING_CREATE_CLASS} text {
    fill: #0d47a1;
    font-size: 6px;
    font-weight: bold;
    text-anchor: middle;
}

.sld-node.${IIDM_LINKED_CLASS},
.sld-node.${IIDM_UNLINKED_CLASS} {
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

.sld-node.${NODE_TARGET_CLASS},
.sld-node.${LINK_END_CLASS},
.sld-node.${LINK_START_CLASS} {
    visibility: visible;
    cursor: pointer;
}
.sld-node.${NODE_TARGET_CLASS} circle,
.sld-node.${LINK_END_CLASS} circle,
.sld-node.${LINK_START_CLASS} circle {
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
.sld-node.${LINK_END_CLASS} {
    fill: #f9a825;
}
.sld-node.${LINK_END_CLASS}:hover {
    fill: #ef6c00;
}
.sld-node.${LINK_START_CLASS} {
    fill: #ef6c00;
    cursor: default;
}
.${LINK_END_CLASS} .sld-busbar-section {
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

export interface PendingMarkerView {
    id?: string;
    label: string;
}

export interface PendingCreateView {
    id: string;
    label: string;
    x: number;
    y: number;
    towards: 1 | -1;
    withSwitch: boolean;
    switchSize: number;
}

export interface DiagramSpan {
    left: number;
    right: number;
    y: number;
}

export interface RemovedDomElement {
    element: Element;
    parent: Node | null;
    nextElement: Node | null;
}

export class SvgDomService {
    constructor(private readonly container: HTMLElement) {
        const style = document.createElement('style');
        style.dataset.neStyle = 'editor';
        style.textContent = EDITOR_STYLE;
        container.appendChild(style);
    }

    getContainer(): HTMLElement {
        return this.container;
    }

    findElementById(elementId: string): SVGElement | null {
        if (!elementId) return null;
        return this.container.querySelector<SVGElement>(`[id="${CSS.escape(elementId)}"]`);
    }

    removeAndSnapshot(element: Element): RemovedDomElement {
        const snapshot: RemovedDomElement = {
            element,
            parent: element.parentNode ?? null,
            nextElement: element.nextSibling,
        };
        element.remove();
        return snapshot;
    }

    restore(snapshot: RemovedDomElement): void {
        if (!snapshot.parent) return;
        snapshot.parent.insertBefore(snapshot.element, snapshot.nextElement);
    }

    getDiagramPoint(nodeId: string): { x: number; y: number } | undefined {
        const element = this.findElementById(nodeId);
        if (!(element instanceof SVGGraphicsElement)) return undefined;

        const matrix = element.getCTM();
        return matrix ? { x: matrix.e, y: matrix.f } : undefined;
    }

    getDiagramX(nodeId: string): number | undefined {
        return this.getDiagramPoint(nodeId)?.x;
    }

    getDiagramSpan(elementId: string): DiagramSpan | undefined {
        const element = this.findElementById(elementId);
        if (!(element instanceof SVGGraphicsElement)) return undefined;

        const line = element.querySelector('line');
        const matrix = element.getCTM();
        if (!line || !matrix) return undefined;

        const start = new DOMPoint(line.x1.baseVal.value, line.y1.baseVal.value);
        const end = new DOMPoint(line.x2.baseVal.value, line.y2.baseVal.value);
        const from = start.matrixTransform(matrix);
        const to = end.matrixTransform(matrix);

        return { left: Math.min(from.x, to.x), right: Math.max(from.x, to.x), y: from.y };
    }

    toDiagramX(clientX: number, clientY: number): number | undefined {
        const svg = this.getSvgRoot();
        const screen = svg?.getScreenCTM?.();
        if (!svg || !screen) return undefined;

        const point = new DOMPoint(clientX, clientY);
        return point.matrixTransform(screen.inverse()).x;
    }

    setNodeTargets(targetIds: readonly string[]): void {
        this.mark(NODE_TARGET_CLASS, targetIds);
    }

    setLinkEnds(firstId: string | null, candidateIds: readonly string[]): void {
        this.mark(LINK_END_CLASS, candidateIds);
        this.mark(LINK_START_CLASS, firstId ? [firstId] : []);
    }

    setBaySlots(slots: readonly BaySlotCandidate[]): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        svg.querySelector(`g.${BAY_SLOT_LAYER_CLASS}`)?.remove();
        if (slots.length === 0) return;

        const layer = document.createElementNS(SVG_NS, 'g');
        layer.setAttribute('class', BAY_SLOT_LAYER_CLASS);
        for (const slot of slots) layer.appendChild(createBaySlot(slot));
        svg.appendChild(layer);
    }

    shiftBay(feederNodeId: string, dx: number): void {
        const cell = this.findElementById(feederNodeId)?.closest('g.sld-extern-cell');
        if (!cell) return;

        if (dx === 0) cell.removeAttribute('transform');
        else cell.setAttribute('transform', `translate(${dx},0)`);
    }

    setSelection(nodeIds: readonly string[]): void {
        this.mark(SELECTED_CLASS, nodeIds);
    }

    setPendingCreations(markers: ReadonlyMap<string, readonly PendingMarkerView[]>): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        for (const marker of svg.querySelectorAll(`g.${PENDING_CREATE_CLASS}`)) {
            if (!marker.closest(`g.${PENDING_LAYER_CLASS}`)) marker.remove();
        }

        for (const [nodeId, pending] of markers) {
            const host = this.findElementById(nodeId);
            if (!host) continue;
            pending.forEach((view, index) => host.appendChild(createPendingMarker(view, index)));
        }
    }

    setPendingStubs(views: readonly PendingCreateView[]): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        svg.querySelector(`g.${PENDING_LAYER_CLASS}`)?.remove();
        if (views.length === 0) return;

        const layer = document.createElementNS(SVG_NS, 'g');
        layer.setAttribute('class', PENDING_LAYER_CLASS);
        for (const view of views) layer.appendChild(createPendingStub(view));
        svg.appendChild(layer);
    }

    setIidmOverlay(diagnostics: ReadonlyMap<string, NodeDiagnostic>): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        for (const label of svg.querySelectorAll(`text.${IIDM_LABEL_CLASS}`)) label.remove();
        for (const marked of svg.querySelectorAll(
            `.${IIDM_LINKED_CLASS}, .${IIDM_UNLINKED_CLASS}`,
        )) {
            marked.classList.remove(IIDM_LINKED_CLASS, IIDM_UNLINKED_CLASS);
        }

        for (const [nodeId, { iidmNode, hidden }] of diagnostics) {
            const element = this.findElementById(nodeId);
            if (!element) continue;

            if (hidden) {
                element.classList.add(
                    iidmNode === undefined ? IIDM_UNLINKED_CLASS : IIDM_LINKED_CLASS,
                );
            }
            if (iidmNode === undefined) continue;

            const label = document.createElementNS(SVG_NS, 'text');
            label.setAttribute('class', IIDM_LABEL_CLASS);
            label.setAttribute('x', '7');
            label.setAttribute('y', '-1');
            label.textContent = String(iidmNode);
            element.appendChild(label);
        }
    }

    setSwitchState(nodeId: string, open: boolean): void {
        const element = this.findElementById(nodeId);
        if (!element) return;
        element.classList.toggle('sld-open', open);
        element.classList.toggle('sld-closed', !open);
    }

    private mark(className: string, ids: readonly string[]): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        for (const marked of svg.querySelectorAll(`.${className}`)) {
            marked.classList.remove(className);
        }

        for (const id of ids) this.findElementById(id)?.classList.add(className);
    }

    private getSvgRoot(): SVGSVGElement | null {
        return this.container.querySelector('svg');
    }
}

const MARKER_HEIGHT = 10;
const MARKER_BASELINE = 7;
const MARKER_TOP = -14;
const NODE_CENTRE = 4;
const SLOT_RADIUS = 4;

function createBaySlot(slot: BaySlotCandidate): SVGGElement {
    const point = document.createElementNS(SVG_NS, 'g');
    point.setAttribute('class', BAY_SLOT_CLASS);
    point.id = slot.id;
    point.setAttribute('transform', `translate(${slot.x},${slot.y})`);

    const dot = document.createElementNS(SVG_NS, 'circle');
    dot.setAttribute('r', String(SLOT_RADIUS));

    const hint = document.createElementNS(SVG_NS, 'title');
    hint.textContent = `Order ${slot.order}`;

    point.append(dot, hint);
    return point;
}

// The stub is drawn along one axis: distances run away from the anchor, `towards` gives the side.
const STUB_LEAD = 16;
const STUB_TAIL = 16;
const STUB_PLAIN = 24;
const LABEL_HEIGHT = 10;
const LABEL_BASELINE = 7;

function createPendingStub(view: PendingCreateView): SVGGElement {
    const { label, towards, withSwitch, switchSize } = view;

    const stub = document.createElementNS(SVG_NS, 'g');
    stub.setAttribute('class', PENDING_CREATE_CLASS);
    stub.id = view.id;
    stub.setAttribute('transform', `translate(${view.x},${view.y})`);

    if (withSwitch) {
        stub.append(
            stubLine(towards, 0, STUB_LEAD),
            stubBox(towards, STUB_LEAD, STUB_LEAD + switchSize, switchSize),
            stubLine(towards, STUB_LEAD + switchSize, STUB_LEAD + switchSize + STUB_TAIL),
        );
    } else {
        stub.append(stubLine(towards, 0, STUB_PLAIN));
    }

    const from = withSwitch ? STUB_LEAD + switchSize + STUB_TAIL : STUB_PLAIN;
    stub.append(
        stubBox(towards, from, from + LABEL_HEIGHT, label.length * 3.4 + 8),
        stubLabel(towards, from, label),
    );
    return stub;
}

function stubLine(towards: number, from: number, to: number): SVGLineElement {
    const line = document.createElementNS(SVG_NS, 'line');
    line.setAttribute('x1', '0');
    line.setAttribute('x2', '0');
    line.setAttribute('y1', String(towards * from));
    line.setAttribute('y2', String(towards * to));
    return line;
}

function stubBox(towards: number, from: number, to: number, width: number): SVGRectElement {
    const box = document.createElementNS(SVG_NS, 'rect');
    box.setAttribute('x', String(-width / 2));
    box.setAttribute('y', String(Math.min(towards * from, towards * to)));
    box.setAttribute('width', String(width));
    box.setAttribute('height', String(to - from));
    return box;
}

function stubLabel(towards: number, from: number, label: string): SVGTextElement {
    const top = Math.min(towards * from, towards * (from + LABEL_HEIGHT));
    const text = document.createElementNS(SVG_NS, 'text');
    text.setAttribute('x', '0');
    text.setAttribute('y', String(top + LABEL_BASELINE));
    text.textContent = label;
    return text;
}

function createPendingMarker(view: PendingMarkerView, index = 0): SVGGElement {
    const { label } = view;
    const width = label.length * 3.4 + 8;
    const top = MARKER_TOP - index * (MARKER_HEIGHT + 2);

    const marker = document.createElementNS(SVG_NS, 'g');
    marker.setAttribute('class', PENDING_CREATE_CLASS);
    if (view.id) marker.id = view.id;

    const box = document.createElementNS(SVG_NS, 'rect');
    box.setAttribute('x', String(NODE_CENTRE - width / 2));
    box.setAttribute('y', String(top));
    box.setAttribute('width', String(width));
    box.setAttribute('height', String(MARKER_HEIGHT));

    const text = document.createElementNS(SVG_NS, 'text');
    text.setAttribute('x', String(NODE_CENTRE));
    text.setAttribute('y', String(top + MARKER_BASELINE));
    text.textContent = label;

    marker.append(box, text);
    return marker;
}


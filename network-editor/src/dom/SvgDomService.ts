import {
    LINK_END_CLASS,
    LINK_START_CLASS,
    NODE_TARGET_CLASS,
    PENDING_CREATE_CLASS,
    SELECTED_CLASS,
    type NodeDiagnostic,
} from '../core/types';

const SVG_NS = 'http://www.w3.org/2000/svg';

const IIDM_LINKED_CLASS = 'ne-iidm-linked';
const IIDM_UNLINKED_CLASS = 'ne-iidm-unlinked';
const IIDM_LABEL_CLASS = 'ne-iidm-label';

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
`;

export interface PendingMarkerView {
    id?: string;
    label: string;
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

    getDiagramX(nodeId: string): number | undefined {
        const element = this.findElementById(nodeId);
        if (!(element instanceof SVGGraphicsElement)) return undefined;
        return element.getCTM()?.e;
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

    setSelection(nodeIds: readonly string[]): void {
        this.mark(SELECTED_CLASS, nodeIds);
    }

    setPendingCreations(markers: ReadonlyMap<string, readonly PendingMarkerView[]>): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        for (const marker of svg.querySelectorAll(`g.${PENDING_CREATE_CLASS}`)) marker.remove();

        for (const [nodeId, pending] of markers) {
            const host = this.findElementById(nodeId);
            if (!host) continue;
            pending.forEach((view, index) => host.appendChild(createPendingMarker(view, index)));
        }
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


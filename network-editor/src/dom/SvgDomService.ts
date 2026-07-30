import { CONNECTION_POINT_CLASS } from '../core/types';

const CONNECTION_POINT_STYLE_ID = 'connection-points';

const CONNECTION_POINT_STYLE = `
.sld-node.${CONNECTION_POINT_CLASS} {
    visibility: visible;
    fill: #1e88e5;
    cursor: pointer;
}
.sld-node.${CONNECTION_POINT_CLASS} circle {
    stroke: #ffffff;
    stroke-width: 1.5;
    vector-effect: non-scaling-stroke;
}
.sld-node.${CONNECTION_POINT_CLASS}:hover {
    fill: #0d47a1;
}
`;

const IIDM_LINKED_CLASS = 'ne-iidm-linked';
const IIDM_UNLINKED_CLASS = 'ne-iidm-unlinked';
const IIDM_LABEL_CLASS = 'ne-iidm-label';
const IIDM_NODE_STYLE_ID = 'iidm-nodes';

/**
 * Debug overlay. Fictitious nodes are drawn by powsybl-diagram as a hidden dot;
 * revealing it and setting the group's `fill` colours that very dot, so nothing
 * is added to the DOM: green when the node names an IIDM node, red when it names
 * none — a bus hook, on which nothing can be attached.
 */
const IIDM_NODE_STYLE = `
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
`;

export interface RemovedDomElement {
    element: Element;
    parent: Node | null;
    nextElement: Node | null;
}

export class SvgDomService {
    constructor(private readonly container: HTMLElement) {}

    getContainer(): HTMLElement {
        return this.container;
    }

    findElementById(elementId: string): SVGElement | null {
        const svg = this.getSvgRoot();
        if (!svg || !elementId) return null;
        const selector = `[id="${CSS.escape(elementId)}"]`;
        return svg.querySelector<SVGElement>(selector);
    }

    findEquipmentLabelByNodeId(nodeId: string): string | undefined {
        return (
            this.findElementById(nodeId)
                ?.querySelector('.sld-label, text')
                ?.textContent?.trim() || undefined
        );
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

    setConnectionPoints(pointIds: readonly string[]): void {
        const svg = this.getSvgRoot();
        if (!svg) return;
        this.ensureStyle(CONNECTION_POINT_STYLE_ID, CONNECTION_POINT_STYLE);

        for (const marked of svg.querySelectorAll(`.${CONNECTION_POINT_CLASS}`)) {
            marked.classList.remove(CONNECTION_POINT_CLASS);
        }

        for (const pointId of pointIds) {
            this.findElementById(pointId)?.classList.add(CONNECTION_POINT_CLASS);
        }
    }

    /**
     * Colours every fictitious node — green when it names an IIDM node, red
     * when it names none — and writes each number next to the node standing on
     * it. Labels go inside the node's own `<g>`, so they inherit its
     * translation and no coordinate has to be computed.
     */
    setIidmOverlay(
        labels: ReadonlyMap<string, number>,
        fictitious: ReadonlyMap<string, boolean>,
    ): void {
        const svg = this.getSvgRoot();
        if (!svg) return;
        this.ensureStyle(IIDM_NODE_STYLE_ID, IIDM_NODE_STYLE);

        for (const label of svg.querySelectorAll(`text.${IIDM_LABEL_CLASS}`)) label.remove();
        for (const marked of svg.querySelectorAll(
            `.${IIDM_LINKED_CLASS}, .${IIDM_UNLINKED_CLASS}`,
        )) {
            marked.classList.remove(IIDM_LINKED_CLASS, IIDM_UNLINKED_CLASS);
        }

        for (const [nodeId, linked] of fictitious) {
            this.findElementById(nodeId)
                ?.classList.add(linked ? IIDM_LINKED_CLASS : IIDM_UNLINKED_CLASS);
        }

        for (const [nodeId, iidmNode] of labels) {
            const element = this.findElementById(nodeId);
            if (!element) continue;

            const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
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

    private getSvgRoot(): SVGSVGElement | null {
        return this.container.querySelector('svg');
    }

    private ensureStyle(id: string, css: string): void {
        if (this.container.querySelector(`style[data-ne-style="${id}"]`)) return;
        const style = document.createElement('style');
        style.dataset.neStyle = id;
        style.textContent = css;
        this.container.appendChild(style);
    }
}


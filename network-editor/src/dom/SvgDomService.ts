export interface RemovedDomElement {
    element: Element;
    parent: Node | null;
    nextElement: Node | null;
}

export class SvgDomService {
    constructor(private readonly container: HTMLElement) {}

    getSvgRoot(): SVGSVGElement | null {
        return this.container.querySelector('svg');
    }

    getContainer(): HTMLElement {
        return this.container;
    }

    findElementById(elementId: string): SVGElement | null {
        const svg = this.getSvgRoot();
        if (!svg || !elementId) return null;
        const selector = `[id="${CSS.escape(elementId)}"]`;
        return svg.querySelector<SVGElement>(selector);
    }

    findEquipmentLabelByNodeId(nodeId: string): string {
        const text = this.findElementById(nodeId)
            ?.querySelector('.sld-label, text')
            ?.textContent?.trim();
        return text || nodeId;
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

    setSwitchState(nodeId: string, open: boolean): void {
        const element = this.findElementById(nodeId);
        if (!element) return;
        element.classList.toggle('sld-open', open);
        element.classList.toggle('sld-closed', !open);
    }

    /**
     * Arms the busbar connection marker. Busbars are already visible, so there
     * is nothing to reveal — the only injected CSS keeps the marker out of hit
     * testing; its looks belong to the host.
     */
    setConnectionPointsInteractive(enabled: boolean): void {
        this.container.classList.toggle(CONNECTION_POINTS_CLASS, enabled);
        const existing = this.container.querySelector(`#${CONNECTION_POINTS_STYLE_ID}`);
        if (!enabled) {
            this.clearConnectionPointHighlight();
            existing?.remove();
            return;
        }
        if (existing) return;
        const style = document.createElement('style');
        style.id = CONNECTION_POINTS_STYLE_ID;
        style.textContent = CONNECTION_POINTS_CSS;
        this.container.prepend(style);
    }

    /** Busbar segments in diagram coordinates, still attached to the SVG. */
    getBusbarSegments(): readonly BusbarSegment[] {
        return this.getBusbars().filter((busbar) => busbar.element.isConnected);
    }

    /** Marks a spot on a busbar — what a click, or later a drop, attaches to. */
    showBusbarMarker(at: { x: number; y: number }): void {
        const svg = this.getSvgRoot();
        if (!svg) return;
        if (!this.busbarMarker?.isConnected) {
            const marker = document.createElementNS(SVG_NS, 'circle');
            marker.setAttribute('class', BUSBAR_TARGET_CLASS);
            marker.setAttribute('r', '6');
            svg.append(marker);
            this.busbarMarker = marker;
        }
        this.busbarMarker.setAttribute('cx', String(at.x));
        this.busbarMarker.setAttribute('cy', String(at.y));
    }

    clearConnectionPointHighlight(): void {
        this.busbarMarker?.remove();
        this.busbarMarker = null;
    }

    /** Drops the cached busbar geometry — call after swapping the SVG. */
    refreshConnectionPoints(): void {
        this.busbars = null;
    }

    getNodePosition(nodeId: string): { x: number; y: number } | null {
        return parseTranslate(this.findElementById(nodeId)?.getAttribute('transform'));
    }

    /** Screen position in diagram coordinates, with the current zoom scale. */
    toDiagramPoint(
        clientX: number,
        clientY: number,
    ): { x: number; y: number; scale: number } | null {
        return this.clientToDiagram(clientX, clientY);
    }

    private busbars: BusbarSegment[] | null = null;
    private busbarMarker: SVGCircleElement | null = null;

    private getBusbars(): BusbarSegment[] {
        if (this.busbars) return this.busbars;
        const svg = this.getSvgRoot();
        if (!svg) return [];

        const segments: BusbarSegment[] = [];
        for (const element of svg.querySelectorAll<SVGGElement>(BUSBAR_SELECTOR)) {
            const origin = element.id && parseTranslate(element.getAttribute('transform'));
            const line = element.querySelector('line');
            if (!origin || !line) continue;
            // The <line> coordinates are relative to the group's translate.
            segments.push({
                id: element.id,
                element,
                x1: origin.x + numberAttribute(line, 'x1'),
                y1: origin.y + numberAttribute(line, 'y1'),
                x2: origin.x + numberAttribute(line, 'x2'),
                y2: origin.y + numberAttribute(line, 'y2'),
            });
        }
        this.busbars = segments;
        return segments;
    }

    private clientToDiagram(
        clientX: number,
        clientY: number,
    ): { x: number; y: number; scale: number } | null {
        const svg = this.getSvgRoot();
        if (!svg) return null;

        const matrix = svg.getScreenCTM?.();
        if (matrix) {
            const inverse = matrix.inverse();
            return {
                x: clientX * inverse.a + clientY * inverse.c + inverse.e,
                y: clientX * inverse.b + clientY * inverse.d + inverse.f,
                scale: Math.abs(matrix.a) || 1,
            };
        }

        const box = svg.getAttribute('viewBox')?.split(/[\s,]+/).map(Number);
        const rect = svg.getBoundingClientRect();
        if (!box || box.length !== 4 || !rect.width || !box[2]) return null;
        const scale = rect.width / box[2];
        return {
            x: box[0] + (clientX - rect.left) / scale,
            y: box[1] + (clientY - rect.top) / scale,
            scale,
        };
    }
}

export interface BusbarSegment {
    id: string;
    element: SVGGElement;
    x1: number;
    y1: number;
    x2: number;
    y2: number;
}

function parseTranslate(transform: string | null | undefined): { x: number; y: number } | null {
    const match = transform?.match(/translate\(\s*([-\d.]+)[\s,]+([-\d.]+)\s*\)/);
    return match ? { x: Number(match[1]), y: Number(match[2]) } : null;
}

function numberAttribute(element: Element, name: string): number {
    return Number(element.getAttribute(name) ?? 0);
}

const SVG_NS = 'http://www.w3.org/2000/svg';
const CONNECTION_POINTS_CLASS = 'ne-connection-points';
const CONNECTION_POINTS_STYLE_ID = 'ne-connection-points-style';
const BUSBAR_SELECTOR = '.sld-busbar-section[id]';

/** On the marker drawn where the cursor projects onto a busbar. */
export const BUSBAR_TARGET_CLASS = 'ne-busbar-target';

const CONNECTION_POINTS_CSS = `
.${CONNECTION_POINTS_CLASS} .${BUSBAR_TARGET_CLASS} {
    pointer-events: none;
}
`;


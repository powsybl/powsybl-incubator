import { flipY, type BayFlip } from '../core/bayGeometry';
import {
    NODE_TARGET_CLASS,
    SELECTED_CLASS,
    SWITCH_END_CLASS,
    SWITCH_START_CLASS,
    type BaySlotCandidate,
    type DiagramPoint,
    type DiagramSpan,
    type FeederDirection,
    type NodeDiagnostic,
} from '../core/types';
import {
    BAY_SLOT_LAYER_CLASS,
    EDITOR_STYLE,
    IIDM_LABEL_CLASS,
    IIDM_LINKED_CLASS,
    IIDM_UNLINKED_CLASS,
    PENDING_LAYER_CLASS,
    PENDING_BADGE_CLASS,
} from './editorStyle';
import {
    createBaySlot,
    createPendingBadge,
    createPendingPreview,
    svgElement,
    type PendingPreview,
    type PendingBadgeView,
} from './svgShapes';

export interface RemovedDomElement {
    element: Element;
    parent: Node | null;
    nextElement: Node | null;
}

/** Everything needed to turn an extern cell over its busbars, one way. */
export interface CellFlip {
    feederNodeId: string;
    direction: FeederDirection;
    y: BayFlip;
    heights: ReadonlyMap<string, number>;
    feeders: ReadonlyMap<string, string | null>;
    arrows: ReadonlyMap<string, string | null>;
}

const TRANSLATE = /translate\(\s*([-\d.e]+)[\s,]+([-\d.e]+)\s*\)/;

const SHAPE_TAGS: ReadonlySet<string> = new Set([
    'path',
    'circle',
    'ellipse',
    'rect',
    'line',
    'polyline',
    'polygon',
]);

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

    getVoltageClasses(elementId: string): string[] {
        const element = this.findElementById(elementId);
        return element ? [...element.classList].filter((name) => /^sld-(vl|bus-)/.test(name)) : [];
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

    getDiagramPoint(nodeId: string): DiagramPoint | undefined {
        const matrix = this.graphicsElement(nodeId)?.getCTM();
        return matrix ? { x: matrix.e, y: matrix.f } : undefined;
    }

    getDiagramSpan(elementId: string): DiagramSpan | undefined {
        const element = this.graphicsElement(elementId);
        const line = element?.querySelector('line');
        const matrix = element?.getCTM();
        if (!line || !matrix) return undefined;

        const start = new DOMPoint(line.x1.baseVal.value, line.y1.baseVal.value);
        const end = new DOMPoint(line.x2.baseVal.value, line.y2.baseVal.value);
        const from = start.matrixTransform(matrix);
        const to = end.matrixTransform(matrix);

        return { left: Math.min(from.x, to.x), right: Math.max(from.x, to.x), y: from.y };
    }

    toDiagramX(clientX: number, clientY: number): number | undefined {
        const screen = this.getSvgRoot()?.getScreenCTM();
        if (!screen) return undefined;

        return new DOMPoint(clientX, clientY).matrixTransform(screen.inverse()).x;
    }

    clear(): void {
        this.setNodeTargets([]);
        this.setSwitchEnds(null, []);
        this.setSelection([]);
        this.setBaySlots([]);
        this.setPendingPreviews([]);
        this.setPendingBadges(new Map());
    }

    setNodeTargets(targetIds: readonly string[]): void {
        this.addClassToElement(NODE_TARGET_CLASS, targetIds);
    }

    setSwitchEnds(firstId: string | null, candidateIds: readonly string[]): void {
        this.addClassToElement(SWITCH_END_CLASS, candidateIds);
        this.addClassToElement(SWITCH_START_CLASS, firstId ? [firstId] : []);
    }

    setSelection(nodeIds: readonly string[]): void {
        this.addClassToElement(SELECTED_CLASS, nodeIds);
    }

    setSwitchState(nodeId: string, open: boolean): void {
        const element = this.findElementById(nodeId);
        if (!element) return;
        element.classList.toggle('sld-open', open);
        element.classList.toggle('sld-closed', !open);
    }

    setBaySlots(slots: readonly BaySlotCandidate[]): void {
        this.replaceLayer(BAY_SLOT_LAYER_CLASS, slots.map(createBaySlot));
    }

    setPendingPreviews(views: readonly PendingPreview[]): void {
        this.replaceLayer(PENDING_LAYER_CLASS, views.map(createPendingPreview));
    }

    setPendingBadges(markers: ReadonlyMap<string, readonly PendingBadgeView[]>): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        for (const marker of svg.querySelectorAll(`g.${PENDING_BADGE_CLASS}`)) marker.remove();

        for (const [nodeId, pending] of markers) {
            const host = this.findElementById(nodeId);
            if (!host) continue;
            pending.forEach((view, index) => host.appendChild(createPendingBadge(view, index)));
        }
    }

    /** Debug overlay: shows the IIDM node each element stands on. */
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

            const label = svgElement('text');
            label.setAttribute('class', IIDM_LABEL_CLASS);
            label.setAttribute('x', '7');
            label.setAttribute('y', '-1');
            label.textContent = String(iidmNode);
            element.appendChild(label);
        }
    }

    shiftBay(feederNodeId: string, dx: number): void {
        const cell = this.bayCell(feederNodeId);
        if (!cell) return;

        if (dx === 0) cell.removeAttribute('transform');
        else cell.setAttribute('transform', `translate(${dx},0)`);
    }

    bayCell(feederNodeId: string): Element | null {
        return this.findElementById(feederNodeId)?.closest('g.sld-extern-cell') ?? null;
    }

    bayCellElementIds(feederNodeId: string): string[] {
        const cell = this.bayCell(feederNodeId);
        if (!cell) return [];
        return [...cell.querySelectorAll(':scope > g[id]')].map((element) => element.id);
    }

    flipCell(flip: CellFlip): void {
        const cell = this.bayCell(flip.feederNodeId);
        if (!cell) return;

        const top = flip.direction === 'TOP';
        cell.classList.toggle('sld-cell-direction-top', top);
        cell.classList.toggle('sld-cell-direction-bottom', !top);

        for (const polyline of cell.querySelectorAll(':scope > g.sld-wire > polyline')) {
            const values = (polyline.getAttribute('points') ?? '').trim().split(/[\s,]+/).map(Number);
            const points: string[] = [];
            for (let i = 0; i + 1 < values.length; i += 2) {
                points.push(`${values[i]},${round(flipY(values[i + 1], flip.y))}`);
            }
            polyline.setAttribute('points', points.join(','));
        }

        for (const group of cell.querySelectorAll(':scope > g[transform]')) {
            const match = TRANSLATE.exec(group.getAttribute('transform') ?? '');
            if (!match) continue;
            const x = Number(match[1]);
            const y = Number(match[2]);
            const half = (flip.heights.get(group.id) ?? 0) / 2;
            group.setAttribute('transform', `translate(${x},${round(flipY(y + half, flip.y) - half)})`);
        }

        for (const [feederId, transform] of flip.feeders) {
            const feeder = this.findElementById(feederId);
            if (!feeder) continue;

            feeder.classList.toggle('sld-top-feeder', top);
            feeder.classList.toggle('sld-bottom-feeder', !top);
            setShapesTransform(feeder, transform);

            const height = flip.heights.get(feederId) ?? 0;
            for (const label of feeder.querySelectorAll(':scope > text.sld-label')) {
                const y = Number(label.getAttribute('y') ?? 0);
                label.setAttribute('y', String(height - y));
            }
        }

        for (const [arrowId, transform] of flip.arrows) {
            const arrow = this.findElementById(arrowId);
            if (arrow) setShapesTransform(arrow, transform);
        }
    }

    private addClassToElement(className: string, ids: readonly string[]): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        for (const marked of svg.querySelectorAll(`.${className}`)) {
            marked.classList.remove(className);
        }

        for (const id of ids) this.findElementById(id)?.classList.add(className);
    }

    private replaceLayer(className: string, children: readonly SVGElement[]): void {
        const svg = this.getSvgRoot();
        if (!svg) return;

        svg.querySelector(`g.${className}`)?.remove();
        if (children.length === 0) return;

        const layer = svgElement('g');
        layer.setAttribute('class', className);
        layer.append(...children);
        svg.appendChild(layer);
    }

    private graphicsElement(elementId: string): SVGGraphicsElement | undefined {
        const element = this.findElementById(elementId);
        return element instanceof SVGGraphicsElement ? element : undefined;
    }

    private getSvgRoot(): SVGSVGElement | null {
        return this.container.querySelector('svg');
    }
}

function round(value: number): number {
    return Math.round(value * 1000) / 1000;
}

function setShapesTransform(component: Element, transform: string | null): void {
    for (const shape of component.children) {
        if (!SHAPE_TAGS.has(shape.localName)) continue;
        if (transform) shape.setAttribute('transform', transform);
        else shape.removeAttribute('transform');
    }
}

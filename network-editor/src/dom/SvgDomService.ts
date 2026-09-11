import {
    NODE_TARGET_CLASS,
    SELECTED_CLASS,
    SWITCH_END_CLASS,
    SWITCH_START_CLASS,
    type BaySlotCandidate,
    type DiagramPoint,
    type DiagramSpan,
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

    /** Takes every editor-owned mark and layer back off the diagram. */
    clear(): void {
        this.setNodeTargets([]);
        this.setSwitchEnds(null, []);
        this.setSelection([]);
        this.setBaySlots([]);
        this.setPendingPreviews([]);
        this.setPendingBadges(new Map());
    }

    setNodeTargets(targetIds: readonly string[]): void {
        this.mark(NODE_TARGET_CLASS, targetIds);
    }

    setSwitchEnds(firstId: string | null, candidateIds: readonly string[]): void {
        this.mark(SWITCH_END_CLASS, candidateIds);
        this.mark(SWITCH_START_CLASS, firstId ? [firstId] : []);
    }

    setSelection(nodeIds: readonly string[]): void {
        this.mark(SELECTED_CLASS, nodeIds);
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
        const cell = this.findElementById(feederNodeId)?.closest('g.sld-extern-cell');
        if (!cell) return;

        if (dx === 0) cell.removeAttribute('transform');
        else cell.setAttribute('transform', `translate(${dx},0)`);
    }

    private mark(className: string, ids: readonly string[]): void {
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

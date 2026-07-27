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
}


import {
    createSldComponentElement,
    DefaultSldLibraryComponents,
    SingleLineDiagramViewer,
} from '@powsybl/network-viewer-core';

import { EditorModel } from './EditorModel';
import { EditorCore } from './EditorCore';
import { SvgDomService } from '../dom/SvgDomService';
import type { NetworkModification } from './modifications';
import {
    EDITOR_OPTION_DEFAULTS,
    type EditorOptions,
    type EquipmentProperties,
    type SymbolProvider,
} from './types';

export type DiagramViewBox = Parameters<SingleLineDiagramViewer['setViewBox']>[0];

const editorByContainer = new WeakMap<HTMLElement, NetworkEditor>();

export class NetworkEditor {
    private readonly container: HTMLElement;
    private readonly viewer: SingleLineDiagramViewer;
    private readonly core: EditorCore;
    private destroyed = false;

    constructor(options: EditorOptions) {
        const opts = { ...EDITOR_OPTION_DEFAULTS, ...options };
        const { callbacks } = options;

        this.container = opts.container;
        editorByContainer.get(opts.container)?.destroy();
        const model = new EditorModel(opts.metadata, opts.initialProperties);

        this.viewer = new SingleLineDiagramViewer(
            opts.container,
            opts.svgContent,
            opts.metadata,
            opts.svgType,
            opts.minWidth,
            opts.minHeight,
            opts.maxWidth,
            opts.maxHeight,
            callbacks?.onNextVoltage ?? null,
            callbacks?.onBreaker ?? null,
            callbacks?.onFeeder ?? null,
            callbacks?.onBus ?? null,
            opts.selectionBackColor,
            callbacks?.onToggleHover ?? null,
        );

        const symbols: SymbolProvider = {
            componentSize: (type) =>
                model.componentSize(type) ??
                DefaultSldLibraryComponents.find((component) => component.type === type)?.size ?? {
                    width: 0,
                    height: 0,
                },
            createSymbol: createSldComponentElement,
        };

        const dom = new SvgDomService(opts.container);
        this.core = new EditorCore(
            model,
            dom,
            symbols,
            opts.onEvent,
            opts.isSupported,
        );

        this.container.addEventListener('contextmenu', this.onContextMenu, true);
        editorByContainer.set(opts.container, this);
    }

    destroy(): void {
        if (this.destroyed) return;
        this.destroyed = true;
        this.container.removeEventListener('contextmenu', this.onContextMenu, true);
        this.core.destroy();
        editorByContainer.delete(this.container);
        this.container.replaceChildren();
    }

    getWidth(): number {
        return this.viewer.getWidth();
    }

    getHeight(): number {
        return this.viewer.getHeight();
    }

    getViewBox(): DiagramViewBox | undefined {
        return this.viewer.getViewBox();
    }

    setViewBox(viewBox: DiagramViewBox): void {
        this.viewer.setViewBox(viewBox);
    }

    undo(): void {
        this.core.undo();
    }

    redo(): void {
        this.core.redo();
    }

    getViewer(): SingleLineDiagramViewer {
        return this.viewer;
    }

    getModifications(): NetworkModification[] {
        return this.core.getModifications();
    }

    clearModifications(): void {
        this.core.clearModifications();
    }

    /** Debug overlay: shows the IIDM node each element stands on. */
    showIidmNodes(enabled: boolean): void {
        this.core.showIidmNodes(enabled);
    }

    cancelGesture(): void {
        this.core.cancelGesture();
    }

    getProperties(equipmentId: string): EquipmentProperties {
        return this.core.getProperties(equipmentId);
    }

    private readonly onContextMenu = (event: MouseEvent): void => {
        this.core.handleContextMenu(event);
    };
}

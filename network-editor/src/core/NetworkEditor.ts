import { SingleLineDiagramViewer } from '@powsybl/network-viewer-core';

import { EditorModel } from './EditorModel';
import { EditorCore } from './EditorCore';
import { SvgDomService } from '../dom/SvgDomService';
import { type EditorAction } from './actions';
import {
    EDITOR_OPTION_DEFAULTS,
    type BayInsertion,
    type ChangeSet,
    type EditorOptions,
    type EditTarget,
    type EquipmentProperties,
} from './types';


export class NetworkEditor {
    private readonly container: HTMLElement;
    private readonly viewer: SingleLineDiagramViewer;
    private readonly core: EditorCore;

    constructor(options: EditorOptions) {
        const opts = { ...EDITOR_OPTION_DEFAULTS, ...options };
        const { callbacks } = options;

        this.container = opts.container;
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

        const dom = new SvgDomService(opts.container);
        this.core = new EditorCore(model, dom, opts.onEvent, opts.onTargets);

        this.container.addEventListener('contextmenu', this.onContextMenu);
    }

    destroy(): void {
        this.container.removeEventListener('contextmenu', this.onContextMenu);
        this.core.destroy();
        this.container.replaceChildren();
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

    getPendingChanges(): ChangeSet {
        return this.core.getPendingChanges();
    }

    /** To be called once the backend applied them and a fresh diagram is loaded. */
    clearPendingChanges(): void {
        this.core.clearPendingChanges();
    }

    /** Debug overlay: shows the IIDM node each element stands on. */
    showIidmNodes(enabled: boolean): void {
        this.core.showIidmNodes(enabled);
    }

    getTargets(): EditTarget[] {
        return this.core.getTargets();
    }

    /**
     * Every entry a host can put in a menu for this target
     */
    actionsFor(target: EditTarget, insertion?: BayInsertion): EditorAction[] {
        return this.core.actionsFor(target, insertion);
    }

    getSelectedEquipmentId(): string | null {
        return this.core.getSelectedEquipmentId();
    }

    getProperties(equipmentId: string): EquipmentProperties {
        return this.core.getProperties(equipmentId);
    }

    seedProperties(equipmentId: string, values: EquipmentProperties): void {
        this.core.seedProperties(equipmentId, values);
    }

    private readonly onContextMenu = (event: MouseEvent): void => {
        this.core.handleContextMenu(event);
    };
}

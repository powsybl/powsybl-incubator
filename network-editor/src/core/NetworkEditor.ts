import { SingleLineDiagramViewer } from '@powsybl/network-viewer-core';

import { EditorModel } from './EditorModel';
import { EditorCore } from './EditorCore';
import { SvgDomService } from '../dom/SvgDomService';
import {
    EDITOR_OPTION_DEFAULTS,
    type ChangeSet,
    type EditorOptions,
    type EquipmentInfo,
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
        this.core = new EditorCore(
            model,
            dom,
            opts.onEvent,
            opts.onEquipmentContextMenu,
        );

        this.container.addEventListener('contextmenu', this.onContextMenu);
    }

    destroy(): void {
        this.container.removeEventListener('contextmenu', this.onContextMenu);
        this.core.destroy();
        this.container.replaceChildren()
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

    getEquipmentInfo(equipmentId: string): EquipmentInfo | null {
        return this.core.getEquipmentInfo(equipmentId);
    }

    deleteElement(equipmentId: string): boolean {
        return this.core.deleteElement(equipmentId);
    }

    deleteFeederBay(equipmentId: string): boolean {
        return this.core.deleteFeederBay(equipmentId);
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

    applyProperties(equipmentId: string, changes: EquipmentProperties): boolean {
        return this.core.applyProperties(equipmentId, changes);
    }

    private readonly onContextMenu = (event: MouseEvent): void => {
        this.core.handleContextMenu(event);
    };
}

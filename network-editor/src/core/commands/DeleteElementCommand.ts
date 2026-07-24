import type { Command } from './Command';
import type { EditorModel } from '../EditorModel';
import type { SvgDomService, RemovedDomElement } from '../../dom/SvgDomService';
import {
    toElementType,
    type ChangeSetEntry,
    type EditorEventListener,
    type ElementType,
    type FeederInfoMetadata,
    type NodeMetadata,
    type WireMetadata,
} from '../types';

export class DeleteElementCommand implements Command {
    readonly label: string;

    private readonly equipmentId: string;
    private readonly type: ElementType;

    private domSnapshots: RemovedDomElement[] = [];
    private nodeMetas: NodeMetadata[] = [];
    private wireMetas: WireMetadata[] = [];
    private feederInfoMetas: FeederInfoMetadata[] = [];

    constructor(
        private readonly node: NodeMetadata,
        private readonly scope: { nodes: NodeMetadata[]; wires: WireMetadata[] },
        private readonly kind: 'element' | 'bay',
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly emit: EditorEventListener,
    ) {
        this.equipmentId = node.equipmentId ?? node.id;
        this.type = toElementType(node.componentType);
        this.label = `Delete ${this.kind} ${this.type} ${this.equipmentId}`;
    }

    execute(): void {
        this.domSnapshots = [];
        this.nodeMetas = [];
        this.wireMetas = [];
        this.feederInfoMetas = [];

        for (const wire of this.scope.wires) {
            this.removeFromDom(wire.id);
            const meta = this.model.removeWire(wire.id);
            if (meta) this.wireMetas.push(meta);
        }

        // Feeder infos (P/Q arrows) attached to the equipment.
        for (const info of [...this.model.getFeederInfosForEquipment(this.equipmentId)]) {
            this.removeFromDom(info.id);
            const meta = this.model.removeFeederInfo(info.id);
            if (meta) this.feederInfoMetas.push(meta);
        }

        for (const node of this.scope.nodes) {
            this.removeFromDom(node.id);
            const meta = this.model.removeNode(node.id);
            if (meta) this.nodeMetas.push(meta);
        }

        this.emit('element:removed', { id: this.equipmentId, type: this.type });
    }

    undo(): void {

        for (const snapshot of [...this.domSnapshots].reverse()) {
            this.dom.restore(snapshot);
        }
        this.domSnapshots = [];

        // Metadata: re-add the nodes, then their wires and feeder infos.
        for (const node of this.nodeMetas) this.model.addNode(node);
        for (const wire of this.wireMetas) this.model.addWire(wire);
        for (const info of this.feederInfoMetas) this.model.addFeederInfo(info);
        this.nodeMetas = [];
        this.wireMetas = [];
        this.feederInfoMetas = [];

        this.emit('element:added', {
            id: this.equipmentId,
            type: this.type,
            voltageLevelId: this.node.vid ?? '',
        });
    }

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: this.kind === 'bay' ? 'delete-bay' : 'delete',
            equipmentType: this.type,
            equipmentId: this.equipmentId,
        };
    }

    private removeFromDom(svgId: string): void {
        const element = this.dom.findElementById(svgId);
        if (!element) return;
        this.domSnapshots.push(this.dom.removeAndSnapshot(element));
    }
}

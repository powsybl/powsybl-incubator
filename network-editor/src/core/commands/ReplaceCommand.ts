import type { Command } from './Command';
import type { EditorModel } from '../EditorModel';
import type { SvgDomService, SymbolSnapshot } from '../../dom/SvgDomService';
import {
    nodeComponentType,
    type ChangeSetEntry,
    type ElementType,
    type EquipmentProperties,
    type NodeMetadata,
    type SymbolProvider,
} from '../types';

export class ReplaceCommand implements Command {
    private readonly oldComponentType: string;
    private readonly oldProperties: EquipmentProperties;
    private snapshot: SymbolSnapshot | undefined;

    constructor(
        readonly equipmentId: string,
        readonly newId: string,
        private readonly type: ElementType,
        private readonly properties: EquipmentProperties,
        private readonly node: NodeMetadata,
        private readonly model: EditorModel,
        private readonly dom: SvgDomService,
        private readonly symbols: SymbolProvider,
        private readonly onRenamed: (oldId: string, newId: string) => void,
    ) {
        this.oldComponentType = node.componentType;
        this.oldProperties = model.getProperties(equipmentId);
    }

    execute(): void {
        const componentType = nodeComponentType(this.type, this.properties);
        const symbol = this.symbols.createSymbol(componentType, {
            orientation: this.node.direction === 'TOP' ? 'UP' : 'DOWN',
        });
        this.snapshot = this.dom.swapSymbol(
            this.node.id,
            symbol,
            this.symbols.componentSize(this.oldComponentType),
            this.symbols.componentSize(componentType),
        );

        this.rename(this.equipmentId, this.newId);
        this.model.setComponentType(this.node.id, componentType);
        this.model.replaceProperties(this.newId, this.properties);
    }

    undo(): void {
        if (this.snapshot) this.dom.restoreSymbol(this.snapshot);
        this.snapshot = undefined;

        this.model.setComponentType(this.node.id, this.oldComponentType);
        this.model.replaceProperties(this.newId, this.oldProperties);
        this.rename(this.newId, this.equipmentId);
    }

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'replace',
            equipmentId: this.equipmentId,
            payload: { equipmentType: this.type, newId: this.newId, properties: this.properties },
        };
    }

    private rename(from: string, to: string): void {
        if (from === to) return;
        this.model.renameEquipment(from, to);
        this.onRenamed(from, to);
    }
}

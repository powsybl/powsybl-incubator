import type { Command } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import { createFeederBay, createInjection, removal, type NetworkChange } from '../changes/changes.ts';
import type { SvgDomService, SymbolSnapshot } from '../dom/SvgDomService.ts';
import {
    nodeComponentType,
    toDirection,
    toElementType,
    type ElementType,
    type EquipmentProperties,
    type NodeMetadata,
    type SymbolProvider,
} from '../types.ts';

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

    /**
     * A bay on a busbar is removed with its switches and created again; an injection on a bare node is
     * replaced alone, so that the node and its switches stay.
     */
    toChanges(): NetworkChange[] {
        const oldType = toElementType(this.oldComponentType);
        const voltageLevelId = this.node.vid ?? '';
        const busbarSectionId = this.model.busbarSectionOfBay(this.node);
        if (busbarSectionId === undefined && this.node.iidmNode !== undefined) {
            return [
                removal(this.equipmentId, oldType, false),
                createInjection(this.type, this.newId, voltageLevelId, this.node.iidmNode, this.properties),
            ];
        }
        const placement = {
            voltageLevelId,
            busbarSectionId: busbarSectionId ?? '',
            order: this.node.order,
            direction: toDirection(this.node.direction),
        };
        return [
            removal(this.equipmentId, oldType, true),
            createFeederBay(this.type, this.newId, placement, this.properties),
        ];
    }

    private rename(from: string, to: string): void {
        if (from === to) return;
        this.model.renameEquipment(from, to);
        this.onRenamed(from, to);
    }
}

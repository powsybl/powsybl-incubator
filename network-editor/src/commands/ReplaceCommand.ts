import type { Command } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import {
    identifiableType,
    injectionCreation,
    nodeInjectionCreation,
    type NetworkModification,
} from '../modifications/modifications.ts';
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

    toModifications(): NetworkModification[] {
        const deletion: NetworkModification = {
            type: 'EQUIPMENT_DELETION',
            equipmentId: this.equipmentId,
            equipmentType: identifiableType(toElementType(this.oldComponentType)),
        };
        const busbarSectionId = this.model.busbarSectionOfBay(this.node);
        const creation =
            busbarSectionId === undefined && this.node.iidmNode !== undefined
                ? nodeInjectionCreation(this.type, this.newId, this.node.vid ?? '', this.node.iidmNode, this.properties)
                : injectionCreation(
                      this.type,
                      this.newId,
                      {
                          voltageLevelId: this.node.vid ?? '',
                          busbarSectionId: busbarSectionId ?? '',
                          order: this.node.order ?? null,
                          direction: toDirection(this.node.direction),
                      },
                      this.properties,
                  );
        return [deletion, creation];
    }

    private rename(from: string, to: string): void {
        if (from === to) return;
        this.model.renameEquipment(from, to);
        this.onRenamed(from, to);
    }
}

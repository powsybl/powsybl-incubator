import type { Command } from './Command';
import type { EditorModel } from '../EditorModel';
import type { ElementType, EquipmentProperties } from '../types';
import { equipmentModification, type NetworkModification } from '../modifications';

export class UpdatePropertiesCommand implements Command {
    private previous: EquipmentProperties = {};

    private readonly changes: EquipmentProperties;

    constructor(
        readonly equipmentId: string,
        private readonly type: ElementType,
        changes: EquipmentProperties,
        private readonly model: EditorModel,
        private readonly onChanged: (equipmentId: string, changes: EquipmentProperties) => void,
    ) {
        this.changes = { ...changes };
    }

    execute(): void {
        const current = this.model.getProperties(this.equipmentId);
        this.previous = Object.fromEntries(
            Object.keys(this.changes).map((key) => [key, current[key]]),
        );
        this.model.mergeProperties(this.equipmentId, this.changes);
        this.onChanged(this.equipmentId, this.changes);
    }

    undo(): void {
        this.model.mergeProperties(this.equipmentId, this.previous);
        this.onChanged(this.equipmentId, this.previous);
    }

    toModifications(): NetworkModification[] {
        return [equipmentModification(this.type, this.equipmentId, this.changes)];
    }
}

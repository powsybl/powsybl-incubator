import type { Command } from './Command';
import type { EditorModel } from '../EditorModel';
import type { ChangeSetEntry, ElementType, EquipmentProperties } from '../types';

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
        this.model.setProperties(this.equipmentId, this.changes);
        this.onChanged(this.equipmentId, this.changes);
    }

    undo(): void {
        this.model.setProperties(this.equipmentId, this.previous);
        this.onChanged(this.equipmentId, this.previous);
    }

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'update',
            equipmentId: this.equipmentId,
            equipmentType: this.type,
            payload: { ...this.changes },
        };
    }
}

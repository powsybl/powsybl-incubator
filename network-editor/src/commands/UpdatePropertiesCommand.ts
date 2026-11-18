import type { Command } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import type { ElementType, EquipmentProperties } from '../types.ts';
import { updateAttributes, type NetworkChange } from '../changes/changes.ts';

export class UpdatePropertiesCommand implements Command {
    private previous: EquipmentProperties = {};

    private readonly changes: EquipmentProperties;

    constructor(
        readonly equipmentId: string,
        private readonly type: ElementType,
        changes: EquipmentProperties,
        private readonly model: EditorModel,
        private readonly onChanged: (equipmentId: string, changes: EquipmentProperties) => void,
        private readonly fields: Readonly<Record<string, unknown>> = {},
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

    toChanges(): NetworkChange[] {
        return [updateAttributes(this.type, this.equipmentId, this.changes, this.fields)];
    }
}

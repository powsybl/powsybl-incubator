import type { Command } from './Command';
import type { EditorModel } from '../EditorModel';
import type {
    ChangeSetEntry,
    EditorEventListener,
    ElementType,
    EquipmentProperties,
} from '../types';

export class UpdatePropertiesCommand implements Command {
    readonly label: string;
    private previous: EquipmentProperties = {};

    constructor(
        private readonly equipmentId: string,
        private readonly type: ElementType,
        private readonly changes: EquipmentProperties,
        private readonly model: EditorModel,
        private readonly emit: EditorEventListener,
    ) {
        this.label = `Update ${type} ${equipmentId}`;
        this.changes = {...changes}
    }

    execute(): void {
        const current = this.model.getProperties(this.equipmentId);
        this.previous = Object.fromEntries(
            Object.keys(this.changes).map((key) => [key, current[key]]),
        );
        this.model.setProperties(this.equipmentId, this.changes);
        this.emit('properties:changed', { id: this.equipmentId, changes: this.changes });
    }

    undo(): void {
        this.model.setProperties(this.equipmentId, this.previous);
        this.emit('properties:changed', { id: this.equipmentId, changes: this.previous });
    }

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'update',
            equipmentType: this.type,
            equipmentId: this.equipmentId,
            payload: { ...this.changes },
        };
    }
}

import type { Command } from './Command';
import type {
    ChangeSetEntry,
    ConnectionPoint,
    EditorEventListener,
    ElementType,
    EquipmentProperties,
} from '../types';

export class CreateEquipmentCommand implements Command {
    readonly label: string;

    constructor(
        private readonly equipmentId: string,
        private readonly type: ElementType,
        private readonly point: ConnectionPoint,
        private readonly properties: EquipmentProperties,
        private readonly emit: EditorEventListener,
    ) {
        this.label = `Create ${this.type} ${this.equipmentId}`;
    }

    execute(): void {
        this.emit('element:created', {
            id: this.equipmentId,
            type: this.type,
            anchor: this.point.anchor,
        });
    }

    undo(): void {
        // Emitting `element:removed` ?
    }

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'create',
            equipmentType: this.type,
            equipmentId: this.equipmentId,
            payload: {
                pointId: this.point.id,
                anchor: this.point.anchor,
                properties: this.properties,
            },
        };
    }
}

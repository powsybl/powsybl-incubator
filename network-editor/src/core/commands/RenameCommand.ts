import type { Command } from './Command';
import type { EditorModel } from '../EditorModel';
import type { ChangeSetEntry } from '../types';

export class RenameCommand implements Command {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string };

    constructor(
        readonly equipmentId: string,
        private readonly newId: string,
        private readonly model: EditorModel,
        private readonly onRenamed: (oldId: string, newId: string) => void,
        markerNodeId: string,
    ) {
        this.pendingMarker = { targetId: newId, nodeId: markerNodeId, label: `✎ ${newId}` };
    }

    execute(): void {
        this.model.renameEquipment(this.equipmentId, this.newId);
        this.onRenamed(this.equipmentId, this.newId);
    }

    undo(): void {
        this.model.renameEquipment(this.newId, this.equipmentId);
        this.onRenamed(this.newId, this.equipmentId);
    }

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'rename',
            equipmentId: this.equipmentId,
            payload: { newId: this.newId },
        };
    }
}

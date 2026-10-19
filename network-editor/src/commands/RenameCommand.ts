import type { Command } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import type { NetworkModification } from '../modifications/modifications.ts';

export class RenameCommand implements Command {
    constructor(
        readonly equipmentId: string,
        readonly newId: string,
        private readonly model: EditorModel,
        private readonly onRenamed: (oldId: string, newId: string) => void,
    ) {}

    execute(): void {
        this.model.renameEquipment(this.equipmentId, this.newId);
        this.onRenamed(this.equipmentId, this.newId);
    }

    undo(): void {
        this.model.renameEquipment(this.newId, this.equipmentId);
        this.onRenamed(this.newId, this.equipmentId);
    }

    toModifications(): NetworkModification[] {
        return [
            {
                type: 'EQUIPMENT_RENAME',
                equipmentId: this.equipmentId,
                newEquipmentId: this.newId,
                newConnectionName: this.newId,
            },
        ];
    }
}

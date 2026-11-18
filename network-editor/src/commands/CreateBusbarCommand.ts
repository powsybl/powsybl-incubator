import type { Command } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import { createBusbarSection, type NetworkChange } from '../changes/changes.ts';

export class CreateBusbarCommand implements Command {
    constructor(
        readonly equipmentId: string,
        readonly vlId: string,
        readonly busbarIndex: number,
        private readonly model: EditorModel,
    ) {}

    execute(): void {
        this.model.reserveId(this.equipmentId);
    }

    undo(): void {
        this.model.releaseId(this.equipmentId);
    }

    toChanges(): NetworkChange[] {
        return [createBusbarSection(this.equipmentId, this.vlId, this.busbarIndex)];
    }
}

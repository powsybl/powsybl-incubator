import type { Command } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import type { NetworkModification } from '../modifications/modifications.ts';

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

    toModifications(): NetworkModification[] {
        return [
            {
                type: 'BUSBAR_CREATION',
                equipmentId: this.equipmentId,
                equipmentName: null,
                voltageLevelId: this.vlId,
                busbarIndex: this.busbarIndex,
                sectionIndex: 1,
            },
        ];
    }
}

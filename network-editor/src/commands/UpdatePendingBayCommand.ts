import type { Command } from './Command.ts';
import { moveFeederBay, type NetworkModification } from '../modifications/modifications.ts';
import type { BayPosition, BusbarTarget } from '../types.ts';

export class UpdatePendingBayCommand implements Command {
    constructor(
        readonly equipmentId: string,
        readonly position: BayPosition,
        private readonly busbar: BusbarTarget,
    ) {}

    execute(): void {}

    undo(): void {}

    toModifications(): NetworkModification[] {
        return [
            moveFeederBay(
                this.busbar.vlId,
                this.equipmentId,
                this.busbar.busbarSectionId,
                this.position.order,
                this.position.direction,
                null,
            ),
        ];
    }
}

import type { Command } from './Command.ts';
import type { EditorModel } from '../model/EditorModel.ts';
import { moveFeederBay, type NetworkModification } from '../modifications/modifications.ts';
import { toDirection, type BayPosition, type BusbarTarget } from '../types.ts';

export class UpdatePendingBayCommand implements Command {
    private previous: { order?: number; direction?: string } = {};

    constructor(
        readonly equipmentId: string,
        private readonly nodeId: string,
        private readonly position: BayPosition,
        private readonly busbar: BusbarTarget,
        private readonly model: EditorModel,
    ) {}

    execute(): void {
        const node = this.model.getNodeById(this.nodeId);
        this.previous = { order: node?.order, direction: node?.direction };
        this.model.setFeederOrder(this.nodeId, this.position.order);
        this.model.setFeederDirection(this.nodeId, this.position.direction);
    }

    undo(): void {
        this.model.setFeederOrder(this.nodeId, this.previous.order);
        const direction = toDirection(this.previous.direction);
        if (direction) this.model.setFeederDirection(this.nodeId, direction);
    }

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

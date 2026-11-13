import type { Command } from './Command.ts';
import type { NetworkModification } from '../modifications/modifications.ts';
import type { BusbarTarget } from '../types.ts';

let nextPreview = 0;

/**
 * A coupling between two busbar sections. powsybl names the devices it creates, so the command holds
 * no model state: it only stands in the history and draws its preview.
 */
export class CreateCouplingCommand implements Command {
    readonly previewId = `ne-coupling-${nextPreview++}`;

    /** No equipment is named by the editor: the preview stands for the coupling. */
    readonly equipmentId = this.previewId;

    constructor(
        readonly first: BusbarTarget,
        readonly second: BusbarTarget,
    ) {}

    execute(): void {}

    undo(): void {}

    toModifications(): NetworkModification[] {
        return [
            {
                type: 'CREATE_COUPLING_DEVICE',
                voltageLevelId: this.first.vlId,
                couplingDeviceInfos: {
                    busbarSectionId1: this.first.busbarSectionId,
                    busbarSectionId2: this.second.busbarSectionId,
                },
            },
        ];
    }
}

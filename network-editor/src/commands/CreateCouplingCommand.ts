import type { Command } from './Command.ts';
import { createCouplingDevice, type NetworkChange } from '../changes/changes.ts';
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

    toChanges(): NetworkChange[] {
        return [createCouplingDevice(this.first.vlId, this.first.busbarSectionId, this.second.busbarSectionId)];
    }
}

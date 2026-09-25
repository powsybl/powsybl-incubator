import type { Command } from './Command';
import type { EditorModel } from '../EditorModel';
import type { CellFlip, SvgDomService } from '../../dom/SvgDomService';
import type {
    BayPosition,
    BaySlot,
    ChangeSetEntry,
    EquipmentTarget,
    FeederDirection,
    NodeMetadata,
    OrderClaim,
} from '../types';

export interface BayTurn {
    forth: CellFlip;
    back: CellFlip;
    feederNodeIds: readonly string[];
    from: FeederDirection;
}

export interface BayShift {
    dx: number;
    previousDx: number;
    x?: number;
}

export class UpdateBayPositionCommand implements Command {

    readonly orderClaims: readonly OrderClaim[];

    private readonly nodeId: string;

    constructor(
        private readonly feeder: EquipmentTarget,
        node: NodeMetadata,
        slot: BaySlot,
        private readonly position: BayPosition,
        private readonly offset: BayShift,
        private readonly dom: SvgDomService,
        private readonly model: EditorModel,
        private readonly turn?: BayTurn,
    ) {
        this.nodeId = node.id;
        this.orderClaims = [{ ...slot, order: position.order, vacatedNodeId: node.id, x: offset.x }];
    }

    get equipmentId(): string {
        return this.feeder.equipmentId;
    }

    get bayPosition(): BayPosition {
        return this.position;
    }

    get shift(): number {
        return this.offset.dx;
    }

    execute(): void {
        this.dom.shiftBay(this.nodeId, this.offset.dx);
        if (!this.turn) return;

        this.dom.flipCell(this.turn.forth);
        for (const nodeId of this.turn.feederNodeIds) {
            this.model.setFeederDirection(nodeId, this.position.direction);
        }
    }

    undo(): void {
        this.dom.shiftBay(this.nodeId, this.offset.previousDx);
        if (!this.turn) return;

        this.dom.flipCell(this.turn.back);
        for (const nodeId of this.turn.feederNodeIds) {
            this.model.setFeederDirection(nodeId, this.turn.from);
        }
    }

    toChangeSetEntry(): ChangeSetEntry {
        return {
            op: 'update-position',
            equipmentId: this.equipmentId,
            payload: {
                node: this.feeder.node!,
                order: this.position.order,
                direction: this.position.direction,
            },
        };
    }
}

import type { Command } from './Command';
import type { SvgDomService } from '../../dom/SvgDomService';
import type {
    BayPosition,
    BaySlot,
    ChangeSetEntry,
    EquipmentTarget,
    NodeMetadata,
    OrderClaim,
} from '../types';

export class UpdateBayPositionCommand implements Command {
    readonly pendingMarker: { targetId: string; nodeId: string; label: string };

    readonly orderClaim: OrderClaim;

    private readonly nodeId: string;

    private readonly dx: number;

    constructor(
        private readonly feeder: EquipmentTarget,
        node: NodeMetadata,
        slot: BaySlot,
        private readonly position: BayPosition,
        private readonly dom: SvgDomService,
        toX?: number,
    ) {
        this.nodeId = node.id;
        this.dx = toX === undefined ? 0 : toX - (dom.getDiagramX(node.id) ?? toX);
        this.pendingMarker = {
            targetId: feeder.id,
            nodeId: node.id,
            label: `↕ ${position.order} ${position.direction}`,
        };
        this.orderClaim = { ...slot, order: position.order, vacatedNodeId: node.id };
    }

    get equipmentId(): string {
        return this.feeder.equipmentId;
    }

    execute(): void {
        this.dom.shiftBay(this.nodeId, this.dx);
    }

    undo(): void {
        this.dom.shiftBay(this.nodeId, 0);
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

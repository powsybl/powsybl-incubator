import type { ChangeSetEntry, OrderClaim } from '../types';

export interface Command {
    execute(): void;
    undo(): void;
    toChangeSetEntry(): ChangeSetEntry;
    readonly pendingMarker?: { targetId: string; nodeId: string; label: string };
    readonly orderClaim?: OrderClaim;
    readonly equipmentId: string;
}

import type { CreateSpec, OrderClaim } from '../types.ts';
import type { NetworkChange } from '../changes/changes.ts';

export interface Command {
    execute(): void;
    undo(): void;
    toChanges(): NetworkChange[];
    readonly pendingMarker?: {
        targetId: string;
        nodeId: string;
        label: string;
        elementId?: string;
    };
    readonly orderClaims?: readonly OrderClaim[];
    readonly equipmentId: string;
}

interface PendingCreate {
    readonly createSpec: CreateSpec;
}

export type PendingCreateCommand = Command & PendingCreate;

export function isPendingCreate(command: Command): command is PendingCreateCommand {
    return 'createSpec' in command;
}

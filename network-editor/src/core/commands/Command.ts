import type { CreateSpec, OrderClaim } from '../types';
import type { NetworkModification } from '../modifications';

export interface Command {
    execute(): void;
    undo(): void;
    toModifications(): NetworkModification[];
    readonly pendingMarker?: {
        targetId: string;
        nodeId: string;
        label: string;
        elementId?: string;
    };
    readonly orderClaims?: readonly OrderClaim[];
    readonly equipmentId: string;
}

export interface PendingCreate {
    readonly createSpec: CreateSpec;
    withSpec(spec: CreateSpec): PendingCreateCommand;
}

export type PendingCreateCommand = Command & PendingCreate;

export function isPendingCreate(command: Command): command is PendingCreateCommand {
    return typeof (command as Partial<PendingCreate>).withSpec === 'function';
}

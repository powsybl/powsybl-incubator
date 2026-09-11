import type { ChangeSetEntry, CreateSpec, OrderClaim } from '../types';

export interface Command {
    execute(): void;
    undo(): void;
    toChangeSetEntry(): ChangeSetEntry;
    readonly pendingMarker?: {
        targetId: string;
        nodeId: string;
        label: string;
        elementId?: string;
    };
    readonly orderClaim?: OrderClaim;
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

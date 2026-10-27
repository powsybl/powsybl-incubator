import { isPendingCreate, type Command, type PendingCreateCommand } from './Command.ts';
import { DeleteElementCommand } from './DeleteElementCommand.ts';
import { RenameCommand } from './RenameCommand.ts';
import { UpdateBayPositionCommand } from './UpdateBayPositionCommand.ts';
import { UpdatePendingBayCommand } from './UpdatePendingBayCommand.ts';
import { UpdatePropertiesCommand } from './UpdatePropertiesCommand.ts';
import type { CreateSpec, OrderClaim, PendingOrders } from '../types.ts';

export interface PendingCreation {
    command: PendingCreateCommand;
    spec: CreateSpec;
}

export function pendingCreations(pending: readonly Command[]): Map<string, PendingCreation> {
    const creations = new Map<string, PendingCreation>();

    for (const command of pending) {
        if (isPendingCreate(command)) {
            creations.set(command.equipmentId, { command, spec: command.createSpec });
            continue;
        }
        const creation = creations.get(command.equipmentId);
        if (!creation) continue;

        if (command instanceof RenameCommand) {
            creations.delete(command.equipmentId);
            creation.spec.provisionalId = command.newId;
            creations.set(command.newId, creation);
        } else if (command instanceof UpdatePropertiesCommand) {
            creation.spec.properties = { ...creation.spec.properties, ...command.changes };
        } else if (command instanceof UpdatePendingBayCommand) {
            creation.spec.order = command.position.order;
            creation.spec.direction = command.position.direction;
        } else if (command instanceof DeleteElementCommand) {
            creations.delete(command.equipmentId);
        }
    }
    return creations;
}

export function pendingBayPosition(
    pending: readonly Command[],
    equipmentId: string,
): UpdateBayPositionCommand | undefined {
    return pending.findLast(
        (command): command is UpdateBayPositionCommand =>
            command instanceof UpdateBayPositionCommand && command.equipmentId === equipmentId,
    );
}

export function renameHistory(pending: readonly Command[], currentId: string): string[] {
    const ids = [currentId];
    for (const command of [...pending].reverse()) {
        if (command instanceof RenameCommand && command.newId === ids[ids.length - 1]) {
            ids.push(command.equipmentId);
        }
    }
    return ids;
}

export function isPendingId(pending: readonly Command[], equipmentId: string): boolean {
    return pending.some((command) => command.equipmentId === equipmentId);
}

export function pendingScope(pending: readonly Command[]): { created: Set<string>; claimed: Set<string> } {
    const created = new Set(pendingCreations(pending).keys());
    const claimed = new Set<string>();

    for (const command of pending) {
        if (command.pendingMarker) claimed.add(command.pendingMarker.targetId);
    }
    return { created, claimed };
}

export function pendingOrders(
    pending: readonly Command[],
    vlId: string,
    vacating?: string,
    ignore?: Command,
): PendingOrders {
    const claims: OrderClaim[] = [];
    const vacated = new Set<string>(vacating ? [vacating] : []);

    const lastClaims = new Map<string, OrderClaim>();
    const creations = new Map<Command, CreateSpec>(
        [...pendingCreations(pending).values()].map(({ command, spec }) => [command, spec]),
    );

    for (const command of pending) {
        if (command === ignore) continue;
        if (isPendingCreate(command) && !creations.has(command)) continue;

        for (const claim of command.orderClaims ?? []) {
            if (claim.vlId !== vlId) continue;
            if (!claim.vacatedNodeId) {
                claims.push({ ...claim, order: creations.get(command)?.order ?? claim.order });
                continue;
            }
            vacated.add(claim.vacatedNodeId);

            if (claim.vacatedNodeId !== vacating) lastClaims.set(claim.vacatedNodeId, claim);
        }
    }
    return { claims: [...claims, ...lastClaims.values()], vacated };
}

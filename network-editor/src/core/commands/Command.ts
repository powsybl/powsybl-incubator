import type { ChangeSetEntry } from '../types';

export interface Command {
    readonly label: string;
    execute(): void;
    undo(): void;
    toChangeSetEntry(): ChangeSetEntry;
}

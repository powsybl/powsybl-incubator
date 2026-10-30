import type { Command } from './Command.ts';

type Step = readonly Command[];

export class CommandStack {
    private readonly undoStack: Step[] = [];
    private readonly redoStack: Step[] = [];

    constructor(private readonly onChange: (state: { canUndo: boolean; canRedo: boolean }) => void) {}

    push(command: Command): void {
        this.pushAll([command]);
    }

    pushAll(commands: readonly Command[]): void {
        if (commands.length === 0) return;
        for (const command of commands) command.execute();
        this.undoStack.push(commands);
        this.redoStack.length = 0;
        this.notify();
    }

    undo(): void {
        const step = this.undoStack.pop();
        if (!step) return;
        for (const command of [...step].reverse()) command.undo();
        this.redoStack.push(step);
        this.notify();
    }

    redo(): void {
        const step = this.redoStack.pop();
        if (!step) return;
        for (const command of step) command.execute();
        this.undoStack.push(step);
        this.notify();
    }

    clear(): void {
        if (this.undoStack.length === 0 && this.redoStack.length === 0) return;
        this.undoStack.length = 0;
        this.redoStack.length = 0;
        this.notify();
    }

    get canUndo(): boolean { return this.undoStack.length > 0; }
    get canRedo(): boolean { return this.redoStack.length > 0; }

    get pending(): readonly Command[] {
        return this.undoStack.flat();
    }

    private notify(): void {
        this.onChange({ canUndo: this.canUndo, canRedo: this.canRedo });
    }
}

import type { Command } from './Command';

type Step = readonly Command[];

export class CommandStack {
    private readonly undoStack: Step[] = [];
    private readonly redoStack: Step[] = [];

    constructor(
        private readonly onChange: (state: { canUndo: boolean; canRedo: boolean }) => void,
    ) {}

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

    replace(previous: Command, next: Command): boolean {
        const at = this.locate(previous);
        if (!at) return false;

        previous.undo();
        next.execute();
        const step = [...this.undoStack[at.step]];
        step[at.index] = next;
        this.undoStack[at.step] = step;
        this.redoStack.length = 0;
        this.notify();
        return true;
    }

    remove(command: Command): boolean {
        const at = this.locate(command);
        if (!at) return false;

        command.undo();
        const step = this.undoStack[at.step].filter((_, index) => index !== at.index);
        if (step.length === 0) this.undoStack.splice(at.step, 1);
        else this.undoStack[at.step] = step;
        this.redoStack.length = 0;
        this.notify();
        return true;
    }

    get canUndo(): boolean { return this.undoStack.length > 0; }
    get canRedo(): boolean { return this.redoStack.length > 0; }

    get pending(): readonly Command[] {
        return this.undoStack.flat();
    }

    private locate(command: Command): { step: number; index: number } | undefined {
        for (const [step, commands] of this.undoStack.entries()) {
            const index = commands.indexOf(command);
            if (index !== -1) return { step, index };
        }
        return undefined;
    }

    private notify(): void {
        this.onChange({ canUndo: this.canUndo, canRedo: this.canRedo });
    }
}

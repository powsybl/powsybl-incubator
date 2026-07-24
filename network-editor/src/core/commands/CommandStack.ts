import type { Command } from './Command';


export class CommandStack {
    private readonly undoStack: Command[] = [];
    private readonly redoStack: Command[] = [];

    constructor(
        private readonly onChange: (state: { canUndo: boolean; canRedo: boolean }) => void,
    ) {}

    push(command: Command): void {
        command.execute();
        this.undoStack.push(command);
        this.redoStack.length = 0;
        this.notify();
    }

    undo(): void {
        const command = this.undoStack.pop();
        if (!command) return;
        command.undo();
        this.redoStack.push(command);
        this.notify();
    }

    redo(): void {
        const command = this.redoStack.pop();
        if (!command) return;
        command.execute();
        this.undoStack.push(command);
        this.notify();
    }

    get canUndo(): boolean { return this.undoStack.length > 0; }
    get canRedo(): boolean { return this.redoStack.length > 0; }

    get pending(): readonly Command[] {
        return this.undoStack;
    }

    clear(): void {
        if (this.undoStack.length === 0 && this.redoStack.length === 0) return;
        this.undoStack.length = 0;
        this.redoStack.length = 0;
        this.notify();
    }

    private notify(): void {
        this.onChange({ canUndo: this.canUndo, canRedo: this.canRedo });
    }
}

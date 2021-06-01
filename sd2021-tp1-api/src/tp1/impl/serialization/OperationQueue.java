package tp1.impl.serialization;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OperationQueue {

    List<Operation> history;
    Queue<Operation> queue;

    public OperationQueue() {
        this.history = new LinkedList<>();
        this.queue = new LinkedList<>();
    }

    public void addOperation(Operation operation) {
        this.queue.add(operation);
    }

    public Operation execute() {
        Operation operation = this.queue.remove();
        this.history.add(operation);
        return operation;
    }

    public List<Operation> getHistory(int nOperation) {
        return this.history.subList(nOperation, this.history.size());
    }

}

package tp1.impl.serialization;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OperationQueue {

    // String is the encoding of SheetsOperation
    List<String> history;
    Queue<SheetsOperation> queue;

    public OperationQueue() {
        this.history = new LinkedList<>();
        this.queue = new LinkedList<>();
    }

    public synchronized void addToHistory(String operationEncoding) {
        this.history.add(operationEncoding);
    }

//    public synchronized void addToQueue(SheetsOperation operation) {
//        this.queue.add(operation);
//    }
//
//    public SheetsOperation execute() {
//        SheetsOperation operation = this.queue.remove();
//        this.history.add(operation);
//        return operation;
//    }

    public List<String> getHistory(int nOperation) {
        return this.history.subList(nOperation, this.history.size());
    }

}

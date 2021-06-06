package tp1.impl.serialization;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

public class OperationQueue {

    // String is the encoding of SheetsOperation
    List<String> history;
    PriorityQueue<SheetsOperation> queue;

    public OperationQueue() {
        this.history = new LinkedList<>();
        this.queue = new PriorityQueue<>(new Comparator<SheetsOperation>() {
            @Override
            public int compare(SheetsOperation o1, SheetsOperation o2) {
                return (int) (o1.getVersion() - o2.getVersion());
            }
        });
    }

    public synchronized void addToHistory(String operationEncoding) {
        this.history.add(operationEncoding);
    }

    public void enqueue(SheetsOperation operation) {
        this.queue.add(operation);
    }

    public String getNextOperation() {
        try {
            return this.queue.poll().encode();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Long peekQueue() {
        try {
            return this.queue.peek().getVersion();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public List<String> getHistory(int nOperation) {
        return this.history.subList(nOperation, this.history.size());
    }

}

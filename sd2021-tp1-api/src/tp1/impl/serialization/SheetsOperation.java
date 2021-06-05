package tp1.impl.serialization;

import tp1.impl.util.encoding.JSON;

public class SheetsOperation {

    public enum Operation {
        Create,
        Delete,
        DeleteUserSheets,
        UpdateCell,
        Share,
        Unshare
    }

    private static final String DELIMITER = "\t";
    public final Operation type;
    private final String jsonArgs;

    public SheetsOperation(Operation type, Object args) {
        this.type = type;
        this.jsonArgs = JSON.encode(args);
    }

    public SheetsOperation(String encoding) {
        String[] tokens = encoding.split(DELIMITER);
        this.type = Operation.valueOf(tokens[0]);
        this.jsonArgs = tokens[1];
    }

    public Operation getType() {
        return this.type;
    }

    public String encode() {
        return new StringBuilder(type.name())
                .append(DELIMITER)
                .append(jsonArgs).toString();
    }

    public <T> T args(Class<T> classOf) {
        return JSON.decode(jsonArgs, classOf);
    }
}

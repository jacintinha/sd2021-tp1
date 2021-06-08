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
    private final Long version;
    private final Operation type;
    private final String jsonArgs;

    public SheetsOperation(Operation type, Long version, Object args) {
        this.version = version;
        this.type = type;
        this.jsonArgs = JSON.encode(args);
    }

    public SheetsOperation(String encoding) {
        String[] tokens = encoding.split(DELIMITER);
        this.version = Long.parseLong(tokens[0]);
        this.type = Operation.valueOf(tokens[1]);
        this.jsonArgs = tokens[2];
    }

    public Operation getType() {
        return this.type;
    }

    public Long getVersion() {
        return this.version;
    }

    public String encode() {
        return new StringBuilder(version.toString())
                .append(DELIMITER)
                .append(type.name())
                .append(DELIMITER)
                .append(jsonArgs).toString();
    }

    public <T> T args(Class<T> classOf) {
        return JSON.decode(jsonArgs, classOf);
    }
}

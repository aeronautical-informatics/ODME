package ses;

public enum VariableType {
    BOOLEAN, INT, FLOAT, STRING;

    public boolean isNumeric() {
        return this == INT || this == FLOAT;
    }
}

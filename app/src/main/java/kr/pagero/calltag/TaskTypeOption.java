package kr.pagero.calltag;

public final class TaskTypeOption {
    public final long id;
    public final String code;
    public final String name;
    public final String color;
    public final boolean defaultType;

    public TaskTypeOption(long id, String code, String name, String color, boolean defaultType) {
        this.id = id;
        this.code = code == null ? "" : code;
        this.name = name == null ? "" : name;
        this.color = color == null ? "#4389FF" : color;
        this.defaultType = defaultType;
    }
}

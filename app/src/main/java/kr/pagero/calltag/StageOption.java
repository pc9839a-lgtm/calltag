package kr.pagero.calltag;

public final class StageOption {
    public final long id;
    public final String name;
    public final int position;
    public final String color;

    public StageOption(long id, String name, int position, String color) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.color = color == null || color.trim().isEmpty() ? "#4389FF" : color.trim();
    }
}

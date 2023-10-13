package input;

public enum MouseButton {
    BUTTON_1      (0),
    BUTTON_2      (1),
    BUTTON_3      (2),
    BUTTON_4      (3),
    BUTTON_5      (4),
    BUTTON_6      (5),
    BUTTON_7      (6),
    BUTTON_8      (7),
    LEFT    (BUTTON_1.getId()),
    RIGHT   (BUTTON_2.getId()),
    MIDDLE  (BUTTON_3.getId());

    public enum State {
        JUST_PRESSED,
        PRESSED,
        JUST_RELEASED,
        RELEASED
    }

    private final int id;
    private State state;

    MouseButton(int id) {
        this.id = id;
        state = State.RELEASED;
    }

    public int getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}

package input;

import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public class MouseInput {
    public enum CursorMode {
        NORMAL(GLFW_CURSOR_NORMAL),
        LIMITED(GLFW_CURSOR_DISABLED),
        HIDDEN(GLFW_CURSOR_HIDDEN);

        final int value;
        CursorMode(int value) {
            this.value = value;
        }
    }

    // TODO: Add a field that stores the cursor texture.

    private static long window;

    private static Vector2f position = new Vector2f();
    private static Vector2f deltaPosition = new Vector2f();
    private static CursorMode cursorMode = CursorMode.NORMAL;

    public static void init(long window) {
        MouseInput.window = window;
    }

    public static void updateCursorPosition(long window, double x, double y) {
        Vector2f newPosition = new Vector2f((float)x, (float)y);
        deltaPosition = new Vector2f(newPosition).sub(position);
        position = newPosition;
    }

    public static MouseButton.State buttonState(MouseButton button) {
        int currentState = glfwGetMouseButton(window, button.getId());
        MouseButton.State lastState = button.getState();

        if(currentState == GLFW_PRESS) {
            if(lastState == MouseButton.State.RELEASED || lastState == MouseButton.State.JUST_RELEASED)
                button.setState(MouseButton.State.JUST_PRESSED);
            else
                button.setState(MouseButton.State.PRESSED);
        }
        else {
            if(lastState == MouseButton.State.PRESSED || lastState == MouseButton.State.JUST_PRESSED)
                button.setState(MouseButton.State.JUST_RELEASED);
            else
                button.setState(MouseButton.State.RELEASED);
        }

        return button.getState();
    }

    public static boolean isPressed(MouseButton mouseButton) {
        return buttonState(mouseButton).equals(MouseButton.State.PRESSED);
    }

    public static boolean isJustPressed(MouseButton mouseButton) {
        return buttonState(mouseButton).equals(MouseButton.State.JUST_PRESSED);
    }

    public static boolean isReleased(MouseButton mouseButton) {
        return buttonState(mouseButton).equals(MouseButton.State.RELEASED);
    }

    public static boolean isJustReleased(MouseButton mouseButton) {
        return buttonState(mouseButton).equals(MouseButton.State.JUST_RELEASED);
    }

    public static Vector2f getPosition() {
        return position;
    }

    public static Vector2f getDeltaPosition() {
        Vector2f delta = deltaPosition;
        deltaPosition = new Vector2f(0);
        return delta;
    }

    public static CursorMode getCursorMode() {
        return cursorMode;
    }

    public static void setCursorMode(CursorMode cursorMode) {
        MouseInput.cursorMode = cursorMode;
        glfwSetInputMode(window, GLFW_CURSOR, cursorMode.value);
    }
}

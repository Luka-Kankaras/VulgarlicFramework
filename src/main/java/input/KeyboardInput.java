package input;

import static org.lwjgl.glfw.GLFW.*;

public class KeyboardInput {
    private static long window;

    public static void init(long window) {
        KeyboardInput.window = window;
    }

    public static Key.State keyState(Key key) {
        int currentState = glfwGetKey(window, key.getId());
        Key.State lastState = key.getState();

        if(currentState == GLFW_PRESS) {
            if(lastState == Key.State.RELEASED || lastState == Key.State.JUST_RELEASED)
                key.setState(Key.State.JUST_PRESSED);
            else
                key.setState(Key.State.PRESSED);
        }
        else {
            if(lastState == Key.State.PRESSED || lastState == Key.State.JUST_PRESSED)
                key.setState(Key.State.JUST_RELEASED);
            else
                key.setState(Key.State.RELEASED);
        }

        return key.getState();
    }

    public static boolean isPressed(Key key) {
        return keyState(key).equals(Key.State.PRESSED);
    }

    public static boolean isJustPressed(Key key) {
        return keyState(key).equals(Key.State.JUST_PRESSED);
    }

    public static boolean isReleased(Key key) {
        return keyState(key).equals(Key.State.RELEASED);
    }

    public static boolean isJustReleased(Key key) {
        return keyState(key).equals(Key.State.JUST_RELEASED);
    }
}

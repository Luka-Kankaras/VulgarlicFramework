package glfw;

import input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

public class Window {

    private final IAppLogic gameLogic;
    private final String title;

    private int width;
    private int height;
    private boolean resized;

    private long handle;

    public Window(String title, int width, int height, IAppLogic gameLogic) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.gameLogic = gameLogic;
    }

    public void run() {
        initializeWindow();
        startLoop();
    }

    private void initializeWindow() {
        // Create error callbacks
        GLFWErrorCallback.createPrint(System.err).set();

        if(!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Check for vulkan support
        if (!glfwVulkanSupported()) {
            throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)");
        }

        // Configure GLFW window hints
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        // Create GLFW window
        handle = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        if(handle == 0) {
            throw new IllegalStateException("Failed to create GLFW window");
        }

        // Set callbacks
        GLFW.glfwSetWindowSizeCallback(handle, this::windowResizeCallback);
        GLFW.glfwSetCursorPosCallback(handle, MouseInput::updateCursorPosition);

        // Get the primary output monitor's video mode
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if(videoMode == null) {
            throw new IllegalStateException("Failed to retrieve primary monitor's video mode");
        }

        // Fullscreen
//        glfwSetWindowMonitor(handle, GLFW.glfwGetPrimaryMonitor(), 0, 0, videoMode.width(), videoMode.height(), videoMode.refreshRate());

        // Set window position at the center of the primary monitor
        GLFW.glfwSetWindowPos(handle, (videoMode.width() - width) / 2, (videoMode.width() - height) / 2);

        // When everything is set up, show the window
        GLFW.glfwShowWindow(handle);
    }

    private void startLoop() {
        gameLogic.init();

        // Game loop
        while(!GLFW.glfwWindowShouldClose(handle)) {
            gameLogic.update();

            GLFW.glfwPollEvents();
        }

        gameLogic.cleanup();

        GLFW.glfwDestroyWindow(handle);

        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    public long getHandle() {
        return handle;
    }

    private void windowResizeCallback(long window, int width, int height) {
        resized = true;
        this.width = width;
        this.height = height;
    }

    public void resetResized() {
        resized = false;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public IAppLogic getGameLogic() {
        return gameLogic;
    }

    public boolean isResized() {
        return resized;
    }

}

package glfw;

public interface IAppLogic {

    // Called once at the beginning of the game loop
    void init();
    // Called each loop iteration
    void update();
    // Called at the end of the program
    void cleanup();
}

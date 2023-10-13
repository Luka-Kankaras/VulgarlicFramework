package main;

import core.model.Model;
import core.model.ModelLoader;
import core.texture.Texture;
import glfw.IAppLogic;
import glfw.Window;
import graphics.Renderer;
import input.Key;
import input.KeyboardInput;
import input.MouseButton;
import input.MouseInput;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import scene.Camera;
import scene.SceneManager;

import java.util.ArrayList;
import java.util.List;

public class Main implements IAppLogic {
    private static Window window;
    private static Renderer renderer;
    private static Camera camera;

    private static final float MOUSE_SENSITIVITY = 0.4f;
    private static final float MOVEMENT_SPEED = 0.2f;

    private static final List<Model> models = new ArrayList<>();

    public static void main(String[] args) {
        EngineProperties.init("engine.properties");

        window = new Window(
                EngineProperties.get("window_title", "Vulgarlic Framework"),
                EngineProperties.getI("window_width", "1280"),
                EngineProperties.getI("window_height", "720"),
                new Main());
        window.run();
    }

    @Override
    public void init() {
        // TODO: Models don't belong in the folder they are in

        renderer = new Renderer(window);
        KeyboardInput.init(window.getHandle());
        MouseInput.init(window.getHandle());

        camera = new Camera();

        models.add(ModelLoader.loadModel(renderer.getDevice(), "src/test/resources/models/cube/cube.obj", "src/test/resources/models/cube"));
        models.get(0).setTransform(new Vector3f(0, 0, -3), new Quaternionf().identity(), new Vector3f(1, 1, 1));

        models.add(ModelLoader.loadModel(renderer.getDevice(), "src/test/resources/models/sponza/Sponza.gltf", "src/test/resources/models/sponza"));
        models.get(1).setTransform(new Vector3f(0, 0, -55), new Quaternionf().identity(), new Vector3f(3, 3, 3));

        int numImages = renderer.getSwapChain().getNumberOfImages();
        for(int i = 0; i < numImages; i++) {
            renderer.forwardRender(renderer.getSwapChain().getImageViews()[i], models);
        }
    }

    @Override
    public void update() {
        if(window.isResized()) {
            window.resetResized();
        }

        float move = MOVEMENT_SPEED;
        if (KeyboardInput.isPressed(Key.W)) {
            camera.moveForward(move);
        } else if (KeyboardInput.isPressed(Key.S)) {
            camera.moveBackwards(move);
        }
        if (KeyboardInput.isPressed(Key.A)) {
            camera.moveLeft(move);
        } else if (KeyboardInput.isPressed(Key.D)) {
            camera.moveRight(move);
        }
        if (KeyboardInput.isPressed(Key.UP)) {
            camera.moveUp(move);
        } else if (KeyboardInput.isPressed(Key.DOWN)) {
            camera.moveDown(move);
        }

        if (MouseInput.isPressed(MouseButton.RIGHT)) {
            Vector2f displVec = MouseInput.getDeltaPosition();
            camera.addRotation((float) Math.toRadians(-displVec.y * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(-displVec.x * MOUSE_SENSITIVITY));
        }

        renderer.update(camera);
    }

    @Override
    public void cleanup() {
        Texture.getLoadedTextures().values().forEach(Texture::cleanup);
        models.forEach(Model::cleanup);
        renderer.cleanup();
    }

}

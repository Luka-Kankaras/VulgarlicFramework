package scene;

import java.util.ArrayList;
import java.util.List;

public class SceneManager {

    private static Scene activeScene;
    private static List<Scene> sceneList;

    public static void init(List<Scene> scenes) {
        sceneList = scenes;
        activeScene = scenes.get(0);
    }

}

package scene;

import core.model.Model;

import java.util.Map;

public abstract class Scene {

    private final String name;
    private final Map<String, Model> modelMap;

    public Scene(String name, Map<String, Model> modelMap) {
        this.name = name;
        this.modelMap = modelMap;
    }

    public void init() {

    }

    public void start() {

    }

    public void update() {

    }


    public String getName() {
        return name;
    }

    public Map<String, Model> getModelMap() {
        return modelMap;
    }

}

package graphics.RenderTechniques;

import graphics.Renderer;
import org.joml.Matrix4f;

public interface IRenderTechnique {
    void update(Matrix4f viewMatrix);
    void cleanup();
}

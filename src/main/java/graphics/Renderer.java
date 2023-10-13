package graphics;

import core.*;
import core.command.Queue;
import core.enums.ImageUsage;
import core.enums.PresentMode;
import core.model.Model;
import core.presentation.SwapChain;
import core.resources.ImageView;
import core.synchronisation.Semaphore;
import glfw.Window;
import graphics.RenderTechniques.ForwardRender;
import graphics.RenderTechniques.IRenderTechnique;
import org.tinylog.Logger;
import scene.Camera;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Renderer {

    private final Instance instance;
    private final PhysicalDevice physicalDevice;
    private final Device device;
    private final SwapChain swapChain;
    private final Surface surface;
    private final PipelineCache pipelineCache;
    private final Queue presentationQueue;
    private final Semaphore[] imageAcquisitionSemaphores;

    private final Map<Long, IRenderTechnique> renderingTechniques;

    public Renderer(Window window) {
        instance = new Instance(true);
        physicalDevice = new PhysicalDevice(instance);
        surface = new Surface(physicalDevice, window.getHandle());
        device = new Device(physicalDevice, surface);
        pipelineCache = new PipelineCache(device);

        renderingTechniques = new HashMap<>();

        swapChain = new SwapChain(device, surface, 3, window.getWidth(), window.getHeight(),
                PresentMode.IMMEDIATE, ImageUsage.COLOR_ATTACHMENT);
        presentationQueue = new Queue(device, device.getGraphicsQueueFamilyIndex(), 0);
        imageAcquisitionSemaphores = new Semaphore[swapChain.getNumberOfImages()];
        for(int i = 0; i < imageAcquisitionSemaphores.length; i++) {
            imageAcquisitionSemaphores[i] = new Semaphore(device);
        }
    }

    public void forwardRender(ImageView renderTarget, List<Model> modelList) {
        if(!renderingTechniques.containsKey(renderTarget.getVkImageView())) {
            renderingTechniques.put(renderTarget.getVkImageView(), new ForwardRender(device, modelList, renderTarget,
                    pipelineCache, swapChain.getExtent().width(), swapChain.getExtent().height()));
        }
    }

    public void update(Camera camera) {
        Semaphore semaphore = imageAcquisitionSemaphores[swapChain.getCurrentImageIndex()];
        swapChain.nextImage(semaphore);

        renderingTechniques.get(swapChain.getImageViews()[swapChain.getCurrentImageIndex()].getVkImageView()).update(camera.getViewMatrix());

        swapChain.present(presentationQueue, semaphore);
    }


    public Instance getInstance() {
        return instance;
    }

    public PhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public Device getDevice() {
        return device;
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }

    public Surface getSurface() {
        return surface;
    }

    public PipelineCache getPipelineCache() {
        return pipelineCache;
    }

    public Queue getPresentationQueue() {
        return presentationQueue;
    }

    public Map<Long, IRenderTechnique> getRenderingTechniques() {

        return renderingTechniques;
    }

    public void cleanup() {
        presentationQueue.waitIdle();

        for(IRenderTechnique technique : renderingTechniques.values()) {
            Logger.debug("Starting cleanup");
            technique.cleanup();
            Logger.debug("Finished cleanup");
        }

        Arrays.stream(imageAcquisitionSemaphores).forEach(Semaphore::cleanup);
        pipelineCache.cleanup();
        swapChain.cleanup();
        surface.cleanup();
        device.cleanup();
        physicalDevice.cleanup();
        instance.cleanup();
    }
}

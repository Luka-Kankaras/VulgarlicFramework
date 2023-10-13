package graphics.RenderTechniques;

import core.Device;
import core.Pipeline;
import core.PipelineCache;
import core.command.CommandBuffer;
import core.command.CommandPool;
import core.command.Queue;
import core.descriptors.Descriptor;
import core.descriptors.DescriptorPool;
import core.descriptors.DescriptorPoolData;
import core.descriptors.DescriptorSet;
import core.enums.*;
import core.model.Model;
import core.presentation.Attachment;
import core.presentation.FrameBuffer;
import core.presentation.RenderPass;
import core.presentation.Subpass;
import core.resources.*;
import core.shader.ShaderModule;
import core.shader.ShaderType;
import core.synchronisation.Fence;
import core.texture.Sampler;
import core.texture.Texture;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;
import org.tinylog.Logger;

import java.io.File;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;

public class ForwardRender implements IRenderTechnique {

    private final int width, height;

    private final Pipeline pipeline;
    private final Queue graphicsQueue;
    private final CommandPool commandPool;
    private final CommandBuffer commandBuffer;
    private final ShaderModule vertexShader, fragmentShader;
    // TODO: Consider making the RenderPass static
    private final RenderPass renderPass;
    private final FrameBuffer frameBuffer;
    private final DescriptorPool descriptorPool;
    private final DescriptorSet matricesDescriptorSet;
    private final Map<String, DescriptorSet> textureDescriptorSets;
    private final Image depthImage;
    private final ImageView depthImageView;
    private final DataBuffer projectionMatrixBuffer, viewMatrixBuffer, modelMatricesBuffer;
    private final Fence renderFence;
    private final Sampler sampler;

    private final List<Model> models;

    public ForwardRender(Device device, List<Model> models, ImageView renderTarget, PipelineCache pipelineCache, int width, int height) {
        graphicsQueue = new Queue(device, device.getGraphicsQueueFamilyIndex(), 0);
        commandPool = new CommandPool(device, graphicsQueue.getQueueFamilyIndex(), CommandPool.FlagBits.RESET_COMMAND_BUFFER);
        commandBuffer = new CommandBuffer(device, commandPool, true);
        vertexShader = new ShaderModule(device, ShaderType.VERTEX_SHADER, "src/main/resources/shaders/fwd_render_vertex.glsl");
        fragmentShader = new ShaderModule(device, ShaderType.FRAGMENT_SHADER, "src/main/resources/shaders/fwd_render_fragment.glsl");
        this.models = models;
        this.width = width;
        this.height = height;

        ImageData depthImageData = new ImageData()
                .width(width)
                .height(height)
                .format(ImageFormat.D32_SFLOAT)
                .tiling(ImageTiling.OPTIMAL)
                .layout(ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                .usage(ImageUsage.SAMPLED, ImageUsage.DEPTH_STENCIL_ATTACHMENT);
        depthImage = new Image(device, depthImageData);

        ImageViewData depthImageViewData = new ImageViewData().format(ImageFormat.D32_SFLOAT).aspectMask(ImageAspect.DEPTH);
        depthImageView = new ImageView(device, depthImage.getVkImage(), depthImageViewData);

        modelMatricesBuffer = new DataBuffer(device, models.size(), 16 * Float.BYTES, BufferUsage.UNIFORM_BUFFER, DataBuffer.FlagBits.NONE);
        
        projectionMatrixBuffer = new DataBuffer(device, 16, Float.BYTES, BufferUsage.UNIFORM_BUFFER, DataBuffer.FlagBits.NONE);
        Matrix4f projectionMatrix = new Matrix4f().identity();
        projectionMatrix.perspective(45, (float) width / (float) height, 0.1f, 100);
        projectionMatrix.m11(projectionMatrix.get(1, 1) * -1);
        projectionMatrixBuffer.populate(projectionMatrix);

        viewMatrixBuffer = new DataBuffer(device, 16, Float.BYTES, BufferUsage.UNIFORM_BUFFER, DataBuffer.FlagBits.NONE);

        CommandBuffer transitionBuffer = new CommandBuffer(device, commandPool, true);

        // register models
        int numTextures = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            transitionBuffer.begin(stack);

            int offset = 0;

            for(Model model : models) {
                for(Model.Material material : model.getMaterialList()) {
                    if(new File(material.texturePath()).exists()) {
                        Texture tex = Texture.load(material.texturePath(), device, ImageFormat.R8G8B8A8_SRGB, ImageAspect.COLOR);
                        tex.recordTextureTransition(transitionBuffer);
                        numTextures++;
                    }
                }

                // Fill the uniform buffer of model matrices
                float[] modelMatrixArray = new float[16];
                model.getModelMatrix().get(modelMatrixArray);

                modelMatricesBuffer.populate(modelMatrixArray, offset);
                offset += 16 * Float.BYTES;
            }
            transitionBuffer.end();
        }

        Fence imageTransitionFence = new Fence(device);
        imageTransitionFence.reset();
        graphicsQueue.submit(transitionBuffer, imageTransitionFence);
        imageTransitionFence.waitIdle();

        imageTransitionFence.cleanup();
        transitionBuffer.cleanup();

        descriptorPool = new DescriptorPool(device, new DescriptorPoolData(DescriptorPool.FlagBits.FREE_DESCRIPTOR_SET).
                add(Descriptor.Type.UNIFORM_BUFFER, 2).
                add(Descriptor.Type.UNIFORM_BUFFER_DYNAMIC, 1).
                add(Descriptor.Type.COMBINED_IMAGE_SAMPLER, numTextures));
        // register models end

        // Define the attachments
        List<Attachment> attachments = new ArrayList<>();
        // Color attachment (for presenting)
        Attachment colorAttachment = new Attachment(ImageFormat.B8G8R8A8_SRGB, SampleCount.COUNT_1,
                Attachment.LoadOp.CLEAR, Attachment.StoreOp.STORE, ImageLayout.UNDEFINED, ImageLayout.PRESENT_SRC_KHR);
        attachments.add(colorAttachment);

        // Depth attachment
        Attachment depthAttachment = new Attachment(ImageFormat.D32_SFLOAT, SampleCount.COUNT_1, Attachment.LoadOp.CLEAR,
                Attachment.StoreOp.STORE, ImageLayout.UNDEFINED, ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        attachments.add(depthAttachment);

        // Define subpass
        Subpass subpass = new Subpass(PipelineBindPoint.GRAPHICS);
        subpass.addColorAttachment(attachments.get(0), ImageLayout.COLOR_ATTACHMENT_OPTIMAL);
        subpass.setDepthStencilAttachment(attachments.get(1), ImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        // Define render pass
        renderPass = new RenderPass(device, attachments, List.of(subpass), null);

        // Define descriptor sets
        Descriptor projectionMatrixDescriptor = new Descriptor(Descriptor.Type.UNIFORM_BUFFER, ShaderStage.VERTEX);
        Descriptor viewMatrixDescriptor = new Descriptor(Descriptor.Type.UNIFORM_BUFFER, ShaderStage.VERTEX);
        Descriptor modelMatricesDescriptor = new Descriptor(Descriptor.Type.UNIFORM_BUFFER_DYNAMIC, ShaderStage.VERTEX);

        matricesDescriptorSet = new DescriptorSet(descriptorPool, Arrays.asList(
                projectionMatrixDescriptor,
                viewMatrixDescriptor,
                modelMatricesDescriptor));

        projectionMatrixDescriptor.bindBuffer(matricesDescriptorSet, projectionMatrixBuffer);
        viewMatrixDescriptor.bindBuffer(matricesDescriptorSet, viewMatrixBuffer);
        modelMatricesDescriptor.bindBuffer(matricesDescriptorSet, modelMatricesBuffer, 16 * Float.BYTES);

        textureDescriptorSets = new HashMap<>();
        sampler = new Sampler(device);

        String refTexPath = "";
        for(Model model : models) {
            for(Model.Material material : model.getMaterialList()) {
                if(material.texturePath().length() == 0) continue;

                Texture modelTexture = Texture.load(material.texturePath(), device, ImageFormat.R8G8B8A8_SRGB, ImageAspect.COLOR);

                if(textureDescriptorSets.containsKey(material.texturePath())) continue;

                Descriptor modelTextureDescriptor = new Descriptor(Descriptor.Type.COMBINED_IMAGE_SAMPLER, ShaderStage.FRAGMENT);
                DescriptorSet modelTextureDescriptorSet = new DescriptorSet(descriptorPool, List.of(modelTextureDescriptor));

                textureDescriptorSets.put(material.texturePath(), modelTextureDescriptorSet);

                modelTextureDescriptor.bindImage(modelTextureDescriptorSet, modelTexture.getImageView(), sampler);

                if(refTexPath.equals("")) {
                    refTexPath = material.texturePath();
                }
            }
        }

        // Define frame buffers
        frameBuffer = new FrameBuffer(device, renderPass, new ImageView[]{renderTarget, depthImageView},
                width, height, 1);

        // Define the pipeline
        pipeline = new Pipeline(device, pipelineCache, Arrays.asList(vertexShader, fragmentShader),
                Arrays.asList(matricesDescriptorSet, textureDescriptorSets.get(refTexPath)), renderPass, width, height, 0, 100);

        renderFence = new Fence(device);
    }

    public void update(Matrix4f viewMatrix) {
        viewMatrixBuffer.populate(viewMatrix);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            commandBuffer.begin(stack);
            renderPass.begin(stack, commandBuffer, frameBuffer, width, height);
            pipeline.bind(commandBuffer, PipelineBindPoint.GRAPHICS);

            // Define viewport and scissor
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.get(0)
                    .x(0)
                    .y(0)
                    .width(width)
                    .height(height)
                    .minDepth(0.0f)
                    .maxDepth(1f);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.get(0)
                    .offset(offset -> {offset.set(0, 0);})
                    .extent(extent -> {extent.set(width, height);});

            vkCmdSetViewport(commandBuffer.getVkCommandBuffer(), 0, viewport);
            vkCmdSetScissor(commandBuffer.getVkCommandBuffer(), 0, scissor);

            LongBuffer descriptorSets = stack.mallocLong(2);
            descriptorSets.put(0, matricesDescriptorSet.getVkDescriptorSet());

            IntBuffer dynamicOffset = stack.mallocInt(1);

            for(int i = 0; i < models.size(); i++) {
                Model model = models.get(i);

                List<Model.RawModelData> meshList = model.getMeshDataList();
                List<Model.Material> materialList = model.getMaterialList();

                for(int j = 0; j < meshList.size(); j++) {
                    Model.RawModelData mesh = meshList.get(j);

                    DataBuffer vertexBuffer = model.getVertexBuffers().get(j);
                    DataBuffer indexBuffer = model.getIndexBuffers().get(j);

                    vkCmdBindVertexBuffers(commandBuffer.getVkCommandBuffer(), 0, stack.longs(vertexBuffer.getVkBuffer()), stack.longs(0));
                    vkCmdBindIndexBuffer(commandBuffer.getVkCommandBuffer(), indexBuffer.getVkBuffer(), 0, VK_INDEX_TYPE_UINT32);

                    descriptorSets.put(1, textureDescriptorSets.get(materialList.get(mesh.materialIndex()).texturePath()).getVkDescriptorSet());

                    dynamicOffset.put(0, i * 16 * Float.BYTES);

                    vkCmdBindDescriptorSets(commandBuffer.getVkCommandBuffer(), VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipeline.getVkPipelineLayout(), 0, descriptorSets, dynamicOffset);

                    vkCmdDrawIndexed(commandBuffer.getVkCommandBuffer(), mesh.indices().length, 1, 0, 0, 0);
                }
            }

            renderPass.end(commandBuffer);
            commandBuffer.end();

            renderFence.reset();
            graphicsQueue.submit(commandBuffer, renderFence);
            renderFence.waitIdle();
            graphicsQueue.waitIdle();

            commandBuffer.reset();
        }
    }

    public void cleanup() {
        sampler.cleanup();
        renderFence.cleanup();
        depthImage.cleanup();
        depthImageView.cleanup();
        projectionMatrixBuffer.cleanup();
        viewMatrixBuffer.cleanup();
        modelMatricesBuffer.cleanup();
        descriptorPool.cleanup();
        frameBuffer.cleanup();
        renderPass.cleanup();
        vertexShader.cleanup();
        fragmentShader.cleanup();
        commandBuffer.cleanup();
        commandPool.cleanup();
        pipeline.cleanup();
    }

}

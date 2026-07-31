package com.example.client.renderer;

import com.example.ExampleMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.resources.Identifier;

public class VoidWraithRenderer extends VexRenderer {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "textures/entity/void_wraith.png");

    public VoidWraithRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(VexRenderState state) {
        return TEXTURE;
    }
}

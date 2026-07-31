package com.example.client;

import com.example.ExampleMod;
import com.example.client.renderer.VoidWraithRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ExampleMod.VOID_WRAITH, VoidWraithRenderer::new);
	}
}

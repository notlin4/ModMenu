package com.terraformersmc.modmenu.gui.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.terraformersmc.modmenu.api.UpdateInfo;
import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DescriptionListWidget extends EntryListWidget<DescriptionListWidget.DescriptionEntry> {

	private static final Text HAS_UPDATE_TEXT = Text.translatable("modmenu.hasUpdate");
	private static final Text EXPERIMENTAL_TEXT = Text.translatable("modmenu.experimental").formatted(Formatting.GOLD);
	private static final Text DOWNLOAD_TEXT = Text.translatable("modmenu.downloadLink")
		.formatted(Formatting.BLUE)
		.formatted(Formatting.UNDERLINE);
	private static final Text CHILD_HAS_UPDATE_TEXT = Text.translatable("modmenu.childHasUpdate");
	private static final Text LINKS_TEXT = Text.translatable("modmenu.links");
	private static final Text SOURCE_TEXT = Text.translatable("modmenu.source")
		.formatted(Formatting.BLUE)
		.formatted(Formatting.UNDERLINE);
	private static final Text LICENSE_TEXT = Text.translatable("modmenu.license");
	private static final Text VIEW_CREDITS_TEXT = Text.translatable("modmenu.viewCredits")
		.formatted(Formatting.BLUE)
		.formatted(Formatting.UNDERLINE);
	private static final Text CREDITS_TEXT = Text.translatable("modmenu.credits");

	private final ModsScreen parent;
	private final TextRenderer textRenderer;
	private ModListEntry lastSelected = null;

	public DescriptionListWidget(
		MinecraftClient client,
		int width,
		int height,
		int y,
		int itemHeight,
		ModsScreen parent
	) {
		super(client, width, height, y, itemHeight);
		this.parent = parent;
		this.textRenderer = client.textRenderer;
	}

	@Override
	public DescriptionEntry getSelectedOrNull() {
		return null;
	}

	@Override
	public int getRowWidth() {
		return this.width - 10;
	}

	@Override
	protected int getScrollbarX() {
		return this.width - 6 + this.getX();
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		Mod mod = parent.getSelectedEntry().getMod();
		builder.put(NarrationPart.TITLE, mod.getTranslatedName() + " " + mod.getPrefixedVersion());
	}

	@Override
	public void renderList(DrawContext DrawContext, int mouseX, int mouseY, float delta) {
		ModListEntry selectedEntry = parent.getSelectedEntry();
		if (selectedEntry != lastSelected) {
			lastSelected = selectedEntry;
			clearEntries();
			setScrollAmount(-Double.MAX_VALUE);
			if (lastSelected != null) {
				DescriptionEntry emptyEntry = new DescriptionEntry(OrderedText.EMPTY);
				int wrapWidth = getRowWidth() - 5;

				Mod mod = lastSelected.getMod();
				Text description = mod.getFormattedDescription();
				if (!description.getString().isEmpty()) {
					for (OrderedText line : textRenderer.wrapLines(description, wrapWidth)) {
						children().add(new DescriptionEntry(line));
					}
				}

				if (ModMenuConfig.UPDATE_CHECKER.getValue() && !ModMenuConfig.DISABLE_UPDATE_CHECKER.getValue()
					.contains(mod.getId())) {
					UpdateInfo updateInfo = mod.getUpdateInfo();
					if (updateInfo != null && updateInfo.isUpdateAvailable()) {
						children().add(emptyEntry);

						int index = 0;
						for (OrderedText line : textRenderer.wrapLines(HAS_UPDATE_TEXT, wrapWidth - 11)) {
							DescriptionEntry entry = new DescriptionEntry(line);
							if (index == 0) {
								entry.setUpdateTextEntry();
							}

							children().add(entry);
							index += 1;
						}

						for (OrderedText line : textRenderer.wrapLines(EXPERIMENTAL_TEXT, wrapWidth - 16)) {
							children().add(new DescriptionEntry(line, 8));
						}


						Text updateMessage = updateInfo.getUpdateMessage();
						String downloadLink = updateInfo.getDownloadLink();
						if (updateMessage == null) {
							updateMessage = DOWNLOAD_TEXT;
						} else {
							if (downloadLink != null) {
								updateMessage = updateMessage.copy()
									.formatted(Formatting.BLUE)
									.formatted(Formatting.UNDERLINE);
							}
						}
						for (OrderedText line : textRenderer.wrapLines(updateMessage, wrapWidth - 16)) {
							if (downloadLink != null) {
								children().add(new LinkEntry(line, downloadLink, 8));
							} else {
								children().add(new DescriptionEntry(line, 8));

							}
						}
					}
					if (mod.getChildHasUpdate()) {
						children().add(emptyEntry);

						int index = 0;
						for (OrderedText line : textRenderer.wrapLines(CHILD_HAS_UPDATE_TEXT, wrapWidth - 11)) {
							DescriptionEntry entry = new DescriptionEntry(line);
							if (index == 0) {
								entry.setUpdateTextEntry();
							}

							children().add(entry);
							index += 1;
						}
					}
				}

				Map<String, String> links = mod.getLinks();
				String sourceLink = mod.getSource();
				if ((!links.isEmpty() || sourceLink != null) && !ModMenuConfig.HIDE_MOD_LINKS.getValue()) {
					children().add(emptyEntry);

					for (OrderedText line : textRenderer.wrapLines(LINKS_TEXT, wrapWidth)) {
						children().add(new DescriptionEntry(line));
					}

					if (sourceLink != null) {
						int indent = 8;
						for (OrderedText line : textRenderer.wrapLines(SOURCE_TEXT, wrapWidth - 16)) {
							children().add(new LinkEntry(line, sourceLink, indent));
							indent = 16;
						}
					}

					links.forEach((key, value) -> {
						int indent = 8;
						for (OrderedText line : textRenderer.wrapLines(Text.translatable(key)
								.formatted(Formatting.BLUE)
								.formatted(Formatting.UNDERLINE),
							wrapWidth - 16
						)) {
							children().add(new LinkEntry(line, value, indent));
							indent = 16;
						}
					});
				}

				Set<String> licenses = mod.getLicense();
				if (!ModMenuConfig.HIDE_MOD_LICENSE.getValue() && !licenses.isEmpty()) {
					children().add(emptyEntry);

					for (OrderedText line : textRenderer.wrapLines(LICENSE_TEXT, wrapWidth)) {
						children().add(new DescriptionEntry(line));
					}

					for (String license : licenses) {
						int indent = 8;
						for (OrderedText line : textRenderer.wrapLines(Text.literal(license), wrapWidth - 16)) {
							children().add(new DescriptionEntry(line, indent));
							indent = 16;
						}
					}
				}

				if (!ModMenuConfig.HIDE_MOD_CREDITS.getValue()) {
					if ("minecraft".equals(mod.getId())) {
						children().add(emptyEntry);

						for (OrderedText line : textRenderer.wrapLines(VIEW_CREDITS_TEXT, wrapWidth)) {
							children().add(new MojangCreditsEntry(line));
						}
					} else if (!"java".equals(mod.getId())) {
						var credits = mod.getCredits();

						if (!credits.isEmpty()) {
							children().add(emptyEntry);

							for (OrderedText line : textRenderer.wrapLines(CREDITS_TEXT, wrapWidth)) {
								children().add(new DescriptionEntry(line));
							}

							var iterator = credits.entrySet().iterator();

							while (iterator.hasNext()) {
								int indent = 8;

								var role = iterator.next();
								var roleName = role.getKey();

								for (var line : textRenderer.wrapLines(this.creditsRoleText(roleName),
									wrapWidth - 16
								)) {
									children().add(new DescriptionEntry(line, indent));
									indent = 16;
								}

								for (var contributor : role.getValue()) {
									indent = 16;

									for (var line : textRenderer.wrapLines(Text.literal(contributor), wrapWidth - 24)) {
										children().add(new DescriptionEntry(line, indent));
										indent = 24;
									}
								}

								if (iterator.hasNext()) {
									children().add(emptyEntry);
								}
							}
						}
					}
				}
			}
		}

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder;
		BuiltBuffer builtBuffer;

		//		{
		//			RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
		//			RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
		//			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		//			bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		//			bufferBuilder.vertex(this.getX(), this.getBottom(), 0.0D).texture(this.getX() / 32.0F, (this.getBottom() + (int) this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255);
		//			bufferBuilder.vertex(this.getRight(), this.getBottom(), 0.0D).texture(this.getRight() / 32.0F, (this.getBottom() + (int) this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255);
		//			bufferBuilder.vertex(this.getRight(), this.getY(), 0.0D).texture(this.getRight() / 32.0F, (this.getY() + (int) this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255);
		//			bufferBuilder.vertex(this.getX(), this.getY(), 0.0D).texture(this.getX() / 32.0F, (this.getY() + (int) this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255);
		//			tessellator.draw();
		//		}

		this.enableScissor(DrawContext);
		super.renderList(DrawContext, mouseX, mouseY, delta);
		DrawContext.disableScissor();

		RenderSystem.depthFunc(515);
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA,
			GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
			GlStateManager.SrcFactor.ZERO,
			GlStateManager.DstFactor.ONE
		);
//		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

		bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(this.getX(), (this.getY() + 4), 0.0F).

			color(0, 0, 0, 0);

		bufferBuilder.vertex(this.getRight(), (this.getY() + 4), 0.0F).

			color(0, 0, 0, 0);

		bufferBuilder.vertex(this.getRight(), this.getY(), 0.0F).

			color(0, 0, 0, 255);

		bufferBuilder.vertex(this.getX(), this.getY(), 0.0F).

			color(0, 0, 0, 255);

		bufferBuilder.vertex(this.getX(), this.getBottom(), 0.0F).

			color(0, 0, 0, 255);

		bufferBuilder.vertex(this.getRight(), this.getBottom(), 0.0F).

			color(0, 0, 0, 255);

		bufferBuilder.vertex(this.getRight(), (this.getBottom() - 4), 0.0F).

			color(0, 0, 0, 0);

		bufferBuilder.vertex(this.getX(), (this.getBottom() - 4), 0.0F).

			color(0, 0, 0, 0);

		try {
			builtBuffer = bufferBuilder.end();
			BufferRenderer.drawWithGlobalProgram(builtBuffer);
			builtBuffer.close();
		} catch (Exception e) {
			// Ignored
		}
		this.renderScrollBar(bufferBuilder, tessellator);

		RenderSystem.disableBlend();
	}

	public void renderScrollBar(BufferBuilder bufferBuilder, Tessellator tessellator) {
		BuiltBuffer builtBuffer;
		int scrollbarStartX = this.getScrollbarX();
		int scrollbarEndX = scrollbarStartX + 6;
		int maxScroll = this.getMaxScroll();
		if (maxScroll > 0) {
			int p = (int) ((float) ((this.getBottom() - this.getY()) * (this.getBottom() - this.getY())) / (float) this.getMaxPosition());
			p = MathHelper.clamp(p, 32, this.getBottom() - this.getY() - 8);
			int q = (int) this.getScrollAmount() * (this.getBottom() - this.getY() - p) / maxScroll + this.getY();
			if (q < this.getY()) {
				q = this.getY();
			}

			bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			bufferBuilder.vertex(scrollbarStartX, this.getBottom(), 0.0F).color(0, 0, 0, 255);
			bufferBuilder.vertex(scrollbarEndX, this.getBottom(), 0.0F).color(0, 0, 0, 255);
			bufferBuilder.vertex(scrollbarEndX, this.getY(), 0.0F).color(0, 0, 0, 255);
			bufferBuilder.vertex(scrollbarStartX, this.getY(), 0.0F).color(0, 0, 0, 255);
			bufferBuilder.vertex(scrollbarStartX, q + p, 0.0F).color(128, 128, 128, 255);
			bufferBuilder.vertex(scrollbarEndX, q + p, 0.0F).color(128, 128, 128, 255);
			bufferBuilder.vertex(scrollbarEndX, q, 0.0F).color(128, 128, 128, 255);
			bufferBuilder.vertex(scrollbarStartX, q, 0.0F).color(128, 128, 128, 255);
			bufferBuilder.vertex(scrollbarStartX, q + p - 1, 0.0F).color(192, 192, 192, 255);
			bufferBuilder.vertex(scrollbarEndX - 1, q + p - 1, 0.0F).color(192, 192, 192, 255);
			bufferBuilder.vertex(scrollbarEndX - 1, q, 0.0F).color(192, 192, 192, 255);
			bufferBuilder.vertex(scrollbarStartX, q, 0.0F).color(192, 192, 192, 255);
			try {
				builtBuffer = bufferBuilder.end();
				BufferRenderer.drawWithGlobalProgram(builtBuffer);
				builtBuffer.close();
			} catch (Exception e) {
				// Ignored
			}
		}
	}

	private Text creditsRoleText(String roleName) {
		// Replace spaces and dashes in role names with underscores if they exist
		// Notably Quilted Fabric API does this with FabricMC as "Upstream Owner"
		var translationKey = roleName.replaceAll("[ -]", "_").toLowerCase();

		// Add an s to the default untranslated string if it ends in r since this
		// Fixes common role names people use in English (e.g. Author -> Authors)
		var fallback = roleName.endsWith("r") ? roleName + "s" : roleName;

		return Text.translatableWithFallback("modmenu.credits.role." + translationKey, fallback)
			.append(Text.literal(":"));
	}

	protected class DescriptionEntry extends ElementListWidget.Entry<DescriptionEntry> {
		protected OrderedText text;
		protected int indent;
		public boolean updateTextEntry = false;

		public DescriptionEntry(OrderedText text, int indent) {
			this.text = text;
			this.indent = indent;
		}

		public DescriptionEntry(OrderedText text) {
			this(text, 0);
		}

		public DescriptionEntry setUpdateTextEntry() {
			this.updateTextEntry = true;
			return this;
		}

		@Override
		public void render(
			DrawContext DrawContext,
			int index,
			int y,
			int x,
			int itemWidth,
			int itemHeight,
			int mouseX,
			int mouseY,
			boolean isSelected,
			float delta
		) {
			if (updateTextEntry) {
				UpdateAvailableBadge.renderBadge(DrawContext, x + indent, y);
				x += 11;
			}
			DrawContext.drawTextWithShadow(textRenderer, text, x + indent, y, 0xAAAAAA);
		}

		@Override
		public List<? extends Element> children() {
			return Collections.emptyList();
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return Collections.emptyList();
		}
	}

	protected class MojangCreditsEntry extends DescriptionEntry {
		public MojangCreditsEntry(OrderedText text) {
			super(text);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (isMouseOver(mouseX, mouseY)) {
				client.setScreen(new MinecraftCredits());
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}

		class MinecraftCredits extends CreditsAndAttributionScreen {
			public MinecraftCredits() {
				super(parent);
			}
		}
	}

	protected class LinkEntry extends DescriptionEntry {
		private final String link;

		public LinkEntry(OrderedText text, String link, int indent) {
			super(text, indent);
			this.link = link;
		}

		public LinkEntry(OrderedText text, String link) {
			this(text, link, 0);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (isMouseOver(mouseX, mouseY)) {
				client.setScreen(new ConfirmLinkScreen((open) -> {
					if (open) {
						Util.getOperatingSystem().open(link);
					}
					client.setScreen(parent);
				}, link, false));
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}
	}

}

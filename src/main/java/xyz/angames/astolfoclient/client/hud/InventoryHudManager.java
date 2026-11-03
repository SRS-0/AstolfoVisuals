package xyz.angames.astolfoclient.client.hud;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(value=EnvType.CLIENT)
public class InventoryHudManager {
    public float x = 20.0f;
    public float y = 300.0f;
    private static final int SLOT_SIZE = 18;
    private static final float WIDTH = 162.0f;
    private static final float HEIGHT = 54.0f;
    private boolean dragging = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private Module cachedModule;

    public void render(DrawContext context) {
        ItemStack stack;
        int i;
        if (this.cachedModule == null) {
            this.cachedModule = AstolfoclientClient.moduleManager.getModuleByName("Interface");
        }
        boolean isEditing = this.client.currentScreen instanceof HudEditorScreen;
        InterfaceModule interfaceMod = (InterfaceModule)this.cachedModule;
        if (interfaceMod == null) {
            return;
        }
        if (!(isEditing || interfaceMod.isEnabled() && interfaceMod.inventoryHud.get())) {
            return;
        }
        if (this.client.player == null) {
            return;
        }
        double currentGuiScale = this.client.getWindow().getScaleFactor();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        float scaleModifier = (float)(2.0 / currentGuiScale);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.x, this.y);
        context.getMatrices().scale(scaleModifier, scaleModifier);
        context.getMatrices().translate(-this.x, -this.y);
        ArrayList<ItemStack> inventoryList = new ArrayList<ItemStack>();
        boolean hasItems = false;
        for (i = 0; i < 27; ++i) {
            stack = (ItemStack)this.client.player.getInventory().getStack(9 + i);
            inventoryList.add(stack);
            if (stack.isEmpty()) continue;
            hasItems = true;
        }
        if (isEditing && !hasItems) {
            inventoryList.set(0, new ItemStack((ItemConvertible)Items.DIAMOND_SWORD));
            inventoryList.set(1, new ItemStack((ItemConvertible)Items.ENCHANTED_GOLDEN_APPLE, 64));
            inventoryList.set(2, new ItemStack((ItemConvertible)Items.ENDER_PEARL, 16));
            inventoryList.set(3, new ItemStack((ItemConvertible)Items.COBWEB, 64));
            inventoryList.set(4, new ItemStack((ItemConvertible)Items.OBSIDIAN, 64));
            inventoryList.set(5, new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING));
            inventoryList.set(6, new ItemStack((ItemConvertible)Items.DIAMOND_PICKAXE));
            inventoryList.set(7, new ItemStack((ItemConvertible)Items.BOW));
            inventoryList.set(8, new ItemStack((ItemConvertible)Items.ARROW, 64));
        }
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.0f, 0.0f);
        for (i = 0; i < 27; ++i) {
            stack = (ItemStack)inventoryList.get(i);
            if (stack.isEmpty()) continue;
            int row = i / 9;
            int col = i % 9;
            int ix = (int)(this.x + (float)(col * 18));
            int iy = (int)(this.y + (float)(row * 18));
            context.drawItem(stack, ix, iy);
            context.drawStackOverlay(this.client.textRenderer, stack, ix, iy);
        }
        context.getMatrices().popMatrix();
        context.getMatrices().popMatrix();
    }

    public float getScaleModifier() {
        double currentGuiScale = this.client.getWindow().getScaleFactor();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        return (float)(2.0 / currentGuiScale);
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        boolean isEditing = this.client.currentScreen instanceof HudEditorScreen;
        InterfaceModule interfaceMod = (InterfaceModule)this.cachedModule;
        if (interfaceMod == null) {
            return false;
        }
        if (!(isEditing || interfaceMod.isEnabled() && interfaceMod.inventoryHud.get())) {
            return false;
        }
        float scaleModifier = this.getScaleModifier();
        float effectiveW = 162.0f * scaleModifier;
        float effectiveH = 54.0f * scaleModifier;
        if (button == 0 && mouseX >= (double)this.x && mouseX <= (double)(this.x + effectiveW) && mouseY >= (double)this.y && mouseY <= (double)(this.y + effectiveH)) {
            this.dragging = true;
            this.dragOffsetX = (float)(mouseX - (double)this.x);
            this.dragOffsetY = (float)(mouseY - (double)this.y);
            return true;
        }
        return false;
    }

    public void onMouseDragged(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            float scaleModifier = this.getScaleModifier();
            float screenW = this.client.getWindow().getScaledWidth();
            float screenH = this.client.getWindow().getScaledHeight();
            float effectiveW = 162.0f * scaleModifier;
            float effectiveH = 54.0f * scaleModifier;
            float targetX = (float)(mouseX - (double)this.dragOffsetX);
            float targetY = (float)(mouseY - (double)this.dragOffsetY);
            this.x = Math.max(0.0f, Math.min(Math.max(0.0f, screenW - effectiveW), targetX));
            this.y = Math.max(0.0f, Math.min(Math.max(0.0f, screenH - effectiveH), targetY));
        }
    }

    public void onMouseReleased(int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
        }
    }

    public void onMouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
        }
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return 162.0f;
    }

    public float getHeight() {
        return 54.0f;
    }
}


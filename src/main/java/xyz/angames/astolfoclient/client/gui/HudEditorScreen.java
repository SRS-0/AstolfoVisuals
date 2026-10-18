package xyz.angames.astolfoclient.client.gui;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;

@Environment(EnvType.CLIENT)
public class HudEditorScreen extends Screen {
   private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
   private Object currentlyDraggedManager = null;
   private static boolean activeVLine = false;
   private static float activeVLineX = 0.0F;
   private static boolean activeHLine = false;
   private static float activeHLineY = 0.0F;

   public HudEditorScreen() {
      super(Component.literal("HUD Editor"));
   }

   @Override
   protected void init() {
      super.init();
      this.clampAllElements();
   }

   private void clampAllElements() {
      Object[] managers = new Object[]{
         AstolfoclientClient.targetHudManager,
         AstolfoclientClient.boyKisserManager,
         AstolfoclientClient.effectHudManager,
         AstolfoclientClient.armorHudManager,
         AstolfoclientClient.inventoryHudManager,
         AstolfoclientClient.activeBindsManager,
         AstolfoclientClient.testHudManager,
         AstolfoclientClient.musicHudManager,
         AstolfoclientClient.infoHudManager
      };

      for (Object manager : managers) {
         if (manager != null) {
            try {
               float scale = this.getManagerScaleModifier(manager);
               float w = this.getManagerWidth(manager) * scale;
               float h = this.getManagerHeight(manager) * scale;
               float maxElemX = Math.max(0.0F, this.width - w);
               float maxElemY = Math.max(0.0F, this.height - h);
               float curX = 0.0F;

               try {
                  Field f = manager.getClass().getField("x");
                  curX = ((Number)f.get(manager)).floatValue();
               } catch (Exception e) {
                  try {
                     Method m = manager.getClass().getMethod("getX");
                     curX = ((Number)m.invoke(manager)).floatValue();
                  } catch (Exception var17) {
                  }
               }

               float curY = 0.0F;

               try {
                  Field f = manager.getClass().getField("y");
                  curY = ((Number)f.get(manager)).floatValue();
               } catch (Exception e) {
                  try {
                     Method m = manager.getClass().getMethod("getY");
                     curY = ((Number)m.invoke(manager)).floatValue();
                  } catch (Exception var15) {
                  }
               }

               float clampedX = Math.max(0.0F, Math.min(maxElemX, curX));
               float clampedY = Math.max(0.0F, Math.min(maxElemY, curY));
               if (clampedX != curX || clampedY != curY) {
                  applyPosition(manager, clampedX, clampedY);
               }
            } catch (Exception var19) {
            }
         }
      }
   }

   public void renderTransparentBackground(GuiGraphicsExtractor context) {
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      if (xyz.angames.astolfoclient.client.compat.GuiCompat.shiftDown()) {
         float gridSpacing = 10.0F;
         int gridColor = 234881023;

         for (float x = gridSpacing; x < this.width; x += gridSpacing) {
            context.fill((int)x, 0, (int)x + 1, this.height, gridColor);
         }

         for (float y = gridSpacing; y < this.height; y += gridSpacing) {
            context.fill(0, (int)y, this.width, (int)y + 1, gridColor);
         }
      }

      Minecraft mc = Minecraft.getInstance();
      double currentGuiScale = mc.getWindow().getGuiScale();
      float scaleModifier = (float)(2.0 / currentGuiScale);
      float hudWidth = this.width / scaleModifier;
      float hudHeight = this.height / scaleModifier;
      context.pose().pushMatrix();
      context.pose().scale(scaleModifier, scaleModifier);
      Matrix4f matrix = GuiCompat.matrix4f(context);
      MsdfFont bikoFont = (MsdfFont)BIKO_FONT.get();
      Color white = Color.WHITE;
      Color gray = new Color(170, 170, 170);
      String title = "HUD Editor";
      float titleSize = 16.0F;
      float titleW = bikoFont.getWidth(title, titleSize);
      float titleX = hudWidth / 2.0F - titleW / 2.0F;
      float titleY = 10.0F / scaleModifier;
      Builder.text().font(bikoFont).text(title).color(new Color(0, 0, 0, 150)).size(titleSize).build().render(matrix, titleX + 1.0F, titleY + 1.0F);
      Builder.text().font(bikoFont).text(title).color(white).size(titleSize).build().render(matrix, titleX, titleY);
      String desc = "Drag elements to reposition them (Hold SHIFT to snap/align)";
      float descSize = 11.5F;
      float descW = bikoFont.getWidth(desc, descSize);
      float descX = hudWidth / 2.0F - descW / 2.0F;
      float descY = 22.0F / scaleModifier;
      Builder.text().font(bikoFont).text(desc).color(new Color(0, 0, 0, 150)).size(descSize).build().render(matrix, descX + 1.0F, descY + 1.0F);
      Builder.text().font(bikoFont).text(desc).color(gray).size(descSize).build().render(matrix, descX, descY);
      String exit = "Press ESC to save and exit";
      float exitSize = 11.0F;
      float exitW = bikoFont.getWidth(exit, exitSize);
      float exitX = hudWidth / 2.0F - exitW / 2.0F;
      float exitY = 34.0F / scaleModifier;
      Builder.text().font(bikoFont).text(exit).color(new Color(0, 0, 0, 150)).size(exitSize).build().render(matrix, exitX + 1.0F, exitY + 1.0F);
      Builder.text().font(bikoFont).text(exit).color(gray).size(exitSize).build().render(matrix, exitX, exitY);
      context.pose().popMatrix();
      if (AstolfoclientClient.hudRenderer != null && AstolfoclientClient.hudRenderer.getLogoRenderer() != null) {
         AstolfoclientClient.hudRenderer.getLogoRenderer().render(context);
      }

      AstolfoclientClient.targetHudManager.render(context, delta);
      AstolfoclientClient.boyKisserManager.render(context, delta);
      AstolfoclientClient.effectHudManager.render(context);
      AstolfoclientClient.armorHudManager.render(context);
      AstolfoclientClient.inventoryHudManager.render(context);
      AstolfoclientClient.activeBindsManager.render(context, delta);
      AstolfoclientClient.testHudManager.render(context, delta);
      AstolfoclientClient.musicHudManager.render(context, delta);
      AstolfoclientClient.infoHudManager.render(context, delta);
      if (xyz.angames.astolfoclient.client.compat.GuiCompat.shiftDown()) {
         int guideColor = -65281;
         if (activeVLine) {
            context.fill((int)activeVLineX, 0, (int)activeVLineX + 1, this.height, guideColor);
         }

         if (activeHLine) {
            context.fill(0, (int)activeHLineY, this.width, (int)activeHLineY + 1, guideColor);
         }
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      return this.mouseClicked(event.x(), event.y(), xyz.angames.astolfoclient.client.compat.KeyMap.sdlButtonToGlfw(event.button()));
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      return this.mouseReleased(event.x(), event.y(), xyz.angames.astolfoclient.client.compat.KeyMap.sdlButtonToGlfw(event.button()));
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      return this.mouseDragged(event.x(), event.y(), xyz.angames.astolfoclient.client.compat.KeyMap.sdlButtonToGlfw(event.button()), dx, dy);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.currentlyDraggedManager = null;
      if (AstolfoclientClient.infoHudManager != null && AstolfoclientClient.infoHudManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.infoHudManager;
         return true;
      } else if (AstolfoclientClient.musicHudManager != null && AstolfoclientClient.musicHudManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.musicHudManager;
         return true;
      } else if (AstolfoclientClient.testHudManager != null && AstolfoclientClient.testHudManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.testHudManager;
         return true;
      } else if (AstolfoclientClient.activeBindsManager != null && AstolfoclientClient.activeBindsManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.activeBindsManager;
         return true;
      } else if (AstolfoclientClient.inventoryHudManager != null && AstolfoclientClient.inventoryHudManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.inventoryHudManager;
         return true;
      } else if (AstolfoclientClient.armorHudManager != null && AstolfoclientClient.armorHudManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.armorHudManager;
         return true;
      } else if (AstolfoclientClient.effectHudManager != null && AstolfoclientClient.effectHudManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.effectHudManager;
         return true;
      } else if (AstolfoclientClient.boyKisserManager != null && AstolfoclientClient.boyKisserManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.boyKisserManager;
         return true;
      } else if (AstolfoclientClient.targetHudManager != null && AstolfoclientClient.targetHudManager.onMouseClicked(mouseX, mouseY, button)) {
         this.currentlyDraggedManager = AstolfoclientClient.targetHudManager;
         return true;
      } else {
         return super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button + 1, 0)), false);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.currentlyDraggedManager == AstolfoclientClient.infoHudManager && AstolfoclientClient.infoHudManager != null) {
         AstolfoclientClient.infoHudManager.onMouseDragged(mouseX, mouseY, button);
      } else if (this.currentlyDraggedManager == AstolfoclientClient.musicHudManager && AstolfoclientClient.musicHudManager != null) {
         AstolfoclientClient.musicHudManager.onMouseDragged(mouseX, mouseY, button);
      } else if (this.currentlyDraggedManager == AstolfoclientClient.testHudManager && AstolfoclientClient.testHudManager != null) {
         AstolfoclientClient.testHudManager.onMouseDragged(mouseX, mouseY, button);
      } else if (this.currentlyDraggedManager == AstolfoclientClient.activeBindsManager && AstolfoclientClient.activeBindsManager != null) {
         AstolfoclientClient.activeBindsManager.onMouseDragged(mouseX, mouseY, button);
      } else if (this.currentlyDraggedManager == AstolfoclientClient.inventoryHudManager && AstolfoclientClient.inventoryHudManager != null) {
         AstolfoclientClient.inventoryHudManager.onMouseDragged(mouseX, mouseY, button);
      } else if (this.currentlyDraggedManager == AstolfoclientClient.armorHudManager && AstolfoclientClient.armorHudManager != null) {
         AstolfoclientClient.armorHudManager.onMouseDragged(mouseX, mouseY, button);
      } else if (this.currentlyDraggedManager == AstolfoclientClient.effectHudManager && AstolfoclientClient.effectHudManager != null) {
         AstolfoclientClient.effectHudManager.onMouseDragged(mouseX, mouseY, button);
      } else if (this.currentlyDraggedManager == AstolfoclientClient.boyKisserManager && AstolfoclientClient.boyKisserManager != null) {
         AstolfoclientClient.boyKisserManager.onMouseDragged(mouseX, mouseY, button);
      } else if (this.currentlyDraggedManager == AstolfoclientClient.targetHudManager && AstolfoclientClient.targetHudManager != null) {
         AstolfoclientClient.targetHudManager.onMouseDragged(mouseX, mouseY, button);
      }

      HudEditorScreen.DraggedElement de = this.getDraggedElement();
      if (de != null) {
         if (xyz.angames.astolfoclient.client.compat.GuiCompat.shiftDown()) {
            this.applySnapping(de);
         }

         float currentScale = de.scaleModifier;
         float elemW = de.width * currentScale;
         float elemH = de.height * currentScale;
         float maxElemX = Math.max(0.0F, this.width - elemW);
         float maxElemY = Math.max(0.0F, this.height - elemH);
         float curX = de.x;

         try {
            Field f = de.manager.getClass().getField("x");
            curX = ((Number)f.get(de.manager)).floatValue();
         } catch (Exception e) {
            try {
               Method m = de.manager.getClass().getMethod("getX");
               curX = ((Number)m.invoke(de.manager)).floatValue();
            } catch (Exception var22) {
            }
         }

         float curY = de.y;

         try {
            Field f = de.manager.getClass().getField("y");
            curY = ((Number)f.get(de.manager)).floatValue();
         } catch (Exception e) {
            try {
               Method m = de.manager.getClass().getMethod("getY");
               curY = ((Number)m.invoke(de.manager)).floatValue();
            } catch (Exception var20) {
            }
         }

         float clampedX = Math.max(0.0F, Math.min(maxElemX, curX));
         float clampedY = Math.max(0.0F, Math.min(maxElemY, curY));
         if (clampedX != curX || clampedY != curY) {
            de.apply(clampedX, clampedY);
         }
      } else {
         activeVLine = false;
         activeHLine = false;
      }

      return super.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button + 1, 0)), deltaX, deltaY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      activeVLine = false;
      activeHLine = false;
      if (AstolfoclientClient.targetHudManager != null) {
         AstolfoclientClient.targetHudManager.onMouseReleased(mouseX, mouseY, button);
      }

      if (AstolfoclientClient.boyKisserManager != null) {
         AstolfoclientClient.boyKisserManager.onMouseReleased(mouseX, mouseY, button);
      }

      if (AstolfoclientClient.effectHudManager != null) {
         AstolfoclientClient.effectHudManager.onMouseReleased(button);
      }

      if (AstolfoclientClient.armorHudManager != null) {
         AstolfoclientClient.armorHudManager.onMouseReleased(button);
      }

      if (AstolfoclientClient.inventoryHudManager != null) {
         AstolfoclientClient.inventoryHudManager.onMouseReleased(button);
      }

      if (AstolfoclientClient.activeBindsManager != null) {
         AstolfoclientClient.activeBindsManager.onMouseReleased(mouseX, mouseY, button);
      }

      if (AstolfoclientClient.testHudManager != null) {
         AstolfoclientClient.testHudManager.onMouseReleased(mouseX, mouseY, button);
      }

      if (AstolfoclientClient.musicHudManager != null) {
         AstolfoclientClient.musicHudManager.onMouseReleased(mouseX, mouseY, button);
      }

      if (AstolfoclientClient.infoHudManager != null) {
         AstolfoclientClient.infoHudManager.onMouseReleased(mouseX, mouseY, button);
      }

      this.currentlyDraggedManager = null;
      return super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button + 1, 0)));
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      AstolfoclientClient.musicHudManager.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private static void applyPosition(Object manager, float newX, float newY) {
      if (manager != null) {
         try {
            Field xField = manager.getClass().getField("x");
            if (xField.getType() == float.class) {
               xField.setFloat(manager, newX);
            } else if (xField.getType() == double.class) {
               xField.setDouble(manager, newX);
            }
         } catch (Exception e) {
            try {
               Method setX = manager.getClass().getMethod("setX", float.class);
               setX.invoke(manager, newX);
            } catch (Exception e2) {
               try {
                  Method setX = manager.getClass().getMethod("setX", double.class);
                  setX.invoke(manager, (double)newX);
               } catch (Exception var9) {
               }
            }
         }

         try {
            Field yField = manager.getClass().getField("y");
            if (yField.getType() == float.class) {
               yField.setFloat(manager, newY);
            } else if (yField.getType() == double.class) {
               yField.setDouble(manager, newY);
            }
         } catch (Exception e) {
            try {
               Method setY = manager.getClass().getMethod("setY", float.class);
               setY.invoke(manager, newY);
            } catch (Exception e2) {
               try {
                  Method setY = manager.getClass().getMethod("setY", double.class);
                  setY.invoke(manager, (double)newY);
               } catch (Exception var6) {
               }
            }
         }
      }
   }

   private HudEditorScreen.DraggedElement getDraggedElement() {
      Object[] managers = new Object[]{
         AstolfoclientClient.targetHudManager,
         AstolfoclientClient.boyKisserManager,
         AstolfoclientClient.effectHudManager,
         AstolfoclientClient.armorHudManager,
         AstolfoclientClient.inventoryHudManager,
         AstolfoclientClient.activeBindsManager,
         AstolfoclientClient.testHudManager,
         AstolfoclientClient.musicHudManager,
         AstolfoclientClient.infoHudManager
      };

      for (Object manager : managers) {
         if (manager != null) {
            boolean dragging = false;

            try {
               Field f = manager.getClass().getDeclaredField("dragging");
               f.setAccessible(true);
               dragging = f.getBoolean(manager);
            } catch (Exception e) {
               try {
                  Field f = manager.getClass().getDeclaredField("isDragging");
                  f.setAccessible(true);
                  dragging = f.getBoolean(manager);
               } catch (Exception var16) {
               }
            }

            if (dragging) {
               HudEditorScreen.DraggedElement de = new HudEditorScreen.DraggedElement();
               de.manager = manager;
               float xVal = 0.0F;

               try {
                  Field f = manager.getClass().getField("x");
                  xVal = ((Number)f.get(manager)).floatValue();
               } catch (Exception e) {
                  try {
                     Method m = manager.getClass().getMethod("getX");
                     xVal = ((Number)m.invoke(manager)).floatValue();
                  } catch (Exception var14) {
                  }
               }

               de.x = xVal;
               float yVal = 0.0F;

               try {
                  Field f = manager.getClass().getField("y");
                  yVal = ((Number)f.get(manager)).floatValue();
               } catch (Exception e) {
                  try {
                     Method m = manager.getClass().getMethod("getY");
                     yVal = ((Number)m.invoke(manager)).floatValue();
                  } catch (Exception var12) {
                  }
               }

               de.y = yVal;
               de.scaleModifier = this.getManagerScaleModifier(manager);
               de.width = this.getManagerWidth(manager);
               de.height = this.getManagerHeight(manager);
               return de;
            }
         }
      }

      return null;
   }

   private float getManagerScaleModifier(Object manager) {
      try {
         Method m = manager.getClass().getDeclaredMethod("getScaleModifier");
         m.setAccessible(true);
         return ((Number)m.invoke(manager)).floatValue();
      } catch (Exception e) {
         try {
            Field f = manager.getClass().getDeclaredField("scaleModifier");
            f.setAccessible(true);
            return ((Number)f.get(manager)).floatValue();
         } catch (Exception var4) {
            double currentGuiScale = Minecraft.getInstance().getWindow().getGuiScale();
            return (float)(2.0 / currentGuiScale);
         }
      }
   }

   private List<HudEditorScreen.ElementRect> getOtherElementRects(Object draggedManager) {
      List<HudEditorScreen.ElementRect> rects = new ArrayList<>();
      Object[] managers = new Object[]{
         AstolfoclientClient.targetHudManager,
         AstolfoclientClient.boyKisserManager,
         AstolfoclientClient.effectHudManager,
         AstolfoclientClient.armorHudManager,
         AstolfoclientClient.inventoryHudManager,
         AstolfoclientClient.activeBindsManager,
         AstolfoclientClient.testHudManager,
         AstolfoclientClient.musicHudManager,
         AstolfoclientClient.infoHudManager
      };

      for (Object manager : managers) {
         if (manager != null && manager != draggedManager) {
            try {
               float xVal = 0.0F;

               try {
                  Field f = manager.getClass().getField("x");
                  xVal = ((Number)f.get(manager)).floatValue();
               } catch (Exception e) {
                  try {
                     Method m = manager.getClass().getMethod("getX");
                     xVal = ((Number)m.invoke(manager)).floatValue();
                  } catch (Exception ex) {
                     continue;
                  }
               }

               float yVal = 0.0F;

               try {
                  Field f = manager.getClass().getField("y");
                  yVal = ((Number)f.get(manager)).floatValue();
               } catch (Exception e) {
                  try {
                     Method m = manager.getClass().getMethod("getY");
                     yVal = ((Number)m.invoke(manager)).floatValue();
                  } catch (Exception ex) {
                     continue;
                  }
               }

               float scale = this.getManagerScaleModifier(manager);
               float w = this.getManagerWidth(manager);
               float h = this.getManagerHeight(manager);
               HudEditorScreen.ElementRect r = new HudEditorScreen.ElementRect();
               r.manager = manager;
               r.left = xVal;
               r.top = yVal;
               r.right = xVal + w * scale;
               r.bottom = yVal + h * scale;
               r.centerX = r.left + w * scale / 2.0F;
               r.centerY = r.top + h * scale / 2.0F;
               rects.add(r);
            } catch (Exception var18) {
            }
         }
      }

      return rects;
   }

   private float getManagerWidth(Object manager) {
      if (manager == null) {
         return 100.0F;
      }

      try {
         Method m = manager.getClass().getMethod("getWidth");
         return ((Number)m.invoke(manager)).floatValue();
      } catch (Exception var5) {
         try {
            Field f = manager.getClass().getDeclaredField("currentWidth");
            f.setAccessible(true);
            return f.getFloat(manager);
         } catch (Exception var4) {
            try {
               Field f = manager.getClass().getDeclaredField("width");
               f.setAccessible(true);
               return f.getFloat(manager);
            } catch (Exception var3) {
               return 100.0F;
            }
         }
      }
   }

   private float getManagerHeight(Object manager) {
      if (manager == null) {
         return 30.0F;
      }

      try {
         Method m = manager.getClass().getMethod("getHeight");
         return ((Number)m.invoke(manager)).floatValue();
      } catch (Exception var5) {
         try {
            Field f = manager.getClass().getDeclaredField("currentHeight");
            f.setAccessible(true);
            return f.getFloat(manager);
         } catch (Exception var4) {
            try {
               Field f = manager.getClass().getDeclaredField("height");
               f.setAccessible(true);
               return f.getFloat(manager);
            } catch (Exception var3) {
               return 30.0F;
            }
         }
      }
   }

   private void applySnapping(HudEditorScreen.DraggedElement de) {
      if (!xyz.angames.astolfoclient.client.compat.GuiCompat.shiftDown()) {
         activeVLine = false;
         activeHLine = false;
      } else {
         float snapThreshold = 5.0F;
         float gridSpacing = 10.0F;
         float currentScale = de.scaleModifier;
         float left = de.x;
         float top = de.y;
         float width = de.width * currentScale;
         float height = de.height * currentScale;
         float right = left + width;
         float bottom = top + height;
         float centerX = left + width / 2.0F;
         float centerY = top + height / 2.0F;
         float snappedLeft = left;
         boolean xSnapped = false;
         activeVLine = false;
         activeVLineX = 0.0F;
         float screenCenterX = this.width / 2.0F;
         if (Math.abs(centerX - screenCenterX) < snapThreshold) {
            snappedLeft = screenCenterX - width / 2.0F;
            xSnapped = true;
            activeVLine = true;
            activeVLineX = screenCenterX;
         }

         List<HudEditorScreen.ElementRect> others = this.getOtherElementRects(de.manager);
         if (!xSnapped) {
            for (HudEditorScreen.ElementRect other : others) {
               if (Math.abs(centerX - other.centerX) < snapThreshold) {
                  snappedLeft = other.centerX - width / 2.0F;
                  xSnapped = true;
                  activeVLine = true;
                  activeVLineX = other.centerX;
                  break;
               }

               if (Math.abs(left - other.left) < snapThreshold) {
                  snappedLeft = other.left;
                  xSnapped = true;
                  activeVLine = true;
                  activeVLineX = other.left;
                  break;
               }

               if (Math.abs(right - other.right) < snapThreshold) {
                  snappedLeft = other.right - width;
                  xSnapped = true;
                  activeVLine = true;
                  activeVLineX = other.right;
                  break;
               }

               if (Math.abs(left - other.right) < snapThreshold) {
                  snappedLeft = other.right;
                  xSnapped = true;
                  activeVLine = true;
                  activeVLineX = other.right;
                  break;
               }

               if (Math.abs(right - other.left) < snapThreshold) {
                  snappedLeft = other.left - width;
                  xSnapped = true;
                  activeVLine = true;
                  activeVLineX = other.left;
                  break;
               }
            }
         }

         if (!xSnapped) {
            float margin = 10.0F;
            if (Math.abs(left - margin) < snapThreshold) {
               snappedLeft = margin;
               xSnapped = true;
            } else if (Math.abs(right - (this.width - margin)) < snapThreshold) {
               snappedLeft = this.width - margin - width;
               xSnapped = true;
            }
         }

         if (!xSnapped) {
            snappedLeft = Math.round(left / gridSpacing) * gridSpacing;
         }

         float snappedTop = top;
         boolean ySnapped = false;
         activeHLine = false;
         activeHLineY = 0.0F;
         float screenCenterY = this.height / 2.0F;
         if (Math.abs(centerY - screenCenterY) < snapThreshold) {
            snappedTop = screenCenterY - height / 2.0F;
            ySnapped = true;
            activeHLine = true;
            activeHLineY = screenCenterY;
         }

         if (!ySnapped) {
            for (HudEditorScreen.ElementRect other : others) {
               if (Math.abs(centerY - other.centerY) < snapThreshold) {
                  snappedTop = other.centerY - height / 2.0F;
                  ySnapped = true;
                  activeHLine = true;
                  activeHLineY = other.centerY;
                  break;
               }

               if (Math.abs(top - other.top) < snapThreshold) {
                  snappedTop = other.top;
                  ySnapped = true;
                  activeHLine = true;
                  activeHLineY = other.top;
                  break;
               }

               if (Math.abs(bottom - other.bottom) < snapThreshold) {
                  snappedTop = other.bottom - height;
                  ySnapped = true;
                  activeHLine = true;
                  activeHLineY = other.bottom;
                  break;
               }

               if (Math.abs(top - other.bottom) < snapThreshold) {
                  snappedTop = other.bottom;
                  ySnapped = true;
                  activeHLine = true;
                  activeHLineY = other.bottom;
                  break;
               }

               if (Math.abs(bottom - other.top) < snapThreshold) {
                  snappedTop = other.top - height;
                  ySnapped = true;
                  activeHLine = true;
                  activeHLineY = other.top;
                  break;
               }
            }
         }

         if (!ySnapped) {
            float margin = 10.0F;
            if (Math.abs(top - margin) < snapThreshold) {
               snappedTop = margin;
               ySnapped = true;
            } else if (Math.abs(bottom - (this.height - margin)) < snapThreshold) {
               snappedTop = this.height - margin - height;
               ySnapped = true;
            }
         }

         if (!ySnapped) {
            snappedTop = Math.round(top / gridSpacing) * gridSpacing;
         }

         float maxLeft = Math.max(0.0F, this.width - width);
         float maxTop = Math.max(0.0F, this.height - height);
         snappedLeft = Math.max(0.0F, Math.min(maxLeft, snappedLeft));
         snappedTop = Math.max(0.0F, Math.min(maxTop, snappedTop));
         de.apply(snappedLeft, snappedTop);
      }
   }

   @Environment(EnvType.CLIENT)
   private static class DraggedElement {
      Object manager;
      float x;
      float y;
      float width;
      float height;
      float scaleModifier;

      void apply(float newX, float newY) {
         HudEditorScreen.applyPosition(this.manager, newX, newY);
      }
   }

   @Environment(EnvType.CLIENT)
   private static class ElementRect {
      Object manager;
      float left;
      float top;
      float right;
      float bottom;
      float centerX;
      float centerY;
   }
}

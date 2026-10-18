package xyz.angames.astolfoclient.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
@SuppressWarnings("unchecked")
public class IASAccountHelper {
   public static void onScreenInit(Screen screen, Consumer<Button> buttonAdder) {
      if (screen != null && buttonAdder != null) {
         String className = screen.getClass().getName();
         if (className.contains("ias") && (className.contains("Account") || className.contains("Switcher"))) {
            rebalanceAndAddRandomButton(screen, buttonAdder);
         }
      }
   }

   private static void rebalanceAndAddRandomButton(Screen screen, Consumer<Button> buttonAdder) {
      int targetRowY = screen.height - 48;
      int btnHeight = 20;
      int btnWidth = 74;
      int gap = 4;
      int startX = screen.width / 2 - 154;
      List<AbstractWidget> topRowWidgets = new ArrayList<>();

      try {
         for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.getY() >= screen.height - 60 && widget.getY() <= screen.height - 35) {
               topRowWidgets.add(widget);
            }
         }
      } catch (Throwable var11) {
      }

      topRowWidgets.sort(Comparator.comparingInt(AbstractWidget::getX));
      if (topRowWidgets.size() >= 3) {
         AbstractWidget btn0 = topRowWidgets.get(0);
         btn0.setX(startX);
         btn0.setY(targetRowY);
         btn0.setWidth(btnWidth);
         AbstractWidget btn1 = topRowWidgets.get(1);
         btn1.setX(startX + btnWidth + gap);
         btn1.setY(targetRowY);
         btn1.setWidth(btnWidth);
         AbstractWidget btn2 = topRowWidgets.get(2);
         btn2.setX(startX + (btnWidth + gap) * 2);
         btn2.setY(targetRowY);
         btn2.setWidth(btnWidth);
      }

      int randomBtnX = startX + (btnWidth + gap) * 3;
      int randomBtnY = targetRowY;
      Button randomBtn = Button.builder(Component.literal("Random"), btn -> addRandomOfflineAccount(screen))
         .bounds(randomBtnX, randomBtnY, btnWidth, btnHeight)
         .build();
      buttonAdder.accept(randomBtn);
   }

   public static void addRandomOfflineAccount(Screen screen) {
      try {
         String randomName = RandomNameGenerator.generateUniqueName();
         // IAS is an optional third-party mod, so it is reached reflectively.
         Class<?> offlineAccount = Class.forName("ru.vidtu.ias.account.OfflineAccount");
         Object account = offlineAccount.getConstructors()[0].newInstance(randomName, null);
         Class<?> storage = Class.forName("ru.vidtu.ias.config.IASStorage");
         ((java.util.List<Object>)storage.getField("ACCOUNTS").get(null)).add(account);
         Class<?> ias = Class.forName("ru.vidtu.ias.IAS");
         ias.getMethod("disclaimersStorage").invoke(null);
         ias.getMethod("saveStorage").invoke(null);

         for (Field field : screen.getClass().getDeclaredFields()) {
            if (field.getType().getName().equals("ru.vidtu.ias.screen.AccountList")) {
               field.setAccessible(true);
               Object listObj = field.get(screen);
               if (listObj != null) {
                  Method updateMethod = listObj.getClass().getDeclaredMethod("update", String.class);
                  updateMethod.setAccessible(true);
                  String searchText = "";

                  for (Field sField : screen.getClass().getDeclaredFields()) {
                     if (EditBox.class.isAssignableFrom(sField.getType())) {
                        sField.setAccessible(true);
                        EditBox tf = (EditBox)sField.get(screen);
                        if (tf != null) {
                           searchText = tf.getValue();
                           break;
                        }
                     }
                  }

                  updateMethod.invoke(listObj, searchText);
               }
               break;
            }
         }
      } catch (Throwable t) {
         t.printStackTrace();
      }
   }
}

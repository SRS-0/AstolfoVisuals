package xyz.angames.astolfoclient.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.angames.astolfoclient.client.util.RandomNameGenerator;

@Environment(value=EnvType.CLIENT)
public class IASAccountHelper {
    public static void onScreenInit(Screen screen, Consumer<Button> buttonAdder) {
        if (screen == null || buttonAdder == null) {
            return;
        }
        String className = screen.getClass().getName();
        if (className.contains("ias") && (className.contains("Account") || className.contains("Switcher"))) {
            IASAccountHelper.rebalanceAndAddRandomButton(screen, buttonAdder);
        }
    }

    private static void rebalanceAndAddRandomButton(Screen screen, Consumer<Button> buttonAdder) {
        int targetRowY = screen.height - 48;
        int btnHeight = 20;
        int btnWidth = 74;
        int gap = 4;
        int startX = screen.width / 2 - 154;
        ArrayList<AbstractWidget> topRowWidgets = new ArrayList<AbstractWidget>();
        try {
            for (GuiEventListener child : screen.children()) {
                AbstractWidget widget;
                if (!(child instanceof AbstractWidget) || (widget = (AbstractWidget)child).getY() < screen.height - 60 || widget.getY() > screen.height - 35) continue;
                topRowWidgets.add(widget);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        topRowWidgets.sort(Comparator.comparingInt(AbstractWidget::getX));
        if (topRowWidgets.size() >= 3) {
            AbstractWidget btn0 = (AbstractWidget)topRowWidgets.get(0);
            btn0.setX(startX);
            btn0.setY(targetRowY);
            btn0.setWidth(btnWidth);
            AbstractWidget btn1 = (AbstractWidget)topRowWidgets.get(1);
            btn1.setX(startX + (btnWidth + gap));
            btn1.setY(targetRowY);
            btn1.setWidth(btnWidth);
            AbstractWidget btn2 = (AbstractWidget)topRowWidgets.get(2);
            btn2.setX(startX + (btnWidth + gap) * 2);
            btn2.setY(targetRowY);
            btn2.setWidth(btnWidth);
        }
        int randomBtnX = startX + (btnWidth + gap) * 3;
        int randomBtnY = targetRowY;
        Button randomBtn = Button.builder((Component)Component.literal((String)"Random"), btn -> IASAccountHelper.addRandomOfflineAccount(screen)).bounds(randomBtnX, randomBtnY, btnWidth, btnHeight).build();
        buttonAdder.accept(randomBtn);
    }

    public static void addRandomOfflineAccount(Screen screen) {
        try {
            String randomName = RandomNameGenerator.generateUniqueName();
            Object account = Class.forName("ru.vidtu.ias.account.OfflineAccount").getConstructor(String.class, java.util.UUID.class).newInstance(randomName, null);
            ((java.util.Collection<Object>)Class.forName("ru.vidtu.ias.config.IASStorage").getField("ACCOUNTS").get(null)).add(account);
            Class.forName("ru.vidtu.ias.IAS").getMethod("disclaimersStorage").invoke(null);
            Class.forName("ru.vidtu.ias.IAS").getMethod("saveStorage").invoke(null);
            for (Field field : screen.getClass().getDeclaredFields()) {
                if (!field.getType().getName().equals("ru.vidtu.ias.screen.AccountList")) continue;
                field.setAccessible(true);
                Object listObj = field.get(screen);
                if (listObj != null) {
                    Method updateMethod = listObj.getClass().getDeclaredMethod("update", String.class);
                    updateMethod.setAccessible(true);
                    String searchText = "";
                    for (Field sField : screen.getClass().getDeclaredFields()) {
                        if (!EditBox.class.isAssignableFrom(sField.getType())) continue;
                        sField.setAccessible(true);
                        EditBox tf = (EditBox)sField.get(screen);
                        if (tf == null) continue;
                        searchText = tf.getValue();
                        break;
                    }
                    updateMethod.invoke(listObj, searchText);
                }
                break;
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}


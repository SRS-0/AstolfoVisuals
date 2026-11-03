package xyz.angames.astolfoclient.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.angames.astolfoclient.client.util.RandomNameGenerator;

@Environment(value=EnvType.CLIENT)
public class IASAccountHelper {
    public static void onScreenInit(Screen screen, Consumer<ButtonWidget> buttonAdder) {
        if (screen == null || buttonAdder == null) {
            return;
        }
        String className = screen.getClass().getName();
        if (className.contains("ias") && (className.contains("Account") || className.contains("Switcher"))) {
            IASAccountHelper.rebalanceAndAddRandomButton(screen, buttonAdder);
        }
    }

    private static void rebalanceAndAddRandomButton(Screen screen, Consumer<ButtonWidget> buttonAdder) {
        int targetRowY = screen.height - 48;
        int btnHeight = 20;
        int btnWidth = 74;
        int gap = 4;
        int startX = screen.width / 2 - 154;
        ArrayList<ClickableWidget> topRowWidgets = new ArrayList<ClickableWidget>();
        try {
            for (Element child : screen.children()) {
                ClickableWidget widget;
                if (!(child instanceof ClickableWidget) || (widget = (ClickableWidget)child).getY() < screen.height - 60 || widget.getY() > screen.height - 35) continue;
                topRowWidgets.add(widget);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        topRowWidgets.sort(Comparator.comparingInt(ClickableWidget::getX));
        if (topRowWidgets.size() >= 3) {
            ClickableWidget btn0 = (ClickableWidget)topRowWidgets.get(0);
            btn0.setX(startX);
            btn0.setY(targetRowY);
            btn0.setWidth(btnWidth);
            ClickableWidget btn1 = (ClickableWidget)topRowWidgets.get(1);
            btn1.setX(startX + (btnWidth + gap));
            btn1.setY(targetRowY);
            btn1.setWidth(btnWidth);
            ClickableWidget btn2 = (ClickableWidget)topRowWidgets.get(2);
            btn2.setX(startX + (btnWidth + gap) * 2);
            btn2.setY(targetRowY);
            btn2.setWidth(btnWidth);
        }
        int randomBtnX = startX + (btnWidth + gap) * 3;
        int randomBtnY = targetRowY;
        ButtonWidget randomBtn = ButtonWidget.builder((Text)Text.literal((String)"Random"), btn -> IASAccountHelper.addRandomOfflineAccount(screen)).dimensions(randomBtnX, randomBtnY, btnWidth, btnHeight).build();
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
                        if (!TextFieldWidget.class.isAssignableFrom(sField.getType())) continue;
                        sField.setAccessible(true);
                        TextFieldWidget tf = (TextFieldWidget)sField.get(screen);
                        if (tf == null) continue;
                        searchText = tf.getText();
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


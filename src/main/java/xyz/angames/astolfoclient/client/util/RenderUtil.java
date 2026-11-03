package xyz.angames.astolfoclient.client.util;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.*;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Matrix4f;
public final class RenderUtil {
 private RenderUtil() {}
 private static QuadRadiusState radii(BorderRadius radius) {return new QuadRadiusState(radius.topLeft(),radius.bottomLeft(),radius.bottomRight(),radius.topRight());}
 public static void drawRoundedRect(Matrix4f matrix, VertexConsumerProvider consumers,float x,float y,float width,float height,BorderRadius radius,int color) {
  Builder.rectangle().size(new SizeState(width,height)).radius(radii(radius)).color(new QuadColorState(color)).build().render(matrix,x,y);
 }
 public static void drawRoundedRectOutline(Matrix4f matrix, VertexConsumerProvider consumers,float x,float y,float width,float height,BorderRadius radius,int color,float thickness) {
  Builder.border().size(new SizeState(width,height)).radius(radii(radius)).color(new QuadColorState(color)).thickness(thickness).build().render(matrix,x,y);
 }
}

package dev.sxmurxy.mre.providers;

import java.awt.Color;

public final class ColorProvider {
	
	public static int pack(int red, int green, int blue, int alpha) {
		return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 0);
	}
	
	public static int[] unpack(int color) {
		return new int[] {color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF};
	}
	
	public static float[] normalize(Color color) {
		return new float[] {color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f};
	}

	public static float[] normalize(int color) {
		int[] components = unpack(color);
		return new float[] {components[0] / 255.0f, components[1] / 255.0f, components[2] / 255.0f, components[3] / 255.0f};
	}

}
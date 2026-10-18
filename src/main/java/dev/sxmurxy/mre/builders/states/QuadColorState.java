package dev.sxmurxy.mre.builders.states;

import java.awt.Color;

public record QuadColorState(int color1, int color2, int color3, int color4) {

	public static final QuadColorState TRANSPARENT = new QuadColorState(0, 0, 0, 0);
	public static final QuadColorState WHITE = new QuadColorState(-1, -1, -1, -1);

	public QuadColorState(Color color1, Color color2, Color color3, Color color4) {
		this(color1.getRGB(), color2.getRGB(), color3.getRGB(), color4.getRGB());
	}

    public QuadColorState(Color color) {
		this(color, color, color, color);
	}
	
	public QuadColorState(int color) {
		this(color, color, color, color);
	}

}
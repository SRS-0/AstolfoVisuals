package dev.sxmurxy.mre.builders.states;

public record SizeState(float width, float height) {

	public static final SizeState NONE = new SizeState(0.0f, 0.0f);

    public SizeState(double width, double height) {
		this((float) width, (float) height);
	}

}
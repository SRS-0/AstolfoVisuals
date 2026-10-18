package dev.sxmurxy.mre.builders.states;

public record QuadRadiusState(float radius1, float radius2, float radius3, float radius4) {

	public static final QuadRadiusState NO_ROUND = new QuadRadiusState(0.0f, 0.0f, 0.0f, 0.0f);

	public QuadRadiusState(double radius1, double radius2, double radius3, double radius4) {
		this((float) radius1, (float) radius2, (float) radius3, (float) radius4);
	}

	public QuadRadiusState(double radius) {
		this(radius, radius, radius, radius);
	}

	public QuadRadiusState(float radius) {
		this(radius, radius, radius, radius);
	}

}
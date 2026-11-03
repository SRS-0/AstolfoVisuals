package xyz.angames.astolfoclient.client.render.compat;

import java.nio.ByteBuffer;
import org.joml.Matrix4f;

/** CPU-side parameter value, serialized to a std140 block for each draw. */
public final class GlUniform {
    private final float[] values;
    private final boolean integer;
    private final int offset;
    GlUniform(int count, boolean integer) { values = new float[Math.max(4, count)]; this.integer = integer; offset=0; }
    private GlUniform(float[] values, int offset, boolean integer) { this.values=values;this.offset=offset;this.integer=integer; }
    GlUniform element(int index) { if(index<0 || index*4>=values.length)throw new IndexOutOfBoundsException(index);return new GlUniform(values,index*4,integer); }
    public void set(float... value) { System.arraycopy(value, 0, values, offset, Math.min(value.length, values.length-offset)); }
    public void set(Matrix4f matrix) { matrix.get(values); }
    public void set(int value) { values[offset] = value; }
    void write(ByteBuffer target) { for (float v : values) { if(integer) target.putInt((int)v); else target.putFloat(v); } }
    int size() { return values.length * Float.BYTES; }
    void writeAt(ByteBuffer target,int start,int components,int length,int stride) {
        for(int item=0;item<length;item++)for(int component=0;component<components;component++) {
            int source=item*Math.max(4,components)+component;
            int destination=start+item*stride+component*4;
            if(integer)target.putInt(destination,(int)values[source]);else target.putFloat(destination,values[source]);
        }
    }
}

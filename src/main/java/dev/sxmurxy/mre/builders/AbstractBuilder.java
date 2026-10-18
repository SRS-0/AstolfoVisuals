package dev.sxmurxy.mre.builders;

public abstract class AbstractBuilder<T> {

	public AbstractBuilder() {
		this.reset();
	}

	public final T build() {
		T instance =  this._build();
		this.reset();

		return instance;
	}

	protected abstract void reset();

	protected abstract T _build();

}
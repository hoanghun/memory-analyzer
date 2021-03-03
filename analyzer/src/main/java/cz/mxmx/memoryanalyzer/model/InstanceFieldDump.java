package cz.mxmx.memoryanalyzer.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

/**
 * Instance field.
 * @param <T> Type of the field.
 */
public class InstanceFieldDump<T> {
	private final String name;
	private final Class<T> type;

	/**
	 * Creates an instance field.
	 * @param name Name of the field.
	 * @param type Type of the field.
	 */
	public InstanceFieldDump(String name, Class<T> type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Class<T> getType() {
		return type;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("name", name)
				.append("type", type)
				.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InstanceFieldDump<?> that = (InstanceFieldDump<?>) o;
		return Objects.equals(name, that.name) && Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type);
	}
}

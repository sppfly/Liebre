/*  Copyright (C) 2017-2018  Vincenzo Gulisano, Dimitris Palyvos Giannas
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Contact:
 *    Vincenzo Gulisano info@vincenzogulisano.com
 *    Dimitris Palyvos Giannas palyvos@chalmers.se
 */
package common.component;

import common.tuple.Tuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import stream.Stream;

/**
 * Object that represents the state of all common stream components such as operators, sinks and
 * sources.
 *
 * @param <IN> The type of the inputs of the component where the state belongs to.
 * @param <OUT> The type of the outputs of the component where the state belongs to.
 * @author palivosd
 */
public class ComponentState<IN extends Tuple, OUT extends Tuple> {

  private static AtomicInteger nextIndex = new AtomicInteger();
  private final ComponentType type;
  private final String id;
  private final int index;
  private final List<Stream<IN>> inputs = new ArrayList<>();
  private final List<Stream<OUT>> outputs = new ArrayList<>();

  private volatile boolean enabled = false;

  /**
   * Construct.
   *
   * @param id The unique ID of the component
   * @param type The type of the component
   */
  public ComponentState(String id, ComponentType type) {
    Validate.notBlank(id);
    Validate.notNull(type);
    this.id = id;
    this.type = type;
    this.index = nextIndex.getAndIncrement();
  }

  public void addOutput(int index, Stream<OUT> stream) {
    outputs.add(index, stream);
    type.validateOutputs(this);
  }

  public void addOutput(Stream<OUT> stream) {
    outputs.add(stream);
    type.validateOutputs(this);
  }

  public void addInput(int index, Stream<IN> stream) {
    inputs.add(index, stream);
    type.validateInputs(this);
  }

  public void addInput(Stream<IN> stream) {
    inputs.add(stream);
    type.validateInputs(this);
  }

  public Stream<IN> getInput(int index) {
    return inputs.get(index);
  }

  public Stream<IN> getInput() {
    return getInput(0);
  }

  public Stream<OUT> getOutput(int index) {
    return outputs.get(index);
  }

  public Stream<OUT> getOutput() {
    return getOutput(0);
  }

  /**
   * Enable the state. Should always be called when calling {@link Component#enable()}
   */
  public void enable() {
    type.validate(this);
    for (Stream<?> input : inputs) {
      input.enable();
    }
    this.enabled = true;
  }

  /**
   * @return {@code true} if the state is enabled
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Disable the state. Should always be called when calling {@link Component#disable()}
   */
  public void disable() {
    for (Stream<?> input : inputs) {
      input.disable();
    }
    this.enabled = false;
  }

  /**
   * Get the unique ID of the state.
   *
   * @return The unique ID of the state.
   */
  public String getId() {
    return id;
  }

  /**
   * Get the unique numerical ID of the state. This can be the same or different than {@link
   * #getId()}.
   *
   * @return The unique numerical index of the state.
   */
  public int getIndex() {
    return index;
  }


  /**
   * Get all the input streams of this state.
   *
   * @return The input streams.
   */
  public Collection<Stream<IN>> getInputs() {
    return Collections.unmodifiableCollection(inputs);
  }

  public Collection<Stream<OUT>> getOutputs() {
    return Collections.unmodifiableCollection(outputs);
  }

  /**
   * Verify that this state has input streams with {@link Tuple}s which can be read.
   *
   * @return {@code true} if all input streams are not empty.
   */
  public boolean hasInput() {
    for (Stream<?> in : inputs) {
      if (in.peek() == null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Verify that this state has output streams with non-zero capacity.
   *
   * @return {@code true} if all output streams have non-zero capacity.
   */
  public boolean hasOutput() {
    for (Stream<OUT> output : outputs) {
      if (output.remainingCapacity() == 0) {
        return false;
      }
    }
    return true;
  }

  public ConnectionsNumber inputsNumber() {
    return type.inputsNumber();
  }

  public ConnectionsNumber outputsNumber() {
    return type.outputsNumber();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ComponentState<?, ?> that = (ComponentState<?, ?>) o;

    return new EqualsBuilder()
        .append(index, that.index)
        .append(id, that.id)
        .append(type, that.type)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(id)
        .append(index)
        .append(type)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("type", type)
        .append("id", id)
        .append("index", index)
        .append("inputs", inputs)
        .append("outputs", outputs)
        .append("enabled", enabled)
        .toString();
  }
}

/* Copyright (C) 2019 Markus Frohme.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.spa.impl;

import java.util.Stack;

import net.automatalib.commons.util.Pair;

/**
 * A state in a {@link DefaultSPA}. Consist of a location and a stack content.
 *
 * @param <I>
 *         input symbol type
 * @param <S>
 *         hypothesis state type
 *
 * @author frohme
 */
public class State<I, S> extends Pair<I, S> {

    private final Stack<State<I, S>> stack;

    public State(I first, S second) {
        super(first, second);
        this.stack = new Stack<>();
    }

    public State(I first, S second, final Stack<State<I, S>> stack) {
        super(first, second);
        this.stack = stack;
    }

    @SuppressWarnings("unchecked")
    public State(I first, S second, Stack<State<I, S>> stack, State<I, S> newTopOfStack) {
        super(first, second);
        this.stack = (Stack<State<I, S>>) stack.clone();
        this.stack.add(newTopOfStack);
    }

    @Override
    public String toString() {
        return "State{" + "tuple=" + this.getFirst() + ',' + this.getSecond() + ", stack=" + this.stack + '}';
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getStack() != null ? getStack().hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        State<?, ?> that = (State<?, ?>) o;

        return getStack().equals(that.getStack());
    }

    public Stack<State<I, S>> getStack() {
        return stack;
    }
}

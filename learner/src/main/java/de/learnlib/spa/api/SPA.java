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
package de.learnlib.spa.api;

import java.util.Map;

import javax.annotation.Nullable;

import net.automatalib.automata.concepts.InputAlphabetHolder;
import net.automatalib.automata.concepts.SuffixOutput;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.graphs.concepts.GraphViewable;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;

/**
 * A system of procedural automata.
 *
 * @param <I>
 *         input symbol type
 */
public interface SPA<S, I>
        extends GraphViewable, InputAlphabetHolder<I>, DeterministicAcceptorTS<S, I>, SuffixOutput<I, Boolean> {

    @Nullable
    I getInitialProcedure();

    @Override
    SPAAlphabet<I> getInputAlphabet();

    default int size() {
        return getProcedures().values().stream().mapToInt(DFA::size).sum();
    }

    Map<I, DFA<?, I>> getProcedures();

    @Override
    default Boolean computeOutput(Iterable<? extends I> input) {
        return this.accepts(input);
    }
}

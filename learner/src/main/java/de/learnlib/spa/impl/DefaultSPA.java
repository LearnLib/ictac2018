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

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import de.learnlib.api.oracle.QueryAnswerer;
import de.learnlib.spa.api.SPA;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.view.ReachabilityView;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.commons.util.collections.IterableUtil;
import net.automatalib.graphs.Graph;
import net.automatalib.ts.simple.SimpleDTS;
import net.automatalib.words.Word;

/**
 * A stack-based implementation for the (instrumented) semantics of a System of Procedural Automata.
 *
 * @param <S>
 *         hypotheses state type
 * @param <I>
 *         input symbol type
 *
 * @author frohme
 */
public class DefaultSPA<S, I> implements SPA<State<I, S>, I>, SimpleDTS<State<I, S>, I>, QueryAnswerer<I, Boolean> {

    private final State<I, S> init = new State<I, S>(null, null) {

        @Override
        public String toString() {
            return "init";
        }
    };

    private final State<I, S> sink = new State<I, S>(null, null) {

        @Override
        public String toString() {
            return "sink";
        }
    };

    private final State<I, S> terminatingState = new State<I, S>(null, null) {

        @Override
        public String toString() {
            return "acc";
        }
    };

    private final SPAAlphabet<I> alphabet;
    private final I initialCall;
    private final Map<I, ? extends DFA<S, I>> procedures;

    public DefaultSPA(SPAAlphabet<I> alphabet, I initialCall, Map<I, ? extends DFA<S, I>> procedures) {
        this.alphabet = alphabet;
        this.initialCall = initialCall;

        this.procedures = procedures;
    }

    @Override
    public State<I, S> getTransition(State<I, S> state, I input) {
        if (this.initialCall == null || this.sink == state || this.terminatingState == state) {
            return sink;
        } else if (alphabet.isInternalSymbol(input)) {
            if (state == init) {
                return this.sink;
            }
            final I identifier = state.getFirst();
            final DFA<S, I> effectiveModel = this.procedures.get(identifier);

            final S next = effectiveModel.getTransition(state.getSecond(), input);

            // undefined internal transition
            if (next == null) {
                return sink;
            }

            return new State<>(identifier, next, state.getStack());
        } else if (alphabet.isCallSymbol(input)) {

            final DFA<S, I> effectiveModel = this.procedures.get(input);

            if (effectiveModel == null) {
                return sink;
            }

            final S next = effectiveModel.getInitialState();

            return new State<>(input, next, state.getStack(), state);
        } else if (alphabet.isReturnSymbol(input)) {

            if (state == init) {
                return sink;
            }

            final I identifier = state.getFirst();
            final DFA<S, I> model = this.procedures.get(identifier);

            // cannot return, return unaccepted word
            if (!model.isAccepting(state.getSecond())) {
                return sink;
            }

            final State<I, S> previousState = state.getStack().peek();

            if (previousState == init) {
                if (this.initialCall.equals(identifier)) {
                    return terminatingState;
                } else {
                    return sink;
                }
            }

            final I previousIdentifier = previousState.getFirst();

            final DFA<S, I> effectiveModel = this.procedures.get(previousIdentifier);
            final S next = effectiveModel.getSuccessor(previousState.getSecond(), state.getFirst());

            // undefined internal transition
            if (next == null) {
                return sink;
            }

            return new State<>(previousIdentifier, next, previousState.getStack());
        } else {
            return this.sink;
        }
    }

    @Override
    public boolean isAccepting(State<I, S> state) {
        return this.terminatingState == state;
    }

    @Override
    public State<I, S> getInitialState() {
        return this.init;
    }

    @Nullable
    @Override
    public I getInitialProcedure() {
        return initialCall;
    }

    @Override
    public SPAAlphabet<I> getInputAlphabet() {
        return this.alphabet;
    }

    @Override
    public Map<I, DFA<?, I>> getProcedures() {
        return Collections.unmodifiableMap(procedures);
    }

    @Override
    public Graph<?, ?> graphView() {
        return new ReachabilityView<>(alphabet, procedures);
    }

    @Nullable
    @Override
    public Boolean answerQuery(Word<I> prefix, Word<I> suffix) {
        return this.computeSuffixOutput(prefix, suffix);
    }

    @Override
    public Boolean computeSuffixOutput(Iterable<? extends I> iterable, Iterable<? extends I> iterable1) {
        return this.accepts(IterableUtil.concat(iterable, iterable1));
    }
}
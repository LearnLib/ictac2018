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
package de.learnlib.spa.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.learnlib.spa.TransformationUtil;
import de.learnlib.spa.api.SPA;
import de.learnlib.spa.api.SPAAlphabet;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.commons.util.Pair;
import net.automatalib.util.automata.Automata;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Alphabet;
import net.automatalib.words.GrowingAlphabet;
import net.automatalib.words.VPDAlphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.GrowingMapAlphabet;

public class Util {

    public static <I> Map<I, Word<I>> computeTerminatingSequences(VPDAlphabet<I> alphabet,
                                                                  I startProcedure,
                                                                  Map<I, DFA<?, I>> submodels,
                                                                  TransformationUtil<I> transformationUtil) {

        final Map<I, Word<I>> terminatingSequences = Maps.newHashMapWithExpectedSize(submodels.size());

        // initial internal sequences
        for (final Entry<I, DFA<?, I>> entry : submodels.entrySet()) {
            final I procedure = entry.getKey();
            final DFA<?, I> dfa = entry.getValue();

            if (dfa.accepts(Word.epsilon())) {
                terminatingSequences.put(procedure, Word.epsilon());

            } else {
                final Iterator<Word<I>> iter = Covers.stateCoverIterator(dfa, alphabet.getInternalAlphabet());
                while (iter.hasNext()) {
                    final Word<I> trace = iter.next();
                    if (dfa.accepts(trace)) {
                        terminatingSequences.put(procedure, trace);
                        break;
                    }
                }
            }

        }

        final Set<I> remainingProcedures = new HashSet<>(submodels.keySet());
        remainingProcedures.add(startProcedure);
        remainingProcedures.removeAll(terminatingSequences.keySet());

        boolean stable = false;

        while (!stable) {
            stable = true;

            final Set<I> eligibleInputs = new HashSet<>(alphabet.getInternalAlphabet());
            eligibleInputs.addAll(terminatingSequences.keySet());

            for (final I i : new ArrayList<>(remainingProcedures)) {

                final DFA<?, I> dfa = submodels.get(i);
                final Iterator<Word<I>> iter = Covers.stateCoverIterator(dfa, eligibleInputs);

                while (iter.hasNext()) {
                    final Word<I> trace = iter.next();
                    if (dfa.accepts(trace)) {
                        terminatingSequences.put(i, transformationUtil.expand(trace, terminatingSequences::get));

                        remainingProcedures.remove(i);
                        eligibleInputs.add(i);
                        stable = false;
                        break;
                    }
                }
            }
        }

        if (!remainingProcedures.isEmpty()) {
            throw new IllegalStateException("There are non-terminating procedures: " + remainingProcedures);
        }

        return terminatingSequences;
    }

    public static <I> Pair<Map<I, Word<I>>, Map<I, Word<I>>> computeAccessAndReturnSequences(VPDAlphabet<I> alphabet,
                                                                                             I startProcedure,
                                                                                             Alphabet<I> subModelAlphabet,
                                                                                             Map<I, DFA<?, I>> submodels,
                                                                                             Map<I, Word<I>> terminatingSequences,
                                                                                             TransformationUtil<I> transformationUtil) {

        final Map<I, Word<I>> accessSequences = Maps.newHashMapWithExpectedSize(submodels.size());
        final Map<I, Word<I>> returnSequences = Maps.newHashMapWithExpectedSize(submodels.size());

        final Set<I> finishedProcedures = Sets.newHashSetWithExpectedSize(submodels.size());

        // initial value
        accessSequences.put(startProcedure, Word.epsilon());
        returnSequences.put(startProcedure, Word.epsilon());
        finishedProcedures.add(startProcedure);

        boolean stable = false;

        while (!stable) {
            stable = true;

            for (final I i : new ArrayList<>(finishedProcedures)) {
                stable &= !computeAccessAndReturnSequencesInternal(alphabet,
                                                                   subModelAlphabet,
                                                                   i,
                                                                   submodels.get(i),
                                                                   finishedProcedures,
                                                                   terminatingSequences,
                                                                   accessSequences,
                                                                   returnSequences,
                                                                   transformationUtil);
            }
        }

        if (!finishedProcedures.containsAll(submodels.keySet())) {
            throw new IllegalStateException("There are non-accessible procedures");
        }

        return Pair.of(accessSequences, returnSequences);
    }

    private static <S, I> boolean computeAccessAndReturnSequencesInternal(VPDAlphabet<I> alphabet,
                                                                          Alphabet<I> subModelAlphabet,
                                                                          I procedure,
                                                                          DFA<S, I> dfa,
                                                                          Set<I> finishedProcedures,
                                                                          Map<I, Word<I>> terminatingSequences,
                                                                          Map<I, Word<I>> accessSequences,
                                                                          Map<I, Word<I>> returnSequences,
                                                                          TransformationUtil<I> transformationUtil) {

        boolean updated = false;

        final Iterator<Word<I>> transitionCoverIterator = Covers.transitionCoverIterator(dfa, subModelAlphabet);

        while (transitionCoverIterator.hasNext()) {
            final Word<I> trace = transitionCoverIterator.next();

            if (dfa.accepts(trace)) {

                final WordBuilder<I> potentialAccessBuilder = new WordBuilder<>();
                final Iterator<I> iter = trace.iterator();

                while (iter.hasNext()) {
                    final I input = iter.next();

                    if (alphabet.isCallSymbol(input) && !finishedProcedures.contains(input)) {

                        final Word<I> localAccess =
                                transformationUtil.expand(potentialAccessBuilder.toWord(), terminatingSequences::get);
                        accessSequences.put(input,
                                            accessSequences.get(procedure).append(procedure).concat(localAccess));

                        final WordBuilder<I> terminatingBuilder = new WordBuilder<>();
                        while (iter.hasNext()) {
                            terminatingBuilder.add(iter.next());
                        }

                        final Word<I> localTerminating =
                                transformationUtil.expand(terminatingBuilder.toWord(), terminatingSequences::get);
                        returnSequences.put(input,
                                            localTerminating.append(alphabet.getReturnSymbol(0))
                                                            .concat(returnSequences.get(procedure)));

                        finishedProcedures.add(input);

                        updated = true;
                    }

                    potentialAccessBuilder.add(input);
                }

                if (finishedProcedures.containsAll(alphabet.getCallAlphabet())) {
                    return updated;
                }

                potentialAccessBuilder.clear();
            }
        }
        return updated;
    }

    public static <I> boolean testEquivalence(SPA<?, I> expected, SPA<?, I> actual) {
        final SPAAlphabet<I> alphabet = expected.getInputAlphabet();
        final GrowingAlphabet<I> proceduralAlphabet = new GrowingMapAlphabet<>();
        proceduralAlphabet.addAll(alphabet.getCallAlphabet());
        proceduralAlphabet.addAll(alphabet.getInternalAlphabet());

        for (final I procedure : alphabet.getCallAlphabet()) {
            final DFA<?, I> expectedProcedure = expected.getProcedures().get(procedure);
            final DFA<?, I> actualProcedure = actual.getProcedures().get(procedure);

            if (!Automata.testEquivalence(expectedProcedure, actualProcedure, proceduralAlphabet)) {
                return false;
            }
        }

        return true;
    }

}

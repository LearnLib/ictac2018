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

import java.util.Collection;
import java.util.Map;

import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.spa.TransformationUtil;
import de.learnlib.spa.api.SPA;
import de.learnlib.spa.api.SPAAlphabet;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.commons.util.Pair;
import net.automatalib.util.automata.Automata;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.fsa.MutableDFAs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.GrowingAlphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.GrowingMapAlphabet;

public class SPAEQ<I> implements EquivalenceOracle<SPA<?, I>, I, Boolean> {

    private final SPA<?, I> spa;

    private final Map<I, Word<I>> terminatingSequences;
    private final Map<I, Word<I>> accessSequences;
    private final Map<I, Word<I>> returnSequences;
    private final TransformationUtil<I> transformationUtil;

    private final CompactDFA<I> rejectAllAutomaton;

    public SPAEQ(SPA<?, I> spa) {
        this.spa = spa;

        final SPAAlphabet<I> alphabet = spa.getInputAlphabet();
        final Alphabet<I> proceduralAlphabet = new GrowingMapAlphabet<>();

        proceduralAlphabet.addAll(alphabet.getCallAlphabet());
        proceduralAlphabet.addAll(alphabet.getInternalAlphabet());

        this.transformationUtil = new TransformationUtil<>(alphabet);

        rejectAllAutomaton = new CompactDFA<>(proceduralAlphabet);
        rejectAllAutomaton.addInitialState(false);
        MutableDFAs.complete(rejectAllAutomaton, proceduralAlphabet);

        this.terminatingSequences = Util.computeTerminatingSequences(alphabet,
                                                                     spa.getInitialProcedure(),
                                                                     spa.getProcedures(),
                                                                     transformationUtil);

        final Pair<Map<I, Word<I>>, Map<I, Word<I>>> accessAndReturnSequences = Util.computeAccessAndReturnSequences(
                alphabet,
                spa.getInitialProcedure(),
                proceduralAlphabet,
                spa.getProcedures(),
                terminatingSequences,
                transformationUtil);

        this.accessSequences = accessAndReturnSequences.getFirst();
        this.returnSequences = accessAndReturnSequences.getSecond();
    }

    @Override
    public DefaultQuery<I, Boolean> findCounterExample(SPA<?, I> hypothesis, Collection<? extends I> inputs) {

        if (!(inputs instanceof SPAAlphabet)) {
            throw new IllegalArgumentException("Inputs are not an SPA alphabet");
        }

        final SPAAlphabet<I> alphabet = (SPAAlphabet<I>) inputs;
        final GrowingAlphabet<I> proceduralAlphabet = new GrowingMapAlphabet<>();

        proceduralAlphabet.addAll(alphabet.getCallAlphabet());
        proceduralAlphabet.addAll(alphabet.getInternalAlphabet());

        for (final I procedure : alphabet.getCallAlphabet()) {

            final DFA<?, I> proc_sul = this.spa.getProcedures().get(procedure);
            final DFA<?, I> proc_hyp;

            final DFA<?, I> hypProc = hypothesis.getProcedures().get(procedure);

            if (hypProc == null) {
                proc_hyp = rejectAllAutomaton;
            } else {
                // hyp does not contain all procedures so copy hyp to an automaton with full alphabet definitions
                if (!hypothesis.getProcedures().keySet().containsAll(alphabet.getCallAlphabet())) {

                    final CompactDFA<I> copy = new CompactDFA<>(proceduralAlphabet, hypProc.size() + 1);

                    final GrowingAlphabet<I> proceduralHypAlphabet = new GrowingMapAlphabet<>();
                    proceduralHypAlphabet.addAll(hypothesis.getProcedures().keySet());
                    proceduralHypAlphabet.addAll(alphabet.getInternalAlphabet());

                    AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                                               hypProc,
                                               proceduralHypAlphabet,
                                               copy);
                    MutableDFAs.complete(copy, proceduralAlphabet, false);
                    proc_hyp = copy;
                } else {
                    proc_hyp = hypProc;
                }
            }

            final Word<I> sepWord = Automata.findSeparatingWord(proc_sul, proc_hyp, proceduralAlphabet);

            if (sepWord != null) {
                final Map<I, Word<I>> as, ts, rs;
                final boolean ce = proc_sul.accepts(sepWord);

                if (ce) { // positive ce
                    as = this.accessSequences;
                    ts = this.terminatingSequences;
                    rs = this.returnSequences;
                } else { // negative ce needs to compute as/ts/rs based on hypothesis so that the error is only in the detected procedure and the computed CE is in fact accepted by the hypothesis
                    final ATRTuple<I> tuple = computeATRTuple(hypothesis);
                    as = tuple.accessSequences;
                    ts = tuple.terminatingSequences;
                    rs = tuple.returnSequences;
                }

                final WordBuilder<I> builder = new WordBuilder<>();
                builder.append(as.get(procedure));
                builder.append(procedure);
                builder.append(transformationUtil.expand(sepWord, ts::get));
                builder.append(alphabet.getReturnSymbol());
                builder.append(rs.get(procedure));

                assert ce || hypothesis.accepts(builder) : "Negative CE is not accepted by hypothesis";

                return new DefaultQuery<>(builder.toWord(), proc_sul.accepts(sepWord));
            }
        }

        return null;
    }

    private ATRTuple<I> computeATRTuple(final SPA<?, I> spa) {

        final SPAAlphabet<I> alphabet = spa.getInputAlphabet();
        final Alphabet<I> proceduralAlphabet = new GrowingMapAlphabet<>();

        proceduralAlphabet.addAll(spa.getProcedures().keySet());
        proceduralAlphabet.addAll(alphabet.getInternalAlphabet());

        Map<I, Word<I>> terminatingSequences = Util.computeTerminatingSequences(alphabet,
                                                                                spa.getInitialProcedure(),
                                                                                spa.getProcedures(),
                                                                                this.transformationUtil);

        final Pair<Map<I, Word<I>>, Map<I, Word<I>>> accessAndReturnSequences = Util.computeAccessAndReturnSequences(
                alphabet,
                spa.getInitialProcedure(),
                proceduralAlphabet,
                spa.getProcedures(),
                terminatingSequences,
                this.transformationUtil);

        final Map<I, Word<I>> accessSequences = accessAndReturnSequences.getFirst();
        final Map<I, Word<I>> returnSequences = accessAndReturnSequences.getSecond();

        return new ATRTuple<>(accessSequences, terminatingSequences, returnSequences);
    }

    private static class ATRTuple<I> {

        private final Map<I, Word<I>> accessSequences;
        private final Map<I, Word<I>> terminatingSequences;
        private final Map<I, Word<I>> returnSequences;

        public ATRTuple(Map<I, Word<I>> accessSequences,
                        Map<I, Word<I>> terminatingSequences,
                        Map<I, Word<I>> returnSequences) {
            this.accessSequences = accessSequences;
            this.terminatingSequences = terminatingSequences;
            this.returnSequences = returnSequences;
        }
    }

}

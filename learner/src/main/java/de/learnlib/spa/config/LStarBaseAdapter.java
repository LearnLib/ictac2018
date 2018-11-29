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
package de.learnlib.spa.config;

import java.util.Objects;

import de.learnlib.algorithms.lstar.dfa.ClassicLStarDFA;
import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.datastructure.observationtable.ObservationTable;
import de.learnlib.spa.LocalRefinementCounter;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

/**
 * Adapter for using {@link ClassicLStarDFA} as a sub-procedural learner.
 *
 * @param <I>
 *         input symbol type
 *
 * @author frohme
 */
public class LStarBaseAdapter<I> extends ClassicLStarDFA<I>
        implements AccessSequenceTransformer<I>, LearningAlgorithm.DFALearner<I>, LocalRefinementCounter {

    private long localRefinements;
    private long sumOfLocalCELengths;

    public LStarBaseAdapter(Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
        super(alphabet, oracle);
    }

    @Override
    protected void refineHypothesisInternal(DefaultQuery<I, Boolean> ceQuery) {
        localRefinements++;
        sumOfLocalCELengths += ceQuery.getInput().length();
        super.refineHypothesisInternal(ceQuery);
    }

    @Override
    public long getNumberOfLocalRefinements() {
        return localRefinements;
    }

    @Override
    public Word<I> transformAccessSequence(Word<I> word) {
        final DFA<?, I> hypothesis = super.getHypothesisModel();
        final ObservationTable<I, Boolean> observationTable = super.getObservationTable();

        final Object reachedState = hypothesis.getState(word);

        for (final Word<I> shortPrefix : observationTable.getShortPrefixes()) {
            final Object reachedSPState = hypothesis.getState(shortPrefix);

            if (Objects.equals(reachedState, reachedSPState)) {
                return shortPrefix;
            }
        }

        throw new IllegalStateException("This should not have happened");
    }

    @Override
    public long getSumOfLocalCELengths() {
        return sumOfLocalCELengths;
    }

    @Override
    public boolean isAccessSequence(Word<I> word) {
        return this.transformAccessSequence(word).equals(word);
    }

}

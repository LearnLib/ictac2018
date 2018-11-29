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

import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.ttt.dfa.TTTLearnerDFA;
import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.spa.LocalRefinementCounter;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

/**
 * Adapter for using {@link TTTLearnerDFA} as a sub-procedural learner.
 *
 * @param <I>
 *         input symbol type
 *
 * @author frohme
 */
public class TTTAdapter<I> extends TTTLearnerDFA<I> implements AccessSequenceTransformer<I>, LocalRefinementCounter {

    private long localRefinements;
    private long sumOfLocalCELengths;

    public TTTAdapter(Alphabet<I> alphabet, MembershipOracle<I, Boolean> oracle) {
        super(alphabet, oracle, AcexAnalyzers.BINARY_SEARCH_BWD);
    }

    @Override
    protected boolean refineHypothesisSingle(DefaultQuery<I, Boolean> ceQuery) {

        final boolean result = super.refineHypothesisSingle(ceQuery);

        if (result) {
            localRefinements++;
            sumOfLocalCELengths += ceQuery.getInput().length();
        }

        return result;
    }

    @Override
    public long getNumberOfLocalRefinements() {
        return localRefinements;
    }

    @Override
    public Word<I> transformAccessSequence(Word<I> word) {
        return super.getHypothesisDS().getState(word).getAccessSequence();
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

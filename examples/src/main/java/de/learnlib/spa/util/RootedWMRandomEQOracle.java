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
import java.util.Collections;
import java.util.Random;
import java.util.stream.Stream;

import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.oracle.equivalence.vpda.RandomWellMatchedWordsEQOracle;
import de.learnlib.spa.api.SPAAlphabet;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

/**
 * @author frohme
 */
public class RootedWMRandomEQOracle<I> extends RandomWellMatchedWordsEQOracle<I> {

    private final I startProcedure;
    private final I returnSymbol;

    private final Collection<Word<I>> additionalQueries;

    public RootedWMRandomEQOracle(MembershipOracle<I, Boolean> mqOracle,
                                  SPAAlphabet<I> alphabet,
                                  I startProcedure,
                                  int ceLength,
                                  double callProb,
                                  int maxTests,
                                  Random random) {
        this(mqOracle, alphabet, startProcedure, ceLength, callProb, maxTests, random, Collections.emptyList());
    }

    public RootedWMRandomEQOracle(MembershipOracle<I, Boolean> mqOracle,
                                  SPAAlphabet<I> alphabet,
                                  I startProcedure,
                                  int ceLength,
                                  double callProb,
                                  int maxTests,
                                  Random random,
                                  Collection<Word<I>> additionalQueries) {
        super(random, mqOracle, alphabet, callProb, maxTests, 0, ceLength);

        this.startProcedure = startProcedure;
        this.returnSymbol = alphabet.getReturnSymbol(0);
        this.additionalQueries = additionalQueries;
    }

    @Override
    protected Stream<Word<I>> generateTestWords(DeterministicAcceptorTS<?, I> hypothesis,
                                                Collection<? extends I> inputs) {

        final WordBuilder<I> wb = new WordBuilder<>();
        final Stream<Word<I>> rootedStream = super.generateTestWords(hypothesis, inputs).map(w -> {
            wb.clear();
            wb.add(startProcedure);
            wb.append(w);
            wb.add(returnSymbol);
            return wb.toWord();
        });

        return Stream.concat(rootedStream, additionalQueries.stream());
    }
}

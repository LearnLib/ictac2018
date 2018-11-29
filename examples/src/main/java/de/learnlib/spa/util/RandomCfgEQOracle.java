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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.oracle.equivalence.vpda.RandomWellMatchedWordsEQOracle;
import de.learnlib.spa.api.SPAAlphabet;
import enumeration.biginteger.BIEnumerator;
import grammar.cfg.MyGrammar;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

/**
 * @author frohme
 */
public class RandomCfgEQOracle extends RandomWellMatchedWordsEQOracle<String> {

    private static final int CE_LENGTH = 100;
    private static final double CALL_PROB = 0.5;
    private static final int MAX_TESTS = 10000;

    private final String startProcedure;
    private final String returnSymbol;

    private final BIEnumerator cfgEnumerator;
    private final Collection<Word<String>> additionalQueries;

    public RandomCfgEQOracle(MembershipOracle<String, Boolean> mqOracle,
                             SPAAlphabet<String> alphabet,
                             Random random,
                             MyGrammar grammar,
                             String startProcedure,
                             String returnSymbol,
                             Collection<Word<String>> additionalQueries) {
        super(random, mqOracle, alphabet, CALL_PROB, MAX_TESTS, 0, CE_LENGTH);

        this.cfgEnumerator = new BIEnumerator(grammar);

        this.startProcedure = startProcedure;
        this.returnSymbol = returnSymbol;
        this.additionalQueries = additionalQueries;
    }

    @Override
    protected Stream<Word<String>> generateTestWords(DeterministicAcceptorTS<?, String> hypothesis,
                                                     Collection<? extends String> inputs) {

        final WordBuilder<String> wb = new WordBuilder<>();
        final Stream<Word<String>> rootedStream = super.generateTestWords(hypothesis, inputs).map(w -> {
            wb.clear();
            wb.add(startProcedure);
            wb.append(w);
            wb.add(returnSymbol);
            return wb.toWord();
        });

        return Streams.concat(rootedStream, generateCFGWords(), additionalQueries.stream());
    }

    private Stream<Word<String>> generateCFGWords() {
        return IntStream.range(0, MAX_TESTS).mapToObj(BigInteger::valueOf).map(cfgEnumerator::N2L).map(s -> {
            if (s.isEmpty()) {
                return Word.epsilon();
            } else {
                return Word.fromSymbols(s.split(" +"));
            }
        });
    }
}

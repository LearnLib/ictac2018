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
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.spa.benchmark.AbstractBenchmark;
import de.learnlib.spa.cfg.Instrumenter;
import de.learnlib.spa.cfg.Instrumenter.InstrumentationResult;
import javacc.ParseException;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.words.Word;

/**
 * @author frohme
 */
public abstract class AbstractCFGBenchmark extends AbstractBenchmark<String> {

    private static final int NUMBER_OF_RUNS = 10;
    private static final Random RANDOM = new Random(42);

    private final InstrumentationResult instrumentationResult;

    public AbstractCFGBenchmark() {
        try {
            instrumentationResult = Instrumenter.generate(getCFG());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getCFG();

    protected void runBenchmarkSuite() {
        runBenchmarkSuite(instrumentationResult.getAlphabet(), NUMBER_OF_RUNS);
    }

    @Override
    protected Supplier<MembershipOracle<String, Boolean>> getMembershipOracleSupplier() {
        return () -> instrumentationResult.getAnswerer().asOracle();
    }

    @Override
    protected Function<MembershipOracle<String, Boolean>, EquivalenceOracle<? super DeterministicAcceptorTS<?, String>, String, Boolean>> getEquivalenceOracleSupplier() {

        final Collection<Word<String>> negativeQueries =
                getStaticTraces().stream().map(s -> s.split("")).map(Word::fromSymbols).collect(Collectors.toList());

        return mqo -> new RandomCfgEQOracle(mqo,
                                            RANDOM,
                                            instrumentationResult.getGrammar(),
                                            instrumentationResult.getInitialCallSymbol(),
                                            Instrumenter.RETURN_SYMBOL,
                                            negativeQueries);
    }

    protected abstract Collection<String> getStaticTraces();
}
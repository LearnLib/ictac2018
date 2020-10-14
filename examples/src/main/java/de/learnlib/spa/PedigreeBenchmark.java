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
package de.learnlib.spa;

import java.io.InputStream;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.oracle.membership.MappedOracle;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.benchmark.AbstractBenchmark;
import de.learnlib.spa.impl.DefaultSPAAlphabet;
import de.learnlib.spa.pedigree.InputMapper;
import de.learnlib.spa.pedigree.InputSymbol;
import de.learnlib.spa.pedigree.Samples;
import de.learnlib.spa.util.RootedWMRandomEQOracle;
import de.learnlib.spa.util.XmlDtdValidationMQOracle;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

/**
 * @author frohme
 */
public class PedigreeBenchmark extends AbstractBenchmark<InputSymbol> {

    private static final int CE_LENGTH = 100;
    private static final double CALL_PROB = 0.5;
    private static final int MAX_TESTS = 10000;
    private static final Random RANDOM = new Random(42);

    private static final int NUMBER_OF_RUNS = 10;

    private static final SPAAlphabet<InputSymbol> ALPHABET;

    static {
        final Alphabet<InputSymbol> internalAlphabet = Alphabets.fromArray(InputSymbol.NAME, InputSymbol.DIED);
        final Alphabet<InputSymbol> callAlphabet =
                Alphabets.fromArray(InputSymbol.PERSON, InputSymbol.MOTHER, InputSymbol.FATHER);

        ALPHABET = new DefaultSPAAlphabet<>(internalAlphabet, callAlphabet, InputSymbol.RETURN);
    }

    public static void main(String[] args) {
        new PedigreeBenchmark().runBenchmarkSuite(ALPHABET, NUMBER_OF_RUNS);
    }

    @Override
    protected Supplier<MembershipOracle<InputSymbol, Boolean>> getMembershipOracleSupplier() {
        final InputStream dtdStream = PedigreeBenchmark.class.getResourceAsStream("/pedigree.dtd");

        return () -> new MappedOracle<>(new XmlDtdValidationMQOracle(dtdStream, InputSymbol.PERSON.toXml()),
                                        new InputMapper(ALPHABET));
    }

    @Override
    protected Function<MembershipOracle<InputSymbol, Boolean>, EquivalenceOracle<? super DeterministicAcceptorTS<?, InputSymbol>, InputSymbol, Boolean>> getEquivalenceOracleSupplier() {

        final Word<InputSymbol> initialTrace = Samples.ALL_PROCEDURES;
        // Alternatively, use a query with redundancy to see the impact of the learning performance
        // final Word<InputSymbol> initialTrace = Samples.getAllProceduresWithRedundancy(200);

        return mqo -> new RootedWMRandomEQOracle<>(mqo,
                                                   ALPHABET,
                                                   InputSymbol.PERSON,
                                                   CE_LENGTH,
                                                   CALL_PROB,
                                                   MAX_TESTS,
                                                   RANDOM,
                                                   Samples.CHARACTERISTIC_SAMPLES);
    }
}
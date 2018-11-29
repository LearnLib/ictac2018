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

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.oracle.equivalence.EQOracleChain;
import de.learnlib.oracle.equivalence.RandomWordsEQOracle;
import de.learnlib.oracle.equivalence.SimulatorEQOracle;
import de.learnlib.oracle.membership.SimulatorOracle.DFASimulatorOracle;
import de.learnlib.spa.benchmark.AbstractBenchmark;
import de.learnlib.spa.benchmark.LearningRun;
import de.learnlib.spa.benchmark.LearningStatistics;
import de.learnlib.spa.config.DiscriminationTreeAdapter;
import de.learnlib.spa.config.KearnsVaziraniAdapter;
import de.learnlib.spa.config.LStarBaseAdapter;
import de.learnlib.spa.config.RivestSchapireAdapter;
import de.learnlib.spa.config.TTTAdapter;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.brics.BricsDFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExponentialRegularBenchmark {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExponentialRegularBenchmark.class);

    private static final Random RANDOM = new Random(42);
    private static final Alphabet<Character> ALPHABET = Alphabets.characters('a', 'c');

    private static final CompactDFA<Character> TARGET;

    static {
        final RegExp re = new RegExp("(abc){64}");
        final Automaton bricsAutomaton = re.toAutomaton();
        final DFA<State, Character> bricsDFA = new BricsDFA(bricsAutomaton, true);
        TARGET = new CompactDFA<>(ALPHABET, 65 * 3);

        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.DFS, bricsDFA, ALPHABET, TARGET);

        final Integer sink = TARGET.addState(false);
        for (final Character i : ALPHABET) {
            for (final Integer s : TARGET.getStates()) {
                if (TARGET.getTransition(s, i) == null) {
                    TARGET.setTransition(s, i, sink);
                }
            }
        }
    }

    public static void main(String[] args) {
        runExperiment("REG [LStar]", (mqOracle) -> new LStarBaseAdapter<>(ALPHABET, mqOracle));
        runExperiment("REG [RS]", (mqOracle) -> new RivestSchapireAdapter<>(ALPHABET, mqOracle));
        runExperiment("REG [KV]", (mqOracle) -> new KearnsVaziraniAdapter<>(ALPHABET, mqOracle));
        runExperiment("REG [DT]", (mqOracle) -> new DiscriminationTreeAdapter<>(ALPHABET, mqOracle));
        runExperiment("REG [TTT]", (mqOracle) -> new TTTAdapter<>(ALPHABET, mqOracle));
    }

    static <M extends DFA<?, Character>> void runExperiment(final String name,
                                                            final Function<MembershipOracle<Character, Boolean>, LearningAlgorithm<? extends M, Character, Boolean>> learner) {

        final MembershipOracle<Character, Boolean> mqOracle = new DFASimulatorOracle<>(TARGET);
        final EquivalenceOracle<M, Character, Boolean> eqOracle =
                new EQOracleChain<>(new RandomWordsEQOracle<>(mqOracle, 0, 100, 10000, RANDOM),
                                    new SimulatorEQOracle<>(TARGET));

        final List<LearningStatistics<Character>> results = IntStream.range(0, 10)
                                                                     .peek(i -> LOGGER.info("Run {}", i + 1))
                                                                     .mapToObj(i -> LearningRun.run(ALPHABET,
                                                                                                    () -> mqOracle,
                                                                                                    mq -> eqOracle,
                                                                                                    learner))
                                                                     .collect(Collectors.toList());

        LOGGER.info(name);
        AbstractBenchmark.printAverages(results);
    }

}

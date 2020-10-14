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
package de.learnlib.spa.benchmark;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.discriminationtree.vpda.DTLearnerVPDA;
import de.learnlib.algorithms.ttt.vpda.TTTLearnerVPDA;
import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.spa.LocalRefinementCounter;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.config.DiscriminationTreeAdapter;
import de.learnlib.spa.config.KearnsVaziraniAdapter;
import de.learnlib.spa.config.LStarBaseAdapter;
import de.learnlib.spa.config.RivestSchapireAdapter;
import de.learnlib.spa.config.TTTAdapter;
import de.learnlib.spa.learner.SPALearner;
import net.automatalib.SupportsGrowingAlphabet;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.words.Alphabet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author frohme
 */
public abstract class AbstractBenchmark<I> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBenchmark.class);

    public static <I> void printAverages(Collection<LearningStatistics<I>> source) {
        LOGGER.info("Resets: {}", computeAverage(source, LearningStatistics::getNumberOfCEs));
        LOGGER.info("Queries: {}", computeAverage(source, LearningStatistics::getNumberOfMQs));
        LOGGER.info("Symbols: {}", computeAverage(source, LearningStatistics::getNumberOfSymbols));
        LOGGER.info("Size: {}", computeAverage(source, LearningStatistics::getSize));
        LOGGER.info("Learning Time: {} ms", computeAverage(source, ls -> ls.getStopWatch().getTime()));
        LOGGER.info("================");
    }

    private static <I> double computeAverage(Collection<LearningStatistics<I>> source,
                                             ToLongFunction<LearningStatistics> extractor) {
        return source.stream().mapToLong(extractor).average().getAsDouble();
    }

    protected void runBenchmarkSuite(SPAAlphabet<I> spaAlphabet, int numOfRuns) {

        final List<LearningStatistics<I>> proceduralResultLStar = testProceduralLStar(spaAlphabet, numOfRuns);
        final List<LearningStatistics<I>> proceduralResultRS = testProceduralRS(spaAlphabet, numOfRuns);
        final List<LearningStatistics<I>> proceduralResultKV = testProceduralKV(spaAlphabet, numOfRuns);
        final List<LearningStatistics<I>> proceduralResultDT = testProceduralDT(spaAlphabet, numOfRuns);
        final List<LearningStatistics<I>> proceduralResultTTT = testProceduralTTT(spaAlphabet, numOfRuns);
        final List<LearningStatistics<I>> dtResult = testOP(spaAlphabet, numOfRuns);
        final List<LearningStatistics<I>> tttResult = testTTT(spaAlphabet, numOfRuns);

        LOGGER.info("SPA [LStar]");
        printAverages(proceduralResultLStar);

        LOGGER.info("SPA [RS]");
        printAverages(proceduralResultRS);

        LOGGER.info("SPA [KV]");
        printAverages(proceduralResultKV);

        LOGGER.info("SPA [DT]");
        printAverages(proceduralResultDT);

        LOGGER.info("SPA [TTT]");
        printAverages(proceduralResultTTT);

        LOGGER.info("VPA [DT]");
        printAverages(dtResult);

        LOGGER.info("VPA [TTT]");
        printAverages(tttResult);
    }

    private List<LearningStatistics<I>> testProceduralLStar(SPAAlphabet<I> alphabet, int numOfRuns) {
        return buildAndRunTest(alphabet, buildProceduralLearnerProvider(alphabet, LStarBaseAdapter::new), numOfRuns);
    }

    private List<LearningStatistics<I>> testProceduralRS(SPAAlphabet<I> alphabet, int numOfRuns) {
        return buildAndRunTest(alphabet,
                               buildProceduralLearnerProvider(alphabet, RivestSchapireAdapter::new),
                               numOfRuns);
    }

    private List<LearningStatistics<I>> testProceduralKV(SPAAlphabet<I> alphabet, int numOfRuns) {
        return buildAndRunTest(alphabet,
                               buildProceduralLearnerProvider(alphabet, KearnsVaziraniAdapter::new),
                               numOfRuns);
    }

    private List<LearningStatistics<I>> testProceduralDT(SPAAlphabet<I> alphabet, int numOfRuns) {
        return buildAndRunTest(alphabet,
                               buildProceduralLearnerProvider(alphabet, DiscriminationTreeAdapter::new),
                               numOfRuns);
    }

    private List<LearningStatistics<I>> testProceduralTTT(SPAAlphabet<I> alphabet, int numOfRuns) {
        return buildAndRunTest(alphabet, buildProceduralLearnerProvider(alphabet, TTTAdapter::new), numOfRuns);
    }

    private <L extends LearningAlgorithm.DFALearner<I> & SupportsGrowingAlphabet<I> & AccessSequenceTransformer<I> & LocalRefinementCounter> Function<MembershipOracle<I, Boolean>, LearningAlgorithm<? extends DeterministicAcceptorTS<?, I>, I, Boolean>> buildProceduralLearnerProvider(
            SPAAlphabet<I> alphabet,
            BiFunction<Alphabet<I>, MembershipOracle<I, Boolean>, L> subLearner) {
        return mqo -> new SPALearner<>(alphabet, mqo, subLearner);
    }

    private List<LearningStatistics<I>> testTTT(SPAAlphabet<I> alphabet, int numOfRuns) {
        return buildAndRunTest(alphabet,
                               mqo -> new TTTLearnerVPDA<>(alphabet, mqo, AcexAnalyzers.BINARY_SEARCH_BWD),
                               numOfRuns);
    }

    private List<LearningStatistics<I>> testOP(SPAAlphabet<I> alphabet, int numOfRuns) {
        return buildAndRunTest(alphabet,
                               mqo -> new DTLearnerVPDA<>(alphabet, mqo, AcexAnalyzers.BINARY_SEARCH_BWD),
                               numOfRuns);
    }

    private List<LearningStatistics<I>> buildAndRunTest(SPAAlphabet<I> alphabet,
                                                        Function<MembershipOracle<I, Boolean>, LearningAlgorithm<? extends DeterministicAcceptorTS<?, I>, I, Boolean>> learnerFunction,
                                                        int numOfRuns) {

        return IntStream.range(0, numOfRuns)
                        .peek(i -> LOGGER.info("Run {}", i + 1))
                        .mapToObj(i -> LearningRun.run(alphabet,
                                                       getMembershipOracleSupplier(),
                                                       getEquivalenceOracleSupplier(),
                                                       learnerFunction))
                        .collect(Collectors.toList());
    }

    protected abstract Supplier<MembershipOracle<I, Boolean>> getMembershipOracleSupplier();

    protected abstract Function<MembershipOracle<I, Boolean>, EquivalenceOracle<? super DeterministicAcceptorTS<?, I>, I, Boolean>> getEquivalenceOracleSupplier();
}

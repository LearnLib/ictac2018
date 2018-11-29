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

import java.util.function.Function;
import java.util.function.Supplier;

import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.statistic.oracle.JointCounterOracle;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import net.automatalib.words.Alphabet;
import org.apache.commons.lang3.time.StopWatch;

/**
 * @author frohme
 */
public final class LearningRun {

    private LearningRun() {}

    public static <M extends DeterministicAcceptorTS<?, I>, I, D> LearningStatistics<I> run(final Alphabet<I> inputAlphabet,
                                                                                            final Supplier<MembershipOracle<I, D>> membershipOracleSupplier,
                                                                                            final Function<MembershipOracle<I, D>, EquivalenceOracle<M, I, D>> equivalenceOracleFunction,
                                                                                            final Function<MembershipOracle<I, D>, LearningAlgorithm<? extends M, I, D>> learningAlgorithmFunction) {

        final MembershipOracle<I, D> mqOracle = membershipOracleSupplier.get();
        final JointCounterOracle<I, D> statMq = new JointCounterOracle<>(mqOracle);

        final EquivalenceOracle<M, I, D> eqOracle = equivalenceOracleFunction.apply(mqOracle);

        final LearningAlgorithm<? extends M, I, D> learner = learningAlgorithmFunction.apply(statMq);

        learner.startLearning();

        DefaultQuery<I, D> ce;

        int numberOfCEs = 0;

        final StopWatch sw = StopWatch.createStarted();
        sw.suspend();

        while ((ce = eqOracle.findCounterExample(learner.getHypothesisModel(), inputAlphabet)) != null) {
            numberOfCEs++;
            sw.resume();
            while (learner.refineHypothesis(ce)) {}
            sw.suspend();
        }

        sw.stop();

        return new LearningStatistics<>(numberOfCEs,
                                        statMq.getQueryCount(),
                                        statMq.getSymbolCount(),
                                        sw,
                                        learner.getHypothesisModel());
    }

}

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

import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.algorithm.feature.SupportsGrowingAlphabet;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.oracle.membership.SimulatorOracle;
import de.learnlib.spa.api.ATRProvider;
import de.learnlib.spa.api.SPA;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.config.DiscriminationTreeAdapter;
import de.learnlib.spa.config.KearnsVaziraniAdapter;
import de.learnlib.spa.config.LStarBaseAdapter;
import de.learnlib.spa.config.RivestSchapireAdapter;
import de.learnlib.spa.config.TTTAdapter;
import de.learnlib.spa.impl.DefaultATRProvider;
import de.learnlib.spa.impl.DefaultSPAAlphabet;
import de.learnlib.spa.impl.OptimizingATRProvider;
import de.learnlib.spa.learner.SPALearner;
import de.learnlib.spa.util.Generator;
import de.learnlib.spa.util.SPAEQ;
import de.learnlib.spa.util.Util;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SPATest {

    private Random random;
    private SPAAlphabet<Integer> alphabet;
    private SPA<?, Integer> spa;

    @BeforeClass
    public void setUp() {
        random = new Random(42);
        alphabet = new DefaultSPAAlphabet<>(Alphabets.integers(10, 25), Alphabets.integers(0, 9), 26);
        spa = Generator.create(random, alphabet, 20);
    }

    @DataProvider(name = "atr")
    public <I> Object[][] dataProvider() {
        return new Object[][] {new Object[] {new DefaultSetup<I>()}, new Object[] {new OptimizingSetup<I>()}};
    }

    @Test(dataProvider = "atr")
    public void testDT(Function<SPAAlphabet<Integer>, ATRProvider<Integer>> atrProvider) {
        final SPA<?, Integer> hyp = learningLoop(spa, DiscriminationTreeAdapter::new, atrProvider);
        Assert.assertTrue(Util.testEquivalence(spa, hyp));
    }

    @Test(dataProvider = "atr")
    public void testKV(Function<SPAAlphabet<Integer>, ATRProvider<Integer>> atrProvider) {
        final SPA<?, Integer> hyp = learningLoop(spa, KearnsVaziraniAdapter::new, atrProvider);
        Assert.assertTrue(Util.testEquivalence(spa, hyp));
    }

    @Test(dataProvider = "atr")
    public void testLStar(Function<SPAAlphabet<Integer>, ATRProvider<Integer>> atrProvider) {
        final SPA<?, Integer> hyp = learningLoop(spa, LStarBaseAdapter::new, atrProvider);
        Assert.assertTrue(Util.testEquivalence(spa, hyp));
    }

    @Test(dataProvider = "atr")
    public void testRV(Function<SPAAlphabet<Integer>, ATRProvider<Integer>> atrProvider) {
        final SPA<?, Integer> hyp = learningLoop(spa, RivestSchapireAdapter::new, atrProvider);
        Assert.assertTrue(Util.testEquivalence(spa, hyp));
    }

    @Test(dataProvider = "atr")
    public void testTTT(Function<SPAAlphabet<Integer>, ATRProvider<Integer>> atrProvider) {
        final SPA<?, Integer> hyp = learningLoop(spa, TTTAdapter::new, atrProvider);
        Assert.assertTrue(Util.testEquivalence(spa, hyp));
    }

    private <I, L extends LearningAlgorithm.DFALearner<I> & SupportsGrowingAlphabet<I> & AccessSequenceTransformer<I> & LocalRefinementCounter> SPA<?, I> learningLoop(
            final SPA<?, I> system,
            final BiFunction<Alphabet<I>, MembershipOracle<I, Boolean>, L> learnerProvider,
            final Function<SPAAlphabet<I>, ATRProvider<I>> atrProvider) {

        final SPAAlphabet<I> alphabet = system.getInputAlphabet();
        final MembershipOracle<I, Boolean> mqOracle = new SimulatorOracle<>(system);
        final EquivalenceOracle<SPA<?, I>, I, Boolean> eqOracle = new SPAEQ<>(system);

        final SPALearner<I, L> learner =
                new SPALearner<>(alphabet, mqOracle, learnerProvider, atrProvider.apply(alphabet));
        learner.startLearning();

        SPA<?, I> hyp = learner.getHypothesisModel();
        DefaultQuery<I, Boolean> ce;

        while ((ce = eqOracle.findCounterExample(hyp, alphabet)) != null) {
            boolean refined = false;
            while (learner.refineHypothesis(ce)) {
                refined = true;
            }
            Assert.assertTrue(refined);
            hyp = learner.getHypothesisModel();
        }

        return hyp;
    }

    private static class DefaultSetup<I> implements Function<SPAAlphabet<I>, ATRProvider<I>> {

        @Override
        public ATRProvider<I> apply(SPAAlphabet<I> alphabet) {
            return new DefaultATRProvider<>(alphabet);
        }

        @Override
        public String toString() {
            return "DefaultATRProvider";
        }
    }

    private static class OptimizingSetup<I> implements Function<SPAAlphabet<I>, ATRProvider<I>> {

        @Override
        public ATRProvider<I> apply(SPAAlphabet<I> alphabet) {
            return new OptimizingATRProvider<>(alphabet);
        }

        @Override
        public String toString() {
            return "OptimizingATRProvider";
        }
    }

    ;
}

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
package de.learnlib.spa.learner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.algorithm.feature.SupportsGrowingAlphabet;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.statistic.oracle.JointCounterOracle;
import de.learnlib.spa.LocalRefinementCounter;
import de.learnlib.spa.TransformationUtil;
import de.learnlib.spa.api.ATRProvider;
import de.learnlib.spa.api.SPA;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.impl.DefaultSPA;
import de.learnlib.spa.impl.EmptySPA;
import de.learnlib.spa.impl.OptimizingATRProvider;
import de.learnlib.spa.impl.ProceduralMembershipOracle;
import de.learnlib.util.MQUtil;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

/**
 * The learning algorithm for {@link DefaultSPA}s.
 *
 * @param <I>
 *         input symbol type
 * @param <L>
 *         sub-learner type
 *
 * @author frohme
 */
public class SPALearner<I, L extends LearningAlgorithm.DFALearner<I> & SupportsGrowingAlphabet<I> & AccessSequenceTransformer<I> & LocalRefinementCounter>
        implements LearningAlgorithm<SPA<?, I>, I, Boolean> {

    private final SPAAlphabet<I> alphabet;
    private final MembershipOracle<I, Boolean> oracle;
    private final BiFunction<Alphabet<I>, MembershipOracle<I, Boolean>, L> learnerProvider;
    private final ATRProvider<I> atrProvider;

    private final Map<I, L> subLearners;
    private final TransformationUtil<I> transformationUtil;
    private final JointCounterOracle<I, Boolean> ceOracle;
    private final Set<I> activeAlphabet;
    private I initialCallSymbol;
    private long numberOfRefinements;

    public SPALearner(final SPAAlphabet<I> alphabet,
                      final MembershipOracle<I, Boolean> oracle,
                      final BiFunction<Alphabet<I>, MembershipOracle<I, Boolean>, L> learnerProvider) {
        this(alphabet, oracle, learnerProvider, new OptimizingATRProvider<>(alphabet));
    }

    public SPALearner(final SPAAlphabet<I> alphabet,
                      final MembershipOracle<I, Boolean> oracle,
                      final BiFunction<Alphabet<I>, MembershipOracle<I, Boolean>, L> learnerProvider,
                      final ATRProvider<I> atrProvider) {
        this.alphabet = alphabet;
        this.oracle = oracle;
        this.learnerProvider = learnerProvider;
        this.atrProvider = atrProvider;

        this.subLearners = Maps.newHashMapWithExpectedSize(this.alphabet.getNumCalls());
        this.transformationUtil = new TransformationUtil<>(alphabet);
        this.ceOracle = new JointCounterOracle<>(oracle);

        this.activeAlphabet = Sets.newHashSetWithExpectedSize(alphabet.getNumCalls() + alphabet.getNumInternals());
        this.activeAlphabet.addAll(alphabet.getInternalAlphabet());
    }

    @Override
    public void startLearning() {
        // do nothing, as we have to wait for evidence that the potential main procedure actually terminates
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<I, Boolean> defaultQuery) {

        boolean changed = this.extractUsefulInformationFromCounterExample(defaultQuery);

        if (changed) {
            numberOfRefinements++;
        }

        while (refineHypothesisInternal(defaultQuery)) {
            numberOfRefinements++;
            changed = true;
        }

        return changed;
    }

    private boolean refineHypothesisInternal(DefaultQuery<I, Boolean> defaultQuery) {

        final SPA<?, I> hypothesis = this.getHypothesisModel();

        if (!MQUtil.isCounterexample(defaultQuery, hypothesis)) {
            return false;
        }

        final Word<I> input = defaultQuery.getInput();

        // look for better sequences and ensure TS conformance prior to CE analysis
        boolean localRefinement = updateATRAndCheckTSConformance(hypothesis);

        final int returnIdx;

        if (defaultQuery.getOutput()) {
            returnIdx = detectRejectingProcedure(getHypothesisModel()::accepts, input);
        } else {
            returnIdx = detectRejectingProcedure(this.ceOracle::answerQuery, input);
        }

        // extract local ce
        final int callIdx = transformationUtil.findCallIndex(input, returnIdx);
        final I procedure = input.getSymbol(callIdx);

        final Word<I> localTrace = transformationUtil.normalize(input.subWord(callIdx + 1, returnIdx), 0);
        final DefaultQuery<I, Boolean> localCE = new DefaultQuery<>(localTrace, defaultQuery.getOutput());

        localRefinement |= this.subLearners.get(procedure).refineHypothesis(localCE);

        if (!localRefinement) {
            throw new AssertionError();
        }

        return true;
    }

    @Override
    public SPA<?, I> getHypothesisModel() {

        if (this.subLearners.isEmpty()) {
            return new EmptySPA<>(this.alphabet);
        }

        final Map<I, ? extends DFA<?, I>> subModels = getSubModels();
        return new DefaultSPA<>(alphabet, initialCallSymbol, (Map<I, DFA<Object, I>>) subModels);
    }

    private boolean extractUsefulInformationFromCounterExample(DefaultQuery<I, Boolean> defaultQuery) {

        if (!defaultQuery.getOutput()) {
            return false;
        }

        final Word<I> input = defaultQuery.getInput();

        // positive CEs should always be rooted at the main procedure
        this.initialCallSymbol = input.firstSymbol();

        final Set<I> newProcedures = atrProvider.scanPositiveCounterexample(input);

        for (I sym : newProcedures) {
            final L newLearner = learnerProvider.apply(this.alphabet.getInternalAlphabet(),
                                                       new ProceduralMembershipOracle<>(alphabet,
                                                                                        oracle,
                                                                                        sym,
                                                                                        atrProvider));
            this.subLearners.put(sym, newLearner);

            newLearner.startLearning();
            for (final I call : this.subLearners.keySet()) {
                newLearner.addAlphabetSymbol(call);
            }

            // try to find a shorter terminating sequence for 'sym' before procedure is invoked in other hypotheses
            this.atrProvider.scanRefinedProcedures(Collections.singletonMap(sym, newLearner.getHypothesisModel()),
                                                   subLearners,
                                                   activeAlphabet);
            this.activeAlphabet.add(sym);

            for (final L learner : this.subLearners.values()) {
                learner.addAlphabetSymbol(sym);
            }
        }

        if (!newProcedures.isEmpty()) {
            this.atrProvider.scanRefinedProcedures(getSubModels(), subLearners, activeAlphabet);
            return true;
        } else {
            return false;
        }
    }

    private Map<I, DFA<?, I>> getSubModels() {
        final Map<I, DFA<?, I>> subModels = Maps.newHashMapWithExpectedSize(this.subLearners.size());

        for (final Map.Entry<I, L> entry : this.subLearners.entrySet()) {
            subModels.put(entry.getKey(), entry.getValue().getHypothesisModel());
        }

        return subModels;
    }

    private boolean updateATRAndCheckTSConformance(SPA<?, I> hypothesis) {
        boolean refinement = false;
        Map<I, DFA<?, I>> subModels = hypothesis.getProcedures();

        while (checkAndEnsureTSConformance(subModels)) {
            refinement = true;
            subModels = getSubModels();
            this.atrProvider.scanRefinedProcedures(subModels, subLearners, activeAlphabet);
        }

        return refinement;
    }

    private <S> int detectRejectingProcedure(Predicate<Word<I>> rejectingSystem, Word<I> input) {

        final List<Integer> returnIndices = new ArrayList<>();

        for (int i = 0; i < input.length(); i++) {
            if (this.alphabet.isReturnSymbol(input.getSymbol(i))) {
                returnIndices.add(i);
            }
        }

        // skip last index, because we know its accepting
        int returnIdxPos = findLowestAcceptingReturnIndex(rejectingSystem,
                                                          input,
                                                          returnIndices.subList(0, returnIndices.size() - 1));

        // if everything is rejecting the error happens at the main procedure
        if (returnIdxPos == -1) {
            returnIdxPos = returnIndices.size() - 1;
        }

        return returnIndices.get(returnIdxPos);
    }

    private int findLowestAcceptingReturnIndex(Predicate<? super Word<I>> system,
                                               Word<I> input,
                                               List<Integer> returnIndices) {

        int lower = 0;
        int upper = returnIndices.size() - 1;
        int result = -1;

        while (upper - lower > -1) {
            final int mid = lower + ((upper - lower) / 2);
            final int returnIdx = returnIndices.get(mid);

            final boolean answer = acceptsDecomposition(system, input, returnIdx + 1);

            if (answer) {
                result = mid;
                upper = mid - 1;
            } else {
                lower = mid + 1;
            }
        }

        return result;
    }

    private boolean acceptsDecomposition(Predicate<? super Word<I>> system, Word<I> input, int idxAfterReturn) {
        final Deque<Word<I>> wordStack = new ArrayDeque<>();
        int idx = idxAfterReturn;

        while (idx > 0) {
            final int callIdx = transformationUtil.findCallIndex(input, idx);
            final I callSymbol = input.getSymbol(callIdx);
            final Word<I> normalized = transformationUtil.normalize(input.subWord(callIdx + 1, idx), 0);
            final Word<I> expanded = transformationUtil.expand(normalized, this.atrProvider::getTerminatingSequence);

            wordStack.push(expanded.prepend(callSymbol));

            idx = callIdx;
        }

        final WordBuilder<I> builder = new WordBuilder<>();
        wordStack.forEach(builder::append);
        builder.append(input.subWord(idxAfterReturn));

        return system.test(builder.toWord());
    }

    private boolean checkAndEnsureTSConformance(Map<I, DFA<?, I>> subModels) {
        boolean refinement = false;

        for (final I procedure : this.subLearners.keySet()) {
            final Word<I> terminatingSequence = this.atrProvider.getTerminatingSequence(procedure);
            final WordBuilder<I> embeddedTS = new WordBuilder<>(terminatingSequence.size() + 2);
            embeddedTS.append(procedure);
            embeddedTS.append(terminatingSequence);
            embeddedTS.append(alphabet.getReturnSymbol());
            refinement |= checkSingleTerminatingSequence(embeddedTS.toWord(), subModels);
        }

        return refinement;
    }

    private boolean checkSingleTerminatingSequence(Word<I> input, Map<I, DFA<?, I>> hypotheses) {
        boolean refinement = false;

        for (int i = 0; i < input.size(); i++) {
            final I sym = input.getSymbol(i);

            if (this.alphabet.isCallSymbol(sym)) {
                final int returnIdx = this.transformationUtil.findReturnIndex(input, i + 1);
                final Word<I> projectedRun = this.transformationUtil.normalize(input.subWord(i + 1, returnIdx), 0);

                if (!hypotheses.get(sym).accepts(projectedRun)) {
                    refinement = true;
                    subLearners.get(sym).refineHypothesis(new DefaultQuery<>(projectedRun, true));
                }
            }
        }

        return refinement;
    }

    public JointCounterOracle<I, Boolean> getCeOracle() {
        return this.ceOracle;
    }

    public long getNumberOfGlobalRefinements() {
        return numberOfRefinements;
    }

    public long getNumberOfLocalRefinements() {
        long numberOfLocalRefinements = 0;

        for (L subLearner : subLearners.values()) {
            numberOfLocalRefinements += subLearner.getNumberOfLocalRefinements();
        }

        return numberOfLocalRefinements;
    }

    public long getSumOfLocalCELengths() {
        long sumOfLocalCELengths = 0;

        for (L subLearner : subLearners.values()) {
            sumOfLocalCELengths += subLearner.getSumOfLocalCELengths();
        }

        return sumOfLocalCELengths;
    }

}

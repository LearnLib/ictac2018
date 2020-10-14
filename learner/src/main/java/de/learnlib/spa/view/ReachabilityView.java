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
package de.learnlib.spa.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import de.learnlib.spa.api.SPAAlphabet;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.graphs.TransitionEdge;
import net.automatalib.commons.util.Pair;
import net.automatalib.graphs.Graph;
import net.automatalib.util.graphs.Path;
import net.automatalib.util.graphs.ShortestPaths;
import net.automatalib.visualization.VisualizationHelper;

/**
 * Graph representation of sub-procedures that only contains nodes and edges on paths leading to an accepting state.
 *
 * @param <S>
 *         hypotheses state type
 * @param <I>
 *         input symbol type
 *
 * @author frohme
 */
public class ReachabilityView<S, I> implements Graph<Pair<I, S>, Pair<Pair<I, S>, I>> {

    private final SPAAlphabet<I> alphabet;
    private final Map<I, ? extends DFA<S, I>> subModels;

    private final List<I> proceduralAlphabet;
    private final Map<I, Map<S, Boolean>> displayableNodesMap;

    public ReachabilityView(final SPAAlphabet<I> alphabet, final Map<I, ? extends DFA<S, I>> subModels) {
        this.alphabet = alphabet;
        this.subModels = subModels;

        this.proceduralAlphabet = new ArrayList<>(alphabet.getNumInternals() + subModels.size());
        this.proceduralAlphabet.addAll(alphabet.getInternalAlphabet());
        this.proceduralAlphabet.addAll(subModels.keySet());

        this.displayableNodesMap = Maps.newHashMapWithExpectedSize(alphabet.getNumCalls());
        this.computeNodesToDisplay();
    }

    private void computeNodesToDisplay() {

        for (final Map.Entry<I, ? extends DFA<S, I>> e : subModels.entrySet()) {
            final I symbol = e.getKey();
            final DFA<S, I> subModel = e.getValue();

            final Graph<S, TransitionEdge<I, S>> subModelAsGraph =
                    subModel.transitionGraphView(this.proceduralAlphabet);
            final Set<S> acceptingStates =
                    subModel.getStates().stream().filter(subModel::isAccepting).collect(Collectors.toSet());

            final Map<S, Boolean> displayMap = Maps.newHashMapWithExpectedSize(this.proceduralAlphabet.size());

            for (final S s : subModel.getStates()) {
                if (subModel.isAccepting(s)) {
                    displayMap.put(s, true);
                } else {
                    // can reach an accepting state?
                    final Iterable<Path<S, TransitionEdge<I, S>>> shortestPaths =
                            ShortestPaths.shortestPaths(subModelAsGraph, s, subModel.size(), acceptingStates);
                    if (shortestPaths.iterator().hasNext()) {
                        displayMap.put(s, true);
                    }
                }
            }

            this.displayableNodesMap.put(symbol, displayMap);
        }
    }

    @Override
    public Collection<Pair<I, S>> getNodes() {
        final List<Pair<I, S>> result = new LinkedList<>();

        for (final Map.Entry<I, ? extends DFA<S, I>> e : subModels.entrySet()) {
            final I procedure = e.getKey();
            final DFA<S, I> subModel = e.getValue();

            final Map<S, Boolean> localDisplayMap = this.displayableNodesMap.get(procedure);

            subModel.getStates()
                    .stream()
                    .filter(localDisplayMap::containsKey)
                    .filter(localDisplayMap::get)
                    .forEach(s -> result.add(Pair.of(procedure, s)));
        }

        return result;
    }

    @Override
    public Collection<Pair<Pair<I, S>, I>> getOutgoingEdges(Pair<I, S> node) {

        final I procedure = node.getFirst();
        final S state = node.getSecond();

        final DFA<S, I> subModel = this.subModels.get(procedure);
        final Map<S, Boolean> localDisplayMap = this.displayableNodesMap.get(procedure);

        final List<Pair<Pair<I, S>, I>> result = new LinkedList<>();

        for (final I i : this.proceduralAlphabet) {
            final Boolean isDisplayed = localDisplayMap.get(subModel.getSuccessor(state, i));

            if (isDisplayed != null && isDisplayed) {
                result.add(Pair.of(node, i));
            }
        }

        return result;
    }

    @Override
    public Pair<I, S> getTarget(Pair<Pair<I, S>, I> edge) {
        final Pair<I, S> state = edge.getFirst();

        final I input = edge.getSecond();
        final I identifier = state.getFirst();

        final DFA<S, I> subModel = subModels.get(identifier);

        final S next = subModel.getSuccessor(state.getSecond(), input);
        return Pair.of(state.getFirst(), next);
    }

    @Override
    public VisualizationHelper<Pair<I, S>, Pair<Pair<I, S>, I>> getVisualizationHelper() {
        return new DotHelper<>(subModels, alphabet.getCallAlphabet());
    }
}

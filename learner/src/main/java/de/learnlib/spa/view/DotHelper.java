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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import de.learnlib.spa.impl.DefaultSPA;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.dot.DefaultDOTVisualizationHelper;
import net.automatalib.words.Alphabet;

/**
 * Helper-class to aggregate some render definitions for {@link DefaultSPA}s.
 *
 * @param <I>
 *         input symbol type
 * @param <S>
 *         hypotheses state type
 *
 * @author frohme
 */
class DotHelper<I, S> extends DefaultDOTVisualizationHelper<Pair<I, S>, Pair<Pair<I, S>, I>> {

    final Map<I, ? extends DFA<S, I>> subModels;
    final Alphabet<I> callAlphabet;

    DotHelper(Map<I, ? extends DFA<S, I>> subModels, Alphabet<I> callAlphabet) {
        this.subModels = subModels;
        this.callAlphabet = callAlphabet;
    }

    @Override
    protected Collection<Pair<I, S>> initialNodes() {
        return subModels.entrySet()
                        .stream()
                        .map(e -> Pair.of(e.getKey(), e.getValue().getInitialState()))
                        .collect(Collectors.toSet());
    }

    @Override
    public boolean getNodeProperties(final Pair<I, S> node, final Map<String, String> properties) {

        super.getNodeProperties(node, properties);

        final I identifier = node.getFirst();
        final DFA<S, I> effectiveModel = subModels.get(identifier);

        if (effectiveModel.isAccepting(node.getSecond())) {
            properties.put(NodeAttrs.SHAPE, NodeShapes.DOUBLECIRCLE);
        } else {
            properties.put(NodeAttrs.SHAPE, NodeShapes.CIRCLE);
        }
        properties.put(NodeAttrs.LABEL, node.getFirst() + " " + node.getSecond());

        return true;
    }

    @Override
    public boolean getEdgeProperties(final Pair<I, S> src,
                                     final Pair<Pair<I, S>, I> edge,
                                     final Pair<I, S> tgt,
                                     final Map<String, String> properties) {

        super.getEdgeProperties(src, edge, tgt, properties);

        properties.put(EdgeAttrs.LABEL, edge.getSecond().toString());

        if (callAlphabet.containsSymbol(edge.getSecond())) {
            properties.put(EdgeAttrs.STYLE, "dashed");
        }

        return true;
    }

    @Override
    public void writePreamble(Appendable a) throws IOException {
        a.append("rankdir=LR;\n");
    }
}

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

import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;
import de.learnlib.spa.api.SPA;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.impl.DefaultSPA;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.random.RandomAutomata;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.SimpleAlphabet;

public class Generator {

    public static <I> SPA<?, I> create(Random random, SPAAlphabet<I> alphabet, int procedureSize) {

        final Map<I, DFA<Integer, I>> dfas = Maps.newHashMapWithExpectedSize(alphabet.getNumCalls());

        final Alphabet<I> proceduralAlphabet = new SimpleAlphabet<>();
        proceduralAlphabet.addAll(alphabet.getCallAlphabet());
        proceduralAlphabet.addAll(alphabet.getInternalAlphabet());

        for (final I procedure : alphabet.getCallAlphabet()) {
            final CompactDFA<I> dfa = RandomAutomata.randomICDFA(random, procedureSize, proceduralAlphabet, true);
            dfas.put(procedure, dfa);
        }

        return new DefaultSPA<>(alphabet,
                                alphabet.getCallAlphabet().apply(random.nextInt(alphabet.getNumCalls())),
                                dfas);
    }

}
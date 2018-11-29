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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.spa.api.SPA;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.impl.DefaultSPA;
import de.learnlib.spa.impl.DefaultSPAAlphabet;
import de.learnlib.spa.palindrome.InputSymbol;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.builders.AutomatonBuilders;
import net.automatalib.visualization.Visualization;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author frohme
 */
public final class PalindromeExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(PalindromeExample.class);

    public static void main(String[] args) {

        final Alphabet<InputSymbol> callAlphabet = Alphabets.fromArray(InputSymbol.S, InputSymbol.T);
        final Alphabet<InputSymbol> internalAlphabet = Alphabets.fromArray(InputSymbol.a, InputSymbol.b, InputSymbol.c);

        final Set<InputSymbol> joinedSymbols = new HashSet<>();
        joinedSymbols.addAll(callAlphabet);
        joinedSymbols.addAll(internalAlphabet);

        final Alphabet<InputSymbol> joinedAlphabet = Alphabets.fromCollection(joinedSymbols);

        // @formatter:off
        final CompactDFA<InputSymbol> sProcedure = AutomatonBuilders.newDFA(joinedAlphabet)
                                                                    .withInitial("s0")
                                                                    .withAccepting("s0", "s1", "s2", "s5")
                                                                    .from("s0").on(InputSymbol.T).to("s5")
                                                                    .from("s0").on(InputSymbol.a).to("s1")
                                                                    .from("s0").on(InputSymbol.b).to("s2")
                                                                    .from("s1").on(InputSymbol.S).to("s3")
                                                                    .from("s2").on(InputSymbol.S).to("s4")
                                                                    .from("s3").on(InputSymbol.a).to("s5")
                                                                    .from("s4").on(InputSymbol.b).to("s5")
                                                                    .create();

        final CompactDFA<InputSymbol> tProcedure = AutomatonBuilders.newDFA(joinedAlphabet)
                                                                    .withInitial("t0")
                                                                    .withAccepting("t1", "t3")
                                                                    .from("t0").on(InputSymbol.S).to("t3")
                                                                    .from("t0").on(InputSymbol.c).to("t1")
                                                                    .from("t1").on(InputSymbol.T).to("t2")
                                                                    .from("t2").on(InputSymbol.c).to("t3")
                                                                    .create();
        // @formatter:on

        final Map<InputSymbol, CompactDFA<InputSymbol>> subModels = new HashMap<>();
        subModels.put(InputSymbol.S, sProcedure);
        subModels.put(InputSymbol.T, tProcedure);

        final SPAAlphabet<InputSymbol> alphabet =
                new DefaultSPAAlphabet<>(callAlphabet, internalAlphabet, InputSymbol.R);
        final SPA<?, InputSymbol> spa = new DefaultSPA<>(alphabet, InputSymbol.S, subModels);

        // @formatter:off
        LOGGER.info("Well-matched palindromes");
        logTrace(spa, InputSymbol.S, InputSymbol.R);
        logTrace(spa, InputSymbol.S, InputSymbol.a, InputSymbol.R);
        logTrace(spa, InputSymbol.S, InputSymbol.a, InputSymbol.S, InputSymbol.R, InputSymbol.a, InputSymbol.R);
        logTrace(spa, InputSymbol.S, InputSymbol.b, InputSymbol.S, InputSymbol.T, InputSymbol.c, InputSymbol.R, InputSymbol.R, InputSymbol.b, InputSymbol.R);

        LOGGER.info("Well-matched but invalid words");
        logTrace(spa, InputSymbol.S, InputSymbol.a, InputSymbol.a, InputSymbol.R);
        logTrace(spa, InputSymbol.S, InputSymbol.a, InputSymbol.T, InputSymbol.a, InputSymbol.R, InputSymbol.a, InputSymbol.R);
        logTrace(spa);

        LOGGER.info("Ill-matched/non-rooted words");
        logTrace(spa, InputSymbol.S, InputSymbol.S, InputSymbol.S);
        logTrace(spa, InputSymbol.R, InputSymbol.S);
        logTrace(spa, InputSymbol.a, InputSymbol.b, InputSymbol.a);
        // @formatter:on

        Visualization.visualize(spa);
    }

    @SafeVarargs
    private static <S, I> void logTrace(SPA<S, I> spa, I... inputs) {
        final List<I> asList = Arrays.asList(inputs);
        final boolean accepted = spa.computeOutput(asList);

        LOGGER.info("Word {} is {}accepted by the SPA", inputs, accepted ? "" : "not ");
    }

}

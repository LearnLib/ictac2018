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
package de.learnlib.spa.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import analysis.coverage.Analyzer;
import com.google.common.collect.Sets;
import de.learnlib.api.oracle.QueryAnswerer;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.impl.DefaultSPAAlphabet;
import grammar.cfg.MyGrammar;
import grammar.cfg.MyProduction;
import javacc.ParseException;
import javacc.ProductionParser;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

/**
 * @author frohme
 */
public final class Instrumenter {

    public static final String RETURN_SYMBOL = "R";

    private Instrumenter() {}

    public static InstrumentationResult generate(final String productionRules) throws ParseException {

        ProductionParser parser = new ProductionParser(productionRules);
        MyProduction[] initialProductions = parser.parser();

        final MyGrammar initialGrammar = new MyGrammar();
        initialGrammar.addProductions(initialProductions);

        if (Arrays.asList(initialGrammar.getVariables()).contains(RETURN_SYMBOL)) {
            throw new IllegalArgumentException("Variables contain return symbol");
        }

        final Set<String> callSymbols = Sets.newHashSetWithExpectedSize(initialGrammar.getVariablesNumber());
        final List<MyProduction> newProductions = new ArrayList<>(initialProductions.length);
        String initialCallsymbol = null;

        // Rewrite <S> -> abc to <S> -> S abc R
        for (final MyProduction prod : initialProductions) {
            final String productionHead = prod.getLHS();
            final String variable = productionHead.substring(1, productionHead.length() - 1);

            if (initialCallsymbol == null) {
                initialCallsymbol = variable;
            }

            final String newRHS = variable + " " + prod.getRHS() + " " + RETURN_SYMBOL;

            newProductions.add(new MyProduction(prod.getLHS(), newRHS));
            callSymbols.add(variable);
        }

        final MyGrammar actualGrammar = new MyGrammar();

        newProductions.forEach(actualGrammar::addProduction);
        actualGrammar.setStartVariable(newProductions.get(0).getLHS());

        final Alphabet<String> callAlphabet = Alphabets.fromCollection(callSymbols);
        final Alphabet<String> internalAlphabet = Alphabets.fromArray(initialGrammar.getTerminals());

        return new InstrumentationResult(initialCallsymbol,
                                         new DefaultSPAAlphabet<>(internalAlphabet, callAlphabet, RETURN_SYMBOL),
                                         actualGrammar);
    }

    public static class InstrumentationResult {

        private final String initialCallSymbol;

        private final SPAAlphabet<String> alphabet;

        private final MyGrammar grammar;

        private final QueryAnswerer<String, Boolean> target;

        public InstrumentationResult(String initialCallSymbol, SPAAlphabet<String> alphabet, MyGrammar grammar) {
            this.initialCallSymbol = initialCallSymbol;
            this.alphabet = alphabet;
            this.grammar = grammar;
            this.target = new CFGAcceptor(new Analyzer(grammar));
        }

        public String getInitialCallSymbol() {
            return initialCallSymbol;
        }

        public SPAAlphabet<String> getAlphabet() {
            return alphabet;
        }

        public MyGrammar getGrammar() {
            return grammar;
        }

        public QueryAnswerer<String, Boolean> getAnswerer() {
            return target;
        }
    }

    public static class CFGAcceptor implements QueryAnswerer<String, Boolean> {

        private final Analyzer analyzer;

        public CFGAcceptor(Analyzer analyzer) {
            this.analyzer = analyzer;
        }

        @Override
        public Boolean answerQuery(Word<String> prefix, Word<String> suffix) {
            final String query = prefix.concat(suffix).stream().collect(Collectors.joining(" "));
            return analyzer.analyzeSentence(query);
        }
    }

}

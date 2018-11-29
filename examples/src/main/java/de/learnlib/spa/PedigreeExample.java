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

import java.io.InputStream;

import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.oracle.equivalence.SampleSetEQOracle;
import de.learnlib.oracle.membership.MappedOracle;
import de.learnlib.spa.api.SPAAlphabet;
import de.learnlib.spa.config.TTTAdapter;
import de.learnlib.spa.impl.DefaultSPAAlphabet;
import de.learnlib.spa.learner.SPALearner;
import de.learnlib.spa.pedigree.InputMapper;
import de.learnlib.spa.pedigree.InputSymbol;
import de.learnlib.spa.pedigree.Samples;
import de.learnlib.spa.util.XmlDtdValidationMQOracle;
import net.automatalib.visualization.Visualization;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;

/**
 * @author frohme
 */
public final class PedigreeExample {

    public static void main(String[] args) {

        // Construct input alphabet
        final Alphabet<InputSymbol> internalAlphabet = Alphabets.fromArray(InputSymbol.NAME, InputSymbol.DIED);
        final Alphabet<InputSymbol> callAlphabet =
                Alphabets.fromArray(InputSymbol.PERSON, InputSymbol.MOTHER, InputSymbol.FATHER);

        final SPAAlphabet<InputSymbol> alphabet =
                new DefaultSPAAlphabet<>(internalAlphabet, callAlphabet, InputSymbol.RETURN);

        // Construct Membership Oracle
        final InputStream dtdStream = PedigreeExample.class.getResourceAsStream("/pedigree.dtd");
        final MembershipOracle<InputSymbol, Boolean> mqo =
                new MappedOracle<>(new XmlDtdValidationMQOracle(dtdStream, InputSymbol.PERSON.toXml()),
                                   new InputMapper(alphabet));

        // Construct Equivalence Oracle
        final SampleSetEQOracle<InputSymbol, Boolean> eqo = new SampleSetEQOracle<>(false);
        eqo.addAll(mqo, Samples.CHARACTERISTIC_SAMPLES);

        // Construct learner
        final SPALearner<InputSymbol, TTTAdapter<InputSymbol>> learner =
                new SPALearner<>(alphabet, mqo, TTTAdapter::new);

        learner.startLearning();

        DefaultQuery<InputSymbol, Boolean> ce;

        // The active learning loop
        while ((ce = eqo.findCounterExample(learner.getHypothesisModel(), alphabet)) != null) {
            while (learner.refineHypothesis(ce)) {}
        }

        // To visualize the model, GraphViz needs to be installed
        Visualization.visualize(learner.getHypothesisModel());
    }

}

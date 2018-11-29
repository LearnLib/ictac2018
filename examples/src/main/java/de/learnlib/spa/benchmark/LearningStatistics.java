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

import de.learnlib.spa.api.SPA;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.vpda.OneSEVPA;
import net.automatalib.ts.acceptors.DeterministicAcceptorTS;
import org.apache.commons.lang3.time.StopWatch;

/**
 * @author frohme
 */
public class LearningStatistics<I> {

    private final long numberOfCEs;
    private final long numberOfMQs;
    private final long numberOfSymbols;

    private final StopWatch stopWatch;

    private final DeterministicAcceptorTS<?, I> hypothesis;

    public LearningStatistics(long numberOfCEs,
                              long numberOfMQs,
                              long numberOfSymbols,
                              StopWatch stopWatch,
                              DeterministicAcceptorTS<?, I> hypothesis) {
        this.numberOfCEs = numberOfCEs;
        this.numberOfMQs = numberOfMQs;
        this.numberOfSymbols = numberOfSymbols;
        this.stopWatch = stopWatch;
        this.hypothesis = hypothesis;
    }

    public long getNumberOfCEs() {
        return numberOfCEs;
    }

    public long getNumberOfMQs() {
        return numberOfMQs;
    }

    public long getNumberOfSymbols() {
        return numberOfSymbols;
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }

    public long getSize() {
        if (hypothesis instanceof OneSEVPA) {
            return ((OneSEVPA<?, I>) hypothesis).size();
        } else if (hypothesis instanceof SPA) {
            return ((SPA) hypothesis).size();
        } else if (hypothesis instanceof DFA) {
            return ((DFA) hypothesis).size();
        }

        return -1;
    }
}

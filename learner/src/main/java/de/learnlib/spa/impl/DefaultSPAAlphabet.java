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
package de.learnlib.spa.impl;

import de.learnlib.spa.api.SPAAlphabet;
import net.automatalib.words.Alphabet;
import net.automatalib.words.VPDAlphabet;
import net.automatalib.words.impl.Alphabets;
import net.automatalib.words.impl.DefaultVPDAlphabet;

public class DefaultSPAAlphabet<I> extends DefaultVPDAlphabet<I> implements SPAAlphabet<I>, VPDAlphabet<I> {

    public DefaultSPAAlphabet(Alphabet<I> internalAlphabet, Alphabet<I> callAlphabet, I returnSymbol) {
        super(internalAlphabet, callAlphabet, Alphabets.singleton(returnSymbol));
    }

}

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

import java.util.List;
import java.util.function.Function;

import de.learnlib.spa.api.SPAAlphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

/**
 * Utility class for projecting/expanding words and index calculations.
 *
 * @param <I>
 *         input symbol class
 *
 * @author frohme
 */
public class TransformationUtil<I> {

    private final SPAAlphabet<I> alphabet;

    public TransformationUtil(SPAAlphabet<I> alphabet) {
        this.alphabet = alphabet;
    }

    /**
     * Return the index of the procedural call for the procedure currently executing the symbol at pos {@code idx}.
     */
    public int findCallIndex(final Word<I> input, final int idx) {
        return findCallIndex(input.asList(), idx);
    }

    public int findCallIndex(final List<I> input, final int idx) {

        int balance = 0;

        for (int i = idx - 1; i >= 0; i--) {
            final I sym = input.get(i);

            if (this.alphabet.isReturnSymbol(sym)) {
                balance++;
            }

            if (this.alphabet.isCallSymbol(sym)) {
                if (balance > 0) {
                    balance--;
                } else {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * replaces all inner calls with an abstracted procedure call.
     */
    public Word<I> normalize(final Word<I> input, final int idx) {
        final WordBuilder<I> wb = new WordBuilder<>(input.size());

        for (int i = idx; i < input.size(); i++) {
            final I sym = input.getSymbol(i);

            if (this.alphabet.isCallSymbol(sym)) {
                final int returnIdx = findReturnIndex(input, i + 1);

                if (returnIdx == -1) {
                    throw new IllegalArgumentException();
                }

                wb.append(input.getSymbol(i));
                i = returnIdx;
            } else {
                wb.append(input.getSymbol(i));
            }
        }

        return wb.toWord();
    }

    /**
     * Return the index of the return call of the procedure currently active at (before) {@code idx}.
     */
    public int findReturnIndex(final Word<I> input, final int idx) {
        return findReturnIndex(input.asList(), idx);
    }

    public int findReturnIndex(final List<I> input, final int idx) {

        int balance = 0;

        for (int i = idx; i < input.size(); i++) {
            final I sym = input.get(i);

            if (this.alphabet.isCallSymbol(sym)) {
                balance++;
            }

            if (this.alphabet.isReturnSymbol(sym)) {
                if (balance > 0) {
                    balance--;
                } else {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Replaces all abstracted procedure calls with their corresponding terminating sequence.
     */
    public Word<I> expand(final Word<I> input, final Function<I, Word<I>> terminatingSequenceProvider) {
        final WordBuilder<I> wb = new WordBuilder<>();

        for (int i = 0; i < input.size(); i++) {
            final I sym = input.getSymbol(i);

            if (this.alphabet.isCallSymbol(sym)) {
                wb.append(sym);
                wb.append(terminatingSequenceProvider.apply(sym));
                wb.append(this.alphabet.getReturnSymbol());
            } else {
                wb.append(sym);
            }
        }

        return wb.toWord();
    }
}

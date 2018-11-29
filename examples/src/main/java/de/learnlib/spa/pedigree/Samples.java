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
package de.learnlib.spa.pedigree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.automatalib.words.Word;

/**
 * @author frohme
 */
public final class Samples {

    public static final Word<InputSymbol> ALL_PROCEDURES = Word.fromSymbols(InputSymbol.PERSON,
                                                                            InputSymbol.NAME,
                                                                            InputSymbol.DIED,
                                                                            InputSymbol.FATHER,
                                                                            InputSymbol.PERSON,
                                                                            InputSymbol.NAME,
                                                                            InputSymbol.RETURN,
                                                                            InputSymbol.RETURN,
                                                                            InputSymbol.MOTHER,
                                                                            InputSymbol.PERSON,
                                                                            InputSymbol.NAME,
                                                                            InputSymbol.RETURN,
                                                                            InputSymbol.RETURN,
                                                                            InputSymbol.RETURN);

    public static final Word<InputSymbol> NO_DOUBLE_FATHER_IN_PERSON = Word.fromSymbols(InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.FATHER,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.FATHER,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN);

    public static final Word<InputSymbol> NO_DOUBLE_MOTHER_IN_PERSON = Word.fromSymbols(InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.MOTHER,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.MOTHER,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN);

    public static final Word<InputSymbol> NO_DOUBLE_MOTHER_AFTER_FATHER_IN_PERSON = Word.fromSymbols(InputSymbol.PERSON,
                                                                                                     InputSymbol.NAME,
                                                                                                     InputSymbol.DIED,
                                                                                                     InputSymbol.FATHER,
                                                                                                     InputSymbol.PERSON,
                                                                                                     InputSymbol.NAME,
                                                                                                     InputSymbol.RETURN,
                                                                                                     InputSymbol.RETURN,
                                                                                                     InputSymbol.MOTHER,
                                                                                                     InputSymbol.PERSON,
                                                                                                     InputSymbol.NAME,
                                                                                                     InputSymbol.RETURN,
                                                                                                     InputSymbol.RETURN,
                                                                                                     InputSymbol.MOTHER,
                                                                                                     InputSymbol.PERSON,
                                                                                                     InputSymbol.NAME,
                                                                                                     InputSymbol.RETURN,
                                                                                                     InputSymbol.RETURN,
                                                                                                     InputSymbol.RETURN);

    public static final Word<InputSymbol> NO_TRIPLE_PERSON_IN_FATHER = Word.fromSymbols(InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.FATHER,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN);

    public static final Word<InputSymbol> NO_TRIPLE_PERSON_IN_MOTHER = Word.fromSymbols(InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.MOTHER,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.PERSON,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN,
                                                                                        InputSymbol.RETURN);

    public static final Word<InputSymbol> NO_MULTIPLE_ATTR_IN_PERSON = Word.fromSymbols(InputSymbol.PERSON,
                                                                                        InputSymbol.DIED,
                                                                                        InputSymbol.DIED,
                                                                                        InputSymbol.NAME,
                                                                                        InputSymbol.RETURN);

    public static final List<Word<InputSymbol>> CHARACTERISTIC_SAMPLES = Arrays.asList(Samples.ALL_PROCEDURES,
                                                                                       Samples.NO_DOUBLE_FATHER_IN_PERSON,
                                                                                       Samples.NO_DOUBLE_MOTHER_IN_PERSON,
                                                                                       Samples.NO_DOUBLE_MOTHER_AFTER_FATHER_IN_PERSON,
                                                                                       Samples.NO_MULTIPLE_ATTR_IN_PERSON,
                                                                                       Samples.NO_TRIPLE_PERSON_IN_MOTHER,
                                                                                       Samples.NO_TRIPLE_PERSON_IN_FATHER);

    private Samples() {
        // prevent instantiation
    }

    public static Word<InputSymbol> getAllProceduresWithRedundancy(int nestingDepth) {
        final List<InputSymbol> inputSymbols = new ArrayList<>();

        inputSymbols.add(InputSymbol.PERSON);
        inputSymbols.add(InputSymbol.NAME);
        inputSymbols.add(InputSymbol.DIED);
        inputSymbols.add(InputSymbol.FATHER);

        nestFatherHierarchy(inputSymbols, nestingDepth);

        inputSymbols.add(InputSymbol.RETURN);
        inputSymbols.add(InputSymbol.MOTHER);
        inputSymbols.add(InputSymbol.PERSON);
        inputSymbols.add(InputSymbol.NAME);
        inputSymbols.add(InputSymbol.RETURN);
        inputSymbols.add(InputSymbol.RETURN);
        inputSymbols.add(InputSymbol.RETURN);

        return Word.fromList(inputSymbols);
    }

    private static void nestFatherHierarchy(List<InputSymbol> inputSymbols, int nestingDepth) {
        inputSymbols.add(InputSymbol.PERSON);
        inputSymbols.add(InputSymbol.NAME);

        if (nestingDepth > 0) {
            inputSymbols.add(InputSymbol.FATHER);
            nestFatherHierarchy(inputSymbols, nestingDepth - 1);
            inputSymbols.add(InputSymbol.RETURN);
        }

        inputSymbols.add(InputSymbol.RETURN);
    }
}

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

import java.util.Stack;

import de.learnlib.api.Mapper.AsynchronousMapper;
import net.automatalib.words.VPDAlphabet;

/**
 * @author frohme
 */
public class InputMapper implements AsynchronousMapper<InputSymbol, Boolean, String, Boolean> {

    private final VPDAlphabet<InputSymbol> alphabet;
    private final Stack<InputSymbol> callStack = new Stack<>();
    private boolean inTag;

    public InputMapper(VPDAlphabet<InputSymbol> alphabet) {
        this.alphabet = alphabet;
    }

    @Override
    public String mapInput(InputSymbol abstractInput) {
        switch (alphabet.getSymbolType(abstractInput)) {
            case CALL: {
                callStack.push(abstractInput);

                // close current tag
                final String prefix;
                if (inTag) {
                    prefix = "><";
                } else {
                    prefix = "<";
                }

                inTag = true;
                return prefix + abstractInput.toXml();
            }
            case INTERNAL:
                return " " + abstractInput.toXml() + " ";
            case RETURN: {
                if (callStack.isEmpty()) {
                    return "ERROR";
                }

                // close current tag
                final String prefix;
                if (inTag) {
                    prefix = "></";
                } else {
                    prefix = "</";
                }
                inTag = false;

                final InputSymbol top = callStack.pop();
                return prefix + top.toXml() + ">";
            }
            default:
                throw new IllegalStateException("Unhandled case");
        }
    }

    @Override
    public Boolean mapOutput(Boolean concreteOutput) {
        return concreteOutput;
    }
}

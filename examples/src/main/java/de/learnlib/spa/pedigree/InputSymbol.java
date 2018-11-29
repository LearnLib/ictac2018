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

/**
 * @author frohme
 */
public enum InputSymbol {
    PERSON("PERSON", "person"),
    MOTHER("MOTHER", "mother"),
    FATHER("FATHER", "father"),
    NAME("name", "name=\"...\""),
    DIED("died", "died=\"...\""),
    RETURN("R", null);

    private String value;
    private String xml;

    InputSymbol(String value, String xml) {
        this.value = value;
        this.xml = xml;
    }

    @Override
    public String toString() {
        return value;
    }

    public String toXml() {
        return xml;
    }
}

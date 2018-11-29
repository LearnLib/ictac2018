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

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ExampleTest {

    @BeforeClass
    public void setupAutoClose() {
        // As soon as we observe an event that indicates a new window, close it to prevent blocking the tests.
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            final WindowEvent windowEvent = (WindowEvent) event;
            final Window w = windowEvent.getWindow();
            w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        }, AWTEvent.WINDOW_FOCUS_EVENT_MASK);
    }

    @Test
    public void testPalindromExample() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> PalindromeExample.main(new String[0]));
    }

    @Test
    public void testPedigreeExample() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> PedigreeExample.main(new String[0]));
    }

}

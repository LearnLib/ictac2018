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
package de.learnlib.spa.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.io.ByteStreams;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.Query;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author frohme
 */
public class XmlDtdValidationMQOracle implements MembershipOracle<String, Boolean> {

    private final DocumentBuilder builder;

    private final String preamble;

    private byte[] dtdArray;

    public XmlDtdValidationMQOracle(final InputStream dtdStream, final String parentType) {

        try {
            dtdArray = ByteStreams.toByteArray(dtdStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setValidating(true);

            builder = domFactory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {

                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE ");
        sb.append(parentType);
        sb.append(" SYSTEM \"\">");

        preamble = sb.toString();
    }

    @Override
    public void processQueries(Collection<? extends Query<String, Boolean>> queries) {
        queries.forEach(this::processQuery);
    }

    @Override
    public void processQuery(Query<String, Boolean> query) {

        final StringBuilder sb = new StringBuilder(preamble);
        query.getInput().forEach(sb::append);

        try (InputStream is = new ByteArrayInputStream(dtdArray)) {
            builder.setEntityResolver((publicId, systemId) -> new InputSource(is));

            try (StringReader sr = new StringReader(sb.toString())) {
                builder.parse(new InputSource(sr));
            }

            query.answer(true);
        } catch (SAXException e) {
            query.answer(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

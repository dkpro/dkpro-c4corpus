/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.impl;

import de.tudarmstadt.ukp.dkpro.c4corpus.boilerplate.impl.Paragraph.PARAGRAPH_TYPE;

/**
 * Data structure representing a pair of integer and paragraph type
 *
 * @author Omnia Zayed
 */
public class Pair {

    public final Integer id;
    public final PARAGRAPH_TYPE classType;

    public Pair(Integer id, PARAGRAPH_TYPE c) {
        this.id = id;
        this.classType = c;
    }

    public Integer getID() {
        return this.id;
    }

    public PARAGRAPH_TYPE getClassType() {
        return this.classType;
    }
}

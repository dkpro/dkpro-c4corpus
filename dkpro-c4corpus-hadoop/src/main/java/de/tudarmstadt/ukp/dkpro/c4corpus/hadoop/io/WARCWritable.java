/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Copyright (c) 2014 Martin Kleppmann
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
package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io;

import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A mutable wrapper around a {@link WARCRecord} implementing the Hadoop Writable interface.
 * This allows WARC records to be used throughout Hadoop (e.g. written to sequence files
 * when shuffling data between mappers and reducers). The record is encoded as a single
 * record in standard WARC/1.0 format.
 * <p>
 * Based on https://github.com/ept/warc-hadoop
 * <p>
 * Note: originally published under MIT license, which is compatible with ASL license
 * https://www.gnu.org/philosophy/license-list.html
 *
 * @author Martin Kleppmann
 * @author Ivan Habernal
 */
public class WARCWritable
        implements Writable
{

    private WARCRecord record;

    /**
     * Creates an empty writable (with a null record).
     */
    public WARCWritable()
    {
        this.record = null;
    }

    /**
     * Creates a writable wrapper around a given WARCRecord.
     *
     * @param record existing record
     */
    public WARCWritable(WARCRecord record)
    {
        this.record = record;
    }

    /**
     * Returns the record currently wrapped by this writable.
     *
     * @return current record
     */
    public WARCRecord getRecord()
    {
        return record;
    }

    /**
     * Updates the record held within this writable wrapper.
     *
     * @param record the record to be set
     */
    public void setRecord(WARCRecord record)
    {
        this.record = record;
    }

    /**
     * Appends the current record to a {@link DataOutput} stream.
     */
    @Override
    public void write(DataOutput out)
            throws IOException
    {
        if (record != null) {
            record.write(out);
        }
    }

    /**
     * Parses a {@link WARCRecord} out of a {@link DataInput} stream, and makes it the
     * writable's current record.
     *
     * @throws IOException if the input is malformed
     */
    @Override
    public void readFields(DataInput in)
            throws IOException
    {
        record = new WARCRecord(in);
    }
}

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
package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Custom Writable for a composite key which wrap the id of the record with the
 * source of input dataset (whether it is from the corpus or the text file)
 *
 * @author Omnia Zayed
 * @author Ivan Habernal
 */
public class CompositeKey
        implements Writable, WritableComparable<CompositeKey>
{

    private Text joinKey = new Text();
    private IntWritable sourceIndex = new IntWritable(); //the source of the input data

    @Override
    public int compareTo(CompositeKey taggedKey)
    {
        int compareValue = this.joinKey.compareTo(taggedKey.getJoinKey());
        if (compareValue == 0) {
            compareValue = this.sourceIndex.compareTo(taggedKey.getSourceIndex());
        }
        return compareValue;
    }

    public Text getJoinKey()
    {
        return joinKey;
    }

    public IntWritable getSourceIndex()
    {
        return sourceIndex;
    }

    public void setJoinKey(Text key)
    {
        this.joinKey = new Text(key);
    }

    public void setSourceIndex(IntWritable index)
    {
        this.sourceIndex = new IntWritable(index.get());
    }

    @Override
    public void write(DataOutput d)
            throws IOException
    {
        joinKey.write(d);
        sourceIndex.write(d);
    }

    @Override
    public void readFields(DataInput di)
            throws IOException
    {
        joinKey.readFields(di);
        sourceIndex.readFields(di);
    }

    @Override
    public String toString()
    {
        return "CompositeKey{" +
                "joinKey=" + joinKey +
                ", sourceIndex=" + sourceIndex +
                '}';
    }
}

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

package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.utils;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Ivan Habernal
 */
public class URIExtractorTest
{
    @Test
    public void testMapper()
            throws IOException, InterruptedException
    {
        final String expectedURI = "https://www.ukp.tu-darmstadt.de/ukp-home/";

        final WARCWritable warc = EasyMock.mock(WARCWritable.class);
        final WARCRecord record = EasyMock.mock(WARCRecord.class);
        final WARCRecord.Header header = EasyMock.mock(WARCRecord.Header.class);

        @SuppressWarnings("unchecked")
        final URIExtractor.URIExtractorMapper.Context context = EasyMock
                .mock(URIExtractor.URIExtractorMapper.Context.class);

        EasyMock.expect(record.getHeader()).andReturn(header);
        EasyMock.expect(warc.getRecord()).andReturn(record);
        EasyMock.expect(header.getTargetURI()).andReturn(expectedURI);
        context.write(new Text(expectedURI), NullWritable.get());
        EasyMock.replay(warc, record, header, context);

        final URIExtractor.URIExtractorMapper mapper = new URIExtractor.URIExtractorMapper();
        mapper.map(new LongWritable(0), warc, context);

        EasyMock.verify(warc, record, header, context);
    }

    @Test
    public void testReducer()
            throws IOException, InterruptedException
    {
        final String expectedURI = "https://www.ukp.tu-darmstadt.de/ukp-home/";

        @SuppressWarnings("unchecked")
        final URIExtractor.URIExtractorReducer.Context context = EasyMock
                .mock(URIExtractor.URIExtractorReducer.Context.class);
        context.write(new Text(expectedURI), NullWritable.get());

        @SuppressWarnings("unchecked")
        final Iterable<NullWritable> values = EasyMock.mock(Iterable.class);

        EasyMock.replay(context, values);

        final URIExtractor.URIExtractorReducer reducer = new URIExtractor.URIExtractorReducer();

        reducer.reduce(new Text(expectedURI), values, context);

        EasyMock.verify(context, values);
    }
}

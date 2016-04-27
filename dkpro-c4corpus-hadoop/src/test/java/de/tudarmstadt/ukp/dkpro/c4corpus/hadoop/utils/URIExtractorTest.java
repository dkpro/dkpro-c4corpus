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

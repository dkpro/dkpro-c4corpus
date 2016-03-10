package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.uriextractor;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.easymock.EasyMock;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.WARCWritable;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord;
import de.tudarmstadt.ukp.dkpro.c4corpus.warc.io.WARCRecord.Header;

public class URIExtractorMapperTest {

	@Test
	public void test() throws IOException, InterruptedException {
		final String expectedURI = "https://www.ukp.tu-darmstadt.de/ukp-home/";

		final WARCWritable warc = EasyMock.mock(WARCWritable.class);
		final WARCRecord record = EasyMock.mock(WARCRecord.class);
		final Header header = EasyMock.mock(Header.class);

		@SuppressWarnings("unchecked")
		final URIExtractorMapper.Context context = EasyMock.mock(URIExtractorMapper.Context.class);

		EasyMock.expect(record.getHeader()).andReturn(header);
		EasyMock.expect(warc.getRecord()).andReturn(record);
		EasyMock.expect(header.getTargetURI()).andReturn(expectedURI);
		context.write(new Text(expectedURI), NullWritable.get());
		EasyMock.replay(warc, record, header, context);
		
		final URIExtractorMapper mapper = new URIExtractorMapper();
		mapper.map(new LongWritable(0), warc, context);
		
		EasyMock.verify(warc, record, header, context);
	}

}

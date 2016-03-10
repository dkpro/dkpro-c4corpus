package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.uriextractor;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.easymock.EasyMock;
import org.junit.Test;

public class URIExtractorReducerTest {

	@Test
	public void test() throws IOException, InterruptedException {
		final String expectedURI = "https://www.ukp.tu-darmstadt.de/ukp-home/";

		@SuppressWarnings("unchecked")
		final URIExtractorReducer.Context context = EasyMock.mock(URIExtractorReducer.Context.class);
		context.write(new Text(expectedURI), NullWritable.get());
		
		@SuppressWarnings("unchecked")
		final Iterable<NullWritable> values = EasyMock.mock(Iterable.class);
		
		EasyMock.replay(context, values);
		
		final URIExtractorReducer reducer = new URIExtractorReducer();
		
		reducer.reduce(new Text(expectedURI), values, context);
		
		EasyMock.verify(context, values);
	}

}

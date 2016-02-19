package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full;

import de.tudarmstadt.ukp.dkpro.c4corpus.deduplication.impl.ParallelDocumentDeDuplication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * De-duplication MR job for identifying IDs of documents to be removed from the collection.
 * The job splits the input documents into smaller chunks on which the local greedy algorithm
 * is performed. See {@link ParallelDocumentDeDuplication}
 *
 * (c) 2016 Ivan Habernal
 */
public class Phase3Step4LocalDeDuplication
        extends Configured
        implements Tool
{

    /**
     * Number of lines of input file to be processed by one mapper
     */
    private static final int LINES = 5000;

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        job.setJarByClass(Phase3Step4LocalDeDuplication.class);
        job.setJobName(Phase3Step4LocalDeDuplication.class.getName());

        // paths
        String inputPath = args[0];
        // text files of ids to be deleted
        String outputPath = args[1];

        // input: reading max N lines for each mapper
        job.setInputFormatClass(NLineInputFormat.class);
        NLineInputFormat.addInputPath(job, new Path(inputPath));
        job.getConfiguration().setInt("mapreduce.input.lineinputformat.linespermap", LINES);

        // mapper
        job.setMapperClass(LocalGreedyDeDuplicationMapper.class);

        LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);

        // reducer
        job.setReducerClass(IDCollectorReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase3Step4LocalDeDuplication(), args);
    }

    /**
     * "Local" greedy de-duplication mapper which processes N lines from the input tuples
     * (duplicate candidates)
     */
    public static class LocalGreedyDeDuplicationMapper
            extends Mapper<LongWritable, Text, Text, NullWritable>
    {
        private static final Log LOG = LogFactory.getLog(LocalGreedyDeDuplicationMapper.class);

        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException
        {
            // split value into lines
            List<String> lines = Arrays.asList(value.toString().split("\n"));

            Set<String> toDelete = ParallelDocumentDeDuplication.selectIDsToDelete(lines);

            LOG.info("Found " + toDelete.size() + " near-duplicate entries for deletion");

            for (String id : toDelete) {
                context.write(new Text(id), NullWritable.get());
            }
        }

    }

    /**
     * Only write the key to the output (the id to be deleted)
     */
    public static class IDCollectorReducer
            extends Reducer<Text, NullWritable, Text, NullWritable>
    {
        @Override protected void reduce(Text key, Iterable<NullWritable> values,
                Context context)
                throws IOException, InterruptedException
        {
            context.write(key, NullWritable.get());
        }
    }
}
package de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.full;

import com.google.common.collect.Lists;
import de.tudarmstadt.ukp.dkpro.c4corpus.deduplication.impl.ParallelDocumentDeDuplication;
import de.tudarmstadt.ukp.dkpro.c4corpus.hadoop.io.NonSplittableTextInputFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
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
 * (c) 2016 Ivan Habernal
 */
public class Phase3Step4GreedyClustering
        extends Configured
        implements Tool
{

    /**
     * Number of lines of input file to be processed by one mapper
     */
    private static final int CLUSTER_PARTITION_SIZE = 5000;

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        job.setJarByClass(Phase3Step4GreedyClustering.class);
        job.setJobName(Phase3Step4GreedyClustering.class.getName());

        // paths
        String inputPath = args[0];
        // text files of ids to be deleted
        String outputPath = args[1];

        // input: reading N lines for each mapper
        job.setInputFormatClass(NLineInputFormat.class);
//        job.setInputFormatClass(MultiLineInputFormat.class);
        NLineInputFormat.addInputPath(job, new Path(inputPath));
        job.getConfiguration()
                .setInt("mapreduce.input.lineinputformat.linespermap", CLUSTER_PARTITION_SIZE);

        // mapper
        job.setMapperClass(FileSplittingMapper.class);

        LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);

        // reducer - no need for one
        //        job.setReducerClass(IDCollectorReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Phase3Step4GreedyClustering(), args);
    }

    /**
     * "Local" greedy deduplication mapper which processes N lines from the input tuples
     * (duplicate candidates)
     */
    public static class FileSplittingMapper
            extends Mapper<LongWritable, Text, Text, NullWritable>
    {
        private static final Log LOG = LogFactory.getLog(FileSplittingMapper.class);

        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException
        {
            // split value into lines
            List<String> lines = Arrays.asList(value.toString().split("\n"));

            // now we split into chunks of max 1000 lines and process
//            List<List<String>> partitions = Lists.partition(lines, CLUSTER_PARTITION_SIZE);
//            LOG.info("Read " + lines.size() + " lines, splitting into " + partitions.size()
//                    + " partitions.");
//            for (int i = 0; i < partitions.size(); i++ ) {
//                List<String> partition = partitions.get(i);
//                LOG.info("Computing partition " + (i + 1) + "/" + partitions.size());
//                Set<String> toDelete = ParallelDocumentDeDuplication.selectIDsToDelete(partition);
                Set<String> toDelete = ParallelDocumentDeDuplication.selectIDsToDelete(lines);

                for (String id : toDelete) {
                    context.write(new Text(id), NullWritable.get());
                }
//            }
        }

    }

    /*
    public static class GreedyDeDuplicationReducer
            extends Reducer<Text, NullWritable, Text, NullWritable>
    {
        @Override
        protected void reduce(Text key, Iterable<NullWritable> values, Context context)
                throws IOException, InterruptedException
        {
            context.write(key, NullWritable.get());
        }
    }
    */
}
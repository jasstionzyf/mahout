/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.ga.watchmaker.cd.hadoop;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.mahout.utils.StringUtils;
import org.apache.mahout.ga.watchmaker.cd.CDFitness;
import org.apache.mahout.ga.watchmaker.cd.DataLine;
import org.apache.mahout.ga.watchmaker.cd.Rule;

/**
 * Hadoop Mapper. Evaluate all the rules with the input data line.  
 */
public class CDMapper extends MapReduceBase implements
    Mapper<LongWritable, Text, LongWritable, CDFitness> {

  public static final String CLASSDISCOVERY_RULES = "mahout.ga.classdiscovery.rules";

  private List<Rule> rules;

  @Override
  public void configure(JobConf job) {
    String rstr = job.get(CLASSDISCOVERY_RULES);
    if (rstr == null)
      throw new RuntimeException("Job Parameter (" + CLASSDISCOVERY_RULES
          + ") not found!");

    configure((List<Rule>) StringUtils.fromString(rstr));

    super.configure(job);
  }

  void configure(List<Rule> rules) {
    this.rules = rules;
  }

  public void map(LongWritable key, Text value,
      OutputCollector<LongWritable, CDFitness> output, Reporter reporter)
      throws IOException {
    DataLine dl = new DataLine(value.toString());
    
    map(key, dl, output);
  }

  void map(LongWritable key, DataLine dl,
      OutputCollector<LongWritable, CDFitness> output) throws IOException {
    for (int index = 0; index < rules.size(); index++) {
      CDFitness eval = evaluate(rules.get(index).classify(dl), dl.getLabel());
      output.collect(new LongWritable(index), eval);
    }
  }

  public static CDFitness evaluate(int prediction, int label) {
    // TODO for now we assume their are only two classes 0 and 1

    int tp = (label == 1 && prediction == 1) ? 1 : 0;
    int fp = (label == 0 && prediction == 1) ? 1 : 0;
    int tn = (label == 0 && prediction == 0) ? 1 : 0;
    int fn = (label == 1 && prediction == 0) ? 1 : 0;

    return new CDFitness(tp, fp, tn, fn);
  }
}

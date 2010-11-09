#!/usr/bin/env bash
set -euo pipefail
: "${HADOOP_STREAMING_JAR:?Set the Hadoop streaming jar path}"
: "${IMA_BATCH_ARCHIVE:?Set the zip containing ima_batch/}"
if [[ $# -ne 2 ]]; then
    printf '%s\n' 'Usage: hadoop-deduplicate.sh INPUT_HDFS OUTPUT_HDFS' >&2
    exit 2
fi
if [[ "$1" == "$2" ]]; then
    printf '%s\n' 'Input and output paths must differ.' >&2
    exit 2
fi
hadoop jar "$HADOOP_STREAMING_JAR" \
    -D mapreduce.job.name=ima-deduplicate \
    -D stream.num.map.output.key.fields=2 \
    -D mapreduce.partition.keypartitioner.options=-k1,2 \
    -partitioner org.apache.hadoop.mapred.lib.KeyFieldBasedPartitioner \
    -archives "$IMA_BATCH_ARCHIVE#ima_batch_bundle" \
    -cmdenv PYTHONPATH=ima_batch_bundle \
    -mapper 'python -m ima_batch.streaming map-deduplicate' \
    -reducer 'python -m ima_batch.streaming reduce-deduplicate' \
    -input "$1" \
    -output "$2"

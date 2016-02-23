/*
 * Pig script to compress a directory
 * input:   hdfs input directory to compress
 *          hdfs output directory
 *
 *
 */

set output.compression.enabled true;
set output.compression.codec org.apache.hadoop.io.compress.BZip2Codec;

%default TODAYS_DATE `date +%Y%m%d%H%M`;

--comma seperated list of hdfs directories to compress

--make sure path is not empty
input0 = LOAD '/data/twitch/chat/processing/chatdata*' USING PigStorage();

--single output directory
STORE input0 INTO '/data/twitch/chat/completed/$TODAYS_DATE' USING PigStorage();

fs -rm -f /data/twitch/chat/processing/chatdata*

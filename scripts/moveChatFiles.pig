/*
 * Pig script to move ready files to a directory
 * input:   hdfs input directory to move
 *          hdfs output directory
 *
 *
 */
fs -touchz /data/twitch/chat/input/chatdata-empty;
fs -rm -f /data/twitch/chat/processing/chatdata-empty;
fs -mv /data/twitch/chat/input/chatdata* /data/twitch/chat/processing/;

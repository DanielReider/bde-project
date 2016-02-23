REGISTER '/usr/jars/elephant-bird-pig.jar'
REGISTER '/usr/jars/elephant-bird-core.jar'
REGISTER '/usr/jars/elephant-bird-compat.jar'

%declare SEQFILE_LOADER 'com.twitter.elephantbird.pig.load.SequenceFileLoader';
%declare TEXT_CONVERTER 'com.twitter.elephantbird.pig.util.TextConverter';
%declare LONG_CONVERTER 'com.twitter.elephantbird.pig.util.LongWritableConverter';
%declare DATETIME `date +%Y-%m-%dT%H-%M-%S`


twitchdata = LOAD '/data/twitch/streammetadata/processing/' using PigStorage(';')
	AS (channel_id:long, viewers:long, game:chararray, name:chararray,
    status: chararray, created:chararray, mature:boolean,
    updated_at:chararray, fps:double);
twitchdata = FILTER twitchdata BY NOT (created MATCHES '.*T.*');
twitchdata = FOREACH twitchdata GENERATE channel_id, viewers, game, name, status, ToDate(created,'yyyy-MM-dd HH:mm:ss') as created, mature , fps;
twitchdata = FOREACH twitchdata GENERATE *, ToString(created,'yyyyMMddHHmm') as groupdate;

--ML data
ml_filterd = FILTER twitchdata BY fps is not null and viewers is not null and viewers > 500;
ml = FOREACH ml_filterd GENERATE name, game, viewers, created, fps;
ml_grouped = GROUP ml BY (name,game);
ml_flat = FOREACH ml_grouped GENERATE FLATTEN(group) as (name,game), AVG(ml.viewers) as viewers, AVG(ml.fps) as fps, MIN(ml.created) as starttime, MAX(ml.created) as endtime;
ml_flatdur = FOREACH ml_flat GENERATE name, game, SecondsBetween(endtime, starttime)/60.0 as dur:double,
(GetHour(starttime) < 6 ? 1 :
	(GetHour(starttime) < 12 ? 2 :
		(GetHour(starttime) < 15 ? 3 :
			(GetHour(starttime) < 18 ? 4 :
				(GetHour(starttime) < 22 ? 5 : 1))))) as daytime, fps, viewers, starttime;

ml_export = FOREACH ml_flatdur GENERATE REPLACE(name, ' |,', '_') as name, REPLACE(game, ' |,', '_') as game, dur, daytime, fps, viewers, starttime;
STORE ml_export INTO '/data/analysis/input/collecting/$DATETIME';
--ML Data end

viewerspermin = GROUP twitchdata BY (name, groupdate);
viewerspermin = FOREACH viewerspermin GENERATE FLATTEN(group) as (name:chararray, groupdate: chararray), AVG(twitchdata.viewers) as viewers:double;
viewerspermin = FOREACH viewerspermin GENERATE CONCAT(name, SUBSTRING(groupdate, 0, 8)) as joinkey, groupdate, viewers;

chatdata = LOAD '/data/twitch/chat/processing' USING $SEQFILE_LOADER ('-c $LONG_CONVERTER', '-c $TEXT_CONVERTER') AS (key:long, value:chararray);
chatdata = FOREACH chatdata GENERATE ToDate(key) as key, FLATTEN( (tuple(chararray,chararray,int)) STRSPLIT(value,',')) as (channel:chararray,msg:chararray, sentiment:int);
chatdata = FOREACH chatdata GENERATE *, ToString(key,'yyyyMMddHHmm') as groupdate, (sentiment==1 ? 1 : 0) as positiv:int, (sentiment==2 ? 1 : 0) as negativ:int, (sentiment==0 ? 1 : 0) as neutral:int;

chatspermin = GROUP chatdata BY (groupdate, channel);
chatspermin = FOREACH chatspermin GENERATE FLATTEN(group) as (groupdate:chararray, channel:chararray), SUM(chatdata.positiv) as positivcount:long, SUM(chatdata.negativ) as negativcount:long, SUM(chatdata.neutral) as neutralcount:long;
chatspermin = FOREACH chatspermin GENERATE CONCAT(channel, SUBSTRING(groupdate, 0, 8)) as joinkey, groupdate, positivcount, negativcount, neutralcount;

metadata = FOREACH twitchdata GENERATE name, game, status, mature, fps, created, CONCAT(name, ToString(created,'yyyyMMdd')) as joinkey;
metadata = GROUP metadata BY joinkey;
metadata = FOREACH metadata GENERATE FLATTEN(group) as joinkey, metadata.(joinkey, name, game, status, mature, fps, created);

grouped = COGROUP metadata by joinkey, viewerspermin by joinkey, chatspermin by joinkey;
grouped = FOREACH grouped GENERATE FLATTEN(group) as joinkey, ToString(CurrentTime(),'#HHmm') AS suffix, flatten(metadata.$1) as metadata, viewerspermin.(groupdate, viewers) as viewerspermin, chatspermin.(groupdate, positivcount,negativcount,neutralcount) as chatspermin;
grouped = FOREACH grouped GENERATE CONCAT(joinkey, suffix), metadata, viewerspermin, chatspermin;



STORE grouped INTO 'hbase://twitchdata' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage(
'general:metadata
 viewerspermin:viewerspermin
 chatpermin:chatspermin'
);

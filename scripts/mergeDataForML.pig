REGISTER ./hbase-server-1.0.0-cdh5.5.0.jar

%declare DATETIME `date +%Y-%m-%dT%H-%M-%S`

ml_data = LOAD '/data/analysis/input/collecting/' using PigStorage('\t')
	AS (name:chararray, game:chararray, dur:double, daytime:int, fps:double, viewers:double, starttime:chararray);
ml_data = FOREACH ml_data GENERATE name, game, dur, daytime, fps, viewers, ToDate(starttime) as starttime;

ml_grouped = GROUP ml_data BY (name,game);

ml_flat = FOREACH ml_grouped GENERATE FLATTEN(group) as (name,game),
  SUM(ml_data.dur) as dur,
  MIN(ml_data.daytime) as daytime,
  AVG(ml_data.fps) as fps,
  AVG(ml_data.viewers) as viewers,
  MIN(ml_data.starttime) as starttime;

ml_prep = FOREACH ml_flat GENERATE name, game, dur,
  (daytime == 1 ? 'Nachts' :
    (daytime == 2 ? 'Morgens' :
      (daytime == 3 ? 'Mittags' :
        (daytime == 4 ? 'Nachmittags' : 'Abends')))) as daytime,
  fps, ROUND(viewers/1000) as viewersclass, ToString(starttime,'yyyyMMddHH') as weatherkey;
ml_prep = FOREACH ml_prep GENERATE name, game, (chararray)dur as dur, daytime,(chararray)fps as fps ,(chararray)viewersclass as viewersclass, weatherkey;

weatherdata = LOAD 'hbase://weather' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('weather:*', '-loadKey true')
  AS (id:chararray, weather:map[]);

weather = FOREACH weatherdata GENERATE id as weatherkey, REPLACE(weather#'49525',',',' ') as w1:chararray, REPLACE(weather#'49479',',',' ') as w2:chararray, REPLACE(weather#'48369',',',' ') as w3:chararray, REPLACE(weather#'48149',',',' ') as w4:chararray;
weather_prep = FOREACH weather GENERATE weatherkey, CONCAT(w1,' ',w2,' ',w3,' ',w4) as weather;

joined_data = JOIN ml_prep BY weatherkey LEFT OUTER, weather_prep BY weatherkey;

ml = FOREACH joined_data GENERATE CONCAT('1,', ml_prep::name,' ', ml_prep::game,' ', ml_prep::dur,' ', ml_prep::daytime,' ', ml_prep::fps,' ', (weather_prep::weather is null? '' : weather_prep::weather),',',ml_prep::viewersclass) as value;
ml_filterdexport = FILTER ml BY value is not null;

STORE ml INTO '/data/analysis/input/merged/$DATETIME';

fs -mv /data/analysis/input/merged/$DATETIME/part-r* /data/analysis/input/completed/;
fs -rm -r -f /data/analysis/input/merged/$DATETIME;
fs -rm -r -f /data/analysis/input/collecting/*;

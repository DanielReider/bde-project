twitchData = LOAD 'hbase://twitchdata'
  USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('general:metadata, chatpermin:chatspermin, viewerspermin:viewerspermin', '-loadKey true')
  AS (id:chararray, general:chararray, chatData:chararray, viewersData:chararray);

test = foreach twitchData generate id, REPLACE(viewersData, '(\\{\\()|(\\)\\})','') as viewers, REPLACE(chatData, '(\\{\\()|(\\)\\})','') as chats;
test = foreach test generate id, REPLACE(viewers, '(\\{)|(\\})','') as viewers, REPLACE(chats, '(\\{)|(\\})','') as chats;

t = FOREACH test GENERATE id, STRSPLIT(viewers, '\\),\\(') as viewers, STRSPLIT(chats, '\\),\\(') as chats;
b = FOREACH t GENERATE id, FLATTEN(chats) as chats, FLATTEN(viewers) as viewers;
c = FOREACH b GENERATE id, STRSPLIT(viewers, ',') as viewers:(time:chararray,view:chararray), STRSPLIT(chats, ',') as chats:(time:chararray,pos:chararray,neg:chararray,neut:chararray);
c = FOREACH c GENERATE id, viewers.(view) as view:chararray, chats.(pos) as pos:chararray, chats.(neg) as neg:chararray, chats.(neut) as neut:chararray;
c = foreach c generate id, (double)view as view, (double)pos as pos, (double)neg as neg, (double)neut as neut;
d = GROUP c BY id;
e = foreach d generate group as id, AVG(c.view), SUM(c.pos), SUM(c.neg), SUM(c.neut);
illustrate e

weatherData = LOAD 'hbase://weather' USING org.apache.pig.backend.hadoop.hbase.HBaseStorage('weather:*')AS (weather:chararray);

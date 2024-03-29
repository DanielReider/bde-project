package de.fhmuenster.bde.twitchChatPull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.flume.Context;
import org.apache.flume.CounterGroup;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

public class IrcSource extends AbstractSource implements EventDrivenSource, Configurable {

	private static final Logger logger = LoggerFactory.getLogger(IrcSource.class);

	private static final int DEFAULT_PORT = 6667;
	private static final String IRC_CHANNEL_PREFIX = "#";

	private static final String API_KEY = "ba118ae867eb8c66d7cc76d6a32eb3ab4c341b04";
	private static final String API_URL = "http://gateway-a.watsonplatform.net/calls/text/TextGetTextSentiment";

	private IRCConnection connection = null;
	static ChannelProcessor mChannel = null;

	private String hostname;
	private Integer port;
	private String nick;
	private String password;
	private String user;
	private String name;
	private String chan;

	private CounterGroup counterGroup;
	BlockingQueue<Event> q = new LinkedBlockingQueue<Event>();

	private static Charset mCharset;

	static public class IRCConnectionListener implements IRCEventListener {

		public void onRegistered() {
		}

		public void onDisconnected() {
			logger.error("IRC source disconnected");
		}

		public void onError(String msg) {
			logger.error("IRC source error: {}", msg);
		}

		public void onError(int num, String msg) {
			logger.error("IRC source error: {} - {}", num, msg);
		}

		public void onInvite(String chan, IRCUser u, String nickPass) {
			logger.info(chan + "> " + u.getNick() + " invites " + nickPass);
		}

		public void onJoin(String chan, IRCUser u) {
			logger.info(chan + "> " + u.getNick() + " joins");
		}

		public void onKick(String chan, IRCUser u, String nickPass, String msg) {
			logger.info(chan + "> " + u.getNick() + " kicks " + nickPass);
		}

		public void onMode(IRCUser u, String nickPass, String mode) {
			logger.info("Mode: " + u.getNick() + " sets modes " + mode + " " + nickPass);
		}

		public void onMode(String chan, IRCUser u, IRCModeParser mp) {
			logger.info(chan + "> " + u.getNick() + " sets mode: " + mp.getLine());
		}

		public void onNick(IRCUser u, String nickNew) {
			logger.info("Nick: " + u.getNick() + " is now known as " + nickNew);
		}

		public void onNotice(String target, IRCUser u, String msg) {
			logger.info(target + "> " + u.getNick() + " (notice): " + msg);
		}

		public void onPart(String chan, IRCUser u, String msg) {
			logger.info(chan + "> " + u.getNick() + " parts");
		}

		public void onPrivmsg(String chan, IRCUser u, String msg) {

			try {
				append(chan, u.getNick(), msg);
			} catch (IOException e) {
				logger.info("Fail to create the event:" + e.getMessage());
			}
		}

		public void onQuit(IRCUser u, String msg) {
			logger.info("Quit: " + u.getNick());
		}

		public void onReply(int num, String value, String msg) {
			logger.info("Reply #" + num + ": " + value + " " + msg);
		}

		public void onTopic(String chan, IRCUser u, String topic) {
			logger.info(chan + "> " + u.getNick() + " changes topic into: " + topic);
		}

		public void onPing(String p) {

		}

		public void unknown(String a, String b, String c, String d) {
			logger.info("UNKNOWN: " + a + " b " + c + " " + d);
		}
	}

	private static void append(String chan, String user, String msg) throws IOException {
		if (msg == null) {
			logger.error("null append!");
			return;
		}
		if (mChannel != null) {
			String charset = "UTF-8";
			String query = String.format("apikey=%s&text=%s&outputMode=%s", URLEncoder.encode(API_KEY, charset),
					URLEncoder.encode(msg, charset), URLEncoder.encode("json", charset));
			StringBuilder jsonResults = new StringBuilder();
			URLConnection connection = new URL(API_URL + "?" + query).openConnection();
			connection.setRequestProperty("Accept-Charset", charset);
			InputStreamReader in = new InputStreamReader(connection.getInputStream());
			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
			String eventString = msg.replace(",", ";");
			try {
				// Create a JSON object hierarchy from the results
				JSONObject jsonObj = new JSONObject(jsonResults.toString());
				Integer sentiment = 0;
				if (!jsonObj.has("docSentiment")) {
					sentiment = 0;
				} else {
					switch (jsonObj.getJSONObject("docSentiment").getString("type")) {
					case "neutral":
						sentiment = 0;
						break;
					case "positive":
						sentiment = 1;
						break;
					case "negative":
						sentiment = 2;
						break;
					}
				}
				eventString = chan.replace("#", "") + "," + eventString + "," + sentiment.toString();
				System.out.println(eventString);
				Event event = EventBuilder.withBody(eventString, mCharset);
				mChannel.processEvent(event);
			} catch (JSONException e) {
				logger.info("Cannot process JSON results:" + e.getMessage());
			}
		}
	}

	public IrcSource() {
		counterGroup = new CounterGroup();
	}

	public void configure(Context context) {
		hostname = context.getString("hostname");
		String portStr = context.getString("port");
		nick = context.getString("nick");
		password = context.getString("password");
		user = context.getString("user");
		name = context.getString("name");
		chan = context.getString("chan");

		if (portStr != null) {
			port = Integer.parseInt(portStr);
		} else {
			port = DEFAULT_PORT;
		}

		Preconditions.checkState(hostname != null, "No hostname specified");
		Preconditions.checkState(nick != null, "No nick specified");
		Preconditions.checkState(chan != null, "No chan specified");
	}

	private void createConnection() throws IOException {
		if (connection == null) {
			logger.debug("Creating new connection to hostname:{} port:{}", hostname, port);
			connection = new IRCConnection(hostname, new int[] { port }, password, nick, user, name);
			connection.addIRCEventListener(new IRCConnectionListener());
			connection.setEncoding("UTF-8");
			connection.setPong(true);
			connection.setDaemon(false);
			connection.setColors(false);
			connection.connect();
			connection.send("join " + IRC_CHANNEL_PREFIX + chan);
		}
	}

	private void destroyConnection() {
		if (connection != null) {
			logger.debug("Destroying connection to: {}:{}", hostname, port);
			connection.close();
		}

		connection = null;
	}

	@Override
	public void start() {
		logger.info("IRC source starting");
		mChannel = getChannelProcessor();
		mCharset = Charset.forName("UTF-8");

		try {
			createConnection();
		} catch (Exception e) {
			logger.error(
					"Unable to create irc client using hostname:" + hostname + " port:" + port + ". Exception follows.",
					e);
			destroyConnection();
			return;
		}

		super.start();

		logger.debug("IRC source {} started", this.getName());
	}

	@Override
	public void stop() {
		logger.info("IRC source {} stopping", this.getName());

		destroyConnection();

		super.stop();

		logger.debug("IRC source {} stopped. Metrics:{}", this.getName(), counterGroup);
	}
}

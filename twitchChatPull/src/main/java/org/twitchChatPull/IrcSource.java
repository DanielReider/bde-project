/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.twitchChatPull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.schwering.irc.lib.IRCConfig;
import org.schwering.irc.lib.IRCConfigBuilder;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCConnectionFactory;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCUser;
import org.schwering.irc.lib.util.IRCModeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private String proxyHost;
  private Integer proxyPort;
  
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
			String query = String.format("apikey=%s&text=%s&outputMode=%s",
					URLEncoder.encode(API_KEY, charset),
	                URLEncoder.encode(msg, charset),
	                URLEncoder.encode("json", charset));
			StringBuilder jsonResults = new StringBuilder();
	        URLConnection urlconnection = new URL(API_URL + "?" + query).openConnection();
	        urlconnection.setRequestProperty("Accept-Charset", charset);
	        InputStreamReader in = new InputStreamReader(urlconnection.getInputStream());
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
	            if (jsonObj.getJSONObject("docSentiment") == null){
	            	sentiment = 0;
	            } else {
	            	switch(jsonObj.getJSONObject("docSentiment").getString("type")){
			            case "neutral": sentiment = 0;
			            break;
			            case "positive": sentiment = 1;
			            break;
			            case "negative": sentiment = 2;
			            break;
		            }	
	            }
	            eventString = chan.replace("#", "") + "," + eventString + "," + sentiment.toString();
	            logger.info(eventString);
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
    	logger.debug(
    	"Creating new connection to hostname:{} port:{}",
	      hostname, port);
    	IRCConfig config = IRCConfigBuilder.newBuilder()
	      .host(hostname)
	      .port(port)
	      .nick(nick)
	      .password(password)
	      .encoding("UTF-8")
	      .autoPong(true)
	      .stripColors(false)
	      .socksProxy(proxyHost, proxyPort)
	      .build();
    	
    	logger.info("config erstellt " + config.getHost() + ","+ config.getNick() );
    	IRCConnection connection = IRCConnectionFactory.newConnection(config,config);
    	logger.info("Connection erstellt");
    	connection.addIRCEventListener(new IRCConnectionListener());
    	logger.info("IRCListener erstellt");
	
		try {
			connection.connect();
			logger.info("Connected");
			connection.doJoin(IRC_CHANNEL_PREFIX + chan);
			logger.info("Join");
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 
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
		Enumeration<NetworkInterface> e = NetworkInterface
				.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			NetworkInterface n = (NetworkInterface) e.nextElement();
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				if (i.getHostAddress().toString().equals("10.60.64.45")) {
					System.out.println("Setting proxy");
					proxyHost = "10.60.17.102";
					proxyPort = 7300;
				}
			}
		}
		createConnection();
    } catch (Exception e) {
      logger.error("Unable to create irc client using hostname:"
          + hostname + " port:" + port + ". Exception follows.", e);
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
package data_collector_ver4;

import java.io.IOException;
import java.io.InputStream;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.api.services.youtube.YouTube.Search;

public class MySQLAccess {
	private Connection connect = null;
	private Statement statement = null;
	private ResultSet resultSet = null;

	private String host;
	private String port;
	private String dbname;
	private String user;
	private String passwd;

	private final String PROPERTIES_FILENAME = "MySQL.properties";
	private Properties properties = new Properties();

	public MySQLAccess() {
		// In the constructor, load the properties of the server setting.
		try {
			InputStream in = Search.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
			properties.load(in);
		} catch (IOException e) {
			System.err.println(
					"There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause() + " : " + e.getMessage());
			System.exit(1);
		}
		host = properties.getProperty("host");
		port = properties.getProperty("port");
		dbname = properties.getProperty("dbname");
		user = properties.getProperty("user");
		passwd = properties.getProperty("passwd");
	}

	private void establishConnection() throws SQLException {
		// This will load the MySQL driver, each DB has its own driver
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// Setup the connection with the DB
		connect = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbname + "?" + "user=" + user
				+ "&password=" + passwd + "&character_set_server=utf8mb4" + "&rewriteBatchedStatements=true");

	}

	// channel table insertion.
	private void writeChannelToDataBase(ArrayList<JSONObject> channelTableList) throws Exception {

		// The table list contains many entities of channels. They need to be
		// inserted one by one.
		String query = "INSERT INTO Channel (ChannelId, ChannelPublishedAt, ChannelTitle, ChannelDescription) "
				+ "VALUE (?, ?, ?, ?) ON DUPLICATE KEY UPDATE ChannelId=ChannelId";
		PreparedStatement preparedStatement = connect.prepareStatement(query);

		for (JSONObject channelTable : channelTableList) {
			// Scratch the information from stored JSON object.
			String channelId = channelTable.getString("ChannelId");
			Timestamp channelPublishedAt = stringToTimestamp(channelTable.getString("ChannelPublishedAt"));
			String channelTitle = channelTable.getString("ChannelTitle");
			String channelDescription = channelTable.getString("ChannelDescription");

			// Setup the query string.

			try {
				// Pass the values into the statement.
				preparedStatement.setString(1, channelId);
				preparedStatement.setTimestamp(2, channelPublishedAt);
				preparedStatement.setString(3, channelTitle);
				preparedStatement.setString(4, channelDescription);

				preparedStatement.addBatch();
			} catch (java.sql.SQLException e) {
				// // The most common error that occurs is caused by the
				// encoding
				// // format. There're Emojis and some languages that cannot be
				// // accepted by the database, which are mostly contained in
				// the
				// // channel title or the channel description. When this error
				// // happens, locate where it comes from (title or
				// description),
				// // and try to avoid updating that column (or both)
				// information.
				// if (e.getMessage().contains("ChannelTitle") &&
				// !e.getMessage().contains("ChannelDescription")) {
				// preparedStatement.setString(1, channelId);
				// preparedStatement.setTimestamp(2, channelPublishedAt);
				// preparedStatement.setString(3, "");
				// preparedStatement.setString(4, channelDescription);
				// } else if (!e.getMessage().contains("ChannelTitle") &&
				// e.getMessage().contains("ChannelDescription")) {
				// preparedStatement.setString(1, channelId);
				// preparedStatement.setTimestamp(2, channelPublishedAt);
				// preparedStatement.setString(3, channelTitle);
				// preparedStatement.setString(4, "");
				// } else if (e.getMessage().contains("ChannelTitle") &&
				// e.getMessage().contains("ChannelDescription")) {
				// preparedStatement.setString(1, channelId);
				// preparedStatement.setTimestamp(2, channelPublishedAt);
				// preparedStatement.setString(3, "");
				// preparedStatement.setString(4, "");
				// } else {
				// System.out.println(e.getMessage());
				// }
				// try {
				// preparedStatement.executeUpdate();
				// } catch (SQLException e1) {
				// System.out.println(e1.getMessage());
				// }
			}
		}

		try {
			preparedStatement.executeBatch();
		} catch (BatchUpdateException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			int[] counts = e.getUpdateCounts();
			int successCount = 0;
			int notAvaliable = 0;
			int failCount = 0;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] >= 0) {
					successCount++;
				} else if (counts[i] == Statement.SUCCESS_NO_INFO) {
					notAvaliable++;
				} else if (counts[i] == Statement.EXECUTE_FAILED) {
					failCount++;
				}
			}
			System.out.println("---Channel---");
			System.out.println("Number of affected rows: " + successCount);
			System.out.println("Number of affected rows (not avaliable): " + notAvaliable);
			System.out.println("Failed count in batch: " + failCount);
		}
	}

	// video category insertion.
	private void writeVideoCategoryToDatabase(ArrayList<JSONObject> videoCategoryTableList)
			throws SQLException, IOException {

		// Setup the query string.
		String query = "INSERT INTO VideoCategory (CategoryId, CategoryTitle) "
				+ "VALUE (?, ?) ON DUPLICATE KEY UPDATE CategoryId=CategoryId";
		PreparedStatement preparedStatement = connect.prepareStatement(query);

		for (JSONObject videoCategoryTable : videoCategoryTableList) {
			// Scratch the information from stored JSON object.
			String categoryId = videoCategoryTable.getString("CategoryId");
			String CategoryTitle = videoCategoryTable.getString("CategoryTitle");

			try {
				// Pass the values into the statement.
				preparedStatement.setString(1, categoryId);
				preparedStatement.setString(2, CategoryTitle);

				preparedStatement.addBatch();
			} catch (SQLException e) {
				// Do nothing.
			}
		}

		try {
			preparedStatement.executeBatch();
		} catch (BatchUpdateException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			int[] counts = e.getUpdateCounts();
			int successCount = 0;
			int notAvaliable = 0;
			int failCount = 0;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] >= 0) {
					successCount++;
				} else if (counts[i] == Statement.SUCCESS_NO_INFO) {
					notAvaliable++;
				} else if (counts[i] == Statement.EXECUTE_FAILED) {
					failCount++;
				}
			}
			System.out.println("---Video category---");
			System.out.println("Number of affected rows: " + successCount);
			System.out.println("Number of affected rows (not avaliable): " + notAvaliable);
			System.out.println("Failed count in batch: " + failCount);
		}

	}

	// video table insertion.
	private void writeVideoToDatabase(ArrayList<JSONObject> videoTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Setup the query string.
		String query = "INSERT INTO Video (VideoId, CategoryId, ChannelId, VideoPublishedAt, Duration, VideoTitle, VideoDescription) "
				+ "VALUE (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE VideoId=VideoId";
		PreparedStatement preparedStatement = connect.prepareStatement(query);

		for (JSONObject videoTable : videoTableList) {
			// Scratch the information from stored JSON object.
			String videoId = videoTable.getString("VideoId");
			String CategoryId = videoTable.getString("CategoryId");
			String ChannelId = videoTable.getString("ChannelId");
			Timestamp VideoPublishedAt = stringToTimestamp(videoTable.getString("VideoPublishedAt"));
			String Duration = videoTable.getString("Duration");
			String VideoTitle = videoTable.getString("VideoTitle");
			String VideoDescription = videoTable.getString("VideoDescription");

			try {
				// Pass the values into the statement.
				preparedStatement.setString(1, videoId);
				preparedStatement.setString(2, CategoryId);
				preparedStatement.setString(3, ChannelId);
				preparedStatement.setTimestamp(4, VideoPublishedAt);
				preparedStatement.setString(5, Duration);
				preparedStatement.setString(6, VideoTitle);
				preparedStatement.setString(7, VideoDescription);

				preparedStatement.addBatch();

			} catch (SQLException e) {
				// // In the video info insertion step, the most common error is
				// // caused by the video description. If it happens, identify
				// it
				// // and avoid inserting that column.
				// if (e.getMessage().contains("VideoDescription")) {
				// preparedStatement.setString(1, videoId);
				// preparedStatement.setString(2, CategoryId);
				// preparedStatement.setString(3, ChannelId);
				// preparedStatement.setTimestamp(4, VideoPublishedAt);
				// preparedStatement.setString(5, Duration);
				// preparedStatement.setString(6, VideoTitle);
				// preparedStatement.setString(7, "");
				//
				// preparedStatement.addBatch();
				// } else {
				// // Do nothing.
				// }
			}
		}

		try {
			preparedStatement.executeBatch();
		} catch (BatchUpdateException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			int[] counts = e.getUpdateCounts();
			int successCount = 0;
			int notAvaliable = 0;
			int failCount = 0;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] >= 0) {
					successCount++;
				} else if (counts[i] == Statement.SUCCESS_NO_INFO) {
					notAvaliable++;
				} else if (counts[i] == Statement.EXECUTE_FAILED) {
					failCount++;
				}
			}
			System.out.println("---Video---");
			System.out.println("Number of affected rows: " + successCount);
			System.out.println("Number of affected rows (not avaliable): " + notAvaliable);
			System.out.println("Failed count in batch: " + failCount);
		}
	}

	// video statistic insertion.
	private void writeVideoStatisticToDatabase(ArrayList<JSONObject> videoStatisticTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Setup the query string.
		String query = "INSERT INTO VideoStatistic (VideoId, VideoTimeStamp, VideoCommentsCount, VideoDislikeCount, VideoFavoriteCount, VideoLikeCount, VideoViewCount) "
				+ "VALUE (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE VideoId=VideoId";

		PreparedStatement preparedStatement = connect.prepareStatement(query);

		for (JSONObject videoStatisticTable : videoStatisticTableList) {
			// Scratch the information from stored JSON object.
			String videoId = videoStatisticTable.getString("VideoId");
			Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.parse(videoStatisticTable.getString("VideoTimeStamp"));
			Timestamp videoTimeStamp = new java.sql.Timestamp(date.getTime());
			long videoFavoriteCount = videoStatisticTable.getLong("VideoFavoriteCount");
			long videoViewCount = videoStatisticTable.getLong("VideoViewCount");
			// Some videos may not allow these information. By default, set them
			// to 0.
			long videoLikeCount = 0;
			long videoDislikeCount = 0;
			long videoCommentsCount = 0;
			// If there's no information in such videos, there will be an JSON
			// exception when I call the non-exist key.
			try {
				videoLikeCount = videoStatisticTable.getLong("VideoLikeCount");
				videoDislikeCount = videoStatisticTable.getLong("VideoDislikeCount");
				videoCommentsCount = videoStatisticTable.getLong("VideoCommentsCount");
			} catch (Exception e) {
				// The variables remain the default values which are all 0.
			}

			try {
				// Pass the values into the statement.
				preparedStatement.setString(1, videoId);
				preparedStatement.setTimestamp(2, videoTimeStamp);
				preparedStatement.setLong(3, videoCommentsCount);
				preparedStatement.setLong(4, videoDislikeCount);
				preparedStatement.setLong(5, videoFavoriteCount);
				preparedStatement.setLong(6, videoLikeCount);
				preparedStatement.setLong(7, videoViewCount);

				preparedStatement.addBatch();
			} catch (SQLException e) {
				// Do nothing.
			}
		}

		try {
			preparedStatement.executeBatch();
		} catch (BatchUpdateException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			int[] counts = e.getUpdateCounts();
			int successCount = 0;
			int notAvaliable = 0;
			int failCount = 0;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] >= 0) {
					successCount++;
				} else if (counts[i] == Statement.SUCCESS_NO_INFO) {
					notAvaliable++;
				} else if (counts[i] == Statement.EXECUTE_FAILED) {
					failCount++;
				}
			}
			System.out.println("---Video statistics---");
			System.out.println("Number of affected rows: " + successCount);
			System.out.println("Number of affected rows (not avaliable): " + notAvaliable);
			System.out.println("Failed count in batch: " + failCount);
		}
	}

	// channel statistic insertion.
	private void writeChannelStatisticToDatebase(ArrayList<JSONObject> channelStatisticTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Setup the query string.
		String query = "INSERT INTO ChannelStatistic (ChannelId, ChannelTimeStamp, ChannelCommentCount, ChannelSubscriberCount, ChannelVideoCount, ChannelViewCount) "
				+ "VALUE (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE ChannelId=ChannelId";

		PreparedStatement preparedStatement = connect.prepareStatement(query);

		for (JSONObject channelStatisticTable : channelStatisticTableList) {
			// Scratch the information from stored JSON object.
			String channelId = channelStatisticTable.getString("ChannelId");
			Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.parse(channelStatisticTable.getString("ChannelTimeStamp"));
			Timestamp channelTimeStamp = new java.sql.Timestamp(date.getTime());
			long channelCommentCount = channelStatisticTable.getLong("ChannelCommentCount");
			long channelSubscriberCount = channelStatisticTable.getLong("ChannelSubscriberCount");
			long channelVideoCount = channelStatisticTable.getLong("ChannelVideoCount");
			long channelViewCount = channelStatisticTable.getLong("ChannelViewCount");

			try {
				// Pass the values into the statement.
				preparedStatement.setString(1, channelId);
				preparedStatement.setTimestamp(2, channelTimeStamp);
				preparedStatement.setLong(3, channelCommentCount);
				preparedStatement.setLong(4, channelSubscriberCount);
				preparedStatement.setLong(5, channelVideoCount);
				preparedStatement.setLong(6, channelViewCount);

				preparedStatement.addBatch();
			} catch (SQLException e) {
				// Do nothing.
			}
		}

		try {
			preparedStatement.executeBatch();
		} catch (BatchUpdateException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			int[] counts = e.getUpdateCounts();
			int successCount = 0;
			int notAvaliable = 0;
			int failCount = 0;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] >= 0) {
					successCount++;
				} else if (counts[i] == Statement.SUCCESS_NO_INFO) {
					notAvaliable++;
				} else if (counts[i] == Statement.EXECUTE_FAILED) {
					failCount++;
				}
			}
			System.out.println("---Channel statistics---");
			System.out.println("Number of affected rows: " + successCount);
			System.out.println("Number of affected rows (not avaliable): " + notAvaliable);
			System.out.println("Failed count in batch: " + failCount);
		}
	}

	// top level comment insertion.
	private void writeTopLevelCommentToDatebase(ArrayList<JSONObject> topLevelCommentTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Setup the query string.
		String query = "INSERT INTO TopLevelComment (TLCommentId, VideoId, ChannelId, TLCommentLikeCount, TLCommentPublishedAt, TLCommentUpdatedAt, TLCommentTextDisplay, TotalReplyCount) "
				+ "VALUE (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE TLCOmmentId=TLCOmmentId";

		PreparedStatement preparedStatement = connect.prepareStatement(query);

		for (JSONObject topLevelCommentTable : topLevelCommentTableList) {
			// Scratch the information from stored JSON object.
			String tlcommentId = topLevelCommentTable.getString("TLCommentId");
			String videoId = topLevelCommentTable.getString("VideoId");
			String channelId = topLevelCommentTable.getString("ChannelId");
			long tlcommentLikeCount = topLevelCommentTable.getLong("TLCommentLikeCount");
			Timestamp TLCommentPublishedAt = stringToTimestamp(topLevelCommentTable.getString("TLCommentPublishedAt"));
			Timestamp TLCommentUpdatedAt = stringToTimestamp(topLevelCommentTable.getString("TLCommentUpdatedAt"));
			String TLCommentTextDisplay = topLevelCommentTable.getString("TLCommentTextDisplay");
			long TotalReplyCount = topLevelCommentTable.getLong("TotalReplyCount");

			try {
				// Pass the values into the statement.
				preparedStatement.setString(1, tlcommentId);
				preparedStatement.setString(2, videoId);
				preparedStatement.setString(3, channelId);
				preparedStatement.setLong(4, tlcommentLikeCount);
				preparedStatement.setTimestamp(5, TLCommentPublishedAt);
				preparedStatement.setTimestamp(6, TLCommentUpdatedAt);
				preparedStatement.setString(7, TLCommentTextDisplay);
				preparedStatement.setLong(8, TotalReplyCount);

				preparedStatement.addBatch();
			} catch (SQLException e) {
				// // As before, the encoding incompatible may cause errors. If
				// it
				// // occurs, avoid updating the column that possibly causes the
				// // error (here the column is TLCommentTextDisplay).
				// if (e.getMessage().contains("TLCommentTextDisplay")) {
				// preparedStatement.setString(1, tlcommentId);
				// preparedStatement.setString(2, videoId);
				// preparedStatement.setString(3, channelId);
				// preparedStatement.setInt(4, tlcommentLikeCount);
				// preparedStatement.setTimestamp(5, TLCommentPublishedAt);
				// preparedStatement.setTimestamp(6, TLCommentUpdatedAt);
				// preparedStatement.setString(7, "");
				// preparedStatement.setInt(8, TotalReplyCount);
				//
				// preparedStatement.addBatch();
				// } else {
				// // Do not add this comment.
				// }
			}
		}

		try {
			preparedStatement.executeBatch();
		} catch (BatchUpdateException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			int[] counts = e.getUpdateCounts();
			int successCount = 0;
			int notAvaliable = 0;
			int failCount = 0;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] >= 0) {
					successCount++;
				} else if (counts[i] == Statement.SUCCESS_NO_INFO) {
					notAvaliable++;
				} else if (counts[i] == Statement.EXECUTE_FAILED) {
					failCount++;
				}
			}
			System.out.println("---Top level comment---");
			System.out.println("Number of affected rows: " + successCount);
			System.out.println("Number of affected rows (not avaliable): " + notAvaliable);
			System.out.println("Failed count in batch: " + failCount);
		}
	}

	private void writeReplyToDatabase(ArrayList<JSONObject> replyTableList)
			throws SQLException, JSONException, ParseException, IOException {

		// Setup the query string.
		String query = "INSERT INTO Reply (ReplyId, TLCOmmentId, ChannelId, ReplyLikeCount, ReplyPublishedAt, ReplyUpdatedAt, ReplyTextDisplay) "
				+ "VALUE (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE TLCOmmentId=TLCOmmentId";

		PreparedStatement preparedStatement = connect.prepareStatement(query);

		for (JSONObject replyTable : replyTableList) {
			// Scratch the information from stored JSON object.
			String replyId = replyTable.getString("ReplyId");
			String tlcommentId = replyTable.getString("TLCommentId");
			String channelId = replyTable.getString("ChannelId");
			long replyLikeCount = replyTable.getLong("ReplyLikeCount");
			Timestamp replyPublishedAt = stringToTimestamp(replyTable.getString("ReplyPublishedAt"));
			Timestamp replyUpdatedAt = stringToTimestamp(replyTable.getString("ReplyUpdatedAt"));
			String replyTextDisplay = replyTable.getString("ReplyTextDisplay");

			try {
				// Pass the values into the statement.
				preparedStatement.setString(1, replyId);
				preparedStatement.setString(2, tlcommentId);
				preparedStatement.setString(3, channelId);
				preparedStatement.setLong(4, replyLikeCount);
				preparedStatement.setTimestamp(5, replyPublishedAt);
				preparedStatement.setTimestamp(6, replyUpdatedAt);
				preparedStatement.setString(7, replyTextDisplay);

				preparedStatement.addBatch();
			} catch (SQLException e) {
				// // Similarly, avoid updating column "ReplyTextDisplay" if it
				// // results in an encoding incompatible error.
				// if (e.getMessage().contains("ReplyTextDisplay")) {
				// preparedStatement.setString(1, replyId);
				// preparedStatement.setString(2, tlcommentId);
				// preparedStatement.setString(3, channelId);
				// preparedStatement.setInt(4, replyLikeCount);
				// preparedStatement.setTimestamp(5, replyPublishedAt);
				// preparedStatement.setTimestamp(6, replyUpdatedAt);
				// preparedStatement.setString(7, "");
				//
				// preparedStatement.addBatch();
				// } else {
				// System.out.println(e.toString());
				// }
			}
		}

		try {
			preparedStatement.executeBatch();
		} catch (BatchUpdateException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			int[] counts = e.getUpdateCounts();
			int successCount = 0;
			int notAvaliable = 0;
			int failCount = 0;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] >= 0) {
					successCount++;
				} else if (counts[i] == Statement.SUCCESS_NO_INFO) {
					notAvaliable++;
				} else if (counts[i] == Statement.EXECUTE_FAILED) {
					failCount++;
				}
			}
			System.out.println("---Reply---");
			System.out.println("Number of affected rows: " + successCount);
			System.out.println("Number of affected rows (not avaliable): " + notAvaliable);
			System.out.println("Failed count in batch: " + failCount);
		}
	}

	// A method that combine and organize the insertions.
	public void writeToDatabase(ArrayList<JSONObject> channelTableList, ArrayList<JSONObject> channelStatisticTableList,
			ArrayList<JSONObject> videoCategoryTableList, ArrayList<JSONObject> videoTableList,
			ArrayList<JSONObject> videoStatisticTableList, ArrayList<JSONObject> topLevelCommentTableList,
			ArrayList<JSONObject> replyTableList) throws Exception {

		establishConnection();
		System.out.println("-------------------------------------------------");
		System.out.println("----------------DATABASE INSERTING---------------");
		writeChannelToDataBase(channelTableList);
		System.out.println("--Channel updated.");
		writeChannelStatisticToDatebase(channelStatisticTableList);
		System.out.println("--Channel Statistic updated.");
		writeVideoCategoryToDatabase(videoCategoryTableList);
		System.out.println("--Category updated.");
		writeVideoToDatabase(videoTableList);
		System.out.println("--Video updated.");
		writeVideoStatisticToDatabase(videoStatisticTableList);
		System.out.println("--Video Statistic updated.");
		writeTopLevelCommentToDatebase(topLevelCommentTableList);
		System.out.println("--TLComment updated.");
		writeReplyToDatabase(replyTableList);
		System.out.println("--Reply updated.");
		System.out.println("----------------INSERTION COMPLETE---------------");
		System.out.println("-------------------------------------------------");
		close();

		Thread.sleep(2000);

	}

	// You need to close the resultSet
	private void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}

	private Timestamp stringToTimestamp(String timeString) throws ParseException {
		DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		Date result = df1.parse(timeString.replace("Z", ""));
		java.sql.Timestamp ts = new java.sql.Timestamp(result.getTime());
		return ts;
	}

	// This method gets a set of video IDs by accessing the database.
	public LinkedHashSet<String> readVideoIdList(int limit) throws Exception {

		establishConnection();

		LinkedHashSet<String> retrievedVideoId = new LinkedHashSet<String>();

		// Query strings:
		String selectQuery = "select VideoId " + "from VideoIdRecord " + "order by CrawledTime asc " + "limit ?";
		String updateQuery = "update VideoIdRecord " + "set CrawledTime = CrawledTime + 1 " + "where VideoId = ?";

		PreparedStatement selectStatement = connect.prepareStatement(selectQuery);
		PreparedStatement updateStatement = connect.prepareStatement(updateQuery);
		try {
			selectStatement.setInt(1, limit);

			resultSet = selectStatement.executeQuery();

			while (resultSet.next()) {
				String videoId = resultSet.getString("VideoId");
				retrievedVideoId.add(videoId);

				updateStatement.setString(1, videoId);
				updateStatement.executeUpdate();
			}
		} catch (Exception e) {
			// TODO: handle exception
			throw e;
		} finally {
			close();
		}
		return retrievedVideoId;
	}

	// This method is used for creating a big video ID base list, which will be
	// used for further crawling.
	public void videoIDListCreator(LinkedHashSet<String> videoIdSet) throws SQLException {

		establishConnection();
		// ** probably can be done with LOAD DATA INFILE, which is much faster
		// than insertion.

		// ** currently using plain INSERT
		Iterator<String> videoIdItor = videoIdSet.iterator();
		//
		String query = "INSERT INTO VideoIdRecord (VideoId, CrawledTime)" + "VALUE (?, ?)"
				+ "ON DUPLICATE KEY UPDATE VideoId=VideoId";

		PreparedStatement preparedStatement = connect.prepareStatement(query);

		int count = 0;
		while (videoIdItor.hasNext()) {
			String videoId = videoIdItor.next();
			count++;
			if (count % 1000 == 0) {
				System.out.println(String.format("d", (count / 1000)) + " collected.");
			}

			try {
				preparedStatement.setString(1, videoId);
				preparedStatement.setInt(2, 0);
				preparedStatement.addBatch();

			} catch (SQLException e) {
				// TODO: handle exception
				throw e;
			}
		}

		try {
			int[] updateCount = preparedStatement.executeBatch();
			System.out.println("inserted " + updateCount.length);
		} catch (BatchUpdateException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			int[] counts = e.getUpdateCounts();
			int successCount = 0;
			int notAvaliable = 0;
			int failCount = 0;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] >= 0) {
					successCount++;
				} else if (counts[i] == Statement.SUCCESS_NO_INFO) {
					notAvaliable++;
				} else if (counts[i] == Statement.EXECUTE_FAILED) {
					failCount++;
				}
			}
			System.out.println("Number of affected rows: " + successCount);
			System.out.println("Number of affected rows (not avaliable): " + notAvaliable);
			System.out.println("Failed count in batch: " + failCount);
		}

		close();
		System.out.print("\nID record updated.");

	}

}

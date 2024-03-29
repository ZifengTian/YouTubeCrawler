package data_collector_ver3;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoCategory;
import com.google.api.services.youtube.model.VideoCategoryListResponse;
import com.google.api.services.youtube.model.VideoListResponse;

public class TestApi {
	private YouTube youtube;
	private String apiKey;
	private long noChannelUserCount;

	// // Help passing channel id between methods.
	// private LinkedHashSet<String> channelIdSet = new LinkedHashSet<String>();
	// private StringBuilder channelIdBuilder = new StringBuilder();
	// // Help passing category id between methods.
	// private LinkedHashSet<String> categoryIdSet = new
	// LinkedHashSet<String>();
	// private StringBuilder categoryIdBuilder = new StringBuilder();

	public TestApi(YouTube youtube, String apiKey) {
		this.youtube = youtube;
		this.apiKey = apiKey;
	}

	// YouTube data API only accept less than 50 length's CSV string, so a
	// splitter is needed.
	private ArrayList<String> csvSplitter(String csvString) {
		ArrayList<String> splittedCSVString = new ArrayList<String>();
		int numberPerChunk = 5;
		String str = new String();
		int position = ordinalIndexOf(csvString, ",", numberPerChunk - 1);
		while (position != -1) {
			str = csvString.substring(0, position);
			csvString = csvString.substring(position + 1);
			splittedCSVString.add(str);
			position = ordinalIndexOf(csvString, ",", numberPerChunk - 1);
		}
		splittedCSVString.add(csvString);
		return splittedCSVString;
	}

	private int ordinalIndexOf(String string, String subString, int index) {
		int position = string.indexOf(subString, 0);
		while (index-- > 0 && position != -1) {
			position = string.indexOf(subString, position + 1);
		}
		return position;
	}

	private String hashSetToCSV(LinkedHashSet<String> idSet) {
		StringBuilder idStringBuilder = new StringBuilder();
		Iterator<String> setIterator = idSet.iterator();
		while (setIterator.hasNext()) {
			idStringBuilder.append(setIterator.next() + ",");
		}
		return idStringBuilder.toString().replaceAll(",$", "");
	}

	/**
	 * 
	 * @param videoIdListCSV
	 * @return videoTableList
	 * @throws IOException
	 */
	public ArrayList<JSONObject> videoTableList(LinkedHashSet<String> videoIdSet, LinkedHashSet<String> channelIdSet,
			LinkedHashSet<String> categoryIdSet) throws IOException {
		ArrayList<JSONObject> videoTableList = new ArrayList<JSONObject>();

		YouTube.Videos.List videoList = youtube.videos().list("id,snippet,contentDetails").setKey(apiKey).setFields(
				"items(id,contentDetails/duration,snippet(categoryId,channelId,description,publishedAt,title))");

		String videoIdListCSV = hashSetToCSV(videoIdSet);
		ArrayList<String> splittedVideoIdListCSV = csvSplitter(videoIdListCSV);
		Iterator<String> videoIdIterator = splittedVideoIdListCSV.iterator();

		while (videoIdIterator.hasNext()) {

			videoList.setId(videoIdIterator.next());
			VideoListResponse videoListResponse = videoList.execute();

			java.util.List<Video> videos = videoListResponse.getItems();
			Iterator<Video> videoIterator = videos.iterator();

			while (videoIterator.hasNext()) {
				Video video = videoIterator.next();
				JSONObject videoInfoTable = new JSONObject().put("VideoId", video.getId())
						.put("CategoryId", video.getSnippet().getCategoryId())
						.put("ChannelId", video.getSnippet().getChannelId())
						.put("VideoPublishedAt", video.getSnippet().getPublishedAt().toString())
						.put("Duration", video.getContentDetails().getDuration())
						.put("VideoTitle", video.getSnippet().getTitle())
						.put("VideoDescription", video.getSnippet().getDescription());
				videoTableList.add(videoInfoTable);

				channelIdSet.add(video.getSnippet().getChannelId());
				categoryIdSet.add(video.getSnippet().getCategoryId());
			}
		}
		return videoTableList;
	}

	public ArrayList<JSONObject> videoStatisticTableList(LinkedHashSet<String> videoIdSet) throws IOException {

		ArrayList<JSONObject> videoStatisticTableList = new ArrayList<JSONObject>();

		YouTube.Videos.List videoList = youtube.videos().list("id,statistics").setKey(apiKey)
				.setFields("items(id,statistics)");

		String videoIdListCSV = hashSetToCSV(videoIdSet);
		ArrayList<String> splittedVideoIdListCSV = csvSplitter(videoIdListCSV);
		Iterator<String> videoIdIterator = splittedVideoIdListCSV.iterator();

		while (videoIdIterator.hasNext()) {

			videoList.setId(videoIdIterator.next());
			VideoListResponse videoListResponse = videoList.execute();

			java.util.List<Video> videos = videoListResponse.getItems();
			Iterator<Video> videoIterator = videos.iterator();

			while (videoIterator.hasNext()) {
				Video video = videoIterator.next();
				JSONObject videoStatisticTable = new JSONObject().put("VideoId", video.getId())
						.put("VideoTimeStamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
						.put("VideoCommentsCount", video.getStatistics().getCommentCount())
						.put("VideoDislikeCount", video.getStatistics().getDislikeCount())
						.put("VideoLikeCount", video.getStatistics().getLikeCount())
						.put("VideoFavoriteCount", video.getStatistics().getFavoriteCount())
						.put("VideoViewCount", video.getStatistics().getViewCount());

				videoStatisticTableList.add(videoStatisticTable);
			}
		}

		return videoStatisticTableList;
	}

	public ArrayList<JSONObject> videoCategoryTableList(LinkedHashSet<String> categoryIdSet) throws IOException {

		StringBuilder categoryIdBuilder = new StringBuilder();
		ArrayList<JSONObject> videoCategoryTableList = new ArrayList<JSONObject>();

		YouTube.VideoCategories.List videoCategories = youtube.videoCategories().list("snippet").setKey(apiKey)
				.setFields("items(id,snippet/title)");

		Iterator<String> categoryIdSetIterator = categoryIdSet.iterator();
		while (categoryIdSetIterator.hasNext()) {
			categoryIdBuilder.append(categoryIdSetIterator.next() + ",");
		}
		String categoryIdCSV = categoryIdBuilder.toString().replaceAll(",$", "");

		ArrayList<String> splittedCategoryIdCSV = csvSplitter(categoryIdCSV);
		Iterator<String> categoryIdIterator = splittedCategoryIdCSV.iterator();

		while (categoryIdIterator.hasNext()) {
			videoCategories.setId(categoryIdIterator.next());
			VideoCategoryListResponse videoCategoryListResponse = videoCategories.execute();

			List<VideoCategory> videoCategoryList = videoCategoryListResponse.getItems();
			Iterator<VideoCategory> videoCategoryIterator = videoCategoryList.iterator();
			while (videoCategoryIterator.hasNext()) {
				VideoCategory videoCategory = videoCategoryIterator.next();
				JSONObject videoCategoryTable = new JSONObject().put("CategoryId", videoCategory.getId())
						.put("CategoryTitle", videoCategory.getSnippet().getTitle());
				videoCategoryTableList.add(videoCategoryTable);
			}
		}

		return videoCategoryTableList;
	}

	public ArrayList<JSONObject>[] videoCommentTableList(LinkedHashSet<String> videoIdSet,
			LinkedHashSet<String> channelIdSet) throws IOException, InterruptedException {
		String videoIdListCSV = hashSetToCSV(videoIdSet);
		String[] videoIdList = videoIdListCSV.split(",");
		// 0: top level comment; 1: reply.
		@SuppressWarnings("unchecked")
		ArrayList<JSONObject>[] videoCommentTableList = (ArrayList<JSONObject>[]) new ArrayList[2];
		for (int i = 0; i < videoCommentTableList.length; i++) {
			videoCommentTableList[i] = new ArrayList<JSONObject>();
		}
		for (String videoId : videoIdList) {
			String curVideoId = videoId;
			Comment curComment = new Comment();

			try {
				YouTube.CommentThreads.List videoCommentsList = youtube.commentThreads().list("snippet,replies")
						.setKey(apiKey).setVideoId(videoId).setTextFormat("plainText").setMaxResults((long) 100)
						.setFields(
								"items(replies(comments(id,snippet(authorChannelId,likeCount,parentId,publishedAt,textDisplay,updatedAt))),"
										+ "snippet(topLevelComment(id,snippet(authorChannelId,likeCount,publishedAt,textDisplay,updatedAt)),"
										+ "totalReplyCount,videoId)),nextPageToken");
				CommentThreadListResponse videoCommentsListResponse = videoCommentsList.execute();
				List<CommentThread> commentThreadList = videoCommentsListResponse.getItems();
				// Collect every pages.
				while (videoCommentsListResponse.getNextPageToken() != null) {
					videoCommentsListResponse = videoCommentsList
							.setPageToken(videoCommentsListResponse.getNextPageToken()).execute();
					commentThreadList.addAll(videoCommentsListResponse.getItems());
				}

				// Start iterator.
				Iterator<CommentThread> iterComment = commentThreadList.iterator();
				while (iterComment.hasNext()) {
					CommentThread videoComment = iterComment.next();
					Comment topLevelComment = videoComment.getSnippet().getTopLevelComment();
					curComment = topLevelComment;

					// avoid null author IDs.
					if (!topLevelComment.getSnippet().getAuthorChannelId().toString().isEmpty()) {

						JSONObject topLevelCommentTable = new JSONObject().put("TLCommentId", topLevelComment.getId())
								.put("VideoId", videoComment.getSnippet().getVideoId())
								.put("ChannelId",
										authorChannelIdFormat(
												topLevelComment.getSnippet().getAuthorChannelId().toString()))
								.put("TLCommentLikeCount", topLevelComment.getSnippet().getLikeCount())
								.put("TLCommentPublishedAt", topLevelComment.getSnippet().getPublishedAt().toString())
								.put("TLCommentUpdatedAt", topLevelComment.getSnippet().getUpdatedAt().toString())
								.put("TLCommentTextDisplay", topLevelComment.getSnippet().getTextDisplay())
								.put("TotalReplyCount", videoComment.getSnippet().getTotalReplyCount());

						videoCommentTableList[0].add(topLevelCommentTable);

						channelIdSet.add(
								authorChannelIdFormat(topLevelComment.getSnippet().getAuthorChannelId().toString()));

						// If reply exists, add them as well.
						if (videoComment.getSnippet().getTotalReplyCount() != 0) {
							List<Comment> replies = videoComment.getReplies().getComments();
							Iterator<Comment> iterReply = replies.iterator();
							while (iterReply.hasNext()) {
								Comment reply = iterReply.next();
								if (!reply.getSnippet().getAuthorChannelId().toString().isEmpty()) {
									JSONObject replyTable = new JSONObject().put("ReplyId", reply.getId())
											.put("TLCommentId", reply.getSnippet().getParentId())
											.put("ChannelId",
													authorChannelIdFormat(
															reply.getSnippet().getAuthorChannelId().toString()))
											.put("ReplyLikeCount", reply.getSnippet().getLikeCount())
											.put("ReplyPublishedAt", reply.getSnippet().getPublishedAt().toString())
											.put("ReplyUpdatedAt", reply.getSnippet().getUpdatedAt().toString())
											.put("ReplyTextDisplay", reply.getSnippet().getTextDisplay());

									videoCommentTableList[1].add(replyTable);

									// Save the author's channel id to
									// channelIdList.
									channelIdSet.add(
											authorChannelIdFormat(reply.getSnippet().getAuthorChannelId().toString()));
								}
							}
						}

					} else {
						String str = topLevelComment.getSnippet().getAuthorDisplayName();
						String googleplus = topLevelComment.getSnippet().getAuthorGoogleplusProfileUrl();
						System.out.println("The author \"" + str + "\"'s channel ID not found."
								+ "\n\tThe google+ url is: " + googleplus);
					}
				}
			} catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
				if (e.getStatusCode() != 403) {
					if (e.getStatusCode() == 404) {
						System.out.println("**No video specified.**");
					} else if (e.getStatusCode() == 400) {
						System.out.println("Problem exists in video: " + curVideoId);
						Thread.sleep(5000);
					} else if (e.getStatusCode() == 500) {
						Thread.sleep(5000);
					} else {
						throw e;
					}
				}
			} catch (NullPointerException e) {
				// TODO: handle exception
				if (!curComment.containsKey("authorChannelId")) {
					System.out.println("Author doesn't have channel ID." + "==> Total: " + ++noChannelUserCount);
				} else {
					throw e;
				}
			}
		}
		return videoCommentTableList;

	}

	public ArrayList<JSONObject> channelTableList(LinkedHashSet<String> channelIdSet) throws IOException {
		ArrayList<JSONObject> channelTableList = new ArrayList<JSONObject>();
		StringBuilder channelIdBuilder = new StringBuilder();

		Iterator<String> channelSetIterator = channelIdSet.iterator();
		while (channelSetIterator.hasNext()) {
			channelIdBuilder.append(channelSetIterator.next() + ",");
		}

		String channelIdCSV = channelIdBuilder.toString().replaceAll(",$", "");

		YouTube.Channels.List channels = youtube.channels().list("id,snippet").setKey(apiKey)
				.setFields("items(id,snippet(country,description,publishedAt,title))");

		ArrayList<String> splittedChannelIdCSV = csvSplitter(channelIdCSV);
		Iterator<String> channelIdIterator = splittedChannelIdCSV.iterator();

		while (channelIdIterator.hasNext()) {
			channels.setId(channelIdIterator.next());
			ChannelListResponse channelListResponse = channels.execute();

			List<Channel> channelList = channelListResponse.getItems();
			Iterator<Channel> channelIterator = channelList.iterator();
			while (channelIterator.hasNext()) {
				Channel channel = channelIterator.next();
				JSONObject channelTable = new JSONObject().put("ChannelId", channel.getId())
						.put("ChannelPublishedAt", channel.getSnippet().getPublishedAt().toString())
						.put("ChannelTitle", channel.getSnippet().getTitle())
						.put("ChannelDescription", channel.getSnippet().getDescription());
				channelTableList.add(channelTable);
			}
		}
		return channelTableList;
	}

	public ArrayList<JSONObject> channelStatisticTableList(LinkedHashSet<String> channelIdSet) throws IOException {
		ArrayList<JSONObject> channelStatisticTableList = new ArrayList<JSONObject>();
		StringBuilder channelIdBuilder = new StringBuilder();

		Iterator<String> channelSetIterator = channelIdSet.iterator();
		while (channelSetIterator.hasNext()) {
			channelIdBuilder.append(channelSetIterator.next() + ",");
		}

		String channelIdCSV = channelIdBuilder.toString().replaceAll(",$", "");

		YouTube.Channels.List channels = youtube.channels().list("id,statistics").setKey(apiKey)
				.setFields("items(id,statistics)");

		ArrayList<String> splittedChannelIdCSV = csvSplitter(channelIdCSV);
		Iterator<String> channelIdIterator = splittedChannelIdCSV.iterator();

		while (channelIdIterator.hasNext()) {
			channels.setId(channelIdIterator.next());
			ChannelListResponse channelListResponse = channels.execute();

			List<Channel> channelList = channelListResponse.getItems();
			Iterator<Channel> channelIterator = channelList.iterator();
			while (channelIterator.hasNext()) {
				Channel channel = channelIterator.next();
				JSONObject channelStatisticTable = new JSONObject().put("ChannelId", channel.getId())
						.put("ChannelTimeStamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
						.put("ChannelCommentCount", channel.getStatistics().getCommentCount())
						.put("ChannelSubscriberCount", channel.getStatistics().getSubscriberCount())
						.put("ChannelVideoCount", channel.getStatistics().getVideoCount())
						.put("ChannelViewCount", channel.getStatistics().getViewCount());
				channelStatisticTableList.add(channelStatisticTable);
			}
		}
		return channelStatisticTableList;
	}

	private String authorChannelIdFormat(String originalString) {
		return originalString.split("=")[1].replace("}", "");
	}
}
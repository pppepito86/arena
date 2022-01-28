package com.olimpiici.arena.grader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.pesho.grader.SubmissionScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olimpiici.arena.service.SubmissionService;

public class Worker {

    private final Logger log = LoggerFactory.getLogger(Worker.class);

	private ObjectMapper mapper = new ObjectMapper();

	private final String workDir;

	private String url;
	private boolean isFree = true;

	public Worker(String url, String workDir) {
		if (!url.startsWith("http"))
			url = "http://" + url;
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
		this.url = url;

		this.workDir = workDir;
	}

	public synchronized SubmissionScore grade(long problemId, long submissionId,
			GraderTask listener, boolean isAuthor) throws Exception {

		if (!isProblemUploaded(problemId)) {
			log.debug("Problem is not uloaded. Uploading...");
			File problemZip = getProblemFile(problemId);
			sendProblemToWorker(problemId, problemZip.getAbsolutePath());
		}

		File sourceFile = getSourceFile(submissionId);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", sourceFile, ContentType.TEXT_PLAIN, sourceFile.getName())
                .addTextBody("metadata", "{\"problemId\":" + problemId + "}", ContentType.APPLICATION_JSON)
                .build();

        // if it's author solution run with a big TL
        String authorParam = isAuthor ? "?tl=20" : "";
		HttpPost post = new HttpPost(url + "/api/v1/submissions/" + submissionId + authorParam);
		post.setEntity(entity);

		CloseableHttpClient httpclient = HttpClients.createDefault();
		httpclient.execute(post);
		httpclient.close();

		for (int i = 0; i < 600; i++) {
			SubmissionScore score = getScore(submissionId);
			listener.updateScore(submissionId, score);

			if (score.isFinished()) return score;
			Thread.sleep(2000);
		}
		throw new IllegalStateException("time out");
	}

	private File getProblemFile(long problemId) {
		return Paths.get(workDir, "problems", ""+problemId, "problem.zip").toFile();
	}

	private File getSourceFile(long submissionId) {
		return SubmissionService.findSubmissionFile(workDir, submissionId).get();
	}

	private SubmissionScore getScore(long submissionId) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet get = new HttpGet(url + "/api/v1/submissions/" + submissionId + "/score");
		CloseableHttpResponse response = httpclient.execute(get);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				log.warn("The worker returned status code {} when checking score" +
						" for submission {}. Reason {} ", statusCode, submissionId,
						response.getStatusLine().getReasonPhrase());
			}
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				log.warn("The worker returned null entity for submission {}",
						statusCode, submissionId);
				SubmissionScore score = new SubmissionScore();
				score.addFinalScore("system error", 0);
				return score;
			}
			String responseString = EntityUtils.toString(entity);
			SubmissionScore score = mapper.readValue(responseString, SubmissionScore.class);

			log.debug("Response for " + submissionId + " is: " + response.getStatusLine() + ", points are: " + score.getScore());
			return score;
		} finally {
			response.close();
			httpclient.close();
		}
	}

	public boolean isAlive() {
		RequestConfig config = RequestConfig.custom()
				  .setConnectTimeout(3000)
				  .setConnectionRequestTimeout(3000)
				  .setSocketTimeout(3000).build();
		try (CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
			HttpGet httpGet = new HttpGet(url + "/api/v1/health-check");
			CloseableHttpResponse response = httpclient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == HttpStatus.OK.value()) {
				String responseString = EntityUtils.toString(response.getEntity());
				return responseString.equals("ok");
			}
			return false;
		} catch (Exception e) {
			log.warn("Cannot connect to worker " + url, e);
			return false;
		}
	}

	private boolean isProblemUploaded(long problemId) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet get = new HttpGet(url + "/api/v1/problems/" + problemId);
		CloseableHttpResponse response = httpclient.execute(get);
		httpclient.close();
		return response.getStatusLine().getStatusCode() == 200;
	}

	private void sendProblemToWorker(long problemId, String zipFilePath) throws IOException {
		File problemFile = new File(zipFilePath);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", problemFile, ContentType.TEXT_PLAIN, problemFile.getName())
                .build();

		HttpPost post = new HttpPost(url + "/api/v1/problems/" + problemId);
		post.setEntity(entity);

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			httpclient.execute(post);
		}

	}

	public synchronized void deleteProblem(long problemId) throws IOException {
		HttpDelete delete = new HttpDelete(url + "/api/v1/problems/" + problemId);

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			httpclient.execute(delete);
		}
	}

	public void setFree(boolean isFree) {
		this.isFree = isFree;
	}

	public boolean isFree() {
		return isFree;
	}

	public String getUrl() {
		return url;
	}

}

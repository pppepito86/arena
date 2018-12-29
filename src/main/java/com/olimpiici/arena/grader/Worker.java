package com.olimpiici.arena.grader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

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

	public SubmissionScore grade(long problemId, long submissionId)
			throws Exception {
		
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
		
		HttpPost post = new HttpPost(url + "/api/v1/submissions/" + submissionId);
		post.setEntity(entity);

		CloseableHttpClient httpclient = HttpClients.createDefault();
		httpclient.execute(post);
		httpclient.close();
		
		for (int i = 0; i < 600; i++) {
			if (isRunning(submissionId)) Thread.sleep(1000);
			else return getScore(submissionId);
		}
		throw new IllegalStateException("time out");
	}

	private File getProblemFile(long problemId) {
		return Paths.get(workDir, "problems", ""+problemId, "problem.zip").toFile();
	}
	
	private File getSourceFile(long submissionId) {
		return Paths.get(workDir, "submissions", ""+submissionId, "solution.cpp").toFile();
	}
	
	private boolean isRunning(long submissionId) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet get = new HttpGet(url + "/api/v1/submissions/" + submissionId + "/status");
		CloseableHttpResponse response = httpclient.execute(get);
		String responseString = EntityUtils.toString(response.getEntity());
		httpclient.close();
		return "running".equals(responseString);
	}
	
	private SubmissionScore getScore(long submissionId) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet get = new HttpGet(url + "/api/v1/submissions/" + submissionId + "/score");
		CloseableHttpResponse response = httpclient.execute(get);
		String responseString = EntityUtils.toString(response.getEntity());
		httpclient.close();
		SubmissionScore score = mapper.readValue(responseString, SubmissionScore.class);
		
		log.debug("Response for " + submissionId + " is: " + response.getStatusLine() + ", points are: " + score.getScore());
		return score;
	}
	
	public boolean isAlive() {
		RequestConfig config = RequestConfig.custom()
				  .setConnectTimeout(1000)
				  .setConnectionRequestTimeout(1000)
				  .setSocketTimeout(1000).build();
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
	
	public boolean isProblemUploaded(long problemId) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet get = new HttpGet(url + "/api/v1/problems/" + problemId);
		CloseableHttpResponse response = httpclient.execute(get);
		httpclient.close();
		return response.getStatusLine().getStatusCode() == 200;
	}

	public void sendProblemToWorker(long problemId, String zipFilePath) {
		RestTemplate rest = new RestTemplate();
		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.add("file", new FileSystemResource(zipFilePath));

		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "multipart/form-data");

		String endpointURL = url + "/api/v1/problems/" + problemId;
		boolean exists;
		try {
			exists = HttpStatus.OK == rest.getForEntity(endpointURL, String.class).getStatusCode();
		} catch (HttpClientErrorException e) {
			exists = false;
		}

		System.out.println("problem exists last check");
		org.springframework.http.HttpEntity<MultiValueMap<String, Object>> params = new org.springframework.http.HttpEntity<MultiValueMap<String, Object>>(parameters,
				headers);
		if (exists) {
			rest.put(endpointURL, params);
		} else {
			rest.postForLocation(endpointURL, params);
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

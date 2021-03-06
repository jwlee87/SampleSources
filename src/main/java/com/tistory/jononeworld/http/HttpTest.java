package com.tistory.jononeworld.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpTest {
	
	private static final String DEFAULT_ENCODING = "UTF-8";

	private String url;
	private MultipartEntityBuilder params;

	/**
	 * @param url 접속할 url
	 */
	public HttpTest(String url){
		this.url = url;

		params = MultipartEntityBuilder.create();
	}
	/**
	 * Map 으로 한꺼번에 파라메터 훅 추가하는 메소드
	 * @param param 파라메터들이 담긴 맵, 파라메터들은 UTF-8로 인코딩 됨
	 * @return
	 */
	public HttpTest addParam(Map<String, Object> param){
		return addParam(param, DEFAULT_ENCODING);
	}

	/**
	 * Map 으로 한꺼번에 파라메터 훅 추가하는 메소드
	 * @param param 파라메터들이 담긴 맵
	 * @param encoding 파라메터 encoding charset
	 * @return
	 */
	public HttpTest addParam(Map<String, Object> param, String encoding){
		for( Map.Entry<String, Object> e : param.entrySet() ){
			if (e.getValue() instanceof File) {
				addParam(e.getKey(), (File)e.getValue(), encoding);
			}else{
				addParam(e.getKey(), (String)e.getValue(), encoding);
			}
		}
		return this;
	}

	/**
	 * 문자열 파라메터를 추가한다.
	 * @param name 추가할 파라메터 이름
	 * @param value 파라메터 값
	 * @return
	 */
	public HttpTest addParam(String name, String value){
		return addParam(name, value, DEFAULT_ENCODING);
	}

	public HttpTest addParam(String name, String value, String encoding){
		params.addPart(name, new StringBody(value, ContentType.create("text/plain", encoding)));
		return this;
	}

	/**
	 * 업로드할 파일 파라메터를 추가한다.
	 * @param name
	 * @param file
	 * @return
	 */
	public HttpTest addParam(String name, File file){
		return addParam(name, file, DEFAULT_ENCODING);
	}

	public HttpTest addParam(String name, File file, String encoding){
		if( file.exists() ){
			try{
				params.addPart(
						name,
						new FileBody(file, ContentType.create("application/octet-stream"),
						URLEncoder.encode(file.getName(), encoding)));
			}catch( Exception ex ){ ex.printStackTrace(); }

		}
		
		return this;
	}
 
	/**
	 * 타겟 URL 로 POST 요청을 보낸다.
	 * @return 요청결과
	 * @throws Exception
	 */
	public String submit() throws Exception{
		CloseableHttpClient http = HttpClients.createDefault();
		StringBuffer result = new StringBuffer();

		try{
			HttpPost post = new HttpPost(url);
			post.setEntity(params.build());

			CloseableHttpResponse response = http.execute(post);

			try{
				HttpEntity res = response.getEntity();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(res.getContent(), Charset.forName("UTF-8")));

				String buffer = null;
				while( (buffer=br.readLine())!=null ){
					result.append(buffer).append("\r\n");
				}
			}finally{
				response.close();
			}
		}finally{
			http.close();
		}
		return result.toString();
	}

	/**
	 * 테스트
	 * @param args
	 *@throws Exception
	*/
	public static void main(String[] args) {
		HttpTest http = new HttpTest("http://127.0.0.1:8888/receiver.jsp");

		try {
			http.addParam("test", "문자열 파라메터 테스트다!")
				.addParam("upload_file1", new File("d:\\첨부파일1.hwp"))
				.addParam("upload_file2", new File("d:\\첨부파일2.jpg"))
				.submit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getHttpPostData(String url, Map<String,String> paramMap) {
		String responseBody = "";
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			Builder builder = RequestConfig.custom();
			builder.setConnectTimeout(4000);
			builder.setSocketTimeout(4000);
			builder.setStaleConnectionCheckEnabled(false);
			RequestConfig config = builder.build();

			try {
				HttpPost httpPost = new HttpPost(url);
				ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
				for (Map.Entry<String, String> entry: paramMap.entrySet()) {
					postParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
				}
				httpPost.setEntity(new UrlEncodedFormEntity(postParams));
				httpPost.setConfig(config);

				System.out.println("url : " + url);
				System.out.println("Executing request " + httpPost.getRequestLine());
	 
				// Create a custom response handler
				ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
					@Override
					public String handleResponse(
						final HttpResponse response) throws ClientProtocolException, IOException {
						int status = response.getStatusLine().getStatusCode();
						if (status >= 200 && status < 300) {
							HttpEntity entity = response.getEntity();
							String responseStr = "";
							if( entity != null ) {
								responseStr = EntityUtils.toString(entity);
							}
							return responseStr;
						} else {
							throw new ClientProtocolException("Unexpected response status: " + status);
						}
					}
				};
				responseBody = httpclient.execute(httpPost, responseHandler);

				System.out.println("----------------------------------------");
				System.out.println(responseBody);

			} finally {
				httpclient.close();
			}

		} catch(Exception e) {
			e.printStackTrace();
		}
		return responseBody;
	}

}

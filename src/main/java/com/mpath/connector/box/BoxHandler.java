package com.mpath.connector.box;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFolder;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mpath.connector.GsonTransformer;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static spark.Spark.*;

public class BoxHandler {
  /** Your Box Client ID */
  private static String CLIENT_ID = "YOUR_CLIENT_ID";
  /** Your Box Client Secret */
  private static String CLIENT_SECRET = "YOUR_CLIENT_SECRET";
  /** We will provide a redirect url for you to set. */
  private static String REDIRECT_URL = "https://studio.mpath.com/oauth";

  /**
   * Base OAuth url for box as shown in doc
   * @see <a href="https://developers.box.com/oauth/">Box OAuth 2.0</a>}
   */
  private static String OAUTH_BASE_URL = "https://www.box.com/api/oauth2";
  private static String AUTH_HEADER = "Authorization";

  private static JsonParser JSON_PARSER = new JsonParser();

  public static void main(String[] args) {
    HttpClient client = HttpClientBuilder.create()
        .setConnectionManager(new PoolingHttpClientConnectionManager())
        .build();

    before((req, res) -> {
      if (!req.headers().contains(AUTH_HEADER)) {
        halt(401, "An Authorization header is required to access this API");
      }
    });

    post("/token", (req, res) -> {
      HttpPost post = new HttpPost(new URI(OAUTH_BASE_URL + "/token"));

      List<NameValuePair> params = ImmutableList.of(
          new BasicNameValuePair("grant_type", "authorization_code"),
          new BasicNameValuePair("code", req.headers(AUTH_HEADER)),
          new BasicNameValuePair("client_id", CLIENT_ID),
          new BasicNameValuePair("client_secret", CLIENT_SECRET),
          new BasicNameValuePair("redirect_uri", REDIRECT_URL)
      );
      post.setEntity(new UrlEncodedFormEntity(params, Charsets.UTF_8));

      HttpResponse response = client.execute(post);
      res.status(response.getStatusLine().getStatusCode());

      return stringFromResponse(response);
    });

    post("/refresh", (req, res) -> {
      JsonObject body = JSON_PARSER.parse(req.body()).getAsJsonObject();
      String refreshToken = getAsString(body, "refreshToken");

      BoxAPIConnection connection =
          new BoxAPIConnection(CLIENT_ID, CLIENT_SECRET, req.headers(AUTH_HEADER), refreshToken);
      connection.refresh();

      return ImmutableMap.of(
          "accessToken", connection.getAccessToken(),
          "refreshToken", connection.getRefreshToken()
      );
    }, new GsonTransformer());

    post("/revoke", (req, res) -> {
      JsonObject body = JSON_PARSER.parse(req.body()).getAsJsonObject();
      HttpPost post = new HttpPost(new URI(OAUTH_BASE_URL + "/revoke"));

      List<NameValuePair> params = ImmutableList.of(
          new BasicNameValuePair("client_id", CLIENT_ID),
          new BasicNameValuePair("client_secret", CLIENT_SECRET),
          new BasicNameValuePair("token", getAsString(body, "token"))
      );
      post.setEntity(new UrlEncodedFormEntity(params, Charsets.UTF_8));

      HttpResponse response = client.execute(post);
      res.status(response.getStatusLine().getStatusCode());
      return stringFromResponse(response);
    });

    get("/folders/:folderId", (req, res) -> {
      BoxAPIConnection api = new BoxAPIConnection(CLIENT_ID, CLIENT_SECRET, req.headers(AUTH_HEADER));
      BoxFolder folder = new BoxFolder(api, req.params(":folderId"));

      return folder.getInfo();
    }, new GsonTransformer());
  }

  /** Convenience method to return a string from an object */
  private static String getAsString(JsonObject object, String key) {
    Preconditions.checkState(object.has(key),
        String.format("%s is missing from body", key));
    return object.get(key).getAsString();
  }

  /** Convenience function to transform Response to String */
  private static String stringFromResponse(HttpResponse response) throws IOException {
    return new String(ByteStreams.toByteArray(response.getEntity().getContent()), Charsets.UTF_8);
  }
}

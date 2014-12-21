package edu.sjsu.cmpe.cache.client;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

/**
 * Created by tusharkarkera on 12/20/14.
 */
public interface CRDTCallbackInterface {
    void putCompleted (HttpResponse<JsonNode> response, String serverUrl);
    void getCompleted (HttpResponse<JsonNode> response, String serverUrl);

    void putFailed (Exception e);
    void getFailed (Exception e);
}

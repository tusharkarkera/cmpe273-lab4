package edu.sjsu.cmpe.cache.client;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by tusharkarkera on 12/20/14.
 */

public class CRDTClient implements CRDTCallbackInterface {

    private ConcurrentHashMap<String, CacheServiceInterface> servers;
    private ArrayList<String> runningServers;
    private ConcurrentHashMap<String, ArrayList<String>> results;
    CacheServiceInterface cacheNode1 = new DistributedCacheService("http://localhost:3000", this);
    CacheServiceInterface cacheNode2 = new DistributedCacheService("http://localhost:3001", this);
    CacheServiceInterface cacheNode3 = new DistributedCacheService("http://localhost:3002", this);

    private static CountDownLatch countDownLatch;

    public CRDTClient() {

        servers = new ConcurrentHashMap<String, CacheServiceInterface>(3);
        servers.put("http://localhost:3000", cacheNode1);
        servers.put("http://localhost:3001", cacheNode2);
        servers.put("http://localhost:3002", cacheNode3);
    }

    @Override
    public void putCompleted(HttpResponse<JsonNode> response, String serverUrl) {
        int code = response.getStatus();
        System.out.println("PUT successful ServerResponse: " + code + " from server " + serverUrl);
        runningServers.add(serverUrl);
        countDownLatch.countDown();
    }

    @Override
    public void putFailed(Exception e) {
        System.out.println("PUT request failed");
        countDownLatch.countDown();
    }


    @Override
    public void getCompleted(HttpResponse<JsonNode> response, String serverUrl) {

        String value = null;
        if (response != null && response.getStatus() == 200) {
            value = response.getBody().getObject().getString("value");
            System.out.println("GET value form " + serverUrl + " is " + value);
            ArrayList serversWithValue = results.get(value);
            if (serversWithValue == null) {
                serversWithValue = new ArrayList(3);
            }
            serversWithValue.add(serverUrl);
            results.put(value, serversWithValue);
        }

        countDownLatch.countDown();
    }

    @Override
    public void getFailed(Exception e) {
        System.out.println("GET request failed");
        countDownLatch.countDown();
    }

    public boolean put(long key, String value) throws InterruptedException {
        runningServers = new ArrayList(servers.size());
        countDownLatch = new CountDownLatch(servers.size());

        for (CacheServiceInterface cache : servers.values()) {
            cache.put(key, value);
        }

        countDownLatch.await();

        boolean isSuccess = Math.round((float)runningServers.size() / servers.size()) == 1;

        if (! isSuccess) {
            delete(key, value);
        }
        return isSuccess;
    }

    public void delete(long key, String value) {

        for (final String serverUrl : runningServers) {
            CacheServiceInterface server = servers.get(serverUrl);
            server.delete(key);
        }
    }

    public String get(long key) throws InterruptedException {
        results = new ConcurrentHashMap<String, ArrayList<String>>();
        countDownLatch = new CountDownLatch(servers.size());

        for (final CacheServiceInterface server : servers.values()) {
            server.get(key);
        }
        countDownLatch.await();


        String correctedValue = results.keys().nextElement();

        if (results.keySet().size() > 1 || results.get(correctedValue).size() != servers.size()) {
            ArrayList<String> maxValues = maxKeyForTable(results);
            if (maxValues.size() == 1) {
                correctedValue = maxValues.get(0);
                ArrayList<String> repairServers = new ArrayList(servers.keySet());
                repairServers.removeAll(results.get(correctedValue));
                for (String serverUrl : repairServers) {
                    System.out.println("corrected by readrepair on: " + serverUrl + " to value: " + correctedValue);
                    CacheServiceInterface server = servers.get(serverUrl);
                    server.put(key, correctedValue);
                }
            } else {
            }
        }
        return correctedValue;
    }

    public ArrayList<String> maxKeyForTable(ConcurrentHashMap<String, ArrayList<String>> table) {
        ArrayList<String> maxKeys= new ArrayList<String>();
        int maxValue = -1;
        for(Map.Entry<String, ArrayList<String>> entry : table.entrySet()) {
            if(entry.getValue().size() > maxValue) {
                maxKeys.clear();
                maxKeys.add(entry.getKey());
                maxValue = entry.getValue().size();
            }
            else if(entry.getValue().size() == maxValue)
            {
                maxKeys.add(entry.getKey());
            }
        }
        return maxKeys;
    }
}

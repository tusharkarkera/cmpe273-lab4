package edu.sjsu.cmpe.cache.client;

import com.mashape.unirest.http.Unirest;

public class Client {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Cache Client...");
        CRDTClient crdtClient = new CRDTClient();

        boolean result = crdtClient.put(1,"a");
        System.out.println(result);
        System.out.println("Step 1: put(1 => a);");
        System.out.println("Sleep time of 30s");
        Thread.sleep(30*1000);


        crdtClient.put(1,"b");
        System.out.println("Step 2: put(1 => b); sleeping 30s");
        System.out.println("Sleep time of 30s");
        Thread.sleep(30*1000);

        String value = crdtClient.get(1);
        System.out.println("Step 3: get(1) => " + value);

        System.out.println("Exiting Client...");
        Unirest.shutdown();
    }

}
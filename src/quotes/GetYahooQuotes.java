package quotes;

/**
*
* Script to download Yahoo historical quotes using the new cookie authenticated site.
*
* Usage: java -classpath $CLASSPATH GetJavaQuotes SYMBOL
*
*
* Author: Brad Lucas brad@beaconhill.com
* Latest: https://github.com/bradlucas/get-yahoo-quotes
* http://blog.bradlucas.com/posts/2017-06-04-yahoo-finance-quote-download-java/
* Copyright (c) 2017 Brad Lucas - All Rights Reserved
*
*
* History
*
* 06-04-2017 : Created script
*
*/
import java.io.BufferedReader;
import java.io. InputStreamReader;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.BufferedOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


import org.apache.http.HttpEntity;;
import org.apache.http.HttpResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.methods.HttpGet;

import org.apache.commons.lang3.StringEscapeUtils;


public class GetYahooQuotes {

    private static GetYahooQuotes instance;
    
    HttpClient client = HttpClientBuilder.create().build();
    HttpClientContext context = HttpClientContext.create();

    private GetYahooQuotes() {
        CookieStore cookieStore = new BasicCookieStore();
        client = HttpClientBuilder.create().build();
        context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
    }
    
    static {
    instance = new GetYahooQuotes();
    }
    
    public static GetYahooQuotes getInstance() {
        return instance;
    }

    public String getPage(String symbol) {
        String rtn = null;
        String url = String.format("https://finance.yahoo.com/quote/%s/?p=%s", symbol, symbol);
        HttpGet request = new HttpGet(url);
        System.out.println(url);

        request.addHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13");
        try {
            HttpResponse response = client.execute(request, context);
            System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rtn = result.toString();
            HttpClientUtils.closeQuietly(response);
        } catch (Exception ex) {
            System.out.println("Exception");
            System.out.println(ex);
        }
        System.out.println("returning from getPage");
        return rtn;
    }

    public List<String> splitPageData(String page) {
        // Return the page as a list of string using } to split the page
        return Arrays.asList(page.split("}"));
    }

    public String findCrumb(List<String> lines) {
        String crumb = "";
        String rtn = "";
        for (String l : lines) {
            if (l.indexOf("CrumbStore") > -1) {
                rtn = l;
                break;
            }
        }
        // ,"CrumbStore":{"crumb":"OKSUqghoLs8"        
        if (rtn != null && !rtn.isEmpty()) {
            String[] vals = rtn.split(":");                 // get third item
            crumb = vals[2].replace("\"", "");              // strip quotes
            crumb = StringEscapeUtils.unescapeJava(crumb);  // unescape escaped values (particularly, \u002f
            }
        return crumb;
    }

    public String getCrumb(String symbol) {
        return findCrumb(splitPageData(getPage(symbol)));
    }
                  

    public List<String> getData(String symbol, long startDate, long endDate, String crumb) {
        
        List<String> lines = new LinkedList<String>();
        
        BufferedInputStream bis = null; 
        
        String filename = String.format("%s.csv", symbol);
        String url = String.format("https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%s&period2=%s&interval=1d&events=history&crumb=%s", symbol, startDate, endDate, crumb);
        HttpGet request = new HttpGet(url);
      //  System.out.println(url);

        request.addHeader("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13");
        try {
            HttpResponse response = client.execute(request, context);
       //     System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();

            String reasonPhrase = response.getStatusLine().getReasonPhrase();
            int statusCode = response.getStatusLine().getStatusCode();
            
       //     System.out.println(String.format("statusCode: %d", statusCode));
        //    System.out.println(String.format("reasonPhrase: %s", reasonPhrase));

            if (entity != null) {
                bis = new BufferedInputStream(entity.getContent());
                
                BufferedReader r = new BufferedReader(new InputStreamReader(bis));
                r.readLine();
         
         String inputLine;
                 
         
       while ((inputLine = r.readLine()) != null) {
           lines.add(inputLine);
      }
                
          
                r.close();
                bis.close();
            }
            HttpClientUtils.closeQuietly(response);

        } catch (Exception ex) {
            System.out.println("Exception");
            System.out.println(ex);
        }
          
        return lines;
    }
    

   
}

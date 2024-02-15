package com.example.finishedtask.services;

import com.example.finishedtask.entities.Result;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class CrawlerServiceImpl implements CrawlerService {
    private static final int THREAD_POOL_SIZE = 5;

    @Override
    public List<Result> inquireResults(Long page) throws IOException, InterruptedException {
        List<String> totalIdList = new ArrayList<>();
        if (Objects.isNull(page)) {
            page = 10L;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);


        List<CompletableFuture<List<String>>> futures = new ArrayList<>();


        for (int i = 1; i <= page; i++) {
            final int currentPage = i;
            CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> getIdsFromApi(currentPage), executorService);
            futures.add(future);
        }

        totalIdList = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<CompletableFuture<Result>> contentFutures = totalIdList.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String content = fetchContentFromURL(Integer.parseInt(id));
                        return saveTxt(Integer.parseInt(id), content);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }, executorService))
                .collect(Collectors.toList());

        List<Result> results = contentFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());


        executorService.shutdown();

        return results;
    }

    @Override
    public Result inquireResultById(String id) throws IOException {
        String content = fetchContentFromURL(Integer.parseInt(id));
        return saveTxt(Integer.parseInt(id), content);
    }

    public static List<String> getIdsFromApi(int page) {
        List<String> idList = new ArrayList<>();

        try {
            URL url = new URL("https://www.ilan.gov.tr/api/api/services/app/Ad/AdsByFilter");
            HttpURLConnection connection = postHttpURLConnection(url.toString());
            String payload = createPayload(page);
            byte[] postData = payload.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                JsonParser jsonParser = new JsonParser();
                JsonObject jsonResponse = jsonParser.parse(response.toString()).getAsJsonObject();
                JsonObject resultObject = jsonResponse.getAsJsonObject("result");
                JsonArray adsArray = resultObject.getAsJsonArray("ads");

                for (int i = 0; i < adsArray.size(); i++) {
                    JsonObject adObject = adsArray.get(i).getAsJsonObject();
                    String idValue = adObject.get("id").getAsString();
                    idList.add(idValue);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return idList;
    }

    public static String createPayload(int currentPage) {
        return "{\n" +
                "  \"keys\": {\n" +
                "    \"txv\": [\n" +
                "      9\n" +
                "    ],\n" +
                "    \"currentPage\": [\n" +
                "      " + currentPage + "\n" +
                "    ],\n" +
                "    \"ats\": [\n" +
                "      3\n" +
                "    ]\n" +
                "  },\n" +
                "  \"skipCount\": 36,\n" +
                "  \"maxResultCount\": 12\n" +
                "}";
    }

    public static String fetchContentFromURL(int id) {
        String urlStr = "https://www.ilan.gov.tr/api/api/services/app/AdDetail/GetAdDetail?id=" + id;
        try {
            HttpURLConnection connection = getHttpURLConnection(urlStr);
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static HttpURLConnection getHttpURLConnection(String urlStr) throws IOException {
        URL apiUrl = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("authority", "www.ilan.gov.tr");
        connection.setRequestProperty("method", "GET");
        connection.setRequestProperty("scheme", "https");
        connection.setRequestProperty("Accept", "text/plain");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        connection.setRequestProperty("Accept-Language", "en,tr;q=0.9");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Dnt", "1");
        connection.setRequestProperty("Expires", "Sat, 01 Jan 2000 00:00:00 GMT");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
        connection.setRequestProperty("Sec-Ch-Ua-Mobile", "?0");
        connection.setRequestProperty("Sec-Ch-Ua-Platform", "\"Windows\"");
        connection.setRequestProperty("Sec-Fetch-Dest", "empty");
        connection.setRequestProperty("Sec-Fetch-Mode", "cors");
        connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("X-Request-Origin", "IGT-UI");
        connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        return connection;
    }

    private static HttpURLConnection postHttpURLConnection(String urlStr) throws IOException {
        URL apiUrl = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json-patch+json");
        connection.setRequestProperty("Accept", "text/plain");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        connection.setRequestProperty("Accept-Language", "en,tr;q=0.9");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Dnt", "1");
        connection.setRequestProperty("Origin", "https://www.ilan.gov.tr");
        connection.setRequestProperty("Referer", "https://www.ilan.gov.tr/ilan/kategori/9/ihale-duyurulari?ats=3");
        connection.setRequestProperty("Sec-Fetch-Dest", "empty");
        connection.setRequestProperty("Sec-Fetch-Mode", "cors");
        connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("X-Request-Origin", "IGT-UI");
        connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        return connection;
    }

    private static Result saveTxt(int id, String content) throws IOException {
        String baseUrl = "https://www.ilan.gov.tr/ilan/";
        String outputFileName = "output.txt";
        Document doc = Jsoup.parse(content);
        String ihaleKayitNo = nullCheck(doc.select("table:contains(İKN) td:eq(2)"), 0);
        String dokumanUrl = nullCheck(doc.select("table:contains(İhale dokümanının görülebileceği) td:eq(2)"), 3);
        String niteligiTuruMiktari = nullCheck(doc.select("table:contains(Niteliği, türü ve miktarı) td:eq(2)"), 1);
        String isinYapilacagiYer = nullCheck(doc.select("table:contains(Yapılacağı/teslim edileceği yer) td:eq(2)"), 2);

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName, true));
        writer.write("\nİhale Kayıt No: " + ihaleKayitNo + "\n");
        writer.write("URL: " + dokumanUrl + "\n");
        writer.write("Niteliği, Türü ve Miktarı: " + niteligiTuruMiktari + "\n");
        writer.write("İşin Yapılacağı Yer: " + isinYapilacagiYer + "\n");
        writer.write("Web Site URL: " + baseUrl + id + "\n");
        writer.write("----------------------------------------------------------");
        writer.close();
        Result result = new Result();
        result.setDokumanUrl(dokumanUrl);
        result.setIhaleKayitNo(ihaleKayitNo);
        result.setNiteligiTuruMiktari(niteligiTuruMiktari);
        result.setBaseUrlId(baseUrl + id);
        result.setIsinYapilacagiYer(isinYapilacagiYer);
        return result;
    }

    private static String nullCheck(Elements elements, int index) {
        if (elements != null && elements.size() > index) {
            return elements.get(index).text();
        } else {
            return "N/A";
        }
    }
}



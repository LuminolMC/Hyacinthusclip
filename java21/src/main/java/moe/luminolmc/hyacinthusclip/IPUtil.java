package moe.luminolmc.hyacinthusclip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class IPUtil {
    private static volatile String cachedCountry = null;
    private static volatile boolean asyncStarted = false;

    protected static String getCountryByIp() {
        // 如果已经查到，直接返回
        if (cachedCountry != null) {
            return cachedCountry;
        }

        HttpClient client = HttpClient.newHttpClient();
        // 主线程最多阻塞 5 秒
        for (IpApi api : IpApi.values()) {
            try {
                HttpResponse<String> response = client.send(createRequest(api.getUrl(), 5), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String redirectUrl = response.headers().firstValue("Location").orElse(null);
                    if (redirectUrl != null) {
                        response = client.send(createRequest(redirectUrl, 5), HttpResponse.BodyHandlers.ofString());
                    }
                }
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    cachedCountry = api.processResponse(response.body());
                    return cachedCountry;
                }
            } catch (Exception ignored) {
                // 超时或其他异常，进入异步
            }
        }
        // 启动异步后台查
        startAsyncQuery();
        return "Unknown";
    }

    private static synchronized void startAsyncQuery() {
        if (asyncStarted) return;
        asyncStarted = true;
        Thread t = new Thread(() -> {
            HttpClient client = HttpClient.newHttpClient();
            for (IpApi api : IpApi.values()) {
                try {
                    HttpResponse<String> response = client.send(createRequest(api.getUrl(), 3600), HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 300 && response.statusCode() < 400) {
                        String redirectUrl = response.headers().firstValue("Location").orElse(null);
                        if (redirectUrl != null) {
                            response = client.send(createRequest(redirectUrl, 3600), HttpResponse.BodyHandlers.ofString());
                        }
                    }
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        cachedCountry = api.processResponse(response.body());
                        return;
                    }
                } catch (Exception ignored) {
                    // 超时或其他异常静默
                }
            }
        }, "IpApi-Async-Query");
        t.setDaemon(true);
        // 3600 秒后强制终止线程
        t.start();
        new Thread(() -> {
            try {
                t.join(3600_000L); // 3600 秒
                if (t.isAlive()) {
                    t.stop(); // 强制终止（已知不推荐，但此处为止血临时方案）
                }
            } catch (Throwable ignored) {}
        }, "IpApi-Async-Killer").start();
    }

    private static HttpRequest createRequest(String url, int timeoutSeconds) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    private enum IpApi {
        IPINFO("http://ipinfo.io/country", String::trim),
        IP_API("http://ip-api.com/json/?fields=country", string -> {
            JsonObject json = JsonParser.parseString(string).getAsJsonObject();
            return json.get("country").getAsString();
        });

        private final String url;
        private final java.util.function.Function<String, String> processor;

        IpApi(String url, java.util.function.Function<String, String> processor) {
            this.url = url;
            this.processor = processor;
        }

        public String getUrl() {
            return url;
        }

        public String processResponse(String response) {
            return processor.apply(response);
        }
    }
}

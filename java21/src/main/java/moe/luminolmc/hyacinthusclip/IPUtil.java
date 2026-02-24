package moe.luminolmc.hyacinthusclip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class IPUtil {
    protected static String getCountryByIp() {
        HttpClient client = HttpClient.newHttpClient();

        for (IpApi api : IpApi.values()) {
            try {
                HttpResponse<String> response = client.send(createRequest(api.getUrl()), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String redirectUrl = response.headers().firstValue("Location").orElse(null);
                    if (redirectUrl != null) {
                        response = client.send(createRequest(redirectUrl), HttpResponse.BodyHandlers.ofString());
                    }
                }
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return api.processResponse(response.body());
                }
            } catch (Exception ignored) {
            }
        }
        return "Unknown";
    }

    private static HttpRequest createRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ConcurrencyVsParallelism {

    // Cliente HTTP compartilhado com HTTP/1.1 explícito (Evita gargalo de
    // multiplexação HTTP/2 da Wikipedia)
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // O exec-maven-plugin ainda exige a assinatura padrão do Java
    public static void main(String[] args) throws Exception {
        cleanUpHtmls();
        System.out.println("Núcleos disponíveis: " + Runtime.getRuntime().availableProcessors());

        List<String> links = getLinks();
        if (links.isEmpty()) {
            System.err.println("Nenhum link encontrado!");
            return;
        }
        System.out.println("Total pages: " + links.size());

        Instant start = Instant.now();
        int qtdTreds = 32;
        System.out.println("Quantidade de threads simuladas: " + qtdTreds);

        // PADRÃO DE THREADS NATIVAS (Exatamente o mesmo comportamento do Python)
        // Como a operação de gravação de arquivos (Files.write) no Java ainda "pina"
        // (prende) a OS Thread local
        // nas Virtual Threads, usar 32 Threads Nativas se sairá incrivelmente melhor
        // aqui!
        try (var executor = Executors.newFixedThreadPool(qtdTreds)) {
            for (String link : links) {
                executor.submit(() -> fetch(link));
            }
        } // O bloco try-with-resources aguarda automaticamente todas as Threads
          // terminarem!

        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("Downloaded %d links in %.3f seconds%n", links.size(), duration.toMillis() / 1000.0);
    }

    private static void cleanUpHtmls() throws IOException {
        Path directory = Path.of("paises");
        if (Files.exists(directory)) {
            // O "var" é do Java 10+ (Inferencia de tipo)
            try (var stream = Files.newDirectoryStream(directory, "*.html")) {
                int count = 0;
                for (Path entry : stream) {
                    Files.delete(entry);
                    System.out.println("Deletado: " + entry);
                    count++;
                }
                if (count > 0) {
                    System.out.println("Total removido: " + count + " arquivos\n");
                }
            }
        }
    }

    private static List<String> getLinks() throws IOException, InterruptedException {
        String baseURL = "https://en.wikipedia.org/wiki/List_of_countries_by_population_(United_Nations)";

        var req = HttpRequest.newBuilder(URI.create(baseURL))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        var res = client.send(req, HttpResponse.BodyHandlers.ofString());

        // Jsoup: Parse do HTML elegante, praticamente mesma API do
        // BeautifulSoup/GoQuery
        Document doc = Jsoup.parse(res.body(), baseURL);
        List<String> result = new ArrayList<>();

        for (Element link : doc.select("td .flagicon + a")) {
            result.add(link.absUrl("href"));
        }

        return result;
    }

    private static void fetch(String link) {
        try {
            Path dir = Path.of("paises");
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }

            var req = HttpRequest.newBuilder(URI.create(link))
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            var res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

            String fileName = link.substring(link.lastIndexOf('/') + 1) + ".html";
            Files.write(dir.resolve(fileName), res.body());
        } catch (Exception e) {
            System.err.println("Erro ao baixar [" + link + "]: " + e.getMessage());
        }
    }
}

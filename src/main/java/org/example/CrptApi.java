package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.ToString;

import java.net.URI;
import java.net.http.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@ToString
class Description {
    @JsonProperty("participantInn")
    private String participantInn;

    public Description(String participantInn){
        this.participantInn = participantInn;
    }

}

@ToString
class Product {
    @JsonProperty("certificate_document")
    private String certificateDocument;

    @JsonProperty("certificate_document_date")
    private String certificateDocumentDate;

    @JsonProperty("certificate_document_number")
    private String certificateDocumentNumber;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private String productionDate;

    @JsonProperty("tnved_code")
    private String tnvedCode;

    @JsonProperty("uit_code")
    private String uitCode;

    @JsonProperty("uitu_code")
    private String uituCode;

    public Product(){
        this.certificateDocument = "string";
        this.certificateDocumentDate = "2020-01-23";
        this.certificateDocumentNumber = "string";
        this.ownerInn = "string";
        this.producerInn = "string";
        this.productionDate = "2020-01-23";
        this.tnvedCode = "string";
        this.uitCode = "string";
        this.uituCode = "string";
    }

}

/**
 * Класс сущности Документ.
 */
@ToString
class Document {
    @JsonProperty("description")
    private Description description;

    @JsonProperty("doc_id")
    private String docId;

    @JsonProperty("doc_status")
    private String docStatus;

    @JsonProperty("doc_type")
    private String docType;

    @JsonProperty("importRequest")
    private boolean importRequest;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("participant_inn")
    private String participantInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private String productionDate;

    @JsonProperty("production_type")
    private String productionType;

    @JsonProperty("products")
    private List<Product> products;

    @JsonProperty("reg_date")
    private String regDate;

    @JsonProperty("reg_number")
    private String regNumber;

    public Document(String participantInn, String docId ){
        this.description = new Description(participantInn);
        this.docId = docId;
        this.docStatus = "string";
        this.docType = "LP_INTRODUCE_GOODS";
        this.importRequest = true;
        this.ownerInn = "string";
        this.participantInn = "string";
        this.producerInn = "string";
        this.productionDate = "2020-01-23";
        this.productionType = "string";
        this.products = Collections.singletonList(new Product());
        this.regDate = "2020-01-23";
        this.regNumber = "string";
    }
}

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Lock lock = new ReentrantLock();
    private int requestCounter = 0;
    private long lastResetTime = System.currentTimeMillis();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;

        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Метод создания документа, принимает документ и подпись.
     * Возьмем lock, для синхронизации.
     * Далее првоеряем, прошла ли минута с последнего сброса счетчика, если да, то обнулим счетчик запросов и обновим время.
     * Проверяем, если количество запросов меньше лимита, то выполняем HTTP-запрос, иначе просим совершить попытку позже.
     * Если запрос был отправлен, то увеличиваем счетчик.
     */
    public void createDocument(Document document, String signature) {
        try {
            lock.lock();

            long currentTime = System.currentTimeMillis();
            long timeSinceLastReset = currentTime - lastResetTime;
            if (TimeUnit.MILLISECONDS.convert(timeSinceLastReset, timeUnit) >= 1) {
                requestCounter = 0;
                lastResetTime = currentTime;
            }

            if (requestCounter < requestLimit) {

                try {
                    URI uri = URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create");

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(document)))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                requestCounter++;
            } else {
                System.out.println("The request limit has been exceeded. Try again later.");
            }
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);
        Document document = new Document("participantInn", "stringDocId");
        crptApi.createDocument(document, "58567F7A51a58708C8B40ec592A38bA64C0697De");

    }
}

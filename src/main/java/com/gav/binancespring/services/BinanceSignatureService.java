package com.gav.binancespring.services;

import com.gav.binancespring.config.BinanceProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BinanceSignatureService {

    private final PrivateKey privateKey;

    public BinanceSignatureService(BinanceProperties properties) {
        this.privateKey = loadPrivateKey(Path.of(properties.privateKeyPath()));
    }

    public String sign(Map<String, Object> params) {
        String payload = params.entrySet().stream()
                .filter(entry -> !"signature".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось подписать запрос Binance", e);
        }
    }

    private static PrivateKey loadPrivateKey(Path path) {
        try {
            String pem = Files.readString(path, StandardCharsets.US_ASCII);
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] pkcs8 = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("Ed25519")
                    .generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось загрузить Ed25519 ключ из " + path, e);
        }
    }
}

package io.accelerate.auth.kmsjwt;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import io.accelerate.auth.kmsjwt.key.KMSDecrypt;
import io.accelerate.auth.kmsjwt.key.KMSEncrypt;
import io.accelerate.auth.kmsjwt.key.KeyOperationException;
import io.accelerate.auth.kmsjwt.token.JWTDecoder;
import io.accelerate.auth.kmsjwt.token.JWTEncoder;
import io.accelerate.auth.kmsjwt.token.JWTVerificationException;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateTokenApp {

    private static final Logger log = LoggerFactory.getLogger(GenerateTokenApp.class);
    private static final String WARMUP_CHALLENGES_CLAIM = "tdl_wrm";
    private static final String OFFICIAL_CHALLENGE_CLAIM = "tdl_chx";

    @Parameter(names = {"-e", "--endpoint"}, description = "Optional KMS endpoint override (e.g. http://localhost:4566)")
    private String endpoint;

    private final GenerateCommand generate = new GenerateCommand();
    private final ValidateCommand validate = new ValidateCommand();

    public static void main(String[] args) {
        int exitCode = new GenerateTokenApp().execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private int execute(String[] args) {
        JCommander commander = JCommander.newBuilder()
                .addObject(this)
                .addCommand("generate", generate)
                .addCommand("validate", validate)
                .programName("kms-jwt-cli")
                .build();

        if (args.length == 0) {
            commander.usage();
            return 1;
        }

        try {
            commander.parse(args);
        } catch (ParameterException e) {
            log.error(e.getMessage());
            commander.usage();
            return 1;
        }

        String parsedCommand = commander.getParsedCommand();
        if (parsedCommand == null) {
            commander.usage();
            return 1;
        }

        try {
            return switch (parsedCommand) {
                case "generate" -> {
                    handleGenerate();
                    yield 0;
                }
                case "validate" -> {
                    handleValidate();
                    yield 0;
                }
                default -> {
                    commander.usage();
                    yield 1;
                }
            };
        } catch (KeyOperationException | JWTVerificationException | IllegalArgumentException e) {
            log.error(e.getMessage());
            return 1;
        }
    }

    private void handleGenerate() throws KeyOperationException {
        log.info("Generating JWT for user \"{}\" with journey \"{}\", valid for {} days",
                generate.username, generate.journey, generate.expiresInDays);
        try (KmsClient kmsClient = buildClient(generate.region)) {
            KMSEncrypt kmsEncrypt = new KMSEncrypt(kmsClient, generate.keyArn);
            Date expiryDate = expirationDate(generate.expiresInDays);
            JourneyClaims journeyClaims = splitJourney(generate.journey);
            String jwt = JWTEncoder.builder(kmsEncrypt)
                    .setExpiration(expiryDate)
                    .claim("usr", generate.username)
                    .claim(WARMUP_CHALLENGES_CLAIM, journeyClaims.warmupChallenges())
                    .claim(OFFICIAL_CHALLENGE_CLAIM, journeyClaims.officialChallenge())
                    .compact();

            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~");
            System.out.println("JWT_TOKEN=" + jwt);
        }
    }

    private void handleValidate() throws KeyOperationException, JWTVerificationException {
        log.info("Validating JWT using key \"{}\" in region \"{}\"", validate.keyArn, validate.region);
        try (KmsClient kmsClient = buildClient(validate.region)) {
            KMSDecrypt kmsDecrypt = new KMSDecrypt(kmsClient, Set.of(validate.keyArn));
            JWTDecoder decoder = new JWTDecoder(kmsDecrypt);
            Claims claims = decoder.decodeAndVerify(validate.token);

            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~");
            System.out.println("JWT_VALIDATED=true");
            printClaim(claims, "usr");
            printClaim(claims, WARMUP_CHALLENGES_CLAIM);
            printClaim(claims, OFFICIAL_CHALLENGE_CLAIM);
        }
    }

    private void printClaim(Claims claims, String key) {
        Object value = claims.get(key);
        if (value != null) {
            if (value instanceof List<?> list) {
                String joined = list.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                System.out.println("JWT_CLAIM_" + key + "=" + joined);
                return;
            }
            System.out.println("JWT_CLAIM_" + key + "=" + value);
        }
    }

    private KmsClient buildClient(String region) {
        var builder = KmsClient.builder()
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
            log.info("Using custom KMS endpoint: {}", endpoint);
        }

        return builder.build();
    }

    private static Date expirationDate(int expiresInDays) {
        return new Date(Instant.now().plus(expiresInDays, ChronoUnit.DAYS).toEpochMilli());
    }

    private static JourneyClaims splitJourney(String journey) {
        if (journey == null || journey.isBlank()) {
            throw new IllegalArgumentException("Journey must contain at least one challenge");
        }

        String[] rawParts = journey.split(",");
        List<String> parts = new ArrayList<>(rawParts.length);
        for (String rawPart : rawParts) {
            String trimmed = rawPart.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }

        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Journey must contain at least one challenge");
        }

        String official = parts.get(parts.size() - 1);
        List<String> warmups = List.copyOf(parts.subList(0, parts.size() - 1));
        return new JourneyClaims(warmups, official);
    }

    private record JourneyClaims(List<String> warmupChallenges, String officialChallenge) {
    }

    @Parameters(commandDescription = "Generate a JWT token")
    private static class GenerateCommand {

        @Parameter(names = {"-r", "--region"}, description = "The region where the KMS key lives", required = true)
        private String region;

        @Parameter(names = {"-k", "--key"}, description = "The ARN of the key to be used", required = true)
        private String keyArn;

        @Parameter(names = {"-u", "--username"}, description = "Unique username. Should not contain names.", required = true)
        private String username;

        @Parameter(names = {"-j", "--journey"}, description = "The journey associated to this user", required = true)
        private String journey;

        @Parameter(names = {"-x", "--expire-in"}, description = "The expiry period in days. Default 2 days")
        private int expiresInDays = 2;
    }

    @Parameters(commandDescription = "Validate a JWT token")
    private static class ValidateCommand {

        @Parameter(names = {"-r", "--region"}, description = "The region where the KMS key lives", required = true)
        private String region;

        @Parameter(names = {"-k", "--key"}, description = "An allowed KMS key ARN", required = true)
        private String keyArn;

        @Parameter(names = {"-t", "--token"}, description = "JWT token value to validate", required = true)
        private String token;
    }
}

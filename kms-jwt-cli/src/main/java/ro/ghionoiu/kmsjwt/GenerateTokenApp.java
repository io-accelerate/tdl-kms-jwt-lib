package ro.ghionoiu.kmsjwt;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.ghionoiu.kmsjwt.key.KMSEncrypt;
import ro.ghionoiu.kmsjwt.key.KeyOperationException;
import ro.ghionoiu.kmsjwt.token.JWTEncoder;
import ro.ghionoiu.kmsjwt.token.JWTVerificationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class GenerateTokenApp {

    private static final Logger log = LoggerFactory.getLogger(GenerateTokenApp.class);

    @Parameter(names = {"-r", "--region"}, description = "The region where the bucket lives", required = true)
    private String region;

    @Parameter(names = {"-k", "--key"}, description = "The ARN of the key to be used", required = true)
    private String keyARN;

    @Parameter(names = {"-u", "--username"}, description = "Unique username. Should not contain names.", required = true)
    private String username;

    @Parameter(names = {"-j", "--journey"}, description = "The journey associated to this user", required = true)
    private String journey;

    @Parameter(names = {"-x", "--expire-in"}, description = "The expiry period in days. Default 2 days")
    private int expiresInDays = 2;

    public static void main(String[] args) throws JWTVerificationException, KeyOperationException {
        GenerateTokenApp main = new GenerateTokenApp();
        new JCommander(main, args);

        String jwt = main.generateJWT();
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.println("JWT_TOKEN=" + jwt);
    }

    private String generateJWT() throws KeyOperationException {
        log.info("Generating JWT for user \"{}\" with journey \"{}\", valid for {} days", username, journey, expiresInDays);

        AWSKMS kmsClient = AWSKMSClientBuilder.standard()
                .withRegion(region)
                .build();

        KMSEncrypt kmsEncrypt = new KMSEncrypt(kmsClient, keyARN);
        Date expiryDate = expirationDate(expiresInDays);
        return JWTEncoder.builder(kmsEncrypt)
                .setExpiration(expiryDate)
                .claim("usr", username)
                .claim("jrn", journey)
                .compact();
    }

    private static Date expirationDate(int expiresInDays) {
        return new Date(Instant.now().plus(expiresInDays, ChronoUnit.DAYS).toEpochMilli());
    }
}
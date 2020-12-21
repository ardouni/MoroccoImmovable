package com.morocco.immovable;

import com.morocco.immovable.Entities.House;
import com.morocco.immovable.Entities.Message;
import com.morocco.immovable.Entities.User;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import io.fusionauth.jwt.InvalidJWTException;
import io.fusionauth.jwt.Signer;
import io.fusionauth.jwt.Verifier;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.hmac.HMACSigner;
import io.fusionauth.jwt.hmac.HMACVerifier;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(8080);
        app.config.addStaticFiles(new File("").getAbsolutePath() + "/files/", Location.EXTERNAL);
        Database.connect();
        app.get("/", Main::index);
        app.get("/houses", Main::getHouses);
        app.get("/owner/houses", Main::getOwnerHouses);
        app.get("/houses/:id", Main::getDetails);
        app.get("/rentedOrSold/houses/:id", Main::rentedOrSold);
        app.get("/search/:query", Main::search);
        app.post("/contact", Main::contact);
        app.post("/login", Main::login);
        app.post("/register", Main::register);
        app.get("/verify/:token", Main::verify);
        app.post("/add", Main::add);

        app.options("/*", context -> {
            String accessControlRequestHeaders = context.header("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                context.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = context.header("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                context.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
        });
        app.before(context -> context.header("Access-Control-Allow-Origin", "*"));
    }

    private static void index(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        HashMap<String, String> response = new HashMap<>();
        response.put("status", "up");
        JsonAdapter<Map> adapter = moshi.adapter(Map.class);
        String json = adapter.toJson(response);
        context.result(json);
    }

    private static void login(Context context) {
        String content = context.body();
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<User> jsonAdapter = moshi.adapter(User.class);
        try {
            User user = jsonAdapter.fromJson(content);
            String sql = "SELECT password, active FROM users WHERE email = ?";
            PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
            preparedStatement.setString(1, user.getEmail());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                if (resultSet.getString(1).equals(hashPassword(user.getPassword()))) {
                    if (resultSet.getBoolean(2)) {
                        Signer signer = HMACSigner.newSHA512Signer(Settings.secret);
                        JWT jwt = new JWT().setSubject(user.getEmail()).setExpiration(ZonedDateTime.now(ZoneOffset.UTC).plusMonths(2));
                        String encodedJWT = JWT.getEncoder().encode(jwt, signer);
                        HashMap<String, String> response = new HashMap<>();
                        response.put("message", "1");
                        response.put("token", encodedJWT);
                        JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                        String json = adapter.toJson(response);
                        context.result(json);
                    } else {
                        HashMap<String, String> response = new HashMap<>();
                        response.put("message", "2");
                        JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                        String json = adapter.toJson(response);
                        context.result(json);
                    }
                }
            } else {
                HashMap<String, String> response = new HashMap<>();
                response.put("message", "0");
                JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                String json = adapter.toJson(response);
                context.result(json);
            }

        } catch (IOException | SQLException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "-1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            e.printStackTrace();
        }
    }

    private static void getHouses(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        String token = context.header("Authorization");
        if (token.trim().isEmpty()) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "3");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }
        Verifier verifier = HMACVerifier.newVerifier(Settings.secret);
        String email;
        try {
            email = JWT.getDecoder().decode(token, verifier).subject;
        } catch (InvalidJWTException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "4");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }

        try {
            String sql = "SELECT * FROM houses WHERE owner <> (SELECT id FROM users WHERE email = ? ) AND status = FALSE";
            PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
            preparedStatement.setString(1, email);
            ResultSet resultSet = preparedStatement.executeQuery();
            ArrayList<House> houses = new ArrayList<>();
            while (resultSet.next()) {
                House house = new House(resultSet.getInt(1), resultSet.getString(2), resultSet.getInt(3), resultSet.getFloat(4), resultSet.getString(5), resultSet.getBoolean(6), resultSet.getString(7));
                houses.add(house);
                Type type = Types.newParameterizedType(List.class, House.class);
                JsonAdapter<ArrayList<House>> adapter = moshi.adapter(type);
                String json = adapter.toJson(houses);
                context.result(json);
            }

        } catch (SQLException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "-1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            e.printStackTrace();
        }
    }

    private static void getOwnerHouses(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        String token = context.header("Authorization");
        if (token.trim().isEmpty()) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "3");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }
        Verifier verifier = HMACVerifier.newVerifier(Settings.secret);
        String email;
        try {
            email = JWT.getDecoder().decode(token, verifier).subject;
        } catch (InvalidJWTException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "4");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }

        try {
            String sql = "SELECT * FROM houses WHERE owner = (SELECT id FROM users WHERE email = ? ) AND status = FALSE";
            PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
            preparedStatement.setString(1, email);
            ResultSet resultSet = preparedStatement.executeQuery();
            ArrayList<House> houses = new ArrayList<>();
            while (resultSet.next()) {
                House house = new House(resultSet.getInt(1), resultSet.getString(2), resultSet.getInt(3), resultSet.getFloat(4), resultSet.getString(5), resultSet.getBoolean(6), resultSet.getString(7));
                houses.add(house);
                Type type = Types.newParameterizedType(List.class, House.class);
                JsonAdapter<ArrayList<House>> adapter = moshi.adapter(type);
                String json = adapter.toJson(houses);
                context.result(json);
            }

        } catch (SQLException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "-1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            e.printStackTrace();
        }
    }

    private static void getDetails(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        String token = context.header("Authorization");
        String id = context.pathParam("id");
        if (token.trim().isEmpty()) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "3");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }
        Verifier verifier = HMACVerifier.newVerifier(Settings.secret);
        String email;
        try {
            email = JWT.getDecoder().decode(token, verifier).subject;
        } catch (InvalidJWTException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "4");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }

        try {
            String sql = "SELECT first_name, last_name, email, phone_number, city FROM users WHERE id = (SELECT owner FROM houses WHERE id = ?)";
            PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
            preparedStatement.setInt(1, Integer.parseInt(id));
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                User user = new User();
                user.setFirstName(resultSet.getString(1));
                user.setLastName(resultSet.getString(2));
                user.setEmail(resultSet.getString(3));
                user.setPhoneNumber(resultSet.getString(4));
                user.setCity(resultSet.getString(5));
                JsonAdapter<User> adapter = moshi.adapter(User.class);
                String json = adapter.toJson(user);
                context.result(json);
            }

        } catch (SQLException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "-1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            e.printStackTrace();
        }
    }

    private static void rentedOrSold(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        String token = context.header("Authorization");
        String id = context.pathParam("id");
        if (token.trim().isEmpty()) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "3");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }
        Verifier verifier = HMACVerifier.newVerifier(Settings.secret);
        String email;
        try {
            email = JWT.getDecoder().decode(token, verifier).subject;
        } catch (InvalidJWTException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "4");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }

        try {
            String sql = "UPDATE houses SET status = TRUE WHERE id = ?";
            PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
            preparedStatement.setInt(1, Integer.parseInt(id));
            int result = preparedStatement.executeUpdate();
            if (result == 1) {
                HashMap<String, String> response = new HashMap<>();
                response.put("message", "1");
                JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                String json = adapter.toJson(response);
                context.result(json);
            }

        } catch (SQLException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "-1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            e.printStackTrace();
        }
    }

    private static void verify(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        String token = context.pathParam("token");
        if (token.trim().isEmpty()) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "empty token");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }
        Verifier verifier = HMACVerifier.newVerifier(Settings.secret);
        String email;
        try {
            email = JWT.getDecoder().decode(token, verifier).subject;
        } catch (InvalidJWTException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "wrong token !");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }

        try {
            String sql = "UPDATE users SET active = TRUE WHERE email = ?";
            PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
            preparedStatement.setString(1, email);
            int result = preparedStatement.executeUpdate();
            if (result == 1) {
                HashMap<String, String> response = new HashMap<>();
                response.put("message", "your email address has been verified, you can now log in !");
                JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                String json = adapter.toJson(response);
                context.result(json);
            }

        } catch (SQLException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "-1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            e.printStackTrace();
        }
    }

    private static void contact(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        String token = context.header("Authorization");
        if (token.trim().isEmpty()) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "3");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }
        Verifier verifier = HMACVerifier.newVerifier(Settings.secret);
        String email;
        try {
            email = JWT.getDecoder().decode(token, verifier).subject;
        } catch (InvalidJWTException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "4");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }

        String content = context.body();
        JsonAdapter<Message> jsonAdapter = moshi.adapter(Message.class);
        try {
            Message message = jsonAdapter.fromJson(content);
            sendMail(email, Settings.email, message.getSubject(), message.getName() + " said :\n" + message.getMessage());
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void search(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        String token = context.header("Authorization");
        if (token.trim().isEmpty()) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "3");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }
        Verifier verifier = HMACVerifier.newVerifier(Settings.secret);
        String email;
        try {
            email = JWT.getDecoder().decode(token, verifier).subject;
        } catch (InvalidJWTException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "4");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }

        try {
            String query = context.queryParam("query");
            String sql = "SELECT * FROM houses WHERE owner <> (SELECT id FROM users WHERE email = ? ) AND (owner IN (SELECT Id FROM users WHERE city LIKE ?) OR price < ? ) AND status = FALSE";
            PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, query + "%");
            preparedStatement.setFloat(3, 50000);
            ResultSet resultSet = preparedStatement.executeQuery();
            ArrayList<House> houses = new ArrayList<>();
            while (resultSet.next()) {
                House house = new House(resultSet.getInt(1), resultSet.getString(2), resultSet.getInt(3), resultSet.getFloat(4), resultSet.getString(5), resultSet.getBoolean(6), resultSet.getString(7));
                houses.add(house);
                Type type = Types.newParameterizedType(List.class, House.class);
                JsonAdapter<ArrayList<House>> adapter = moshi.adapter(type);
                String json = adapter.toJson(houses);
                context.result(json);
            }

        } catch (SQLException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "-1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            e.printStackTrace();
        }
    }

    private static void register(Context context) {
        String content = context.body();
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<User> jsonAdapter = moshi.adapter(User.class);
        try {
            User user = jsonAdapter.fromJson(content);
            String sql = "INSERT INTO users (first_name, last_name, email, password, identity_code, phone_number, age, city) VALUES (?,?,?,?,?,?,?,?)";
            PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
            preparedStatement.setString(1, user.getFirstName());
            preparedStatement.setString(2, user.getLastName());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, hashPassword(user.getPassword()));
            preparedStatement.setString(5, user.getIdentityCode());
            preparedStatement.setString(6, user.getPhoneNumber());
            preparedStatement.setInt(7, user.getAge());
            preparedStatement.setString(8, user.getCity());
            int result = preparedStatement.executeUpdate();
            if (result == 1) {
                Signer signer = HMACSigner.newSHA512Signer(Settings.secret);
                JWT jwt = new JWT().setSubject(user.getEmail()).setExpiration(ZonedDateTime.now(ZoneOffset.UTC).plusMonths(2));
                String token = JWT.getEncoder().encode(jwt, signer);
                sendMail("", user.getEmail(), "Morocco Immovable : Verify your email", "Hi,\nPlease click here to verify your email address: http://localhost:8080/verify/" + token + " ,\nSincerely !");
                HashMap<String, String> response = new HashMap<>();
                response.put("message", "1");
                JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                String json = adapter.toJson(response);
                context.result(json);
            } else {
                HashMap<String, String> response = new HashMap<>();
                response.put("message", "0");
                JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                String json = adapter.toJson(response);
                context.result(json);
            }

        } catch (IOException | SQLException e) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "-1");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            e.printStackTrace();
        }
    }

    private static void add(Context context) {
        Moshi moshi = new Moshi.Builder().build();
        String token = context.header("Authorization");
        if (token.trim().isEmpty()) {
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "3");
            JsonAdapter<Map> adapter = moshi.adapter(Map.class);
            String json = adapter.toJson(response);
            context.result(json);
            return;
        }
        Verifier verifier = HMACVerifier.newVerifier(Settings.secret);
        String email = JWT.getDecoder().decode(token, verifier).subject;
        try {
            String content = context.body();
            JsonAdapter<House> jsonAdapter = moshi.adapter(House.class);
            House house = jsonAdapter.fromJson(content);
            String name = generatePhotoName();
            byte[] decodedBytes = Base64.getDecoder().decode(house.getPhoto().split("base64,")[1]);
            File file = new File(new File("").getAbsolutePath() + "/files/" + name + ".jpg");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(decodedBytes);
            fileOutputStream.flush();
            fileOutputStream.close();

            try {

                String sql = "INSERT INTO houses (description, owner, price, type, status, photo) VALUES (?,(SELECT id FROM users WHERE email = ? ),?,?,?,?)";
                PreparedStatement preparedStatement = Database.connection.prepareStatement(sql);
                preparedStatement.setString(1, house.getDescription());
                preparedStatement.setString(2, email);
                preparedStatement.setFloat(3, house.getPrice());
                preparedStatement.setString(4, house.getType());
                preparedStatement.setBoolean(5, false);
                preparedStatement.setString(6, name + ".jpg");
                int result = preparedStatement.executeUpdate();
                if (result == 1) {
                    HashMap<String, String> response = new HashMap<>();
                    response.put("message", "1");
                    JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                    String json = adapter.toJson(response);
                    context.result(json);

                } else {
                    HashMap<String, String> response = new HashMap<>();
                    response.put("message", "0");
                    JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                    String json = adapter.toJson(response);
                    context.result(json);
                }

            } catch (SQLException e) {
                HashMap<String, String> response = new HashMap<>();
                response.put("message", "-1");
                JsonAdapter<Map> adapter = moshi.adapter(Map.class);
                String json = adapter.toJson(response);
                context.result(json);
                e.printStackTrace();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static String hashPassword(String password) {
        Charset utf8 = StandardCharsets.UTF_8;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        byte[] result = md.digest(password.getBytes(utf8));
        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String generatePhotoName() {
        int len = 10;
        String text = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom random = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            stringBuilder.append(text.charAt(random.nextInt(text.length())));
        return stringBuilder.toString();
    }

    private static void sendMail(String from, String to, String subject, String content) {
        try {
            Email email = new SimpleEmail();
            email.setHostName("smtp.mail.yahoo.com");
            email.setSmtpPort(465);
            email.setAuthenticator(new DefaultAuthenticator(Settings.email, Settings.emailPassword));
            email.setSSLOnConnect(true);
            email.setFrom(from.equals("") ? Settings.email : from);
            email.setSubject(subject);
            email.setMsg(content);
            email.addTo(to);
            email.send();
        } catch (EmailException e) {
            e.printStackTrace();
        }
    }

}

package grabber;

import grabber.utils.HabrCareerDateTimeParser;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;
import java.sql.*;
import java.util.ArrayList;

public class PsqlStore implements Store, AutoCloseable {
    private Connection cn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("driver"));
            cn = DriverManager.getConnection(
                    cfg.getProperty("url"),
                    cfg.getProperty("username"),
                    cfg.getProperty("password"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (cn != null) {
            cn.close();
            System.out.println("Closing!");
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement
                     = cn.prepareStatement("insert into post(name, link, text, created) values (?, ?, ?, ?) on conflict (link) do nothing")) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getLink());
            statement.setString(3, post.getDescription());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement =
                     cn.prepareStatement("select * from post")) {

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(resultPost(resultSet));
                }
            }
        } catch (SQLException  e) {
            e.printStackTrace();
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement statement =
                     cn.prepareStatement("select * from post where id = ?")) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    post = resultPost(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return post;
    }

    public Post resultPost(ResultSet resultSet) throws SQLException {
        return new Post(resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("link"),
                resultSet.getString("text"),
                resultSet.getTimestamp("created").toLocalDateTime());
    }

    public static void main(String[] args) {
        Properties config = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            config.load(in);
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        try (PsqlStore psqlStore = new PsqlStore(config)) {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> posts = habrCareerParse.list("https://career.habr.com/vacancies/java_developer?page=");
        for (Post post : posts) {
            psqlStore.save(post);
        }
        psqlStore.getAll().forEach(System.out::println);
        System.out.println(psqlStore.findById(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

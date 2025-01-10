package ru.job4j.articles.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.articles.model.Article;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ArticleStore implements Store<Article>, AutoCloseable {

    /* Логгер для записи логов операций. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleStore.class.getSimpleName());

    /* Конфигурации подключения к базе данных. */
    private final Properties properties;

    /* Соединение с базой данных для выполнения SQL-запросов. */
    private Connection connection;

    public ArticleStore(Properties properties) {
        this.properties = properties;
        initConnection();
        initScheme();
    }

    /* initConnection(): устанавливает соединение с базой данных. */
    private void initConnection() {
        LOGGER.info("Создание подключения к БД статей");
        try {
            connection = DriverManager.getConnection(
                    properties.getProperty("url"),
                    properties.getProperty("username"),
                    properties.getProperty("password")
            );
        } catch (SQLException throwables) {
            LOGGER.error("Не удалось выполнить операцию: { }", throwables.getCause());
            throw new IllegalStateException();
        }
    }

    /* создает схему таблицы слов, используя SQL-скрипт. */
    private void initScheme() {
        LOGGER.info("Инициализация таблицы статей");
        try (Statement statement = connection.createStatement()) {
            /* Создает таблицу dictionary если её нет с id и текстовым значением */
            var sql = Files.readString(Path.of("db/scripts", "articles.sql"));
            statement.execute(sql);
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
    }

    @Override
    public Article save(Article model) {
        LOGGER.info("Сохранение статьи");
        String sql = "insert into articles(text) values(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, model.getText());
            statement.executeUpdate();
            ResultSet key = statement.getGeneratedKeys();
            while (key.next()) {
                model.setId(key.getInt(1));
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
        return model;
    }

    public void saveAll(List<Article> articles) {
        LOGGER.info("Сохранение списка статей, количество: {}", articles.size());
        String sql = "INSERT INTO articles(text) VALUES(?)";
        final int batchSize = 1000;
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Article article : articles) {
                statement.setString(1, article.getText());
                statement.addBatch();
                if (++count % batchSize == 0) {
                    statement.executeBatch();
                    LOGGER.info("Выполнен пакетный запрос для {} статей", count);
                }
            }
            statement.executeBatch();
            LOGGER.info("Выполнен финальный пакетный запрос для {} статей", count);
        } catch (SQLException e) {
            LOGGER.error("Ошибка при сохранении списка статей: {}", e.getMessage(), e);
            throw new IllegalStateException("Не удалось сохранить статьи в базу данных.", e);
        }
    }

    @Override
    public List<Article> findAll() {
        LOGGER.info("Загрузка всех статей");
        String sql = "select * from articles";
        List<Article> articles = new ArrayList<Article>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet selection = statement.executeQuery();
            while (selection.next()) {
                articles.add(new Article(
                        selection.getInt("id"),
                        selection.getString("text")
                ));
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
        return articles;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
}

package ru.job4j.articles.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.articles.model.Word;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WordStore implements Store<Word>, AutoCloseable {

    /* Создание приватного статического не изменяемого поля Logger используется для ведения логов (записи событий программы)  */
    private static final Logger LOGGER = LoggerFactory.getLogger(WordStore.class.getSimpleName());

    /* Передаются наши данные для БД */
    private final Properties properties;

    /* Переменная connection для хранения подключения к базе данных. */
    private Connection connection;

    public WordStore(Properties properties) {
        this.properties = properties;
        initConnection();
        initScheme();
        initWords();
    }

    /* initConnection(): устанавливает соединение с базой данных. */
    private void initConnection() {
        LOGGER.info("Подключение к базе данных слов");
        try {
            connection = DriverManager.getConnection(
                    properties.getProperty("url"),
                    properties.getProperty("username"),
                    properties.getProperty("password")
            );
        } catch (SQLException e) {
            LOGGER.error("Не удалось выполнить операцию: { Возможная проблема вы подключены к базе данных }", e.getCause());
            throw new IllegalStateException();
        }
    }

    /* создает схему таблицы слов, используя SQL-скрипт. */
    private void initScheme() {
        LOGGER.info("Создание схемы таблицы слов");
        try (Statement statement = connection.createStatement()) {
            /* Создает таблицу dictionary если её нет с id и текстовым значением */
            String sql = Files.readString(Path.of("db/scripts", "dictionary.sql"));
            statement.execute(sql);
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
    }

    /* initWords(): заполняет таблицу словами из SQL-скрипта. */
    private void initWords() {
        LOGGER.info("Заполнение таблицы слов");
        try (Statement statement = connection.createStatement()) {
            /* Загружает 1000 слов в таблицу */
            String sql = Files.readString(Path.of("db/scripts", "words.sql"));
            statement.executeLargeUpdate(sql);
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
    }

    /* save(Word model): сохраняет новое слово в базу данных и возвращает его с установленным ID. */
    @Override
    public Word save(Word model) {
        LOGGER.info("Добавление слова в базу данных");
        String sql = "insert into dictionary(word) values(?);";
        try (PreparedStatement  statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, model.getValue());
            statement.executeUpdate();
            ResultSet key = statement.getGeneratedKeys();
            if (key.next()) {
                model.setId(key.getInt(1));
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
        return model;
    }

    /* findAll(): извлекает все слова из базы данных и возвращает их в виде списка. */
    @Override
    public List<Word> findAll() {
        LOGGER.info("Загрузка всех слов");
        String sql = "select * from dictionary";
        ArrayList<Word> words = new ArrayList<Word>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet selection = statement.executeQuery();
            while (selection.next()) {
                words.add(new Word(
                        selection.getInt("id"),
                        selection.getString("word")
                ));
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось выполнить операцию: { }", e.getCause());
            throw new IllegalStateException();
        }
        return words;
    }

    /* close(): закрывает соединение с базой данных. */
    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

}

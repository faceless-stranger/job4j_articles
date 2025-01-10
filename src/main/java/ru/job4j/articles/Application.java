package ru.job4j.articles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.articles.service.SimpleArticleService;
import ru.job4j.articles.service.generator.RandomArticleGenerator;
import ru.job4j.articles.store.ArticleStore;
import ru.job4j.articles.store.WordStore;
import java.io.InputStream;
import java.util.Properties;

public class Application {

    /* Создание приватного статического не изменяемого поля
   Logger используется для ведения логов (записи событий программы) и создается через LoggerFactory. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class.getSimpleName());

    /* Объявление константы TARGET_COUNT с значением 1_000_000. */
    public static final int TARGET_COUNT = 500_000;

    public static void main(String[] args) {
        /* Подгрузка properties для дальнейшей работы с БД */
        Properties properties = loadProperties();
        /* Подключение к базе данных, cоздание схемы таблицы слов, заполняет таблицу словами и передаем наши настройки БД */
        WordStore wordStore = new WordStore(properties);
        /* Подключение к базе данных, cоздание таблицы статей */
        ArticleStore articleStore = new ArticleStore(properties);
        /* Этот класс, отвечает за генерацию случайных статей, используя слова из базы данных. */
        RandomArticleGenerator articleGenerator = new RandomArticleGenerator();
        /* Создается объект SimpleArticleService — сервис, отвечающий за логику генерации статей. */
        SimpleArticleService articleService = new SimpleArticleService(articleGenerator);

        articleService.generate(wordStore, TARGET_COUNT, articleStore);
    }

    /*Метод loadProperties() используется для загрузки конфигурационных настроек приложения из файла application.properties,
    чтобы получить необходимые параметры для подключения к базе данных  */
    private static Properties loadProperties() {
        /* Логируем сообщение о начале загрузки настроек приложения */
        LOGGER.info("Загрузка настроек приложения");
        /* Создаем новый объект Properties для хранения загруженных данных из файла */
        Properties properties = new Properties();
        /* Используем загрузчик класса Application для получения ресурса (файла application.properties),
           чтобы гарантировать правильный доступ к файлу независимо от среды выполнения. */
        try (InputStream in = Application.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(in);
        } catch (Exception e) {
            LOGGER.error("Не удалось загрузить настройки. { }", e.getCause());
            throw new IllegalStateException();
        }
        return properties;
    }

}

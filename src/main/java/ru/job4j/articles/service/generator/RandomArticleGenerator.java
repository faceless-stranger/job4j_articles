package ru.job4j.articles.service.generator;

import ru.job4j.articles.model.Article;
import ru.job4j.articles.model.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RandomArticleGenerator implements ArticleGenerator {
    /* Класс RandomArticleGenerator создает случайные статьи из списка слов.
    Он перемешивает список слов, объединяет их в строку и возвращает объект статьи с этим текстом. */
    @Override
    public Article generate(List<Word> words) {
        /* Перемешиваем список слов */
        Collections.shuffle(words);
        /* Собираем перемешанные слова в строку, разделяя их пробелами */
        String content = words.stream()
                .limit(100) /* Ограничиваем, например, до 100 слов */
                .map(Word::getValue) /* Получаем значение каждого слова */
                .collect(Collectors.joining(" ")); /* Объединяем слова через пробел */
        /* Создаем и возвращаем объект Article со сгенерированным содержанием */
        return new Article(content);
    }
}

package grabber;

import grabber.utils.DateTimeParser;
import grabber.utils.HabrCareerDateTimeParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.time.LocalDateTime;


public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);
    private static final int PAGES = 5;

    public static void main(String[] args) throws IOException {
        for (int index = 1; index <= PAGES; index++) {
            System.out.println("Страница " + index);
            Connection connection = Jsoup.connect(PAGE_LINK + index);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Element dateElement = row.select(".vacancy-card__date").first();
                Element datelinkElement = dateElement.child(0);
                HabrCareerDateTimeParser dateTimeParser = new HabrCareerDateTimeParser();
                LocalDateTime date = dateTimeParser.parse(datelinkElement.attr("datetime"));
                String vacancyName = titleElement.text();
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));

                String description = null;
                try {
                    description = retrieveDescription(link);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.printf("%s %s %s%n", vacancyName, link, date);
                System.out.printf(" Описание вакансии%n %s%n", description);
            });
        }
    }

    private static String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Elements element =   document.select(".style-ugc");
        return element.text();
    }
}
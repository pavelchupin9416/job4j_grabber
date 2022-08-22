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
import java.util.ArrayList;
import java.util.List;


public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);
    private static final int PAGES = 5;
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }


    public static void main(String[] args) throws IOException {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        habrCareerParse.list(PAGE_LINK).forEach(System.out::println);
    }

    @Override
    public List<Post> list(String link) throws IOException {
        List<Post> posts = new ArrayList<>();
        for (int index = 1; index <= PAGES; index++)  {
            Connection connection = Jsoup.connect(link + index);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Element dateElement = row.select(".vacancy-card__date").first();
                Element datelinkElement = dateElement.child(0);
                LocalDateTime date = dateTimeParser.parse(datelinkElement.attr("datetime"));
                String vacancyName = titleElement.text();
                String linkVacancy = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                String description = null;
                try {
                    description = retrieveDescription(linkVacancy);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                posts.add(new Post(vacancyName, linkVacancy, description, date));
            }); }
        return posts;
    }

        private String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Elements element =   document.select(".style-ugc");
        return element.text();
    }
}


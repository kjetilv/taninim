package mediaserver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import mediaserver.media.Masada;
import mediaserver.media.MasadaRef;
import mediaserver.util.IO;
import mediaserver.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public final class MasadaParser {

    public static void main(String[] args) {
        Collection<MasadaRef> sorted = getHtmlMasadaRefs();
        Masada masada = new Masada(3).withNotes(sorted);
        sorted.forEach(System.out::println);
    }

    @Nonnull
    public static Collection<MasadaRef> getHtmlMasadaRefs() {
        Collection<Path> paths = IO.paths("wiki.masada.world");
        return paths.stream().map(path -> {
                try {
                    return Pair.of(
                        path,
                        Jsoup.parse(path.toFile(), StandardCharsets.UTF_8.name()));
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read " + path, e);
                }
            }
        ).map(pair -> {
            try {
                return parse(pair);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to parse " + pair, e);
            }
        }).collect(Collectors.toList());
    }

    @Nonnull
    private static MasadaRef parse(Pair<Path, ? extends Document> pair) {
        String refs = Optional.ofNullable(pair.getT2().getElementsByTag("h3"))
            .filter(e -> !e.isEmpty())
            .map(Elements::first)
            .map(element ->
                element.getElementsByTag("span"))
            .map(Elements::first)
            .map(Element::text)
            .orElseThrow(() ->
                new IllegalStateException("Cannot find ref"));
        List<String> notes = Optional.ofNullable(pair.getT2().getElementsMatchingOwnText("Notes"))
            .map(Elements::first)
            .map(Element::parent)
            .map(Element::parent)
            .map(Element::childNodes)
            .stream()
            .flatMap(List::stream)
            .filter(e -> e.nodeName().equalsIgnoreCase("ul"))
            .filter(e -> e.childNodes().stream()
                .filter(c -> c.nodeName().equalsIgnoreCase("li"))
                .noneMatch(c -> c.childNodes().stream().anyMatch(li ->
                    containsText(li, "have been recorded once", "have never been recorded") ||
                        isLink(li, "youtube", "youtu.be", "spotify", "bandcamp"))))
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .map(Element::text)
            .collect(Collectors.toList());
        int book = refs.toLowerCase().startsWith("book one") ? 1
            : refs.startsWith("book two") ? 2
                : 3;
        int number = Integer.parseInt(refs.substring(refs.lastIndexOf('#') + 1).trim());
        String pathname = pair.getT1().toString();
        String suffix = pathname.substring(pathname.lastIndexOf('=') + 1);
        String name = suffix.substring(0, suffix.lastIndexOf('.'));
        return new MasadaRef(book, number, name, notes);
    }

    private static boolean isLink(Node node, String... sites) {
        return nodes(node).anyMatch(e ->
            e.nodeName().equalsIgnoreCase("a") &&
                e.attributes().hasKey("href") &&
                Arrays.stream(sites).anyMatch(site -> e.attributes().get("href").contains(site)));
    }

    private static boolean containsText(Node node, String... texts) {
        return nodes(node)
            .filter(TextNode.class::isInstance)
            .map(TextNode.class::cast)
            .anyMatch(e -> Arrays.stream(texts).anyMatch(text -> e.text().contains(text)));
    }

    private static Stream<Node> nodes(Node e) {
        return e.childNodes().isEmpty() ? Stream.of(e)
            : Stream.concat(Stream.of(e), e.childNodes().stream().flatMap(MasadaParser::nodes));
    }
}

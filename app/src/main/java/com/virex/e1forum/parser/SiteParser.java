package com.virex.e1forum.parser;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.virex.e1forum.db.entity.Forum;
import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.db.entity.User;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 корявый парсер сайта

 так получилось что список форумов полный в старой версии сайта
 а список топиков и посты удобней парсить в мобильной версии: есть аватарки, методы отправки в ЛК и т.д.
 */
public class SiteParser {

    public enum ParseStatus {
        START,
        INPROCESS,
        END
    }

    public enum SiteType {
        PARCE_MOBILE_SITE,
        PARCE_OLD_SITE
    }

    public interface ParserListener {
        void onParse(Forum forum, ParseStatus parseStatus);
        void onParse(Topic topic, ParseStatus parseStatus);
        void onParse(Post post, ParseStatus parseStatus);
        void onParse(User user, ParseStatus parseStatus);
    }

    public static void parseForums(SiteType siteType, String text, @NonNull ParserListener parserListener){

        switch (siteType){
            case PARCE_OLD_SITE:
                parserListener.onParse((Forum) null, ParseStatus.START);
                Document document = Jsoup.parse(text);
                Elements allA=document.select("a");
                for(Element element:allA){
                    if (element.attr("href").contains("list.php?f=")){
                        Forum forum=new Forum();
                        forum.title=element.text();
                        //forum.id = Integer.parseInt(element.attr("href").replace("list.php?f=", ""));
                        String href=element.attr("href");
                        forum.id = Integer.parseInt(href.substring(href.lastIndexOf('=')+1));
                        parserListener.onParse(forum, ParseStatus.INPROCESS);
                    }
                }
                parserListener.onParse((Forum) null, ParseStatus.END);
                break;
        }

    }

    public static void parseTopics(SiteType siteType, String text, int forum_id, @NonNull ParserListener parserListener){

        String today = new SimpleDateFormat("dd-MM-yyyy",Locale.ENGLISH).format(new Date());
        String yesterday = new SimpleDateFormat("dd-MM-yyyy",Locale.ENGLISH).format(new Date(new Date().getTime() - 24*3600*1000));

        switch (siteType){
            case PARCE_MOBILE_SITE:
                parserListener.onParse((Topic) null, ParseStatus.START);
                Document document = Jsoup.parse(text);
                Elements allItems=document.getElementsByClass("themes-body-item");
                for(Element element:allItems){
                    Topic topic=new Topic();
                    topic.forum_id=forum_id;

                    // /f/152/1625025/
                    String tmp=element.getElementsByClass("title-cont").select("a").attr("href");
                    String[] list=tmp.split("/");
                    if (list.length>0)
                        topic.id=Integer.parseInt(list[list.length-1]);

                    topic.title=element.getElementsByClass("title-cont").text();
                    topic.titleSearch=topic.title.toLowerCase();
                    topic.comments=element.getElementsByClass("comments-wrap").text();
                    topic.userName=element.getElementsByClass("username-cont").html();
                    topic.userSearch=element.getElementsByClass("username-cont").text().toLowerCase();
                    topic.lastComment=element.getElementsByClass("comment-date").text();
                    topic.isClosed=!element.getElementsByClass("lock-ico").isEmpty();
                    topic.isAttathed=!element.getElementsByClass("attach-ico").isEmpty();

                    if (!element.getElementsByClass("tpages-cont").isEmpty()) {
                        String txt=element.getElementsByClass("tpages-cont").text().trim();
                        txt = txt.replaceAll("[\\D]", "");
                        int pagesCount=Integer.parseInt(txt);
                        topic.pagesCount =pagesCount;
                    }

                    //если в тексте есть "Сегодня в" то подставляем сегодняшнюю дату
                    //если в тексте есть "Вчера в" то подставляем вчерашнюю дату
                    if (topic.lastComment.toUpperCase().contains("Сегодня в".toUpperCase()) || topic.lastComment.toUpperCase().contains("Вчера в".toUpperCase())) {
                        String txt=topic.lastComment.toUpperCase().replace("Сегодня в".toUpperCase(),today);
                        txt=txt.toUpperCase().replace("Вчера в".toUpperCase(),yesterday);
                        try {
                            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US);
                            Calendar parsed=Calendar.getInstance();
                            parsed.setTime(format.parse(txt));

                            topic.lastmod = parsed.getTimeInMillis();
                        }catch(ParseException e){
                        }
                    } else {
                        SimpleDateFormat format=null;
                        //разделяем дату на части
                        String[] txtDate=topic.lastComment.split(" ");
                        //17:15 29.11.2004
                        if (txtDate[1].length()==10)
                            format = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.US);

                        //18:30 19.08
                        if (txtDate[1].length()==5)
                            format = new SimpleDateFormat("HH:mm dd.MM", Locale.US);

                        try {
                            String txt=topic.lastComment;

                            Calendar parsed=Calendar.getInstance();
                            parsed.setTime(format.parse(txt));
                            ////18:30 19.08
                            if (txtDate[1].length()==5)
                                parsed.set(Calendar.YEAR,Calendar.getInstance().get(Calendar.YEAR));


                            topic.lastmod = parsed.getTimeInMillis();
                        }catch(Exception e){
                        }
                    }

                    parserListener.onParse(topic, ParseStatus.INPROCESS);
                }
                parserListener.onParse((Topic) null, ParseStatus.END);
                break;
        }
    }

    public static void parsePosts(SiteType siteType, String text, int forum_id, int topic_id, @NonNull ParserListener parserListener ){

        switch (siteType) {
            case PARCE_MOBILE_SITE:
                parserListener.onParse((Post) null, ParseStatus.START);
                Document document = Jsoup.parse(text);

                //переделываем все <span class="message-smiles__elem mark-head-smiles__elem20">:beer:</span> в картинки
                //document.getElementsByClass("message-smiles__elem").tagName("img").attr("href", "your-source-here");
                Elements icons=document.getElementsByClass("message-smiles__elem");
                for(Element icon:icons) {
                    icon.attr("src", "local="+icon.text()).tagName("img");
                }

                Elements allItems=document.getElementsByClass("theme-item");

                for(Element element:allItems) {
                    Post post = new Post();

                    post.id=Integer.valueOf(element.attr("id"));
                    post.forum_id=forum_id;
                    post.topic_id=topic_id;

                    //post.user=element.getElementsByClass("username-cont").html();
                    Elements nickTags=element.getElementsByClass("user");
                    if (nickTags.size()==0) nickTags=element.getElementsByClass("moder");
                    String userNick = nickTags.first().text();

                    User user = new User();

                    try {
                        String avatar = element.getElementsByClass("avatar").attr("style");
                        if (avatar.length()>0) {
                            avatar = avatar.substring(avatar.indexOf("https://"), avatar.indexOf("')"));
                            user.avatarURL = avatar;

                            post.userAvatarURL = avatar;

                            /*
                            //загрузка аватара в массив байт и сохранение в базу - не срослось
                            Bitmap img=Glide.with(context).asBitmap().load(avatar).submit().get();
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            img.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            user.avatarIMG = stream.toByteArray();
                            */
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    String userLink = nickTags.first().attr("href");
                    user.link=userLink;

                    try {
                        Uri uri = Uri.parse(userLink);
                        user.user_id = Integer.parseInt(uri.getQueryParameter("user"));
                    } catch (Exception e){
                    }

                    user.nick=userNick;

                    Elements userActions=element.select(".user-option-list").select(".user-option-item");
                    for(Element act:userActions){
                        String actionLink=act.select("a").attr("href");
                        if (actionLink.contains("show_message_page"))
                            user.actionLK=actionLink;

                        if (actionLink.contains("showEmailPage"))
                            user.actionMail=actionLink;

                    }
                    //уведомляем о распарсеном пользователе
                    parserListener.onParse(user, ParseStatus.INPROCESS);

                    //post.user=userNick;
                    //if (user.user_id>0)
                        //post.user=String.format(Locale.ENGLISH,"<b><a href=\"%s\">%s</a></b>","user:".concat(userNick),userNick);
                    //else
                        //post.user=String.format(Locale.ENGLISH,"<a href=\"%s\">%s</a>","user:".concat(userNick),userNick);
                    post.user=userNick;
                    post.userSearch=userNick.toLowerCase();

                    Elements tags=element.getElementsByClass("theme-text");

                    //rename div -> p
                    //tags.tagName("p");
                    element.getElementsByTag("script").remove();

                    for(Element el:tags){
                        el.getElementsByClass("quote-w").tagName("blockquote");
                        el.getElementsByClass("reply-cont").tagName("p");
                        if (el.getElementsByClass("reply-cont").size()>0)
                            el.getElementsByClass("reply-cont").first().appendElement("br");
                        el.getElementsByClass("reply-cont").tagName("font").attr("color","#ff0000");

                        el.getElementsByClass("itemGO-block").remove();
                    }

                    post.text=tags.html();
                    post.textSearch=tags.html().toLowerCase();

                    Elements votes=element.getElementsByClass("vote-res").select("span");

                    if (votes.size()==0){
                        post.carmaPlus=0;
                        post.carmaMinus=0;
                    } else {
                        post.carmaPlus=Integer.valueOf(votes.get(0).text());
                        post.carmaMinus=Integer.valueOf(votes.get(1).text());
                    }

                    //если нет тегов - то плюсомёт отключен
                    if (element.select(".vote-up.btn-active").isEmpty())
                        //post.disableCarma=true;
                        post.disableCarma=false; //--мы не залогинены поэтому плюсомет игнорируем
                    else {
                        //иначе - кнопки есть значит плюсомет включен
                        post.disableCarma=false;

                        //но если стоит стиль display:none - то всё же плюсомет отключен
                        Elements ell=element.select(".vote-up.btn-active");
                        if (ell.attr("style").replace(" ","").contains("display:none")) //<-- точное соответствие [display: none;] попробовал без пробела
                            post.disableCarma=true;

                    }

                    try {
                        //HH - от 0 до 24
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH);
                        String time = element.select("time").attr("datetime");
                        Calendar parsed = Calendar.getInstance();
                        parsed.setTime(format.parse(time));
                        post.lastmod = parsed.getTimeInMillis();
                    }catch(Exception e){
                    }

                    //уведомляем о распарсенном посте
                    parserListener.onParse(post, ParseStatus.INPROCESS);
                }
                parserListener.onParse((Post) null, ParseStatus.END);
                break;
        }
    }

    public static String extractTagText(String html, String tagName){
        Document document = Jsoup.parse(html);
        return document.getElementsByClass(tagName).text();
    }

    public static String clearTags(String html){
        if (TextUtils.isEmpty(html))
            return null;
        Document document = Jsoup.parse(html);
        return document.text();
    }

    public static HashMap<String, String> extractFormValues(String html){
        HashMap<String, String> result= new HashMap<>();

        Document document = Jsoup.parse(html);
        Element postform = document.getElementById("postform");

        if (postform==null) return result;

        Elements inputElements = postform.getElementsByTag("input");

        for (Element inputElement : inputElements) {
            String key = inputElement.attr("name");
            String value = inputElement.attr("value");
            result.put(key,value);
        }
        return result;
    }

    public static HashMap<String, String> extractTableValues(String html){
        HashMap<String, String> result= new HashMap<>();

        Document document = Jsoup.parse(html);

        Element table = document.select("table").first();
        if (table==null) return result;

        for (Element row : table.select("tr")) {
            Elements tds = row.select("td");

            if (tds.size() == 2) {
                String key = tds.get(0).text();
                String value = tds.get(1).text();
                result.put(key,value);
            }

        }
        return result;
    }

    public static String extractQuotedString(String text){
        Pattern p = Pattern.compile("'([^']*)'");
        Matcher m = p.matcher(text);
        while (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static String convertBBCodeToE1(String text){
        text=text.replace("[b]","<b>").replace("[/b]","</b>");
        text=text.replace("[i]","<i>").replace("[/i]","</i>");
        text=text.replace("[s]","<s>").replace("[/s]","</s>");
        text=text.replace("[u]","<u>").replace("[/u]","</u>");

        //уникальный для е1 тег nickname
        text=replaceQuote(text);

        return text;
    }

    //[quote=rib65] -> [quote][nickname]rib65[/nickname]
    private static String replaceQuote(String text){
        StringBuffer result=new StringBuffer(text);

        //поиск в html тегах
        Pattern word = Pattern.compile("\\[/?(?:quote|code|img|color|size)*?.*?]",Pattern.CASE_INSENSITIVE);
        Matcher match = word.matcher(text);

        while (match.find()) {
            //исходное найденное слово (написанное например капсом)
            String src=match.group();
            //удаляем кавычки
            src=src.replace("[","").replace("]","");

            if (src.contains("quote=")){
                String[] items=src.split("=");

                src=String.format("[quote][nickname]%s[/nickname]",items[1]);

                result.replace(match.start(),match.end(), src);
            }
        }
        return result.toString();
    }

    public static String URLEncodeString(String source){
        source=source.replace("UTF-8","windows-1251");
        try {
            source= URLEncoder.encode(source, "windows-1251");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return source;
    }

}

package com.virex.e1forum.parser;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.virex.e1forum.db.entity.Forum;
import com.virex.e1forum.db.entity.Post;
import com.virex.e1forum.db.entity.Topic;
import com.virex.e1forum.db.entity.User;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*
 корявый парсер сайта

 так получилось что список форумов полный в старой версии сайта
 а список топиков и посты удобней парсить в мобильной версии: есть аватарки, методы отправки в ЛК и т.д.
 */
public class SiteParser {

    public enum SiteType {
        PARCE_MOBILE_SITE,
        PARCE_OLD_SITE
    }

    public interface ParserListener {
        void onParse(Forum forum);
        void onParse(Topic topic);
        void onParse(Post post);
        void onParse(User user);
    }

    public static void parseForums(SiteType siteType, String text, @NonNull ParserListener parserListene){

        switch (siteType){
            case PARCE_OLD_SITE:
                Document document = Jsoup.parse(text);
                Elements allA=document.select("a");
                for(Element element:allA){
                    if (element.attr("href").contains("/talk/forum/list.php?f=")){
                        Forum forum=new Forum();
                        forum.title=element.text();
                        forum.id =Integer.parseInt(element.attr("href").replace("/talk/forum/list.php?f=",""));
                        parserListene.onParse(forum);
                    }
                }
                break;
        }

    }

    public static void parseTopics(SiteType siteType, String text, int forum_id, @NonNull ParserListener parserListene){

        String today = new SimpleDateFormat("dd-MM-yyyy",Locale.ENGLISH).format(new Date());
        String yesterday = new SimpleDateFormat("dd-MM-yyyy",Locale.ENGLISH).format(new Date(new Date().getTime() - 24*3600*1000));

        switch (siteType){
            case PARCE_MOBILE_SITE:
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
                    topic.comments=element.getElementsByClass("comments-wrap").text();
                    topic.userName=element.getElementsByClass("username-cont").html();
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

                    parserListene.onParse(topic);
                }
                break;
        }
    }

    public static void parsePosts(SiteType siteType, String text, int forum_id, int topic_id, @NonNull ParserListener parserListene ){

        switch (siteType) {
            case PARCE_MOBILE_SITE:
                Document document = Jsoup.parse(text);
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
                        user.idForum = Integer.parseInt(uri.getQueryParameter("user"));
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
                    parserListene.onParse(user);

                    //post.user=userNick;
                    //if (user.idForum>0)
                        //post.user=String.format(Locale.ENGLISH,"<b><a href=\"%s\">%s</a></b>","user:".concat(userNick),userNick);
                    //else
                        post.user=String.format(Locale.ENGLISH,"<a href=\"%s\">%s</a>","user:".concat(userNick),userNick);

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
                    //post.text=element.getElementsByClass("theme-text").html();

                    Elements votes=element.getElementsByClass("vote-res").select("span");

                    if (votes.size()==0){
                        post.carmaPlus=0;
                        post.carmaMinus=0;
                    } else {
                        post.carmaPlus=Integer.valueOf(votes.get(0).text());
                        post.carmaMinus=Integer.valueOf(votes.get(1).text());
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
                    parserListene.onParse(post);
                }
                break;
        }
    }
}

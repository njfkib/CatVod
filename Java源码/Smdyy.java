package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.okhttp.OKCallBack;
import com.github.catvod.utils.okhttp.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;


/** 可当做影视工厂类的模板，蓝光，追剧喵，神马影院
 * 神马影院
 * <p>
 * Author: baddychen
 */
public class Smdyy extends Spider {
    private static final String siteUrl = "https://www.smdyy.cc";

    /**
     * 播放源配置
     */
    private JSONObject playerConfig;
    /**
     * 筛选配置
     */
    private JSONObject filterConfig;

    private Pattern regexCategory = Pattern.compile("/list/(\\d+).html");
    private Pattern regexVid = Pattern.compile("/kan/(\\d+).html");
    private Pattern regexPlay = Pattern.compile("/play/(\\d+)-(\\d+)-(\\d+).html");
    private Pattern regexPage = Pattern.compile("/show/(\\S+).html");

    @Override
    public void init(Context context) {
        super.init(context);
        try {
            playerConfig = new JSONObject("{\"duoduozy\":{\"show\":\"高清线路\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"sohu\":{\"show\":\"优选7\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"qq\":{\"show\":\"优选3\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"bilibili\":{\"show\":\"bilibili\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"youku\":{\"show\":\"优选1\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"qiyi\":{\"show\":\"优选2\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"letv\":{\"show\":\"优选6\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"xigua\":{\"show\":\"西瓜视频\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"mgtv\":{\"show\":\"优选4\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"tkm3u8\":{\"show\":\"备用\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"},\"pptv\":{\"show\":\"优选5\",\"or\":999,\"ps\":\"0\",\"parse\":\"https://player.6080kan.cc/player/play.php?url=\"}}");
            filterConfig = new JSONObject("{}");
        } catch (JSONException e) {
            SpiderDebug.log(e);
        }
    }

    /**
     * 爬虫headers
     *
     * @param url
     * @return
     */
    protected HashMap<String, String> getHeaders(String url) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("method", "GET");
        if (!TextUtils.isEmpty(url)) {
            headers.put("Referer", url);
        }
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        return headers;
    }

    /**
     * 获取分类数据 + 首页最近更新视频列表数据
     *
     * @param filter 是否开启筛选 关联的是 软件设置中 首页数据源里的筛选开关
     * @return
     */
    @Override
    public String homeContent(boolean filter) {
        try {
            Document doc = Jsoup.parse(OkHttpUtil.string(siteUrl, getHeaders(siteUrl)));
            // 分类节点
            Elements elements = doc.select("ul.stui-header__menu > li a");
            JSONArray classes = new JSONArray();
            for (Element ele : elements) {
                String name = ele.text();
                boolean show = true;
                if (filter) {
                    show = name.equals("电影") ||
                            name.equals("电视剧") ||
                            name.equals("动漫") ||
                            name.equals("日韩剧") ||
                            name.equals("欧美剧");
                }
                if (show) {
                    Matcher mather = regexCategory.matcher(ele.attr("href"));
                    if (!mather.find())
                        continue;
                    String id = mather.group(1).trim();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type_id", id);
                    jsonObject.put("type_name", name);
                    classes.put(jsonObject);
                }
            }
            JSONObject result = new JSONObject();
            if (filter) {
                result.put("filters", filterConfig);
            }
            result.put("class", classes);
            try {
                Elements list = doc.select("div.stui-pannel div.stui-pannel__bd ul.stui-vodlist li div.stui-vodlist__box");
                JSONArray videos = new JSONArray();
                for (int i = 0; i < list.size(); i++) {
                    Element vod = list.get(i);
                    Matcher matcher = regexVid.matcher(vod.selectFirst(".stui-vodlist__thumb").attr("href"));
                    if (!matcher.find())
                        continue;
                    String title = vod.selectFirst(".stui-vodlist__thumb").attr("title");
                    String cover = vod.selectFirst(".stui-vodlist__thumb").attr("data-original");
                    String remark = vod.selectFirst("span.pic-text").text();
                    if (!TextUtils.isEmpty(cover) && !cover.startsWith("http")) {
                        cover = siteUrl + cover;
                    }
                    String id = matcher.group(1);
                    JSONObject v = new JSONObject();
                    v.put("vod_id", id);
                    v.put("vod_name", title);
                    v.put("vod_pic", cover);
                    v.put("vod_remarks", remark);
                    videos.put(v);
                }
                result.put("list", videos);
            } catch (Exception e) {
                SpiderDebug.log(e);
            }
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    /**
     * 获取分类信息数据
     *
     * @param tid    分类id
     * @param pg     页数
     * @param filter 同homeContent方法中的filter
     * @param extend 筛选参数{k:v, k1:v1}
     * @return
     */
    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        try {
            String[] urlParams = new String[]{"", "", "", "", "", "", "", "", "", "", "", ""};
            urlParams[0] = tid;
            urlParams[8] = pg;
            if (extend != null && extend.size() > 0) {
                for (Iterator<String> it = extend.keySet().iterator(); it.hasNext(); ) {
                    String key = it.next();
                    String value = extend.get(key);
                    if (value.trim().length() == 0)
                        continue;
                    urlParams[Integer.parseInt(key)] = URLEncoder.encode(value);
                }
            }
            // 获取分类数据的url
            String url = siteUrl + "/show/" + TextUtils.join("-", urlParams) + "/";
            String html = OkHttpUtil.string(url, getHeaders(url));
            Document doc = Jsoup.parse(html);
            JSONObject result = new JSONObject();
            int pageCount = 0;
            int page = -1;

            Elements pageInfo = doc.select(".stui-page__item li");
            if (pageInfo.size() == 0) {
                page = Integer.parseInt(pg);
                pageCount = page;
            } else {
                for (int i = 0; i < pageInfo.size(); i++) {
                    Element li = pageInfo.get(i);
                    Element a = li.selectFirst("a");
                    if (a == null)
                        continue;
                    String name = a.text();
                    if (page == -1 && li.hasClass("active")) {
                        Matcher matcher = regexPage.matcher(a.attr("href"));
                        if (matcher.find()) {
                            page = Integer.parseInt(matcher.group(1).split("-")[8]);
                        } else {
                            page = 0;
                        }
                    }
                    if (name.equals("尾页")) {
                        Matcher matcher = regexPage.matcher(a.attr("href"));
                        if (matcher.find()) {
                            pageCount = Integer.parseInt(matcher.group(1).split("-")[8]);
                        } else {
                            pageCount = 0;
                        }
                        break;
                    }
                }
            }

            JSONArray videos = new JSONArray();
            if (!html.contains("没有找到您想要的结果哦")) {
                Elements list = doc.select("ul.stui-vodlist li div.stui-vodlist__box");
                for (int i = 0; i < list.size(); i++) {
                    Element vod = list.get(i);
                    String title = vod.selectFirst(".stui-vodlist__thumb").attr("title");
                    String cover = vod.selectFirst(".stui-vodlist__thumb").attr("data-original");
                    if (!TextUtils.isEmpty(cover) && !cover.startsWith("http")) {
                        cover = siteUrl + cover;
                    }
                    String remark = vod.selectFirst("span.pic-text").text();
                    Matcher matcher = regexVid.matcher(vod.selectFirst(".stui-vodlist__thumb").attr("href"));
                    if (!matcher.find())
                        continue;
                    String id = matcher.group(1);
                    JSONObject v = new JSONObject();
                    v.put("vod_id", id);
                    v.put("vod_name", title);
                    v.put("vod_pic", cover);
                    v.put("vod_remarks", remark);
                    videos.put(v);
                }
            }
            result.put("page", page);
            result.put("pagecount", pageCount);
            result.put("limit", 48);
            result.put("total", pageCount <= 1 ? videos.length() : pageCount * 48);

            result.put("list", videos);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }


    private static String Regex(Pattern pattern, String content) {
        if (pattern == null) {
            return content;
        }
        try {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return content;
    }
    /**
     * 视频详情信息
     *
     * @param ids 视频id
     * @return
     */
    @Override
    public String detailContent(List<String> ids) {
        try {
            // 视频详情url
            String url = siteUrl + "/kan/" + ids.get(0) + ".html";
            Document doc = Jsoup.parse(OkHttpUtil.string(url, getHeaders(url)));
            JSONObject result = new JSONObject();
            JSONObject vodList = new JSONObject();

            // 取基本数据
            String cover = doc.selectFirst("a.pic img").attr("data-original");
            if (!TextUtils.isEmpty(cover) && !cover.startsWith("http")) {
                cover = siteUrl + cover;
            }
            String title = doc.selectFirst("div.stui-content__detail h1.title").text();
            String category = "", area = "", year = "", remark = "", director = "", actor = "", desc = "";
            Elements data = doc.select("p.data");
            desc = doc.selectFirst("span.detail-content").text().trim();
            category = Regex(Pattern.compile("类型：(\\S+)"), data.get(0).text());
            area = Regex(Pattern.compile("地区：(\\S+)"), data.get(0).text());
            year = Regex(Pattern.compile("年份：(\\S+)"), data.get(0).text());
            actor = Regex(Pattern.compile("主演：(\\S+)"), data.get(1).text());
            director = Regex(Pattern.compile("导演：(\\S+)"), data.get(1).text());

            vodList.put("vod_id", ids.get(0));
            vodList.put("vod_name", title);
            vodList.put("vod_pic", cover);
            vodList.put("type_name", category);
            vodList.put("vod_year", year);
            vodList.put("vod_area", area);
            vodList.put("vod_remarks", remark);
            vodList.put("vod_actor", actor);
            vodList.put("vod_director", director);
            vodList.put("vod_content", desc);

            Map<String, String> vod_play = new TreeMap<>(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    try {
                        int sort1 = playerConfig.getJSONObject(o1).getInt("or");
                        int sort2 = playerConfig.getJSONObject(o2).getInt("or");

                        if (sort1 == sort2) {
                            return 1;
                        }
                        return sort1 - sort2 > 0 ? 1 : -1;
                    } catch (JSONException e) {
                        SpiderDebug.log(e);
                    }
                    return 1;
                }
            });

            // 取播放列表数据
            Elements sources = doc.select("div.stui-vodlist__head");
            //Elements sourceList = doc.select("div.stui-vodlist__head > ul.stui-content__playlist");

            for (int i = 0; i < sources.size(); i++) {
                Element source = sources.get(i);
                String sourceName = source.selectFirst("h3").text().trim();
                boolean found = false;
                for (Iterator<String> it = playerConfig.keys(); it.hasNext(); ) {
                    String flag = it.next();
                    if (playerConfig.getJSONObject(flag).getString("show").equals(sourceName)) {
                        //sourceName = flag;
                        sourceName = playerConfig.getJSONObject(flag).getString("show");
                        found = true;
                        break;
                    }
                }
                if (!found)
                    continue;
                String playList = "";
                Elements playListA = source.select("ul.stui-content__playlist > li > a");
                List<String> vodItems = new ArrayList<>();

                for (int j = 0; j < playListA.size(); j++) {
                    Element vod = playListA.get(j);
                    Matcher matcher = regexPlay.matcher(vod.attr("href"));
                    if (!matcher.find())
                        continue;
                    String playURL = matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3);
                    vodItems.add(vod.text() + "$" + playURL);
                }
                if (vodItems.size() > 0)
                    playList = TextUtils.join("#", vodItems);

                if (playList.length() == 0)
                    continue;

                vod_play.put(sourceName, playList);
            }

            if (vod_play.size() > 0) {
                String vod_play_from = TextUtils.join("$$$", vod_play.keySet());
                String vod_play_url = TextUtils.join("$$$", vod_play.values());
                vodList.put("vod_play_from", vod_play_from);
                vodList.put("vod_play_url", vod_play_url);
            }
            JSONArray list = new JSONArray();
            list.put(vodList);
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    /**
     * 获取视频播放信息
     *
     * @param flag     播放源
     * @param id       视频id
     * @param vipFlags 所有可能需要vip解析的源
     * @return
     */

    private final Pattern urlt = Pattern.compile("\"url\": *\"([^\"]*)\",");
    private final Pattern token = Pattern.compile("\"token\": *\"([^\"]*)\"");
    private final Pattern vkey = Pattern.compile("\"vkey\": *\"([^\"]*)\",");

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            JSONObject headers = new JSONObject();
            headers.put("Referer", " https://www.smdyy.cc/");
            headers.put("User-Agent", " Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");
            headers.put("Accept", " text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            headers.put("Accept-Language", " zh-CN,zh;q=0.9,en-GB;q=0.8,en-US;q=0.7,en;q=0.6");
            headers.put("Accept-Encoding", " gzip, deflate");
            String url = siteUrl + "/play/" + id + ".html";
            Elements allScript = Jsoup.parse(OkHttpUtil.string(url, getHeaders(url))).select("script");
            JSONObject result = new JSONObject();
            for (int i = 0; i < allScript.size(); i++) {
                String scContent = allScript.get(i).html().trim();
                if (scContent.startsWith("var player_")) {
                    JSONObject player = new JSONObject(scContent.substring(scContent.indexOf('{'), scContent.lastIndexOf('}') + 1));
                    if (playerConfig.has(player.getString("from"))) {
                    JSONObject pCfg = playerConfig.getJSONObject(player.getString("from"));
                    //String jxurl = "https://player.tjomet.com/lgyy/?url=" + player.getString("url");
                    String jxurl = pCfg.getString("parse") + player.getString("url");
                    Document doc = Jsoup.parse(OkHttpUtil.string(jxurl, getHeaders(jxurl)));
                    Elements script = doc.select("body>script");
                    for (int j = 0; j < script.size(); j++) {
                        String Content = script.get(j).html().trim();
                        Matcher matcher = urlt.matcher(Content);
                        if (!matcher.find()) {
                            return "";
                        }
                        String urlt = matcher.group(1);
                        Matcher matcher1 = token.matcher(Content);
                        if (!matcher1.find()) {
                            return "";
                        }
                        String token = matcher1.group(1);
                        Matcher matcher2 = vkey.matcher(Content);
                        if (!matcher2.find()) {
                            return "";
                        }
                        String vkey = matcher2.group(1);
                        HashMap hashMap = new HashMap();
                        hashMap.put("token", token);
                        hashMap.put("url", urlt);
                        hashMap.put("vkey", vkey);
                        hashMap.put("sign", "D4GE4tm2Q3NXbeeK");
                        OkHttpUtil.post(OkHttpUtil.defaultClient(), "https://player.6080kan.cc/player/hd0L3TjH4m8zSK1N.php", hashMap, new OKCallBack.OKCallBackString() {
                            @Override
                            protected void onFailure(Call call, Exception exc) {
                            }

                            public void onResponse(String str) {
                                try {
                                    String url = new String(Base64.decode(new JSONObject(str).getString("url").substring(8).getBytes(), 0));
                                    result.put("url", url.substring(8, url.length() - 8));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        result.put("header", headers.toString());
                        result.put("parse", 0);
                        result.put("playUrl", "");

                    }
                    }
                }
            }
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    /**
     * 搜索
     *
     * @param key
     * @param quick 是否播放页的快捷搜索
     * @return
     */
    @Override
    public String searchContent(String key, boolean quick) {
        try {
            if (quick)
                return "";
            long currentTime = System.currentTimeMillis();
            String url = siteUrl + "/index.php/ajax/suggest?mid=1&wd=" + URLEncoder.encode(key) + "&limit=10&timestamp=" + currentTime;
            JSONObject searchResult = new JSONObject(OkHttpUtil.string(url, getHeaders(url)));
            JSONObject result = new JSONObject();
            JSONArray videos = new JSONArray();
            if (searchResult.getInt("total") > 0) {
                JSONArray lists = new JSONArray(searchResult.getString("list"));
                for (int i = 0; i < lists.length(); i++) {
                    JSONObject vod = lists.getJSONObject(i);
                    String id = vod.getString("id");
                    String title = vod.getString("name");
                    String cover = vod.getString("pic");
                    if (!TextUtils.isEmpty(cover) && !cover.startsWith("http")) {
                        cover = siteUrl + cover;
                    }
                    JSONObject v = new JSONObject();
                    v.put("vod_id", id);
                    v.put("vod_name", title);
                    v.put("vod_pic", cover);
                    v.put("vod_remarks", "");
                    videos.put(v);
                }
            }
            result.put("list", videos);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }
}

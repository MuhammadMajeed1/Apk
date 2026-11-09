package com.gttv.player;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChannelCatalog {
    public static final class Channel {
        public final String id;
        public final String name;
        public final String category;
        public final String url;

        Channel(String id, String name, String category, String url) {
            this.id=id;
            this.name=name;
            this.category=category;
            this.url=url;
        }
    }

    private ChannelCatalog() {}

    public static List<Channel> load(Context context) {
        List<Channel> channels=new ArrayList<>();
        try (InputStream input=context.getAssets().open("channels.json");
             ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] buf=new byte[4096];
            int n;
            while ((n=input.read(buf))!=-1) output.write(buf,0,n);
            JSONObject root=new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            JSONArray entries=root.getJSONArray("channels");
            for(int i=0;i<entries.length();i++) {
                JSONObject obj=entries.getJSONObject(i);
                String id=obj.getString("id").trim();
                String name=obj.getString("name").trim();
                String category=obj.optString("category","قنوات").trim();
                String url=obj.getString("url").trim();
                if(!id.isEmpty() && !name.isEmpty() && url.startsWith("https://") && url.contains(".m3u8")) {
                    channels.add(new Channel(id,name,category,url));
                }
            }
        }catch(Exception ignored) {}
        return Collections.unmodifiableList(channels);
    }
}

package com.gttv.player;

import android.util.Base64;
import java.nio.charset.StandardCharsets;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback;
import androidx.media3.exoplayer.source.MediaSource;

/**
 * Standard Android ClearKey implementation for DASH sources configured to
 * support ClearKey DRM. Does not extract keys or defeat any Widevine license.
 */
@UnstableApi
public final class DrmStreamFactory {
    private DrmStreamFactory() {}

    public static MediaSource create(ChannelCatalog.Channel channel, DataSource.Factory dataSource) {
        if (!"clearkey".equals(channel.drm)) {
            throw new IllegalArgumentException("Unsupported DASH DRM configuration");
        }
        String response="{\"keys\":[{\"kty\":\"oct\",\"kid\":\""
            +toBase64Url(channel.keyId)+"\",\"k\":\""+toBase64Url(channel.key)
            +"\"}],\"type\":\"temporary\"}";
        DefaultDrmSessionManager drmManager=new DefaultDrmSessionManager.Builder()
            .setPlayClearSamplesWithoutKeys(true)
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID,FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(new LocalMediaDrmCallback(response.getBytes(StandardCharsets.UTF_8)));
        MediaItem mediaItem=new MediaItem.Builder()
            .setUri(channel.url)
            .setMimeType(MimeTypes.APPLICATION_MPD)
            .setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).build())
            .build();
        return new DashMediaSource.Factory(dataSource)
            .setDrmSessionManagerProvider(item->drmManager)
            .createMediaSource(mediaItem);
    }

    private static String toBase64Url(String hex) {
        if (hex==null || hex.length()!=32 || !hex.matches("[0-9a-fA-F]{32}"))
            throw new IllegalArgumentException("Invalid ClearKey hex pair");
        byte[] data=new byte[16];
        for(int i=0;i<16;i++)
            data[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);
        return Base64.encodeToString(data,Base64.NO_WRAP|Base64.NO_PADDING|Base64.URL_SAFE);
    }
}

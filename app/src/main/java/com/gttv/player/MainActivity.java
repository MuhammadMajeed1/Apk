package com.gttv.player;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import androidx.media3.datasource.DataSource;
import androidx.media3.common.VideoSize;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

@UnstableApi
public final class MainActivity extends Activity {
    private static final int PRIMARY=0xfff4f8fe, SECONDARY=0xffa9c0d5, ACCENT=0xff53c8ec;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final List<LinearLayout> channelRows=new ArrayList<>();
    private List<ChannelCatalog.Channel> channels;
    private FrameLayout root;
    private PlayerView video;
    private ExoPlayer player;
    private DataSource.Factory streamFactory;
    private ProgressBar spinner;
    private LinearLayout sidePanel, infoPanel, infoActions;
    private ScrollView channelScroll;
    private TextView title, status, qualityButton, zapBanner, digitsBanner;
    private boolean listOpen=false, infoOpen=false, playbackFailed=false;
    private int channelIndex=-1, previousChannel=-1, qualityLimit=0;
    private long lastZap=0L, lastBack=0L;
    private String numberInput="";
    private Runnable loadTimeout, infoTimeout, bannerTimeout, numberTimeout;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
        channels=ChannelCatalog.load(this);
        buildScreen();
        buildPlayer();
        if(!channels.isEmpty()) tune(0); // Auto-start channel 001, MBC 1
        else {setStatus("القنوات غير متاحة","تعذر قراءة ملف channels.json"); showInfo(true);}
    }

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){
        GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));return b;
    }
    private StateListDrawable focusBackground(){
        GradientDrawable selected=bg(0xff245e78,12);selected.setStroke(dp(2),ACCENT);
        StateListDrawable d=new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_focused},selected);
        d.addState(new int[]{},bg(0xff243a53,12));return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private TextView button(String s,Runnable action){
        TextView t=text(s,15,PRIMARY,true);t.setGravity(Gravity.CENTER);t.setBackground(focusBackground());
        t.setFocusable(true);t.setClickable(true);t.setOnClickListener(v->action.run());return t;
    }
    private void buildScreen(){
        root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        video=new PlayerView(this);video.setUseController(false);video.setFocusable(true);
        video.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        video.setKeepContentOnPlayerReset(true);
        root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        spinner=new ProgressBar(this);spinner.setVisibility(View.GONE);
        root.addView(spinner,new FrameLayout.LayoutParams(dp(46),dp(46),Gravity.CENTER));
        createInfo();
        createChannelList();
        zapBanner=text("",19,PRIMARY,true);
        zapBanner.setPadding(dp(16),dp(6),dp(16),dp(6));zapBanner.setBackground(bg(0xed12283f,10));
        zapBanner.setVisibility(View.GONE);
        FrameLayout.LayoutParams z=new FrameLayout.LayoutParams(dp(310),dp(52),Gravity.TOP|Gravity.LEFT);
        z.setMargins(dp(22),dp(22),0,0);root.addView(zapBanner,z);
        digitsBanner=text("",24,ACCENT,true);
        digitsBanner.setGravity(Gravity.CENTER);digitsBanner.setBackground(bg(0xef14283b,10));
        digitsBanner.setVisibility(View.GONE);
        FrameLayout.LayoutParams d=new FrameLayout.LayoutParams(dp(108),dp(60),Gravity.TOP|Gravity.RIGHT);
        d.setMargins(0,dp(22),dp(23),0);root.addView(digitsBanner,d);
        setContentView(root);video.requestFocus();
    }
    private void createInfo(){
        infoPanel=new LinearLayout(this);infoPanel.setOrientation(1);
        infoPanel.setPadding(dp(25),dp(11),dp(25),dp(13));
        infoPanel.setBackgroundColor(0xef0e2035);
        LinearLayout header=new LinearLayout(this);header.setOrientation(0);
        title=text("GT TV",23,PRIMARY,true);header.addView(title,new LinearLayout.LayoutParams(0,dp(37),1));
        TextView live=text("●  مباشر",15,ACCENT,true);live.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        header.addView(live,new LinearLayout.LayoutParams(dp(93),dp(37)));infoPanel.addView(header);
        status=text("يتم تجهيز المشغل...",14,SECONDARY,false);
        infoPanel.addView(status,new LinearLayout.LayoutParams(-1,dp(34)));
        infoActions=new LinearLayout(this);infoActions.setOrientation(0);
        qualityButton=button("الجودة: تلقائي",this::cycleQuality);
        TextView[] buttons={button("قائمة القنوات",this::openList),qualityButton,
            button("تحديث البث",this::reload),button("القناة السابقة",this::previous)};
        for(TextView b:buttons){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(47),1f);
            p.setMargins(dp(4),0,dp(4),0);infoActions.addView(b,p);}
        infoPanel.addView(infoActions,new LinearLayout.LayoutParams(-1,dp(47)));
        root.addView(infoPanel,new FrameLayout.LayoutParams(-1,dp(146),Gravity.BOTTOM));
        infoPanel.setVisibility(View.GONE);
    }
    private void createChannelList(){
        sidePanel=new LinearLayout(this);sidePanel.setOrientation(1);
        sidePanel.setPadding(dp(18),dp(23),dp(18),dp(13));sidePanel.setBackgroundColor(0xf7102034);
        sidePanel.addView(text("قائمة القنوات",23,PRIMARY,true),new LinearLayout.LayoutParams(-1,dp(45)));
        sidePanel.addView(text("↑ ↓ للتنقل  ·  OK للمشاهدة",13,SECONDARY,false),
            new LinearLayout.LayoutParams(-1,dp(38)));
        channelScroll=new ScrollView(this);channelScroll.setVerticalScrollBarEnabled(false);
        channelScroll.setFocusable(false);
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(1);
        for(int i=0;i<channels.size();i++){
            final int idx=i;ChannelCatalog.Channel c=channels.get(i);
            LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(11),dp(3),dp(11),dp(3));row.setBackground(focusBackground());
            row.setFocusable(true);row.setClickable(true);
            row.setOnClickListener(v->tune(idx));
            row.setOnFocusChangeListener((v,has)->{
                if(has)channelScroll.smoothScrollTo(0,Math.max(0,v.getTop()-dp(80)));
            });
            row.addView(text(String.format(Locale.US,"%03d",i+1),18,ACCENT,true),
                new LinearLayout.LayoutParams(dp(62),-1));
            LinearLayout words=new LinearLayout(this);words.setOrientation(1);
            words.addView(text(c.name,17,PRIMARY,true),new LinearLayout.LayoutParams(-1,dp(30)));
            words.addView(text(c.category,12,SECONDARY,false),new LinearLayout.LayoutParams(-1,dp(22)));
            row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(68));p.bottomMargin=dp(8);
            rows.addView(row,p);channelRows.add(row);
        }
        channelScroll.addView(rows);sidePanel.addView(channelScroll,new LinearLayout.LayoutParams(-1,0,1f));
        TextView footer=text("CH+/CH−: تبديل سريع للقنوات",13,SECONDARY,false);
        footer.setGravity(Gravity.CENTER);sidePanel.addView(footer,new LinearLayout.LayoutParams(-1,dp(42)));
        root.addView(sidePanel,new FrameLayout.LayoutParams(dp(376),-1,Gravity.RIGHT));
        sidePanel.setVisibility(View.GONE);
    }
    private void buildPlayer(){
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory()
            .setUserAgent("GTTVPlayer/3.0").setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12000).setReadTimeoutMs(12000);
        streamFactory=new DefaultDataSource.Factory(this,http);
        DefaultLoadControl buffering=new DefaultLoadControl.Builder()
            .setBufferDurationsMs(7000,26000,1500,2600).build();
        player=new ExoPlayer.Builder(this,new DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .setMediaSourceFactory(new DefaultMediaSourceFactory(streamFactory))
            .setLoadControl(buffering).build();
        video.setPlayer(player);
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){
                if(state==Player.STATE_BUFFERING){
                    spinner.setVisibility(View.VISIBLE);setStatus(null,"جارٍ تحميل البث...");loadingDeadline();
                }else if(state==Player.STATE_READY){
                    spinner.setVisibility(View.GONE);playbackFailed=false;clearLoadingDeadline();
                    setStatus(null,"بث مباشر  ·  ↑ ↓ لتغيير القناة  ·  OK لقائمة القنوات");
                    scheduleInfoDismiss();
                }else if(state==Player.STATE_ENDED){
                    spinner.setVisibility(View.GONE);clearLoadingDeadline();
                    setStatus(null,"انتهى البث. استخدم تحديث البث.");showInfo(true);
                }
            }
            @Override public void onPlayerError(PlaybackException e){
                spinner.setVisibility(View.GONE);playbackFailed=true;clearLoadingDeadline();
                setStatus(null,"تعذر البث (رمز "+e.errorCode+"). اضغط تحديث البث.");showInfo(true);
            }
        });
    }
    private void tune(int index){
        if(index<0||index>=channels.size())return;
        if(channelIndex>=0&&channelIndex!=index)previousChannel=channelIndex;
        channelIndex=index;playbackFailed=false;cancelNumbers();closeList();
        qualityLimit=0;updateQuality();
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
            .clearVideoSizeConstraints().build());
        ChannelCatalog.Channel c=channels.get(index);
        setStatus(String.format(Locale.US,"%03d",index+1)+"  "+c.name,"جارٍ الاتصال بالبث...");
        spinner.setVisibility(View.VISIBLE);
        if(c.url.endsWith(".mpd")){
            try{
                player.setMediaSource(DrmStreamFactory.create(c,streamFactory));
            }catch(Exception error){
                spinner.setVisibility(View.GONE);
                playbackFailed=true;
                setStatus(null,"تعذر تجهيز قناة DASH المشفّرة. تأكد من دعم المصدر لنظام ClearKey.");
                showInfo(true);
                return;
            }
        }else{
            player.setMediaItem(new MediaItem.Builder().setUri(c.url)
                .setMimeType(MimeTypes.APPLICATION_M3U8).build());
        }
        player.prepare();player.play();
        loadingDeadline();announceChannel(c);
        showInfo(false);
    }
    private void announceChannel(ChannelCatalog.Channel c){
        if(bannerTimeout!=null)handler.removeCallbacks(bannerTimeout);
        zapBanner.setText(String.format(Locale.US,"%03d",channelIndex+1)+"  "+c.name);
        zapBanner.setVisibility(View.VISIBLE);
        bannerTimeout=()->zapBanner.setVisibility(View.GONE);
        handler.postDelayed(bannerTimeout,2700);
    }
    private void setStatus(String name,String desc){if(name!=null)title.setText(name);if(desc!=null)status.setText(desc);}
    private void openList(){
        if(channels.isEmpty())return;
        clearInfoDismiss();listOpen=true;infoOpen=false;infoPanel.setVisibility(View.GONE);
        sidePanel.setVisibility(View.VISIBLE);
        LinearLayout row=channelRows.get(Math.max(0,channelIndex));
        row.requestFocus();
        channelScroll.post(()->channelScroll.scrollTo(0,Math.max(0,row.getTop()-dp(100))));
    }
    private void closeList(){listOpen=false;if(sidePanel!=null)sidePanel.setVisibility(View.GONE);
        if(video!=null)video.requestFocus();}
    private void showInfo(boolean focusButtons){
        if(listOpen)closeList();clearInfoDismiss();infoOpen=true;infoPanel.setVisibility(View.VISIBLE);
        if(focusButtons)infoActions.getChildAt(2).requestFocus();
        else {video.requestFocus();scheduleInfoDismiss();}
    }
    private void hideInfo(){
        if(playbackFailed)return;
        infoOpen=false;infoPanel.setVisibility(View.GONE);video.requestFocus();
    }
    private void scheduleInfoDismiss(){
        clearInfoDismiss();if(listOpen||playbackFailed||!infoOpen)return;
        infoTimeout=this::hideInfo;handler.postDelayed(infoTimeout,5000);
    }
    private void clearInfoDismiss(){if(infoTimeout!=null)handler.removeCallbacks(infoTimeout);infoTimeout=null;}
    private void loadingDeadline(){
        clearLoadingDeadline();final int expected=channelIndex;
        loadTimeout=()->{
            if(expected==channelIndex&&player!=null&&player.getPlaybackState()==Player.STATE_BUFFERING){
                playbackFailed=true;spinner.setVisibility(View.GONE);player.stop();
                setStatus(null,"لم يبدأ البث خلال 28 ثانية. جرّب تحديث البث أو قناة أخرى.");
                showInfo(true);
            }
        };
        handler.postDelayed(loadTimeout,28000);
    }
    private void clearLoadingDeadline(){if(loadTimeout!=null)handler.removeCallbacks(loadTimeout);loadTimeout=null;}
    private void reload(){if(channelIndex>=0)tune(channelIndex);}
    private void previous(){
        if(previousChannel>=0)tune(previousChannel);
        else Toast.makeText(this,"لا توجد قناة سابقة",Toast.LENGTH_SHORT).show();
    }
    private void cycleQuality(){
        if(channelIndex<0)return;
        qualityLimit=0;
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
            .clearVideoSizeConstraints().build());
        updateQuality();
        Toast.makeText(this,"الجودة تتكيف تلقائياً مع سرعة الإنترنت",Toast.LENGTH_SHORT).show();
        scheduleInfoDismiss();
    }
    private void updateQuality(){
        qualityButton.setText("الجودة: تلقائية");
    }
    private void zap(int direction){
        if(channels.isEmpty())return;
        long now=SystemClock.uptimeMillis();if(now-lastZap<260)return;lastZap=now;
        int index=Math.max(0,channelIndex);
        tune((index+direction+channels.size())%channels.size());
    }
    private void numberKey(int digit){
        if(numberTimeout!=null)handler.removeCallbacks(numberTimeout);
        if(numberInput.length()>=3)numberInput="";
        numberInput+=digit;digitsBanner.setText(numberInput);digitsBanner.setVisibility(View.VISIBLE);
        int value;
        try{value=Integer.parseInt(numberInput);}catch(Exception error){value=-1;}
        if(numberInput.length()==3 ||
           (numberInput.length()==2 && !numberInput.startsWith("0") && value>=10 && value<=channels.size())){
            commitNumber();
        }else{
            numberTimeout=this::commitNumber;
            handler.postDelayed(numberTimeout,1350);
        }
    }
    private void commitNumber(){
        String entry=numberInput;cancelNumbers();
        try{
            int selected=Integer.parseInt(entry);
            if(selected>0&&selected<=channels.size())tune(selected-1);
            else Toast.makeText(this,"رقم القناة غير موجود",Toast.LENGTH_SHORT).show();
        }catch(Exception ignored){}
    }
    private void cancelNumbers(){
        if(numberTimeout!=null)handler.removeCallbacks(numberTimeout);
        numberTimeout=null;numberInput="";
        if(digitsBanner!=null)digitsBanner.setVisibility(View.GONE);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getAction()!=KeyEvent.ACTION_DOWN)return super.dispatchKeyEvent(event);
        int code=event.getKeyCode();
        if(event.getRepeatCount()>0&&(code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_DPAD_CENTER
            ||code==KeyEvent.KEYCODE_ENTER))return true;
        if(code>=KeyEvent.KEYCODE_0&&code<=KeyEvent.KEYCODE_9){
            numberKey(code-KeyEvent.KEYCODE_0);return true;
        }
        if(code>=KeyEvent.KEYCODE_NUMPAD_0&&code<=KeyEvent.KEYCODE_NUMPAD_9){
            numberKey(code-KeyEvent.KEYCODE_NUMPAD_0);return true;
        }
        if(code==KeyEvent.KEYCODE_CHANNEL_UP||code==KeyEvent.KEYCODE_MEDIA_NEXT){zap(1);return true;}
        if(code==KeyEvent.KEYCODE_CHANNEL_DOWN||code==KeyEvent.KEYCODE_MEDIA_PREVIOUS){zap(-1);return true;}
        if(code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_ESCAPE){
            if(listOpen){closeList();return true;}
            if(infoOpen){playbackFailed=false;hideInfo();return true;}
            long now=SystemClock.uptimeMillis();
            if(now-lastBack<2200)finish();
            else {lastBack=now;Toast.makeText(this,"اضغط رجوع مرة أخرى للخروج",Toast.LENGTH_SHORT).show();}
            return true;
        }
        if(code==KeyEvent.KEYCODE_MENU||code==KeyEvent.KEYCODE_GUIDE){
            if(listOpen)closeList();else openList();return true;
        }
        if(code==KeyEvent.KEYCODE_INFO){showInfo(true);return true;}
        if(!listOpen&&!infoOpen){
            if(code==KeyEvent.KEYCODE_DPAD_UP){zap(1);return true;}
            if(code==KeyEvent.KEYCODE_DPAD_DOWN){zap(-1);return true;}
            if(code==KeyEvent.KEYCODE_DPAD_LEFT){openList();return true;}
            if(code==KeyEvent.KEYCODE_DPAD_RIGHT){showInfo(true);return true;}
            if(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER){
                openList();return true;
            }
        }
        if(infoOpen&&!listOpen&&code==KeyEvent.KEYCODE_DPAD_UP){zap(1);return true;}
        if(infoOpen&&!listOpen&&code==KeyEvent.KEYCODE_DPAD_DOWN){zap(-1);return true;}
        if(listOpen&&code==KeyEvent.KEYCODE_DPAD_LEFT){closeList();return true;}
        return super.dispatchKeyEvent(event);
    }
    @Override protected void onStart(){super.onStart();if(player!=null&&channelIndex>=0)player.play();}
    @Override protected void onStop(){if(player!=null)player.pause();super.onStop();}
    @Override protected void onDestroy(){
        clearInfoDismiss();clearLoadingDeadline();cancelNumbers();
        if(bannerTimeout!=null)handler.removeCallbacks(bannerTimeout);
        handler.removeCallbacksAndMessages(null);
        if(player!=null){video.setPlayer(null);player.release();player=null;}
        super.onDestroy();
    }
}

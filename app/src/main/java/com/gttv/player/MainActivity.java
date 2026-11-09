package com.gttv.player;
import android.app.*;import android.os.*;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;
import androidx.media3.common.*;import androidx.media3.exoplayer.*;import androidx.media3.exoplayer.source.*;import androidx.media3.datasource.*;import androidx.media3.ui.*;
public class MainActivity extends Activity{
 static final String[] N={"MBC 1","MBC 4","MBC دراما"};
 static final String[] U={
 "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-mbc-1-na/eec141533c90dd34722c503a296dd0d8/index.m3u8",
 "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-mbc-4/24f134f1cd63db9346439e96b86ca6ed/index.m3u8",
 "https://shd-gcp-live.lg.mncdn.com/live/bitmovin-mbc-masr-drama/567b703c19ede6598222de81b0e4508b/manifest/video/288p350kbps/video.m3u8"};
 int accent=0xff54c5f0,cur=-1,q=0; boolean menu=true,controls=false;
 FrameLayout root;LinearLayout drawer,hud,buttons;TextView title,status,qbutton;PlayerView playerView;ExoPlayer player;ProgressBar spinner;
 Handler timer=new Handler(Looper.getMainLooper());Runnable timeout,hide;
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 GradientDrawable shape(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(15));return g;}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 TextView button(String s,Runnable action){TextView t=text(s,17,-1);t.setGravity(Gravity.CENTER);t.setTypeface(null,1);
 GradientDrawable a=shape(0xff245a75);a.setStroke(dp(2),accent);StateListDrawable d=new StateListDrawable();
 d.addState(new int[]{android.R.attr.state_focused},a);d.addState(new int[]{},shape(0xff1a324a));t.setBackground(d);
 t.setFocusable(true);t.setClickable(true);t.setOnClickListener(v->action.run());return t;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);setRequestedOrientation(0);
 root=new FrameLayout(this);root.setBackgroundColor(-16777216);
 playerView=new PlayerView(this);playerView.setUseController(false);playerView.setFocusable(true);root.addView(playerView,new FrameLayout.LayoutParams(-1,-1));
 spinner=new ProgressBar(this);spinner.setVisibility(View.GONE);root.addView(spinner,new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.CENTER));
 hud=new LinearLayout(this);hud.setOrientation(1);hud.setPadding(dp(32),dp(15),dp(32),dp(16));hud.setBackgroundColor(0xe9162638);
 title=text("GT TV",27,-1);title.setTypeface(null,1);hud.addView(title,new LinearLayout.LayoutParams(-1,dp(44)));
 status=text("اختر قناة",14,0xffafc3d5);hud.addView(status,new LinearLayout.LayoutParams(-1,dp(33)));
 buttons=new LinearLayout(this);
 TextView ch=button("القنوات",()->showMenu());qbutton=button("الجودة: تلقائي",()->changeQuality());
 TextView retry=button("تحديث البث",()->{if(cur>=0)play(cur);});TextView home=button("الرئيسية",()->showMenu());
 for(TextView t:new TextView[]{ch,qbutton,retry,home}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(51),1);p.setMargins(dp(4),0,dp(4),0);buttons.addView(t,p);}
 hud.addView(buttons);root.addView(hud,new FrameLayout.LayoutParams(-1,dp(163),Gravity.BOTTOM));hud.setVisibility(View.GONE);
 drawer=new LinearLayout(this);drawer.setOrientation(1);drawer.setPadding(dp(25),dp(30),dp(25),dp(18));drawer.setBackgroundColor(0xf20e2035);
 TextView heading=text("GT  TV PLAYER",25,accent);heading.setTypeface(null,1);drawer.addView(heading,new LinearLayout.LayoutParams(-1,dp(60)));
 drawer.addView(text("اختر قناة واضغط OK",15,0xffc0ccdb),new LinearLayout.LayoutParams(-1,dp(48)));
 for(int i=0;i<N.length;i++){final int k=i;TextView t=button(N[i],()->play(k));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(72));p.bottomMargin=dp(14);drawer.addView(t,p);}
 drawer.addView(text("↑ ↓ تغيير القناة   •   OK تحكم",13,0xffb0c4d8));
 root.addView(drawer,new FrameLayout.LayoutParams(dp(355),-1,Gravity.RIGHT));setContentView(root);drawer.getChildAt(2).requestFocus();
 DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setConnectTimeoutMs(12000).setReadTimeoutMs(12000).setAllowCrossProtocolRedirects(true);
 player=new ExoPlayer.Builder(this,new DefaultRenderersFactory(this).setEnableDecoderFallback(true))
 .setMediaSourceFactory(new DefaultMediaSourceFactory(new DefaultDataSource.Factory(this,http))).build();playerView.setPlayer(player);
 player.addListener(new Player.Listener(){
 public void onPlaybackStateChanged(int state){if(state==Player.STATE_BUFFERING){spinner.setVisibility(View.VISIBLE);status.setText("جارٍ الاتصال بالبث...");}
 if(state==Player.STATE_READY){spinner.setVisibility(View.GONE);cancelTimeout();status.setText("مباشر • OK تحكم • ↑ ↓ تغيير القناة");hideLater();}
 if(state==Player.STATE_ENDED){spinner.setVisibility(View.GONE);status.setText("انتهى البث. اضغط تحديث");showControls();}}
 public void onPlayerError(PlaybackException e){spinner.setVisibility(View.GONE);cancelTimeout();status.setText("تعذر البث - خطأ "+e.errorCode+"، جرّب تحديث أو قناة أخرى");showControls();}
 });}
 void play(int index){cur=index;menu=false;controls=true;drawer.setVisibility(View.GONE);hud.setVisibility(View.VISIBLE);
 title.setText(N[index]);q=0;qbutton.setText(index==2?"الجودة: المصدر 288p":"الجودة: تلقائي");status.setText("جارٍ فتح القناة...");
 player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().clearVideoSizeConstraints().build());
 player.setMediaItem(new MediaItem.Builder().setUri(U[index]).setMimeType(MimeTypes.APPLICATION_M3U8).build());
 spinner.setVisibility(View.VISIBLE);player.prepare();player.play();scheduleTimeout();hideLater();playerView.requestFocus();}
 void scheduleTimeout(){cancelTimeout();final int k=cur;timeout=()->{if(k==cur&&player.getPlaybackState()!=Player.STATE_READY){player.stop();spinner.setVisibility(View.GONE);status.setText("لم يبدأ البث خلال 25 ثانية: المصدر غير متاح أو محجوب");showControls();}};timer.postDelayed(timeout,25000);}
 void cancelTimeout(){if(timeout!=null)timer.removeCallbacks(timeout);timeout=null;}
 void hideLater(){if(hide!=null)timer.removeCallbacks(hide);hide=()->{if(!menu){controls=false;hud.setVisibility(View.GONE);playerView.requestFocus();}};timer.postDelayed(hide,7000);}
 void showControls(){if(!menu){controls=true;hud.setVisibility(View.VISIBLE);buttons.getChildAt(0).requestFocus();hideLater();}}
 void showMenu(){if(hide!=null)timer.removeCallbacks(hide);menu=true;controls=false;drawer.setVisibility(View.VISIBLE);hud.setVisibility(View.GONE);drawer.getChildAt(cur<0?2:cur+2).requestFocus();}
 void changeQuality(){if(cur==2){status.setText("جودة ثابتة 288p");return;}q=(q+1)%3;
 TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();
 if(q==1)p.setMaxVideoSize(854,480);else if(q==2)p.setMaxVideoSize(1280,720);else p.clearVideoSizeConstraints();
 player.setTrackSelectionParameters(p.build());qbutton.setText(q==0?"الجودة: تلقائي":q==1?"الجودة: 480p":"الجودة: 720p");hideLater();}
 @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()!=0)return super.dispatchKeyEvent(e);int k=e.getKeyCode();
 if(k==4||k==111){if(menu){if(cur>=0){menu=false;drawer.setVisibility(View.GONE);playerView.requestFocus();}else finish();}else showMenu();return true;}
 if(cur>=0&&(k==166||k==167)){play((cur+(k==167?1:2))%3);return true;}
 if(!menu&&!controls&&cur>=0){if(k==19){play((cur+2)%3);return true;}if(k==20){play((cur+1)%3);return true;}
 if(k==21){showMenu();return true;}if(k==22||k==23||k==66||k==82){showControls();return true;}}
 if(!menu&&k==82){showMenu();return true;}return super.dispatchKeyEvent(e);}
 @Override protected void onStop(){if(player!=null)player.pause();super.onStop();}
 @Override protected void onStart(){super.onStart();if(player!=null&&cur>=0)player.play();}
 @Override protected void onDestroy(){cancelTimeout();timer.removeCallbacksAndMessages(null);if(player!=null)player.release();super.onDestroy();}
}
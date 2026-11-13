package com.btolens.script2cartoon;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class MainActivity extends Activity {
    EditText script; TextView status; Button generate;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(30,40,30,24); root.setBackgroundColor(Color.rgb(247,248,252));
        TextView title = new TextView(this); title.setText("Script2Cartoon"); title.setTextSize(28); title.setTextColor(Color.rgb(23,34,65)); root.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText("Paste your Hindi or English story. Create a silent 9:16 animated MP4."); subtitle.setTextSize(15); root.addView(subtitle);
        script = new EditText(this); script.setHint("Paste your script here. Separate scenes with new lines."); script.setGravity(Gravity.TOP); script.setMinLines(9); script.setTextSize(17); root.addView(script, new LinearLayout.LayoutParams(-1,0,1));
        generate = new Button(this); generate.setText("Generate MP4"); root.addView(generate);
        status = new TextView(this); status.setText("Videos are saved to Movies/Script2Cartoon. No internet required."); root.addView(status);
        setContentView(root);
        generate.setOnClickListener(v -> { String input=script.getText().toString().trim(); if(input.isEmpty()){status.setText("Enter a script first.");return;} generate.setEnabled(false); status.setText("Rendering video. Please wait..."); new Thread(() -> { try { String result=render(input);runOnUiThread(()->{status.setText("Saved: "+result);generate.setEnabled(true);}); } catch(Exception ex){runOnUiThread(()->{status.setText("Export failed: "+ex.getMessage());generate.setEnabled(true);});} }).start(); });
    }
    String render(String input) throws Exception {
        ArrayList<String> scenes = new ArrayList<>();
        for(String part: input.split("\\n+|(?<=[.!?।])\\s+")) if(!part.trim().isEmpty()) scenes.add(part.trim());
        if(scenes.isEmpty()) throw new Exception("No scenes found");
        if(scenes.size()>30) scenes = new ArrayList<>(scenes.subList(0,30));
        final int w=540,h=960,fps=15; final long frameUs=1000000L/fps;
        String name="Short_"+System.currentTimeMillis()+".mp4";
        File temp = new File(getCacheDir(),name);
        MediaCodec codec = null; MediaMuxer muxer = null; boolean started=false; int track=-1;
        try {
            MediaFormat fmt=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,w,h);
            fmt.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            fmt.setInteger(MediaFormat.KEY_BIT_RATE,1200000);fmt.setInteger(MediaFormat.KEY_FRAME_RATE,fps);fmt.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
            codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);codec.configure(fmt,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);Surface surface=codec.createInputSurface();codec.start();
            muxer=new MediaMuxer(temp.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            Paint p=new Paint(3);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            int[] colors={0xff172b55,0xff35416c,0xff174f59,0xff5c315b,0xff394b30};
            long totalFrames=0;
            for(int s=0;s<scenes.size();s++) {
                String text=scenes.get(s);int frames=Math.min(90,Math.max(45, text.length()/3*fps/4));
                for(int f=0;f<frames;f++) {
                    Canvas c=surface.lockCanvas(null);
                    c.drawColor(colors[s%colors.length]);
                    p.setColor(0xffffb44b);p.setTextSize(34);c.drawText("BEYOND THE OBVIOUS LENS",28,90,p);
                    p.setColor(Color.WHITE);p.setTextSize(38);
                    drawWrapped(c,p,text,45,380,w-90,55);
                    p.setTextSize(24);p.setColor(0xffffc46c);c.drawText("SCENE "+(s+1)+" / "+scenes.size(),45,850,p);
                    surface.unlockCanvasAndPost(c);
                    totalFrames++;
                    int[] state=drain(codec,muxer,info,track,started,false); track=state[0]; started=state[1]==1;
                    // Track creation is handled in drain state below.
                }
            }
            codec.signalEndOfInputStream();
            boolean done=false; long deadline=System.currentTimeMillis()+30000;
            while(!done && System.currentTimeMillis()<deadline){
                int ix=codec.dequeueOutputBuffer(info,10000);
                if(ix==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){if(started)throw new Exception("Unexpected format change");track=muxer.addTrack(codec.getOutputFormat());muxer.start();started=true;}
                else if(ix>=0){ByteBuffer data=codec.getOutputBuffer(ix);if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)info.size=0;if(info.size>0){if(!started)throw new Exception("Muxer not ready");data.position(info.offset);data.limit(info.offset+info.size);muxer.writeSampleData(track,data,info);}done=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(ix,false);}
            }
            if(!done)throw new Exception("Encoder timed out");
        } finally {if(codec!=null){try{codec.stop();}catch(Exception ignored){}codec.release();}if(muxer!=null){try{muxer.stop();}catch(Exception ignored){}muxer.release();}}
        android.content.ContentValues cv=new android.content.ContentValues();cv.put(android.provider.MediaStore.Video.Media.DISPLAY_NAME,name);cv.put(android.provider.MediaStore.Video.Media.MIME_TYPE,"video/mp4");cv.put(android.provider.MediaStore.Video.Media.RELATIVE_PATH,"Movies/Script2Cartoon");
        android.net.Uri uri=getContentResolver().insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cv);if(uri==null)throw new Exception("Could not create output file");
        try(InputStream in=new FileInputStream(temp);OutputStream out=getContentResolver().openOutputStream(uri)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}temp.delete();return "Movies/Script2Cartoon/"+name;
    }
    int[] drain(MediaCodec codec,MediaMuxer muxer,MediaCodec.BufferInfo info,int track,boolean started,boolean unused) {
        while(true){int ix=codec.dequeueOutputBuffer(info,0);
            if(ix==MediaCodec.INFO_TRY_AGAIN_LATER)break;
            if(ix==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){if(!started){track=muxer.addTrack(codec.getOutputFormat());muxer.start();started=true;}}
            else if(ix>=0){ByteBuffer data=codec.getOutputBuffer(ix);if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)info.size=0;
                if(info.size>0&&started){data.position(info.offset);data.limit(info.offset+info.size);muxer.writeSampleData(track,data,info);}codec.releaseOutputBuffer(ix,false);}
        }
        return new int[]{track,started?1:0};
    }
    void drawWrapped(Canvas c,Paint p,String text,int x,int y,int maxWidth,int lineHeight){
        String[] words=text.split("\\s+");StringBuilder line=new StringBuilder();ArrayList<String> lines=new ArrayList<>();
        for(String word:words){String test=line.length()==0?word:line+" "+word;if(p.measureText(test)>maxWidth&&line.length()>0){lines.add(line.toString());line=new StringBuilder(word);}else line=new StringBuilder(test);}
        if(line.length()>0)lines.add(line.toString());int start=y-(lines.size()-1)*lineHeight/2;
        for(int i=0;i<Math.min(10,lines.size());i++)c.drawText(lines.get(i),x,start+i*lineHeight,p);
    }
}

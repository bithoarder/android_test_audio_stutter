package net.bitheap.test.audiostuttering;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TestActivity extends Activity 
{
  private Thread m_thread;
  private WakeLock m_wakeLock;
  private int m_dummy;
  
  @Override
  public void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
    m_wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AudioTrackTest");
    
    ((Button)findViewById(R.id.playbutton)).setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        play();
      }
    });

    ((Button)findViewById(R.id.stopbutton)).setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        stop();
      }
    });
  }
  
  private void play()
  {
    if(m_thread == null)
    {
      m_thread = new Thread(new Runnable() 
      {
        public void run() 
        {
          m_wakeLock.acquire();
          Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

          int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.CHANNEL_CONFIGURATION_MONO);
          AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO , AudioFormat.ENCODING_PCM_16BIT, minBufferSize*4, AudioTrack.MODE_STREAM);
          audio.play();
          
          // generate sinus tone
          short[] audioBuffer = new short[1024];
          for(int i=0; i<audioBuffer.length; i++)
            audioBuffer[i] = (short)(Math.sin(i*Math.PI/audioBuffer.length*64)*32765.0f);

          long sleepDuration = audioBuffer.length*1000000000l/44100/3;

          // find out how many cyces to waste to spend around 1/3 of the total avilable time between AudioTrack writes
          // to increase the change of getting at least one uninterrupted run, run the profile a few times:
          int loopsPerBuffer = 0;
          for(int j=0; j<10; j++)
          {
            long profileStart = System.nanoTime();
            for(int i=0; i<1000; i++)
              m_dummy += i; // to prevent the compiler from removing the loop
            long profileDuration = System.nanoTime()-profileStart;
            if(loopsPerBuffer==0 || sleepDuration/profileDuration<loopsPerBuffer)
              loopsPerBuffer = (int)(sleepDuration/profileDuration);
          }
          Log.v("StutterTest", "loopsPerBuffer:"+loopsPerBuffer);
          
          while(!Thread.interrupted())
          {
            // to simulate expensive audio processing, burn about 1/3 of the cycles between writes:
            for(int j=0; j<loopsPerBuffer; j++)
            {
              for(int i=0; i<1000; i++)
                m_dummy += i;
            }

            audio.write(audioBuffer, 0, audioBuffer.length);
          }
          
          audio.stop();
          audio.release();
          
          m_wakeLock.release();
        }
      });
      m_thread.start();
    }
  }
  
  private void stop()
  {
    if(m_thread != null)
    {
      try
      {
        m_thread.interrupt();
        m_thread.join();
        m_thread = null;
      }
      catch(InterruptedException e)
      {
      }
    }
  }
  
}
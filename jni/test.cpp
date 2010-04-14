#include <jni.h>
#include <sys/time.h> 
#include <android/log.h>
#include <media/AudioTrack.h>
using namespace android;

#define DEBUG(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, "Stutteringtest(jni)", fmt, ##args)
#define ASSERT(cond) ((cond)?(void)0:__android_log_assert("##cond", "Stutteringtest(jni)", ""))
#define ASSERTMSG(cond, fmt, args...) ((cond)?(void)0:__android_log_assert("##cond", "Stutteringtest(jni)", fmt, ##args))

static volatile bool g_stop = false;
int g_dummy;

static unsigned long long gettick()
{
  struct timeval tv;
  gettimeofday(&tv, 0);
  return tv.tv_sec*1000000ll + tv.tv_usec;
}

extern "C"
JNIEXPORT void Java_net_bitheap_test_audiostuttering_TestActivity_jniRun(JNIEnv *env, jobject thiz)
{
  DEBUG("jniInit: enter");
  
  g_stop = false;
  
  AudioTrack track(AudioSystem::MUSIC, 44100, AudioSystem::PCM_16_BIT, AudioSystem::CHANNEL_OUT_MONO, 4*1024);

  status_t status = track.initCheck();
  ASSERTMSG(status==0, "Failed to initialize AudioTrack: %u", status);
  
  short sample[1024];
  for(int i=0; i<1024; i++)
  {
    sample[i] = i*65536*32/1024;
  }
  
  long long sleepDuration = 1024*1000000ll/44100/3;

  // find out how many cyces to waste to spend around 1/3 of the total avilable time between AudioTrack writes
  // to increase the change of getting at least one uninterrupted run, run the profile a few times:
  int loopsPerBuffer = 0;
  for(int j=0; j<10; j++)
  {
    unsigned long long profileStart = gettick();
    DEBUG("loopsPerBuffer: %lld", profileStart);
    for(int i=0; i<1000000; i++)
      g_dummy += i; // to prevent the compiler from removing the loop
    unsigned long long profileDuration = gettick()-profileStart;
    DEBUG("profileDuration: %lld", profileDuration);
    if(loopsPerBuffer==0 || sleepDuration/profileDuration<loopsPerBuffer)
      loopsPerBuffer = sleepDuration/profileDuration;
  }
  DEBUG("loopsPerBuffer: %d", loopsPerBuffer);
  
  track.start();
  while(!g_stop)
  {
    for(int i=0; i<loopsPerBuffer; i++)
    {
      for(int i=0; i<1000000; i++)
        g_dummy += i; // to prevent the compiler from removing the loop
    }
  
    track.write(sample, sizeof(sample));
  }
  
  track.stop();

  DEBUG("jniInit: leave");
}

extern "C"
JNIEXPORT void Java_net_bitheap_test_audiostuttering_TestActivity_jniStop(JNIEnv *env, jobject thiz)
{
  g_stop = true;
}


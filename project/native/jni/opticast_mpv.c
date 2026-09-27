/* OptiCast native adapter. GPL-3.0-or-later when distributed with the GPL mpv build. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <locale.h>
#include <string.h>
#include <mpv/client.h>
#include <libavcodec/jni.h>

typedef struct { mpv_handle *mpv; jobject surface; } Player;
static jobject application;
#define JNI(name) Java_com_opticast_player_player_mpv_NativeMpv_##name
static Player *ptr(jlong p) { return (Player *)(intptr_t)p; }
static char *utf8(JNIEnv *e,jstring s) {
    jclass cls=(*e)->FindClass(e,"java/lang/String");
    jmethodID method=(*e)->GetMethodID(e,cls,"getBytes","(Ljava/lang/String;)[B");
    jstring charset=(*e)->NewStringUTF(e,"UTF-8");
    jbyteArray bytes=(*e)->CallObjectMethod(e,s,method,charset);
    (*e)->DeleteLocalRef(e,charset);(*e)->DeleteLocalRef(e,cls);
    if(!bytes)return NULL;
    jsize n=(*e)->GetArrayLength(e,bytes);char *p=malloc(n+1);
    if(p){(*e)->GetByteArrayRegion(e,bytes,0,n,(jbyte *)p);p[n]=0;}
    (*e)->DeleteLocalRef(e,bytes);return p;
}
static jstring java_string(JNIEnv *e,const char *s) {
    size_t n=strlen(s);jbyteArray bytes=(*e)->NewByteArray(e,n);
    if(!bytes)return NULL;(*e)->SetByteArrayRegion(e,bytes,0,n,(const jbyte *)s);
    jclass cls=(*e)->FindClass(e,"java/lang/String");
    jmethodID ctor=(*e)->GetMethodID(e,cls,"<init>","([BLjava/lang/String;)V");
    jstring charset=(*e)->NewStringUTF(e,"UTF-8");
    jstring result=(*e)->NewObject(e,cls,ctor,bytes,charset);
    (*e)->DeleteLocalRef(e,charset);(*e)->DeleteLocalRef(e,bytes);(*e)->DeleteLocalRef(e,cls);return result;
}
JNIEXPORT jlong JNICALL JNI(create)(JNIEnv *e,jclass c,jobject context) {
    JavaVM *vm=NULL; (*e)->GetJavaVM(e,&vm); av_jni_set_java_vm(vm,NULL);
    if (!application) { application=(*e)->NewGlobalRef(e,context); av_jni_set_android_app_ctx(application,NULL); }
    setlocale(LC_NUMERIC,"C");
    Player *p=calloc(1,sizeof(*p)); if(!p)return 0;
    p->mpv=mpv_create(); if(!p->mpv){free(p);return 0;} return (jlong)(intptr_t)p;
}
JNIEXPORT jint JNICALL JNI(option)(JNIEnv *e,jclass c,jlong h,jstring key,jstring value) {
    if(!h)return MPV_ERROR_UNINITIALIZED;
    const char *k=(*e)->GetStringUTFChars(e,key,NULL),*v=(*e)->GetStringUTFChars(e,value,NULL);
    int rc=mpv_set_option_string(ptr(h)->mpv,k,v);
    (*e)->ReleaseStringUTFChars(e,key,k);(*e)->ReleaseStringUTFChars(e,value,v);return rc;
}
JNIEXPORT jint JNICALL JNI(initialize)(JNIEnv *e,jclass c,jlong h) { return h?mpv_initialize(ptr(h)->mpv):MPV_ERROR_UNINITIALIZED; }
JNIEXPORT jint JNICALL JNI(set)(JNIEnv *e,jclass c,jlong h,jstring key,jstring value) {
    if(!h)return MPV_ERROR_UNINITIALIZED;
    const char *k=(*e)->GetStringUTFChars(e,key,NULL),*v=(*e)->GetStringUTFChars(e,value,NULL);
    int rc=mpv_set_property_string(ptr(h)->mpv,k,v);
    (*e)->ReleaseStringUTFChars(e,key,k);(*e)->ReleaseStringUTFChars(e,value,v);return rc;
}
JNIEXPORT jstring JNICALL JNI(get)(JNIEnv *e,jclass c,jlong h,jstring key) {
    if(!h)return NULL; const char *k=(*e)->GetStringUTFChars(e,key,NULL);
    char *v=mpv_get_property_string(ptr(h)->mpv,k); (*e)->ReleaseStringUTFChars(e,key,k);
    if(!v)return NULL; jstring result=java_string(e,v);mpv_free(v);return result;
}
JNIEXPORT jint JNICALL JNI(command)(JNIEnv *e,jclass c,jlong h,jobjectArray args) {
    if(!h)return MPV_ERROR_UNINITIALIZED;jsize n=(*e)->GetArrayLength(e,args);
    if(n<1||n>32)return MPV_ERROR_INVALID_PARAMETER;
    const char *a[33]={0};jstring s[32]={0};
    for(int i=0;i<n;i++){s[i]=(*e)->GetObjectArrayElement(e,args,i);a[i]=utf8(e,s[i]);}
    int rc=mpv_command(ptr(h)->mpv,a);
    for(int i=0;i<n;i++){free((void *)a[i]);(*e)->DeleteLocalRef(e,s[i]);}return rc;
}
JNIEXPORT jint JNICALL JNI(surface)(JNIEnv *e,jclass c,jlong h,jobject surface) {
    if(!h)return MPV_ERROR_UNINITIALIZED; Player *p=ptr(h);
    // Detach the renderer before invalidating its global Surface reference.
    mpv_set_property_string(p->mpv,"vo","null");
    int64_t zero=0;mpv_set_option(p->mpv,"wid",MPV_FORMAT_INT64,&zero);
    if(p->surface){(*e)->DeleteGlobalRef(e,p->surface);p->surface=NULL;}
    if(!surface)return 0;
    p->surface=(*e)->NewGlobalRef(e,surface); if(!p->surface)return MPV_ERROR_NOMEM;
    int64_t wid=(int64_t)(intptr_t)p->surface;
    int rc=mpv_set_option(p->mpv,"wid",MPV_FORMAT_INT64,&wid);
    if(rc>=0)rc=mpv_set_property_string(p->mpv,"vo","gpu");return rc;
}
JNIEXPORT jintArray JNICALL JNI(events)(JNIEnv *e,jclass c,jlong h) {
    jint values[2]={0,0};
    if(h)for(int i=0;i<64;i++){
        mpv_event *event=mpv_wait_event(ptr(h)->mpv,0);
        if(event->event_id==MPV_EVENT_NONE)break;
        if(event->event_id==MPV_EVENT_FILE_LOADED)values[0]|=1;
        if(event->event_id==MPV_EVENT_PLAYBACK_RESTART)values[0]|=2;
        if(event->event_id==MPV_EVENT_END_FILE){
            mpv_event_end_file *end=event->data;
            if(end->reason==MPV_END_FILE_REASON_EOF)values[0]|=4;
            if(end->error<0){values[0]|=8;values[1]=end->error;}
        }
    }
    jintArray result=(*e)->NewIntArray(e,2);(*e)->SetIntArrayRegion(e,result,0,2,values);return result;
}
JNIEXPORT void JNICALL JNI(destroy)(JNIEnv *e,jclass c,jlong h) {
    if(!h)return;Player *p=ptr(h);mpv_terminate_destroy(p->mpv);
    if(p->surface)(*e)->DeleteGlobalRef(e,p->surface);free(p);
}

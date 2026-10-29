#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <android/bitmap.h>
#include <x264.h>

static jbyteArray bytes(JNIEnv *env, const uint8_t *data, int length) {
    jbyteArray result=(*env)->NewByteArray(env,length);
    if(result)(*env)->SetByteArrayRegion(env,result,0,length,(const jbyte *)data);
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_dev_cleanframe_tv_PreviewEncoder_encode(JNIEnv *env,jclass cls,jobject bitmap) {
    AndroidBitmapInfo info;
    if(AndroidBitmap_getInfo(env,bitmap,&info)!=ANDROID_BITMAP_RESULT_SUCCESS||info.format!=ANDROID_BITMAP_FORMAT_RGBA_8888||info.width!=3840||info.height!=2160)return NULL;
    x264_param_t param;
    x264_param_default_preset(&param,"ultrafast","zerolatency");
    param.i_width=3840;param.i_height=2160;param.i_csp=X264_CSP_I420;
    param.i_fps_num=1;param.i_fps_den=1;param.i_timebase_num=1;param.i_timebase_den=1;
    param.b_vfr_input=0;param.i_keyint_max=250;param.i_bframe=0;param.i_threads=2;
    param.b_annexb=1;param.b_repeat_headers=0;param.i_log_level=X264_LOG_NONE;
    param.rc.i_rc_method=X264_RC_CRF;param.rc.f_rf_constant=18;
    param.vui.i_colorprim=1;param.vui.i_transfer=1;param.vui.i_colmatrix=1;
    x264_param_apply_profile(&param,"high");
    x264_t *encoder=x264_encoder_open(&param);
    if(!encoder)return NULL;
    x264_picture_t picture,out;
    if(x264_picture_alloc(&picture,X264_CSP_I420,3840,2160)<0){x264_encoder_close(encoder);return NULL;}
    uint8_t *rgba=NULL;
    if(AndroidBitmap_lockPixels(env,bitmap,(void **)&rgba)!=ANDROID_BITMAP_RESULT_SUCCESS){x264_picture_clean(&picture);x264_encoder_close(encoder);return NULL;}
    for(int y=0;y<2160;y++)for(int x=0;x<3840;x++){
        uint8_t *p=rgba+y*info.stride+x*4;
        picture.img.plane[0][y*picture.img.i_stride[0]+x]=(uint8_t)(((47*p[0]+157*p[1]+16*p[2]+128)>>8)+16);
    }
    for(int y=0;y<2160;y+=2)for(int x=0;x<3840;x+=2){
        int r=0,g=0,b=0;
        for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++){uint8_t *p=rgba+(y+dy)*info.stride+(x+dx)*4;r+=p[0];g+=p[1];b+=p[2];}
        r>>=2;g>>=2;b>>=2;
        picture.img.plane[1][(y/2)*picture.img.i_stride[1]+x/2]=(uint8_t)(((-26*r-87*g+112*b+128)>>8)+128);
        picture.img.plane[2][(y/2)*picture.img.i_stride[2]+x/2]=(uint8_t)(((112*r-102*g-10*b+128)>>8)+128);
    }
    AndroidBitmap_unlockPixels(env,bitmap);
    jobjectArray result=(*env)->NewObjectArray(env,4,(*env)->FindClass(env,"[B"),NULL);
    x264_nal_t *nals;int count;
    if(x264_encoder_headers(encoder,&nals,&count)>0)for(int i=0;i<count;i++){
        if(nals[i].i_type==NAL_SPS||nals[i].i_type==NAL_PPS){jbyteArray data=bytes(env,nals[i].p_payload,nals[i].i_payload);(*env)->SetObjectArrayElement(env,result,nals[i].i_type==NAL_SPS?0:1,data);(*env)->DeleteLocalRef(env,data);}
    }
    for(int frame=0;frame<2;frame++){
        picture.i_pts=frame;
        int length=x264_encoder_encode(encoder,&nals,&count,&picture,&out);
        if(length<=0){result=NULL;break;}
        uint8_t *buffer=malloc(length);if(!buffer){result=NULL;break;}
        int offset=0;for(int i=0;i<count;i++){memcpy(buffer+offset,nals[i].p_payload,nals[i].i_payload);offset+=nals[i].i_payload;}
        jbyteArray data=bytes(env,buffer,offset);free(buffer);
        (*env)->SetObjectArrayElement(env,result,frame+2,data);(*env)->DeleteLocalRef(env,data);
    }
    x264_picture_clean(&picture);x264_encoder_close(encoder);
    return result;
}

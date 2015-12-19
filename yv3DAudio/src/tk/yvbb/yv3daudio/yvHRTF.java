package tk.yvbb.yv3daudio;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.un4seen.bass.*;

public class yvHRTF {
    private int ChannelHandle,DSPHandle=0;
    private Vec SpePos=new Vec();
    private short Previous[]=new short[128];
    private static Vec LisPos=new Vec(),LisFace=new Vec(1,0,0),LisUp=new Vec(0,1,0);
    private static boolean Initialized=false;
    private static short HRTFData[][][]=new short[14][181][256];
    private static boolean HRTFDataAvailable[][]=new boolean[14][181];
    
    private static void Init() {
        int n=0;
        for (int i=0;i<14;++i)
            for (int j=0;j<181;++j) {
                String Filename;
                Filename=String.format("hrtf-data/elev%d/H%de%03da.dat",(i-4)*10,(i-4)*10,j);
                InputStream is;
                FILE *fp=fopen(Filename,"rb");
                if (fp==NULL)
                    continue;
                for (int k=0;k<256;k+=2) {
                    fread(((char*)HRTFData[i][j])+k+1,1,1,fp);
                    fread(((char*)HRTFData[i][j])+k,1,1,fp);
                }
                fclose(fp);
                HRTFDataAvailable[i][j]=1;
                ++n;
            }
        if (n==0)
            System.exit(0);
        Initialized=true;
    }
    
    public yvHRTF(int handle) {
        if (!Initialized)
            Init();
        ChannelHandle=handle;
    }
    
    protected void finalize() {
        Stop3D();
    }
    
    public void Start3D() {
        BASS.DSPPROC DSP=new BASS.DSPPROC() {
            public void DSPPROC(int handle, int channel, ByteBuffer buffer, int length, Object user) {
                yvHRTF TheyvHRTF=(yvHRTF)user;
                Vec LS=TheyvHRTF.SpePos.Minus(LisPos);
                short Previous[]=TheyvHRTF.Previous;
                int e=(int)(9*(1-2*LS.Angle(LisUp)/Math.PI)+4.5);
                e=(e<0)?0:e;
                Vec d=LisFace.Det(LisUp),Sur=LS.Minus(LisUp.Multiply(LS.Dot(LisUp)/LisUp.Len()));
                int a=(int)(Sur.AngleDegree(LisFace)+.5);
                for (int i=a,j=a;;i=(i+180)%181,j=(i+1)%181) {
                    if (HRTFDataAvailable[e][i]) {
                        a=i;
                        break;
                    } else if (HRTFDataAvailable[e][j]) {
                        a=j;
                        break;
                    }
                }
                short *LeftData,*RightData;
                if (Sur.AngleDegree(d)>90) {
                    LeftData=HRTFData[e][a]+1;
                    RightData=HRTFData[e][a];
                } else {
                    LeftData=HRTFData[e][a];
                    RightData=HRTFData[e][a]+1;
                }
                double distance=LS.Len();
                short *ori=(short*)buffer,*buf=(short*)malloc(length/2);
                for (int i=0;i<length/2;i+=2)
                    buf[i/2]=ori[i]/2+ori[i+1]/2;
                for (int i=0;i<length/2;i+=2) {
                    int cur=i/2;
                    int data=0;
                    for (int j=0;j<128;++j)
                        if (cur-j>=0)
                            data+=int(buf[cur-j])*LeftData[j*2];
                        else
                            data+=int(Previous[cur-j+128])*LeftData[j*2];
                    ori[i]=data/500000;
                    data=0;
                    for (int j=0;j<128;++j)
                        if (cur-j>=0)
                            data+=int(buf[cur-j])*RightData[j*2];
                        else
                            data+=int(Previous[cur-j+128])*RightData[j*2];
                    ori[i+1]=data/500000;
                    if (distance>1) {
                        ori[i]/=distance;
                        ori[i+1]/=distance;
                    }
                }
                memcpy(Previous,buf+length/4-128,256);
            }
        };
        if (!Started()) {
            DSPHandle=BASS.BASS_ChannelSetDSP(ChannelHandle,DSP,this,1);
        }
    }
    
    public void Stop3D() {
        BASS.BASS_ChannelRemoveDSP(ChannelHandle,DSPHandle);
        DSPHandle=0;
    }
    
    public boolean Started() {
        return DSPHandle!=0;
    }
    
    public static void MoveListener(Vec _LisPos,Vec _LisFace,Vec _LisUp) {
        LisPos=_LisPos;
        LisFace=_LisFace.toDirection();
        LisUp=_LisUp.toDirection();
    }
    
    public void MoveSpeaker(Vec _SpePos) {
        SpePos=_SpePos;
    }
}

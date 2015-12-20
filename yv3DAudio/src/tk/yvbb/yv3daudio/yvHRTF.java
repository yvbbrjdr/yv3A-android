package tk.yvbb.yv3daudio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import com.un4seen.bass.*;

public class yvHRTF {
    private int ChannelHandle,DSPHandle=0;
    private Vec SpePos=new Vec();
    private short Previous[]=new short[128];
    private static Vec LisPos=new Vec(),LisFace=new Vec(1,0,0),LisUp=new Vec(0,1,0);
    private static short HRTFData[][][]=new short[14][181][256];
    private static boolean HRTFDataAvailable[][]=new boolean[14][181];
    
    public static void Init(AssetManager am) {
        int n=0;
        byte t[]=new byte[512];
        ByteBuffer bb=ByteBuffer.allocate(512);
        for (int i=0;i<14;++i)
            for (int j=0;j<181;++j) {
                String Filename=String.format("hrtf-data/elev%d/H%de%03da.dat",(i-4)*10,(i-4)*10,j);
                InputStream is;
                try {
                    is=am.open(Filename);
                    is.read(t,0,512);
                    bb.rewind();
                    bb.put(t);
                    bb.order(ByteOrder.BIG_ENDIAN);
                    bb.rewind();
                    ShortBuffer sb=bb.asShortBuffer();
                    sb.get(HRTFData[i][j]);
                    is.close();
                    HRTFDataAvailable[i][j]=true;
                    ++n;
                } catch (IOException e) {
                    continue;
                }
            }
        if (n==0)
            System.exit(0);
    }
    
    public yvHRTF(int handle) {
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
                int oleft=0,oright=0;
                if (Sur.AngleDegree(d)>90) {
                    oleft=1;
                } else {
                    oright=1;
                }
                double distance=LS.Len();
                short ori[]=new short[length/2],buf[]=new short[length/4];
                buffer.order(null);
                ShortBuffer sbuffer=buffer.asShortBuffer();
                sbuffer.get(ori);
                for (int i=0;i<length/2;i+=2)
                    buf[i/2]=(short)(ori[i]/2+ori[i+1]/2);
                for (int i=0;i<length/2;i+=2) {
                    int cur=i/2;
                    int data=0;
                    for (int j=0;j<128;++j)
                        if (cur-j>=0)
                            data+=(int)(buf[cur-j])*HRTFData[e][a][j*2+oleft];
                        else
                            data+=(int)(Previous[cur-j+128])*HRTFData[e][a][j*2+oleft];
                    ori[i]=(short)(data/500000);
                    data=0;
                    for (int j=0;j<128;++j)
                        if (cur-j>=0)
                            data+=(int)(buf[cur-j])*HRTFData[e][a][j*2+oright];
                        else
                            data+=(int)(Previous[cur-j+128])*HRTFData[e][a][j*2+oright];
                    ori[i+1]=(short)(data/500000);
                    if (distance>1) {
                        ori[i]/=distance;
                        ori[i+1]/=distance;
                    }
                }
                sbuffer.rewind();
                sbuffer.put(ori);
                for (int i=0;i<128;++i) {
                    Previous[i]=buf[length/4-128+i];
                }
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

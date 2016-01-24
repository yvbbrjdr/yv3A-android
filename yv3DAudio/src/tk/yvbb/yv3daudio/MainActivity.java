package tk.yvbb.yv3daudio;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

import com.un4seen.bass.BASS;
import com.un4seen.bass.BASSenc;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class MainActivity extends ActionBarActivity {

    int chan,hauto;
    File filepath;
    String[] filelist;
    yvHRTF hrtf;
    Button openbut,stopbut,applybut,funcbut,setbufbut;
    ToggleButton playtb,autotb,enctb;
    EditText xtext,ytext,ztext,buftext;
    String funcs="x=cos(t) 0 3600\ny=0 0 3600\nz=sin(t) 0 3600";

    double parseDouble(String s) {
        double ret;
        try {
            ret=Double.parseDouble(s);
        } catch (Exception e) {
            ret=0;
        }
        return ret;
    }

    Handler TurningHandler=new Handler() {
        public void handleMessage(Message msg) {
            double pos=BASS.BASS_ChannelBytes2Seconds(chan,BASS.BASS_ChannelGetPosition(chan,BASS.BASS_POS_BYTE));
            String[] s=funcs.split(" |\n");
            for (int i=0;i<s.length;i+=3) {
                if (i+2>=s.length)
                    break;
                if (parseDouble(s[i+1])<=pos&&pos<=parseDouble(s[i+2])) {
                    ProcessExpr(pos,s[i]);
                }
            }
            super.handleMessage(msg);
        }

        private void ProcessExpr(double pos,String Expression) {
            EditText et;
            switch (Expression.charAt(0)) {
                case 'x':
                    et=xtext;
                    break;
                case 'y':
                    et=ytext;
                    break;
                case 'z':
                    et=ztext;
                    break;
                default:
                    return;
            }
            et.setText(Eval(Expression.substring(2).replaceAll("t",Double.toString(pos))));
            ApplyClick(null);
        }

        private String Eval(String Expression) {
            Evaluator je=new Evaluator();
            try {
                return je.evaluate(Expression);
            } catch (EvaluationException e) {
                return "0";
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        BASS.BASS_Init(-1,44100,0);
        String path=getApplicationInfo().nativeLibraryDir;
        String[] list=new File(path).list();
        for (String s:list) {
            BASS.BASS_PluginLoad(path+"/"+s,0);
        }
        yvHRTF.Init(getAssets());
        filepath=Environment.getExternalStorageDirectory();
        openbut=(Button)findViewById(R.id.OpenButton);
        playtb=(ToggleButton)findViewById(R.id.PlayTButton);
        stopbut=(Button)findViewById(R.id.StopButton);
        setbufbut=(Button)findViewById(R.id.SetBuffer);
        xtext=(EditText)findViewById(R.id.XText);
        ytext=(EditText)findViewById(R.id.YText);
        ztext=(EditText)findViewById(R.id.ZText);
        buftext=(EditText)findViewById(R.id.BufferText);
        applybut=(Button)findViewById(R.id.ApplyButton);
        autotb=(ToggleButton)findViewById(R.id.AutoTButton);
        funcbut=(Button)findViewById(R.id.FuncButton);
        enctb=(ToggleButton)findViewById(R.id.EncodeTButton);
        buftext.setText(Integer.toString(BASS.BASS_GetConfig(BASS.BASS_CONFIG_BUFFER)));
    }

    public void SetBufClick(View v) {
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER,Integer.parseInt(buftext.getText().toString()));
        buftext.setText(Integer.toString(BASS.BASS_GetConfig(BASS.BASS_CONFIG_BUFFER)));
    }

    public void OpenClick(View v) {
        String[] list=filepath.list();
        if (list==null) list=new String[0];
        if (!filepath.getPath().equals("/")) {
            filelist=new String[list.length+1];
            filelist[0]="..";
            System.arraycopy(list, 0, filelist, 1, list.length);
        } else
            filelist=list;
        new AlertDialog.Builder(this)
            .setTitle("Choose a file to play")
            .setItems(filelist, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    File sel;
                    if (filelist[which].equals("..")) sel=filepath.getParentFile();
                    else sel=new File(filepath, filelist[which]);
                    if (sel.isDirectory()) {
                        filepath=sel;
                        OpenClick(null);
                    } else {
                        String file=sel.getPath();
                        if (autotb.isChecked())
                            StopAuto();
                        if (enctb.isChecked())
                            StopEncode();
                        if (!BASS.BASS_StreamFree(chan))
                            BASS.BASS_MusicFree(chan);
                        if ((chan=BASS.BASS_StreamCreateFile(file, 0, 0, BASS.BASS_SAMPLE_LOOP))==0
                            && (chan=BASS.BASS_MusicLoad(file, 0, 0, BASS.BASS_SAMPLE_LOOP|BASS.BASS_MUSIC_RAMP, 1))==0) {
                            openbut.setText("Open");
                            playtb.setEnabled(false);
                            playtb.setChecked(false);
                            stopbut.setEnabled(false);
                            applybut.setEnabled(false);
                            autotb.setEnabled(false);
                            enctb.setEnabled(false);
                            return;
                        }
                        openbut.setText(file);
                        playtb.setEnabled(true);
                        playtb.setChecked(true);
                        stopbut.setEnabled(true);
                        applybut.setEnabled(true);
                        autotb.setEnabled(true);
                        enctb.setEnabled(true);
                        hrtf=new yvHRTF(chan);
                        ApplyClick(null);
                        hrtf.Start3D();
                        BASS.BASS_ChannelPlay(chan,false);
                    }
                }
            })
               .show();
    }

    public void PlayClick(View v) {
        if (playtb.isChecked()) {
            BASS.BASS_ChannelPlay(chan,false);
            stopbut.setEnabled(true);
        } else {
            BASS.BASS_ChannelPause(chan);
        }
    }

    public void StopClick(View v) {
        BASS.BASS_ChannelStop(chan);
        BASS.BASS_ChannelSetPosition(chan,0,0);
        playtb.setChecked(false);
        stopbut.setEnabled(false);
    }
    
    public void ApplyClick(View v) {
        double x=parseDouble(xtext.getText().toString()),
               y=parseDouble(ytext.getText().toString()),
               z=parseDouble(ztext.getText().toString());
        hrtf.MoveSpeaker(new Vec(x,y,z));
    }
    
    public void AutoClick(View v) {
        if (autotb.isChecked()) {
            StartAuto();
        } else {
            StopAuto();
        }
    }

    public void FuncClick(View v) {
        Intent intent=new Intent(MainActivity.this,FunctionControls.class);
        intent.putExtra("Funcs",funcs);
        startActivityForResult(intent,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==1&&requestCode==1)
            funcs=data.getStringExtra("Funcs");
    }

    public void StartAuto() {
        BASS.DSPPROC DSP=new BASS.DSPPROC() {
            public void DSPPROC(int handle,int channel,ByteBuffer buffer,int length,Object user) {
                ((Handler)user).sendEmptyMessage(1);
            }
        };
        hauto=BASS.BASS_ChannelSetDSP(chan,DSP,TurningHandler,2);
        xtext.setEnabled(false);
        ytext.setEnabled(false);
        ztext.setEnabled(false);
        autotb.setChecked(true);
    }

    public void StopAuto() {
        BASS.BASS_ChannelRemoveDSP(chan,hauto);
        xtext.setEnabled(true);
        ytext.setEnabled(true);
        ztext.setEnabled(true);
        autotb.setChecked(false);
    }

    public void StartEncode() {
        SimpleDateFormat df=new SimpleDateFormat("yyyyMMddHHmmss");
        BASS.BASS_CHANNELINFO info=new BASS.BASS_CHANNELINFO();
        BASS.BASS_ChannelGetInfo(chan,info);
        String filename=info.filename+"."+df.format(new Date())+".wav";
        BASSenc.BASS_Encode_Start(chan,filename,BASSenc.BASS_ENCODE_PCM,null,null);
        Toast toast=Toast.makeText(getApplicationContext(),"The file will be writen to "+filename,Toast.LENGTH_LONG);
        toast.show();
        enctb.setChecked(true);
    }

    public void StopEncode() {
        BASSenc.BASS_Encode_Stop(chan);
        enctb.setChecked(false);
    }

    public void EncodeClick(View v) {
        if (enctb.isChecked()) {
            StartEncode();
        } else {
            StopEncode();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
